package at.jku.pervasive.ecg;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.bluetooth.BluetoothConnectionException;
import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;

import com.intel.bluetooth.BlueCoveImpl;
import com.intel.bluetooth.BluetoothConsts;
import com.intel.bluetooth.RemoteDeviceHelper;

public class HeartManDiscovery {

  public static final UUID HEARTMAN_SERVICE_UUID = BluetoothConsts.RFCOMM_PROTOCOL_UUID;
  public static Object STACK_ID;

  private static final HeartManDiscovery INSTANCE = new HeartManDiscovery();

  public static final HeartManDiscovery getInstance() {
    return INSTANCE;
  }

  private final Semaphore deviceInquiry = new Semaphore(1);
  private List<HeartManDevice> discoveredHeartManDevices;
  private final Map<String, List<ServiceRecord>> servicesDiscovered = new HashMap<String, List<ServiceRecord>>();
  private final Map<String, RemoteDevice> devicesDiscovered = new HashMap<String, RemoteDevice>();
  private final Map<String, ListeningTask> listeningTasks = new HashMap<String, ListeningTask>();

  // flag, indicating whether a device inquiry has started
  private boolean isDeviceInquiryStarted;

  // check for new ecg values every UPDATE_RATE ms
  private final long updateRate;

  private HeartManDiscovery() {
    this(5);
  }

  private HeartManDiscovery(long updateRate) {
    super();

    this.updateRate = updateRate;

    try {
      BlueCoveImpl.useThreadLocalBluetoothStack();
      STACK_ID = BlueCoveImpl.getThreadBluetoothStackID();
      BlueCoveImpl.setDefaultThreadBluetoothStackID(STACK_ID);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public List<HeartManDevice> discoverHeartManDevices() throws IOException, InterruptedException {

    BlueCoveImpl.setDefaultThreadBluetoothStackID(STACK_ID);

    final Object inquiryCompletedEvent = new Object();

    devicesDiscovered.clear();

    DiscoveryListener listener = new DiscoveryListener() {

      @Override
      public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
        System.out.println("Device " + btDevice.getBluetoothAddress() + " found");
        try {
          // RemoteDeviceHelper.authenticate(btDevice, "heartman");
        } catch (Exception e) {
          e.printStackTrace();
        }
        try {
          String name = btDevice.getFriendlyName(true);

          // Only add devices which have "heartman" in their name
          if (name != null && name.toLowerCase().contains("heartman")) {
            devicesDiscovered.put(btDevice.getBluetoothAddress(), btDevice);
          }
        } catch (IOException cantGetDeviceName) {
        }
        try {
          System.out.println("     name " + btDevice.getFriendlyName(true));
          System.out.println("     adress: " + btDevice.getBluetoothAddress());
        } catch (IOException cantGetDeviceName) {
        }
      }

      @Override
      public void inquiryCompleted(int discType) {
        System.out.println("Device Inquiry completed!");
        synchronized (inquiryCompletedEvent) {
          deviceInquiry.release();
          inquiryCompletedEvent.notifyAll();
        }
      }

      @Override
      public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
      }

      @Override
      public void serviceSearchCompleted(int transID, int respCode) {
      }
    };

    synchronized (inquiryCompletedEvent) {
      deviceInquiry.acquire();
      boolean started = LocalDevice.getLocalDevice().getDiscoveryAgent().startInquiry(DiscoveryAgent.GIAC, listener);
      if (started) {
        this.isDeviceInquiryStarted = true;
        System.out.println("wait for device inquiry to complete...");
        inquiryCompletedEvent.wait();
        System.out.println(devicesDiscovered.size() + " device(s) found");

        discoveredHeartManDevices = new LinkedList<HeartManDevice>();
        for (RemoteDevice device : devicesDiscovered.values()) {
          String name = "UNKNOWN";
          try {
            name = device.getFriendlyName(false);
          } catch (Exception e) {
            e.printStackTrace();
          }
          HeartManDevice heartManDevice = new HeartManDevice(device.getBluetoothAddress(), name);
          discoveredHeartManDevices.add(heartManDevice);
        }

        for (HeartManDevice device : discoveredHeartManDevices) {
          searchServices(device.getAddress());
        }

        this.isDeviceInquiryStarted = false;
        return discoveredHeartManDevices;
      }

      this.isDeviceInquiryStarted = false;
      return null;
    }
  }

  public List<HeartManDevice> getDiscoveredHeartManDevices() {
    return discoveredHeartManDevices;
  }

