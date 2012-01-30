package at.jku.pervasive.ecg;

import java.nio.ByteBuffer;

public class ByteUtils {

  public static byte[] toByteArray(double value) {
    byte[] bytes = new byte[8];
    ByteBuffer.wrap(bytes).putDouble(value);
    return bytes;
  }

  public static double toDouble(byte[] bytes) {
    return ByteBuffer.wrap(bytes).getDouble();
  }

}
