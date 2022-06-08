/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.calendar;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import ucar.unidata.util.StringUtil2;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * A CalendarPeriod is a logical duration of time, it requires a Calendar to convert to an actual duration of time.
 * A CalendarPeriod is expressed as {integer x Field}.
 */
@Immutable
public class CalendarPeriod {
  // TODO is this needed?
  private static final Cache<CalendarPeriod, CalendarPeriod> cache = CacheBuilder.newBuilder().maximumSize(100).build();

  /** 1 hour */
  public static final CalendarPeriod Hour = CalendarPeriod.of(1, Field.Hour);
  /** 1 minute */
  public static final CalendarPeriod Minute = CalendarPeriod.of(1, Field.Minute);
  /** 1 second */
  public static final CalendarPeriod Second = CalendarPeriod.of(1, Field.Second);
  /** 1 millisec */
  public static final CalendarPeriod Millisec = CalendarPeriod.of(1, Field.Millisec);

  public enum Field {
    Millisec("millisecs", ChronoUnit.MILLIS), //
    Second("seconds", ChronoUnit.SECONDS), //
    Minute("minutes", ChronoUnit.MINUTES), //
    Hour("hours", ChronoUnit.HOURS), //
    Day("days", ChronoUnit.DAYS), //
    Month("months", ChronoUnit.MONTHS), //
    Year("years", ChronoUnit.YEARS); //

    final String name;
    final ChronoUnit chronoUnit;

    Field(String name, ChronoUnit chronoUnit) {
      this.name = name;
      this.chronoUnit = chronoUnit;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  /**
   * Convert a udunit period string into a CalendarPeriod.Field.
   * 
   * @param udunit period string
   * @return CalendarPeriod.Field enum or null if not valid format
   */
  public static @Nullable Field fromUnitString(String udunit) {
    if (udunit == null) {
      return null;
    }
    udunit = udunit.trim();
    udunit = udunit.toLowerCase();

    if (udunit.equals("s")) {
      return Field.Second;
    }
    if (udunit.equals("ms")) {
      return Field.Millisec;
    }

    // eliminate plurals
    if (udunit.endsWith("s")) {
      udunit = udunit.substring(0, udunit.length() - 1);
    }

    switch (udunit) {
      case "second":
      case "sec":
      case "s":
        return Field.Second;
      case "millisecond":
      case "millisec":
      case "msec":
        return Field.Millisec;
      case "minute":
      case "min":
        return Field.Minute;
      case "hour":
      case "hr":
      case "h":
        return Field.Hour;
      case "day":
      case "d":
        return Field.Day;
      case "month":
      case "mon":
        return Field.Month;
      case "year":
      case "yr":
        return Field.Year;
      default:
        return null;
    }
  }

  // minimize memory use by interning.
  // wacko shit in GribPartitionBuilder TimeCoordinate, whoduhthunk?
  public static CalendarPeriod of(int value, Field field) {
    CalendarPeriod want = new CalendarPeriod(value, field);
    if (cache == null)
      return want;
    CalendarPeriod got = cache.getIfPresent(want);
    if (got != null)
      return got;
    cache.put(want, want);
    return want;
  }

  /**
   * Convert a udunit period string into a CalendarPeriod
   * 
   * @param udunit period string : "[val] unit"
   * @return CalendarPeriod or null if illegal format or unknown unit
   */
  @Nullable
  public static CalendarPeriod of(String udunit) {
    if (udunit == null || udunit.isEmpty()) {
      return null;
    }
    int value;
    String units;

    List<String> split = StringUtil2.splitList(udunit);
    if (split.size() == 1) {
      value = 1;
      units = split.get(0);

    } else if (split.size() == 2) {
      try {
        value = Integer.parseInt(split.get(0));
      } catch (Throwable t) {
        return null;
      }
      units = split.get(1);
    } else {
      return null;
    }

    Field unit = CalendarPeriod.fromUnitString(units);
    if (unit == null) {
      return null;
    }
    return CalendarPeriod.of(value, unit);
  }

  ////////////////////////
  // the common case of a single field
  private final int value;
  private final Field field;

  private CalendarPeriod(int value, Field field) {
    this.value = value;
    this.field = field;
  }

  /** Create a new CalendarPeriod with same Field and different value. */
  public CalendarPeriod withValue(int value) {
    return new CalendarPeriod(value, this.field);
  }

  /** The number of Fields, eg 30 minutes. */
  public int getValue() {
    return value;
  }

  /** The underlying Field, eg minutes. */
  public Field getField() {
    return field;
  }

  /** The java.time.ChronoUnit */
  public ChronoUnit getChronoUnit() {
    return field.chronoUnit;
  }

  /** Whether a field has CALENDAR on by default. True for Month and Year. */
  public boolean isDefaultCalendarField() {
    return field == Field.Month || field == Field.Year;
  }

  /**
   * Get the conversion factor of the other CalendarPeriod to this one. Cant be used for Month or Year.
   * 
   * @param from convert from this
   * @return conversion factor, so that getConvertFactor(from) * from = this
   */
  public double getConvertFactor(CalendarPeriod from) {
    return (double) from.millisecs() / millisecs();
  }

  private int millisecs() {
    if (field == Field.Millisec)
      return value;
    else if (field == Field.Second)
      return 1000 * value;
    else if (field == Field.Minute)
      return 60 * 1000 * value;
    else if (field == Field.Hour)
      return 60 * 60 * 1000 * value;
    else if (field == Field.Day)
      return 24 * 60 * 60 * 1000 * value;
    else
      throw new IllegalStateException("Illegal Field = " + field);
  }

  @Override
  public String toString() {
    return value + " " + field;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    CalendarPeriod that = (CalendarPeriod) o;
    return value == that.value && field == that.field;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, field);
  }
}
