package at.jku.pervasive.ecg;

import java.io.ByteArrayInputStream;

import junit.framework.TestCase;

import org.apache.commons.codec.binary.Hex;

public class ConvertTest extends TestCase {

  public void testConvertHexToFloat() throws Exception {
    byte[] data = new byte[] { (byte) 254, (byte) 112 };
    String encodeHexString = Hex.encodeHexString(data);
    System.out.println(encodeHexString);
    short value = Integer.valueOf(encodeHexString, 16).shortValue();
    assertEquals(-400, value);

    // http://docs.oracle.com/javase/1.5.0/docs/api/java/io/DataInput.html#readShort()
    byte a = data[0], b = data[1];
    short secondValue = (short) ((a << 8) | (b & 0xff));
    System.out.println(secondValue);
  }

  public void testHeartManInputStream() throws Exception {
    byte[] data = new byte[] { (byte) 254, (byte) 112 };
    ByteArrayInputStream bais = new ByteArrayInputStream(data);

    HeartManInputStream heartManInputStream = new HeartManInputStream(bais);
    double value = heartManInputStream.nextECGValue();
    assertEquals(-400, value, 0.001D);
  }

  public void testHeartManInputStreamMV() throws Exception {
    byte[] data = new byte[] { (byte) 254, (byte) 112 };
    ByteArrayInputStream bais = new ByteArrayInputStream(data);

    HeartManInputStream heartManInputStream = new HeartManInputStream(bais);
    double value = heartManInputStream.nextECGValue(true);
    assertEquals(-0.083, value, 0.001D);
  }

}
