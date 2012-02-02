package at.jku.pervasive.ecg;

import java.io.File;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.bluetooth.ServiceRecord;

import junit.framework.TestCase;

public class ECGMonitorTest extends TestCase {

  private HeartManDiscovery heartManDiscovery;
  private HeartManSimulator heartManSimulator;

  public void NONtestECGMonitor() throws Exception {
    File file = new File("recording20s_sleep20ms_1.dat");
    String address = heartManSimulator.createFileDevice(file);
    heartManDiscovery.discoverHeartManDevices();
    List<ServiceRecord> services = heartManDiscovery.searchServices(address);
    assertNotNull(services);
    ServiceRecord service = services.get(0);

    ECGMonitor ecgMonitor = new ECGMonitor();
    ecgMonitor.setVisible(true);

    IHeartManListener l = ecgMonitor.getHeartManListener();

    heartManDiscovery.startListening(address, l, service);
    Semaphore s = new Semaphore(0);
    s.acquire();
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
