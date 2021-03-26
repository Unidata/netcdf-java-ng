/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import com.google.auto.value.AutoValue;
import java.lang.Math;

/**
 * Computes the distance, azimuth, and back azimuth between
 * two lat-lon positions on the Earth's surface. Reference ellipsoid is the WGS-84 by default.
 *
 * You may use a default Earth (EarthEllipsoid.WGS84) or you may define your own using
 * a ucar.unidata.geoloc.Earth object.
 */
@AutoValue
public abstract class Bearing {
  private static final Earth defaultEarth = EarthEllipsoid.WGS84;
  private static final double EPS = 0.5E-13;
  private static final double DEGREES_TO_RADIANS = Math.toRadians(1.0);
  private static final double RADIANS_TO_DEGREES = Math.toDegrees(1.0);

  /**
   * Calculate the bearing between the 2 points.
   * See calculateBearing below.
   *
   * @param e Earth object (defines radius & flattening)
   * @param pt1 Point 1
   * @param pt2 Point 2
   * @return The bearing
   */
  public static Bearing calculateBearing(Earth e, LatLonPoint pt1, LatLonPoint pt2) {
    return calculateBearing(e, pt1.getLatitude(), pt1.getLongitude(), pt2.getLatitude(), pt2.getLongitude());
  }

  /**
   * Calculate the bearing between the 2 points.
   * See calculateBearing below. Uses default Earth object.
   *
   * @param pt1 Point 1
   * @param pt2 Point 2
   * @return The bearing
   */
  public static Bearing calculateBearing(LatLonPoint pt1, LatLonPoint pt2) {
    return calculateBearing(defaultEarth, pt1.getLatitude(), pt1.getLongitude(), pt2.getLatitude(), pt2.getLongitude());
  }

  /**
   * Computes distance (in km), azimuth (degrees clockwise positive
   * from North, 0 to 360), and back azimuth (degrees clockwise positive
   * from North, 0 to 360), from latitude-longituide point pt1 to
   * latitude-longituide pt2. Uses default Earth object.
   *
   * @param lat1 Lat of point 1
   * @param lon1 Lon of point 1
   * @param lat2 Lat of point 2
   * @param lon2 Lon of point 2
   * @return a Bearing object with distance (in km), azimuth from
   *         pt1 to pt2 (degrees, 0 = north, clockwise positive)
   */
  public static Bearing calculateBearing(double lat1, double lon1, double lat2, double lon2) {
    return calculateBearing(defaultEarth, lat1, lon1, lat2, lon2);
  }

  /**
   * Calculate a position given an azimuth and distance from
   * another point.
   *
   * @param e Earth object (defines radius and flattening)
   * @param pt1 Point 1
   * @param az azimuth (degrees)
   * @param dist distance from the point (km)
   * @return The LatLonPoint
   */
  public static LatLonPoint findPoint(Earth e, LatLonPoint pt1, double az, double dist) {
    return findPoint(e, pt1.getLatitude(), pt1.getLongitude(), az, dist);
  }

  /**
   * Calculate a position given an azimuth and distance from
   * another point. Uses default Earth.
   *
   * @param pt1 Point 1
   * @param az azimuth (degrees)
   * @param dist distance from the point (km)
   * @return The LatLonPoint
   */
  public static LatLonPoint findPoint(LatLonPoint pt1, double az, double dist) {
    return findPoint(defaultEarth, pt1.getLatitude(), pt1.getLongitude(), az, dist);
  }

  /**
   * Calculate a position given an azimuth and distance from
   * another point. See details, below. Uses default Earth.
   *
   * @param lat1 latitude of starting point
   * @param lon1 longitude of starting point
   * @param az forward azimuth (degrees)
   * @param dist distance from the point (km)
   * @return the position as a LatLonPointImpl
   * @deprecated will return LatLonPoint in 6.
   */
  public static LatLonPoint findPoint(double lat1, double lon1, double az, double dist) {
    return findPoint(defaultEarth, lat1, lon1, az, dist);
  }

