package at.jku.pervasive.ecg;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

public class ECGMonitor extends JFrame {
  private class ListenForUpdates extends Thread implements IHeartManListener {

    private final BlockingQueue<Double> buffer;
    private final TimeSeries series;

    public ListenForUpdates(TimeSeries series) {
      super();
      this.series = series;
      this.buffer = new LinkedBlockingQueue<Double>();
    }

    @Override
    public void dataReceived(String address, double value) {
      this.buffer.add(value);
    }

    @Override
    public void run() {
      while (!isInterrupted()) {
        try {
          Double value = this.buffer.take();
          this.series.addOrUpdate(new Millisecond(), value);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static void main(String[] args) throws Exception {
    HeartManDiscovery heartManDiscovery = new HeartManDiscovery();
    heartManDiscovery.discoverHeartManDevices();

    String heartman = "00A096203DCB";

    ECGMonitor monitor = new ECGMonitor();
    monitor.setVisible(true);

    heartManDiscovery.startListening(heartman, monitor.getHeartManListener());
  }

  private final ListenForUpdates listenForUpdates;

  public ECGMonitor() {
    super();

    setDefaultCloseOperation(EXIT_ON_CLOSE);

    TimeSeries timeSeries = new TimeSeries("ecg");
    timeSeries.setMaximumItemAge(TimeUnit.SECONDS.toMillis(10));

    XYDataset dataset = new TimeSeriesCollection(timeSeries);
    JFreeChart chart = ChartFactory.createTimeSeriesChart("ecg", "time", "V",
        dataset, false, false, false);

    XYPlot xyPlot = (XYPlot) chart.getPlot();
    ValueMarker baselineMarker = new ValueMarker(0.0D, Color.BLACK,
        new BasicStroke());
    xyPlot.addRangeMarker(baselineMarker);

    ChartPanel chartPanel = new ChartPanel(chart);
    add(chartPanel);

    listenForUpdates = new ListenForUpdates(timeSeries);
    listenForUpdates.start();

    pack();

    // center on screen
    setLocationRelativeTo(null);
  }

  public IHeartManListener getHeartManListener() {
    return listenForUpdates;
  }
}
