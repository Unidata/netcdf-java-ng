/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.nc2.grid.CoordInterval;
import ucar.nc2.grid.GridSubset;
import ucar.nc2.grid.GridAxisInterval;
import ucar.nc2.grid.GridAxisSpacing;
import ucar.nc2.grid.Grids;
import ucar.nc2.util.Misc;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Formatter;
import java.util.Optional;

/**
 * Helper class for GridAxisInterval for subsetting and searching.
 * Placed in this package so that its not part of the public API.
 */
@Immutable
public class SubsetIntervalHelper {
  private final GridAxisInterval orgGridAxis;

  public SubsetIntervalHelper(GridAxisInterval orgGrid) {
    this.orgGridAxis = orgGrid;
  }

  // TODO incomplete
  @Nullable
  public GridAxisInterval.Builder<?> subsetBuilder(GridSubset params, Formatter errLog) {
    switch (orgGridAxis.getAxisType()) {
      case GeoZ:
      case Pressure:
      case Height: {
        Double dval = params.getVertPoint();
        if (dval != null) {
          return subsetClosest(dval);
        }
        CoordInterval intv = params.getVertIntv();
        if (intv != null) {
          return subsetClosest(intv);
        }
        // default is all
        break;
      }

      // timeOffset, timeOffsetIntv, timeLatest, timeFirst; timeAll is the default
      case Time:
      case TimeOffset: {
        Double dval = params.getTimeOffset();
        if (dval != null) {
          return subsetClosest(dval); // TODO what does this mean for an interval?
        }
        CoordInterval intv = params.getTimeOffsetIntv();
        if (intv != null) {
          return subsetClosest(intv);
        }
        if (params.getTimeLatest()) {
          int last = orgGridAxis.getNominalSize() - 1;
          return makeSubsetByIndex(Range.make(last, last));
        }
        if (params.getTimeFirst()) {
          return makeSubsetByIndex(Range.make(0, 0));
        }
        if (params.getTimeOffsetRange() != null) {
          CoordInterval range = params.getTimeOffsetRange();
          int start_index = SubsetHelpers.findCoordElement(orgGridAxis, range.start(), true); // bounded, always valid
                                                                                              // index
          int end_index = SubsetHelpers.findCoordElement(orgGridAxis, range.end(), true); // bounded, always valid index
          return makeSubsetByIndex(Range.make(start_index, end_index));
        }

        // default is all
        break;
      }

      // These are subsetted by the HorizCS
      case GeoX:
      case GeoY:
      case Lat:
      case Lon:
        return null;

      default:
        // default is all
        break;
    }

    // otherwise return copy of the original axis
    return orgGridAxis.toBuilder();
  }

  @Nullable
  public GridAxisInterval.Builder<?> subsetClosest(double want) {
    return makeSubsetValuesClosest(want);
  }

  @Nullable
  public GridAxisInterval.Builder<?> subsetClosest(CoordInterval want) {
    for (int idx = 0; idx < orgGridAxis.getNominalSize(); idx++) {
      CoordInterval intv = orgGridAxis.getCoordInterval(idx);
      double bound1 = intv.start();
      double bound2 = intv.end();
      if (Misc.nearlyEquals(bound1, want.start()) && Misc.nearlyEquals(bound2, want.end())) {
        return makeSubsetByIndex(Range.make(idx, idx));
      }
    }
    return makeSubsetValuesClosest(want);
  }

  // SubsetRange must be contained in this range
  public GridAxisInterval.Builder<?> makeSubsetByIndex(Range subsetRange) {
    int ncoords = subsetRange.length();
    Preconditions.checkArgument(subsetRange.last() < orgGridAxis.getNominalSize());

    double resolution = 0.0;
    if (orgGridAxis.getSpacing().isRegular()) {
      resolution = subsetRange.stride() * orgGridAxis.getResolution();
    }

    GridAxisInterval.Builder<?> builder = orgGridAxis.toBuilder();
    builder.subset(ncoords, orgGridAxis.getCoordDouble(subsetRange.first()),
        orgGridAxis.getCoordDouble(subsetRange.last()), resolution, subsetRange);
    return builder;
  }

