package com.example.gulp;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

public class SwallowChoiceScreen extends Screen {
   private static final int W = 320;
   private static final int H = 180;
   private static final int BOX_W = 140;
   private static final int BOX_H = 120;
   private static final int BOX_TOP = 30;
   private static final String GULP_TEXT = "Press V to swallow mobs and players. Your belly grows, and your stomach level and perks apply.";
   private static final String FLAMEVORE_TEXT = "Flamevore does the swallowing. Gulp's mob eating and belly are off, but items, levels, perks and storage stay.";
   private List<FormattedCharSequence> gulpLines = List.of();
   private List<FormattedCharSequence> flamevoreLines = List.of();

   public SwallowChoiceScreen() {
      super(Component.m_237113_("Swallowing system"));
   }

   private int left() {
      return (this.f_96543_ - 320) / 2;
   }

   private int top() {
      return (this.f_96544_ - 180) / 2;
   }

   private int boxX(int i) {
      return i == 0 ? this.left() + 15 : this.left() + 320 - 15 - 140;
   }

   private int boxY() {
      return this.top() + 30;
   }

   private boolean over(int i, double mx, double my) {
      return mx >= (double)this.boxX(i) && mx < (double)(this.boxX(i) + 140) && my >= (double)this.boxY() && my < (double)(this.boxY() + 120);
   }

   protected void m_7856_() {
      this.gulpLines = this.f_96547_.m_92923_(Component.m_237113_("Press V to swallow mobs and players. Your belly grows, and your stomach level and perks apply."), 124);
      this.flamevoreLines = this.f_96547_.m_92923_(Component.m_237113_("Flamevore does the swallowing. Gulp's mob eating and belly are off, but items, levels, perks and storage stay."), 124);
   }

   private void choose(SwallowSystem system) {
      this.f_96541_.m_91106_().m_120367_(SimpleSoundInstance.m_263171_(SoundEvents.f_12490_, 1.0F));
      Net.send(19, system.ordinal());
      this.m_7379_();
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (this.over(0, mouseX, mouseY)) {
            this.choose(SwallowSystem.GULP);
            return true;
         }

         if (this.over(1, mouseX, mouseY)) {
            this.choose(SwallowSystem.FLAMEVORE);
            return true;
         }
      }

      return super.m_6375_(mouseX, mouseY, button);
   }

   public boolean m_7043_() {
      return false;
   }

   private void drawBox(GuiGraphics g, int i, String label, List<FormattedCharSequence> lines, int mouseX, int mouseY) {
      int bx = this.boxX(i);
      int by = this.boxY();
      boolean hover = this.over(i, (double)mouseX, (double)mouseY);
      g.m_280509_(bx, by, bx + 140, by + 120, hover ? -12961222 : -14540254);
      int edge = hover ? -1 : -10066330;
      g.m_280509_(bx, by, bx + 140, by + 1, edge);
      g.m_280509_(bx, by + 120 - 1, bx + 140, by + 120, edge);
      g.m_280509_(bx, by, bx + 1, by + 120, edge);
      g.m_280509_(bx + 140 - 1, by, bx + 140, by + 120, edge);
      g.m_280168_().m_85836_();
      g.m_280168_().m_85841_(2.0F, 2.0F, 1.0F);
      g.m_280056_(this.f_96547_, label, (int)(((float)bx + 70.0F) / 2.0F - (float)this.f_96547_.m_92895_(label) / 2.0F), (by + 8) / 2, 16777215, true);
      g.m_280168_().m_85849_();

      for(int l = 0; l < lines.size(); ++l) {
         g.m_280649_(this.f_96547_, (FormattedCharSequence)lines.get(l), bx + 8, by + 34 + l * 9, hover ? 14540253 : 11184810, false);
      }

   }

   public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(g);
      int x = this.left();
      int y = this.top();
      g.m_280509_(x, y, x + 320, y + 180, -434891756);
      g.m_280509_(x, y, x + 320, y + 1, -10066330);
      g.m_280509_(x, y + 180 - 1, x + 320, y + 180, -10066330);
      g.m_280509_(x, y, x + 1, y + 180, -10066330);
      g.m_280509_(x + 320 - 1, y, x + 320, y + 180, -10066330);
      g.m_280137_(this.f_96547_, "Which swallowing system would you like to use?", x + 160, y + 12, 16777215);
      this.drawBox(g, 0, "Gulp", this.gulpLines, mouseX, mouseY);
      this.drawBox(g, 1, "Flamevore", this.flamevoreLines, mouseX, mouseY);
      g.m_280137_(this.f_96547_, "Can be changed later in Settings", x + 160, y + 180 - 20, 10066329);
      super.m_88315_(g, mouseX, mouseY, partialTick);
   }
}
