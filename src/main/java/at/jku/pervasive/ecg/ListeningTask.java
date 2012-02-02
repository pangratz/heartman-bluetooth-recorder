package at.jku.pervasive.ecg;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
  private final List<IByteListener> byteListeners;
  private final List<IHeartManListener> listeners;
  private final ServiceRecord serviceRecord;
  private final Object stackId;

  public ListeningTask(Object stackId, ServiceRecord serviceRecord) {
    super();
    this.stackId = stackId;
    this.serviceRecord = serviceRecord;
    this.address = serviceRecord.getHostDevice().getBluetoothAddress();

    listeners = new ArrayList<IHeartManListener>(1);
    byteListeners = new ArrayList<IByteListener>(1);
  }

  public void addListener(IByteListener byteListener) {
    if (byteListener != null) {
      this.byteListeners.add(byteListener);
    }

  }

  public void addListener(IHeartManListener listener) {
    if (listener != null) {
      this.listeners.add(listener);
    }
  }

  public void clearListener() {
    this.listeners.clear();
    this.byteListeners.clear();
  }

  public void removeListener(IHeartManListener listener) {
    if (listener != null) {
      this.listeners.remove(listener);
    }
  }

  @Override
  public void run() {
    HeartManInputStream is = null;
    StreamConnection conn = null;

    try {

      BlueCoveImpl.setThreadBluetoothStackID(stackId);

      int security = ServiceRecord.NOAUTHENTICATE_NOENCRYPT;
      RemoteDevice host = serviceRecord.getHostDevice();
      RemoteDeviceHelper.authenticate(host, "Heartman");

      String url = serviceRecord.getConnectionURL(security, false);

      InputStream inStream = Connector.openInputStream(url);
      is = new HeartManInputStream(inStream);

      System.out.println("opened DataInputStream");
      double ecgValue;
      long now;
      while (!isInterrupted()) {
        now = System.currentTimeMillis();
        ecgValue = is.nextECGValue(true);
        for (IHeartManListener l : listeners) {
          l.dataReceived(address, now, ecgValue);
        }
        try {
          Thread.sleep(5);
        } catch (Exception e) {
          e.printStackTrace();
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

  protected void dataReceived(long timestamp, byte[] data) {
    short valueFromEcg = (short) ((data[0] << 8) | (data[1] & 0xff));
    double value = HeartManInputStream.MAGIC_NUMBER * valueFromEcg;

    for (IByteListener byteListener : byteListeners) {
      byteListener.bytesReceived(data);
    }

    for (IHeartManListener listener : listeners) {
      listener.dataReceived(address, timestamp, value);
    }
  }
}
