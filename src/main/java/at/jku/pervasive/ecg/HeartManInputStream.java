package at.jku.pervasive.ecg;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class HeartManInputStream extends BufferedInputStream {

  public static double MAGIC_NUMBER = 0.00020926D;
  private final byte[] buffer = new byte[2];

  public HeartManInputStream(InputStream in) {
    super(in);
  }

  public HeartManInputStream(InputStream in, int size) {
    super(in, size);
  }

  public double nextECGValue() throws IOException {
    return nextECGValue(false);
  }

  public double nextECGValue(boolean mV) throws IOException {
    read(buffer);
    short value = (short) ((buffer[0] << 8) | (buffer[1] & 0xff));
    if (mV) {
      return MAGIC_NUMBER * value;
    }
    return value;
  }

}
