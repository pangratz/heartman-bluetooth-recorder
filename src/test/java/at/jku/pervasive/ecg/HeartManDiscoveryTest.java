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

  public void testStartListeningForMoreDevices() throws Exception {
    heartManSimulator.startDevice(666L);
    heartManSimulator.startDevice(667L);

    final Semaphore s = new Semaphore(0);
    TestHeartManListener listener = new TestHeartManListener() {
      @Override
      public void dataReceived(double value) {
        super.dataReceived(value);
        s.release(1);
      }
    };

    List<HeartManDevice> devices = heartManDiscovery.discoverHeartManDevices();
    assertNotNull(devices);
    assertEquals(2, devices.size());

    heartManDiscovery.startListening(devices.get(0), listener);
    heartManDiscovery.startListening(devices.get(1), listener);

    heartManSimulator.sendValue(666L, 42);
    s.acquire();

    assertTrue(listener.invoked);
    assertEquals(42, listener.receivedValue, 0.1D);
    assertEquals(0, s.availablePermits());

    heartManSimulator.sendValue(667L, 13);
    s.acquire();

    assertTrue(listener.invoked);
    assertEquals(13, listener.receivedValue, 0.1D);
    assertEquals(0, s.availablePermits());
  }

  public void testStartListeningWithMoreListeners() throws Exception {
    heartManSimulator.startDevice(666L);

    final Semaphore s = new Semaphore(0);
    TestHeartManListener l1 = new TestHeartManListener() {
      @Override
      public void dataReceived(double value) {
        super.dataReceived(value);
        s.release(1);
      }
    };
    TestHeartManListener l2 = new TestHeartManListener() {
      @Override
      public void dataReceived(double value) {
        super.dataReceived(value);
        s.release(1);
      }
    };

    List<HeartManDevice> devices = heartManDiscovery.discoverHeartManDevices();
    HeartManDevice device = devices.get(0);

    heartManDiscovery.startListening(device, l1);
    heartManSimulator.sendValue(666L, 123.4D);
    // wait until listener got invoked
    s.acquire(1);

    // l1 invoked - l2 not
    assertTrue(l1.invoked);
    assertEquals(123.4D, l1.receivedValue, 0.1D);
    assertFalse(l2.invoked);

    // reset
    l1.reset();
    l2.reset();

    // add second listener
    heartManDiscovery.startListening(device, l2);
    heartManSimulator.sendValue(666L, 567.8D);
    // wait until both are invoked
    s.acquire(2);

    // both listeners invoked
    assertTrue(l1.invoked);
    assertEquals(567.8D, l1.receivedValue, 0.1D);
    assertTrue(l2.invoked);
    assertEquals(567.8D, l2.receivedValue, 0.1D);
  }

  public void testStartListeringWithInvalidListener() throws Exception {
    heartManSimulator.startDevice(666L);

    List<HeartManDevice> devices = heartManDiscovery.discoverHeartManDevices();
    assertNotNull(devices);
    assertEquals(1, devices.size());
    HeartManDevice device = devices.get(0);

    try {
      heartManDiscovery.startListening(device, null);
    } catch (Exception e) {
      fail("should not throw an exception");
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
