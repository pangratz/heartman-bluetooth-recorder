package at.jku.pervasive.ecg;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;

import org.apache.commons.io.IOUtils;

public class ByteRecorder implements IByteListener {

	public static void main(String[] args) throws Exception {
		HeartManDiscovery discovery = HeartManDiscovery.getInstance();

		String heartman = "00A096203DCB"; // C102
		RemoteDevice remoteDevice = discovery.pingDevice(heartman);
		List<ServiceRecord> services = discovery.searchServices(remoteDevice);
		ServiceRecord serviceRecord = services.get(0);

		File output = new File("recording20s_sleep50ms_2.dat");
		ByteRecorder recorder = new ByteRecorder(output);

		discovery.startListening(heartman, recorder, serviceRecord);
		Thread.sleep(TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS));
		discovery.stopListening(heartman);
		recorder.close();
		System.exit(0);
	}

	private boolean closed;
	private final BufferedOutputStream out;

	public ByteRecorder(File output) throws IOException {
		super();

		FileOutputStream fos = new FileOutputStream(output);
		out = new BufferedOutputStream(fos);
	}

	@Override
	public void bytesReceived(byte[] data) {
		if (!closed) {
			try {
				IOUtils.write(data, out);
			} catch (IOException e) {
				e.printStackTrace();
				IOUtils.closeQuietly(out);
			}
		}
	}

	public void close() {
		try {
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		closed = true;
	}

}
