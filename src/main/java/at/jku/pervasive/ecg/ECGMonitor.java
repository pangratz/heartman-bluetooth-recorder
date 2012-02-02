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
import org.jfree.data.xy.XYDataset;

public class ECGMonitor extends JFrame {

  private class ListenForUpdates extends Thread implements IHeartManListener {

    private final List<TimeSeriesDataItem> buffer;
    private final TimeSeries series;

    public ListenForUpdates(TimeSeries series) {
      super();
      this.series = series;
      this.buffer = Collections
          .synchronizedList(new LinkedList<TimeSeriesDataItem>());
    }

    @Override
    public void dataReceived(String address, long timestamp, double value) {
      if (!filter || (value < 4.0 && value > -2.0)) {
        Millisecond now = new Millisecond(new Date(timestamp));
        this.buffer.add(new TimeSeriesDataItem(now, value));
        // last = timestamp;
      }
    }

    @Override
    public void run() {
      while (!isInterrupted()) {
        try {
          Thread.sleep(40);
          // tell plot to repaint
          if (doUpdate) {
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
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
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

    TimeSeries timeSeries = new TimeSeries("ecg");
    timeSeries.setMaximumItemAge(TimeUnit.SECONDS.toMillis(5));

    XYDataset dataset = new TimeSeriesCollection(timeSeries);
    JFreeChart chart = ChartFactory.createTimeSeriesChart("ecg", "time", "mV",
        dataset, false, true, false);

    XYPlot xyPlot = (XYPlot) chart.getPlot();
    ValueMarker baselineMarker = new ValueMarker(0.0D, Color.BLACK,
        new BasicStroke());
    xyPlot.addRangeMarker(baselineMarker);

    xyPlot.getRangeAxis().setRange(new Range(-1.0, 3.0));

    ChartPanel chartPanel = new ChartPanel(chart);
    add(chartPanel, BorderLayout.CENTER);

    JPanel settingsPanel = createSettingsPanel();
    add(settingsPanel, BorderLayout.SOUTH);

    listenForUpdates = new ListenForUpdates(timeSeries);
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
