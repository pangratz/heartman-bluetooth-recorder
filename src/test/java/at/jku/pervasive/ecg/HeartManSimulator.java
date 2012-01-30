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

  public void startDevice(long uuid) throws BluetoothStateException {
    this.startServer(uuid, new HeartManMock(uuid));
  }

  public void startServer(long uuid, HeartManMock mock)
      throws BluetoothStateException {
    mocks.put(uuid, mock);
    Thread t = EmulatorTestsHelper.runNewEmulatorStack(mock);
    serverThreads.add(t);
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
