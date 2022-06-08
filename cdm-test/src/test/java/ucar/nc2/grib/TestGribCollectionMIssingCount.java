/*
 * Copyright (c) 1998 - 2014. University Corporation for Atmospheric Research/Unidata
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation. Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.grib;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.grib.collection.*;
import ucar.nc2.util.DebugFlags;
import ucar.nc2.internal.cache.FileCache;
import ucar.nc2.internal.cache.FileCacheIF;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/**
 * Look for missing data in Grib Collections.
 *
 * Indicates that coordinates are not matching, because DGEX_CONUS is dense (has data for each coordinate).
 * Note that not all grib collections will be dense.
 *
 * @author John
 * @since 10/13/2014
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribCollectionMIssingCount {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @BeforeClass
  static public void before() {
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
    Formatter out = new Formatter(System.out);

    FileCacheIF cache = GribCdmIndex.gribCollectionCache;
    if (cache != null) {
      cache.showTracking(out);
      cache.showCache(out);
      cache.clearCache(false);
    }

    FileCacheIF rafCache = RandomAccessFile.getGlobalFileCache();
    if (rafCache != null) {
      rafCache.showCache(out);
    }

    System.out.printf("            countGC=%7d%n", GribCollectionImmutable.countGC);
    System.out.printf("            countPC=%7d%n", PartitionCollectionImmutable.countPC);
    System.out.printf("    countDataAccess=%7d%n", GribIosp.debugIndexOnlyCount);
    System.out.printf(" total files needed=%7d%n",
        GribCollectionImmutable.countGC + PartitionCollectionImmutable.countPC + GribIosp.debugIndexOnlyCount);

    FileCache.shutdown();
    RandomAccessFile.setGlobalFileCache(null);
    TestDir.checkLeaks();
    RandomAccessFile.setDebugLeaks(false);
  }

  @Test
  public void testGC() {
    GribCollectionMissing.Count count = GribCollectionMissing
        .read(TestDir.cdmUnitTestDir + "gribCollections/dgex/20141011/DGEX_CONUS_12km_20141011_1800.grib2.ncx4");
    assertThat(count.nread).isEqualTo(1009);
    assertThat(count.nmiss).isEqualTo(0);
    assertThat(count.nerrs).isEqualTo(0);
  }

  @Test
  public void testPofG() {
    GribCollectionMissing.Count count =
        GribCollectionMissing.read(TestDir.cdmUnitTestDir + "gribCollections/dgex/20141011/dgex_46-20141011.ncx4");
    assertThat(count.nread).isEqualTo(2018);
    assertThat(count.nmiss).isEqualTo(0);
    assertThat(count.nerrs).isEqualTo(0);
  }

  @Test
  public void testPofP() {
    RandomAccessFile.setDebugLeaks(true);
    GribCollectionMissing.Count count =
        GribCollectionMissing.read(TestDir.cdmUnitTestDir + "gribCollections/dgex/dgex_46.ncx4");
    TestDir.checkLeaks();
    assertThat(count.nread).isEqualTo(4036);
    assertThat(count.nmiss).isEqualTo(0);
    assertThat(count.nerrs).isEqualTo(0);
  }

  @Test
  public void testGC_Grib2() {
    GribCollectionMissing.Count count = GribCollectionMissing
        .read(TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/GFS_Global_2p5deg_20150301_1200.grib2.ncx4");

    System.out.printf("%n%50s == %d/%d/%d%n", "total", count.nerrs, count.nmiss, count.nread);
    assertThat(count.nread).isEqualTo(28971);
    assertThat(count.nmiss).isEqualTo(596);
    assertThat(count.nerrs).isEqualTo(0);
  }

  @Test
  public void testPofG_Grib2() {
    GribCollectionMissing.Count count =
        GribCollectionMissing.read(TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4");

    System.out.printf("%n%50s == %d/%d/%d%n", "total", count.nerrs, count.nmiss, count.nread);
    assertThat(count.nread).isEqualTo(130953);
    assertThat(count.nmiss).isEqualTo(5023); // rectMissing, not just missing
    assertThat(count.nerrs).isEqualTo(0);
  }

  //// ncss/GFS/CONUS_80km/GFS_CONUS_80km-CONUS_80km.ncx4 has lots of missing records
  @Test
  public void testGC_Grib1() {
    GribCollectionMissing.Count count = GribCollectionMissing
        .read(TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/20141024/GFS_CONUS_80km_20141024_0000.grib1.ncx4");

    System.out.printf("%n%50s == %d/%d/%d%n", "total", count.nerrs, count.nmiss, count.nread);
    assertThat(count.nread).isEqualTo(6969);
    assertThat(count.nmiss).isEqualTo(153);
    assertThat(count.nerrs).isEqualTo(0);
  }

  @Test
  public void testMRC() {
    GribCollectionMissing.Count count = GribCollectionMissing
        .read(TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/20141024/gfsConus80_dir-20141024.ncx4");

    System.out.printf("%n%50s == %d/%d/%d%n", "total", count.nerrs, count.nmiss, count.nread);
    assertThat(count.nread).isEqualTo(27876);
    assertThat(count.nmiss).isEqualTo(612);
    assertThat(count.nerrs).isEqualTo(0);
  }

  @Test
  public void testPofP_Grib1() {
    GribCollectionMissing.Count count =
        GribCollectionMissing.read(TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/gfsConus80_dir.ncx4");

    System.out.printf("%n%50s == %d/%d/%d%n", "total", count.nerrs, count.nmiss, count.nread);
    assertThat(count.nread).isEqualTo(41814);
    assertThat(count.nmiss).isEqualTo(918);
    assertThat(count.nerrs).isEqualTo(0);
  }

}
