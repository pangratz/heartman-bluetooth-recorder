package at.jku.pervasive.ecg;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.bluetooth.RemoteDevice;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import org.apache.commons.io.IOUtils;

public class FileHeartManMock extends HeartManMock {

  private final File dataFile;

  public FileHeartManMock(File dataFile) {
    super();
    this.dataFile = dataFile;
  }

  @Override
  public void run() {
    InputStream in = null;
    OutputStream dos = null;
    try {
      in = new BufferedInputStream(new FileInputStream(dataFile));
      StreamConnectionNotifier service = null;

      String url = "btspp://localhost:"
          + HeartManDiscovery.HEARTMAN_SERVICE_UUID
          + ";authenticate=false;encrypt=false";

      service = (StreamConnectionNotifier) Connector.open(url);
      System.out.println("Waiting for incoming connection...");
      StreamConnection connection = service.acceptAndOpen();

      RemoteDevice client = RemoteDevice.getRemoteDevice(connection);
      String clientFriendlyName = client.getFriendlyName(true);
      System.out.println("connected to " + clientFriendlyName);

      dos = connection.openOutputStream();

      byte[] buffer = new byte[128];
      int len = -1;
      while ((len = in.read(buffer)) != -1) {
        dos.write(buffer, 0, len);
      }

      dos.close();
      connection.close();
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