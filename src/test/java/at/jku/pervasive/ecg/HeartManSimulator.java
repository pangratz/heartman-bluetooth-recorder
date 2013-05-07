package at.jku.pervasive.ecg;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.bluetooth.BluetoothStateException;

import com.intel.bluetooth.EmulatorTestsHelper;

/**
 * Idea taken from
 * http://bluecove.org/bluecove-emu/xref-test/net/sf/bluecove/ExampleTest.html
 */
public class HeartManSimulator {

	private final String baseAddress = "0B100000000%1$s";
	private final AtomicInteger count = new AtomicInteger(0);
	private final Map<String, HeartManMock> mocks;
	private final List<Thread> serverThreads;

	public HeartManSimulator() throws BluetoothStateException {
		super();

		serverThreads = new LinkedList<Thread>();
		mocks = new HashMap<String, HeartManMock>(1);

		EmulatorTestsHelper.startInProcessServer();
		EmulatorTestsHelper.useThreadLocalEmulator();
	}

	public String createDevice() throws BluetoothStateException {
		String address = String.format(baseAddress, count.incrementAndGet());

		HeartManMock mock = new HeartManMock();
		mocks.put(address, mock);
		Thread t = EmulatorTestsHelper.runNewEmulatorStack(mock);
		serverThreads.add(t);

		return address;
	}

	public String createFileDevice(File file) throws BluetoothStateException {
		String address = String.format(baseAddress, count.incrementAndGet());

		HeartManMock mock = new FileHeartManMock(file);
		mocks.put(address, mock);
		Thread t = EmulatorTestsHelper.runNewEmulatorStack(mock);
		serverThreads.add(t);

		return address;
	}

	public void sendValue(String address, double value) {
		HeartManMock mock = mocks.get(address);
		if (mock != null) {
			mock.sendValue(value);
		}
	}

	public void stopServer() throws InterruptedException {
		for (HeartManMock mock : mocks.values()) {
			mock.stop();
		}
		for (Thread serverThread : serverThreads) {
			if ((serverThread != null) && (serverThread.isAlive())) {
				serverThread.interrupt();
				serverThread.join();
			}
		}
		EmulatorTestsHelper.stopInProcessServer();
	}

	public String createRandomDevice() throws BluetoothStateException {
		String address = String.format(baseAddress, count.incrementAndGet());

		HeartManMock mock = new RandomHeartManMock();
		mocks.put(address, mock);
		Thread t = EmulatorTestsHelper.runNewEmulatorStack(mock);
		serverThreads.add(t);

		return address;
	}

}