  /**
   * Calculate a position given an azimuth and distance from
   * another point.
   * <p/>
   * <p/>
   * Algorithm from National Geodetic Survey, FORTRAN program "forward,"
   * subroutine "DIRCT1," by stephen j. frakes.
   * http://www.ngs.noaa.gov/TOOLS/Inv_Fwd/Inv_Fwd.html
   * <p>
   * Original documentation:
   *
   * <pre>
   *    SOLUTION OF THE GEODETIC DIRECT PROBLEM AFTER T.VINCENTY
   *    MODIFIED RAINSFORD'S METHOD WITH HELMERT'S ELLIPTICAL TERMS
   *    EFFECTIVE IN ANY AZIMUTH AND AT ANY DISTANCE SHORT OF ANTIPODAL
   * </pre>
   *
   * @param e Earth object (defines radius and flattening)
   * @param lat1 latitude of starting point
   * @param lon1 longitude of starting point
   * @param az forward azimuth (degrees)
   * @param dist distance from the point (km)
   * @return the position as a LatLonPointImpl
   * @deprecated will return LatLonPoint in 6.
   */
  public static LatLonPoint findPoint(Earth e, double lat1, double lon1, double az, double dist) {
    if (dist == 0) {
      return LatLonPoint.create(lat1, lon1);
    }

    double A = e.getMajor(); // Earth radius
    double F = e.getFlattening(); // Earth flattening value
    double R = 1.0 - F;

    // Algorithm from National Geodetic Survey, FORTRAN program "forward,"
    // subroutine "DIRCT1," by stephen j. frakes.
    // http://www.ngs.noaa.gov/TOOLS/Inv_Fwd/Inv_Fwd.html
    // Conversion to JAVA from FORTRAN was made with as few changes as
    // possible to avoid errors made while recasting form, and
    // to facilitate any future comparisons between the original
    // code and the altered version in Java.
    // Original documentation:
    // SUBROUTINE DIRCT1(GLAT1,GLON1,GLAT2,GLON2,FAZ,BAZ,S)
    //
    // SOLUTION OF THE GEODETIC DIRECT PROBLEM AFTER T.VINCENTY
    // MODIFIED RAINSFORD'S METHOD WITH HELMERT'S ELLIPTICAL TERMS
    // EFFECTIVE IN ANY AZIMUTH AND AT ANY DISTANCE SHORT OF ANTIPODAL
    //
    // A IS THE SEMI-MAJOR AXIS OF THE REFERENCE ELLIPSOID
    // F IS THE FLATTENING OF THE REFERENCE ELLIPSOID
    // LATITUDES AND LONGITUDES IN RADIANS POSITIVE NORTH AND EAST
    // AZIMUTHS IN RADIANS CLOCKWISE FROM NORTH
    // GEODESIC DISTANCE S ASSUMED IN UNITS OF SEMI-MAJOR AXIS A
    //
    // PROGRAMMED FOR CDC-6600 BY LCDR L.PFEIFER NGS ROCKVILLE MD 20FEB75
    // MODIFIED FOR SYSTEM 360 BY JOHN G GERGEN NGS ROCKVILLE MD 750608
    //

    if (az < 0.0) {
      az += 360.0; // reset azs from -180 to 180 to 0 to 360
    }
    double FAZ = az * DEGREES_TO_RADIANS;
    double GLAT1 = lat1 * DEGREES_TO_RADIANS;
    double GLON1 = lon1 * DEGREES_TO_RADIANS;
    double S = dist * 1000.; // convert to meters
    double TU = R * Math.sin(GLAT1) / Math.cos(GLAT1);
    double SF = Math.sin(FAZ);
    double CF = Math.cos(FAZ);
    double BAZ = 0.;
    if (CF != 0) {
      BAZ = Math.atan2(TU, CF) * 2;
    }
    double CU = 1. / Math.sqrt(TU * TU + 1.);
    double SU = TU * CU;
    double SA = CU * SF;
    double C2A = -SA * SA + 1.;
    double X = Math.sqrt((1. / R / R - 1.) * C2A + 1.) + 1.;
    X = (X - 2.) / X;
    double C = 1. - X;
    C = (X * X / 4. + 1) / C;
    double D = (0.375 * X * X - 1.) * X;
    TU = S / R / A / C;
    double Y = TU;
    double SY, CY, CZ, E, GLAT2, GLON2;
    do {
      SY = Math.sin(Y);
      CY = Math.cos(Y);
      CZ = Math.cos(BAZ + Y);
      E = CZ * CZ * 2. - 1.;
      C = Y;
      X = E * CY;
      Y = E + E - 1.;
      Y = (((SY * SY * 4. - 3.) * Y * CZ * D / 6. + X) * D / 4. - CZ) * SY * D + TU;
    } while (Math.abs(Y - C) > EPS);
    BAZ = CU * CY * CF - SU * SY;
    C = R * Math.sqrt(SA * SA + BAZ * BAZ);
    D = SU * CY + CU * SY * CF;
    GLAT2 = Math.atan2(D, C);
    C = CU * CY - SU * SY * CF;
    X = Math.atan2(SY * SF, C);
    C = ((-3. * C2A + 4.) * F + 4.) * C2A * F / 16.;
    D = ((E * CY * C + CZ) * SY * C + Y) * SA;
    GLON2 = GLON1 + X - (1. - C) * D * F;
    return LatLonPoint.create(GLAT2 * RADIANS_TO_DEGREES, GLON2 * RADIANS_TO_DEGREES);
  }

