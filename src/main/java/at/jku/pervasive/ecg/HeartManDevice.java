package at.jku.pervasive.ecg;

public class HeartManDevice {

  private final String name;
  private String address;

  public HeartManDevice(String address, String name) {
    super();

    this.address = address;
    this.name = name;
  }

  public String getAddress() {
    return address;
  }

  public String getName() {
    return name;
  }

}
