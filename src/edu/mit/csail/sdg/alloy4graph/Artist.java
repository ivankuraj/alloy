/*
 * Alloy Analyzer
 * Copyright (c) 2007 Massachusetts Institute of Technology
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA,
 * 02110-1301, USA
 */

package edu.mit.csail.sdg.alloy4graph;

import static java.awt.BasicStroke.CAP_ROUND;
import static java.awt.BasicStroke.JOIN_ROUND;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import edu.mit.csail.sdg.alloy4.OurPDFWriter;

/**
 * This class abstracts the drawing operations so that we can (potentially)
 * draw the graph using different frameworks; eg. Java2D, asPNG, asPDF...
 *
 * <p><b>Thread Safety:</b> Can be called only by the AWT event thread.
 */

public final class Artist {

    /** The corresponding Graphics2D object. */
    private Graphics2D gr;

    /** The corresponding OurPDFWriter. */
    private OurPDFWriter pdf;

    /** Construct an artist that acts as a wrapper around the given Graphics2D object. */
    public Artist(Graphics2D graphics2D) { this.gr=graphics2D; this.pdf=null; }

    /** Construct an artist that acts as a wrapper around the given OurPDFWriter object. */
    public Artist(OurPDFWriter pdfWriter) { this.gr=null; this.pdf=pdfWriter; }

    /** Shifts the coordinate space by the given amount. */
    public void translate(int x, int y) {
        if (gr!=null) gr.translate(x,y);
        else pdf.write("1 0 0 1 ").write(x).writespace().write(y).write(" cm\n");
    }

    /** Draws a circle of the given radius, centered at (0,0) */
    public void drawCircle(int radius) {
        if (gr!=null) { gr.drawArc(-radius, -radius, radius*2, radius*2, 0, 360); return; }
        // Approximate a circle using 4 cubic bezier curves
        int r=radius;
        double k=(0.55238D*radius);
        pdf.write(r).write(" 0 m ")
           .write(r).writespace().write(k).writespace().write(k).writespace().write(r).write(" 0 ").write(r).write(" c ")
           .write(-k).writespace().write(r).writespace().write(-r).writespace().write(k).writespace().write(-r).write(" 0 c ")
           .write(-r).writespace().write(-k).writespace().write(-k).writespace().write(-r).write(" 0 ").write(-r).write(" c ")
           .write(k).writespace().write(-r).writespace().write(r).writespace().write(-k).writespace().write(r).write(" 0 c S\n");
    }

    /** Fills a circle of the given radius, centered at (0,0) */
    public void fillCircle(int radius) {
        if (gr!=null) { gr.fillArc(-radius, -radius, radius*2, radius*2, 0, 360); return; }
        // Approximate a circle using 4 cubic bezier curves
        int r=radius;
        double k=(0.55238D*radius);
        pdf.write(r).write(" 0 m ")
           .write(r).writespace().write(k).writespace().write(k).writespace().write(r).write(" 0 ").write(r).write(" c ")
           .write(-k).writespace().write(r).writespace().write(-r).writespace().write(k).writespace().write(-r).write(" 0 c ")
           .write(-r).writespace().write(-k).writespace().write(-k).writespace().write(-r).write(" 0 ").write(-r).write(" c ")
           .write(k).writespace().write(-r).writespace().write(r).writespace().write(-k).writespace().write(r).write(" 0 c f\n");
    }

    /** Draws a line from (x1,y1) to (x2,y2) */
    public void drawLine(int x1, int y1, int x2, int y2) {
        if (gr!=null) gr.drawLine(x1,y1,x2,y2);
        else pdf.write(x1).writespace().write(y1).write(" m ").write(x2).writespace().write(y2).write(" l S\n");
    }

    /** Changes the current color. */
    public void setColor(Color c) {
        if (gr!=null) { gr.setColor(c); return; }
        int rgb=c.getRGB(), r=(rgb>>16)&0xFF, g=(rgb>>8)&0xFF, b=(rgb&0xFF);
        pdf.write(r/255D).writespace().write(g/255D).writespace().write(b/255D).write(" RG\n");
        pdf.write(r/255D).writespace().write(g/255D).writespace().write(b/255D).write(" rg\n");
    }

