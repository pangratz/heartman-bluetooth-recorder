package at.jku.pervasive.ecg;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.xy.XYDataset;

public class ECGMonitor extends JFrame {

  private class ListenForUpdates extends Thread implements IHeartManListener {

    private final List<TimeSeriesDataItem> buffer;
    private final long last = 0;
    private final TimeSeries series;

    public ListenForUpdates(TimeSeries series) {
      super();
      this.series = series;
      this.buffer = Collections
          .synchronizedList(new LinkedList<TimeSeriesDataItem>());
    }

    @Override
    public void dataReceived(String address, long timestamp, double value) {
      if ((value < 4.0 && value > -2.0)) {
        Millisecond now = new Millisecond(new Date(timestamp));
        this.buffer.add(new TimeSeriesDataItem(now, value));
        // last = timestamp;
      }
    }

    @Override
    public void run() {
      while (!isInterrupted()) {
        try {
          Thread.sleep(150);
          // tell plot to repaint
          series.setNotify(false);
          synchronized (buffer) {
            for (TimeSeriesDataItem dataItem : buffer) {
              series.add(dataItem);
            }
            buffer.clear();
          }
          series.fireSeriesChanged();
          series.setNotify(true);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

  }

  public static void main(String[] args) throws Exception {
    HeartManDiscovery heartManDiscovery = new HeartManDiscovery();
    // heartManDiscovery.discoverHeartManDevices();

    String heartman = "00A096203DCB"; // C102
    // heartman = "00A096203DD1"; // C157
    RemoteDevice remoteDevice = heartManDiscovery.pingDevice(heartman);
    List<ServiceRecord> services = heartManDiscovery
        .searchServices(remoteDevice);
    ServiceRecord serviceRecord = services.get(0);

    ECGMonitor monitor = new ECGMonitor();
    monitor.setVisible(true);

    IHeartManListener hml = monitor.getHeartManListener();
    heartManDiscovery.startListening(heartman, hml, serviceRecord);
  }

  private final ListenForUpdates listenForUpdates;

  public ECGMonitor() {
    super();

    setDefaultCloseOperation(EXIT_ON_CLOSE);

    TimeSeries timeSeries = new TimeSeries("ecg");
    timeSeries.setMaximumItemAge(TimeUnit.SECONDS.toMillis(5));

    XYDataset dataset = new TimeSeriesCollection(timeSeries);
    JFreeChart chart = ChartFactory.createTimeSeriesChart("ecg", "time", "mV",
        dataset, false, false, false);

    XYPlot xyPlot = (XYPlot) chart.getPlot();
    ValueMarker baselineMarker = new ValueMarker(0.0D, Color.BLACK,
        new BasicStroke());
    xyPlot.addRangeMarker(baselineMarker);

    xyPlot.getRangeAxis().setRange(new Range(-1.0, 3.0));

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
