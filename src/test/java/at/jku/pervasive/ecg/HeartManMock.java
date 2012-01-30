package at.jku.pervasive.ecg;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.Semaphore;

import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import org.apache.commons.io.IOUtils;

public class HeartManMock implements Runnable {

  private boolean isRunning;
  private final Semaphore lock = new Semaphore(0);
  private final String name;
  private double nextValue;
  private final UUID uuid;

  public HeartManMock(long uuidValue) {
    this(uuidValue, "HeartMan" + uuidValue);
  }

  public HeartManMock(long uuidValue, String name) {
    super();
    this.uuid = new UUID(uuidValue);
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public UUID getUUID() {
    return uuid;
  }

  @Override
  public final void run() {
    this.isRunning = true;

    System.out.println("started HeartManMock");

    DataOutputStream dos = null;
    StreamConnectionNotifier service = null;

    String url = "btspp://localhost:" + uuid.toString() + ";name=" + name;
    try {
      service = (StreamConnectionNotifier) Connector.open(url);
      StreamConnection connection = service.acceptAndOpen();
      dos = connection.openDataOutputStream();

      System.out.println("isRunning: " + isRunning);

      while (isRunning) {
        System.out.println("waiting for lock to release");
        try {
          lock.acquire();
          System.out.println("lock released and got value " + nextValue);
          dos.writeDouble(nextValue);
        } catch (InterruptedException e) {
        }
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
      IOUtils.closeQuietly(dos);
      if (service != null) {
        try {
          service.close();
        } catch (IOException e) {
        }
      }
    }
  }

  public void sendValue(double value) {
    System.out.println("send value");
    this.nextValue = value;
    lock.release();
  }

  public void stop() {
    System.out.println("stopping this thing!");
    this.isRunning = false;
    lock.release();
  }

}
