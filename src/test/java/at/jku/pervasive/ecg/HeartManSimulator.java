package at.jku.pervasive.ecg;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.bluetooth.BluetoothStateException;

import com.intel.bluetooth.EmulatorTestsHelper;

/**
 * Idea taken from
 * http://bluecove.org/bluecove-emu/xref-test/net/sf/bluecove/ExampleTest.html
 */
public class HeartManSimulator {

  private final Map<Long, HeartManMock> mocks;
  private final List<Thread> serverThreads;

  public HeartManSimulator() throws BluetoothStateException {
    super();

    serverThreads = new LinkedList<Thread>();
    mocks = new HashMap<Long, HeartManMock>(1);

    EmulatorTestsHelper.startInProcessServer();
    EmulatorTestsHelper.useThreadLocalEmulator();
  }

  public void sendValue(long uuid, double value) {
    HeartManMock mock = mocks.get(uuid);
    if (mock != null) {
      mock.sendValue(value);
    }
  }

  public String startDevice(long uuid) throws BluetoothStateException {
    return startServer(uuid, new HeartManMock(uuid));
  }

  public String startServer(long uuid, HeartManMock mock)
      throws BluetoothStateException {
    mocks.put(uuid, mock);
    Thread t = EmulatorTestsHelper.runNewEmulatorStack(mock);
    serverThreads.add(t);

    String mockName = mock.getName();
    try {
      List<HeartManDevice> devices = new HeartManDiscovery()
          .discoverHeartManDevices();
      for (HeartManDevice device : devices) {
        if (mockName.equals(device.getName())) {
          return device.getDevice().getBluetoothAddress();
        }
      }
    } catch (Exception e) {
    }

    return null;
  }

  public void stopServer() throws InterruptedException {
    for (long uuid : mocks.keySet()) {
      mocks.get(uuid).stop();
    }
    for (Thread serverThread : serverThreads) {
      if ((serverThread != null) && (serverThread.isAlive())) {
        serverThread.interrupt();
        serverThread.join();
      }
    }
    EmulatorTestsHelper.stopInProcessServer();
  }

}
