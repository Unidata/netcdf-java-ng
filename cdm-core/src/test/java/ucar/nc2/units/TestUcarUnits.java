/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.units;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.util.Misc;
import ucar.units.*;
import java.lang.invoke.MethodHandles;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link ucar.units} */
public class TestUcarUnits {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testBasic() throws UnitException {
    UnitFormat format = UnitFormatManager.instance();

    Unit meter = format.parse("meter");
    Unit second = format.parse("second");
    Unit meterPerSecondUnit = meter.divideBy(second);
    Unit knot = format.parse("knot");
    assertThat(meterPerSecondUnit.isCompatible(knot)).isTrue();

    logger.debug("5 knots is {} {}", knot.convertTo(5, meterPerSecondUnit), format.format(meterPerSecondUnit));
    assertThat(Misc.nearlyEquals(2.5722222f, knot.convertTo(5, meterPerSecondUnit))).isTrue();
  }

  @Test
  public void testTimeConversion() throws UnitException {
    UnitFormat format = UnitFormatManager.instance();
    Unit t1, t2;

    t1 = format.parse("secs since 1999-01-01 00:00:00");
    t2 = format.parse("secs since 1999-01-02 00:00:00");
    assertThat(t1.isCompatible(t2)).isTrue();

    logger.debug("t2.convertTo(0.0, t1) = {}", t2.convertTo(0.0, t1));
    assertThat(Misc.nearlyEquals(86400.0, t2.convertTo(0.0, t1))).isTrue();
  }

  @Test
  public void testTimeConversion2() throws UnitException {
    UnitFormat format = UnitFormatManager.instance();
    Unit t1, t2;

    t1 = format.parse("hours since 1999-01-01 00:00:00");
    t2 = format.parse("hours since 1999-01-02 00:00:00");
    assertThat(t1.isCompatible(t2)).isTrue();

    assertThat(Misc.nearlyEquals(24, t2.convertTo(0.0, t1))).isTrue();
  }

  @Test
  public void testException() throws UnitException {
    UnitFormat format = UnitFormatManager.instance();
    Unit uu = format.parse("barf");
    logger.debug("Parse ok = {}", uu);
  }
}
