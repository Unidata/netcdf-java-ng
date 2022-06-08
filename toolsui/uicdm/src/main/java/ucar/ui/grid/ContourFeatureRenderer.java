/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.grid;

import javax.annotation.Nullable;
import ucar.ui.gis.GisFeatureRenderer;
import ucar.ui.widget.FontUtil;
import ucar.unidata.geoloc.*;
import ucar.unidata.util.Format;
import ucar.ui.prefs.Debug;
import java.awt.RenderingHints;
import java.util.*;
import java.awt.Color;
import java.awt.Shape;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Contour rendering.
 *
 * @author caron
 */
public class ContourFeatureRenderer extends GisFeatureRenderer {

  private Projection dataProjection;
  private List<ContourFeature> contourList; // list of ContourFeatures
  private boolean ShowLabels;

  /**
   * cstr
   */
  public ContourFeatureRenderer(ContourGrid conGrid, Projection dataProjection) {
    this.dataProjection = dataProjection;
    ShowLabels = true;
    contourList = conGrid.getContourLines();
  }

  /**
   * set switch whether contours labels are desired.
   * default true is set in cstr.
   */
  public void setShowLabels(boolean showlabels) {
    ShowLabels = showlabels;
  }

  @Nullable
  public LatLonRect getPreferredArea() {
    return null;
  }

  protected List getFeatures() {
    // collection of ContourFeature-s
    return contourList;
  }

  protected Projection getDataProjection() {
    return dataProjection;
  }


  /**
   * Overrides the GisFeatureRenderer draw() method, to draw contours
   * and with contour labels.
   *
   * @param g the Graphics2D context on which to draw
   * @param deviceFromNormalAT transforms "Normalized Device" to Device coordinates
   */
  public void draw(java.awt.Graphics2D g, AffineTransform deviceFromNormalAT) {
    /*
     * OLD WAY
     * // make & set desired font for contour label.
     * // contour label size in "points" is last arg
     * Font font1 = new Font("Helvetica", Font.PLAIN, 25);
     * // make a transform to un-flip the font
     * AffineTransform unflip = AffineTransform.getScaleInstance(1, -1);
     * Font font = font1.deriveFont(unflip);
     * g.setFont(font);
     */

    /* from original GisFeatureRenderer method draw: */
    g.setColor(Color.black);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    g.setStroke(new java.awt.BasicStroke(0.0f));

    Rectangle2D clipRect = (Rectangle2D) g.getClip();

    // draw the contours
    for (Shape s : getShapes(g, deviceFromNormalAT)) {
      Rectangle2D shapeBounds = s.getBounds2D();
      if (shapeBounds.intersects(clipRect))
        g.draw(s);
    }

    // additional code beyond GisFeatureRenderer method draw():
    // render contour value for this contour line. */
    if (ShowLabels) {
      Font f = FontUtil.getStandardFont(10).getFont();
      Font saveFont = g.getFont();

      // use world coordinates for position, but draw in "normal" coordinates
      // so that the symbols stay the same size
      AffineTransform deviceFromWorldAT = g.getTransform();
      AffineTransform normalFromWorldAT;
      // transform World to Normal coords:
      // normalFromWorldAT = deviceFromNormalAT-1 * deviceFromWorldAT
      try {
        normalFromWorldAT = deviceFromNormalAT.createInverse();
        normalFromWorldAT.concatenate(deviceFromWorldAT);
      } catch (java.awt.geom.NoninvertibleTransformException e) {
        System.out.println(" ContourFeatureRenderer: NoninvertibleTransformException on " + deviceFromNormalAT);
        return;
      }
      g.setTransform(deviceFromNormalAT); // so g now wants "normal coords"
      g.setFont(f);

      Iterator CViter = contourList.iterator();
      Point2D worldPt = new Point2D.Double();
      Point2D normalPt = new Point2D.Double();
      float[] coords = new float[6];
      for (Shape s : getShapes(g, deviceFromNormalAT)) {
        double contValue = ((ContourFeature) CViter.next()).getContourValue();

        // get position xpos,ypos on this contour where to put label
        // in current world coordinates in the current Shape s.
        PathIterator piter = s.getPathIterator(null);
        // int cs, count=-1; original
        int cs, count = 12;
        while (!piter.isDone()) {
          count++;
          if (count % 25 == 0) { // for every 25th position on this path
            cs = piter.currentSegment(coords);

            if (cs == PathIterator.SEG_MOVETO || cs == PathIterator.SEG_LINETO) {
              worldPt.setLocation(coords[0], coords[1]);
              normalFromWorldAT.transform(worldPt, normalPt); // convert to normal
              // render the contour value to the screen
              g.drawString(Format.d(contValue, 4), (int) normalPt.getX(), (int) normalPt.getY());
            }
          }
          piter.next();
        } // while not done
      } // end while shape.hasNext()

      // restore original transform and font
      g.setTransform(deviceFromWorldAT);
      g.setFont(saveFont);

    } // end if ShowLabels == true


    if (Debug.isSet("contour/doLabels")) {
      // get iterator to the class member ArrayList of GisFeature-s
      for (Object aContourList : contourList) {
        // ContourFeature cf = iter.next();
        System.out
            .println(" ContourFeatureRenderer: contour value = " + ((ContourFeature) aContourList).getContourValue());
      }
    }
  }
}
