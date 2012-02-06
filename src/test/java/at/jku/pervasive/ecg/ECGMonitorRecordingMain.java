package at.jku.pervasive.ecg;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.ServiceRecord;

import junit.framework.Assert;

public class ECGMonitorRecordingMain {

  public static void main(String[] args) throws Exception {
    HeartManSimulator simulator = new HeartManSimulator();
    HeartManDiscovery discovery = new HeartManDiscovery();
    Assert.assertTrue(discovery.isBluetoothEnabled());

    String address1 = startDevice(simulator, "/recording20s_sleep5ms_1.dat");
    String address2 = startDevice(simulator, "/recording20s_sleep5ms_2.dat");

    discovery.discoverHeartManDevices();

    List<ServiceRecord> firstServices = discovery.searchServices(address1);
    ServiceRecord firstServiceRecord = firstServices.get(0);

    List<ServiceRecord> secondServices = discovery.searchServices(address2);
    ServiceRecord secondServiceRecord = secondServices.get(0);

    ECGMonitor ecgMonitor = new ECGMonitor();
    ecgMonitor.setVisible(true);

    IHeartManListener l = ecgMonitor.getHeartManListener();
    discovery.startListening(address1, l, firstServiceRecord);
    discovery.startListening(address2, l, secondServiceRecord);
  }

  private static String startDevice(HeartManSimulator simulator,
      String recording) throws URISyntaxException, BluetoothStateException {
    URL url = ECGMonitorMain.class.getResource(recording);
    File file = new File(url.toURI());
    String address1 = simulator.createFileDevice(file);
    return address1;
  }

}
