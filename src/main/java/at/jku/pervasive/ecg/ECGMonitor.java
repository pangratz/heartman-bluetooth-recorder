package at.jku.pervasive.ecg;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Millisecond;

public class ECGMonitor extends JFrame {

  private class ListenForUpdates extends Thread implements IHeartManListener {

    private final List<Float> buffer1;
    private final List<Float> buffer2;
    private String firstAddress;
    private final DynamicTimeSeriesCollection series1;
    private final DynamicTimeSeriesCollection series2;

    public ListenForUpdates(DynamicTimeSeriesCollection timeSeries1,
        DynamicTimeSeriesCollection timeSeries2) {
      super();
      this.series1 = timeSeries1;
      this.series2 = timeSeries2;
      this.buffer1 = Collections.synchronizedList(new LinkedList<Float>());
      this.buffer2 = Collections.synchronizedList(new LinkedList<Float>());
    }

    @Override
    public void dataReceived(String address, long timestamp, double value) {
      if (firstAddress == null) {
        firstAddress = address;
      }
      if (!filter || (value < 4.0 && value > -2.0)) {
        if (address.equals(firstAddress)) {
          this.buffer1.add((float) value);
        } else {
          this.buffer2.add((float) value);
        }
      }
    }

    @Override
    public void run() {
      while (!isInterrupted()) {
        try {
          Thread.sleep(50);
          // tell plot to repaint
          if (doUpdate) {
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                updateSeries(series1, buffer1);
                updateSeries(series2, buffer2);
              };
            });
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    protected void updateSeries(DynamicTimeSeriesCollection series,
        List<Float> buffer) {
      synchronized (buffer) {
        float[] data = new float[buffer.size()];
        int index = 0;
        for (Float value : buffer) {
          data[index++] = value.floatValue();
        }
        series.appendData(data);
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

    DynamicTimeSeriesCollection timeSeries1 = new DynamicTimeSeriesCollection(
        1, 5 * 1000, new Millisecond());
    DynamicTimeSeriesCollection timeSeries2 = new DynamicTimeSeriesCollection(
        1, 5 * 1000, new Millisecond());

    timeSeries1.appendData(new float[] { 1.0f });
    timeSeries2.appendData(new float[] { 1.0f });

    CombinedDomainXYPlot domainXYPlot = new CombinedDomainXYPlot();
    domainXYPlot.setDomainAxis(new DateAxis());
    domainXYPlot.add(createXYPlot(timeSeries1));
    domainXYPlot.add(createXYPlot(timeSeries2));
    JFreeChart chart = new JFreeChart(domainXYPlot);
    add(new ChartPanel(chart), BorderLayout.CENTER);

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

  private XYPlot createXYPlot(DynamicTimeSeriesCollection timeSeries) {
    JFreeChart chart = ChartFactory.createTimeSeriesChart("ecg", "time", "mV",
        timeSeries, false, true, false);

    XYPlot xyPlot = (XYPlot) chart.getPlot();
    ValueMarker baselineMarker = new ValueMarker(0.0D, Color.BLACK,
        new BasicStroke());
    xyPlot.addRangeMarker(baselineMarker);

    xyPlot.getRangeAxis().setRange(new Range(-1.0, 3.0));
    return xyPlot;
  }
}
