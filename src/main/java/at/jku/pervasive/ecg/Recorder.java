package at.jku.pervasive.ecg;

import java.util.LinkedList;
import java.util.List;

public class Recorder implements IHeartManListener {

  private final List<Double> recordings;

  public Recorder() {
    super();

    this.recordings = new LinkedList<Double>();
  }

  @Override
  public void dataReceived(String address, double value) {
    this.recordings.add(value);
  }

  public List<Double> getRecordings() {
    return recordings;
  }

}
