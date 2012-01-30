package at.jku.pervasive.ecg;

import javax.bluetooth.RemoteDevice;

public class HeartManDevice {

  private final RemoteDevice device;
  private final String name;

  public HeartManDevice(String name, RemoteDevice device) {
    super();

    this.name = name;
    this.device = device;
  }

  public RemoteDevice getDevice() {
    return device;
  }

  public String getName() {
    return name;
  }

}
