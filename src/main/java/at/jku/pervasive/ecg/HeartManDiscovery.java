package at.jku.pervasive.ecg;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;

public class HeartManDiscovery {

  public static final List<RemoteDevice> devicesDiscovered = new LinkedList<RemoteDevice>();
  public static final List<String> serviceFound = new LinkedList<String>();

  public List<HeartManDevice> discoverHeartManDevices() throws IOException,
      InterruptedException {

    final Object inquiryCompletedEvent = new Object();

    devicesDiscovered.clear();

    DiscoveryListener listener = new DiscoveryListener() {

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

      public void inquiryCompleted(int discType) {
        System.out.println("Device Inquiry completed!");
        synchronized (inquiryCompletedEvent) {
          inquiryCompletedEvent.notifyAll();
        }
      }

      public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
        System.out.println("servicesDiscovered");
        for (int i = 0; i < servRecord.length; i++) {
          String url = servRecord[i].getConnectionURL(
              ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
          if (url == null) {
            continue;
          }
          serviceFound.add(url);
          DataElement serviceName = servRecord[i].getAttributeValue(0x0100);
          if (serviceName != null) {
            System.out.println("service " + serviceName.getValue() + " found "
                + url);
          } else {
            System.out.println("service found " + url);
          }
        }
      }

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
          heartManDevices
              .add(new HeartManDevice(device.getFriendlyName(false)));
        }

        return heartManDevices;
      }

      return null;
    }
  }

  public String getData(long uuid) throws IOException {
    DiscoveryAgent da = LocalDevice.getLocalDevice().getDiscoveryAgent();
    int security = ServiceRecord.NOAUTHENTICATE_NOENCRYPT;
    String serviceUrl = da.selectService(new UUID(uuid), security, false);

    DataInputStream dis = Connector.openDataInputStream(serviceUrl);
    String read = dis.readUTF();
    dis.close();
    return read;
  }

  public boolean isBluetoothEnabled() {
    return LocalDevice.isPowerOn();
  }

}
