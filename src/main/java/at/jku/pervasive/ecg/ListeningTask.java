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

  // how many data points shall be fetched per read of ECG input stream?
  public final static int DATA_POINTS_PER_READ = 50;

  private final String address;
  private final List<IByteListener> byteListeners;
  private final List<IHeartManListener> listeners;
  private final ServiceRecord serviceRecord;
  private final Object stackId;
  private final long updateRate;

  public ListeningTask(Object stackId, long updateRate, ServiceRecord serviceRecord) {
    super();
    this.stackId = stackId;
    this.updateRate = updateRate;
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
    InputStream in = null;
    StreamConnection conn = null;

    try {

      BlueCoveImpl.setDefaultThreadBluetoothStackID(stackId);

      int security = ServiceRecord.NOAUTHENTICATE_NOENCRYPT;
      RemoteDevice host = serviceRecord.getHostDevice();
      RemoteDeviceHelper.authenticate(host, "Heartman");

      String url = serviceRecord.getConnectionURL(security, false);

      conn = (StreamConnection) Connector.open(url, Connector.READ);
      in = conn.openInputStream();
      System.out.println("opened InputStream " + url);

      // 2 bytes per data point
      byte[] buffer = new byte[DATA_POINTS_PER_READ * 2];
      while (!isInterrupted()) {
        double ecgValue;
        int read = in.read(buffer);
        int readDataPoints = read / 2;

        // calculate timestamp of first data point, which is
        // readDataPoints in
        // the past
        long timestamp = System.currentTimeMillis() - readDataPoints * updateRate;

        for (int i = 0; i < readDataPoints; i++) {
          ecgValue = HeartManInputStream.calculateECGValue(new byte[] { buffer[2 * i], buffer[2 * i + 1] }, true);
          for (IHeartManListener l : listeners) {
            l.dataReceived(address, timestamp + i * updateRate, ecgValue);
          }
          for (IByteListener bL : byteListeners) {
            bL.bytesReceived(buffer);
          }
        }

        try {
          Thread.sleep(updateRate * DATA_POINTS_PER_READ);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      in.close();
    } catch (BluetoothStateException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (in != null) {
        IOUtils.closeQuietly(in);
      }
      if (conn != null) {
        try {
          conn.close();
        } catch (Exception e) {
        }
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
