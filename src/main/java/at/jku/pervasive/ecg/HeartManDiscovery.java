package at.jku.pervasive.ecg;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;

import com.intel.bluetooth.BlueCoveImpl;
import com.intel.bluetooth.BluetoothConsts;

public class HeartManDiscovery {

  public static final UUID RFCOMM_UUID = BluetoothConsts.RFCOMM_PROTOCOL_UUID;

  private final List<RemoteDevice> devicesDiscovered = new LinkedList<RemoteDevice>();
  private final Map<String, ListeningTask> listeningTasks = new HashMap<String, ListeningTask>();

  private final Object STACK_ID;

  public HeartManDiscovery() {
    super();

    try {
      STACK_ID = BlueCoveImpl.getThreadBluetoothStackID();
    } catch (BluetoothStateException e) {
      throw new RuntimeException(e);
    }
  }

  public List<HeartManDevice> discoverHeartManDevices() throws IOException,
      InterruptedException {

    final Object inquiryCompletedEvent = new Object();

    devicesDiscovered.clear();

    DiscoveryListener listener = new DiscoveryListener() {

      @Override
      public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
        System.out.println("Device " + btDevice.getBluetoothAddress()
            + " found");
        devicesDiscovered.add(btDevice);
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
      boolean started = LocalDevice.getLocalDevice().getDiscoveryAgent()
          .startInquiry(DiscoveryAgent.GIAC, listener);
      if (started) {
        System.out.println("wait for device inquiry to complete...");
        inquiryCompletedEvent.wait();
        System.out.println(devicesDiscovered.size() + " device(s) found");

        List<HeartManDevice> heartManDevices = new LinkedList<HeartManDevice>();
        for (RemoteDevice device : devicesDiscovered) {
          heartManDevices.add(new HeartManDevice(device.getFriendlyName(false),
              device));
        }

        return heartManDevices;
      }

      return null;
    }
  }

  public double getData(long uuid) throws IOException {
    DiscoveryAgent da = LocalDevice.getLocalDevice().getDiscoveryAgent();
    int security = ServiceRecord.NOAUTHENTICATE_NOENCRYPT;
    String serviceUrl = da.selectService(new UUID(uuid), security, false);

    DataInputStream dis = Connector.openDataInputStream(serviceUrl);
    double read = dis.readDouble();
    dis.close();
    return read;
  }

  public boolean isBluetoothEnabled() {
    return LocalDevice.isPowerOn();
  }

  public List<ServiceRecord> searchServices(RemoteDevice remoteDevice)
      throws BluetoothStateException {

    final List<ServiceRecord> serviceRecords = new LinkedList<ServiceRecord>();

    LocalDevice localDevice = LocalDevice.getLocalDevice();
    DiscoveryAgent discoveryAgent = localDevice.getDiscoveryAgent();

    final Semaphore searchServicesLock = new Semaphore(0);

    final int[] attrs = new int[] { 0x0100 }; // Service name
    UUID[] serviceUUIDs = new UUID[] { HeartManDiscovery.RFCOMM_UUID };
    discoveryAgent.searchServices(attrs, serviceUUIDs, remoteDevice,
        new DiscoveryListener() {

          @Override
          public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
          }

          @Override
          public void inquiryCompleted(int discType) {
          }

          @Override
          public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
            for (int i = 0; i < servRecord.length; i++) {
              String url = servRecord[i].getConnectionURL(
                  ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
              if (url == null) {
                continue;
              }
              DataElement serviceName = servRecord[i].getAttributeValue(0x0100);

              if (serviceName != null) {
                System.out.println("service " + serviceName.getValue()
                    + " found " + url);
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
    try {
      searchServicesLock.acquire();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return serviceRecords;
  }

  public void startListening(HeartManDevice device, HeartManListener listener)
      throws BluetoothStateException {
    String address = device.getDevice().getBluetoothAddress();
    ListeningTask listeningTask = listeningTasks.get(address);
    boolean start = false;
    if (listeningTask == null) {
      List<ServiceRecord> services = searchServices(device.getDevice());

      ServiceRecord serviceRecord = services.get(0);
      listeningTask = new ListeningTask(STACK_ID, serviceRecord);
      listeningTasks.put(address, listeningTask);
      start = true;

    }
    listeningTask.addListener(listener);
    if (start) {
      listeningTask.start();
    }
  }

  public void stopListening(long uuid) {
    ListeningTask listeningTask = listeningTasks.get(uuid);
    if (listeningTask != null) {
      listeningTask.interrupt();
    }
  }
}
