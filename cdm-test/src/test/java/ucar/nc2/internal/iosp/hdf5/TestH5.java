/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf5;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.Index;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

@Category(NeedsCdmUnitTest.class)
public class TestH5 {
  private static boolean dumpFile = false;
  public static String testDir = TestDir.cdmUnitTestDir + "formats/hdf5/";

  public static NetcdfFile open(String filename) throws IOException {
    System.out.println("**** Open " + filename);
    NetcdfFile ncfile = NetcdfFiles.open(filename);
    if (TestH5.dumpFile)
      System.out.println("open " + ncfile);
    return ncfile;
  }

  public static NetcdfFile openH5(String filename) throws IOException {
    System.out.println("**** Open " + testDir + filename);
    NetcdfFile ncfile = NetcdfFiles.open(testDir + filename);
    if (TestH5.dumpFile)
      System.out.println("open H5 " + ncfile);
    return ncfile;
  }

  public static NetcdfDataset openH5dataset(String filename) throws IOException {
    System.out.println("**** Open " + testDir + filename);
    NetcdfDataset ncfile = NetcdfDatasets.openDataset(testDir + filename);
    if (TestH5.dumpFile)
      System.out.println("open H5 " + ncfile);
    return ncfile;
  }

  public static class H5FileFilter implements FileFilter {
    public boolean accept(File file) {
      String name = file.getPath();
      return (name.endsWith(".h5") || name.endsWith(".H5") || name.endsWith(".he5") || name.endsWith(".nc"));
    }
  }

  //////////////////////////////////////////////////////////////////////////

  // file that is offset 2048 bytes - NPP!
  @Test
  public void testSuperblockIsOffset() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("superblockIsOffsetNPP.h5")) {
      Variable v = ncfile.findVariable("BeamTime");
      System.out.printf("%s%n", v);

      Array<Long> data = (Array<Long>) v.readArray();
      Index ii = data.getIndex();
      assertThat(data.get(ii.set(11, 93))).isEqualTo(1718796166693743L);
    }
  }

  // file that is offset 512 bytes - MatLab, using compact layout (!)
  @Test
  public void testOffsetCompactLayout() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("matlab_cols.mat")) {
      Variable v = ncfile.findVariable("b");
      System.out.printf("%s%n", v);

      Array<Number> data = (Array<Number>) v.readArray();
      Index ii = data.getIndex();
      assertThat(data.get(ii.set(3, 2)).doubleValue()).isEqualTo(12.0);
    }
  }

  // groups have a cycle using hard link
  /*
   * $ h5dump h5ex_g_traverse.h5
   * HDF5 "h5ex_g_traverse.h5" {
   * GROUP "/" {
   * GROUP "group1" {
   * DATASET "dset1" {
   * DATATYPE H5T_STD_I32LE
   * DATASPACE SIMPLE { ( 1, 1 ) / ( 1, 1 ) }
   * DATA {
   * (0,0): 0
   * }
   * }
   * GROUP "group3" {
   * DATASET "dset2" {
   * HARDLINK "/group1/dset1"
   * }
   * GROUP "group4" {
   * GROUP "group1" {
   * GROUP "group5" {
   * HARDLINK "/group1"
   * }
   * }
   * GROUP "group2" {
   * }
   * }
   * }
   * }
   * GROUP "group2" {
   * HARDLINK "/group1/group3"
   * }
   * }
   */
  @Test
  public void testGroupHardLinks() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("groupHasCycle.h5")) {
      System.out.printf("%s%n", ncfile);
    }
  }

}
