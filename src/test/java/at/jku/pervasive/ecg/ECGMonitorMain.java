package at.jku.pervasive.ecg;

import java.io.IOException;
import java.util.List;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;

import junit.framework.Assert;

public class ECGMonitorMain {

  public static void main(String[] args) throws Exception {
    String address1 = "00A096203DCB"; // HeartMan C102
    String address2 = "00A096203DCB"; // HeartMan C102

    HeartManDiscovery heartManDiscovery = new HeartManDiscovery();
    Assert.assertTrue(heartManDiscovery.isBluetoothEnabled());

    ServiceRecord serviceRecord1 = getServiceRecord(address1, heartManDiscovery);
    ServiceRecord serviceRecord2 = getServiceRecord(address2, heartManDiscovery);

    ECGMonitor ecgMonitor = new ECGMonitor();
    ecgMonitor.setVisible(true);

    IHeartManListener l = ecgMonitor.getHeartManListener();

    heartManDiscovery.startListening(address1, l, serviceRecord1);
    heartManDiscovery.startListening(address2, l, serviceRecord2);
  }

  private static ServiceRecord getServiceRecord(String address,
      HeartManDiscovery heartManDiscovery) throws IOException,
      BluetoothStateException {
    RemoteDevice device1 = heartManDiscovery.pingDevice(address);
    List<ServiceRecord> services1 = heartManDiscovery.searchServices(device1);
    Assert.assertNotNull(services1);
    Assert.assertTrue(services1.size() > 0);
    ServiceRecord serviceRecord = services1.get(0);
    return serviceRecord;
  }
}
