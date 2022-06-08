/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
/*
 * Copyright 2006 Jerry Huxtable
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file was semi-automatically converted from the public-domain USGS PROJ source.
 *
 * Changes by Bernhard Jenny, May 2007: added missing toString() and
 * isEqualArea(); this class now derives from CylindricalProjection instead of
 * Projection; removed isRectilinear, which is defined in the new superclass
 * CylindricalProjection; and removed trueScaleLatitude that did hide a field
 * of the Projection class of the same name.
 */
package ucar.unidata.geoloc.projection.proj4;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;
import ucar.nc2.Attribute;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.AbstractProjection;
import java.util.Formatter;

/**
 * Adapted from com.jhlabs.map.proj.LambertConformalConicProjection
 *
 * @see "http://www.jhlabs.com/java/maps/proj/index.html"
 * @see "http://trac.osgeo.org/proj/"
 */
@Immutable
public class LambertConformalConicEllipse extends AbstractProjection {
  private static final double TOL = 1.0e-10;

  // projection parameters
  private final double lat0deg, lon0deg; // projection origin, degrees
  private final double lat0rad, lon0rad; // projection origin, radians
  private final double par1deg, par2deg; // standard parellels, degrees
  private final double par1rad, par2rad; // standard parellels, radians
  private final double falseEasting, falseNorthing; // km

  // earth shape
  private final Earth earth;
  private final double e; // earth.getEccentricitySquared
  private final double totalScale; // scale to convert cartesian coords in km

  private final double n;
  private final double c;
  private final double rho0;

  // spherical vs ellipsoidal
  private final boolean isSpherical;

  @Override
  public Projection constructCopy() {
    return new LambertConformalConicEllipse(getOriginLat(), getOriginLon(), getParallelOne(), getParallelTwo(),
        getFalseEasting(), getFalseNorthing(), getEarth());
  }

  /**
   * Constructor with default parameters
   */
  public LambertConformalConicEllipse() {
    this(23.0, -96.0, 29.5, 45.5, 0, 0, new Earth(6378137.0, 0.0, 298.257222101));
  }

