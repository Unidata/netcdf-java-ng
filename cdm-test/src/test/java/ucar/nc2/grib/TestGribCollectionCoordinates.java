/*
 * Copyright (c) 1998-2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.CollectionUpdateType;
import ucar.nc2.Variable;
import ucar.nc2.dataset.*;
import ucar.nc2.grib.collection.*;
import ucar.nc2.util.DebugFlags;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test GribCollection Coordinates
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribCollectionCoordinates {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static CollectionUpdateType updateMode = CollectionUpdateType.always;

  @BeforeClass
  static public void before() throws IOException {
    GribIosp.debugIndexOnlyCount = 0;
    GribCollectionImmutable.countGC = 0;
    PartitionCollectionImmutable.countPC = 0;
    RandomAccessFile.enableDefaultGlobalFileCache();
    RandomAccessFile.setDebugLeaks(true);
    Grib.setDebugFlags(DebugFlags.create("Grib/indexOnly"));
    GribCdmIndex.setGribCollectionCache(new ucar.nc2.internal.cache.FileCacheGuava("GribCollectionCacheGuava", 100));
    GribCdmIndex.gribCollectionCache.resetTracking();
  }

  @AfterClass
  static public void after() {
    Grib.setDebugFlags(DebugFlags.create(""));
    /*
     * Formatter out = new Formatter(System.out);
     * 
     * FileCacheIF cache = GribCdmIndex.gribCollectionCache;
     * if (cache != null) {
     * cache.showTracking(out);
     * cache.showCache(out);
     * cache.clearCache(false);
     * }
     * 
     * FileCacheIF rafCache = RandomAccessFile.getGlobalFileCache();
     * if (rafCache != null) {
     * rafCache.showCache(out);
     * }
     * 
     * System.out.printf("            countGC=%7d%n", GribCollectionImmutable.countGC);
     * System.out.printf("            countPC=%7d%n", PartitionCollectionImmutable.countPC);
     * System.out.printf("    countDataAccess=%7d%n", GribIosp.debugIndexOnlyCount);
     * System.out.printf(" total files needed=%7d%n", GribCollectionImmutable.countGC +
     * PartitionCollectionImmutable.countPC + GribIosp.debugIndexOnlyCount);
     * 
     * FileCache.shutdown();
     * RandomAccessFile.setGlobalFileCache(null);
     * TestDir.checkLeaks();
     * RandomAccessFile.setDebugLeaks(false);
     */
  }

  /////////////////////////////////////////////////////////

  // check that all time variables are coordinates (TwoD PofP was not eliminating unused coordinates after merging)
  @Test
  public void testExtraCoordinates() throws IOException {
    Grib.setDebugFlags(DebugFlags.create("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config =
        new FeatureCollectionConfig("namAlaska22", "test/namAlaska22", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "gribCollections/namAlaska22/.*gbx9", null, null, null, "file", null);
    config.gribConfig.setExcludeZero(true); // no longer the default

    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    String topLevelIndex = GribCdmIndex.getTopIndexFileFromConfig(config).getAbsolutePath();

    System.out.printf("changed = %s%n", changed);

    boolean ok = true;

    try (NetcdfDataset ds = NetcdfDatasets.openDataset(topLevelIndex)) {
      for (Variable vds : ds.getVariables()) {
        String stdname = vds.findAttributeString("standard_name", "no");
        if (!stdname.equalsIgnoreCase("time"))
          continue;

        System.out.printf(" %s == %s%n", vds.getFullName(), vds.getClass().getName());
        assertThat(vds).isInstanceOf(CoordinateAxis.class);

        // test that zero Intervals are removed
        if (vds instanceof CoordinateAxis1D) {
          CoordinateAxis1D axis = (CoordinateAxis1D) vds;
          if (axis.isInterval()) {
            for (int i = 0; i < axis.getSize(); i++) {
              double[] bound = axis.getCoordBounds(i);
              if (bound[0] == bound[1]) {
                System.out.printf("ERR1 %s(%d) = [%f,%f]%n", vds.getFullName(), i, bound[0], bound[1]);
                ok = false;
              }
            }
          }
        }
      }
    }

    assertThat(ok).isTrue();
  }

  // make sure Best reftimes always increase
  @Test
  public void testBestReftimeMonotonic() throws IOException {
    FeatureCollectionConfig config =
        new FeatureCollectionConfig("gfs_2p5deg", "test/gfs_2p5deg", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/.*grib2", null, null, null, "file", null);

    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
    String topLevelIndex = GribCdmIndex.getTopIndexFileFromConfig(config).getAbsolutePath();
    boolean ok = true;

    try (NetcdfDataset ds = NetcdfDatasets.openDataset(topLevelIndex)) {
      for (Variable vds : ds.getVariables()) {
        String stdname = vds.findAttributeString("standard_name", "no");
        if (!stdname.equalsIgnoreCase("forecast_reference_time"))
          continue;

        System.out.printf(" %s == %s%n", vds.getFullName(), vds.getClass().getName());
        assertThat(vds).isInstanceOf(CoordinateAxis1D.class);
        CoordinateAxis1D axis = (CoordinateAxis1D) vds;

        // test that values are monotonic
        double last = Double.NaN;
        for (int i = 0; i < axis.getSize(); i++) {
          double val = axis.getCoordValue(i);
          if (i > 0 && (val < last)) {
            System.out.printf("  %s(%d) == %f < %f%n", vds.getFullName(), i, val, last);
            ok = false;
          }
          last = val;
        }
      }
    }

    assertThat(ok).isTrue();
  }



}