    /** Draws the outline of the given shape. */
    public void draw(Shape shape, boolean fillOrNot) {
        if (gr!=null) { if (fillOrNot) gr.fill(shape); else gr.draw(shape); return; }
        if (shape instanceof Line2D.Double) {
            Line2D.Double obj=(Line2D.Double)shape;
            pdf.write(obj.x1).writespace().write(obj.y1).write(" m ")
               .write(obj.x2).writespace().write(obj.y2).write(fillOrNot?" l f\n":" l S\n");
        } else if (shape instanceof QuadCurve2D.Double) {
            // Convert the quadratic bezier curve into a cubic bezier curve
            QuadCurve2D.Double obj=(QuadCurve2D.Double)shape;
            double px = obj.x1 + (obj.ctrlx - obj.x1)*2D/3D, qx = px + (obj.x2 - obj.x1)/3D;
            double py = obj.y1 + (obj.ctrly - obj.y1)*2D/3D, qy = py + (obj.y2 - obj.y1)/3D;
            pdf.write(obj.x1).writespace().write(obj.y1).write(" m ")
               .write(px).writespace().write(py).writespace()
               .write(qx).writespace().write(qy).writespace()
               .write(obj.x2).writespace().write(obj.y2).write(fillOrNot?" c f\n":" c S\n");
        } else if (shape instanceof Polygon) {
            Polygon obj=(Polygon)shape;
            for(int i=0; i<obj.npoints; i++) pdf.write(obj.xpoints[i]).writespace().write(obj.ypoints[i]).write(i==0?" m\n":" l\n");
            pdf.write(fillOrNot ? "h f\n" : "h S\n");
        } else {
            double[] pt=new double[6];
            double moveX=0, moveY=0, nowX=0, nowY=0;
            for(PathIterator it=shape.getPathIterator(null); !it.isDone(); it.next()) {
               int type=it.currentSegment(pt);
               switch(type) {
                 case PathIterator.SEG_MOVETO:
                     nowX=moveX=pt[0]; nowY=moveY=pt[1];
                     pdf.write(nowX).writespace().write(nowY).write(" m\n");
                     break;
                 case PathIterator.SEG_CLOSE:  nowX=moveX; nowY=moveY; pdf.write("h\n"); break;
                 case PathIterator.SEG_LINETO:
                     nowX=pt[0]; nowY=pt[1];
                     pdf.write(nowX).writespace().write(nowY).write(" l\n");
                     break;
                 case PathIterator.SEG_QUADTO:
                     double px = nowX + (pt[0] - nowX)*2D/3D, qx = px + (pt[2] - nowX)/3D;
                     double py = nowY + (pt[1] - nowY)*2D/3D, qy = py + (pt[3] - nowY)/3D;
                     nowX=pt[2]; nowY=pt[3];
                     pdf.write(px).writespace().write(py).writespace()
                        .write(qx).writespace().write(qy).writespace()
                        .write(nowX).writespace().write(nowY).write(" c\n");
                     break;
                 case PathIterator.SEG_CUBICTO:
                     nowX=pt[4]; nowY=pt[5];
                     pdf.write(pt[0]).writespace().write(pt[1]).writespace()
                        .write(pt[2]).writespace().write(pt[3]).writespace()
                        .write(nowX).writespace().write(nowY).write(" c\n");
                     break;
               }
            }
            pdf.write(fillOrNot ? "f \n" : "S\n");
        }
    }

    /** The pattern for dotted line. */
    private static float[] dot = new float[]{1f,3f};

    /** The pattern for dashed line. */
    private static float[] dashed = new float[]{6f,3f};

    /**
     * Modifies the given Graphics2D object to use the line style representing by this object.
     * <p> NOTE: as a special guarantee, if gr2d==null, then this method returns immediately without doing anything.
     * <p> NOTE: just like the typical AWT and Swing methods, this method can be called only by the AWT event dispatching thread.
     */
    public void set(VizStyle style, double scale) {
        if (gr!=null) {
           BasicStroke bs;
           switch(style) {
              case BOLD:   bs=new BasicStroke(scale>1 ? (float)(2.5d/scale) : 2.5f); break;
              case DOTTED: bs=new BasicStroke(scale>1 ? (float)(1.0d/scale) : 1f, CAP_ROUND, JOIN_ROUND, 15f, dot, 0f); break;
              case DASHED: bs=new BasicStroke(scale>1 ? (float)(1.0d/scale) : 1f, CAP_ROUND, JOIN_ROUND, 15f, dashed, 5f); break;
              default:     bs=new BasicStroke(scale>1 ? (float)(1.0d/scale) : 1f);
           }
           gr.setStroke(bs);
           return;
        }
        switch(style) {
          case BOLD:   pdf.write("2 w [] 0 d\n"); return;
          case DOTTED: pdf.write("1 w [1 3] 0 d\n"); return;
          case DASHED: pdf.write("1 w [6 3] 0 d\n"); return;
          default:     pdf.write("1 w [] 0 d\n"); return;
        }
    }

    /** Saves the current font size. */
    private int fontSize = 12;

    /** Saves the current font boldness. */
    private boolean fontBoldness = false;

    /** Changes the current font. */
    public void setFont(int fontSize, boolean fontBoldness) {
        updateCache(fontSize, fontBoldness);
        if (gr!=null) gr.setFont(cachedFont); else { this.fontSize=fontSize; this.fontBoldness=fontBoldness; }
    }