  /**
   * Computes distance (in km), azimuth (degrees clockwise positive
   * from North, 0 to 360), and back azimuth (degrees clockwise positive
   * from North, 0 to 360), from latitude-longituide point pt1 to
   * latitude-longituide pt2.
   * <p>
   * Algorithm from U.S. National Geodetic Survey, FORTRAN program "inverse,"
   * subroutine "INVER1," by L. PFEIFER and JOHN G. GERGEN.
   * See http://www.ngs.noaa.gov/TOOLS/Inv_Fwd/Inv_Fwd.html
   * <P>
   * Original documentation:
   * <br>
   * SOLUTION OF THE GEODETIC INVERSE PROBLEM AFTER T.VINCENTY
   * <br>
   * MODIFIED RAINSFORD'S METHOD WITH HELMERT'S ELLIPTICAL TERMS
   * <br>
   * EFFECTIVE IN ANY AZIMUTH AND AT ANY DISTANCE SHORT OF ANTIPODAL
   * <br>
   * STANDPOINT/FOREPOINT MUST NOT BE THE GEOGRAPHIC POLE
   * </P>
   * Reference ellipsoid is the WGS-84 ellipsoid.
   * <br>
   * See http://www.colorado.edu/geography/gcraft/notes/datum/elist.html
   * <p/>
   * Requires close to 1.4 E-5 seconds wall clock time per call
   * on a 550 MHz Pentium with Linux 7.2.
   *
   * @param e Earth object (defines radius and flattening)
   * @param lat1 Lat of point 1
   * @param lon1 Lon of point 1
   * @param lat2 Lat of point 2
   * @param lon2 Lon of point 2
   * @return a Bearing object with distance (in km), azimuth from
   *         pt1 to pt2 (degrees, 0 = north, clockwise positive)
   */
  public static Bearing calculateBearing(Earth e, double lat1, double lon1, double lat2, double lon2) {

    if ((lat1 == lat2) && (lon1 == lon2)) {
      return Bearing.create(0, 0, 0);
    }

    double A = e.getMajor(); // Earth radius
    double F = e.getFlattening(); // Earth flattening value
    double R = 1.0 - F;

    // Algorithm from National Geodetic Survey, FORTRAN program "inverse,"
    // subroutine "INVER1," by L. PFEIFER and JOHN G. GERGEN.
    // http://www.ngs.noaa.gov/TOOLS/Inv_Fwd/Inv_Fwd.html
    // Conversion to JAVA from FORTRAN was made with as few changes as possible
    // to avoid errors made while recasting form, and to facilitate any future
    // comparisons between the original code and the altered version in Java.
    // Original documentation:
    // SOLUTION OF THE GEODETIC INVERSE PROBLEM AFTER T.VINCENTY
    // MODIFIED RAINSFORD'S METHOD WITH HELMERT'S ELLIPTICAL TERMS
    // EFFECTIVE IN ANY AZIMUTH AND AT ANY DISTANCE SHORT OF ANTIPODAL
    // STANDPOINT/FOREPOINT MUST NOT BE THE GEOGRAPHIC POLE
    // A IS THE SEMI-MAJOR AXIS OF THE REFERENCE ELLIPSOID
    // F IS THE FLATTENING (NOT RECIPROCAL) OF THE REFERNECE ELLIPSOID
    // LATITUDES GLAT1 AND GLAT2
    // AND LONGITUDES GLON1 AND GLON2 ARE IN RADIANS POSITIVE NORTH AND EAST
    // FORWARD AZIMUTHS AT BOTH POINTS RETURNED IN RADIANS FROM NORTH
    //
    // Reference ellipsoid is the WGS-84 ellipsoid.
    // See http://www.colorado.edu/geography/gcraft/notes/datum/elist.html
    // FAZ is forward azimuth in radians from pt1 to pt2;
    // BAZ is backward azimuth from point 2 to 1;
    // S is distance in meters.
    //
    // Conversion to JAVA from FORTRAN was made with as few changes as possible
    // to avoid errors made while recasting form, and to facilitate any future
    // comparisons between the original code and the altered version in Java.
    //
    // IMPLICIT REAL*8 (A-H,O-Z)
    // COMMON/CONST/PI,RAD
    // COMMON/ELIPSOID/A,F
    double GLAT1 = DEGREES_TO_RADIANS * lat1;
    double GLAT2 = DEGREES_TO_RADIANS * lat2;
    double TU1 = R * Math.sin(GLAT1) / Math.cos(GLAT1);
    double TU2 = R * Math.sin(GLAT2) / Math.cos(GLAT2);
    double CU1 = 1. / Math.sqrt(TU1 * TU1 + 1.);
    double SU1 = CU1 * TU1;
    double CU2 = 1. / Math.sqrt(TU2 * TU2 + 1.);
    double S = CU1 * CU2;
    double BAZ = S * TU2;
    double FAZ = BAZ * TU1;
    double GLON1 = DEGREES_TO_RADIANS * lon1;
    double GLON2 = DEGREES_TO_RADIANS * lon2;
    double X = GLON2 - GLON1;
    double D, SX, CX, SY, CY, Y, SA, C2A, CZ, E, C;
    int loopCnt = 0;
    do {
      loopCnt++;
      // Check for an infinite loop
      if (loopCnt > 1000) {
        throw new IllegalArgumentException(
            "Too many iterations calculating bearing:" + lat1 + " " + lon1 + " " + lat2 + " " + lon2);
      }
      SX = Math.sin(X);
      CX = Math.cos(X);
      TU1 = CU2 * SX;
      TU2 = BAZ - SU1 * CU2 * CX;
      SY = Math.sqrt(TU1 * TU1 + TU2 * TU2);
      CY = S * CX + FAZ;
      Y = Math.atan2(SY, CY);
      SA = S * SX / SY;
      C2A = -SA * SA + 1.;
      CZ = FAZ + FAZ;
      if (C2A > 0.) {
        CZ = -CZ / C2A + CY;
      }
      E = CZ * CZ * 2. - 1.;
      C = ((-3. * C2A + 4.) * F + 4.) * C2A * F / 16.;
      D = X;
      X = ((E * CY * C + CZ) * SY * C + Y) * SA;
      X = (1. - C) * X * F + GLON2 - GLON1;
      // IF(DABS(D-X).GT.EPS) GO TO 100
    } while (Math.abs(D - X) > EPS);

    FAZ = Math.atan2(TU1, TU2);
    BAZ = Math.atan2(CU1 * SX, BAZ * CX - SU1 * CU2) + Math.PI;
    X = Math.sqrt((1. / R / R - 1.) * C2A + 1.) + 1.;
    X = (X - 2.) / X;
    C = 1. - X;
    C = (X * X / 4. + 1.) / C;
    D = (0.375 * X * X - 1.) * X;
    X = E * CY;
    S = 1. - E - E;
    S = ((((SY * SY * 4. - 3.) * S * CZ * D / 6. - X) * D / 4. + CZ) * SY * D + Y) * C * A * R;

    double distance = S / 1000.0; // meters to km
    double azimuth = FAZ * RADIANS_TO_DEGREES; // radians to degrees

    if (azimuth < 0.0) {
      azimuth += 360.0; // reset azs from -180 to 180 to 0 to 360
    }

    double backazimuth = BAZ * RADIANS_TO_DEGREES; // radians to degrees; already in 0 to 360 range

    return Bearing.create(azimuth, backazimuth, distance);
  }

  /////////////////////////////////////////////////////////////////////////////////////

  /**
   * Create a Bearing object.
   * 
   * @param azimuth the azimuth, degrees, 0 = north, clockwise positive
   * @param backazimuth the back azimuth, degrees, 0 = north, clockwise positive
   * @param distance separation in kilometers
   */
  public static Bearing create(double azimuth, double backazimuth, double distance) {
    return new AutoValue_Bearing(azimuth, backazimuth, distance);
  }

  /** Get the azimuth in degrees, 0 = north, clockwise positive */
  public abstract double getAzimuth();

  /** Get the back azimuth in degrees, 0 = north, clockwise positive */
  public abstract double getBackAzimuth();

  /** Get the distance in kilometers */
  public abstract double getDistance();
}
