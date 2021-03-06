/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import java.util.Iterator;

import static com.google.common.truth.Truth.assertThat;

@Category(NeedsCdmUnitTest.class)
public class TestOffAggDirectory {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testNcmlDirect() throws IOException {
    String filename = "file:" + TestDir.cdmUnitTestDir + "ncml/nc/seawifs/aggDirectory.ncml";

    try (NetcdfDataset ncfile = NetcdfDatasets.openDataset(filename, false, null)) {
      logger.debug(" TestNcmlAggDirectory.open {}", filename);
      testDimensions(ncfile);
      testCoordVar(ncfile);
      testAggCoordVar(ncfile);
      testReadData(ncfile);
    }
  }

  @Test
  public void testNcmlDataset() throws IOException {
    String filename = "file:" + TestDir.cdmUnitTestDir + "ncml/nc/seawifs/aggDirectory.ncml";

    try (NetcdfFile ncfile = NetcdfDatasets.openDataset(filename, true, null)) {
      logger.debug(" TestNcmlAggExisting.openDataset {}", filename);
      testDimensions(ncfile);
      testCoordVar(ncfile);
      testAggCoordVar(ncfile);
      testReadData2(ncfile);
    }
  }

  @Test
  public void testNcmlGrid() throws IOException {
    String filename = "file:" + TestDir.cdmUnitTestDir + "ncml/nc/seawifs/aggDirectory.ncml";
    System.out.printf("TestNcmlAggExisting.openGrid %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset ncd = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(ncd).isNotNull();
      assertThat(ncd.getGrids()).hasSize(2);
    }
  }

  private void testDimensions(NetcdfFile ncfile) {
    Dimension latDim = ncfile.findDimension("latitude");
    assert null != latDim;
    assert latDim.getShortName().equals("latitude");
    assert latDim.getLength() == 630;
    assert !latDim.isUnlimited();

    Dimension lonDim = ncfile.findDimension("longitude");
    assert null != lonDim;
    assert lonDim.getShortName().equals("longitude");
    assert lonDim.getLength() == 630;
    assert !lonDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("time");
    assert null != timeDim;
    assert timeDim.getShortName().equals("time");
    assert timeDim.getLength() == 6;
  }

  private void testCoordVar(NetcdfFile ncfile) throws IOException {
    Variable lat = ncfile.findVariable("latitude");
    assert lat.getArrayType() == ArrayType.FLOAT;
    assert lat.getDimension(0).equals(ncfile.findDimension("latitude"));

    Attribute att = lat.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getArrayType() == ArrayType.STRING;
    assert att.getStringValue().equals("degree_N");

    Array data = lat.readArray();
    assert data.getRank() == 1;
    assert data.getSize() == 630;
    assert data.getShape()[0] == 630;
    Iterator<Float> dataI = data.iterator();

    assertThat(Misc.nearlyEquals(dataI.next(), 43.0f)).isTrue();
    assertThat(Misc.nearlyEquals(dataI.next(), 43.01045f)).isTrue();
    assertThat(Misc.nearlyEquals(dataI.next(), 43.020893f)).isTrue();
  }

  private void testAggCoordVar(NetcdfFile ncfile) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getShortName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == 6;
    assert time.getShape()[0] == 6;
    assert time.getArrayType() == ArrayType.FLOAT;

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array data = time.readArray();
    assert data.getRank() == 1;
    assert data.getSize() == 6;
    assert data.getShape()[0] == 6;
    Iterator<Float> dataI = data.iterator();

    float vals[] = {890184.0f, 890232.0f, 890256.0f, 890304.0f, 890352.0f, 890376.0f};
    int count = 0;
    while (dataI.hasNext()) {
      assertThat(Misc.nearlyEquals(dataI.next(), vals[count++])).isTrue();
    }
  }

  private void testReadData(NetcdfFile ncfile) throws IOException {
    Variable v = ncfile.findVariable("chlorophylle_a");
    assert null != v;
    assert v.getShortName().equals("chlorophylle_a");
    assert v.getRank() == 3;
    assert v.getShape()[0] == 6;
    assert v.getShape()[1] == 630;
    assert v.getShape()[2] == 630;
    assert v.getArrayType() == ArrayType.SHORT;

    assert !v.isCoordinateVariable();

    assert v.getDimension(0) == ncfile.findDimension("time");
    assert v.getDimension(1) == ncfile.findDimension("latitude");
    assert v.getDimension(2) == ncfile.findDimension("longitude");

    Array<Short> data = (Array<Short>) v.readArray();
    assert data.getRank() == 3;
    assert data.getShape()[0] == 6;
    assert data.getShape()[1] == 630;
    assert data.getShape()[2] == 630;

    short[] vals = {32767, 32767, 20, 32767, 20, 20};
    int[] shape = data.getShape();
    Index tIndex = data.getIndex();
    for (int i = 0; i < shape[0]; i++) {
      short val = data.get(tIndex.set(i, 133, 133));
      assertThat(Misc.nearlyEquals(vals[i], val)).isTrue();
    }
  }

  private void testReadData2(NetcdfFile ncfile) throws IOException {
    Variable v = ncfile.findVariable("chlorophylle_a");
    assert null != v;
    assert v.getShortName().equals("chlorophylle_a");
    assert v.getRank() == 3;
    assert v.getShape()[0] == 6;
    assert v.getShape()[1] == 630;
    assert v.getShape()[2] == 630;
    assert v.getArrayType() == ArrayType.DOUBLE;

    assert !v.isCoordinateVariable();

    assert v.getDimension(0) == ncfile.findDimension("time");
    assert v.getDimension(1) == ncfile.findDimension("latitude");
    assert v.getDimension(2) == ncfile.findDimension("longitude");

    Array<Double> data = (Array<Double>) v.readArray();
    assert data.getRank() == 3;
    assert data.getShape()[0] == 6;
    assert data.getShape()[1] == 630;
    assert data.getShape()[2] == 630;

    double[] vals = {Double.NaN, Double.NaN, .20, Double.NaN, .20, .20};
    int[] shape = data.getShape();
    Index tIndex = data.getIndex();
    for (int i = 0; i < shape[0]; i++) {
      double val = data.get(tIndex.set(i, 133, 133));
      if (Double.isNaN(val))
        assert Double.isNaN(vals[i]);
      else
        assertThat(Misc.nearlyEquals(vals[i], val)).isTrue();
    }
  }

  @Test
  public void testBlanksInDirectory() throws IOException {
    String dir = TestDir.cdmUnitTestDir + "encoding/";
    String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n"
        + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n"
        + " <aggregation type='joinNew' dimName='fake'>\n" + "  <netcdf location='" + dir
        + "dir mit blank/20070101.nc' coord='1'/>\n" + "  <netcdf location='" + dir
        + "dir mit blank/20070301.nc' coord='2'/>\n" + " </aggregation>\n" + "</netcdf> ";
    try (NetcdfDataset ncfile = NetcdfDatasets.openNcmlDataset(new StringReader(ncml), null, null)) {
      logger.debug("result={}", ncfile);
    }
  }
}
