/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import ucar.nc2.*;
import ucar.nc2.constants.DataFormatType;
import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.grib.GribUtils;
import java.io.IOException;
import java.util.Formatter;

import ucar.unidata.io.RandomAccessFile;

/**
 * PartitionCollection for Grib1.
 *
 * @author caron
 * @since 2/21/14
 */
public class Grib1Partition extends PartitionCollectionImmutable {

  Grib1Partition(PartitionCollectionMutable pc) {
    super(pc);
  }

  @Override
  public ucar.nc2.dataset.NetcdfDataset getNetcdfDataset(Dataset ds, GroupGC group, String filename,
      FeatureCollectionConfig config, Formatter errlog, org.slf4j.Logger logger) throws IOException {

    ucar.nc2.grib.collection.Grib1Iosp iosp = new ucar.nc2.grib.collection.Grib1Iosp(group, ds.getType());
    RandomAccessFile raf = (RandomAccessFile) iosp.sendIospMessage(NetcdfFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE);
    NetcdfFile ncfile = NetcdfFiles.build(iosp, raf, getLocation(), null);
    return NetcdfDatasets.enhance(ncfile, NetcdfDataset.getDefaultEnhanceMode(), null);
  }

  @Override
  public GribIosp getIosp() throws IOException {
    GribIosp result = new Grib1Iosp(this);
    result.createCustomizer();
    return result;
  }

  @Override
  public void addGlobalAttributes(AttributeContainerMutable result) {
    String val = cust.getGeneratingProcessName(getGenProcessId());
    if (val != null)
      result.addAttribute(new Attribute(GribUtils.GEN_PROCESS, val));
    result.addAttribute(new Attribute(CDM.FILE_FORMAT, DataFormatType.GRIB1.getDescription()));
  }

  @Override
  public void addVariableAttributes(AttributeContainerMutable v, GribCollectionImmutable.VariableIndex vindex) {
    Grib1Collection.addVariableAttributes(v, vindex, this);
  }

  @Override
  public String makeVariableId(GribCollectionImmutable.VariableIndex v) {
    return Grib1Collection.makeVariableId(getCenter(), getSubcenter(), v.getTableVersion(), v.getParameter(),
        v.getLevelType(), v.isLayer(), v.getIntvType(), v.getIntvName());
  }

}
