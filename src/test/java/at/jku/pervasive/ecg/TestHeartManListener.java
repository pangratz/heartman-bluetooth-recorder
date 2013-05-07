package at.jku.pervasive.ecg;

public class TestHeartManListener implements IHeartManListener {

	public String address = null;
	public boolean invoked = false;
	public double receivedValue = -1.0D;

	@Override
	public void dataReceived(String address, long timestamp, double value) {
		this.invoked = true;
		this.address = address;
		this.receivedValue = value;
	}

	public void reset() {
		this.receivedValue = -1.0D;
		this.invoked = false;
		this.address = null;
	}

}
