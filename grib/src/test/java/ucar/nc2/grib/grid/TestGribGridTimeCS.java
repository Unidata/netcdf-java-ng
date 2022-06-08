/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grid;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Range;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.calendar.CalendarPeriod;
import ucar.nc2.constants.AxisType;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxisDependenceType;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.grid.GridTimeCoordinateSystem;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.grid.GridTimeCoordinateSystem.Type;

/** Test {@link GribGridTimeCoordinateSystem} */
@Category(NeedsCdmUnitTest.class)
public class TestGribGridTimeCS {

  @Test
  public void testMRUTC() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "tds_index/NCEP/MRMS/Radar/MRMS_Radar_20201027_0000.grib2.ncx4";
    String gridName = "VIL_altitude_above_msl";
    testObservation(endpoint, gridName, new int[] {30}, CalendarPeriod.Field.Minute);
  }

  @Test
  public void testMRUTP() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "tds_index/NCEP/MRMS/Radar/MRMS-Radar.ncx4";
    String gridName = "MESHMax1440min_altitude_above_msl";
    testObservation(endpoint, gridName, new int[] {1476}, CalendarPeriod.Field.Minute);
  }

  public void testObservation(String endpoint, String gridName, int[] expectedTimeShape,
      CalendarPeriod.Field offsetPeriod) throws IOException {
    System.out.printf("Test Dataset %s gridName %s%n", endpoint, gridName);

    Formatter errlog = new Formatter();
    try (GribGridDataset gds = GribGridDataset.open(endpoint, errlog).orElseThrow()) {
      assertThat(gds).isNotNull();

      Grid grid = gds.findGrid(gridName).orElse(null);
      assertThat(grid).isNotNull();

      GridCoordinateSystem cs = grid.getCoordinateSystem();
      assertThat(cs).isNotNull();

      GridTimeCoordinateSystem subject = cs.getTimeCoordinateSystem();
      assertThat(subject).isNotNull();
      assertThat(subject.getNominalShape())
          .isEqualTo(Arrays.stream(expectedTimeShape).boxed().collect(Collectors.toList()));

      assertThat(subject.getType()).isEqualTo(GridTimeCoordinateSystem.Type.Observation);
      assertThat(subject.getRuntimeDateUnit()).isNotNull();
      assertThat(subject.getBaseDate()).isEqualTo(subject.getRuntimeDateUnit().getBaseDateTime());

      int ntimes = expectedTimeShape[0];
      assertThat(subject.getSubsetRanges()).isEqualTo(ImmutableList.of(new Range(ntimes)));

      assertThat((Object) subject.getRunTimeAxis()).isNull();
      assertThat(subject.getRuntimeDate(0)).isEqualTo(subject.getBaseDate());
      GridAxis<?> timeAxis = subject.getTimeOffsetAxis(0);
      assertThat((Object) timeAxis).isNotNull();
      assertThat(subject.getOffsetPeriod()).isEqualTo(CalendarPeriod.of(timeAxis.getUnits()));

      List<CalendarDate> times = subject.getTimesForRuntime(0);
      assertThat(times).hasSize(ntimes);
      CalendarDate baseDate = subject.getBaseDate();
      for (int idx = 0; idx < ntimes; idx++) {
        CalendarDate expected = baseDate.add((long) timeAxis.getCoordDouble(idx), offsetPeriod);
        assertThat(times.get(idx)).isEqualTo(expected);
      }
    }
  }

  @Test
  public void testSRC() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "tds_index/NCEP/NAM/CONUS_80km/NAM_CONUS_80km_20201027_0000.grib1.ncx4";
    String gridName = "Temperature_isobaric";
    testSingleRuntime(endpoint, gridName, new int[] {1, 11}, CalendarPeriod.Field.Hour);
  }


  @Test
  public void testEns() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ft/grid/ensemble/jitka/MOEASURGEENS20100709060002.grib";
    String gridName = "VAR10-3-192_FROM_74-0--1_surface_ens";
    testSingleRuntime(filename, gridName, new int[] {1, 477}, CalendarPeriod.Field.Minute);
  }

  @Test
  public void testEns2() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ft/grid/ensemble/jitka/ECME_RIZ_201201101200_00600_GB.ncx4";
    String gridName = "Total_precipitation_surface";
    testSingleRuntime(filename, gridName, new int[] {1, 1}, CalendarPeriod.Field.Hour);
  }

  public void testSingleRuntime(String endpoint, String gridName, int[] expectedTimeShape,
      CalendarPeriod.Field offsetPeriod) throws IOException {
    System.out.printf("Test Dataset %s gridName %s%n", endpoint, gridName);

    Formatter errlog = new Formatter();
    try (GribGridDataset gds = GribGridDataset.open(endpoint, errlog).orElseThrow()) {
      assertThat(gds).isNotNull();

      Grid grid = gds.findGrid(gridName).orElse(null);
      assertThat(grid).isNotNull();

      GridCoordinateSystem cs = grid.getCoordinateSystem();
      assertThat(cs).isNotNull();

      GridTimeCoordinateSystem subject = cs.getTimeCoordinateSystem();
      assertThat(subject).isNotNull();
      assertThat(subject.getNominalShape())
          .isEqualTo(Arrays.stream(expectedTimeShape).boxed().collect(Collectors.toList()));

      GridAxisPoint runtime = subject.getRunTimeAxis();
      assertThat((Object) runtime).isNotNull();
      CalendarDateUnit cdu = CalendarDateUnit.fromUdunitString(null, runtime.getUnits()).orElseThrow();

      assertThat(subject.getType()).isEqualTo(Type.SingleRuntime);
      assertThat(subject.getRuntimeDateUnit()).isEqualTo(cdu);
      assertThat(subject.getBaseDate()).isEqualTo(cdu.getBaseDateTime());
      assertThat(runtime.getNominalSize()).isEqualTo(1);

      int nruntimes = expectedTimeShape[0];
      int ntimes = expectedTimeShape[1];
      assertThat(subject.getSubsetRanges()).isEqualTo(ImmutableList.of(new Range(nruntimes), new Range(ntimes)));

      for (int runidx = 0; runidx < nruntimes; runidx++) {
        GridAxis<?> offset = subject.getTimeOffsetAxis(runidx);
        assertThat(offset.isInterval()).isFalse();
        assertThat(offset.getAxisType()).isEqualTo(AxisType.TimeOffset);
        assertThat(offset.getDependenceType()).isEqualTo(GridAxisDependenceType.independent);
        assertThat(offset.getDependsOn()).isEqualTo(new ArrayList<>());
        assertThat(subject.getOffsetPeriod()).isEqualTo(CalendarPeriod.of(offset.getUnits()));
      }

      for (int runidx = 0; runidx < nruntimes; runidx++) {
        GridAxis<?> offset = subject.getTimeOffsetAxis(runidx);
        CalendarDate baseForRun = subject.getRuntimeDate(runidx);
        assertThat(baseForRun).isNotNull();
        List<CalendarDate> times = subject.getTimesForRuntime(runidx);
        assertThat(times).hasSize(ntimes);
        int offsetIdx = 0;
        for (CalendarDate time : times) {
          CalendarDate expected = baseForRun.add((long) offset.getCoordDouble(offsetIdx++), offsetPeriod);
          assertThat(time).isEqualTo(expected);
        }
      }
    }
  }

  @Test
  public void testTwodRegular() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "tds_index/NCEP/NBM/Ocean/NCEP_OCEAN_MODEL_BLEND.ncx4";
    String gridName = "Wind_speed_height_above_ground";
    testOffsetRegular(endpoint, gridName, new int[] {59, 75}, CalendarPeriod.Field.Hour, Type.OffsetRegular);
  }

  @Test
  public void testTwodOrthogonal() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";
    String gridName = "Ozone_Mixing_Ratio_isobaric";
    testOffsetRegular(endpoint, gridName, new int[] {4, 93}, CalendarPeriod.Field.Hour, Type.Offset);
  }

  public void testOffsetRegular(String endpoint, String gridName, int[] expectedTimeShape,
      CalendarPeriod.Field offsetPeriod, Type type) throws IOException {
    System.out.printf("Test Dataset %s gridName %s%n", endpoint, gridName);

    Formatter errlog = new Formatter();
    try (GribGridDataset gds = GribGridDataset.open(endpoint, errlog).orElseThrow()) {
      assertThat(gds).isNotNull();

      Grid grid = gds.findGrid(gridName).orElse(null);
      assertThat(grid).isNotNull();

      GridCoordinateSystem cs = grid.getCoordinateSystem();
      assertThat(cs).isNotNull();

      GridTimeCoordinateSystem subject = cs.getTimeCoordinateSystem();
      assertThat(subject).isNotNull();
      assertThat(subject.getType()).isEqualTo(type);

      GridAxisPoint runtime = subject.getRunTimeAxis();
      assertThat((Object) runtime).isNotNull();
      CalendarDateUnit expectedCdu = CalendarDateUnit.fromUdunitString(null, runtime.getUnits()).orElseThrow();
      assertThat(subject.getRuntimeDateUnit()).isEqualTo(expectedCdu);
      assertThat(subject.getBaseDate()).isEqualTo(expectedCdu.getBaseDateTime());

      int nruntimes = expectedTimeShape[0];
      int ntimes = expectedTimeShape[1];
      assertThat(subject.getSubsetRanges()).isEqualTo(ImmutableList.of(new Range(nruntimes), new Range(ntimes)));
      assertThat(subject.getNominalShape())
          .isEqualTo(Arrays.stream(expectedTimeShape).boxed().collect(Collectors.toList()));

      for (int runidx = 0; runidx < nruntimes; runidx++) {
        GridAxis<?> offset = subject.getTimeOffsetAxis(runidx);
        assertThat(offset.isInterval()).isFalse();
        assertThat(offset.isRegular()).isFalse();
        assertThat(offset.getNominalSize()).isEqualTo(ntimes);
        assertThat(offset.getAxisType()).isEqualTo(AxisType.TimeOffset);
        assertThat(offset.getDependenceType()).isEqualTo(GridAxisDependenceType.independent);
        assertThat(offset.getDependsOn()).isEqualTo(new ArrayList<>());
        assertThat(offset.getUnits()).isEqualTo("hours");
        assertThat(subject.getOffsetPeriod()).isEqualTo(CalendarPeriod.of(offset.getUnits()));
      }

      for (int runidx = 0; runidx < nruntimes; runidx++) {
        GridAxis<?> offset = subject.getTimeOffsetAxis(runidx);
        CalendarDate baseForRun = subject.getRuntimeDate(runidx);
        assertThat(baseForRun).isNotNull();
        List<CalendarDate> times = subject.getTimesForRuntime(runidx);
        assertThat(times).hasSize(ntimes);
        int offsetIdx = 0;
        for (CalendarDate time : times) {
          CalendarDate expected = baseForRun.add((long) offset.getCoordDouble(offsetIdx++), offsetPeriod);
          assertThat(time).isEqualTo(expected);
        }
      }
    }
  }

  @Test
  public void testTwod() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "tds_index/NCEP/NDFD/NWS/NDFD_NWS_CONUS_CONDUIT.ncx4";
    String gridName = "Total_precipitation_surface_Mixed_intervals_Accumulation_probability_above_0p254";
    testOffsetIrregular(endpoint, gridName, new int[] {1479, 15}, CalendarPeriod.Field.Hour);
  }

  public void testOffsetIrregular(String endpoint, String gridName, int[] expectedTimeShape,
      CalendarPeriod.Field offsetPeriod) throws IOException {
    System.out.printf("Test Dataset %s gridName %s%n", endpoint, gridName);

    Formatter errlog = new Formatter();
    try (GribGridDataset gds = GribGridDataset.open(endpoint, errlog).orElseThrow()) {
      assertThat(gds).isNotNull();

      Grid grid = gds.findGrid(gridName).orElse(null);
      assertThat(grid).isNotNull();

      GridCoordinateSystem cs = grid.getCoordinateSystem();
      assertThat(cs).isNotNull();

      GridTimeCoordinateSystem subject = cs.getTimeCoordinateSystem();
      assertThat(subject).isNotNull();
      assertThat(subject.getNominalShape())
          .isEqualTo(Arrays.stream(expectedTimeShape).boxed().collect(Collectors.toList()));

      GridAxisPoint runtime = subject.getRunTimeAxis();
      assertThat((Object) runtime).isNotNull();
      CalendarDateUnit cdu = CalendarDateUnit.fromUdunitString(null, runtime.getUnits()).orElseThrow();

      assertThat(subject.getType()).isEqualTo(Type.OffsetIrregular);
      assertThat(subject.getRuntimeDateUnit()).isEqualTo(cdu);
      assertThat(subject.getBaseDate()).isEqualTo(cdu.getBaseDateTime());

      int nruntimes = expectedTimeShape[0];
      int ntimes = expectedTimeShape[1];
      assertThat(subject.getSubsetRanges()).isEqualTo(ImmutableList.of(new Range(nruntimes), new Range(ntimes)));

      for (int runidx = 0; runidx < nruntimes; runidx++) {
        GridAxis<?> offset = subject.getTimeOffsetAxis(runidx);
        assertThat(offset.getNominalSize()).isLessThan(ntimes + 1);
        assertThat(offset.getAxisType()).isEqualTo(AxisType.TimeOffset);
        assertThat(offset.getDependenceType()).isEqualTo(GridAxisDependenceType.independent);
        assertThat(offset.getDependsOn()).isEqualTo(new ArrayList<>());
        assertThat(subject.getOffsetPeriod()).isEqualTo(CalendarPeriod.of(offset.getUnits()));
      }

      boolean showMessage = true;
      for (int runidx = 0; runidx < nruntimes; runidx++) {
        GridAxis<?> offset = subject.getTimeOffsetAxis(runidx);
        CalendarDate baseForRun = subject.getRuntimeDate(runidx);
        assertThat(baseForRun).isNotNull();
        List<CalendarDate> times = subject.getTimesForRuntime(runidx);
        assertThat(times.size()).isLessThan(ntimes + 1);
        int offsetIdx = 0;
        for (CalendarDate time : times) {
          double val = offset.getCoordDouble(offsetIdx);
          // this only works if val is integral
          if (Misc.nearlyEquals(val, (long) val, Misc.defaultDiffFLoat)) {
            CalendarDate expected = baseForRun.add((long) offset.getCoordDouble(offsetIdx), offsetPeriod);
            if (!time.equals(expected)) {
              System.out.printf("HEY");
            }
            assertThat(time).isEqualTo(expected);
          } else if (showMessage) {
            System.out.printf(" *** need cdu.makeFractionalCalendarDate(coord) coord = %f%n", val);
            showMessage = false;
          }
          offsetIdx++;
        }
      }
    }
  }

}
