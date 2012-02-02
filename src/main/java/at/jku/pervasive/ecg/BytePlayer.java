package at.jku.pervasive.ecg;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class BytePlayer extends Thread {

  private final File input;
  private IHeartManListener listener;

  public BytePlayer(File input) {
    this.input = input;
  }

  public void play(IHeartManListener listener) {
    this.listener = listener;
    this.start();
  }

  @Override
  public void run() {
    try {
      InputStream in = new BufferedInputStream(new FileInputStream(input));
      byte[] buffer = new byte[2];
      while (in.read(buffer) != -1) {
        short value = ByteConverter.convertToShort(buffer);
        listener.dataReceived("null", System.currentTimeMillis(), value);
      }
      System.out.println("finished");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
