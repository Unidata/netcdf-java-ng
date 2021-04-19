/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import java.io.IOException;

import ucar.array.Index;
import ucar.array.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.internal.util.CompareArrayToArray;

import static com.google.common.truth.Truth.assertThat;

/** Test NetcdfFormatWriter */
public class TestNetcdfFormatWriter {
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  /*
   * byte Band1(y, x);
   * > Band1:_Unsigned = "true";
   * > Band1:_FillValue = -1b; // byte
   * >
   * > byte Band2(y, x);
   * > Band2:_Unsigned = "true";
   * > Band2:valid_range = 0s, 254s; // short
   */
  @Test
  public void testUnsignedAttribute() throws IOException, InvalidRangeException {
    String filename = tempFolder.newFile().getAbsolutePath();

    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(filename);
    writerb.addUnlimitedDimension("time");

    writerb.addVariable("time", ArrayType.BYTE, "time").addAttribute(new Attribute(CDM.UNSIGNED, "true"))
        .addAttribute(new Attribute(CDM.SCALE_FACTOR, 10.0))
        .addAttribute(Attribute.builder(CDM.VALID_RANGE).setValues(ImmutableList.of(10, 240), false).build());

    /*
     * byte Band1(y, x);
     * > Band1:_Unsigned = "true";
     * > Band1:_FillValue = -1b; // byte
     */
    writerb.addVariable("Band1", ArrayType.BYTE, "time").addAttribute(new Attribute(CDM.UNSIGNED, "true"))
        .addAttribute(new Attribute(CDM.FILL_VALUE, (byte) -1)).addAttribute(new Attribute(CDM.SCALE_FACTOR, 1.0));

    /*
     * byte Band2(y, x);
     * > Band2:_Unsigned = "true";
     * > Band2:valid_range = 0s, 254s; // short
     */
    writerb.addVariable("Band2", ArrayType.BYTE, "time").addAttribute(new Attribute(CDM.UNSIGNED, "true"))
        .addAttribute(new Attribute(CDM.SCALE_FACTOR, 1.0)).addAttribute(
            Attribute.builder(CDM.VALID_RANGE).setValues(ImmutableList.of((short) 0, (short) 254), false).build());

    try (NetcdfFormatWriter writer = writerb.build()) {
      ucar.array.Index index = ucar.array.Index.ofRank(1);
      byte[] data = new byte[1];
      for (int time = 0; time < 256; time++) {
        data[0] = (byte) time;
        writer.writeOriginPrimitive("time", index, data, 1);
        writer.writeOriginPrimitive("Band1", index, data, 1);
        writer.writeOriginPrimitive("Band2", index, data, 1);
        index.incr(0);
      }
    }

    Array<Byte> expected = Arrays.makeArray(ArrayType.BYTE, 256, 0, 1);
    try (NetcdfFile ncFile = NetcdfFiles.open(filename)) {
      Array<?> time = ncFile.readSectionArray("time");
      assertThat(CompareArrayToArray.compareData("time", time, expected)).isTrue();
      Array<?> Band1 = ncFile.readSectionArray("Band1");
      assertThat(CompareArrayToArray.compareData("time", Band1, expected)).isTrue();
      Array<?> Band2 = ncFile.readSectionArray("Band2");
      assertThat(CompareArrayToArray.compareData("time", Band2, expected)).isTrue();
    }
  }