    /** Draws the given string at (x,y) */
    public void drawString(String text, int x, int y) {
        if (text.length()==0) return;
        if (gr!=null) { gr.drawString(text,x,y); return; }
        updateCache(fontSize, fontBoldness);
        GlyphVector gv=cachedFont.createGlyphVector(new FontRenderContext(null,false,false), text);
        translate(x,y);
        draw(gv.getOutline(), true);
        translate(-x,-y);
    }

    /*
     * Here is a quick summary of various PDF Graphics operations
     * ==========================================================
     *
     * $x $y m                 --> begins a new path at the given coordinate
     * $x $y l                 --> add the segment (LASTx,LASTy)..($x,$y) to the current path
     * $cx $cy $x $y v         --> add the bezier curve (LASTx,LASTy)..(LASTx,LASTy)..($cx,$cy)..($x,$y) to the current path
     * $cx $cy $x $y y         --> add the bezier curve (LASTx,LASTy)....($cx,$cy).....($x,$y)...($x,$y) to the current path
     * $ax $ay $bx $by $x $y c --> add the bezier curve (LASTx,LASTy)....($ax,$ay)....($bx,$by)..($x,$y) to the current path
     * h                       --> close the current subpath by straightline segment from current point to the start of this subpath
     * $x $y $w $h re          --> append a rectangle to the current path as a complete subpath with lower-left corner at $x $y
     *
     * PATH S    --> draw that path
     * PATH f    --> fill that path
     * PATH B    --> fill then draw the path
     *
     * q                      --> saves the current graphics state
     * 1 J                    --> sets the round cap
     * 1 j                    --> sets the round joint
     * [] 0 d                 --> sets the dash pattern as SOLID
     * [4 6] 0 d              --> sets the dash pattern as 4 UNITS ON then 6 UNITS OFF
     * 5 w                    --> sets the line width as 5 UNITS
     * $a $b $c $d $e $f cm   --> appends the given matrix                [1 0 0 1 dx dy] is translate to dx dy
     * $R $G $B RG            --> sets the stroke color (where 0 <= $R <= 1, etc)
     * $R $G $B rg            --> sets the fill   color (where 0 <= $R <= 1, etc)
     * Q                      --> restores the current graphics state
     */

    /** If nonnull, it caches an empty bitmap. */
    private static BufferedImage cachedImage;

    /** If nonnull, it caches the Graphics2D object associated with the current font size and font boldness. */
    private static Graphics2D cachedGraphics;

    /** If nonnull, it caches the FontMetrics object associated with the current font size and font boldness. */
    private static FontMetrics cachedFontMetrics;

    /** If nonnull, it caches the Font object associated with the current font size and font boldness. */
    private static Font cachedFont;

    /** If cachedFont!=null, this is its font size. */
    private static int cachedFontSize;

    /** If cachedFont!=null, this is its font boldness. */
    private static boolean cachedFontBoldness;

    /** Updates cached{Image,Graphics,Metrics,Font} based on the given font size and font boldness, and return max ascent+descent. */
    private static int updateCache(int fontSize, boolean fontBoldness) {
       if (cachedFont==null || cachedFontMetrics==null || fontSize!=cachedFontSize || fontBoldness!=cachedFontBoldness) {
          if (cachedImage==null) cachedImage=new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
          if (cachedGraphics==null) cachedGraphics=(Graphics2D)(cachedImage.getGraphics());
          if (fontSize<8) fontSize=8;
          if (fontSize>72) fontSize=72;
          cachedFont = new Font("Lucida Grande",
                       ((cachedFontBoldness=fontBoldness) ? Font.BOLD : Font.PLAIN), cachedFontSize=fontSize);
          cachedGraphics.setFont(cachedFont);
          cachedFontMetrics=cachedGraphics.getFontMetrics();
       }
       return cachedFontMetrics.getMaxAscent()+cachedFontMetrics.getMaxDescent();
    }

    /** Returns the max ascent when drawing text using the given font size and font boldness settings. */
    public static int getMaxAscent(int fontSize, boolean fontBoldness) {
        updateCache(fontSize, fontBoldness);
        return cachedFontMetrics.getMaxAscent();
    }

    /** Returns the sum of the max ascent and max descent when drawing text using the given font size and font boldness settings. */
    public static int getMaxAscentAndDescent(int fontSize, boolean fontBoldness) {
        return updateCache(fontSize, fontBoldness);
    }

    /** Returns the bounding box when drawing the given string using the given font size and font boldness settings. */
    public static Rectangle2D getStringBounds(int fontSize, boolean fontBoldness, String string) {
        updateCache(fontSize, fontBoldness);
        return cachedFontMetrics.getStringBounds(string, cachedGraphics);
    }
}