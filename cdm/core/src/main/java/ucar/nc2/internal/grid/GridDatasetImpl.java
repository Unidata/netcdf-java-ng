/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.grid;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.*;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.grid.*;
import ucar.nc2.internal.dataset.DatasetClassifier;

import java.io.IOException;
import java.util.*;

/** GridDataset implementation wrapping a NetcdfDataset. */
public class GridDatasetImpl implements GridDataset {
  private static Logger log = LoggerFactory.getLogger(GridDatasetImpl.class);

  public static Optional<GridDatasetImpl> create(NetcdfDataset ncd, Formatter errInfo) throws IOException {
    Set<Enhance> enhance = ncd.getEnhanceMode();
    if (enhance == null || !enhance.contains(Enhance.CoordSystems)) {
      enhance = NetcdfDataset.getDefaultEnhanceMode();
      ncd = NetcdfDatasets.enhance(ncd, enhance, null);
    }

    DatasetClassifier facc = new DatasetClassifier(ncd, errInfo);
    return (facc.getFeatureType() == FeatureType.GRID) ? Optional.of(new GridDatasetImpl(ncd, facc, errInfo))
        : Optional.empty();
  }

  ///////////////////////////////////////////////////////////////////
  // TODO make Immutable

  private final NetcdfDataset ncd;
  private final FeatureType featureType;

  private final ArrayList<GridCS> coordsys = new ArrayList<>();

  private final Map<String, GridAxis> gridAxes;
  private final ArrayList<Grid> grids = new ArrayList<>();
  private final Multimap<GridCS, Grid> gridsets;

  private GridDatasetImpl(NetcdfDataset ncd, DatasetClassifier classifier, Formatter errInfo) {
    this.ncd = ncd;
    this.featureType = classifier.getFeatureType();

    this.gridAxes = new HashMap<>();

    // Do all the independent axes first
    for (CoordinateAxis axis : classifier.getIndependentAxes()) {
      if (axis.getFullName().startsWith("Best/")) {
        continue;
      }
      if (axis.getRank() < 2) {
        GridAxis gridAxis = Grids.extractGridAxis1D(ncd, axis, GridAxis.DependenceType.independent);
        gridAxes.put(axis.getFullName(), gridAxis);
      } else {
        log.warn("Independent gridAxis {} rank > 1", axis.getFullName());
        errInfo.format("Independent gridAxis %s rank > 1", axis.getFullName());
      }
    }

    // Now we can do dependent, knowing we have all the independent ones in gridAxes
    for (CoordinateAxis axis : classifier.getDependentAxes()) {
      if (axis.getFullName().startsWith("Best/")) {
        continue;
      }
      if (axis.getRank() < 2) {
        GridAxis gridAxis = Grids.extractGridAxis1D(ncd, axis, GridAxis.DependenceType.dependent);
        gridAxes.put(axis.getFullName(), gridAxis);
      } else if (axis.getAxisType() == AxisType.TimeOffset && axis.getRank() == 2) {
        GridAxis gridAxis = Grids.extractGridAxisOffset2D(axis, GridAxis.DependenceType.dependent, gridAxes);
        gridAxes.put(axis.getFullName(), gridAxis);
      }
    }

    // Convert coordsys
    Map<String, GridCS> trackCsConverted = new HashMap<>();
    for (DatasetClassifier.CoordSysClassifier csc : classifier.getCoordinateSystemsUsed()) {
      if (csc.getName().startsWith("Best/")) {
        continue;
      }
      GridCS gcs = new GridCS(csc, this.gridAxes);
      coordsys.add(gcs);
      trackCsConverted.put(csc.getName(), gcs);
    }
    // Largest Coordinate Systems come first
    coordsys.sort((o1, o2) -> o2.getGridAxes().size() - o1.getGridAxes().size());

    this.gridsets = ArrayListMultimap.create();
    for (Variable v : ncd.getVariables()) {
      if (v.getFullName().startsWith("Best/")) { // TODO remove Best from grib generation code
        continue;
      }
      VariableEnhanced ve = (VariableEnhanced) v;
      List<CoordinateSystem> css = new ArrayList<>(ve.getCoordinateSystems());
      if (css.isEmpty()) {
        continue;
      }
      // Use the largest (# axes)
      css.sort((o1, o2) -> o2.getCoordinateAxes().size() - o1.getCoordinateAxes().size());
      for (CoordinateSystem cs : css) {
        GridCS gcs = trackCsConverted.get(cs.getName());
        if (gcs != null && gcs.getFeatureType() == this.featureType && gcs.isCoordinateSystemFor(v)) {
          Grid grid = new GridVariable(gcs, (VariableDS) ve);
          grids.add(grid);
          this.gridsets.put(gcs, grid);
          break;
        }
      }
    }
  }

  @Override
  public String getName() {
    String loc = ncd.getLocation();
    int pos = loc.lastIndexOf('/');
    if (pos < 0)
      pos = loc.lastIndexOf('\\');
    return (pos < 0) ? loc : loc.substring(pos + 1);
  }

  public FeatureType getCoverageType() {
    return featureType;
  }

  @Override
  public String getLocation() {
    return ncd.getLocation();
  }

  @Override
  public ImmutableList<GridCoordinateSystem> getCoordSystems() {
    return ImmutableList.copyOf(coordsys);
  }

  @Override
  public ImmutableList<GridAxis> getCoordAxes() {
    return ImmutableList.copyOf(gridAxes.values());
  }

  @Override
  public ImmutableList<Grid> getGrids() {
    return ImmutableList.copyOf(grids);
  }

  @Override
  public Optional<Grid> findGrid(String name) {
    return grids.stream().filter(g -> g.getName().equals(name)).findFirst();
  }

  @Override
  public FeatureType getFeatureType() {
    return featureType;
  };

  @Override
  public String toString() {
    Formatter f = new Formatter();
    toString(f);
    return f.toString();
  }

  @Override
  public void toString(Formatter buf) {
    int countGridset = 0;

    for (GridCS gcs : gridsets.keySet()) {
      buf.format("%nGridset %d: ", countGridset);
      gcs.show(buf, false);
      buf.format("%n");
      buf.format("Name___________________________________________Unit____________Description%n");
      for (Grid grid : gridsets.get(gcs)) {
        buf.format(" %-46s %-15s %s%n", grid.getName(), grid.getUnitsString(), grid.getDescription());
      }
      countGridset++;
      buf.format("%n");
    }

    buf.format("%nGeoReferencing Coordinate Axes%n");
    buf.format("Name__________________________Units_________________________Type______Description%n");
    for (CoordinateAxis axis : ncd.getCoordinateAxes()) {
      axis.getInfo(buf);
      buf.format("%n");
    }
  }

  private boolean wasClosed = false;

  @Override
  public synchronized void close() throws IOException {
    try {
      if (!wasClosed)
        ncd.close();
    } finally {
      wasClosed = true;
    }
  }
}
