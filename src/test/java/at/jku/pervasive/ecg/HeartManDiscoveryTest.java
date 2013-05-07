package at.jku.pervasive.ecg;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;

import junit.framework.TestCase;

import org.junit.Assert;

public class HeartManDiscoveryTest extends TestCase {

	private class TestThread extends Thread {

		private boolean error;

		public boolean isError() {
			return error;
		}

		public void setError(boolean error) {
			this.error = error;
		}

	}

	private HeartManDiscovery heartManDiscovery;

	private HeartManSimulator heartManSimulator;

	public void testPingDevice() throws IOException, URISyntaxException {
		String device = heartManSimulator.createFileDevice(getFile("/recording20s_sleep5ms_1.dat"));
		RemoteDevice remoteDevice = heartManDiscovery.pingDevice(device);
		assertNotNull(remoteDevice);
	}

	public void testPingNotExistingDevice() throws IOException {
		RemoteDevice remoteDevice = heartManDiscovery.pingDevice("0B1000000001");
		assertNull(remoteDevice);
	}

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

	public void testListeningOnMoreDevices() throws Exception {
		final String first = heartManSimulator.createFileDevice(getFile("/recording20s_sleep5ms_1.dat"));
		final String second = heartManSimulator.createFileDevice(getFile("/recording20s_sleep5ms_2.dat"));

		heartManDiscovery.discoverHeartManDevices();

		List<ServiceRecord> firstServices = heartManDiscovery.searchServices(first);
		assertNotNull(firstServices);
		assertTrue(firstServices.size() > 0);
		ServiceRecord firstServiceRecord = firstServices.get(0);

		List<ServiceRecord> secondServices = heartManDiscovery.searchServices(second);
		assertNotNull(secondServices);
		assertTrue(secondServices.size() > 0);
		ServiceRecord secondServiceRecord = secondServices.get(0);

		final Semaphore s1 = new Semaphore(0);
		final Semaphore s2 = new Semaphore(0);
		TestHeartManListener l = new TestHeartManListener() {
			@Override
			public void dataReceived(String address, long timestamp, double value) {
				System.out.printf("%1$s received %2$f\n", address, value);
				if (address.equals(first)) {
					s1.release();
				} else if (address.equals(second)) {
					s2.release();
				}
			}
		};

		heartManDiscovery.startListening(first, l, firstServiceRecord);
		heartManDiscovery.startListening(second, l, secondServiceRecord);

		s1.acquire();
		s2.acquire();
	}

