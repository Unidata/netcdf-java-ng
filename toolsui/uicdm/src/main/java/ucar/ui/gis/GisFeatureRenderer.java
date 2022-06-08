/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.gis;

import ucar.ui.util.Renderer;
import ucar.unidata.geoloc.*;
import ucar.ui.prefs.Debug;
import java.awt.Color;
import java.awt.Shape;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Superclass for rendering collections of GisFeatures.
 *
 * @author John Caron
 */
public abstract class GisFeatureRenderer implements Renderer {
  private Color color = Color.blue; // default color of polylines
  protected Projection displayProject; // the current display Projection
  protected ArrayList<Shape> shapeList;

  ////// this is what the subclasses have to implement (besides the constructor)
  /**
   * Preferred map area on opening for first time.
   * 
   * @return lat/lon bounding box that specifies preferred area.
   */
  public abstract LatLonRect getPreferredArea();

  protected abstract java.util.List<GisFeature> getFeatures(); // collection of AbstractGisFeature

  // what projection is the data in? set to null if no Projection (no conversion)
  // assumes data projection doesnt change
  protected abstract Projection getDataProjection();

  ///////////
  public java.awt.Color getColor() {
    return color;
  }

  public void setColor(Color color) {
    this.color = color;
  }

  public void setProjection(Projection project) {
    displayProject = project;
    shapeList = null;
  }

  /**
   * Draws all the features that are within the graphics clip rectangle,
   * using the previously set displayProjection.
   *
   * @param g the Graphics2D context on which to draw
   * @param pixelAT transforms "Normalized Device" to Device coordinates
   */
  public void draw(java.awt.Graphics2D g, AffineTransform pixelAT) {
    g.setColor(color);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    g.setStroke(new java.awt.BasicStroke(0.0f));

    Rectangle2D clipRect = (Rectangle2D) g.getClip();
    for (Shape s : getShapes(g, pixelAT)) {
      Rectangle2D shapeBounds = s.getBounds2D();
      if (shapeBounds.intersects(clipRect))
        g.draw(s);
    }
  }

  // get the set of shapes to draw, convert projections if need be
  protected Iterable<Shape> getShapes(java.awt.Graphics2D g, AffineTransform normal2device) {
    if (shapeList != null)
      return shapeList;

    if (Debug.isSet("projection/LatLonShift"))
      System.out.println("projection/LatLonShift GisFeatureRenderer.getShapes called");

    Projection dataProject = getDataProjection();

    // a list of GisFeatureAdapter-s
    List<GisFeature> featList = getFeatures();
    shapeList = new ArrayList<>(featList.size());

    for (Object o : featList) {
      AbstractGisFeature feature = (AbstractGisFeature) o;
      Shape shape;
      if (dataProject == null) {
        shape = feature.getShape();
      } else if (dataProject.isLatLon()) {
        // always got to run it through if its lat/lon
        shape = feature.getProjectedShape(displayProject);
      } else if (dataProject == displayProject) {
        shape = feature.getShape();
      } else {
        shape = feature.getProjectedShape(dataProject, displayProject);
      }

      shapeList.add(shape);
    }

    return shapeList;
  }

}
