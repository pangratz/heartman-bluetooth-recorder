package at.jku.pervasive.ecg;

import javax.swing.JFrame;

public class ECGMonitor extends JFrame {

  public static void main(String[] args) {
    new ECGMonitor().setVisible(true);
  }

  public ECGMonitor() {
    super();

    setDefaultCloseOperation(EXIT_ON_CLOSE);
  }

}
