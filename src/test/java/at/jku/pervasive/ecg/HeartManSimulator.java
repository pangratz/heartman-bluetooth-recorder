package at.jku.pervasive.ecg;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.UUID;

import com.intel.bluetooth.EmulatorTestsHelper;

/**
 * Idea taken from
 * http://bluecove.org/bluecove-emu/xref-test/net/sf/bluecove/ExampleTest.html
 */
public class HeartManSimulator {

  private final Map<UUID, HeartManMock> mocks;
  private final List<Thread> serverThreads;

  public HeartManSimulator() throws BluetoothStateException {
    super();

    serverThreads = new LinkedList<Thread>();
    mocks = new HashMap<UUID, HeartManMock>(1);

    EmulatorTestsHelper.startInProcessServer();
    EmulatorTestsHelper.useThreadLocalEmulator();
  }

  public void sendValue(long uuid, double value) {
    HeartManMock mock = mocks.get(new UUID(uuid));
    if (mock != null) {
      mock.sendValue(value);
    }
  }

  public void startDevice(long uuid) throws BluetoothStateException {
    this.startServer(new HeartManMock(uuid));
  }

  public void startServer(HeartManMock mock) throws BluetoothStateException {
    mocks.put(mock.getUUID(), mock);
    Thread t = EmulatorTestsHelper.runNewEmulatorStack(mock);
    serverThreads.add(t);
  }

  public void stopServer() throws InterruptedException {
    for (UUID uuid : mocks.keySet()) {
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
