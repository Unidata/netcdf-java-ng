/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.op;

import ucar.nc2.NetcdfFile;
// import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.ui.OpPanel;
import ucar.ui.ToolsUI;
import ucar.ui.widget.BAMutil;
import ucar.util.prefs.PreferencesExt;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;

public class DatasetViewerPanel extends OpPanel {
  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final DatasetViewer dsViewer;
  private NetcdfFile ncfile;

  public DatasetViewerPanel(PreferencesExt dbPrefs) {
    super(dbPrefs, "dataset:");

    dsViewer = new DatasetViewer(dbPrefs, fileChooser);
    add(dsViewer, BorderLayout.CENTER);

    AbstractButton infoButton = BAMutil.makeButtcon("Information", "Detail Info", false);
    infoButton.addActionListener(e -> {
      if (ncfile != null) {
        detailTA.setText(ncfile.getDetailInfo());
        detailTA.gotoTop();
        detailWindow.show();
      }
    });
    buttPanel.add(infoButton);

    AbstractAction dumpAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        NetcdfFile ds = dsViewer.getDataset();
        if (ds != null) {
          logger.debug("setNCdumpPanel");
          ToolsUI.setNCdumpPanel(ds);
        }
      }
    };
    BAMutil.setActionProperties(dumpAction, "Dump", "NCDump", false, 'D', -1);
    BAMutil.addActionToContainer(buttPanel, dumpAction);

    dsViewer.addActions(buttPanel);
  }

  @Override
  public boolean process(Object o) {
    String location = (String) o;
    boolean err = false;
    NetcdfFile ncnew;

    try {
      if (ncfile != null) {
        ncfile.close();
      }
    } catch (IOException ioe) {
      logger.warn("close failed");
    }

    try {
      /*
       * if (jni) {
       * Nc4Iosp iosp = new Nc4Iosp();
       * ncnew = new NetcdfFileSubclass(iosp, location);
       * RandomAccessFile raf = new RandomAccessFile(location, "r");
       * iosp.open(raf, ncnew, null);
       * } else {
       */
      ncnew = ToolsUI.getToolsUI().openFile(location, useCoords, null);
      // }
      if (ncnew != null) {
        setDataset(ncnew);
      }
    } catch (Exception ioe) {
      StringWriter sw = new StringWriter(5000);
      ioe.printStackTrace(new PrintWriter(sw));
      detailTA.setText(sw.toString());
      detailWindow.show();
      err = true;
    }

    return !err;
  }

  @Override
  public void closeOpenFiles() throws IOException {
    if (ncfile != null) {
      ncfile.close();
    }
    ncfile = null;
    dsViewer.clear();
  }

  public void setDataset(NetcdfFile nc) {
    try {
      if (ncfile != null) {
        ncfile.close();
      }
    } catch (IOException ioe) {
      logger.warn("close failed");
    }
    ncfile = nc;

    if (nc != null) {
      dsViewer.setDataset(nc);
      setSelectedItem(nc.getLocation());
    }
  }

  @Override
  public void save() {
    super.save();
    dsViewer.save();
  }

  public void setText(String text) {
    detailTA.setText(text);
  }

  public void appendLine(String text) {
    detailTA.appendLine(text);
  }
}
