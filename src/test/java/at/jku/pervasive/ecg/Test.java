package at.jku.pervasive.ecg;

import java.io.File;
import java.util.List;

import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;

public class Test {

  public static void main(String[] args) throws Exception {
    HeartManSimulator simulator = new HeartManSimulator();
    String address = simulator.createFileDevice(new File(
        "1024byte_recording.dat"));
    HeartManDiscovery heartManDiscovery = new HeartManDiscovery();
    // heartManDiscovery.discoverHeartManDevices();

    String heartman = "00A096203DCB"; // C102
    // heartman = "00A096203DD1"; // C157
    RemoteDevice remoteDevice = heartManDiscovery.pingDevice(address);
    List<ServiceRecord> services = heartManDiscovery
        .searchServices(remoteDevice);
    ServiceRecord serviceRecord = services.get(0);

    ECGMonitor monitor = new ECGMonitor();
    monitor.setVisible(true);

    heartManDiscovery.startListening(address, monitor.getHeartManListener());
  }
}
