/* Copyright Unidata */
package ucar.unidata.geoloc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.util.Misc;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/** Test that the algorithm for longitude normalization works */
@RunWith(Parameterized.class)
public class TestLongitudeNormalization {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[] {100.0, -100.0, 0.0});
    result.add(new Object[] {-100.0, 100.0, 360.0});
    result.add(new Object[] {-100.0, -180.0, 0.0});
    result.add(new Object[] {-180.0, -100.0, 360.0});
    result.add(new Object[] {-180.0, 180.0, 360.0});
    result.add(new Object[] {181.0, -180.0, -360.0});
    result.add(new Object[] {181.0, -200.0, -360.0});
    result.add(new Object[] {-200.0, 200.0, 720.0});
    result.add(new Object[] {-179.0, 180.0, 360.0});

    return result;
  }

  double lon, from;
  Double expectedDiff;

  public TestLongitudeNormalization(double lon, double from, Double expectedDiff) {
    this.lon = lon;
    this.from = from;
    this.expectedDiff = expectedDiff;
  }

  @Test
  public void doit() {
    double compute = lonNormalFrom(lon, from);

    if (expectedDiff != null) {
      logger.debug("({} from {}) = {}, diff = {} expectedDiff {}", lon, from, compute, compute - lon, expectedDiff);
      assertThat(Misc.nearlyEquals(expectedDiff, compute - lon)).isTrue();
    } else {
      logger.debug("({} from {}) = {}, diff = {}", lon, from, compute, compute - lon);
    }

    String msg = String.format("(%f from %f) = %f%n", lon, from, compute);
    assertWithMessage(msg).that(compute).isAtLeast(from);
    assertWithMessage(msg).that(compute).isAtMost(from + 360);
  }

  /**
   * put longitude into the range [start, start+360] deg
   *
   * @param lon lon to normalize
   * @param start starting point
   * @return longitude into the range [center +/- 180] deg
   */
  static public double lonNormalFrom(double lon, double start) {
    while (lon < start)
      lon += 360;
    while (lon > start + 360)
      lon -= 360;
    return lon;
  }
}