	public void testRecording() throws Exception {
		String address = heartManSimulator.createDevice();
		List<HeartManDevice> devices = heartManDiscovery.discoverHeartManDevices();
		assertNotNull(devices);
		assertEquals(1, devices.size());

		heartManDiscovery.startRecording(address);

		final Semaphore s = new Semaphore(0);
		TestHeartManListener listener = new TestHeartManListener() {
			@Override
			public void dataReceived(String address, long timestamp, double value) {
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
			public void dataReceived(String address, long timestamp, double value) {
				super.dataReceived(address, timestamp, value);
				s.release();
			}
		};
		heartManDiscovery.startListening(address, listener);
		heartManSimulator.sendValue(address, 6.0D);
		s.acquireUninterruptibly();

		assertEquals(6.0D, listener.receivedValue, 0.01D);
		System.out.println("finished");
	}

	public void testStartListeningForInvalidAdress() throws Exception {
		try {
			heartManDiscovery.startListening((String) null, new TestHeartManListener());
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
			public void dataReceived(String address, long timestamp, double value) {
				super.dataReceived(address, timestamp, value);
				s.release(1);
			}
		};

		List<HeartManDevice> devices = heartManDiscovery.discoverHeartManDevices();
		assertNotNull(devices);
		assertEquals(2, devices.size());

		heartManDiscovery.startListening(first, listener);
		heartManDiscovery.startListening(second, listener);

		heartManSimulator.sendValue(first, 1.0D);
		s.acquire();

		assertTrue(listener.invoked);
		assertEquals(first, listener.address);
		assertEquals(1.0D, listener.receivedValue, 0.1D);
		assertEquals(0, s.availablePermits());

		listener.reset();
		heartManSimulator.sendValue(second, 2.0D);
		s.acquire();

		assertTrue(listener.invoked);
		assertEquals(second, listener.address);
		assertEquals(2.0D, listener.receivedValue, 0.1D);
		assertEquals(0, s.availablePermits());
	}

	public void testStartListeningForDeviceWhenAlreadyListeningOnOther() throws Exception {
		String first = heartManSimulator.createDevice();
		String second = heartManSimulator.createDevice();

		final Semaphore s = new Semaphore(0);
		TestHeartManListener listener = new TestHeartManListener() {
			@Override
			public void dataReceived(String address, long timestamp, double value) {
				super.dataReceived(address, timestamp, value);
				s.release(1);
			}
		};

		List<HeartManDevice> devices = heartManDiscovery.discoverHeartManDevices();
		assertNotNull(devices);
		assertEquals(2, devices.size());

		heartManDiscovery.startListening(first, listener);

		heartManSimulator.sendValue(first, 1.0D);
		s.acquire();

		assertTrue(listener.invoked);
		assertEquals(first, listener.address);
		assertEquals(1.0D, listener.receivedValue, 0.1D);
		assertEquals(0, s.availablePermits());

		heartManDiscovery.startListening(second, listener);

		listener.reset();
		heartManSimulator.sendValue(second, 2.0D);
		s.acquire();

		assertTrue(listener.invoked);
		assertEquals(second, listener.address);
		assertEquals(2.0D, listener.receivedValue, 0.1D);
		assertEquals(0, s.availablePermits());
	}

	public void testStartListeningWithMoreListeners() throws Exception {
		String address = heartManSimulator.createDevice();

		final Semaphore s = new Semaphore(0);
		TestHeartManListener l1 = new TestHeartManListener() {
			@Override
			public void dataReceived(String address, long timestamp, double value) {
				super.dataReceived(address, timestamp, value);
				s.release(1);
			}
		};
		TestHeartManListener l2 = new TestHeartManListener() {
			@Override
			public void dataReceived(String address, long timestamp, double value) {
				super.dataReceived(address, timestamp, value);
				s.release(1);
			}
		};

		heartManDiscovery.discoverHeartManDevices();

		heartManDiscovery.startListening(address, l1);
		heartManSimulator.sendValue(address, 1.0D);
		// wait until listener got invoked
		s.acquire(1);

		// l1 invoked - l2 not
		assertTrue(l1.invoked);
		assertEquals(1.0D, l1.receivedValue, 0.1D);
		assertFalse(l2.invoked);

		// reset
		l1.reset();
		l2.reset();

		// add second listener
		heartManDiscovery.startListening(address, l2);
		heartManSimulator.sendValue(address, 5.0D);
		// wait until both are invoked
		s.acquire(2);

		// both listeners invoked
		assertTrue(l1.invoked);
		assertEquals(5.0D, l1.receivedValue, 0.1D);
		assertTrue(l2.invoked);
		assertEquals(5.0D, l2.receivedValue, 0.1D);
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
			public void dataReceived(String address, long timestamp, double value) {
				super.dataReceived(address, timestamp, value);
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
		final Semaphore s = new Semaphore(-1);

		TestThread t1 = new TestThread() {
			@Override
			public void run() {
				List<HeartManDevice> firstList;
				try {
					firstList = heartManDiscovery.discoverHeartManDevices();
					Assert.assertNotNull(firstList);
					Assert.assertEquals(1, firstList.size());
				} catch (Exception e) {
					this.setError(true);
					e.printStackTrace();
				} finally {
					s.release();
				}
			}
		};

		TestThread t2 = new TestThread() {
			@Override
			public void run() {
				try {
					HeartManDiscovery secondDiscovery = HeartManDiscovery.getInstance();
					List<HeartManDevice> secondList = secondDiscovery.discoverHeartManDevices();
					Assert.assertNotNull(secondList);
					Assert.assertEquals(1, secondList.size());
				} catch (Exception e) {
					this.setError(true);
					e.printStackTrace();
				} finally {
					s.release();
				}
			}
		};

		t1.start();
		t2.start();

		t1.join();
		t2.join();

		s.acquire();

		assertFalse(t1.isError());
		assertFalse(t2.isError());
	}

	protected File getFile(String path) throws URISyntaxException {
		URL url = ECGMonitorMain.class.getResource(path);
		return new File(url.toURI());
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		heartManSimulator = new HeartManSimulator();

		heartManDiscovery = HeartManDiscovery.getInstance();
		assertTrue(heartManDiscovery.isBluetoothEnabled());
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		heartManDiscovery.tearDown();
		heartManDiscovery = null;

		heartManSimulator.stopServer();
		heartManSimulator = null;
	}

}
