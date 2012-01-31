package at.jku.pervasive.ecg;

import java.util.List;
import java.util.concurrent.Semaphore;

import javax.bluetooth.ServiceRecord;

import junit.framework.TestCase;

public class HeartManDiscoveryTest extends TestCase {

  private HeartManDiscovery heartManDiscovery;
  private HeartManSimulator heartManSimulator;

  public void testDiscoverHeartManDevices() throws Exception {
    heartManSimulator.createDevice();

    List<HeartManDevice> devices = heartManDiscovery.discoverHeartManDevices();
    assertNotNull(devices);
    assertEquals(1, devices.size());

    HeartManDevice device = devices.get(0);
    assertNotNull(device);
  }

  public void testGetService() throws Exception {
    String address = heartManSimulator.createDevice();

    heartManDiscovery.discoverHeartManDevices();

    List<ServiceRecord> services = heartManDiscovery.searchServices(address);
    assertNotNull(services);
    assertEquals(1, services.size());
  }

  public void testRecording() throws Exception {
    String address = heartManSimulator.createDevice();
    heartManDiscovery.discoverHeartManDevices();
    heartManDiscovery.startRecording(address);

    final Semaphore s = new Semaphore(0);
    TestHeartManListener listener = new TestHeartManListener() {
      @Override
      public void dataReceived(String address, double value) {
        s.release();
      }
    };
    heartManDiscovery.startListening(address, listener);
    heartManSimulator.sendValue(address, 1.0);
    heartManSimulator.sendValue(address, 2.0);
    heartManSimulator.sendValue(address, 3.0);
    heartManSimulator.sendValue(address, 4.0);
    s.acquire(4);

    List<Double> recordings = heartManDiscovery.stopRecording(address);
    assertNotNull(recordings);
    assertEquals(4, recordings.size());
    assertEquals(1.0, recordings.get(0), 0.1D);
    assertEquals(2.0, recordings.get(1), 0.1D);
    assertEquals(3.0, recordings.get(2), 0.1D);
    assertEquals(4.0, recordings.get(3), 0.1D);
  }

  public void testStartListening() throws Exception {
    String address = heartManSimulator.createDevice();

    heartManDiscovery.discoverHeartManDevices();

    final Semaphore s = new Semaphore(0);
    TestHeartManListener listener = new TestHeartManListener() {
      @Override
      public void dataReceived(String address, double value) {
        super.dataReceived(address, value);
        s.release();
      }
    };
    heartManDiscovery.startListening(address, listener);
    heartManSimulator.sendValue(address, 666.6D);
    s.acquireUninterruptibly();

    assertEquals(666.6D, listener.receivedValue, 0.01D);
    System.out.println("finished");
  }

  public void testStartListeningForInvalidAdress() throws Exception {
    try {
      heartManDiscovery.startListening((String) null,
          new DefaulHeartManListener());
      fail("should throw an exception when trying to start listening for a device which is not available");
    } catch (Exception e) {
    }
  }

  public void testStartListeningForMoreDevices() throws Exception {
    String first = heartManSimulator.createDevice();
    String second = heartManSimulator.createDevice();

    final Semaphore s = new Semaphore(0);
    TestHeartManListener listener = new TestHeartManListener() {
      @Override
      public void dataReceived(String address, double value) {
        super.dataReceived(address, value);
        s.release(1);
      }
    };

    List<HeartManDevice> devices = heartManDiscovery.discoverHeartManDevices();
    assertNotNull(devices);
    assertEquals(2, devices.size());

    heartManDiscovery.startListening(first, listener);
    heartManDiscovery.startListening(second, listener);

    heartManSimulator.sendValue(first, 42);
    s.acquire();

    assertTrue(listener.invoked);
    assertEquals(first, listener.address);
    assertEquals(42, listener.receivedValue, 0.1D);
    assertEquals(0, s.availablePermits());

    listener.reset();
    heartManSimulator.sendValue(second, 13);
    s.acquire();

    assertTrue(listener.invoked);
    assertEquals(second, listener.address);
    assertEquals(13, listener.receivedValue, 0.1D);
    assertEquals(0, s.availablePermits());
  }

  public void testStartListeningWithMoreListeners() throws Exception {
    String address = heartManSimulator.createDevice();

    final Semaphore s = new Semaphore(0);
    TestHeartManListener l1 = new TestHeartManListener() {
      @Override
      public void dataReceived(String address, double value) {
        super.dataReceived(address, value);
        s.release(1);
      }
    };
    TestHeartManListener l2 = new TestHeartManListener() {
      @Override
      public void dataReceived(String address, double value) {
        super.dataReceived(address, value);
        s.release(1);
      }
    };

    heartManDiscovery.discoverHeartManDevices();

    heartManDiscovery.startListening(address, l1);
    heartManSimulator.sendValue(address, 123.4D);
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
    heartManDiscovery.startListening(address, l2);
    heartManSimulator.sendValue(address, 567.8D);
    // wait until both are invoked
    s.acquire(2);

    // both listeners invoked
    assertTrue(l1.invoked);
    assertEquals(567.8D, l1.receivedValue, 0.1D);
    assertTrue(l2.invoked);
    assertEquals(567.8D, l2.receivedValue, 0.1D);
  }

  public void testStartListeringWithInvalidListener() throws Exception {
    String address = heartManSimulator.createDevice();

    List<HeartManDevice> devices = heartManDiscovery.discoverHeartManDevices();
    assertNotNull(devices);
    assertEquals(1, devices.size());

    try {
      heartManDiscovery.startListening(address, null);
    } catch (Exception e) {
      fail("should not throw an exception");
    }
  }

  public void testStopListening() throws Exception {
    String address = heartManSimulator.createDevice();
    heartManDiscovery.discoverHeartManDevices();

    final Semaphore s = new Semaphore(0);
    TestHeartManListener listener = new TestHeartManListener() {
      @Override
      public void dataReceived(String address, double value) {
        super.dataReceived(address, value);
        s.release();
      }
    };

    heartManDiscovery.startListening(address, listener);
    heartManSimulator.sendValue(address, 42);
    s.acquire();

    assertTrue(listener.invoked);
    listener.reset();

    heartManDiscovery.stopListening(address);

    heartManSimulator.sendValue(address, 69);

    assertFalse(listener.invoked);
  }

  public void testStopListeningForNullAddress() throws Exception {
    try {
      heartManDiscovery.stopListening(null);
    } catch (Exception e) {
      fail("should not thrown an exception");
    }
  }

  public void testStopListeningWhenNotListening() throws Exception {
    String address = heartManSimulator.createDevice();

    try {
      heartManDiscovery.stopListening(address);
    } catch (Exception e) {
      fail("should not thrown an exception");
    }
  }

  public void testTwoHeartManDiscoveryInstances() throws Exception {
    heartManSimulator.createDevice();

    List<HeartManDevice> firstList = heartManDiscovery
        .discoverHeartManDevices();
    assertNotNull(firstList);
    assertEquals(1, firstList.size());

    HeartManDiscovery secondDiscovery = new HeartManDiscovery();
    List<HeartManDevice> secondList = secondDiscovery.discoverHeartManDevices();
    assertNotNull(secondList);
    assertEquals(1, secondList.size());
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