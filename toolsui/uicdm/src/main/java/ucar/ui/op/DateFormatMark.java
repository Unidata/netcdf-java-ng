/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.op;

import thredds.inventory.internal.DateFromString;
import ucar.nc2.calendar.CalendarDateFormatter;
import ucar.ui.OpPanel;
import ucar.ui.widget.TextHistoryPane;
import ucar.util.prefs.PreferencesExt;
import ucar.ui.prefs.ComboBox;
import java.awt.BorderLayout;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import javax.swing.JButton;

public class DateFormatMark extends OpPanel {
  private final ComboBox<String> testCB;
  private final TextHistoryPane ta;

  public DateFormatMark(PreferencesExt prefs) {
    super(prefs, "dateFormatMark:", false, false);

    ta = new TextHistoryPane(true);
    add(ta, BorderLayout.CENTER);

    testCB = new ComboBox<>(prefs);
    buttPanel.add(testCB);

    JButton compareButton = new JButton("Apply");
    compareButton.addActionListener(e -> apply(cb.getSelectedItem(), testCB.getSelectedItem()));
    buttPanel.add(compareButton);
  }

  @Override
  public boolean process(Object o) {
    return false;
  }

  @Override
  public void closeOpenFiles() {
    // Nothing to do here.
  }

  private void apply(Object mark, Object testo) {
    String dateFormatMark = (String) mark;
    String filename = (String) testo;
    try {
      Date coordValueDate = DateFromString.getDateUsingDemarkatedCount(filename, dateFormatMark, '#');
      String coordValue = CalendarDateFormatter.toDateTimeStringISO(coordValueDate);
      ta.setText("got date= " + coordValue);
    } catch (Exception e) {
      StringWriter sw = new StringWriter(5000);
      e.printStackTrace(new PrintWriter(sw));
      ta.setText(sw.toString());
    }
  }
}
