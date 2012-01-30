package at.jku.pervasive.ecg;

import java.io.IOException;
import java.util.List;

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

  public void testGetData() throws IOException {
    heartManSimulator.startDevice(666);

    heartManSimulator.sendValue(666L, 1.0);
    assertEquals(1.0D, heartManDiscovery.getData(666), 0.1D);
  }

  public void testGetService() throws Exception {
    heartManSimulator.startDevice(123);
    heartManSimulator.startDevice(456);

    List<HeartManDevice> heartManDevices = heartManDiscovery
        .discoverHeartManDevices();

    System.out.println("###");

    heartManDiscovery.searchServices(heartManDevices.get(0).getDevice());
    heartManDiscovery.searchServices(heartManDevices.get(1).getDevice());
  }

  public void testStartListening() throws Exception {
    heartManSimulator.startDevice(666L);

    TestHeartManListener listener = new TestHeartManListener() {
      @Override
      public void dataReceived(double value) {
        super.dataReceived(value);
        System.out.println("data Received: " + value);
      }
    };
    List<HeartManDevice> devices = heartManDiscovery.discoverHeartManDevices();
    heartManDiscovery.startListening(devices.get(0), listener);
    Thread.sleep(4000);
    heartManSimulator.sendValue(666L, 1.0D);
    assertEquals(1.0D, listener.receivedValue, 0.1D);
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
