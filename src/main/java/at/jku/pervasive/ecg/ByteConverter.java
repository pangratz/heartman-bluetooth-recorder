package at.jku.pervasive.ecg;

public class ByteConverter {
  public static short convertToShort(byte[] data) {
    return (short) ((data[0] << 8) | (data[1] & 0xff));
  }

  /**
   * Convenience method to convert a byte array to a hex string.
   * 
   * @param data
   *          the byte[] to convert
   * @return String the converted byte[]
   */
  public String bytesToHex(byte[] data) {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < data.length; i++) {
      buf.append(byteToHex(data[i]));
    }
    return buf.toString();
  }

  /**
   * Convenience method to convert a byte to a hex string.
   * 
   * @param data
   *          the byte to convert
   * @return String the converted byte
   */
  public String byteToHex(byte data) {
    StringBuffer buf = new StringBuffer();
    buf.append(toHexChar((data >>> 4) & 0x0F));
    buf.append(toHexChar(data & 0x0F));
    return buf.toString();
  }

  /**
   * Convenience method to convert an int to a hex char.
   * 
   * @param i
   *          the int to convert
   * @return char the converted char
   */
  public char toHexChar(int i) {
    if ((0 <= i) && (i <= 9)) {
      return (char) ('0' + i);
    } else {
      return (char) ('a' + (i - 10));
    }
  }

}
