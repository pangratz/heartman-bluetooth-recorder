package at.jku.pervasive.ecg;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import org.apache.commons.io.IOUtils;

import com.intel.bluetooth.BlueCoveImpl;
import com.intel.bluetooth.RemoteDeviceHelper;

public class ListeningTask extends Thread {

  private final String address;
  private final List<IHeartManListener> listeners;
  private final ServiceRecord serviceRecord;
  private final Object stackId;

  public ListeningTask(Object stackId, ServiceRecord serviceRecord) {
    super();
    this.stackId = stackId;
    this.serviceRecord = serviceRecord;
    this.address = serviceRecord.getHostDevice().getBluetoothAddress();

    listeners = new ArrayList<IHeartManListener>(1);
  }

  public void addListener(IHeartManListener listener) {
    if (listener != null) {
      this.listeners.add(listener);
    }
  }

  public void clearListener() {
    this.listeners.clear();
  }

  public void removeListener(IHeartManListener listener) {
    if (listener != null) {
      this.listeners.remove(listener);
    }
  }

  @Override
  public void run() {
    InputStream is = null;
    StreamConnection conn = null;

    try {

      BlueCoveImpl.setThreadBluetoothStackID(stackId);

      int security = ServiceRecord.NOAUTHENTICATE_NOENCRYPT;
      RemoteDevice host = serviceRecord.getHostDevice();
      boolean authenticate = RemoteDeviceHelper.authenticate(host, "Heartman");
      System.out.println("authenticate: " + authenticate);

      String url = serviceRecord.getConnectionURL(security, false);
      // InputStream openInputStream = Connector.openInputStream(url);

      is = Connector.openInputStream(url);

      System.out.println("opened DataInputStream");

      byte[] buff = new byte[256];
      while (!isInterrupted()) {
        is.read(buff);
        System.out.println(Arrays.toString(buff));
        double value = ByteBuffer.wrap(buff).getDouble();
        for (IHeartManListener listener : listeners) {
          listener.dataReceived(this.address, value);
        }
      }

      is.close();
    } catch (BluetoothStateException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      IOUtils.closeQuietly(is);
      try {
        if (conn != null) {
          conn.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
