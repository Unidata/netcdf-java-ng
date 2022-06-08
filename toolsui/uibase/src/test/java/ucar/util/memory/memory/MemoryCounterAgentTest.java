/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.util.memory.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import ucar.nc2.dataset.NetcdfDatasets;

public class MemoryCounterAgentTest {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void measureSize(Object o) {
    long memShallow = MemoryCounterAgent.sizeOf(o);
    long memDeep = MemoryCounterAgent.deepSizeOf(o);
    System.out.printf("%s, shallow=%d, deep=%d%n", o.getClass().getSimpleName(), memShallow, memDeep);
  }

  public static void measureSize(String name, Object o, Class skipClass, boolean show) {
    if (o == null)
      return;
    long memShallow = MemoryCounterAgent.sizeOf(o);
    long memDeep = MemoryCounterAgent.deepSizeOf3(name, o, skipClass, show);
    System.out.printf("%n %4d %6d %s (%s)%n", memShallow, memDeep, name, o.getClass().getName());
  }

  // TODO get this working
  public void testStuff() throws IOException {
    measureSize(new Object());
    measureSize(new HashMap());
    measureSize(new LinkedHashMap());
    measureSize(new ReentrantReadWriteLock());
    measureSize(new byte[1000]);
    measureSize(new boolean[1000]);
    measureSize(new String("Hello World".toCharArray()));
    measureSize("Hello World");
    measureSize(10);
    measureSize(100);
    measureSize(1000);
    measureSize(new Parent());
    measureSize(new Kid());
    measureSize(Thread.State.TERMINATED); //

    System.out.println("======================");
    // testGrib1();
    System.out.println("======================");
    testGrib2();
  }

  private static class Parent {
    private int i;
    private boolean b;
    private long l;
  }

  private static class Kid extends Parent {
    private boolean b;
    private float f;
  }

  static void testHDF4() throws IOException {
    String filename = "C:/data/hdf4/MI1B2T_B54_O003734_AN_05.hdf";
    NetcdfFile ncfile = NetcdfFiles.open(filename);
    measureSize(filename, ncfile, null, true);
    ncfile.close();
  }

  static void testHDF5() throws IOException {
    String filename = "C:/data/hdf5/HIRDLS2-Aura12h_b033_2000d275.he5";
    NetcdfFile ncfile = NetcdfFiles.open(filename);
    // measureSize(filename, ncfile, null, true);

    Variable vv = ncfile.findVariable("HDFEOS/SWATHS/HIRDLS/Geolocation Fields/SpacecraftAltitude");
    measureSize(vv.getFullName(), vv, Group.class, true);

    // showAll(ncfile);
    ncfile.close();
  }

  static void testBufr() throws IOException {
    String filename = "C:/data/bufr/edition3/idd/profiler/PROFILER_3.bufr";
    NetcdfFile ncfile = NetcdfFiles.open(filename);
    measureSize(filename, ncfile, null, true);

    // Variable vv = ncfile.findVariable("HDFEOS/SWATHS/HIRDLS/Geolocation Fields/SpacecraftAltitude");
    // measureSize(vv.getName(), vv, Group.class, true);

    // showAll(ncfile);
    ncfile.close();
  }

  static void testGrib1() throws IOException {
    String filename = "D:\\data\\grib\\nam\\conus80/NAM_CONUS_80km_20060811_0000.grib1";
    NetcdfFile ncfile = NetcdfFiles.open(filename);
    measureSize(filename, ncfile, null, false);

    Variable vv = ncfile.findVariable("Absolute_vorticity");
    measureSize(vv.getFullName(), vv, Group.class, false);

    ncfile.close();
  }

  static void testGrib2() throws IOException {
    String filename = "D:/datasets/ncep/gfs/global0p5/GFS_Global_0p5deg_20060824_0000.grib2";
    NetcdfFile ncfile = NetcdfFiles.open(filename);
    measureSize(filename, ncfile, null, true);

    ncfile.close();
  }

  static void showAll(NetcdfFile ncfile) {
    for (Dimension dim : ncfile.getRootGroup().getDimensions())
      measureSize(dim.getShortName(), dim, Group.class, false);

    for (Variable v : ncfile.getVariables()) {
      measureSize(v.getFullName(), v, Group.class, false);
      for (Attribute att : v.attributes()) {
        measureSize(att.getShortName(), att, null, false);
      }
    }

    for (Attribute att : ncfile.getRootGroup().attributes()) {
      measureSize(att.getShortName(), att, null, false);
    }

    Group root = ncfile.getRootGroup();
    measureSize("rootGroup", root, null, false);
    for (Group g : root.getGroups())
      measureSize(g.getFullName(), g, null, false);
  }

  static void testN3() throws IOException {
    try (NetcdfFile ncfile = NetcdfDatasets.openFile("C:/data/test2.nc", null)) {
      measureSize("beforeRead", ncfile, null, true);
      for (Variable v : ncfile.getVariables()) {
        v.readArray();
      }
      measureSize("afterRead", ncfile, null, true);
    }
  }

  static void testNcml() throws IOException {
    String filename = "C:/dev/tds/thredds/cdm/src/test/data/ncml/aggUnionSimple.xml";
    try (NetcdfDataset ncfile = NetcdfDatasets.openDataset(filename, false, null)) {
      measureSize("aggUnionSimple", ncfile, null, true);
    }
  }

}
