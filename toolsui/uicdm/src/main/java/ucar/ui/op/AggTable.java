/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.op;

import static ucar.nc2.dataset.NetcdfDataset.AGGREGATION;

import javax.annotation.Nullable;
import ucar.array.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.internal.ncml.AggDataset;
import ucar.nc2.internal.ncml.Aggregation;
import ucar.nc2.write.NcdumpArray;
import ucar.ui.widget.BAMutil;
import ucar.ui.widget.IndependentWindow;
import ucar.ui.widget.PopupMenu;
import ucar.ui.widget.TextHistoryPane;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.BeanTable;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Formatter;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 * ToolsUI/NcML/Aggregation
 *
 * @author caron
 * @since Aug 15, 2008
 */
public class AggTable extends JPanel {
  private final PreferencesExt prefs;

  private final BeanTable<DatasetBean> datasetTable;
  private final JSplitPane split;

  private final TextHistoryPane infoTA, aggTA;
  private IndependentWindow infoWindow;

  private NetcdfDataset current;

  AggTable(PreferencesExt prefs, JPanel buttPanel) {
    this.prefs = prefs;

    // the info window
    infoTA = new TextHistoryPane();
    infoWindow = new IndependentWindow("Extra Information", BAMutil.getImage("nj22/NetcdfUI"), infoTA);
    infoWindow.setBounds((Rectangle) prefs.getBean("InfoWindowBounds", new Rectangle(300, 300, 500, 300)));

    datasetTable = new BeanTable<>(DatasetBean.class, (PreferencesExt) prefs.node("DatasetBean"), false);

    PopupMenu varPopup = new PopupMenu(datasetTable.getJTable(), "Options");
    varPopup.addAction("Open as NetcdfFile", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        DatasetBean dsb = datasetTable.getSelectedBean();
        if (dsb == null) {
          return;
        }
        AggTable.this.firePropertyChange("openNetcdfFile", null, dsb.acquireFile());
      }
    });

    varPopup.addAction("Check CoordSystems", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        DatasetBean dsb = datasetTable.getSelectedBean();
        if (dsb == null) {
          return;
        }
        AggTable.this.firePropertyChange("openCoordSystems", null, dsb.acquireFile());
      }
    });

    varPopup.addAction("Open as GridDataset", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        DatasetBean dsb = datasetTable.getSelectedBean();
        if (dsb == null) {
          return;
        }
        AggTable.this.firePropertyChange("openGridDataset", null, dsb.acquireFile());
      }
    });

    AbstractButton compareButton = BAMutil.makeButtcon("Select", "Check files", false);
    compareButton.addActionListener(e -> {
      Formatter f = new Formatter();
      compare(f);
      checkAggCoordinate(f);

      infoTA.setText(f.toString());
      infoTA.gotoTop();
      infoWindow.show();
    });
    buttPanel.add(compareButton);

    aggTA = new TextHistoryPane();

    split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, datasetTable, aggTA);
    split.setDividerLocation(prefs.getInt("splitPos", 500));

    setLayout(new BorderLayout());
    add(split, BorderLayout.CENTER);
  }

  /**
   *
   */
  public void save() {
    datasetTable.saveState(false);
    prefs.putBeanObject("InfoWindowBounds", infoWindow.getBounds());
    prefs.putInt("splitPos", split.getDividerLocation());
  }

  public void clear() {
    datasetTable.clearBeans();
    aggTA.clear();
  }

  void setAggDataset(NetcdfDataset ncd) {
    current = ncd;

    Aggregation agg = (Aggregation) current.sendIospMessage(AGGREGATION);
    List<DatasetBean> beanList = new ArrayList<>();
    for (AggDataset dataset : agg.getDatasets()) {
      beanList.add(new DatasetBean(dataset));
    }

    datasetTable.setBeans(beanList);

    Formatter f = new Formatter();
    agg.getDetailInfo(f);
    aggTA.setText(f.toString());
  }

  private void checkAggCoordinate(Formatter f) {
    if (null == current) {
      return;
    }

    try {
      Aggregation agg = (Aggregation) current.sendIospMessage(AGGREGATION);
      String aggDimName = agg.getDimensionName();
      Variable aggCoord = current.findVariable(aggDimName);

      Array<?> data = aggCoord.readArray();
      f.format("   Aggregated coordinate variable %s%n", aggCoord);
      f.format(NcdumpArray.printArray(data, aggDimName, null));

      for (Object bean : datasetTable.getBeans()) {
        DatasetBean dbean = (DatasetBean) bean;
        AggDataset ads = dbean.ds;

        try (NetcdfFile aggFile = ads.acquireFile(null)) {
          f.format("   Component file %s%n", aggFile.getLocation());
          Variable aggCoordp = aggFile.findVariable(aggDimName);
          if (aggCoordp == null) {
            f.format("   doesnt have coordinate variable%n");
          } else {
            data = aggCoordp.readArray();
            f.format(NcdumpArray.printArray(data,
                aggCoordp.getNameAndDimensions() + " (" + aggCoordp.getUnitsString() + ")", null));
          }
        }
      }
    } catch (Throwable t) {
      StringWriter sw = new StringWriter(10000);
      t.printStackTrace(new PrintWriter(sw));
      f.format(sw.toString());
    }
  }

  private void compare(Formatter f) {
    try {
      NetcdfFile org = null;
      for (Object bean : datasetTable.getBeans()) {
        DatasetBean dbean = (DatasetBean) bean;
        AggDataset ads = dbean.ds;

        NetcdfFile ncd = ads.acquireFile(null);
        if (org == null) {
          org = ncd;
        } else {
          CompareNetcdf2 cn = new CompareNetcdf2(f, false, false, false);
          cn.compareVariables(org, ncd);
          ncd.close();
          f.format("--------------------------------%n");
        }
      }
      if (org != null) {
        org.close();
      }
    } catch (Throwable t) {
      StringWriter sw = new StringWriter(10000);
      t.printStackTrace(new PrintWriter(sw));
      f.format(sw.toString());
    }
  }

  public static class DatasetBean {
    AggDataset ds;

    @Nullable
    NetcdfFile acquireFile() {
      try {
        return ds.acquireFile(null);
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    }

    DatasetBean(AggDataset ds) {
      this.ds = ds;
    }

    public String getLocation() {
      return ds.getLocation();
    }

    public String getCacheLocation() {
      return ds.getCacheLocation();
    }

    public String getId() {
      return ds.getId();
    }
  }
}
