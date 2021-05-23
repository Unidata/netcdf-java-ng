package ucar.nc2.time2;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link CalendarDateUnit} */
public class TestCalendarDateUnit {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testFromUdunitString() {
    testUnit("days", false);
    testUnit("hours", false);
    testUnit("months", true);
    testUnit("years", true);
  }

  private void testUnit(String unitP, boolean badok) {
    String unit = unitP + " since 2008-02-29";
    CalendarDateUnit cdu = CalendarDateUnit.fromUdunitString(null, unit).orElseThrow();
    assertThat(cdu.getCalendar()).isEqualTo(Calendar.getDefault());
    assertThat(cdu.getCalendarPeriod()).isEqualTo(CalendarPeriod.of(unitP));
    assertThat(cdu.getCalendarField()).isEqualTo(CalendarPeriod.fromUnitString(unitP));
    assertThat(cdu.isCalendarField()).isEqualTo(false);

    for (int i = 0; i < 13; i++) {
      CalendarDate cd = cdu.makeCalendarDate(i);
      System.out.printf("%d %s == %s%n", i, cdu, CalendarDateFormatter.toDateTimeStringISO(cd));

      // LOOK note that this fails for month, year
      if (!badok) {
        assertThat(cdu.makeOffsetFromRefDate(cd)).isEqualTo(i);
      }
    }
    System.out.printf("%n");
  }

  @Test
  public void testCalendarUnit() {
    testCalendarUnit("days", CalendarPeriod.Field.Day);
    testCalendarUnit("hours", CalendarPeriod.Field.Hour);
    testCalendarUnit("months", CalendarPeriod.Field.Month);
    testCalendarUnit("years", CalendarPeriod.Field.Year);
  }

  private void testCalendarUnit(String unitP, CalendarPeriod.Field field) {
    String bases = "2008-03-31";
    String unit = "calendar " + unitP + " since  " + bases;
    CalendarDate baseDate = CalendarDate.fromUdunitIsoDate(null, bases).orElseThrow();
    CalendarDateUnit cdu = CalendarDateUnit.fromUdunitString(null, unit).orElseThrow();

    assertThat(cdu.getCalendar()).isEqualTo(Calendar.getDefault());
    assertThat(cdu.getCalendarPeriod()).isEqualTo(CalendarPeriod.of(unitP));
    assertThat(cdu.getCalendarField()).isEqualTo(CalendarPeriod.fromUnitString(unitP));
    assertThat(cdu.isCalendarField()).isEqualTo(true);

    for (int i = 0; i < 15; i++) {
      CalendarDate cd = cdu.makeCalendarDate(i);
      System.out.printf("%2d %s == %s", i, cdu, CalendarDateFormatter.toDateTimeStringISO(cd));
      CalendarDate expected = baseDate.add(1, CalendarPeriod.of(i, field));
      assertThat(cd).isEqualTo(expected);
      long offset = cdu.makeOffsetFromRefDate(cd);
      System.out.printf(" (%d) %s%n", offset, offset == i ? "" : "***");
      // assertThat(cdu.makeOffsetFromRefDate(cd)).isEqualTo(i);
    }

    for (int i = 0; i < 13; i++) {
      CalendarDate cd = cdu.makeCalendarDate(i * 10);
      System.out.printf("%2d %s == %s", i * 10, cdu, CalendarDateFormatter.toDateTimeStringISO(cd));
      CalendarDate expected = baseDate.add(1, CalendarPeriod.of(i * 10, field));
      assertThat(cd).isEqualTo(expected);
      long offset = cdu.makeOffsetFromRefDate(cd);
      System.out.printf(" (%d) %s%n", offset, offset == i * 10 ? "" : "***");

      // assertThat(cdu.makeOffsetFromRefDate(cd)).isEqualTo(i * 10);
    }
    System.out.printf("%n");
  }

  @Test
  public void testBasics() {
    CalendarDateUnit cdu = CalendarDateUnit.fromUdunitString(Calendar.julian, "years since 1970-01-01").orElseThrow();
    assertThat(cdu.getCalendar()).isEqualTo(Calendar.julian);
    assertThat(cdu.getCalendarPeriod()).isEqualTo(CalendarPeriod.of("years"));
    assertThat(cdu.getCalendarField()).isEqualTo(CalendarPeriod.fromUnitString("years"));
    assertThat(cdu.isCalendarField()).isEqualTo(false);

    CalendarDate refDate = CalendarDate.fromUdunitIsoDate("julian", "1970-01-01").orElseThrow();
    assertThat(cdu.getBaseDateTime()).isEqualTo(refDate);

    CalendarDateUnit cdu2 = CalendarDateUnit.of(cdu.getCalendar(), cdu.getCalendarField(), refDate.getOffsetDateTime(),
        cdu.isCalendarField());
    assertThat(cdu2).isEqualTo(cdu);
    assertThat(cdu2.hashCode()).isEqualTo(cdu.hashCode());
  }

  @Test
  public void testBig() {
    CalendarDateUnit cdu = CalendarDateUnit.fromUdunitString(null, "years since 1970-01-01").orElseThrow();
    long val = 50 * 1000 * 1000;
    CalendarDate cd = cdu.makeCalendarDate(val);
    System.out.printf("%d %s == %s%n", val, cdu, CalendarDateFormatter.toDateTimeStringISO(cd));
    assertThat(cd.toString()).isEqualTo("+50001970-01-01T00:00Z");

    cdu = CalendarDateUnit.fromUdunitString(null, "calendar years since 1970-01-01").orElseThrow();;
    cd = cdu.makeCalendarDate(val);
    System.out.printf("%n%d %s == %s%n", val, cdu, CalendarDateFormatter.toDateTimeStringISO(cd));
    assertThat(cd.toString()).isEqualTo("+50001970-01-01T00:00Z");
  }

}
