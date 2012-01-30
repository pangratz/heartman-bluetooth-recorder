package at.jku.pervasive.ecg;

import java.util.List;
import java.util.concurrent.Semaphore;

import junit.framework.TestCase;

public class HeartManDiscoveryTest extends TestCase {

  private HeartManDiscovery heartManDiscovery;
  private HeartManSimulator heartManSimulator;

  public void testDiscoverHeartManDevices() throws Exception {
    heartManSimulator.startDevice(666);

    List<HeartManDevice> devices = heartManDiscovery.discoverHeartManDevices();
    assertNotNull(devices);
    assertEquals(1, devices.size());

    HeartManDevice device = devices.get(0);
    assertNotNull(device);
  }

  public void testGetService() throws Exception {
    heartManSimulator.startDevice(123);
    heartManSimulator.startDevice(456);

    List<HeartManDevice> heartManDevices = heartManDiscovery
        .discoverHeartManDevices();

    heartManDiscovery.searchServices(heartManDevices.get(0).getDevice());
    heartManDiscovery.searchServices(heartManDevices.get(1).getDevice());
  }

  public void testStartListening() throws Exception {
    heartManSimulator.startDevice(666L);

    final Semaphore s = new Semaphore(0);
    TestHeartManListener listener = new TestHeartManListener() {
      @Override
      public void dataReceived(double value) {
        super.dataReceived(value);
        s.release();
      }
    };
    List<HeartManDevice> devices = heartManDiscovery.discoverHeartManDevices();
    heartManDiscovery.startListening(devices.get(0), listener);
    heartManSimulator.sendValue(666L, 666.6D);
    s.acquireUninterruptibly();

    assertEquals(666.6D, listener.receivedValue, 0.01D);
    System.out.println("finished");
  }

  public void testStartListeningForInvalidAdress() throws Exception {
    try {
      heartManDiscovery.startListening(null, new DefaulHeartManListener());
      fail("should throw an exception when trying to start listening for a device which is not available");
    } catch (Exception e) {
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
