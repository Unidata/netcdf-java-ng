/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.grid;

import com.google.common.math.DoubleMath;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.grid.GridSubset;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.GridTimeCoordinateSystem;

import java.util.Formatter;
import java.util.Optional;

/**
 * Helper class for subsetting and searching, when you need the GridTimeCoordinateSystem.
 * Placed in this package so that its not part of the public API.
 */
public class SubsetTimeHelper {
  private final GridTimeCoordinateSystem tcs;
  public GridAxisPoint runtimeAxis;

  public SubsetTimeHelper(GridTimeCoordinateSystem tcs) {
    this.tcs = tcs;
    this.runtimeAxis = tcs.getRunTimeAxis();
  }

  /**
   * Called by Observation and SingleRuntime, so, runtime if any is ignored.
   * /*
   * ### Time subsetting
   * 1. **time**
   * The value is the CalendarDate of the requested time.
   * 2. **timeLatest**
   * Request the latest time.
   * 3. **timeAll**
   * Request all times.
   * 4. **timePresent**
   * Request the times closest to the present time.
   * 5. **timeStride**
   * Request every nth time value. Use with time to request where to start. why needed ??
   * timeClosest? timeInInterval?
   */

  public Optional<? extends GridAxis<?>> subsetTime(GridSubset params, Formatter errlog) {
    GridAxis<?> timeOffsetAxis = tcs.getTimeOffsetAxis(0); // Observation and SingleRuntime

    Double wantOffset = null;
    CalendarDate wantTime = params.getTime();
    if (wantTime != null) {
      wantOffset = (double) tcs.makeOffsetDateUnit(0).makeOffsetFromRefDate(wantTime);
    } else if (params.getTimePresent()) {
      wantOffset = (double) tcs.makeOffsetDateUnit(0).makeOffsetFromRefDate(CalendarDate.present());
    }
    if (wantOffset != null) {
      return timeOffsetAxis.subset(GridSubset.create().setTimeOffsetCoord(wantOffset), errlog);
    }

    // timeOffset, timeOffsetIntv, timeLatest
    return timeOffsetAxis.subset(params, errlog);
  }

  /*
   * public static final String runtime = "runtime"; // value = CalendarDate
   * public static final String runtimeLatest = "runtimeLatest"; // value = Boolean
   * public static final String runtimeAll = "runtimeAll"; // value = Boolean
   * 
   * // LOOK The value is the offset in the units of the GridAxis, or baseDate?
   * public static final String timeOffset = "timeOffset"; // value = Double
   * public static final String timeOffsetIntv = "timeOffsetIntv"; // value = CoordInterval
   * public static final String timeOffsetAll = "timeOffsetAll"; // value = Boolean
   * public static final String timeOffsetFirst = "timeOffsetFirst"; // value = Boolean LOOK deprecated I think
   * 
   * // validtime
   * public static final String time = "time"; // value = CalendarDate
   * public static final String timeRange = "timeRange"; // value = CalendarDateRange LOOK unimplemented
   * public static final String timeLatest = "timeLatest"; // value = Boolean
   * public static final String timePresent = "timePresent"; // value = Boolean
   * public static final String timeStride = "timeStride"; // value = Integer LOOK is this needed?
   * public static final String timeAll = "timeAll"; // value = Boolean LOOK whats diff with timeOffsetAll?
   * public static final String timePoint = "timePoint"; // value = Double LOOK whats diff with timeOffset?
   * public static final String timeIntv = "timeIntv"; // value = CoordInterval LOOK whats diff with timeOffsetIntv?
   */

  /*
   * ### Runtime subsetting
   * 1. **runtime**
   * The value is the CalendarDate of the requested runtime.
   * 2. **runtimeLatest**
   * Requests the most recent runtime.
   * 3. **runtimeAll**
   * Request all runtimes. Limit?
   * The Runtime coordinate may be missing, a scalar or have a single value.
   * runtimeClosest? runtimeInInterval? runtimesInInterval?
   * 
   * ### Time subsetting
   * 1. **time**
   * The value is the CalendarDate of the requested time.
   * 2. **timeLatest**
   * Request the latest time.
   * 3. **timeAll**
   * Request all times.
   * 4. **timePresent**
   * Request the times closest to the present time.
   * 5. **timeStride**
   * Request every nth time value. Use with time to request where to start. why needed ??
   * timeClosest? timeInInterval?
   * 
   * ### TimeOffset subsetting
   * 1. **timeOffset**
   * The value is the offset in the units of the GridAxisPoint.
   * 2. **timeOffsetIntv**
   * The value is the offset in the units of the GridAxisInterval.
   */

