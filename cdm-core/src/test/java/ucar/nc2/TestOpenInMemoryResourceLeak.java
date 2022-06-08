/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.unidata.util.test.TestDir;

/**
 * NetcdfFile.openInMemory(URI) and NetcdfFiles.openInMemory(URI) were leaking an open input stream.
 * This tests for a leak by trying to delete a file that's been opened in memory using the URI signature.
 *
 * See https://github.com/Unidata/netcdf-java/issues/166
 * Contributed by https://github.com/tayloj
 */
public class TestOpenInMemoryResourceLeak {

  // holds the tempFile Path object created before each test runs
  Path tempFile;

  /*
   * Before each test, create a copy of an existing netcdf file from the built-in test datasets.
   * We will try to delete this temp file after opening it in memory.
   */
  @Before
  public void makeFileToBeDeleted() throws IOException {
    tempFile = Files.createTempFile("cdmTest_", ".nc");
    Path ncfileToCopy = Paths.get(TestDir.cdmLocalTestDataDir + "jan.nc");
    Files.copy(ncfileToCopy, tempFile, StandardCopyOption.REPLACE_EXISTING);
  }

  /** Test for leak using NetcdfFile.openInMemory(URI) */
  @Test
  public void inputStreamNetcdfFileLeak() throws IOException {
    /*
     * Read the file into a NetcdfFile. try-with-resources ensures that the NetcdfFile's close()
     * method is called, so all resources with it are released.
     */
    try (NetcdfFile ncfile = NetcdfFiles.openInMemory(tempFile.toUri())) {
      // prove it's opened
      assertThat(ncfile.getFileTypeId()).isEqualTo("NetCDF-3");
    }

    /*
     * Try to delete the temp file with Files.delete(). When this fails, it will throw an exception like:
     *
     * java.nio.file.FileSystemException:
     * C:\Users\\username\AppData\Local\Temp\file8726442302596323190.nc: The process cannot access
     * the file because it is being used by another process.
     */
    Files.delete(tempFile);
  }

  /** Test for leak using NetcdfFiles.openInMemory(URI) */
  @Test
  public void inputStreamNetcdfFilesLeak() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.openInMemory(tempFile.toUri())) {
      // prove it's opened
      assertThat(ncfile.getFileTypeId()).isEqualTo("NetCDF-3");
    }

    Files.delete(tempFile);
  }

  /** Check that the temp file has been deleted after each test */
  @After
  public void reallyDeleted() {
    assertThat(Files.notExists(tempFile)).isTrue();
  }

}
