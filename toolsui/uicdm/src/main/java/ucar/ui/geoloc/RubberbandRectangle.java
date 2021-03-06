/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.geoloc;

import java.awt.*;

/**
 * Rectangle Rubberbanding.
 * 
 * @author David M. Geary
 * @author John Caron
 */
public class RubberbandRectangle extends Rubberband {
  public RubberbandRectangle(Component component, boolean listen) {
    super(component, listen);
  }

  public void drawLast(Graphics2D graphics) {
    Rectangle rect = lastBounds();
    graphics.drawRect(rect.x, rect.y, rect.width, rect.height);
  }

  public void drawNext(Graphics2D graphics) {
    Rectangle rect = getBounds();
    graphics.drawRect(rect.x, rect.y, rect.width, rect.height);
  }
}

