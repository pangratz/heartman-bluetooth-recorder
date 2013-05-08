package at.jku.pervasive.ecg;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.bluetooth.RemoteDevice;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import org.apache.commons.io.IOUtils;

public class RandomHeartManMock extends HeartManMock {

  @Override
  public void run() {
    InputStream in = null;
    OutputStream dos = null;
    try {
      StreamConnectionNotifier service = null;

      String url = "btspp://localhost:" + HeartManDiscovery.HEARTMAN_SERVICE_UUID + ";authenticate=false;encrypt=false";

      service = (StreamConnectionNotifier) Connector.open(url);
      System.out.println("Waiting for incoming connection...");
      StreamConnection connection = service.acceptAndOpen();

      RemoteDevice client = RemoteDevice.getRemoteDevice(connection);
      String clientFriendlyName = client.getFriendlyName(true);
      System.out.println("connected to " + clientFriendlyName);

      dos = connection.openOutputStream();

      // in = new ByteArrayInputStream(data.toByteArray());
      while (true) {
        // number between [2.7; -0.8]
        double value = Math.random() * 3.5 - 0.8;
        byte[] buffer = HeartManInputStream.caluclateByteValue(value / HeartManInputStream.MAGIC_NUMBER);
        IOUtils.write(buffer, dos);
      }

      // System.out.println("already over :(");
      // connection.close();
    } catch (IOException io) {
      // silent
      System.err.println(io.getMessage());
    } catch (Exception e) {
      System.out.println("uweeh");
      e.printStackTrace();
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(dos);
    }
  }
}
