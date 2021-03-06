/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

/*
 * Created by JFormDesigner on Thu Aug 25 08:13:12 MDT 2011
 */

package ucar.ui.dialog;

import ucar.ui.grib.Grib1TablesViewer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * @author John Caron
 */
public class Grib1TableCompareDialog extends JDialog {
  Grib1TablesViewer.TableBean table1bean;
  Grib1TablesViewer.TableBean table2bean;

  public static class Data {
    public Grib1TablesViewer.TableBean table1bean;
    public Grib1TablesViewer.TableBean table2bean;
    public boolean compareNames, compareUnits, compareDesc, showMissing, cleanUnits, udunits;

    private Data(Grib1TablesViewer.TableBean table1bean, Grib1TablesViewer.TableBean table2bean, boolean compareNames,
        boolean compareUnits, boolean cleanUnits, boolean udunits, boolean compareDesc, boolean showMissing) {
      this.table1bean = table1bean;
      this.table2bean = table2bean;
      this.compareNames = compareNames;
      this.compareUnits = compareUnits;
      this.cleanUnits = cleanUnits;
      this.udunits = udunits;
      this.compareDesc = compareDesc;
      this.showMissing = showMissing;
    }
  }

  public Grib1TableCompareDialog(Frame owner) {
    super(owner);
    initComponents();
  }

  public Grib1TableCompareDialog(Dialog owner) {
    super(owner);
    initComponents();
  }

  public void setTable1(Grib1TablesViewer.TableBean bean) {
    this.table1bean = bean;
    table1.setText(bean.getPath());
  }

  public void setTable2(Grib1TablesViewer.TableBean bean) {
    this.table2bean = bean;
    table2.setText(bean == null ? " all" : bean.getPath());
  }

  private void fileBrowserActionPerformed(ActionEvent e) {}

  private void cancelButtonActionPerformed(ActionEvent e) {}

  private void okButtonActionPerformed(ActionEvent e) {
    Data data = new Data(table1bean, table2bean, compareNames.isSelected(), compareUnits.isSelected(),
        cleanUnits.isSelected(), udUnits.isSelected(), compareDesc.isSelected(), showMissing.isSelected());
    firePropertyChange("OK", null, data);
    setVisible(false);
  }

