/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.projection;

import org.junit.Before;
import org.junit.Test;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.EarthEllipsoid;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionRect;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/** Test {@link ucar.unidata.geoloc.projection.LatLonProjection} */
public class TestLatLonProjection {

  private LatLonProjection p;

  @Before
  public void setUp() {
    p = new LatLonProjection();
  }

  @Test
  public void testBasics() {
    assertThat(p.getName()).isEqualTo("LatLonProjection");
    assertThat(p.getCenterLon()).isEqualTo(0.0);
    assertThat(p.getEarth()).isEqualTo(new Earth());
    assertThat(p.toString()).isEqualTo(
        "LatLonProjection{centerLon=0.0, earth=spherical_earth equatorRadius=6371229.000000 inverseFlattening=Infinity}");

    Projection p2 = p.constructCopy();
    assertThat(p).isEqualTo(p2);
    assertThat(p.hashCode()).isEqualTo(p2.hashCode());
    assertThat(p.toString()).isEqualTo(p2.toString());
  }

  @Test
  public void testEarth() {
    LatLonProjection ell = new LatLonProjection("earth", EarthEllipsoid.WGS84, 15.0);
    assertThat(ell.getName()).isEqualTo("earth");
    assertThat(ell.getCenterLon()).isEqualTo(15.0);
    assertThat(ell.getEarth()).isEqualTo(EarthEllipsoid.WGS84);
    assertThat(ell.toString()).isEqualTo("LatLonProjection{centerLon=15.0, earth=WGS84}");

    Projection p2 = ell.constructCopy();
    assertThat(p2).isEqualTo(ell);
    assertThat(p2.hashCode()).isEqualTo(ell.hashCode());
    assertThat(p2.toString()).isEqualTo(ell.toString());
  }

  @Test
  public void testLatLonToProjBB() {
    runCenter();
    runCenter(110.45454545454547);
    runCenter(-110.45454545454547);
    runCenter(0.0);
    runCenter(420.0);
  }

  void runCenter() {
    double xinc = 22.5;
    double yinc = 20.0;
    for (double lon = 0.0; lon < 380.0; lon += xinc) {
      LatLonPoint ptL = LatLonPoint.create(-73.79, lon);
      LatLonRect llbb = LatLonRect.builder(ptL, yinc, xinc).build();

      ProjectionRect ma2 = p.latLonToProjBB(llbb);
      LatLonRect p2 = p.projToLatLonBB(ma2);

      assertThat(llbb.nearlyEquals(p2)).isTrue();
    }
  }

  void runCenter(double center) {
    double xinc = 22.5;
    double yinc = 20.0;
    for (double lon = 0.0; lon < 380.0; lon += xinc) {
      LatLonPoint ptL = LatLonPoint.create(0, center + lon);
      LatLonRect llbb = LatLonRect.builder(ptL, yinc, xinc).build();

      ProjectionRect ma2 = p.latLonToProjBB(llbb);
      LatLonRect p2 = p.projToLatLonBB(ma2);

      assertThat(llbb.nearlyEquals(p2)).isTrue();
    }
  }

  public LatLonRect testIntersection(LatLonRect bbox, LatLonRect bbox2) {
    LatLonRect result = bbox.intersect(bbox2);
    if (result != null) {
      assertWithMessage("bbox= " + bbox.toString2() + "\nbbox2= " + bbox2.toString2() + "\nintersect= "
          + (result == null ? "null" : result.toString2())).that(bbox.intersect(bbox2))
              .isEqualTo(bbox2.intersect(bbox));
    }
    return result;
  }

  @Test
  public void testIntersection() {
    LatLonRect bbox = LatLonRect.builder(LatLonPoint.create(40.0, -100.0), 10.0, 20.0).build();
    LatLonRect bbox2 = LatLonRect.builder(LatLonPoint.create(-40.0, -180.0), 120.0, 300.0).build();
    assertThat(testIntersection(bbox, bbox2)).isNotNull();

    bbox = LatLonRect.builder(LatLonPoint.create(-90.0, -100.0), 90.0, 300.0).build();
    bbox2 = LatLonRect.builder(LatLonPoint.create(-40.0, -180.0), 120.0, 300.0).build();
    assertThat(testIntersection(bbox, bbox2)).isNotNull();

    bbox2 = LatLonRect.builder(LatLonPoint.create(10, -180.0), 120.0, 300.0).build();
    assertThat(testIntersection(bbox, bbox2)).isNull();

    bbox = LatLonRect.builder(LatLonPoint.create(-90.0, -100.0), 90.0, 200.0).build();
    bbox2 = LatLonRect.builder(LatLonPoint.create(-40.0, 120.0), 120.0, 300.0).build();
    assertThat(testIntersection(bbox, bbox2)).isNotNull();

    bbox = LatLonRect.builder(LatLonPoint.create(-90.0, -100.0), 90.0, 200.0).build();
    bbox2 = LatLonRect.builder(LatLonPoint.create(-40.0, -220.0), 120.0, 140.0).build();
    assertThat(testIntersection(bbox, bbox2)).isNotNull();
  }

  private LatLonRect testExtend(LatLonRect.Builder bbox, LatLonRect bbox2) {
    bbox.extend(bbox2);
    return bbox.build();
  }

