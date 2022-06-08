/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.op;

import java.nio.charset.StandardCharsets;
import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.internal.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateFormatter;
import ucar.nc2.calendar.CalendarPeriod;
import ucar.ui.widget.FileManager;
import ucar.util.prefs.PreferencesExt;
import java.awt.BorderLayout;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

/** Component "Data Table" in DatasetViewer when you have a Variable (not Structure) selected */
public class VariableTable extends JPanel {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final FileManager fileChooser; // for exporting

  private List<Object> col0;
  private Array<?>[] data;

  private int col0Dim = -1;
  private boolean col0isDate;

  private NetcdfFile file;
  private NetcdfDataset fds;

  private String[] columnNames;

  private final VariableTableModel dataModel = new VariableTableModel();

  private JCheckBox includeGlobals;

  VariableTable(PreferencesExt prefs) {
    PreferencesExt fcPrefs = (prefs == null) ? null : (PreferencesExt) prefs.node("FileManager");
    fileChooser = new FileManager(null, null, "csv", "comma seperated values", fcPrefs);
  }

  public void clear() {
    if (dataModel != null) {
      dataModel.clear();
    }
    col0 = null;
  }

  public void saveState() {
    fileChooser.save();
  }

  public void setVariableList(List<Variable> vl) {
    int length;
    int i = 0;

    // find the number of columns, get shape for each variable after the first DIMENSON
    for (Variable v : vl) {
      int[] shape = v.getShape();

      length = 1;
      for (int j = 1; j < shape.length; j++) {
        length *= shape[j];
      }
      i += length;
      log.info("Length " + length + " i = " + i + " var " + v.getDimensionsString());
    }
    length = i;

    columnNames = new String[length + 1];
    data = new Array[length];

    // Add each variable to the list of data, and also create column names
    i = 0;

    for (Variable v : vl) {
      log.info("Variable " + v.getShortName());
      List<Dimension> vd = v.getDimensions();
      int dimNo = 0;
      for (Dimension d : vd) {
        Variable dimVar = file.findVariable(d.getShortName());
        if (dimVar != null) {
          Attribute axis = dimVar.findAttribute("axis");
          if ((axis != null) && axis.isString() && (axis.getStringValue().compareTo("T") == 0)) {
            CoordinateAxis1DTime tm;
            try {
              tm = CoordinateAxis1DTime.factory(fds, VariableDS.fromVar(v.getParentGroup(), dimVar, true), null);
              col0 = new ArrayList<>(tm.getCalendarDates());
              col0Dim = dimNo;
              col0isDate = true;
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
        dimNo++;
      }
      int[] shape = v.getShape();

      if (col0Dim < 0) {
        col0 = new ArrayList<>();
        for (int j = 0; j < shape[0]; j++) {
          col0.add(j);
        }
        col0Dim = 0;
      }
      columnNames[0] = vd.get(col0Dim).getShortName();

      try {
        if ((shape.length > 1) && (shape[1] > 1)) {
          Variable dimVar = file.findVariable(v.getDimension(1).getShortName());
          Array<?> dimArray = null;
          if (dimVar != null)
            dimArray = dimVar.readArray();

          for (int j = 0; j < shape[1]; j++) {
            data[i] = v.slice(1, j).readArray();
            if (dimVar != null) {
              columnNames[i + 1] = v.getShortName() + "[" + dimArray.get(j) + "]";
            } else {
              columnNames[i + 1] = v.getShortName() + "[" + j + "]";
            }

            Attribute unit = v.findAttribute("units");

            if ((unit != null) && unit.isString()) {
              columnNames[i + 1] += " (" + unit.getStringValue() + ")";
            }

            i++;
          }
        } else {
          data[i] = v.readArray();
          columnNames[i + 1] = v.getShortName();
          Attribute unit = v.findAttribute("units");

          if ((unit != null) && unit.isString()) {
            columnNames[i + 1] += " (" + unit.getStringValue() + ")";
          }
          i++;
        }
      } catch (IOException | InvalidRangeException e) {
        e.printStackTrace();
      }
    }
  }

  public void setDataset(NetcdfFile ds) {
    file = ds;
    try {
      fds = NetcdfDatasets.enhance(file, NetcdfDataset.getDefaultEnhanceMode(), null);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  class VariableTableModel extends AbstractTableModel {

    public void clear() {}

    @Override
    public int getColumnCount() {
      return data.length + 1;
    }

    @Override
    public String getColumnName(int col) {
      return columnNames[col];
    }

    @Override
    public Class<?> getColumnClass(int col) {
      return String.class;
    }

    @Override
    public int getRowCount() {
      return col0.size();
    }

    @Override
    public Object getValueAt(int row, int col) {
      if (col == 0) {
        return col0.get(row);
      }

      return data[col - 1].get(row);
    }
  }

  public void createTable() {
    this.setLayout(new BorderLayout());

    JTable table = new JTable(dataModel);
    table.setPreferredScrollableViewportSize(new java.awt.Dimension(500, 70));
    if (col0isDate) {
      table.getColumnModel().getColumn(0).setCellRenderer(new DateRenderer());
    }
    table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

    // Create the scroll pane and add the table to it.
    JScrollPane scrollPane = new JScrollPane(table);

    // Add the scroll pane to this panel.
    this.removeAll();
    table.setFillsViewportHeight(true);

    add(scrollPane);

    includeGlobals = new JCheckBox("Export Attributes");

    JButton export = new JButton("Export");
    export.addActionListener(e -> export());

    JPanel holderPanel = new JPanel(new BorderLayout());
    holderPanel.add(export, BorderLayout.EAST);
    holderPanel.add(includeGlobals, BorderLayout.CENTER);
    add(holderPanel, BorderLayout.PAGE_END);
  }

  private void export() {
    String filename = fileChooser.chooseFilename();

    if (filename == null) {
      return;
    }

    CalendarDateFormatter printForm = new CalendarDateFormatter("yyyy-MM-dd HH:mm:ss");

    try (FileOutputStream fos = new FileOutputStream(filename);
        OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
        PrintWriter pw = new PrintWriter(osw)) {

      pw.println("; file name : " + fds.getLocation());

      if (includeGlobals.isSelected()) {
        for (Attribute a : fds.getRootGroup().attributes()) {
          pw.println("; " + a.toString());
        }
      }

      pw.println("; this file written : " + new Date());

      TableModel model = dataModel;
      for (int col = 0; col < model.getColumnCount(); col++) {
        if (col > 0) {
          pw.print(",");
        }
        pw.print(model.getColumnName(col));
      }
      pw.println();

      for (int row = 0; row < model.getRowCount(); row++) {
        for (int col = 0; col < model.getColumnCount(); col++) {
          if (col > 0) {
            pw.print(",");
          }
          Object o = model.getValueAt(row, col);
          if (o instanceof CalendarDate) {
            CalendarDate d = (CalendarDate) o;
            pw.print(printForm.toString(d));
          } else {
            pw.print(o);
          }

        }
        pw.println();
      }
      pw.close();
      JOptionPane.showMessageDialog(this, "File successfully written");
    } catch (IOException ioe) {
      JOptionPane.showMessageDialog(this, "ERROR: " + ioe.getMessage());
      ioe.printStackTrace();
    }

    fileChooser.save();
  }

  static class DateRenderer extends DefaultTableCellRenderer {
    private final CalendarDateFormatter newForm;
    private final CalendarDateFormatter oldForm;
    private final CalendarDate cutoff;

    DateRenderer() {

      oldForm = new CalendarDateFormatter("yyyy-MM-dd HH:mm:ss");
      newForm = new CalendarDateFormatter("dd MMM HH:mm:ss");

      CalendarDate now = CalendarDate.present();
      cutoff = now.add(-1, CalendarPeriod.Field.Year); // "now" time format within a year
    }

    public void setValue(Object value) {
      if (value == null) {
        setText("");
      } else {
        CalendarDate date = (CalendarDate) value;
        if (date.isBefore(cutoff)) {
          setText(oldForm.toString(date));
        } else {
          setText(newForm.toString(date));
        }
      }
    }
  }
}
