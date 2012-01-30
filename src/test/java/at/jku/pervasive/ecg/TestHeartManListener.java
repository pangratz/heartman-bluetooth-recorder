package at.jku.pervasive.ecg;

public class TestHeartManListener implements HeartManListener {

  public boolean invoked = false;
  public double receivedValue = -1.0D;

  @Override
  public void dataReceived(double value) {
    this.receivedValue = value;
    this.invoked = true;
  }

  public void reset() {
    this.receivedValue = -1.0D;
    this.invoked = false;
  }

}
