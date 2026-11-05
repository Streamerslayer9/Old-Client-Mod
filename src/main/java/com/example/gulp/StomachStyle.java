package com.example.gulp;

import java.util.Random;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class StomachStyle {
   public static final float WALL_SAT = 0.55F;
   public static final float ACID_SAT = 0.85F;
   private static final int STAR_COUNT = 40;
   private static final int[] STAR_X = new int[40];
   private static final int[] STAR_Y = new int[40];
   private static final int DEFAULT_WALL = 1315860;
   private static final int DEFAULT_ACID = 14716976;

   private StomachStyle() {
   }

   public static int wallRgb() {
      return Mth.m_14169_(GulpConfig.wallHue, 0.55F, 0.1F + 0.4F * GulpConfig.wallShade);
   }

   public static int acidRgb() {
      return Mth.m_14169_(GulpConfig.acidHue, 0.85F, 0.45F + 0.55F * GulpConfig.acidShade);
   }

   private static int wallBase() {
      return GulpConfig.wallCustom ? wallRgb() : 1315860;
   }

   public static int hueRgb(float hue) {
      return Mth.m_14169_(hue, 0.85F, 1.0F);
   }

   public static int wallArgb() {
      return -436207616 | (GulpConfig.wallCustom ? wallRgb() : 1315860);
   }

   public static int acidArgb() {
      return -16777216 | (GulpConfig.acidCustom ? acidRgb() : 14716976);
   }

   public static void paintWall(GuiGraphics g, int x, int y, int w, int h) {
      paintWall(g, x, y, w, h, GulpConfig.stomachPattern);
   }

   public static void paintWall(GuiGraphics g, int x, int y, int w, int h, Pattern pattern) {
      g.m_280509_(x, y, x + w, y + h, wallArgb());
      drawPattern(g, x + 1, y + 1, w - 2, h - 2, pattern);
      int border = GulpConfig.wallCustom ? -16777216 | lighten(wallBase(), 0.45F) : -10066330;
      g.m_280509_(x, y, x + w, y + 1, border);
      g.m_280509_(x, y + h - 1, x + w, y + h, border);
      g.m_280509_(x, y, x + 1, y + h, border);
      g.m_280509_(x + w - 1, y, x + w, y + h, border);
   }

   public static void paintAcid(GuiGraphics g, int x, int y, int w, int h) {
      if (w > 0 && h > 4) {
         int rgb = acidArgb() & 16777215;
         int body = -2013265920 | rgb;
         int deep = 1426063360 | rgb >> 1 & 8355711;
         int shine = -872415232 | lighten(rgb, 0.5F);
         double t = (double)(System.currentTimeMillis() % 100000L) / (double)1000.0F;
         int bottom = y + h;

         for(int px = 0; px < w; px += 2) {
            int cw = Math.min(2, w - px);
            int backTop = y + 3 + (int)Math.round(Math.sin((double)px * 0.09 - t * 1.1) * (double)2.5F);
            g.m_280509_(x + px, backTop, x + px + cw, bottom, deep);
            int frontTop = y + 5 + (int)Math.round(Math.sin((double)px * 0.14 + t * 1.8) * (double)2.0F + Math.sin((double)px * 0.31 + t * 0.9));
            g.m_280509_(x + px, frontTop, x + px + cw, bottom, body);
            g.m_280509_(x + px, frontTop, x + px + cw, frontTop + 1, shine);
         }

         for(int i = 0; i < 8; ++i) {
            double cycle = (t * (double)0.25F + (double)i * 0.37) % (double)1.0F;
            int bx = x + 6 + (i * 53 + 17) % Math.max(1, w - 12);
            int by = bottom - 3 - (int)(cycle * (double)(h - 12));
            g.m_280509_(bx, by, bx + 2, by + 2, 1711276032 | lighten(rgb, 0.7F));
         }

      }
   }

   private static void drawPattern(GuiGraphics g, int x, int y, int w, int h, Pattern pattern) {
      if (pattern != StomachStyle.Pattern.NONE && w > 0 && h > 0) {
         int ink = 805306368 | lighten(wallBase(), 0.55F);
         int x2 = x + w;
         int y2 = y + h;
         switch (pattern) {
            case STARS:
               int bright = 1879048192 | lighten(wallBase(), 0.8F);

               for(int i = 0; i < 40; ++i) {
                  int sx = x + 3 + STAR_X[i] * (w - 6) / 1000;
                  int sy = y + 3 + STAR_Y[i] * (h - 6) / 1000;
                  g.m_280509_(sx, sy, sx + 1, sy + 1, bright);
                  if (i % 3 == 0) {
                     g.m_280509_(sx - 1, sy, sx + 2, sy + 1, ink);
                     g.m_280509_(sx, sy - 1, sx + 1, sy + 2, ink);
                  }
               }
               break;
            case WAVES:
               double phase = (double)(System.currentTimeMillis() % 6000L) / (double)6000.0F * Math.PI * (double)2.0F;
               int row = 0;

               for(int ry = y + 4; ry < y2 - 2; ry += 8) {
                  for(int px = x; px < x2; px += 2) {
                     int wy = ry + (int)Math.round(Math.sin((double)(px - x) * 0.18 + (double)row * 0.9 + phase) * (double)2.0F);
                     if (wy >= y && wy < y2) {
                        g.m_280509_(px, wy, Math.min(x2, px + 2), wy + 1, ink);
                     }
                  }

                  ++row;
               }
               break;
            case STRIPES:
               for(int px = x + 2; px < x2; px += 10) {
                  g.m_280509_(px, y, Math.min(x2, px + 3), y2, ink);
               }
               break;
            case DOTS:
               for(int py = y + 4; py < y2 - 1; py += 8) {
                  for(int px = x + 4; px < x2 - 1; px += 8) {
                     g.m_280509_(px, py, px + 2, py + 2, ink);
                  }
               }
               break;
            case CHECKER:
               int py = y;

               for(int r = 0; py < y2; ++r) {
                  for(int px = x + ((r & 1) == 0 ? 0 : 10); px < x2; px += 20) {
                     g.m_280509_(px, py, Math.min(x2, px + 10), Math.min(y2, py + 10), ink);
                  }

                  py += 10;
               }
         }

      }
   }

   public static int lighten(int rgb, float amount) {
      int r = rgb >> 16 & 255;
      int gr = rgb >> 8 & 255;
      int b = rgb & 255;
      r += (int)((float)(255 - r) * amount);
      gr += (int)((float)(255 - gr) * amount);
      b += (int)((float)(255 - b) * amount);
      return r << 16 | gr << 8 | b;
   }

   static {
      Random r = new Random(11L);

      for(int i = 0; i < 40; ++i) {
         STAR_X[i] = r.nextInt(1000);
         STAR_Y[i] = r.nextInt(1000);
      }

   }

   public static enum Pattern {
      NONE("None"),
      STARS("Stars"),
      WAVES("Wavy Stripes"),
      STRIPES("Stripes"),
      DOTS("Dots"),
      CHECKER("Checkerboard");

      public final String label;

      private Pattern(String label) {
         this.label = label;
      }

      public Pattern next() {
         Pattern[] v = values();
         return v[(this.ordinal() + 1) % v.length];
      }

      // $FF: synthetic method
      private static Pattern[] $values() {
         return new Pattern[]{NONE, STARS, WAVES, STRIPES, DOTS, CHECKER};
      }
   }
}
