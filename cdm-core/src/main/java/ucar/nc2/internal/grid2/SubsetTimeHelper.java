/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.grid2;

import com.google.common.math.DoubleMath;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.grid.GridSubset;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridAxisPoint;
import ucar.nc2.grid2.GridTimeCoordinateSystem;

import java.util.Formatter;
import java.util.Optional;

/**
 * Helper class for subsetting and searching, when you need the GridTimeCoordinateSystem.
 * Placed in this package so that its not part of the public API.
 */
public class SubsetTimeHelper {
  private final GridTimeCoordinateSystem tcs;
  public final GridAxisPoint runtimeAxis;

  public SubsetTimeHelper(GridTimeCoordinateSystem tcs) {
    this.tcs = tcs;
    this.runtimeAxis = tcs.getRunTimeAxis();
  }

  /*
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
    GridAxis<?> timeOffsetAxis = tcs.getTimeOffsetAxis(0);
    int timeIdx = -1;

    // time
    Double wantOffset = null;
    CalendarDate wantTime = params.getTime();
    if (wantTime != null) {
      wantOffset = (double) tcs.getCalendarDateUnit().makeOffsetFromRefDate(wantTime);
    } /*
       * else if (params.getTimePresent()) {
       * // time present
       * wantTime = CalendarDate.present();
       * double wantOffset = tcs.getCalendarDateUnit().makeOffsetFromRefDate(wantTime);
       * timeIdx = searchClosest(timeOffsetAxis, wantOffset);
       * if (timeIdx < 0) {
       * errlog.format("Cant find time = %s%n", wantTime);
       * return Optional.empty();
       * }
       * }
       */
    if (wantOffset != null) {
      return timeOffsetAxis.subset(GridSubset.create().setTimeOffsetCoord(wantOffset), errlog);
    }

    // timeOffset, timeOffsetIntv, timeLatest
    return timeOffsetAxis.subset(params, errlog);
  }

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

  public Optional<GridAxis<?>> subsetOffset(GridSubset params, Formatter errlog) {
    GridAxisPoint runtimeAxis = tcs.getRunTimeAxis();
    int runIdx = 0; // if nothing set, use the first one.

    if (runtimeAxis != null) {
      if (params.getRunTimeLatest()) {
        runIdx = runtimeAxis.getNominalSize() - 1;
      }

      // runtime, runtimeLatest
      CalendarDate wantRuntime = params.getRunTime();
      if (wantRuntime != null) {
        double want = tcs.getCalendarDateUnit().makeOffsetFromRefDate(wantRuntime);
        runIdx = search(tcs.getRunTimeAxis(), want);
        if (runIdx < 0) {
          errlog.format("Cant find runtime = %s%n", wantRuntime);
          return Optional.empty();
        }
      } else if (params.getRunTimeLatest()) {
        runIdx = runtimeAxis.getNominalSize() - 1; // LOOK using nominal...
      }
    }

    // suppose these were the options for time. DO they have to be processed differently for different
    // GridTimeCoordinateSystem.Type?

    int timeIdx = -1;

    /*
     * time: searching for a specific time. LOOK use Best when theres multiple.
     * CalendarDate wantTime = params.getTime();
     * if (wantTime != null) {
     * double want = tcs.getCalendarDateUnit().makeOffsetFromRefDate(wantTime);
     * timeIdx = search(this.timeOffsetAxis, want);
     * if (runIdx < 0) {
     * errlog.format("Cant find time = %s%n", wantTime);
     * return Optional.empty();
     * }
     * }
     * 
     * // timeOffset
     * Double dval = params.getTimeOffset();
     * if (dval != null) {
     * timeIdx = search(this.timeOffsetAxis, dval);
     * }
     * 
     * // timeOffsetIntv
     * CoordInterval intv = params.getTimeOffsetIntv();
     * if (intv != null) {
     * timeIdx = search(this.timeOffsetAxis, dval);
     * }
     * 
     * // otherwise return copy of the original axis
     * return time.toBuilder();
     */
    return Optional.empty();
  }

  private static int search(GridAxis<?> time, double want) {
    if (time.getNominalSize() == 1) {
      return DoubleMath.fuzzyEquals(want, time.getCoordMidpoint(0), 1.0e-8) ? 0 : -1;
    }
    if (time.isRegular()) {
      double fval = (want - time.getCoordMidpoint(0)) / time.getResolution();
      double ival = Math.rint(fval);
      return DoubleMath.fuzzyEquals(fval, ival, 1.0e-8) ? (int) ival : (int) -ival - 1; // LOOK
    }

    // otherwise do a binary search
    return time.binarySearch(want);
  }

}
