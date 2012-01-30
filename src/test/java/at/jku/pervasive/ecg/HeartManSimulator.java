package at.jku.pervasive.ecg;

import java.util.LinkedList;
import java.util.List;

import javax.bluetooth.BluetoothStateException;

import com.intel.bluetooth.EmulatorTestsHelper;

/**
 * Idea taken from
 * http://bluecove.org/bluecove-emu/xref-test/net/sf/bluecove/ExampleTest.html
 */
public class HeartManSimulator {

  private final List<Thread> serverThreads;

  public HeartManSimulator() throws BluetoothStateException {
    super();

    serverThreads = new LinkedList<Thread>();

    EmulatorTestsHelper.startInProcessServer();
    EmulatorTestsHelper.useThreadLocalEmulator();
  }

  public void startDevice(long uuid) throws BluetoothStateException {
    this.startServer(new HeartManMock(uuid));
  }

  public void startServer(HeartManMock mock) throws BluetoothStateException {
    Thread t = EmulatorTestsHelper.runNewEmulatorStack(mock);
    serverThreads.add(t);
  }

  public void stopServer() throws InterruptedException {
    for (Thread serverThread : serverThreads) {
      if ((serverThread != null) && (serverThread.isAlive())) {
        serverThread.interrupt();
        serverThread.join();
      }
    }
    EmulatorTestsHelper.stopInProcessServer();
  }

}
