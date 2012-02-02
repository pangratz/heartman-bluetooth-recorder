package at.jku.pervasive.ecg;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ByteRecorder implements IByteListener {

  private boolean closed;
  private final OutputStream out;

  public ByteRecorder(File output) throws IOException {
    super();

    out = new BufferedOutputStream(new FileOutputStream(output), 81920);
  }

  @Override
  public void bytesReceived(byte[] data) {
    if (!closed) {
      try {
        out.write(data);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void close() {
    try {
      out.flush();
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    closed = true;
  }

}
