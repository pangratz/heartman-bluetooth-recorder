package at.jku.pervasive.ecg;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

public class HeartManMock implements Runnable {

  private boolean isRunning;
  private final String name;
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

  public final void run() {
    this.isRunning = true;

    System.out.println("started HeartManMock");

    StreamConnectionNotifier service = null;
    String url = "btspp://localhost:" + uuid.toString() + ";name=" + name;
    try {
      service = (StreamConnectionNotifier) Connector.open(url);
      StreamConnection conn = service.acceptAndOpen();

      DataOutputStream dos = conn.openDataOutputStream();
      DataInputStream dis = conn.openDataInputStream();

      while (this.isRunning) {
        System.out.println("sending stuff");
        dos.writeUTF("hello");
        Thread.sleep(100);
      }

      dos.close();
      dis.close();

      conn.close();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (service != null) {
        try {
          service.close();
        } catch (IOException e) {
        }
      }
    }
  }

  public void stop() {
    this.isRunning = false;
  }

}
