/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.ncml;

import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;

/** Test TestNcml - modifications aggregation features. */
public class TestAggModify {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  String ncml = "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" // leavit
      + "  <remove type='variable' name='P'/>\n" // leavit
      + "  <aggregation dimName='time' type='joinExisting'>\n" // leavit
      + "    <netcdf location='file:src/test/data/ncml/nc/jan.nc'/>\n" // leavit
      + "    <netcdf location='file:src/test/data/ncml/nc/feb.nc'/>\n" // leavit
      + "  </aggregation>\n" // leavit
      + "</netcdf>"; // leavit

  @Test
  public void testWithDateFormatMark() throws Exception {
    System.out.printf("ncml=%s%n", ncml);
    String filename = "file:" + TestNcmlRead.topDir + "testAggModify.ncml";
    NetcdfDataset ncfile = NetcdfDatasets.openNcmlDataset(new StringReader(ncml), filename, null);
    System.out.println(" TestNcmlAggExisting.open " + filename + "\n" + ncfile);

    Variable v = ncfile.findVariable("T");
    assert null != v;

    v = ncfile.findVariable("P");
    assert null == v;

    ncfile.close();
  }


}
