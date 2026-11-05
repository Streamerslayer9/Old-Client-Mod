package com.example.gulp;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PerksScreen extends Screen {
   private static final int W = 250;
   private static final int ROW_H = 30;
   private static final int HEADER = 24;
   private static final int FOOTER = 40;
   private static final int TRACK_W = 6;
   private static final int DESC_W = 164;
   private String lastSig = "";
   private final List<Button> buyButtons = new ArrayList();
   private int scroll;
   private int[] rowY = new int[0];
   private int[] rowH = new int[0];
   private List<List<FormattedCharSequence>> descLines = new ArrayList();
   private int contentH;
   private boolean draggingBar;

   public PerksScreen() {
      super(Component.m_237113_("Stomach Perks"));
   }

   private int left() {
      return (this.f_96543_ - 250) / 2;
   }

   private int panelH() {
      return Math.min(24 + this.contentH + 40, Math.max(154, this.f_96544_ - 16));
   }

   private int top() {
      return (this.f_96544_ - this.panelH()) / 2;
   }

   private int viewH() {
      return this.panelH() - 24 - 40;
   }

   private int maxScroll() {
      return Math.max(0, this.contentH - this.viewH());
   }

   private void clampScroll() {
      this.scroll = Math.max(0, Math.min(this.maxScroll(), this.scroll));
   }

   private void layoutRows() {
      Perk[] perks = Perk.values();
      this.rowY = new int[perks.length];
      this.rowH = new int[perks.length];
      this.descLines = new ArrayList();
      int yy = 0;

      for(int i = 0; i < perks.length; ++i) {
         List<FormattedCharSequence> lines = this.f_96547_.m_92923_(Component.m_237113_(perks[i].description), 164);
         this.descLines.add(lines);
         this.rowY[i] = yy;
         this.rowH[i] = Math.max(30, 12 + lines.size() * 9 + 4);
         yy += this.rowH[i];
      }

      this.contentH = yy;
   }

   static ItemStack iconFor(Perk perk) {
      ItemStack var10000;
      switch (perk) {
         case ROOMY -> var10000 = new ItemStack(Items.f_42768_);
         case HEALING -> var10000 = new ItemStack(Items.f_42436_);
         case RICH -> var10000 = new ItemStack(Items.f_42616_);
         case QUICK -> var10000 = new ItemStack(Items.f_42545_);
         case IRON -> var10000 = new ItemStack(Items.f_42469_);
         case GOURMET -> var10000 = new ItemStack(Items.f_42580_);
         case HEARTY -> var10000 = new ItemStack(Items.f_42677_);
         case UNIVERSAL -> var10000 = new ItemStack(Items.f_42726_);
         case STRIP -> var10000 = new ItemStack(Items.f_42690_);
         case MENDING -> var10000 = new ItemStack(Items.f_42146_);
         case SMELTING -> var10000 = new ItemStack(Items.f_42770_);
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   protected void m_7856_() {
      ScreenLoop.start();
      this.lastSig = ClientState.signature();
      this.layoutRows();
      int x = this.left();
      int y = this.top();
      this.clampScroll();
      this.buyButtons.clear();
      Perk[] perks = Perk.values();

      for(int i = 0; i < perks.length; ++i) {
         Perk perk = perks[i];
         int ry = y + 24 + this.rowY[i] - this.scroll;
         Button buy = Button.m_253074_(Component.m_237113_("+"), (btn) -> {
            int count = m_96637_() ? 100 : (m_96638_() ? 5 : 1);
            Net.send(6, i | count << 8);
         }).m_252987_(x + 250 - 40, ry + (this.rowH[i] - 20) / 2, 26, 20).m_253136_();
         buy.f_93623_ = ClientState.points() > 0 && ClientState.rank(perk) < perk.maxRank;
         this.buyButtons.add(buy);
         this.m_142416_(buy);
      }

      this.m_142416_(Button.m_253074_(Component.m_237113_("New look"), (btn) -> {
         GulpConfig.legacyPerkMenu = false;
         GulpConfig.save();
         this.f_96541_.m_91152_(new PerkTreeScreen());
      }).m_252987_(x + 92, y + 2, 64, 20).m_253136_());
      this.m_142416_(Button.m_253074_(Component.m_237113_("Back"), (btn) -> this.f_96541_.m_91152_(new StomachScreen())).m_252987_(x + 8, y + this.panelH() - 28, 60, 20).m_253136_());
      this.m_142416_(Button.m_253074_(Component.m_237113_("Close"), (btn) -> this.m_7379_()).m_252987_(x + 250 - 68, y + this.panelH() - 28, 60, 20).m_253136_());
   }

   public void m_86600_() {
      if (!this.lastSig.equals(ClientState.signature())) {
         this.m_169413_();
         this.m_7856_();
      }

   }

   private int trackX() {
      return this.left() + 250 - 6 - 3;
   }

   private int trackTop() {
      return this.top() + 24;
   }

   private int thumbH() {
      return Math.max(16, (int)((long)this.viewH() * (long)this.viewH() / (long)Math.max(1, this.contentH)));
   }

   private int thumbY() {
      int travel = this.viewH() - this.thumbH();
      return this.trackTop() + (this.maxScroll() == 0 ? 0 : (int)((long)travel * (long)this.scroll / (long)this.maxScroll()));
   }

   private void dragTo(double mouseY) {
      int travel = this.viewH() - this.thumbH();
      if (travel > 0) {
         double t = (mouseY - (double)this.trackTop() - (double)this.thumbH() / (double)2.0F) / (double)travel;
         this.scroll = (int)Math.round(t * (double)this.maxScroll());
         this.clampScroll();
      }
   }

   public boolean m_6050_(double mouseX, double mouseY, double delta) {
      if (this.maxScroll() > 0) {
         this.scroll -= (int)Math.round(delta * (double)15.0F);
         this.clampScroll();
         return true;
      } else {
         return super.m_6050_(mouseX, mouseY, delta);
      }
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      if (button == 0 && this.maxScroll() > 0 && mouseX >= (double)this.trackX() && mouseX <= (double)(this.trackX() + 6) && mouseY >= (double)this.trackTop() && mouseY <= (double)(this.trackTop() + this.viewH())) {
         this.draggingBar = true;
         this.dragTo(mouseY);
         return true;
      } else {
         return super.m_6375_(mouseX, mouseY, button);
      }
   }

   public boolean m_7979_(double mouseX, double mouseY, int button, double dx, double dy) {
      if (this.draggingBar) {
         this.dragTo(mouseY);
         return true;
      } else {
         return super.m_7979_(mouseX, mouseY, button, dx, dy);
      }
   }

   public boolean m_6348_(double mouseX, double mouseY, int button) {
      this.draggingBar = false;
      return super.m_6348_(mouseX, mouseY, button);
   }

   public boolean m_7043_() {
      return false;
   }

   public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(g);
      this.clampScroll();
      int x = this.left();
      int y = this.top();
      int h = this.panelH();
      g.m_280509_(x, y, x + 250, y + h, -434891756);
      g.m_280509_(x, y, x + 250, y + 1, -10066330);
      g.m_280509_(x, y + h - 1, x + 250, y + h, -10066330);
      g.m_280509_(x, y, x + 1, y + h, -10066330);
      g.m_280509_(x + 250 - 1, y, x + 250, y + h, -10066330);
      g.m_280056_(this.f_96547_, "Stomach Perks", x + 8, y + 8, 16777215, true);
      int var10000 = ClientState.points();
      String pts = "Perk points: " + var10000;
      g.m_280056_(this.f_96547_, pts, x + 250 - 8 - this.f_96547_.m_92895_(pts), y + 8, ClientState.points() > 0 ? 16770634 : 10066329, true);
      int viewTop = y + 24;
      int viewBottom = viewTop + this.viewH();
      g.m_280588_(x + 1, viewTop, x + 250 - 1, viewBottom);
      Perk[] perks = Perk.values();

      for(int i = 0; i < perks.length; ++i) {
         Perk perk = perks[i];
         int ry = viewTop + this.rowY[i] - this.scroll;
         if (ry + this.rowH[i] >= viewTop && ry <= viewBottom) {
            g.m_280509_(x + 6, ry + 1, x + 250 - 12, ry + this.rowH[i] - 2, 1090519039);
            g.m_280480_(iconFor(perk), x + 12, ry + (this.rowH[i] - 16) / 2);
            g.m_280056_(this.f_96547_, perk.title, x + 36, ry + 2, 16777215, true);
            var10000 = ClientState.rank(perk);
            String rank = var10000 + "/" + perk.maxRank;
            g.m_280056_(this.f_96547_, rank, x + 250 - 50 - this.f_96547_.m_92895_(rank), ry + 2, ClientState.rank(perk) >= perk.maxRank ? 5635925 : 12303291, true);
            List<FormattedCharSequence> lines = (List)this.descLines.get(i);

            for(int l = 0; l < lines.size(); ++l) {
               g.m_280649_(this.f_96547_, (FormattedCharSequence)lines.get(l), x + 36, ry + 12 + l * 9, 10066329, false);
            }
         }
      }

      g.m_280618_();

      for(int i = 0; i < this.buyButtons.size(); ++i) {
         Button b = (Button)this.buyButtons.get(i);
         int by = viewTop + this.rowY[i] - this.scroll + (this.rowH[i] - b.m_93694_()) / 2;
         b.m_253211_(by);
         b.f_93624_ = by >= viewTop && by + b.m_93694_() <= viewBottom;
      }

      if (this.maxScroll() > 0) {
         int tx = this.trackX();
         g.m_280509_(tx, viewTop, tx + 6, viewBottom, -16777216);
         g.m_280509_(tx + 1, viewTop + 1, tx + 6 - 1, viewBottom - 1, -14277082);
         int ty = this.thumbY();
         boolean hot = this.draggingBar || mouseX >= tx && mouseX <= tx + 6 && mouseY >= ty && mouseY <= ty + this.thumbH();
         g.m_280509_(tx + 1, ty, tx + 6 - 1, ty + this.thumbH(), hot ? -2236963 : -6645094);
      }

      String hint = "Click +1   Shift +5   Ctrl max";
      String withScroll = hint + "   Scroll for more";
      if (this.maxScroll() > 0 && this.f_96547_.m_92895_(withScroll) <= 234) {
         hint = withScroll;
      }

      g.m_280056_(this.f_96547_, hint, x + (250 - this.f_96547_.m_92895_(hint)) / 2, y + h - 38, 7829367, false);
      super.m_88315_(g, mouseX, mouseY, partialTick);
   }
}
