/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Formatter;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.regions.Region;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.unidata.io.InMemoryRandomAccessFile;
import ucar.unidata.io.RandomAccessFile;
import ucar.nc2.internal.http.RafHttp;
import ucar.unidata.io.s3.S3RandomAccessFile;
import ucar.unidata.util.test.category.NeedsExternalResource;

public class TestNetcdfFilesS3Rafs {

  private final String s3Bucket = "noaa-goes16";
  private final String s3Key =
      "ABI-L1b-RadM/2017/241/23/OR_ABI-L1b-RadM1-M3C11_G16_s20172412359247_e20172412359304_c20172412359341.nc";
  private final String s3uri = "cdms3:" + s3Bucket + "?" + s3Key;

  private final String baseHttpLocation = "noaa-goes16.s3.amazonaws.com/" + s3Key;
  private final String inMemLocation = "slurp://" + baseHttpLocation;
  private final String httpsLocation = "https://" + baseHttpLocation;

  @BeforeClass
  public static void setup() {
    System.setProperty("aws.region", Region.US_EAST_1.toString());
  }

  @Test
  @Category(NeedsExternalResource.class)
  public void testHttpsRaf() throws IOException {
    System.out.printf("Open %s%n", httpsLocation);
    try (NetcdfFile ncf = NetcdfFiles.open(httpsLocation)) {
      Object raf = ncf.sendIospMessage(NetcdfFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE);
      assertThat(raf).isInstanceOf(RafHttp.class);
    }
  }

  @Test
  @Category(NeedsExternalResource.class)
  public void testRedirectedHttpRaf() throws IOException {
    String httpLocation = "http://" + baseHttpLocation;
    System.out.printf("Open %s%n", httpLocation);
    try (NetcdfFile ncf = NetcdfFiles.open(httpLocation)) {
      Object raf = ncf.sendIospMessage(NetcdfFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE);
      assertThat(raf).isInstanceOf(RafHttp.class);
    }
  }

  @Test
  @Category(NeedsExternalResource.class)
  public void testInMemoryRaf() throws IOException {
    System.out.printf("Open %s%n", inMemLocation);
    try (NetcdfFile ncf = NetcdfFiles.open(inMemLocation)) {
      Object raf = ncf.sendIospMessage(NetcdfFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE);
      assertThat(raf).isInstanceOf(InMemoryRandomAccessFile.class);
    }
  }

  @Test
  @Category(NeedsExternalResource.class)
  public void compareRafs() throws IOException {
    // download a local copy of the file
    File tempFile = File.createTempFile("ncj-", null);
    tempFile.deleteOnExit();
    FileUtils.copyURLToFile(new URL(httpsLocation), tempFile);

    // open using three different RAFs
    try (NetcdfFile local = NetcdfFiles.open(tempFile.getCanonicalPath());
        NetcdfFile inMem = NetcdfFiles.open(inMemLocation);
        NetcdfFile http = NetcdfFiles.open(httpsLocation);
        NetcdfFile s3 = NetcdfFiles.open(s3uri)) {

      // check that expected RAFs are used
      Object raf = local.sendIospMessage(NetcdfFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE);
      assertThat(raf).isInstanceOf(RandomAccessFile.class);
      raf = inMem.sendIospMessage(NetcdfFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE);
      assertThat(raf).isInstanceOf(InMemoryRandomAccessFile.class);
      raf = http.sendIospMessage(NetcdfFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE);
      assertThat(raf).isInstanceOf(RafHttp.class);
      raf = s3.sendIospMessage(NetcdfFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE);
      assertThat(raf).isInstanceOf(S3RandomAccessFile.class);

      // compare at a NetcdfFile level
      // CompareNetcdf(showCompare, showEach, compareData)
      Formatter f = new Formatter();
      CompareNetcdf2 comparer = new CompareNetcdf2(f, false, false, true);

      assertThat(comparer.compare(local, inMem)).isTrue();
      assertThat(comparer.compare(local, http)).isTrue();
      assertThat(comparer.compare(local, s3)).isTrue();
    }
  }

  @Test
  @Category(NeedsExternalResource.class)
  public void testS3Raf() throws IOException {
    System.out.printf("Open %s%n", s3uri);
    try (NetcdfFile ncf = NetcdfFiles.open(s3uri)) {
      Object raf = ncf.sendIospMessage(NetcdfFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE);
      assertThat(raf).isInstanceOf(S3RandomAccessFile.class);
    }
  }

  @AfterClass
  public static void cleanup() {
    System.clearProperty("aws.region");
  }
}