  private void initComponents() {
    // JFormDesigner - Component initialization - DO NOT MODIFY //GEN-BEGIN:initComponents
    // Generated using JFormDesigner non-commercial license
    dialogPane = new JPanel();
    contentPanel = new JPanel();
    label1 = new JLabel();
    buttonBar = new JPanel();
    compareDesc = new JCheckBox();
    compareUnits = new JCheckBox();
    compareNames = new JCheckBox();
    cancelButton = new JButton();
    okButton = new JButton();
    table1 = new JTextField();
    showMissing = new JCheckBox();
    table2 = new JTextField();
    label2 = new JLabel();
    cleanUnits = new JCheckBox();
    udUnits = new JCheckBox();

    // ======== this ========
    setTitle("Compare Grib1 tables");
    Container contentPane = getContentPane();
    contentPane.setLayout(new BorderLayout());

    // ======== dialogPane ========
    {
      dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
      dialogPane.setLayout(new BorderLayout());

      // ======== contentPanel ========
      {

        // ---- label1 ----
        label1.setText("table1:");
        label1.setFont(new Font("Dialog", Font.BOLD, 12));

        // ======== buttonBar ========
        {
          buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
          buttonBar.setLayout(new GridBagLayout());
          ((GridBagLayout) buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
          ((GridBagLayout) buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};
        }

        // ---- compareDesc ----
        compareDesc.setText("compare Desc");
        compareDesc.setSelected(true);

        // ---- compareUnits ----
        compareUnits.setText("compareUnits");

        // ---- compareNames ----
        compareNames.setText("compareNames");

        // ---- cancelButton ----
        cancelButton.setText("Cancel");
        cancelButton.addActionListener(this::cancelButtonActionPerformed);

        // ---- okButton ----
        okButton.setText("OK");
        okButton.addActionListener(this::okButtonActionPerformed);

        // ---- table1 ----
        table1.setEditable(false);

        // ---- showMissing ----
        showMissing.setText("showMissing");

        // ---- table2 ----
        table2.setEditable(false);

        // ---- label2 ----
        label2.setText("table2:");
        label2.setFont(new Font("Dialog", Font.BOLD, 12));

        // ---- cleanUnits ----
        cleanUnits.setText("cleanUnits");

        // ---- udUnits ----
        udUnits.setText("udUnits");
        udUnits.setSelected(true);

        GroupLayout contentPanelLayout = new GroupLayout(contentPanel);
        contentPanel.setLayout(contentPanelLayout);
        contentPanelLayout.setHorizontalGroup(contentPanelLayout.createParallelGroup()
            .addGroup(contentPanelLayout.createSequentialGroup().addContainerGap().addGroup(contentPanelLayout
                .createParallelGroup(GroupLayout.Alignment.LEADING,
                    false)
                .addGroup(contentPanelLayout
                    .createSequentialGroup().addGroup(contentPanelLayout
                        .createParallelGroup().addComponent(label2).addComponent(label1))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(contentPanelLayout.createParallelGroup().addGroup(contentPanelLayout
                        .createSequentialGroup()
                        .addGroup(contentPanelLayout.createParallelGroup().addGroup(contentPanelLayout
                            .createSequentialGroup()
                            .addGroup(contentPanelLayout
                                .createParallelGroup().addComponent(compareUnits).addComponent(compareDesc))
                            .addGap(26, 26, 26)
                            .addGroup(contentPanelLayout.createParallelGroup().addComponent(showMissing)
                                .addComponent(cleanUnits))
                            .addGroup(contentPanelLayout.createParallelGroup()
                                .addGroup(contentPanelLayout.createSequentialGroup().addGap(15, 15, 15)
                                    .addComponent(udUnits)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE,
                                        Short.MAX_VALUE)
                                    .addComponent(okButton, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(cancelButton,
                                        GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE))
                                .addGroup(contentPanelLayout.createSequentialGroup().addGap(18, 18, 18)
                                    .addComponent(compareNames))))
                            .addComponent(table1, GroupLayout.PREFERRED_SIZE, 611, GroupLayout.PREFERRED_SIZE))
                        .addGap(111, 111, 111))
                        .addComponent(table2, GroupLayout.PREFERRED_SIZE, 611, GroupLayout.PREFERRED_SIZE)))
                .addComponent(buttonBar, GroupLayout.Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 779,
                    GroupLayout.PREFERRED_SIZE))
                .addContainerGap()));
        contentPanelLayout.setVerticalGroup(contentPanelLayout.createParallelGroup()
            .addGroup(contentPanelLayout.createSequentialGroup().addGap(11, 11, 11)
                .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(table1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE)
                    .addComponent(label1))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(contentPanelLayout
                    .createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(label2).addComponent(table2,
                        GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(compareDesc).addComponent(showMissing).addComponent(compareNames))
                .addGroup(contentPanelLayout
                    .createParallelGroup()
                    .addGroup(contentPanelLayout.createSequentialGroup().addGap(22, 22, 22)
                        .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                            .addComponent(cancelButton).addComponent(okButton)))
                    .addGroup(contentPanelLayout.createSequentialGroup()
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(contentPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                            .addComponent(compareUnits).addComponent(cleanUnits).addComponent(udUnits))))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 23, Short.MAX_VALUE).addComponent(buttonBar,
                    GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addContainerGap()));
      }
      dialogPane.add(contentPanel, BorderLayout.SOUTH);
    }
    contentPane.add(dialogPane, BorderLayout.CENTER);
    pack();
    setLocationRelativeTo(getOwner());
    // JFormDesigner - End of component initialization //GEN-END:initComponents
  }

  // JFormDesigner - Variables declaration - DO NOT MODIFY //GEN-BEGIN:variables
  // Generated using JFormDesigner non-commercial license
  private JPanel dialogPane;
  private JPanel contentPanel;
  private JLabel label1;
  private JPanel buttonBar;
  private JCheckBox compareDesc;
  private JCheckBox compareUnits;
  private JCheckBox compareNames;
  private JButton cancelButton;
  private JButton okButton;
  private JTextField table1;
  private JCheckBox showMissing;
  private JTextField table2;
  private JLabel label2;
  private JCheckBox cleanUnits;
  private JCheckBox udUnits;
  // JFormDesigner - End of variables declaration //GEN-END:variables
}
