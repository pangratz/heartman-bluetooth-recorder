package at.jku.pervasive.ecg;

import java.util.List;

import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;

import junit.framework.Assert;

public class ECGMonitorMain {

  public static void main(String[] args) throws Exception {
    String address = "00A096203DCB"; // HeartMan C102

    HeartManDiscovery heartManDiscovery = new HeartManDiscovery();
    Assert.assertTrue(heartManDiscovery.isBluetoothEnabled());

    RemoteDevice device = heartManDiscovery.pingDevice(address);
    List<ServiceRecord> services = heartManDiscovery.searchServices(device);
    Assert.assertNotNull(services);
    Assert.assertTrue(services.size() > 0);
    ServiceRecord serviceRecord = services.get(0);

    ECGMonitor ecgMonitor = new ECGMonitor();
    ecgMonitor.setVisible(true);

    IHeartManListener l = ecgMonitor.getHeartManListener();

    heartManDiscovery.startListening(address, l, serviceRecord);
  }
}