  @Test
  public void testExtend() {
    LatLonRect bbox;

    bbox = testExtend(LatLonRect.builder(LatLonPoint.create(-81.0, 30.0), LatLonPoint.create(-60.0, 120.0)),
        LatLonRect.builder(LatLonPoint.create(-81.0, -10.0), LatLonPoint.create(-60.0, 55.0)).build());
    assertThat(bbox.getWidth()).isWithin(0.01).of(130.0);
    assertThat(bbox.crossDateline()).isFalse();

    bbox = testExtend(LatLonRect.builder(LatLonPoint.create(-81.0, -200.0), LatLonPoint.create(-60.0, -100.0)),
        LatLonRect.builder(LatLonPoint.create(-81.0, 177.0), LatLonPoint.create(-60.0, 200.0)).build());
    assertThat(bbox.getWidth()).isWithin(0.01).of(100.0);
    assertThat(bbox.crossDateline()).isTrue();

    // ---------
    // --------------
    bbox = testExtend(LatLonRect.builder(LatLonPoint.create(-81.0, -200.0), LatLonPoint.create(-60.0, -100.0)),
        LatLonRect.builder(LatLonPoint.create(-81.0, -150.0), LatLonPoint.create(-60.0, 200.0)).build());
    assertThat(bbox.getWidth()).isWithin(0.01).of(360.0);
    assertThat(bbox.crossDateline()).isFalse();

    // -------
    // ---------
    bbox = testExtend(LatLonRect.builder(LatLonPoint.create(-81.0, -180.0), LatLonPoint.create(-60.0, 135.0)),
        LatLonRect.builder(LatLonPoint.create(-81.0, 135.0), LatLonPoint.create(-60.0, 180.0)).build());
    assertThat(bbox.getWidth()).isWithin(0.01).of(360.0);
    assertThat(bbox.crossDateline()).isFalse();

    // ------
    // ------
    bbox = testExtend(LatLonRect.builder(LatLonPoint.create(-81.0, -180.0), LatLonPoint.create(-60.0, 0.0)),
        LatLonRect.builder(LatLonPoint.create(-81.0, 135.0), LatLonPoint.create(-60.0, 160.0)).build());
    assertThat(bbox.getWidth()).isWithin(0.01).of(225.0);
    assertThat(bbox.crossDateline()).isTrue();;

    // ---------
    // ------
    bbox = testExtend(LatLonRect.builder(LatLonPoint.create(-81.0, -180.0), LatLonPoint.create(-60.0, 0.0)),
        LatLonRect.builder(LatLonPoint.create(-81.0, 135.0), LatLonPoint.create(-60.0, 180.0)).build());
    assertThat(bbox.getWidth()).isWithin(0.01).of(225.0);
    assertThat(bbox.crossDateline()).isTrue();

    // ---------
    // ------
    bbox = testExtend(LatLonRect.builder(LatLonPoint.create(-81.0, 135.0), LatLonPoint.create(-60.0, 180.0)),
        LatLonRect.builder(LatLonPoint.create(-81.0, -180.0), LatLonPoint.create(-60.0, 0.0)).build());
    assertThat(bbox.getWidth()).isWithin(0.01).of(225.0);
    assertThat(bbox.crossDateline()).isTrue();
  }

  @Test
  public void testLatLonToProjRect() {
    ProjectionRect[] result = p.latLonToProjRect(0, 20, 40, 60);
    assertThat(result).asList().containsExactly(new ProjectionRect(20, 0, 60, 40), null);

    result = p.latLonToProjRect(0, 100, 40, 260);
    assertThat(result).asList().containsExactly(new ProjectionRect(100, 0, 180, 40),
        new ProjectionRect(-180, 0, -100, 40));

    result = p.latLonToProjRect(0, 100, 40, -100);
    assertThat(result).asList().containsExactly(new ProjectionRect(100, 0, 180, 40),
        new ProjectionRect(-180, 0, -100, 40));

    result = p.latLonToProjRect(0, 0, 40, 360);
    assertThat(result).asList().containsExactly(new ProjectionRect(0, 0, 180, 40), new ProjectionRect(-180, 0, 0, 40));

    result = p.latLonToProjRect(0, 0, 40, 1.e-9);
    assertThat(result).asList().containsExactly(new ProjectionRect(0, 0, 180, 40), new ProjectionRect(-180, 0, 0, 40));

    result = p.latLonToProjRect(0, -180, 40, -180);
    assertThat(result).asList().containsExactly(new ProjectionRect(-180, 0, 180, 40), null);

    LatLonProjection p2 = new LatLonProjection("center180", null, 180);
    ProjectionRect[] result2 = p2.latLonToProjRect(LatLonRect.fromSpec("0, -10, 99, 20"));
    assertThat(result2).asList().containsExactly(ProjectionRect.builder().setRect(350, 0, 10, 90).build(),
        ProjectionRect.builder().setRect(0, 0, 10, 90).build());

    result2 = p2.latLonToProjRect(LatLonRect.fromSpec("0, 111, 99, 200"));
    assertThat(result2).asList().containsExactly(ProjectionRect.builder().setRect(111, 0, 200, 90).build(), null);
  }

  @Test
  public void problem2() {
    ProjectionRect[] result = p.latLonToProjRect(0, -180, 40, -180);
    assertThat(result).asList().containsExactly(new ProjectionRect(-180, 0, 180, 40), null);
  }



}
