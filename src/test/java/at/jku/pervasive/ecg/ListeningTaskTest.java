package at.jku.pervasive.ecg;

import java.util.List;

import javax.bluetooth.ServiceRecord;

import junit.framework.TestCase;

import com.intel.bluetooth.BlueCoveImpl;

public class ListeningTaskTest extends TestCase {

  private HeartManSimulator heartManSimulator;
  private ServiceRecord serviceRecord;
  private Object stackId;

  public void testAddListener() throws Exception {
    ListeningTask listeningTask = new ListeningTask(stackId, 10, serviceRecord);

    listeningTask.addListener(new TestHeartManListener());
  }

  public void testAddNullListener() throws Exception {
    ListeningTask listeningTask = new ListeningTask(stackId, 10, serviceRecord);

    try {
      listeningTask.addListener((IHeartManListener) null);
    } catch (Exception e) {
      fail("adding a null listener should not throw an excpetion");
    }
  }

  public void testRemoveListener() throws Exception {
    ListeningTask listeningTask = new ListeningTask(stackId, 10, serviceRecord);

    TestHeartManListener testHeartManListener = new TestHeartManListener();
    listeningTask.addListener(testHeartManListener);
    try {
      listeningTask.removeListener(testHeartManListener);
    } catch (Exception e) {
      fail("removing a listener should not throw an exception");
    }
  }

  public void testRemoveListenerTwice() throws Exception {
    ListeningTask listeningTask = new ListeningTask(stackId, 10, serviceRecord);

    TestHeartManListener testHeartManListener = new TestHeartManListener();
    listeningTask.addListener(testHeartManListener);
    try {
      listeningTask.removeListener(testHeartManListener);
      listeningTask.removeListener(testHeartManListener);
    } catch (Exception e) {
      fail("removing a listener twice should not throw an exception");
    }
  }

  public void testRemoveListenerWhichHasNotBeenAdded() throws Exception {
    ListeningTask listeningTask = new ListeningTask(stackId, 10, serviceRecord);

    TestHeartManListener testHeartManListener = new TestHeartManListener();
    try {
      listeningTask.removeListener(testHeartManListener);
    } catch (Exception e) {
      fail("removing a listener which has not been added should not throw an exception");
    }
  }

  public void testRemoveNullListener() throws Exception {
    ListeningTask listeningTask = new ListeningTask(stackId, 10, serviceRecord);

    try {
      listeningTask.removeListener(null);
    } catch (Exception e) {
      fail("removing a null listener should not throw an exception");
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    heartManSimulator = new HeartManSimulator();
    HeartManDiscovery heartManDiscovery = new HeartManDiscovery();

    String address = heartManSimulator.createDevice();
    heartManDiscovery.discoverHeartManDevices();
    List<ServiceRecord> services = heartManDiscovery.searchServices(address);
    serviceRecord = services.get(0);

    stackId = BlueCoveImpl.getThreadBluetoothStackID();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();

    heartManSimulator.stopServer();
  }

}
