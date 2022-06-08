/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

import org.junit.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.util.Calendar;
import java.util.Date;

public class TestTimeDuration {

  private static void doDuration(String s) {
    try {
      System.out.println("start = (" + s + ")");
      TimeDuration d = TimeDuration.parse(s);
      System.out.println("duration = (" + d.toString() + ")");
    } catch (java.text.ParseException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testStuff() {
    doDuration("3 days");
  }

  @Test
  public void testStuff2() throws DatatypeConfigurationException {
    DatatypeFactory factory = DatatypeFactory.newInstance();
    Duration d = factory.newDuration("P3D");

    long secs1 = d.getTimeInMillis(new Date()) / 1000;
    Calendar c = Calendar.getInstance();
    c.set(1970, 0, 1, 0, 0, 0);
    long secs2 = d.getTimeInMillis(c.getTime()) / 1000;

    System.out.printf("%d %d same = %s%n", secs1, secs2, secs1 == secs2);
  }

}
