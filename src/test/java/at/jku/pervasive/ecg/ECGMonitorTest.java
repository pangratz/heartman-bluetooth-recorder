package at.jku.pervasive.ecg;

import junit.framework.TestCase;

public class ECGMonitorTest extends TestCase {

  private HeartManDiscovery heartManDiscovery;
  private HeartManSimulator heartManSimulator;

  public void NOTtestECGMonitor() throws Exception {
    String address = heartManSimulator.createDevice();
    heartManDiscovery.discoverHeartManDevices();

    ECGMonitor ecgMonitor = new ECGMonitor();
    ecgMonitor.setVisible(true);

    heartManDiscovery.startListening(address, ecgMonitor.getHeartManListener());

    while (true) {
      heartManSimulator.sendValue(address, Math.random() - 0.5D);
      Thread.sleep((long) (Math.random() * 100 + 100));
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    heartManSimulator = new HeartManSimulator();

    heartManDiscovery = new HeartManDiscovery();
    assertTrue(heartManDiscovery.isBluetoothEnabled());
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();

    heartManDiscovery = null;

    heartManSimulator.stopServer();
    heartManSimulator = null;
  }

}