  public Optional<? extends GridAxis<?>> subsetOffset(GridSubset params, Formatter errlog) {
    GridAxisPoint runtimeAxis = tcs.getRunTimeAxis();
    int runIdx = 0; // if nothing set, use the first one.

    if (runtimeAxis != null) {
      if (params.getRunTimeLatest()) {
        runIdx = runtimeAxis.getNominalSize() - 1;
      }

      // runtime, runtimeLatest
      CalendarDate wantRuntime = params.getRunTime();
      if (wantRuntime != null) {
        double want = tcs.getRuntimeDateUnit().makeOffsetFromRefDate(wantRuntime);
        runIdx = search(tcs.getRunTimeAxis(), want);
        if (runIdx < 0) {
          errlog.format("Cant find runtime = %s%n", wantRuntime);
          return Optional.empty();
        }
      } else if (params.getRunTimeLatest()) {
        runIdx = runtimeAxis.getNominalSize() - 1; // LOOK using nominal...
      }

      // LOOK what about subsetting across multiple runtimes ??
      SubsetPointHelper helper = new SubsetPointHelper(runtimeAxis);
      this.runtimeAxis = helper.makeSubsetByIndex(runIdx).build();
    }

    // suppose these were the options for time. Do they have to be processed differently for different
    // GridTimeCoordinateSystem.Type?

    int timeIdx = -1;
    GridAxis<?> timeOffsetAxis = tcs.getTimeOffsetAxis(runIdx);

    // convert calendarDate to
    CalendarDate wantTime = params.getTime();
    if (wantTime != null) {
      double want = tcs.makeOffsetDateUnit(runIdx).makeOffsetFromRefDate(wantTime);
      return timeOffsetAxis.subset(GridSubset.create().setTimeOffsetCoord(want), errlog);
      /*
       * if (timeOffsetAxis instanceof GridAxisPoint) {
       * timeIdx = search(timeOffsetAxis, want);
       * if (timeIdx < 0) {
       * errlog.format("Cant find time = %s%n", wantTime);
       * return Optional.empty();
       * }
       * SubsetPointHelper helper = new SubsetPointHelper((GridAxisPoint) timeOffsetAxis);
       * return Optional.of(helper.makeSubsetByIndex(timeIdx).build());
       * } else {
       * timeIdx = search(timeOffsetAxis, want);
       * if (timeIdx < 0) {
       * errlog.format("Cant find time = %s%n", wantTime);
       * return Optional.empty();
       * }
       * SubsetIntervalHelper helper = new SubsetIntervalHelper((GridAxisInterval) timeOffsetAxis);
       * return Optional.of(helper.makeSubsetByIndex(timeIdx).build());
       * }
       */
    }

    // timeOffset, timeOffsetIntv, timeLatest
    return timeOffsetAxis.subset(params, errlog);

    /*
     * timeOffset
     * Double dval = params.getTimeOffset();
     * if (dval != null) {
     * timeIdx = search(timeOffsetAxis, dval);
     * }
     * 
     * // timeOffsetIntv
     * CoordInterval intv = params.getTimeOffsetIntv();
     * if (intv != null) {
     * timeIdx = search(timeOffsetAxis, intv);
     * }
     * 
     * if (timeIdx >= 0) {
     * if (timeOffsetAxis.isInterval()) {
     * SubsetIntervalHelper helper = new SubsetIntervalHelper((GridAxisInterval) timeOffsetAxis);
     * return Optional.of(helper.makeSubsetByIndex(new Range(timeIdx, timeIdx)).build());
     * } else {
     * SubsetPointHelper helper = new SubsetPointHelper((GridAxisPoint) timeOffsetAxis);
     * return Optional.of(helper.makeSubsetByIndex(new Range(timeIdx, timeIdx)).build());
     * }
     * 
     * } else {
     * // otherwise return original axis
     * return Optional.of(timeOffsetAxis);
     * }
     */
  }

  private static int search(GridAxis<?> time, double want) {
    if (time.getNominalSize() == 1) {
      return DoubleMath.fuzzyEquals(want, time.getCoordDouble(0), 1.0e-8) ? 0 : -1;
    }
    if (time.isRegular()) {
      double fval = (want - time.getCoordDouble(0)) / time.getResolution();
      double ival = Math.rint(fval);
      return DoubleMath.fuzzyEquals(fval, ival, 1.0e-8) ? (int) ival : (int) -ival - 1; // LOOK
    }

    // otherwise do a binary search
    return time.binarySearch(want);
  }

}
