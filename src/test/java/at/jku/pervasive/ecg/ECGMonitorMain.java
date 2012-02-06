package at.jku.pervasive.ecg;

import java.io.IOException;
import java.util.List;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;

import junit.framework.Assert;

public class ECGMonitorMain {

  public static final String HEART_MAN_C102 = "00A096203DCB";
  public static final String HEART_MAN_C151 = "00A096203DCD";
  public static final String HEART_MAN_C157 = "00A096203DD1";

  public static void main(String[] args) throws Exception {
    String address1 = HEART_MAN_C102;
    String address2 = HEART_MAN_C157;

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
