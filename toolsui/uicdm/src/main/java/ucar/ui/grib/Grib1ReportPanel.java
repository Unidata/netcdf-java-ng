/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.grib;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import thredds.inventory.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.grib.GribData;
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.grib.grib1.*;
import ucar.nc2.Attribute;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib1.tables.Grib1ParamTableReader;
import ucar.nc2.grib.grib1.tables.Grib1ParamTables;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.ui.ReportPanel;
import ucar.nc2.internal.util.Counters;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;
import java.io.*;
import java.util.*;

/**
 * Run through collections of Grib 1 files and make reports
 *
 * @author John
 * @since 8/28/11
 */
public class Grib1ReportPanel extends ReportPanel {

  public enum Report {
    checkTables, showLocalParams, summary, rename, showEncoding, gribIndex, gds
  }

  private Grib1Customizer cust;

  public Grib1ReportPanel(PreferencesExt prefs) {
    super(prefs);
  }

  @Override
  public Object[] getOptions() {
    return Grib1ReportPanel.Report.values();
  }

  @Override
  protected void doReport(Formatter f, Object option, MCollection dcm, boolean useIndex, boolean eachFile,
      boolean extra) throws IOException {
    cust = null;

    switch ((Report) option) {
      case checkTables:
        doCheckTables(f, dcm, useIndex);
        break;
      case gds:
        doUniqueGds(f, dcm, useIndex);
        break;
      case showLocalParams:
        doCheckLocalParams(f, dcm, useIndex);
        break;
      case summary:
        doScanIssues(f, dcm, useIndex, eachFile, extra);
        break;
      case rename:
        doRename(f, dcm, useIndex);
        break;
      case showEncoding:
        doShowEncoding(f, dcm);
        break;
      case gribIndex:
        doGribIndex(f, dcm, eachFile);
        break;
    }
  }

