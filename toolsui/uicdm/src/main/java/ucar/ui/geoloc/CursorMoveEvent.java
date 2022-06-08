/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.geoloc;

import ucar.unidata.geoloc.ProjectionPoint;
import java.awt.geom.Point2D;

/**
 * Cursor has moved to a new location.
 * 
 * @author John Caron
 */
public class CursorMoveEvent extends java.util.EventObject {
  private final ProjectionPoint world;

  public CursorMoveEvent(Object source, ProjectionPoint world) {
    super(source);
    this.world = world;
  }

  public CursorMoveEvent(Object source, Point2D location) {
    super(source);
    this.world = ProjectionPoint.create(location.getX(), location.getY());
  }

  public Point2D getLocationPoint() {
    return new Point2D.Double(world.getX(), world.getY());
  }

  public ProjectionPoint getLocation() {
    return world;
  }
}

