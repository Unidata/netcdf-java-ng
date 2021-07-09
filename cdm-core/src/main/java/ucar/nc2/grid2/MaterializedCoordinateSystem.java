/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid2;

import com.google.common.collect.ImmutableList;
import ucar.array.Range;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.List;

/** A Coordinate System for materialized gridded data. */
@Immutable
public class MaterializedCoordinateSystem {

  // LOOK should this be MaterializedTimeCoordinateSystem ?
  @Nullable
  public GridTimeCoordinateSystem getTimeCoordSystem() {
    return tcs;
  }

  @Nullable
  public GridAxisPoint getEnsembleAxis() {
    return ens;
  }

  @Nullable
  public GridAxis<?> getVerticalAxis() {
    return vert;
  }

  public GridHorizCoordinateSystem getHorizCoordSystem() {
    return hcs;
  }

  /** Get the X axis (either GeoX or Lon). */
  public GridAxisPoint getXHorizAxis() {
    return getHorizCoordSystem().getXHorizAxis();
  }

  /** Get the Y axis (either GeoY or Lat). */
  public GridAxisPoint getYHorizAxis() {
    return getHorizCoordSystem().getYHorizAxis();
  }

  /** The shape of this array. */
  public List<Integer> getMaterializedShape() {
    List<Integer> result = new ArrayList<>();
    if (getTimeCoordSystem() != null) {
      result.addAll(getTimeCoordSystem().getNominalShape());
    }
    if (getEnsembleAxis() != null) {
      result.add(getEnsembleAxis().getNominalSize());
    }
    if (getVerticalAxis() != null) {
      result.add(getVerticalAxis().getNominalSize());
    }
    result.addAll(getHorizCoordSystem().getShape());
    return result;
  }

  public List<ucar.array.Range> getSubsetRanges() {
    List<ucar.array.Range> result = new ArrayList<>();
    if (getTimeCoordSystem() != null) {
      result.addAll(getTimeCoordSystem().getSubsetRanges());
    }
    if (getEnsembleAxis() != null) {
      result.add(getEnsembleAxis().getSubsetRange());
    }
    if (getVerticalAxis() != null) {
      result.add(getVerticalAxis().getSubsetRange());
    }
    result.addAll(getHorizCoordSystem().getSubsetRanges());
    return result;
  }

  public List<GridAxis<?>> getAxes() {
    List<GridAxis<?>> result = new ArrayList<>();
    if (getTimeCoordSystem() != null) {
      if (getTimeCoordSystem().getRunTimeAxis() != null) {
        result.add(getTimeCoordSystem().getRunTimeAxis());
      }
      result.add(getTimeCoordSystem().getTimeOffsetAxis(0));
    }
    if (getEnsembleAxis() != null) {
      result.add(getEnsembleAxis());
    }
    if (getVerticalAxis() != null) {
      result.add(getVerticalAxis());
    }
    result.add(getHorizCoordSystem().getYHorizAxis());
    result.add(getHorizCoordSystem().getXHorizAxis());
    return result;
  }

  //////////////////////////////////////////////////////////////////////////
  private final GridTimeCoordinateSystem tcs;
  private final GridHorizCoordinateSystem hcs;
  private final GridAxisPoint ens;
  private final GridAxis<?> vert;
  private final ImmutableList<Range> ranges;

  private MaterializedCoordinateSystem(Builder builder) {
    this.tcs = builder.tcs;
    this.hcs = builder.hcs;
    this.ens = builder.ens;
    this.vert = builder.vert;
    this.ranges = builder.ranges;
  }

  /** Turn into a mutable Builder. Can use toBuilder().build() to copy. */
  public Builder toBuilder() {
    return builder().setTimeCoordSys(this.tcs).setHorizCoordSys(this.hcs).setEnsAxis(this.ens).setVertAxis(this.vert);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private GridTimeCoordinateSystem tcs;
    private GridHorizCoordinateSystem hcs;
    private GridAxisPoint ens;
    private GridAxis<?> vert;
    private ImmutableList<Range> ranges;
    private boolean built;

    public Builder setTimeCoordSys(GridTimeCoordinateSystem tcs) {
      this.tcs = tcs;
      return this;
    }

    public Builder setHorizCoordSys(GridHorizCoordinateSystem hcs) {
      this.hcs = hcs;
      return this;
    }

    public Builder setEnsAxis(GridAxisPoint ens) {
      this.ens = ens;
      return this;
    }

    public Builder setVertAxis(GridAxis<?> vert) {
      this.vert = vert;
      return this;
    }

    public Builder setRanges(List<Range> ranges) {
      this.ranges = ImmutableList.copyOf(ranges);
      return this;
    }

    public MaterializedCoordinateSystem build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new MaterializedCoordinateSystem(this);
    }
  }

}
