/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.grid;

import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.grid.GridTimeCoordinateSystem;
import ucar.nc2.grid.MaterializedCoordinateSystem;
import ucar.ui.util.NamedObject;
import ucar.unidata.geoloc.ProjectionRect;

import javax.annotation.Nullable;

/** Holds the current selected state. Shared between Renderer and Viewer. */
class DataState {
  GridDataset gridDataset;
  Grid grid;
  GridCoordinateSystem gcs; // from the grid object
  GridTimeCoordinateSystem tcs; // from the grid object
  MaterializedCoordinateSystem mcs; // from the currently read (materialized) data array

  @Nullable
  GridAxisPoint rtaxis;
  @Nullable
  GridAxis<?> toaxis;
  @Nullable
  GridAxis<?> zaxis;
  @Nullable
  GridAxisPoint ensaxis;

  RuntimeNamedObject runtimeCoord;
  Object timeCoord;
  Object vertCoord;
  Double ensCoord;
  int horizStride = 1;
  ProjectionRect projRect;
  int[] index = null;

  public DataState(GridDataset gridDataset, Grid grid) {
    this.gridDataset = gridDataset;
    this.grid = grid;
    this.gcs = grid.getCoordinateSystem();
    this.tcs = grid.getCoordinateSystem().getTimeCoordinateSystem();
    if (tcs != null) {
      this.toaxis = tcs.getTimeOffsetAxis(0);
      this.rtaxis = tcs.getRunTimeAxis();
    }
    this.zaxis = gcs.getVerticalAxis();
    this.ensaxis = gcs.getEnsembleAxis();
  }

  boolean setRuntimeCoord(@Nullable Object coord) {
    boolean changed = coord != null && !coord.equals(runtimeCoord);
    runtimeCoord = (RuntimeNamedObject) coord;
    return changed;
  }

  boolean setTimeCoord(@Nullable Object coord) {
    boolean changed = coord != null && !coord.equals(timeCoord);
    timeCoord = coord;
    return changed;
  }

  boolean setVertCoord(@Nullable Object coord) {
    boolean changed = coord != null && !coord.equals(vertCoord);
    vertCoord = coord;
    return changed;
  }

  boolean setEnsCoord(@Nullable Object coord) {
    boolean changed = coord != null && !coord.equals(ensCoord);
    ensCoord = (Double) coord;
    return changed;
  }

  boolean setProjRect(@Nullable ProjectionRect projRect) {
    boolean changed = (projRect == null) != (this.projRect == null);
    if (projRect != null && this.projRect != null) {
      changed = !projRect.nearlyEquals(this.projRect);
    }
    this.projRect = projRect;
    return changed;
  }

  Grid lastGrid;
  Object lastRuntime;
  Object lastTime;
  Object lastVert;
  Object lastEnsemble;
  int lastStride;
  ProjectionRect lastProjRect;

  void saveState() {
    lastGrid = grid;
    lastRuntime = runtimeCoord;
    lastTime = timeCoord;
    lastVert = vertCoord;
    lastEnsemble = ensCoord;
    lastStride = horizStride;
    lastProjRect = projRect;
  }

  boolean hasChanged() {
    if (grid != null && !grid.equals(lastGrid)) {
      return true;
    }
    if (runtimeCoord != null && !runtimeCoord.equals(lastRuntime)) {
      return true;
    }
    if (timeCoord != null && !timeCoord.equals(lastTime)) {
      return true;
    }
    if (vertCoord != null && !vertCoord.equals(lastVert)) {
      return true;
    }
    if (ensCoord != null && !ensCoord.equals(lastEnsemble)) {
      return true;
    }
    if (horizStride != lastStride) {
      return true;
    }
    if (projRect != lastProjRect) {
      return true;
    }
    return false;
  }

  @Override
  public String toString() {
    return "DataState{" + "grid=" + grid + '}';
  }

  static class RuntimeNamedObject implements NamedObject {
    final int runtimeIdx;
    final CalendarDate runtime;

    public RuntimeNamedObject(int runtimeIdx, CalendarDate runtime) {
      this.runtimeIdx = runtimeIdx;
      this.runtime = runtime;
    }

    @Override
    public String getName() {
      return runtime.toString();
    }

    @Override
    public String getDescription() {
      return runtime.toString();
    }

    @Override
    public Object getValue() {
      return this;
    }
  }
}