  private Grib1Index createIndex(MFile mf) throws IOException {
    String path = mf.getPath();
    Grib1Index index = new Grib1Index();
    if (!index.readIndex(path, mf.getLastModified())) {
      // make sure its a grib1 file
      try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
        if (!Grib1RecordScanner.isValidFile(raf))
          return null;
        index.makeIndex(path, raf);
      }
    }
    return index;
  }

  /////////////////////////////////////////////////////////////////

  private void doGribIndex(Formatter f, MCollection dcm, boolean eachFile) throws IOException {
    Counters counters = new Counters();

    // must open collection again without gbx filtering
    try (MCollection dcm2 = getCollectionUnfiltered(spec, f)) {

      for (MFile mfile : dcm2.getFilesSorted()) {
        String path = mfile.getPath();
        f.format(" %s%n", path);
        doGribIndex(f, mfile, counters, eachFile);
      }
    }

    counters.show(f);
  }

  private void doGribIndex(Formatter fm, MFile ff, Counters counters, boolean eachFile) {
    String path = ff.getPath();
    Grib1Index g1idx = new Grib1Index();
    g1idx.readIndex(path, 0, thredds.inventory.CollectionUpdateType.nocheck);
    counters.count("GDS", g1idx.getGds().size());

    // count unique hash
    Set<Integer> gdsHash = new HashSet<>();
    for (Grib1SectionGridDefinition gdss : g1idx.getGds()) {
      gdsHash.add(gdss.getGDS().hashCode());
    }
    counters.count("GDShashes", gdsHash.size());
    if (eachFile) {
      fm.format("   count=%d countHash=%d%n", g1idx.getGds().size(), gdsHash.size());
    }
  }

  ///////////////////////////////////////////////

  private void doCheckLocalParams(Formatter f, MCollection dcm, boolean useIndex) throws IOException {
    f.format("Check Grib-1 Parameter Tables for local entries%n");
    int[] accum = new int[4];

    for (MFile mfile : dcm.getFilesSorted()) {
      String path = mfile.getPath();
      f.format("%n %s%n", path);
      try {
        doCheckLocalParams(mfile, f, accum);
      } catch (Throwable t) {
        System.out.printf("FAIL on %s%n", mfile.getPath());
        t.printStackTrace();
      }
    }

    f.format("%nGrand total=%d local = %d missing = %d%n", accum[0], accum[2], accum[3]);
  }

  private void doCheckLocalParams(MFile ff, Formatter fm, int[] accum) throws IOException {
    int local = 0;
    int miss = 0;
    int nonop = 0;
    int total = 0;

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(ff.getPath(), errlog)) {
      Attribute gatt = gds.attributes().findAttribute("GRIB table");
      if (gatt != null) {
        String[] s = gatt.getStringValue().split("-");
        Grib1ParamTableReader gtable = new Grib1ParamTables().getParameterTable(Integer.parseInt(s[0]),
            Integer.parseInt(s[1]), Integer.parseInt(s[2]));
        fm.format("  %s == %s%n", gatt, gtable.getPath());
      }
      for (Grid dt : gds.getGrids()) {
        String currName = dt.getName();
        total++;

        Attribute att = dt.attributes().findAttributeIgnoreCase("Grib_Parameter");
        int number = (att == null) ? 0 : att.getNumericValue().intValue();
        if (number >= 128) {
          fm.format("  local parameter = %s (%d) units=%s %n", currName, number, dt.getUnits());
          local++;
          if (currName.startsWith("VAR"))
            miss++;
        }
      }
    }

    fm.format("total=%d local = %d miss=%d %n", total, local, miss);
    accum[0] += total;
    accum[1] += nonop;
    accum[2] += local;
    accum[3] += miss;
  }

  /////////////////////////////////////////////////////////////////

  // Look through the collection and find what GDS templates are used.
  private void doCheckTables(Formatter f, MCollection dcm, boolean useIndex) throws IOException {
    Counters counters = new Counters();

    for (MFile mfile : dcm.getFilesSorted()) {
      String path = mfile.getPath();
      f.format(" %s%n", path);
      if (useIndex)
        doCheckTablesWithIndex(f, mfile, counters);
      else
        doCheckTablesNoIndex(f, mfile, counters);
    }

    f.format("Check Parameter Tables%n");
    counters.show(f);
  }

  private void doCheckTablesWithIndex(Formatter mf, MFile ff, Counters counters) throws IOException {
    Grib1Index index = createIndex(ff);
    if (index == null)
      return;
    for (ucar.nc2.grib.grib1.Grib1Record gr : index.getRecords()) {
      doCheckTables(gr, counters);
    }
  }

  private void doCheckTablesNoIndex(Formatter fm, MFile ff, Counters counters) {
    String path = ff.getPath();
    try (RandomAccessFile raf = new ucar.unidata.io.RandomAccessFile(path, "r")) {

      raf.order(ucar.unidata.io.RandomAccessFile.BIG_ENDIAN);
      raf.seek(0);

      Grib1RecordScanner reader = new Grib1RecordScanner(raf);
      while (reader.hasNext()) {
        ucar.nc2.grib.grib1.Grib1Record gr = reader.next();
        if (gr == null)
          break;
        doCheckTables(gr, counters);
      }

    } catch (Throwable ioe) {
      fm.format("Failed on %s == %s%n", path, ioe.getMessage());
      System.out.printf("Failed on %s%n", path);
      ioe.printStackTrace();
    }
  }

  private void doCheckTables(ucar.nc2.grib.grib1.Grib1Record gr, Counters counters) {
    Grib1SectionProductDefinition pds = gr.getPDSsection();
    String key = Grib1Utils.extractParameterCode(gr);
    counters.count("center", pds.getCenter());
    counters.count("tableVersion", key);

    if (pds.getParameterNumber() > 127)
      counters.count("local", key);

    Grib1ParamTableReader table =
        new Grib1ParamTables().getParameterTable(pds.getCenter(), pds.getSubCenter(), pds.getTableVersion());
    if (table == null || null == table.getParameter(pds.getParameterNumber()))
      counters.count("missing", key);
  }

  /////////////////////////////////////////////////////////////////

  private void doScanIssues(Formatter f, MCollection dcm, boolean useIndex, boolean eachFile, boolean extraInfo)
      throws IOException {
    Counters countersAll = new Counters();

    for (MFile mfile : dcm.getFilesSorted()) {
      Counters countersOneFile = countersAll.makeSubCounters();

      String path = mfile.getPath();
      f.format(" %s%n", path);
      if (useIndex)
        doScanIssuesWithIndex(f, mfile, extraInfo, countersOneFile);
      else
        doScanIssuesNoIndex(f, mfile, extraInfo, countersOneFile);

      if (eachFile) {
        countersOneFile.show(f);
      }

      countersAll.addTo(countersOneFile);
    }

    f.format("ScanIssues - all files%n");
    countersAll.show(f);
  }

  private void doScanIssuesWithIndex(Formatter fm, MFile ff, boolean extraInfo, Counters counters) throws IOException {
    Grib1Index index = createIndex(ff);
    if (index == null)
      return;
    for (ucar.nc2.grib.grib1.Grib1Record gr : index.getRecords()) {
      doScanIssues(gr, fm, ff.getPath(), extraInfo, counters);
    }
  }

  private void doScanIssuesNoIndex(Formatter fm, MFile ff, boolean extraInfo, Counters counters) {

    String path = ff.getPath();
    try (RandomAccessFile raf = new ucar.unidata.io.RandomAccessFile(path, "r")) {
      raf.order(ucar.unidata.io.RandomAccessFile.BIG_ENDIAN);
      raf.seek(0);

      Grib1RecordScanner reader = new Grib1RecordScanner(raf);
      while (reader.hasNext()) {
        ucar.nc2.grib.grib1.Grib1Record gr = reader.next();
        if (gr == null)
          break;
        doScanIssues(gr, fm, path, extraInfo, counters);
      }

    } catch (Throwable ioe) {
      fm.format("Failed on %s == %s%n", path, ioe.getMessage());
      System.out.printf("Failed on %s%n", path);
      ioe.printStackTrace();
    }
  }

  private void doScanIssues(ucar.nc2.grib.grib1.Grib1Record gr, Formatter fm, String path, boolean extraInfo,
      Counters counters) {

    Grib1SectionGridDefinition gdss = gr.getGDSsection();
    Grib1Gds gds = gdss.getGDS();
    Grib1SectionProductDefinition pds = gr.getPDSsection();
    String table = pds.getCenter() + "-" + pds.getSubCenter() + "-" + pds.getTableVersion();
    counters.count("table version", table);
    counters.count("param", pds.getParameterNumber());
    counters.count("timeRangeIndicator", pds.getTimeRangeIndicator());
    counters.count("vertCoord", pds.getLevelType());
    counters.count("referenceDate", pds.getReferenceDate().toString());

    if (cust == null)
      cust = Grib1Customizer.factory(gr, null);

    Grib1ParamTime ptime = cust.getParamTime(pds);
    counters.count("timeCoord", ptime.getTimeCoord());
    counters.count("earthShape", gds.getEarthShape());
    counters.count("UVisReletiveToEastNorth", gds.getUVisReletiveToEastNorth() ? "true" : "false");

    if (gdss.isThin()) {
      if (extraInfo)
        fm.format("  THIN= (gds=%d) %s%n", gdss.getGridTemplate(), path);
      counters.count("thin", gdss.getGridTemplate());
    }

    if (!pds.gdsExists()) {
      if (extraInfo)
        fm.format("   PREDEFINED GDS= %s%n", path);
      counters.count("predefined", gdss.getPredefinedGridDefinition());
    }

    if (gdss.hasVerticalCoordinateParameters()) {
      if (extraInfo)
        fm.format("   Has vertical coordinates in GDS= %s%n", path);
      counters.count("vertCoordInGDS", pds.getLevelType());
    }

  }


  /////////////////////////////////////////////////////////////////

  private void doShowEncoding(Formatter f, MCollection dcm) throws IOException {
    Counters countersAll = new Counters();
    for (MFile mfile : dcm.getFilesSorted()) {
      f.format(" %s%n", mfile.getPath());
      // need dataRaf, so cant useIndex
      doShowEncodingNoIndex(f, mfile, countersAll);
    }

    countersAll.show(f);
  }

  private void doShowEncodingNoIndex(Formatter fm, MFile ff, Counters counters) {
    String path = ff.getPath();
    try (RandomAccessFile raf = new ucar.unidata.io.RandomAccessFile(path, "r")) {
      raf.order(ucar.unidata.io.RandomAccessFile.BIG_ENDIAN);
      raf.seek(0);

      Grib1RecordScanner reader = new Grib1RecordScanner(raf);
      while (reader.hasNext()) {
        ucar.nc2.grib.grib1.Grib1Record gr = reader.next();
        if (gr == null)
          break;
        GribData.Info info = gr.getBinaryDataInfo(raf);
        counters.count("decimalScale", info.decimalScaleFactor);
        counters.count("binScale", info.binaryScaleFactor);
        counters.count("nbits", info.numberOfBits);
        counters.count("gridType", info.getGridPointS());
        counters.count("packing", info.getPackingS());
        counters.count("dataType", info.getDataTypeS());
        counters.count("hasOctet14", info.hasOctet14() ? 1 : 0);

        if (info.binaryScaleFactor != 0 && info.decimalScaleFactor != 0) {
          counters.count("scale", 1);
        } else {
          counters.count("scale", 0);
        }
      }

    } catch (Throwable ioe) {
      fm.format("Failed on %s == %s%n", path, ioe.getMessage());
      System.out.printf("Failed on %s%n", path);
      ioe.printStackTrace();
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////

  private void doRename(Formatter f, MCollection dcm, boolean useIndex) throws IOException {
    f.format("CHECK Grib-1 Names: Old vs New for collection %s%n", dcm.getCollectionName());

    List<VarName> varNames = new ArrayList<>(3000);
    Map<String, List<String>> gridsAll = new HashMap<>(1000); // old -> list<new>

    for (MFile mfile : dcm.getFilesSorted()) {
      f.format("%n%s%n", mfile.getPath());
      Map<Integer, GridMatch> gridsNew = getGridsNew(mfile, f);
      Map<Integer, GridMatch> gridsOld = getGridsOld(mfile, f);

      // look for exact match
      for (GridMatch gm : gridsNew.values()) {
        GridMatch match = gridsOld.get(gm.hashCode());
        if (match != null) {
          gm.match = match;
          match.match = gm;
        }
      }

      // look for alternative match
      for (GridMatch gm : gridsNew.values()) {
        if (gm.match == null) {
          GridMatch match = altMatch(gm, gridsOld.values());
          if (match != null) {
            gm.match = match;
            match.match = gm;
          }
        }
      }

      f.format("%n");
      List<GridMatch> listNew = new ArrayList<>(gridsNew.values());
      Collections.sort(listNew);
      for (GridMatch gm : listNew) {
        f.format(" %s%n", gm.grid.getName());
        if (gm.match != null)
          f.format(" %s%n", gm.match.grid.getName());
        f.format("%n");
      }

      f.format("%nMISSING MATCHES IN NEW%n");
      List<GridMatch> list = new ArrayList<>(gridsNew.values());
      Collections.sort(list);
      for (GridMatch gm : list) {
        if (gm.match == null)
          f.format(" %s (%s) == %s%n", gm.grid.getName(), gm.show(), gm.grid.getDescription());
      }


      f.format("%nMISSING MATCHES IN OLD%n");
      List<GridMatch> listOld = new ArrayList<>(gridsOld.values());
      Collections.sort(listOld);
      for (GridMatch gm : listOld) {
        if (gm.match == null)
          f.format(" %s (%s)%n", gm.grid.getName(), gm.show());
      }

      // add to gridsAll
      for (GridMatch gmOld : listOld) {
        String key = gmOld.grid.getName();
        List<String> newGrids = gridsAll.computeIfAbsent(key, k -> new ArrayList<>());
        if (gmOld.match != null) {
          String keyNew = gmOld.match.grid.getName() + " == " + gmOld.match.grid.getDescription();
          if (!newGrids.contains(keyNew))
            newGrids.add(keyNew);
        }
      }

      // add matches to VarNames
      for (GridMatch gmOld : listOld) {
        if (gmOld.match == null) {
          f.format("MISSING %s (%s)%n", gmOld.grid.getName(), gmOld.show());
          continue;
        }
        Attribute att = gmOld.match.grid.attributes().findAttributeIgnoreCase(Grib.VARIABLE_ID_ATTNAME);
        String varId = att == null ? "" : att.getStringValue();
        varNames.add(new VarName(mfile.getName(), gmOld.grid.getName(), gmOld.match.grid.getName(), varId));
      }

    }

    f.format("%nOLD -> NEW MAPPINGS%n");
    List<String> keys = new ArrayList<>(gridsAll.keySet());
    int total = keys.size();
    int dups = 0;
    Collections.sort(keys);
    for (String key : keys) {
      f.format(" OLD %s%n", key);
      List<String> newGrids = gridsAll.get(key);
      Collections.sort(newGrids);
      if (newGrids.size() > 1)
        dups++;
      for (String newKey : newGrids)
        f.format(" NEW %s%n", newKey);
      f.format("%n");
    }
    f.format("Number with more than one map=%d total=%d%n", dups, total);

    // old -> new mapping xml table
    if (!useIndex) {
      Element rootElem = new Element("gribVarMap");
      Document doc = new Document(rootElem);
      rootElem.setAttribute("collection", dcm.getCollectionName());

      String currentDs = null;
      Element dsElem = null;
      for (VarName vn : varNames) {
        if (!vn.dataset.equals(currentDs)) {
          dsElem = new Element("dataset");
          rootElem.addContent(dsElem);
          dsElem.setAttribute("name", vn.dataset);
          currentDs = vn.dataset;
        }
        Element param = new Element("param");
        dsElem.addContent(param);
        param.setAttribute("oldName", vn.oldVar);
        param.setAttribute("newName", vn.newVar);
        param.setAttribute("varId", vn.varId);
      }

      FileOutputStream fout = new FileOutputStream("C:/tmp/grib1VarMap.xml");
      XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
      fmt.output(doc, fout);
      fout.close();
    }

  }

  private static class VarName {
    String dataset;
    String oldVar;
    String newVar;
    String varId;

    private VarName(String dataset, String oldVar, String newVar, String varId) {
      this.dataset = dataset;
      this.oldVar = oldVar;
      this.newVar = newVar;
      this.varId = varId;
    }
  }

  private GridMatch altMatch(GridMatch want, Collection<GridMatch> test) {
    // look for scale factor errors in prob
    for (GridMatch gm : test) {
      if (gm.match != null)
        continue; // already matched
      if (gm.altMatch(want))
        return gm;
    }

    // give up matching the prob
    for (GridMatch gm : test) {
      if (gm.match != null)
        continue; // already matched
      if (gm.altMatchNoProb(want))
        return gm;
    }

    return null;
  }

  private static class GridMatch implements Comparable<GridMatch> {
    Grid grid;
    GridMatch match;
    boolean isNew;
    int[] param = new int[3];
    int level;
    boolean isLayer, isError;
    int interval = -1;
    int prob = -1;
    int ens = -1;
    int probLimit;

    private GridMatch(GridDataset gds, Grid grid, boolean aNew) {
      this.grid = grid;
      isNew = aNew;

      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      GridAxis<?> zaxis = gcs.getVerticalAxis();
      if (zaxis != null) {
        isLayer = zaxis.isInterval();
      }

      if (isNew) {
        /*
         * :Grib1_Center = 7; // int
         * :Grib1_Subcenter = 0; // int
         * :Grib1_TableVersion = 2; // int
         * :Grib1_Parameter = 33;
         */
        Attribute att = grid.attributes().findAttributeIgnoreCase("Grib1_Center");
        param[0] = att.getNumericValue().intValue();
        att = grid.attributes().findAttributeIgnoreCase("Grib1_Subcenter");
        param[1] = att.getNumericValue().intValue();
        att = grid.attributes().findAttributeIgnoreCase("Grib1_Parameter");
        param[2] = att.getNumericValue().intValue();

        att = grid.attributes().findAttributeIgnoreCase("Grib1_Level_Type");
        level = att.getNumericValue().intValue();
        isError = grid.getName().contains("error");

        att = grid.attributes().findAttributeIgnoreCase("Grib1_Statistical_Interval_Type");
        if (att != null) {
          int intv = att.getNumericValue().intValue();
          if (intv != 255)
            interval = intv;
        }

        att = grid.attributes().findAttributeIgnoreCase("Grib1_Probability_Type"); // ??
        if (att != null)
          prob = att.getNumericValue().intValue();

        att = grid.attributes().findAttributeIgnoreCase("Grib1_Probability_Name"); // ??
        if (att != null) {
          String pname = att.getStringValue();
          int pos = pname.indexOf('_');
          pname = pname.substring(pos + 1);
          probLimit = (int) (1000.0 * Double.parseDouble(pname));
        }

        att = grid.attributes().findAttributeIgnoreCase("Grib1_Ensemble_Derived_Type");
        if (att != null)
          ens = att.getNumericValue().intValue();

      } else { // OLD
        Attribute att = grid.attributes().findAttributeIgnoreCase("GRIB_center_id");
        param[0] = att.getNumericValue().intValue();
        att = gds.attributes().findAttribute("Originating_subcenter_id");
        param[1] = att.getNumericValue().intValue();
        att = grid.attributes().findAttributeIgnoreCase("GRIB_param_number");
        param[2] = att.getNumericValue().intValue();

        att = grid.attributes().findAttributeIgnoreCase("GRIB_level_type");
        level = att.getNumericValue().intValue();
        isError = grid.getName().contains("error");

        String desc = grid.getDescription();
        if (desc.contains("Accumulation"))
          interval = 4;

        att = grid.attributes().findAttributeIgnoreCase("GRIB_probability_type");
        if (att != null)
          prob = att.getNumericValue().intValue();
        if (prob == 0) {
          att = grid.attributes().findAttributeIgnoreCase("GRIB_probability_lower_limit");
          if (att != null)
            probLimit = (int) (1000 * att.getNumericValue().doubleValue());
          // if (Math.abs(probLimit) > 100000) probLimit /= 1000; // wierd bug in 4.2
        } else if (prob == 1) {
          att = grid.attributes().findAttributeIgnoreCase("GRIB_probability_upper_limit"); // GRIB_probability_upper_limit
                                                                                           // = 12.89;
          // // double
          if (att != null)
            probLimit = (int) (1000 * att.getNumericValue().doubleValue());
          // if (Math.abs(probLimit) > 100000) probLimit /= 1000; // wierd bug in 4.2
        }

        att = grid.attributes().findAttributeIgnoreCase("GRIB_ensemble_derived_type");
        if (att != null)
          ens = att.getNumericValue().intValue();
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;

      GridMatch gridMatch = (GridMatch) o;
      if (!Arrays.equals(param, gridMatch.param))
        return false;
      if (ens != gridMatch.ens)
        return false;
      if (interval != gridMatch.interval)
        return false;
      if (isError != gridMatch.isError)
        return false;
      if (isLayer != gridMatch.isLayer)
        return false;
      if (level != gridMatch.level)
        return false;
      if (prob != gridMatch.prob)
        return false;
      return probLimit == gridMatch.probLimit;
    }

    public boolean altMatch(GridMatch gridMatch) {
      if (!altMatchNoProb(gridMatch))
        return false;
      if (probLimit / 1000 == gridMatch.probLimit)
        return true;
      return probLimit == gridMatch.probLimit / 1000;
    }

    public boolean altMatchNoProb(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;

      GridMatch gridMatch = (GridMatch) o;

      if (ens != gridMatch.ens)
        return false;
      if (interval != gridMatch.interval)
        return false;
      if (isError != gridMatch.isError)
        return false;
      if (isLayer != gridMatch.isLayer)
        return false;
      if (level != gridMatch.level)
        return false;
      return prob == gridMatch.prob;

    }


    @Override
    public int hashCode() {
      int result = 1;
      result = 31 * result + level;
      result = 31 * result + param[0];
      result = 31 * result + (isLayer ? 1 : 0);
      result = 31 * result + (isError ? 1 : 0);
      result = 31 * result + param[1];
      result = 31 * result + interval;
      result = 31 * result + prob;
      result = 31 * result + param[2];
      result = 31 * result + ens;
      result = 31 * result + probLimit;
      return result;
    }

    @Override
    public int compareTo(GridMatch o) {
      return grid.getName().compareTo(o.grid.getName());
    }

    String show() {
      Formatter f = new Formatter();
      f.format("%d-%d-%d-", param[0], param[1], param[2]);
      f.format("%d", level);
      if (isLayer)
        f.format("_layer");
      if (interval >= 0)
        f.format("_intv%d", interval);
      if (prob >= 0)
        f.format("_prob%d_%d", prob, probLimit);
      if (ens >= 0)
        f.format("_ens%d", ens);
      if (isError)
        f.format("_error");
      return f.toString();
    }
  }

  private Map<Integer, GridMatch> getGridsNew(MFile ff, Formatter f) throws IOException {
    Map<Integer, GridMatch> grids = new HashMap<>(100);
    Formatter errlog = new Formatter();
    try (GridDataset ncfile = GridDatasetFactory.openGridDataset(ff.getPath(), errlog)) {
      for (Grid dt : ncfile.getGrids()) {
        GridMatch gm = new GridMatch(ncfile, dt, true);
        GridMatch dup = grids.get(gm.hashCode());
        if (dup != null)
          f.format(" DUP NEW (%d == %d) = %s (%s) and DUP %s (%s)%n", gm.hashCode(), dup.hashCode(), gm.grid.getName(),
              gm.show(), dup.grid.getName(), dup.show());
        else
          grids.put(gm.hashCode(), gm);
      }
    }
    return grids;
  }

  private Map<Integer, GridMatch> getGridsOld(MFile ff, Formatter f) {
    Map<Integer, GridMatch> grids = new HashMap<>(100);
    Formatter errlog = new Formatter();
    try (NetcdfFile ncfile = NetcdfFiles.open(ff.getPath(), "ucar.nc2.iosp.grib.GribServiceProvider", -1, null, null)) {
      NetcdfDataset ncd = NetcdfDatasets.enhance(ncfile, NetcdfDataset.getDefaultEnhanceMode(), null);
      GridDataset grid = GridDatasetFactory.wrapGridDataset(ncd, errlog).orElseThrow();
      for (Grid dt : grid.getGrids()) {
        GridMatch gm = new GridMatch(grid, dt, false);
        GridMatch dup = grids.get(gm.hashCode());
        if (dup != null)
          f.format(" DUP OLD (%d == %d) = %s (%s) and DUP %s (%s)%n", gm.hashCode(), dup.hashCode(), gm.grid.getName(),
              gm.show(), dup.grid.getName(), dup.show());
        else
          grids.put(gm.hashCode(), gm);
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
    return grids;
  }

  ///////////////////////////////////////////////

  // Look through the collection and find what GDS templates are used.
  private void doUniqueGds(Formatter f, MCollection dcm, boolean useIndex) throws IOException {
    f.format("Show Unique GDS%n");

    Map<Integer, GdsList> gdsSet = new HashMap<>();
    for (MFile mfile : dcm.getFilesSorted()) {
      f.format(" %s%n", mfile.getPath());
      doUniqueGds(mfile, gdsSet, f);
    }

    List<GdsList> sorted = new ArrayList<>(gdsSet.values());
    Collections.sort(sorted);

    for (GdsList gdsl : sorted) {
      f.format("%nGDS %s template= %d %n", gdsl.gds.getNameShort(), gdsl.gds.template);
      for (FileCount fc : gdsl.fileList) {
        f.format("  %5d %s %n", fc.countRecords, fc.f.getPath());
      }
    }
  }

  private void doUniqueGds(MFile mf, Map<Integer, GdsList> gdsSet, Formatter f) throws IOException {
    String path = mf.getPath();
    Grib1Index g1idx = new Grib1Index();
    boolean ok = g1idx.readIndex(path, 0, thredds.inventory.CollectionUpdateType.nocheck);
    if (!ok) {
      f.format("**Cant open %s%n", path);
      return;
    }

    for (Grib1Record gr : g1idx.getRecords()) {
      int template = gr.getGDSsection().getGDS().template;
      gdsSet.computeIfAbsent(template, k -> new GdsList(gr.getGDSsection().getGDS()));
      GdsList gdsList = gdsSet.get(template);
      FileCount fc = gdsList.findOrAdd(mf);
      fc.countRecords++;
    }
  }

  private static class GdsList implements Comparable<GdsList> {
    Grib1Gds gds;
    java.util.List<FileCount> fileList = new ArrayList<>();

    private GdsList(Grib1Gds gds) {
      this.gds = gds;
    }

    FileCount findOrAdd(MFile f) {
      for (FileCount fc : fileList) {
        if (fc.f.getPath().equals(f.getPath()))
          return fc;
      }

      FileCount fc = new FileCount(f);
      fileList.add(fc);
      return fc;
    }

    @Override
    public int compareTo(GdsList o) {
      return gds.template - o.gds.template;
    }
  }

  private static class FileCount {
    private FileCount(MFile f) {
      this.f = f;
    }

    MFile f;
    int countRecords;
  }

}

