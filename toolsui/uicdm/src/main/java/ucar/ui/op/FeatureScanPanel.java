/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.op;

import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/** Scan for Feature Datasets */
public class FeatureScanPanel extends JPanel {
  private final PreferencesExt prefs;

  private final BeanTable<FeatureScan.Bean> ftTable;
  private final JSplitPane split;
  private final TextHistoryPane dumpTA;
  private final IndependentWindow infoWindow;

  public FeatureScanPanel(PreferencesExt prefs) {
    this.prefs = prefs;

    ftTable = new BeanTable<>(FeatureScan.Bean.class, (PreferencesExt) prefs.node("FeatureDatasetBeans"), false);
    ftTable.addListSelectionListener(e -> {
      FeatureScan.Bean ftb = ftTable.getSelectedBean();
      if (ftb != null) {
        setSelectedFeatureDataset(ftb);
      }
    });

    PopupMenu varPopup = new PopupMenu(ftTable.getJTable(), "Options");
    varPopup.addAction("Open as NetcdfFile", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = ftTable.getSelectedBean();
        if (ftb == null)
          return;
        FeatureScanPanel.this.firePropertyChange("openNetcdfFile", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Open in CoordSystems", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = ftTable.getSelectedBean();
        if (ftb == null)
          return;
        FeatureScanPanel.this.firePropertyChange("openCoordSystems", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Open as GridDataset", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = ftTable.getSelectedBean();
        if (ftb == null)
          return;
        FeatureScanPanel.this.firePropertyChange("openGridDataset", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Open as NewGrid", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = ftTable.getSelectedBean();
        if (ftb == null)
          return;
        FeatureScanPanel.this.firePropertyChange("openNewGrid", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Open as PointDataset", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = ftTable.getSelectedBean();
        if (ftb == null)
          return;
        FeatureScanPanel.this.firePropertyChange("openPointFeatureDataset", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Open as RadialDataset", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = ftTable.getSelectedBean();
        if (ftb == null)
          return;
        FeatureScanPanel.this.firePropertyChange("openRadialDataset", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Open as NcML", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        FeatureScan.Bean ftb = ftTable.getSelectedBean();
        if (ftb == null)
          return;
        FeatureScanPanel.this.firePropertyChange("openNcML", null, ftb.f.getPath());
      }
    });

    varPopup.addAction("Show Report on selected rows", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        List<FeatureScan.Bean> selected = ftTable.getSelectedBeans();
        Formatter f = new Formatter();
        for (FeatureScan.Bean bean : selected) {
          bean.toString(f, false);
        }
        dumpTA.setText(f.toString());
      }
    });

    // the info window
    TextHistoryPane infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    dumpTA = new TextHistoryPane();
    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, ftTable, dumpTA);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
  }

  public PreferencesExt getPrefs() {
    return prefs;
  }

  public void save() {
    ftTable.saveState(false);
    prefs.putInt("splitPos", split.getDividerLocation());
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
  }

  public void clear() {
    ftTable.setBeans(new ArrayList<>()); // clear
  }

  public boolean setScanDirectory(String dirName) {
    clear();

    // repaint();
    FeatureScan scanner = new FeatureScan(dirName, true);
    Formatter errlog = new Formatter();
    List<FeatureScan.Bean> beans = scanner.scan(errlog);
    if (beans.isEmpty()) {
      dumpTA.setText(errlog.toString());
      return false;
    }

    ftTable.setBeans(beans);
    // repaint();
    return true;
  }

  private void setSelectedFeatureDataset(FeatureScan.Bean ftb) {
    dumpTA.setText(ftb.toString());
    dumpTA.gotoTop();
  }

}
