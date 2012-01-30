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

  private final List<HeartManListener> listeners;
  private final ServiceRecord serviceRecord;
  private final Object stackId;

  public ListeningTask(Object stackId, ServiceRecord serviceRecord) {
    super();
    this.stackId = stackId;
    this.serviceRecord = serviceRecord;

    listeners = new ArrayList<HeartManListener>(1);
  }

  public void addListener(HeartManListener listener) {
    if (listener != null) {
      this.listeners.add(listener);
    }
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
        for (HeartManListener listener : listeners) {
          listener.dataReceived(value);
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