  protected ListeningTask getListeningTask(String address, ServiceRecord serviceRecord) {
    ListeningTask listeningTask = listeningTasks.get(address);
    if (listeningTask == null) {
      listeningTask = new ListeningTask(STACK_ID, updateRate, serviceRecord);
      listeningTasks.put(address, listeningTask);
      listeningTask.start();
    }
    return listeningTask;
  }

  public boolean isBluetoothEnabled() {
    return LocalDevice.isPowerOn();
  }

  public boolean isDeviceInquiryStarted() {
    return isDeviceInquiryStarted;
  }

  public RemoteDevice pingDevice(String address) throws IOException {
    String urlPattern = "btspp://%1$s:1;authenticate=false;encrypt=false;master=false";
    String url = String.format(urlPattern, address);

    System.out.println("ping device");

    try {
      Connection connection = Connector.open(url, Connector.READ);
      RemoteDevice remoteDevice = RemoteDevice.getRemoteDevice(connection);
      RemoteDeviceHelper.authenticate(remoteDevice, "Heartman");
      return remoteDevice;
    } catch (BluetoothConnectionException bce) {
      if (bce.getMessage().startsWith("No such device")) {
        return null;
      }
      throw bce;
    }
  }

  public List<ServiceRecord> searchServices(RemoteDevice remoteDevice) throws BluetoothStateException {

    final List<ServiceRecord> serviceRecords = new LinkedList<ServiceRecord>();

    LocalDevice localDevice = LocalDevice.getLocalDevice();
    DiscoveryAgent discoveryAgent = localDevice.getDiscoveryAgent();

    final Semaphore searchServicesLock = new Semaphore(0);

    final int[] attrs = new int[] { 0x0100 }; // Service name
    UUID[] serviceUUIDs = new UUID[] { HeartManDiscovery.HEARTMAN_SERVICE_UUID };
    discoveryAgent.searchServices(attrs, serviceUUIDs, remoteDevice, new DiscoveryListener() {

      @Override
      public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
      }

      @Override
      public void inquiryCompleted(int discType) {
      }

      @Override
      public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
        for (int i = 0; i < servRecord.length; i++) {
          String url = servRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
          if (url == null) {
            continue;
          }
          DataElement serviceName = servRecord[i].getAttributeValue(0x0100);

          if (serviceName != null) {
            System.out.println("service " + serviceName.getValue() + " found " + url);
            serviceRecords.add(servRecord[i]);

          } else {
            System.out.println("service found " + url);
          }
        }
      }

      @Override
      public void serviceSearchCompleted(int transID, int respCode) {
        searchServicesLock.release();
      }
    });
    searchServicesLock.acquireUninterruptibly();
    return serviceRecords;
  }

  public List<ServiceRecord> searchServices(String address) throws BluetoothStateException {

    System.out.println("search for services for " + address);

    RemoteDevice device = devicesDiscovered.get(address);
    if (device != null) {
      List<ServiceRecord> serviceRecords = servicesDiscovered.get(address);
      if (serviceRecords == null) {
        serviceRecords = searchServices(device);
        servicesDiscovered.put(address, serviceRecords);
      }
      return serviceRecords;
    }
    return null;
  }

  public void startListening(String address, IHeartManListener listener) throws BluetoothStateException {
    ListeningTask listeningTask = listeningTasks.get(address);
    boolean start = false;
    if (listeningTask == null) {
      List<ServiceRecord> services = searchServices(address);

      ServiceRecord serviceRecord = services.get(0);
      listeningTask = new ListeningTask(STACK_ID, updateRate, serviceRecord);
      listeningTasks.put(address, listeningTask);
      start = true;

    }
    listeningTask.addListener(listener);
    if (start) {
      listeningTask.start();
    }
  }

  public void startListening(String address, IHeartManListener listener, ServiceRecord serviceRecord)
      throws BluetoothStateException {
    ListeningTask listeningTask = listeningTasks.get(address);
    boolean start = false;
    if (listeningTask == null) {
      listeningTask = new ListeningTask(STACK_ID, updateRate, serviceRecord);
      listeningTasks.put(address, listeningTask);
      start = true;

    }
    listeningTask.addListener(listener);
    if (start) {
      listeningTask.start();
    }
  }

  public void stopListening(String address) {
    ListeningTask listeningTask = listeningTasks.get(address);
    if (listeningTask != null) {
      listeningTask.clearListener();
      listeningTask.interrupt();
      listeningTask = null;
      listeningTasks.put(address, null);
    }
  }

  public void tearDown() {
    for (String address : listeningTasks.keySet()) {
      stopListening(address);
    }
  }
}
