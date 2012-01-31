package at.jku.pervasive.ecg;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.ServiceRecord;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import org.apache.commons.io.IOUtils;

import com.intel.bluetooth.BlueCoveImpl;

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

  @Override
  public void run() {
    DataInputStream dis = null;
    StreamConnection conn = null;

    try {

      BlueCoveImpl.setThreadBluetoothStackID(stackId);

      int security = ServiceRecord.NOAUTHENTICATE_NOENCRYPT;
      String url = serviceRecord.getConnectionURL(security, false);
      dis = Connector.openDataInputStream(url);

      while (!isInterrupted()) {
        double value = dis.readDouble();
        for (IHeartManListener listener : listeners) {
          listener.dataReceived(this.address, value);
        }
      }

      dis.close();
    } catch (BluetoothStateException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      IOUtils.closeQuietly(dis);
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
