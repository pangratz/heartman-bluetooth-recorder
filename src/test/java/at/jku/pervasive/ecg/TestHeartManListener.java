package at.jku.pervasive.ecg;

public class TestHeartManListener implements HeartManListener {

  public double receivedValue = -1.0D;

  public void dataReceived(double value) {
    this.receivedValue = value;
  }

}
