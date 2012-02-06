package at.jku.pervasive.ecg;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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

public class ECGMonitor extends JFrame {

  private class ListenForUpdates extends Thread implements IHeartManListener {

    private final List<TimeSeriesDataItem> buffer1;
    private final List<TimeSeriesDataItem> buffer2;
    private String firstAddress;
    private final TimeSeries series1;
    private final TimeSeries series2;

    public ListenForUpdates(TimeSeries series1, TimeSeries series2) {
      super();
      this.series1 = series1;
      this.series2 = series2;
      this.buffer1 = Collections
          .synchronizedList(new LinkedList<TimeSeriesDataItem>());
      this.buffer2 = Collections
          .synchronizedList(new LinkedList<TimeSeriesDataItem>());
    }

    @Override
    public void dataReceived(String address, long timestamp, double value) {
      if (firstAddress == null) {
        firstAddress = address;
      }
      if (!filter || (value < 4.0 && value > -2.0)) {
        Millisecond now = new Millisecond(new Date(timestamp));
        TimeSeriesDataItem dataItem = new TimeSeriesDataItem(now, value);
        if (address.equals(firstAddress)) {
          this.buffer1.add(dataItem);
        } else {
          this.buffer2.add(dataItem);
        }
      }
    }

    @Override
    public void run() {
      while (!isInterrupted()) {
        try {
          Thread.sleep(200);
          // tell plot to repaint
          if (doUpdate) {
            updateSeries(series1, buffer1);
            updateSeries(series2, buffer2);
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    protected void updateSeries(TimeSeries series,
        List<TimeSeriesDataItem> buffer) {
      series.setNotify(false);
      synchronized (buffer) {
        for (TimeSeriesDataItem dataItem : buffer) {
          series.add(dataItem);
        }
        buffer.clear();
      }
      series.fireSeriesChanged();
      series.setNotify(true);
    }

  }

  private static final long serialVersionUID = 7543095620229093879L;

  private boolean doUpdate = true;
  private boolean filter = true;
  private final ListenForUpdates listenForUpdates;

  public ECGMonitor() {
    super();

    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setLayout(new BorderLayout());

    TimeSeries timeSeries1 = new TimeSeries("ecg 1");
    timeSeries1.setMaximumItemAge(TimeUnit.SECONDS.toMillis(5));
    TimeSeries timeSeries2 = new TimeSeries("ecg 2");
    timeSeries2.setMaximumItemAge(TimeUnit.SECONDS.toMillis(5));

    TimeSeriesCollection dataset = new TimeSeriesCollection();
    dataset.addSeries(timeSeries1);
    dataset.addSeries(timeSeries2);
    JFreeChart chart = ChartFactory.createTimeSeriesChart("ecg", "time", "mV",
        dataset, true, true, false);

    XYPlot xyPlot = (XYPlot) chart.getPlot();
    ValueMarker baselineMarker = new ValueMarker(0.0D, Color.BLACK,
        new BasicStroke());
    xyPlot.addRangeMarker(baselineMarker);

    xyPlot.getRangeAxis().setRange(new Range(-1.0, 3.0));

    ChartPanel chartPanel = new ChartPanel(chart);
    add(chartPanel, BorderLayout.CENTER);

    JPanel settingsPanel = createSettingsPanel();
    add(settingsPanel, BorderLayout.SOUTH);

    listenForUpdates = new ListenForUpdates(timeSeries1, timeSeries2);
    listenForUpdates.start();

    pack();

    // center on screen
    setLocationRelativeTo(null);
  }

  public IHeartManListener getHeartManListener() {
    return listenForUpdates;
  }

  private JPanel createSettingsPanel() {
    JPanel panel = new JPanel(new FlowLayout());

    JCheckBox updateCheckBox = new JCheckBox("update ecg?");
    updateCheckBox.setSelected(this.doUpdate);
    updateCheckBox.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent evt) {
        JCheckBox checkBox = (JCheckBox) evt.getSource();
        doUpdate = checkBox.isSelected();
      }
    });
    panel.add(updateCheckBox);

    JCheckBox doFilterCheckBox = new JCheckBox("do Filter?");
    doFilterCheckBox.setSelected(this.filter);
    doFilterCheckBox.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent evt) {
        JCheckBox checkBox = (JCheckBox) evt.getSource();
        filter = checkBox.isSelected();
      }
    });
    panel.add(doFilterCheckBox);

    return panel;
  }
}
