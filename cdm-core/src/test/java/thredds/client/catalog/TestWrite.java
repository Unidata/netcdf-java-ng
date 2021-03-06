/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import thredds.client.catalog.tools.CatalogXmlWriter;
import java.io.*;
import java.util.Iterator;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class TestWrite {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();
  static boolean debugCompare = true, debugCompareList = true;

  @Test
  public void testWrite1() throws IOException {
    String filename = "test1.xml";
    Catalog cat = ClientCatalogUtil.open(filename);
    assertThat(cat).isNotNull();

    File tmpFile = tempFolder.newFile();
    System.out.println(" output filename= " + tmpFile.getPath());

    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(tmpFile))) {
      CatalogXmlWriter writer = new CatalogXmlWriter();
      writer.writeXML(cat, out);
    }

    // read it back in
    Catalog catR = ClientCatalogUtil.open("file:" + tmpFile.getPath());
    assertThat(catR).isNotNull();

    compare(cat, catR);
  }

  private void compare(Catalog cat, Catalog catR) {
    List<Dataset> datasets = cat.getDatasetsLocal();
    List<Dataset> datasetsR = catR.getDatasetsLocal();

    assertThat(datasets.size()).isEqualTo(datasetsR.size());

    int n = Math.min(datasets.size(), datasetsR.size());
    for (int i = 0; i < n; i++) {
      compareDatasets(datasets.get(i), datasetsR.get(i));
    }
  }

  private void compareDatasets(Dataset d, Dataset dR) {
    if (debugCompare)
      System.out.println(" compare datasets (" + d.getName() + ") and (" + dR.getName() + ")");
    compareList(d.getDocumentation(), dR.getDocumentation());
    compareList(d.getAccess(), dR.getAccess());
    // compareList( d.getMetadataOther(), dR.getMetadataOther());
    compareListVariables(d.getVariables(), dR.getVariables());
    compareListVariables(dR.getVariables(), d.getVariables());

    List<Dataset> datasets = d.getDatasets();
    List<Dataset> datasetsR = dR.getDatasets();

    for (int i = 0; i < datasets.size(); i++) {
      compareDatasets(datasets.get(i), datasetsR.get(i));
    }

  }

  private void compareList(List d, List dR) {
    boolean ok = true;
    Iterator iter = d.iterator();
    while (iter.hasNext()) {
      Object item = iter.next();
      int index = dR.indexOf(item);
      if (index < 0) {
        System.out.println("   cant find " + item.getClass().getName() + " " + item + " in output ");
        ok = false;
      } else if (debugCompareList)
        System.out.println("   item ok = (" + item + ")");
    }

    iter = dR.iterator();
    while (iter.hasNext()) {
      Object item = iter.next();
      int index = d.indexOf(item);
      if (index < 0) {
        System.out.println("   cant find " + item.getClass().getName() + " " + item + " in input ");
        ok = false;
      } else if (debugCompareList)
        System.out.println("   itemR ok = (" + item + ")");
    }

    assertThat(ok).isTrue();
  }

  private void compareListVariables(List<ThreddsMetadata.VariableGroup> d, List<ThreddsMetadata.VariableGroup> dR) {
    boolean ok = true;
    for (ThreddsMetadata.VariableGroup item : d) {
      int index = dR.indexOf(item);
      if (index < 0) {
        System.out.println("   cant find " + item.getClass().getName() + " " + item + " in output ");
        ok = false;
      } else if (debugCompareList) {
        ThreddsMetadata.VariableGroup item2 = dR.get(index);
        System.out.println("   Variables ok = (" + item + ") == (" + item2 + ")");
      }
    }

    assertThat(ok).isTrue();
  }

}