  /**
   * Construct a LambertConformal Projection, two standard parellels.
   * For the one standard parellel case, set them both to the same value.
   *
   * @param lat0 lat origin of the coord. system on the projection plane
   * @param lon0 lon origin of the coord. system on the projection plane
   * @param par1 standard parallel 1
   * @param par2 standard parallel 2
   * @param falseEasting natural_x_coordinate + false_easting = x coordinate in km
   * @param falseNorthing natural_y_coordinate + false_northing = y coordinate in km
   * @param earth shape of the earth
   * @throws IllegalArgumentException if lat0, par1, par2 = +/-90 deg
   */
  public LambertConformalConicEllipse(double lat0, double lon0, double par1, double par2, double falseEasting,
      double falseNorthing, Earth earth) {

    super("LambertConformalConicEllipse", false);

    this.lat0deg = lat0;
    this.lon0deg = lon0;

    this.lat0rad = Math.toRadians(lat0);
    this.lon0rad = Math.toRadians(lat0);

    this.par1deg = par1;
    this.par2deg = par2;

    if (par1 == 0) {
      this.par1rad = lat0rad;
      this.par2rad = lat0rad;
    } else {
      this.par1rad = Math.toRadians(par1);
      this.par2rad = Math.toRadians(par2);
    }

    this.falseEasting = falseEasting;
    this.falseNorthing = falseNorthing;

    this.earth = earth;
    this.e = earth.getEccentricity();
    double es = earth.getEccentricitySquared();
    this.isSpherical = earth.isSpherical();
    this.totalScale = earth.getMajor() * .001; // scale factor for cartesion coords in km.

    if (Math.abs(par1rad + par2rad) < TOL)
      throw new IllegalArgumentException("par1rad + par2rad < TOL");

    double sinphi = Math.sin(par1rad);
    double cosphi = Math.cos(par1rad);
    boolean isSecant = Math.abs(par1rad - par2rad) >= TOL;

    double nTemp = sinphi;
    double rho0Temp;

    if (!isSpherical) {
      double ml1, m1;

      m1 = MapMath.msfn(sinphi, cosphi, es);
      ml1 = MapMath.tsfn(par1rad, sinphi, e);
      if (isSecant) {
        nTemp = Math.log(m1 / MapMath.msfn(sinphi = Math.sin(par2rad), Math.cos(par2rad), es));
        nTemp /= Math.log(ml1 / MapMath.tsfn(par2rad, sinphi, e));
      }
      rho0Temp = m1 * Math.pow(ml1, -nTemp) / nTemp;
      c = rho0Temp;
      rho0Temp *= (Math.abs(Math.abs(lat0rad) - MapMath.HALFPI) < TOL) ? 0.
          : Math.pow(MapMath.tsfn(lat0rad, Math.sin(lat0rad), e), nTemp);

    } else {
      if (isSecant) {
        nTemp = Math.log(cosphi / Math.cos(par2rad))
            / Math.log(Math.tan(MapMath.QUARTERPI + .5 * par2rad) / Math.tan(MapMath.QUARTERPI + .5 * par1rad));
      }

      c = cosphi * Math.pow(Math.tan(MapMath.QUARTERPI + .5 * par1rad), nTemp) / nTemp;
      rho0Temp = (Math.abs(Math.abs(lat0rad) - MapMath.HALFPI) < TOL) ? 0.
          : c * Math.pow(Math.tan(MapMath.QUARTERPI + .5 * lat0rad), -nTemp);
    }

    this.n = nTemp;
    this.rho0 = rho0Temp;

    addParameter(CF.GRID_MAPPING_NAME, CF.LAMBERT_CONFORMAL_CONIC);
    addParameter(CF.LATITUDE_OF_PROJECTION_ORIGIN, lat0);
    addParameter(CF.LONGITUDE_OF_CENTRAL_MERIDIAN, lon0);
    if (par2 == par1) {
      addParameter(CF.STANDARD_PARALLEL, par1);
    } else {
      addParameter(Attribute.builder(CF.STANDARD_PARALLEL).setValues(ImmutableList.of(par1, par2), false).build());
    }

    if ((falseEasting != 0.0) || (falseNorthing != 0.0)) {
      addParameter(CF.FALSE_EASTING, falseEasting);
      addParameter(CF.FALSE_NORTHING, falseNorthing);
      addParameter(CDM.UNITS, "km");
    }

    addParameter(CF.SEMI_MAJOR_AXIS, earth.getMajor());
    addParameter(CF.INVERSE_FLATTENING, 1.0 / earth.getFlattening());

  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    LambertConformalConicEllipse that = (LambertConformalConicEllipse) o;

    if (Double.compare(that.falseEasting, falseEasting) != 0)
      return false;
    if (Double.compare(that.falseNorthing, falseNorthing) != 0)
      return false;
    if (Double.compare(that.lat0rad, lat0rad) != 0)
      return false;
    if (Double.compare(that.lon0rad, lon0rad) != 0)
      return false;
    if (Double.compare(that.par1rad, par1rad) != 0)
      return false;
    if (Double.compare(that.par2rad, par2rad) != 0)
      return false;
    return earth.equals(that.earth);
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(lat0rad);
    result = (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(lon0rad);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(par1rad);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(par2rad);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(falseEasting);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(falseNorthing);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    result = 31 * result + earth.hashCode();
    return result;
  }

  // bean properties

  public Earth getEarth() {
    return earth;
  }

  /**
   * Get the second standard parallel
   *
   * @return the second standard parallel in degrees
   */
  public double getParallelTwo() {
    return par2deg;
  }

  /**
   * Get the first standard parallel
   *
   * @return the first standard parallel in degrees
   */
  public double getParallelOne() {
    return par1deg;
  }

  /**
   * Get the origin longitude.
   *
   * @return the origin longitude in degrees
   */
  public double getOriginLon() {
    return lon0deg;
  }


  /**
   * Get the origin latitude.
   *
   * @return the origin latitude in degrees
   */
  public double getOriginLat() {
    return lat0deg;
  }

  /**
   * Get the false easting, in km.
   *
   * @return the false easting in km
   */
  public double getFalseEasting() {
    return falseEasting;
  }

  /**
   * Get the false northing, in km.
   *
   * @return the false northing in km
   */
  public double getFalseNorthing() {
    return falseNorthing;
  }

  /**
   * This returns true when the line between pt1 and pt2 crosses the seam.
   * When the cone is flattened, the "seam" is lon0 +- 180.
   *
   * @param pt1 point 1
   * @param pt2 point 2
   * @return true when the line between pt1 and pt2 crosses the seam.
   */
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    return LatLonPoints.isInfinite(pt1) || LatLonPoints.isInfinite(pt2);
  }

  private double computeTheta(double lon) {
    double dlon = LatLonPoints.lonNormal(lon - lon0deg);
    return n * Math.toRadians(dlon);
  }

  // also see Snyder p 101
  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latLon) {
    double fromLat = Math.toRadians(latLon.getLatitude());
    double theta = computeTheta(latLon.getLongitude());

    double rho = 0.0;
    if (Math.abs(Math.abs(fromLat) - MapMath.HALFPI) >= TOL) {
      double term;
      if (isSpherical)
        term = Math.pow(Math.tan(MapMath.QUARTERPI + .5 * fromLat), -n);
      else
        term = Math.pow(MapMath.tsfn(fromLat, Math.sin(fromLat), e), n);
      rho = c * term;
    }

    double toX = (rho * Math.sin(theta));
    double toY = (rho0 - rho * Math.cos(theta));

    return ProjectionPoint.create(totalScale * toX + falseEasting, totalScale * toY + falseNorthing);
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint world) {
    double toLat, toLon;
    double fromX = (world.getX() - falseEasting) / totalScale; // assumes cartesion coords in km
    double fromY = (world.getY() - falseNorthing) / totalScale;

    fromY = rho0 - fromY;
    double rho = MapMath.distance(fromX, fromY);
    if (rho != 0) {
      if (n < 0.0) {
        rho = -rho;
        fromX = -fromX;
        fromY = -fromY;
      }
      if (isSpherical)
        toLat = 2.0 * Math.atan(Math.pow(c / rho, 1.0 / n)) - MapMath.HALFPI;
      else
        toLat = MapMath.phi2(Math.pow(rho / c, 1.0 / n), e);

      toLon = Math.atan2(fromX, fromY) / n;
      // coverity[swapped_arguments]

    } else {
      toLon = 0.0;
      toLat = n > 0.0 ? MapMath.HALFPI : -MapMath.HALFPI;
    }

    return LatLonPoint.create(Math.toDegrees(toLat), Math.toDegrees(toLon) + lon0deg);
  }

}
