package at.jku.pervasive.ecg;

import java.io.File;
import java.net.URL;

import junit.framework.Assert;

public class ECGMonitorRecordingMain {

  public static void main(String[] args) throws Exception {
    HeartManSimulator simulator = new HeartManSimulator();
    HeartManDiscovery discovery = new HeartManDiscovery();
    Assert.assertTrue(discovery.isBluetoothEnabled());

    String recording = "/recording20s_sleep5ms_1.dat";
    URL url = ECGMonitorMain.class.getResource(recording);
    File file = new File(url.toURI());
    String address = simulator.createFileDevice(file);
    discovery.discoverHeartManDevices();

    ECGMonitor ecgMonitor = new ECGMonitor();
    ecgMonitor.setVisible(true);

    IHeartManListener l = ecgMonitor.getHeartManListener();
    discovery.startListening(address, l);
  }

}
