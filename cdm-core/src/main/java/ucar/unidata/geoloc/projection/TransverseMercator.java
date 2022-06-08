/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.projection;


import com.google.common.math.DoubleMath;
import javax.annotation.concurrent.Immutable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;
import ucar.unidata.util.SpecialMathFunction;

/**
 * Transverse Mercator projection, spherical earth.
 * Projection plane is a cylinder tangent to the earth at tangentLon.
 * See John Snyder, Map Projections used by the USGS, Bulletin 1532, 2nd edition (1983), p 53
 */
@Immutable
public class TransverseMercator extends AbstractProjection {

  private final double lat0, lon0, scale, earthRadius;
  private final double falseEasting, falseNorthing;

  // values passed in through the constructor
  // need for constructCopy
  private final double _lat0, _lon0, _scale;

  @Override
  public AbstractProjection constructCopy() {
    return new TransverseMercator(getOriginLat(), getTangentLon(), getScale(), getFalseEasting(), getFalseNorthing(),
        getEarthRadius());
  }

  /**
   * Constructor with default parameteres
   */
  public TransverseMercator() {
    this(40.0, -105.0, .9996);
  }

  /**
   * Construct a TransverseMercator Projection.
   *
   * @param lat0 origin of projection coord system is at (lat0, tangentLon)
   * @param tangentLon longitude that the cylinder is tangent at ("central meridian")
   * @param scale scale factor along the central meridian
   */
  public TransverseMercator(double lat0, double tangentLon, double scale) {
    this(lat0, tangentLon, scale, 0.0, 0.0, EARTH_RADIUS);
  }

  /**
   * Construct a TransverseMercator Projection.
   *
   * @param lat0 origin of projection coord system is at (lat0, tangentLon)
   * @param tangentLon longitude that the cylinder is tangent at ("central meridian")
   * @param scale scale factor along the central meridian
   * @param east false easting in units of km
   * @param north false northing in units of km
   * @param radius earth radius in km
   */
  public TransverseMercator(double lat0, double tangentLon, double scale, double east, double north, double radius) {
    super("TransverseMercator", false);

    this._lon0 = tangentLon;
    this._lat0 = lat0;
    this._scale = scale;

    this.lat0 = Math.toRadians(lat0);
    this.lon0 = Math.toRadians(tangentLon);
    this.earthRadius = radius;
    this.scale = scale * earthRadius;
    this.falseEasting = (!Double.isNaN(east)) ? east : 0.0;
    this.falseNorthing = (!Double.isNaN(north)) ? north : 0.0;

    addParameter(CF.GRID_MAPPING_NAME, CF.TRANSVERSE_MERCATOR);
    addParameter(CF.LONGITUDE_OF_CENTRAL_MERIDIAN, tangentLon);
    addParameter(CF.LATITUDE_OF_PROJECTION_ORIGIN, lat0);
    addParameter(CF.SCALE_FACTOR_AT_CENTRAL_MERIDIAN, scale);
    addParameter(CF.EARTH_RADIUS, earthRadius * 1000);

    if ((falseEasting != 0.0) || (falseNorthing != 0.0)) {
      addParameter(CF.FALSE_EASTING, falseEasting);
      addParameter(CF.FALSE_NORTHING, falseNorthing);
      addParameter(CDM.UNITS, "km");
    }
  }

  // bean properties

  /**
   * Get the scale
   *
   * @return the scale
   */
  public double getScale() {
    return _scale;
  }


  /**
   * Get the tangent longitude in degrees
   *
   * @return the origin longitude in degrees.
   */
  public double getTangentLon() {
    return _lon0;
  }

  /**
   * Get the origin latitude in degrees
   *
   * @return the origin latitude in degrees.
   */
  public double getOriginLat() {
    return _lat0;
  }

  /**
   * Get the false easting, in units of km.
   *
   * @return the false easting.
   */
  public double getFalseEasting() {
    return falseEasting;
  }

  /**
   * Get the false northing, in units of km
   *
   * @return the false northing.
   */
  public double getFalseNorthing() {
    return falseNorthing;
  }

  public double getEarthRadius() {
    return earthRadius;
  }

  @Override
  public String toString() {
    return "TransverseMercator{" + "lat0=" + lat0 + ", lon0=" + lon0 + ", scale=" + scale + ", earthRadius="
        + earthRadius + ", falseEasting=" + falseEasting + ", falseNorthing=" + falseNorthing + '}';
  }

  /**
   * Does the line between these two points cross the projection "seam".
   *
   * @param pt1 the line goes between these two points
   * @param pt2 the line goes between these two points
   * @return false if there is no seam
   */
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    // either point is infinite
    if (LatLonPoints.isInfinite(pt1) || LatLonPoints.isInfinite(pt2)) {
      return true;
    }

    double y1 = pt1.getY() - falseNorthing;
    double y2 = pt2.getY() - falseNorthing;

    // opposite signed long lines
    return (y1 * y2 < 0) && (Math.abs(y1 - y2) > 2 * earthRadius);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    TransverseMercator that = (TransverseMercator) o;
    double tolerance = 1e-6;

    if (DoubleMath.fuzzyCompare(that.earthRadius, earthRadius, tolerance) != 0)
      return false;
    if (DoubleMath.fuzzyCompare(that.falseEasting, falseEasting, tolerance) != 0)
      return false;
    if (DoubleMath.fuzzyCompare(that.falseNorthing, falseNorthing, tolerance) != 0)
      return false;
    if (DoubleMath.fuzzyCompare(that.lat0, lat0, tolerance) != 0)
      return false;
    if (DoubleMath.fuzzyCompare(that.lon0, lon0, tolerance) != 0)
      return false;
    if (DoubleMath.fuzzyCompare(that.scale, scale, tolerance) != 0)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = lat0 != +0.0d ? Double.doubleToLongBits(lat0) : 0L;
    result = (int) (temp ^ (temp >>> 32));
    temp = lon0 != +0.0d ? Double.doubleToLongBits(lon0) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = scale != +0.0d ? Double.doubleToLongBits(scale) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = earthRadius != +0.0d ? Double.doubleToLongBits(earthRadius) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = falseEasting != +0.0d ? Double.doubleToLongBits(falseEasting) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = falseNorthing != +0.0d ? Double.doubleToLongBits(falseNorthing) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latLon) {
    double toX, toY;
    double fromLat = latLon.getLatitude();
    double fromLon = latLon.getLongitude();

    double lon = Math.toRadians(fromLon);
    double lat = Math.toRadians(fromLat);
    double dlon = lon - lon0;
    double b = Math.cos(lat) * Math.sin(dlon);

    if ((Math.abs(Math.abs(b) - 1.0)) < TOLERANCE) { // infinite projection
      toX = Double.POSITIVE_INFINITY;
      toY = Double.POSITIVE_INFINITY;
    } else {
      toX = scale * SpecialMathFunction.atanh(b);
      toY = scale * (Math.atan2(Math.tan(lat), Math.cos(dlon)) - lat0);
    }

    return ProjectionPoint.create(toX + falseEasting, toY + falseNorthing);
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint world) {
    double toLat, toLon;
    double fromX = world.getX();
    double fromY = world.getY();

    double x = (fromX - falseEasting) / scale;
    double d = (fromY - falseNorthing) / scale + lat0;
    toLon = Math.toDegrees(lon0 + Math.atan2(Math.sinh(x), Math.cos(d)));
    toLat = Math.toDegrees(Math.asin(Math.sin(d) / Math.cosh(x)));
    return LatLonPoint.create(toLat, toLon);
  }

}


