package com.example.gulp;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class StyleScreen extends Screen {
   private static final int W = 250;
   private static final int H = 224;
   private static final float[] SWATCH_HUES = new float[]{0.0F, 0.06F, 0.12F, 0.33F, 0.5F, 0.6F, 0.75F, 0.9F};
   private final int[] swatchX;
   private Button patternButton;
   private Slider wallHue;
   private Slider wallShade;
   private Slider acidHue;
   private Slider acidShade;

   public StyleScreen() {
      super(Component.m_237113_("Stomach look"));
      this.swatchX = new int[SWATCH_HUES.length];
   }

   private int left() {
      return (this.f_96543_ - 250) / 2;
   }

   private int top() {
      return (this.f_96544_ - 224) / 2;
   }

   protected void m_7856_() {
      ScreenLoop.start();
      int x = this.left();
      int y = this.top();
      int sw = 150;
      int sx = x + 15;
      this.wallHue = new Slider(sx, y + 34, sw, "Hue", true, GulpConfig.wallHue, (v) -> {
         GulpConfig.wallHue = v;
         GulpConfig.wallCustom = true;
      });
      this.wallShade = new Slider(sx, y + 52, sw, "Shade", false, GulpConfig.wallShade, (v) -> {
         GulpConfig.wallShade = v;
         GulpConfig.wallCustom = true;
      });
      this.m_142416_(this.wallHue);
      this.m_142416_(this.wallShade);
      this.patternButton = Button.m_253074_(Component.m_237119_(), (btn) -> {
         GulpConfig.stomachPattern = GulpConfig.stomachPattern.next();
         GulpConfig.save();
         this.refresh();
      }).m_252987_(x + 15, y + 72, 220, 18).m_253136_();
      this.m_142416_(this.patternButton);
      this.acidHue = new Slider(sx, y + 114, sw, "Hue", true, GulpConfig.acidHue, (v) -> {
         GulpConfig.acidHue = v;
         GulpConfig.acidCustom = true;
      });
      this.acidShade = new Slider(sx, y + 132, sw, "Shade", false, GulpConfig.acidShade, (v) -> {
         GulpConfig.acidShade = v;
         GulpConfig.acidCustom = true;
      });
      this.m_142416_(this.acidHue);
      this.m_142416_(this.acidShade);
      int swatch = 14;
      int gap = 2;

      for(int i = 0; i < SWATCH_HUES.length; ++i) {
         float hue = SWATCH_HUES[i];
         this.swatchX[i] = x + 15 + i * (swatch + gap);
         this.m_142416_(Button.m_253074_(Component.m_237119_(), (btn) -> {
            this.wallHue.set(hue);
            GulpConfig.save();
         }).m_252987_(this.swatchX[i], y + 94, swatch, 8).m_253136_());
         this.m_142416_(Button.m_253074_(Component.m_237119_(), (btn) -> {
            this.acidHue.set(hue);
            GulpConfig.save();
         }).m_252987_(this.swatchX[i], y + 150, swatch, 8).m_253136_());
      }

      this.m_142416_(Button.m_253074_(Component.m_237113_("Reset"), (btn) -> {
         GulpConfig.resetStomachLook();
         GulpConfig.save();
         this.m_232761_();
      }).m_252987_(x + 15, y + 224 - 54, 60, 18).m_253136_());
      this.m_142416_(Button.m_253074_(Component.m_237113_("Back"), (btn) -> this.f_96541_.m_91152_(new SettingsScreen())).m_252987_(x + 8, y + 224 - 28, 60, 20).m_253136_());
      this.m_142416_(Button.m_253074_(Component.m_237113_("Close"), (btn) -> this.m_7379_()).m_252987_(x + 250 - 68, y + 224 - 28, 60, 20).m_253136_());
      this.refresh();
   }

   private void refresh() {
      this.patternButton.m_93666_(Component.m_237113_("Wall pattern: " + GulpConfig.stomachPattern.label));
   }

   public boolean m_7043_() {
      return false;
   }

   public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(g);
      int x = this.left();
      int y = this.top();
      g.m_280509_(x, y, x + 250, y + 224, -434891756);
      g.m_280509_(x, y, x + 250, y + 1, -10066330);
      g.m_280509_(x, y + 224 - 1, x + 250, y + 224, -10066330);
      g.m_280509_(x, y, x + 1, y + 224, -10066330);
      g.m_280509_(x + 250 - 1, y, x + 250, y + 224, -10066330);
      g.m_280056_(this.f_96547_, "Stomach look", x + 8, y + 8, 16777215, true);
      g.m_280056_(this.f_96547_, "Stomach wall", x + 15, y + 22, -3355444, false);
      g.m_280056_(this.f_96547_, "Digestive acid", x + 15, y + 102 + 0, -3355444, false);
      int px = x + 15 + 150 + 12;
      int pw = 63;
      StomachStyle.paintWall(g, px, y + 30, pw, 120);
      StomachStyle.paintAcid(g, px + 1, y + 90, pw - 2, 59);
      g.m_280137_(this.f_96547_, "Wall", px + pw / 2, y + 50, -1);
      g.m_280137_(this.f_96547_, "Acid", px + pw / 2, y + 120, -1);
      super.m_88315_(g, mouseX, mouseY, partialTick);

      for(int i = 0; i < SWATCH_HUES.length; ++i) {
         int c = -16777216 | StomachStyle.hueRgb(SWATCH_HUES[i]);
         g.m_280509_(this.swatchX[i] + 1, y + 95, this.swatchX[i] + 13, y + 101, c);
         g.m_280509_(this.swatchX[i] + 1, y + 151, this.swatchX[i] + 13, y + 157, c);
      }

   }

   private static final class Slider extends AbstractSliderButton {
      private final String name;
      private final boolean rainbow;
      private final Consumer<Float> setter;

      Slider(int x, int y, int w, String name, boolean rainbow, float start, Consumer<Float> setter) {
         super(x, y, w, 14, Component.m_237119_(), (double)start);
         this.name = name;
         this.rainbow = rainbow;
         this.setter = setter;
         this.m_5695_();
      }

      void set(float v) {
         this.f_93577_ = (double)Math.max(0.0F, Math.min(1.0F, v));
         this.m_5697_();
         this.m_5695_();
      }

      protected void m_5695_() {
         String var10001 = this.name;
         this.m_93666_(Component.m_237113_(var10001 + ": " + Math.round(this.f_93577_ * (double)100.0F) + "%"));
      }

      protected void m_5697_() {
         this.setter.accept((float)this.f_93577_);
      }

      public void m_7691_(double mouseX, double mouseY) {
         super.m_7691_(mouseX, mouseY);
         GulpConfig.save();
      }

      public void m_87963_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
         int x = this.m_252754_();
         int y = this.m_252907_();
         int w = this.m_5711_();
         int h = this.m_93694_();
         g.m_280509_(x - 1, y - 1, x + w + 1, y + h + 1, -16777216);
         if (this.rainbow) {
            for(int i = 0; i < w; ++i) {
               g.m_280509_(x + i, y, x + i + 1, y + h, -16777216 | StomachStyle.hueRgb((float)i / (float)w));
            }
         } else {
            g.m_280509_(x, y, x + w, y + h, -14013910);
            g.m_280509_(x, y, x + (int)((double)w * this.f_93577_), y + h, -9408400);
         }

         int hx = x + (int)((double)(w - 4) * this.f_93577_);
         g.m_280509_(hx - 1, y - 2, hx + 5, y + h + 2, -16777216);
         g.m_280509_(hx, y - 1, hx + 4, y + h + 1, this.m_198029_() ? -1 : -3355444);
         Font f = Minecraft.m_91087_().f_91062_;
         g.m_280653_(f, this.m_6035_(), x + w / 2, y + 3, 16777215);
      }
   }
}
