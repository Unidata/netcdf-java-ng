/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.bufr;

import ucar.ui.OpPanel;
import ucar.util.prefs.PreferencesExt;
import java.awt.BorderLayout;

public class BufrCodePanel extends OpPanel {
  private final BufrWmoCodesPanel codeTable;

  public BufrCodePanel(PreferencesExt p) {
    super(p, "table:", false, false, false);
    codeTable = new BufrWmoCodesPanel(prefs, buttPanel);
    add(codeTable, BorderLayout.CENTER);
  }

  @Override
  public boolean process(Object command) {
    return true;
  }

  @Override
  public void save() {
    codeTable.save();
    super.save();
  }

  @Override
  public void closeOpenFiles() {
    // Nothing to do here.
  }
}
