package at.jku.pervasive.ecg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.bluetooth.UUID;
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

  private class ClientConnectionThread extends Thread {

    private StreamConnection conn;

    private ClientConnectionThread(StreamConnection conn) {
      super("ClientConnectionThread");
      this.conn = conn;
    }

    public void run() {
      try {
        InputStream in;
        OutputStream dos = this.conn.openOutputStream();

        ByteArrayOutputStream data = new ByteArrayOutputStream();
        IOUtils.copy(new FileInputStream(dataFile), data);

        in = new ByteArrayInputStream(data.toByteArray());
        while (in != null) {

          byte[] buffer = new byte[2];
          while (in.read(buffer) != -1) {
            IOUtils.write(buffer, dos);
          }

          in = new ByteArrayInputStream(data.toByteArray());
        }

        dos.close();
      } catch (Throwable e) {
        // System.err.print(e.toString());
        // e.printStackTrace();
      } finally {
        if (conn != null) {
          try {
            conn.close();
          } catch (IOException ignore) {
          }
        }
      }
    }
  }

  @Override
  public void run() {
    InputStream in = null;
    OutputStream dos = null;
    try {
      StreamConnectionNotifier service = null;

      UUID uuid = new UUID(Math.round(Math.random() * 100));
      String url = "btspp://localhost:" + uuid + ";authenticate=false;encrypt=false;name=LeHeartman";

      service = (StreamConnectionNotifier) Connector.open(url, Connector.READ_WRITE);
      System.out.println("Waiting for incoming connection...");
      StreamConnection connection = service.acceptAndOpen();

      ClientConnectionThread cct = new ClientConnectionThread(connection);
      cct.setDaemon(true);
      cct.start();
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
