package at.jku.pervasive.ecg;

public interface IHeartManListener {

	public void dataReceived(String address, long timestamp, double value);

}