  @Test
  public void testWriteUnlimited() throws IOException, InvalidRangeException {
    String filename = tempFolder.newFile().getAbsolutePath();

    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(filename);
    writerb.addUnlimitedDimension("time");
    writerb.addAttribute(new Attribute("name", "value"));

    // public Variable addVariable(Group g, String shortName, ArrayType dataType, String dims) {
    writerb.addVariable("time", ArrayType.DOUBLE, "time");

    // write
    try (NetcdfFormatWriter writer = writerb.build()) {
      Array<?> data = Arrays.makeArray(ArrayType.DOUBLE, 4, 0, 1);
      Variable time = writer.findVariable("time"); // ?? immutable ??
      assertThat(time).isNotNull();
      writer.write(time, data);
      assertThat(time.getSize()).isEqualTo(4);
    }

    // read it back
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      Variable vv = ncfile.findVariable("time");
      assertThat(vv).isNotNull();
      assertThat(vv.getSize()).isEqualTo(4);

      Array<?> expected = Arrays.makeArray(ArrayType.DOUBLE, 4, 0, 1);
      Array<?> data = vv.readArray();
      assertThat(CompareArrayToArray.compareData("time", data, expected)).isTrue();
    }
  }

  @Test
  public void testWriteRecordOneAtaTime() throws IOException, InvalidRangeException {
    String filename = tempFolder.newFile().getAbsolutePath();

    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(filename);
    // define dimensions, including unlimited
    Dimension latDim = writerb.addDimension("lat", 3);
    Dimension lonDim = writerb.addDimension("lon", 4);
    writerb.addDimension(Dimension.builder().setName("time").setIsUnlimited(true).build());

    // define Variables
    writerb.addVariable("lat", ArrayType.FLOAT, "lat").addAttribute(new Attribute("units", "degrees_north"));
    writerb.addVariable("lon", ArrayType.FLOAT, "lon").addAttribute(new Attribute("units", "degrees_east"));
    writerb.addVariable("rh", ArrayType.INT, "time lat lon")
        .addAttribute(new Attribute("long_name", "relative humidity")).addAttribute(new Attribute("units", "percent"));
    writerb.addVariable("T", ArrayType.DOUBLE, "time lat lon")
        .addAttribute(new Attribute("long_name", "surface temperature")).addAttribute(new Attribute("units", "degC"));
    writerb.addVariable("time", ArrayType.INT, "time").addAttribute(new Attribute("units", "hours since 1990-01-01"));

    try (NetcdfFormatWriter writer = writerb.build()) {
      // write out the non-record variables
      writer.writeWithPrimitive("lat", new float[] {41, 40, 39});
      writer.writeWithPrimitive("lon", new float[] {-109, -107, -105, -103});

      Index timeOrigin = Index.ofRank(1);
      Index recordOrigin = Index.ofRank(3);

      Variable timeVar = writer.findVariable("time");
      Preconditions.checkNotNull(timeVar);

      // write 10 records
      int[] timeValue = new int[1];
      for (int timeIdx = 0; timeIdx < 10; timeIdx++) {
        timeValue[0] = timeIdx;

        // 12 values in one record
        Array<?> rhData = Arrays.makeArray(ArrayType.INT, 12, 0, 1 + timeIdx, 1, 3, 4);
        Array<?> tData = Arrays.makeArray(ArrayType.DOUBLE, 12, 99 * timeIdx, (1 + timeIdx) / 3.14159, 1, 3, 4);

        // write the data
        writer.write("T", recordOrigin, tData);
        writer.write("rh", recordOrigin, rhData);
        writer.writeOriginPrimitive(timeVar, timeOrigin, timeValue, 1);

        timeOrigin.incr(0);
        recordOrigin.incr(0);
      } // loop over record
    }

    // read it back
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      Variable time = ncfile.getRootGroup().findVariableLocal("time");
      assertThat(time).isNotNull();
      assertThat(time.getSize()).isEqualTo(10);

      Array<?> expected = Arrays.makeArray(ArrayType.INT, 12, 0, 1);
      Array<?> data = time.readArray();
      assertThat(CompareArrayToArray.compareData("time", data, expected)).isTrue();
    }
  }

  // fix for bug introduced 2/9/10, reported by Christian Ward-Garrison cwardgar@usgs.gov
  @Test
  public void testRecordSizeBug() throws IOException, InvalidRangeException {
    String filename = tempFolder.newFile().getAbsolutePath();
    int size = 10;

    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(filename).setFill(false);
    writerb.addUnlimitedDimension("time");
    writerb.addVariable("time", ArrayType.INT, "time").addAttribute(new Attribute("units", "hours since 1990-01-01"));

    try (NetcdfFormatWriter writer = writerb.build()) {
      Index timeOrigin = Index.ofRank(1);

      for (int time = 0; time < size; time++) {
        Array<?> timeData = Arrays.factory(ArrayType.INT, new int[] {1}, new int[] {time * 12});
        writer.write("time", timeOrigin, timeData);
        timeOrigin.incr(0);
      }
    }

    try (NetcdfFile ncFile = NetcdfFiles.open(filename)) {
      Array<?> result = ncFile.readSectionArray("time");
      assertThat(result.show()).isEqualTo("0, 12, 24, 36, 48, 60, 72, 84, 96, 108");
    }
  }

  @Test
  public void testStringWriting() throws IOException, ucar.array.InvalidRangeException {
    String filename = tempFolder.newFile().getAbsolutePath();
    int strlen = 25;

    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(filename).setFill(false);
    writerb.addDimension("len", strlen);
    writerb.addUnlimitedDimension("time");
    writerb.addVariable("time", ArrayType.CHAR, "time len");

    try (NetcdfFormatWriter writer = writerb.build()) {
      Variable time = writer.findVariable("time");
      assertThat(time).isNotNull();

      ucar.array.Index index = ucar.array.Index.ofRank(time.getRank());
      writer.writeStringData(time, index, "This is the first string.");
      writer.writeStringData(time, index, "Shorty");
      writer.writeStringData(time, index, "This is too long so it will get truncated");
    }

    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      Variable time = ncfile.findVariable("time");
      assertThat(time).isNotNull();
      ucar.array.Array<?> timeData = time.readArray();

      assertThat(timeData.getArrayType()).isEqualTo(ArrayType.CHAR);
      ucar.array.ArrayChar timecData = (ucar.array.ArrayChar) timeData;
      ucar.array.Array<String> achar3Data = timecData.makeStringsFromChar();

      assertThat(achar3Data.get(0)).isEqualTo("This is the first string.");
      assertThat(achar3Data.get(1)).isEqualTo("Shorty");
      assertThat(achar3Data.get(2)).isEqualTo("This is too long so it wi");
      assertThat(achar3Data.get(2)).hasLength(strlen);
    }
  }
}