  @Nullable
  private GridAxisInterval.Builder<?> makeSubsetValuesClosest(CoordInterval want) {
    int closest_index = SubsetHelpers.findCoordElement(orgGridAxis, want, true); // bounded, always valid index
    if (closest_index < 0) { // TODO discontIntv returns -1
      return null;
    }
    Range range = Range.make(closest_index, closest_index);

    GridAxisInterval.Builder<?> builder = orgGridAxis.toBuilder();
    if (orgGridAxis.getSpacing() == GridAxisSpacing.regularInterval) {
      CoordInterval intv = orgGridAxis.getCoordInterval(closest_index);
      double val1 = intv.start();
      double val2 = intv.end();
      builder.subset(1, val1, val2, val2 - val1, range);
    } else {
      builder.subset(1, 0, 0, 0.0, range);
    }
    return builder;
  }

  @Nullable
  private GridAxisInterval.Builder<?> makeSubsetValuesClosest(double want) {
    int closest_index = SubsetHelpers.findCoordElement(orgGridAxis, want, true); // bounded, always valid index
    if (closest_index < 0) { // TODO discontIntv returns -1
      return null;
    }
    GridAxisInterval.Builder<?> builder = orgGridAxis.toBuilder();

    Range range;
    try {
      range = new Range(closest_index, closest_index);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen
    }

    if (orgGridAxis.getSpacing() == GridAxisSpacing.regularInterval) {
      CoordInterval intv = orgGridAxis.getCoordInterval(closest_index);
      double val1 = intv.start();
      double val2 = intv.end();
      builder.subset(1, val1, val2, val2 - val1, range);

    } else {
      builder.subset(1, 0, 0, 0.0, range);
    }
    return builder;
  }

  /////////////////////////////////////////////////////////////////
  // Not currently used. needed to implement stride ??

  public Optional<GridAxisInterval.Builder<?>> subset(double minValue, double maxValue, int stride, Formatter errLog) {
    return makeSubsetValues(minValue, maxValue, stride, errLog);
  }

  // TODO could specialize when only one point
  private Optional<GridAxisInterval.Builder<?>> makeSubsetValues(double minValue, double maxValue, int stride,
      Formatter errLog) {
    double lower = Grids.isAscending(orgGridAxis) ? Math.min(minValue, maxValue) : Math.max(minValue, maxValue);
    double upper = Grids.isAscending(orgGridAxis) ? Math.max(minValue, maxValue) : Math.min(minValue, maxValue);

    int minIndex = SubsetHelpers.findCoordElement(orgGridAxis, lower, false);
    int maxIndex = SubsetHelpers.findCoordElement(orgGridAxis, upper, false);

    if (minIndex >= orgGridAxis.getNominalSize()) {
      errLog.format("%s: no points in subset: lower %f > end %f", this.orgGridAxis.getName(), lower,
          orgGridAxis.getCoordInterval(0).start());
      return Optional.empty();
    }
    if (maxIndex < 0) {
      errLog.format("%s: no points in subset: upper %f < start %f", this.orgGridAxis.getName(), upper,
          orgGridAxis.getCoordInterval(orgGridAxis.getNominalSize() - 1).end());
      return Optional.empty();
    }

    if (minIndex < 0)
      minIndex = 0;
    if (maxIndex >= orgGridAxis.getNominalSize())
      maxIndex = orgGridAxis.getNominalSize() - 1;

    int count = maxIndex - minIndex + 1;
    if (count <= 0)
      throw new IllegalArgumentException("no points in subset");

    try {
      return Optional.of(makeSubsetByIndex(new Range(minIndex, maxIndex, stride)));
    } catch (InvalidRangeException e) {
      errLog.format("%s", e.getMessage());
      return Optional.empty();
    }
  }

}
