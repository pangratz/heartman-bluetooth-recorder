package at.jku.pervasive.ecg;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class ECGMonitor extends JFrame {

  private class ListenForUpdates extends Thread implements IHeartManListener {

    private final List<XYDataItem> buffer;
    private final long last = 0;
    private final XYSeries series;

    public ListenForUpdates(XYSeries series) {
      super();
      this.series = series;
      this.buffer = Collections.synchronizedList(new LinkedList<XYDataItem>());
    }

    @Override
    public void dataReceived(String address, long timestamp, double value) {
      // System.out.println(sequence + ": " + value);
      this.buffer.add(new XYDataItem(timestamp, value));
    }

    @Override
    public void run() {
      while (!isInterrupted()) {
        try {
          Thread.sleep(200);
          // tell plot to repaint
          series.setNotify(false);
          synchronized (buffer) {
            for (XYDataItem dataItem : buffer) {
              series.addOrUpdate(dataItem.getXValue(), dataItem.getYValue());
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

    XYSeries series = new XYSeries("ecg");
    series.setMaximumItemCount(100);
    XYSeriesCollection dataset = new XYSeriesCollection(series);

    JFreeChart chart = ChartFactory.createXYLineChart("ecg", "time", "mV",
        dataset, PlotOrientation.VERTICAL, false, false, false);

    XYPlot xyPlot = (XYPlot) chart.getPlot();
    ValueMarker baselineMarker = new ValueMarker(0.0D, Color.BLACK,
        new BasicStroke());
    xyPlot.addRangeMarker(baselineMarker);

    xyPlot.getRangeAxis().setRange(new Range(-2.0, 4.0));

    ChartPanel chartPanel = new ChartPanel(chart);
    add(chartPanel);

    listenForUpdates = new ListenForUpdates(series);
    listenForUpdates.start();

    pack();

    // center on screen
    setLocationRelativeTo(null);
  }

  public IHeartManListener getHeartManListener() {
    return listenForUpdates;
  }
}
