package com.example.gulp;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class PerkTreeScreen extends Screen {
   private static final int W = 320;
   private static final int H = 272;
   private static final int GROWTH = -10758496;
   private static final int LOOT = -10967297;
   private static final int SURVIVAL = -38037;
   private static final int GOLD = -11443;
   private static final int DIM = -13290175;
   private static final int NODE_R = 13;
   private static final int RING_R = 17;
   private static final int SEGMENTS = 48;
   private static final int RX = 104;
   private static final int RY = 60;
   private static final int ORBIT_RX = 138;
   private static final int ORBIT_RY = 84;
   private static final int CENTER_Y = 106;
   private static final int DETAIL_Y = 192;
   private static final int DETAIL_H = 52;
   private final List<Perk> order = new ArrayList();
   private final int[] starX = new int[45];
   private final int[] starY = new int[45];

   public PerkTreeScreen() {
      super(Component.m_237113_("Stomach Perks"));
      this.order.add(Perk.ROOMY);
      this.order.add(Perk.GOURMET);
      this.order.add(Perk.QUICK);
      this.order.add(Perk.RICH);
      this.order.add(Perk.STRIP);
      this.order.add(Perk.UNIVERSAL);
      this.order.add(Perk.HEARTY);
      this.order.add(Perk.IRON);
      this.order.add(Perk.HEALING);
      this.order.add(Perk.MENDING);
      this.order.add(Perk.SMELTING);

      for(Perk p : Perk.values()) {
         if (!this.order.contains(p)) {
            this.order.add(p);
         }
      }

      Random random = new Random(7L);

      for(int i = 0; i < this.starX.length; ++i) {
         this.starX[i] = 4 + random.nextInt(312);
         this.starY[i] = 24 + random.nextInt(220);
      }

   }

   private int left() {
      return (this.f_96543_ - 320) / 2;
   }

   private int top() {
      return (this.f_96544_ - 272) / 2;
   }

   private int centerX() {
      return this.left() + 160;
   }

   private int centerY() {
      return this.top() + 106;
   }

   private double angle(int i) {
      return (-Math.PI / 2D) + (Math.PI * 2D) * (double)i / (double)this.order.size();
   }

   private int nodeX(int i) {
      return this.centerX() + (int)Math.round(Math.cos(this.angle(i)) * (double)104.0F);
   }

   private int nodeY(int i) {
      return this.centerY() + (int)Math.round(Math.sin(this.angle(i)) * (double)60.0F);
   }

   private static int colorOf(Perk perk) {
      int var10000;
      switch (perk) {
         case HEALING:
         case IRON:
         case HEARTY:
         case UNIVERSAL:
         case MENDING:
            var10000 = -38037;
            break;
         case RICH:
         case STRIP:
         case SMELTING:
            var10000 = -10967297;
            break;
         default:
            var10000 = -10758496;
      }

      return var10000;
   }

   private static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
      for(int dy = -r; dy <= r; ++dy) {
         int dx = (int)Math.round(Math.sqrt((double)r * (double)r - (double)dy * (double)dy));
         g.m_280509_(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
      }

   }

   private static void ring(GuiGraphics g, int cx, int cy, int radius, int segments, int lit, int litColor, int dimColor) {
      for(int i = 0; i < segments; ++i) {
         double a = (-Math.PI / 2D) + (Math.PI * 2D) * (double)i / (double)segments;
         int px = cx + (int)Math.round(Math.cos(a) * (double)radius);
         int py = cy + (int)Math.round(Math.sin(a) * (double)radius);
         g.m_280509_(px - 1, py - 1, px + 1, py + 1, i < lit ? litColor : dimColor);
      }

   }

   private static int brighten(int color) {
      int r = color >> 16 & 255;
      int gr = color >> 8 & 255;
      int b = color & 255;
      r += (255 - r) * 35 / 100;
      gr += (255 - gr) * 35 / 100;
      b += (255 - b) * 35 / 100;
      return -16777216 | r << 16 | gr << 8 | b;
   }

   private static String effect(Perk perk, int rank) {
      String var10000;
      switch (perk) {
         case HEALING -> var10000 = rank + "% max HP / 5s";
         case IRON -> var10000 = "-" + rank * 5 + "% struggle damage";
         case HEARTY -> var10000 = String.format("+%.1f HP per digest", (double)rank * (double)0.5F);
         case UNIVERSAL -> var10000 = String.format("+%.1f Hunger Bars per %d items", StomachItems.hungerBars(rank), 32);
         case MENDING -> var10000 = rank == 0 ? "No repair" : "+" + rank * 2 + " durability / 5s per item";
         case RICH -> var10000 = "Looting " + (rank % 2 == 0 ? String.valueOf(rank / 2) : rank / 2 + " or " + (rank / 2 + 1));
         case STRIP -> var10000 = rank == 0 ? "Locked" : (rank >= 3 ? "No levels lost" : "-" + (3 - rank) + " level(s) per enchant");
         case SMELTING -> var10000 = rank == 0 ? "Locked" : StomachItems.smeltPerPass(rank) + " ore / 4s, x" + StomachItems.smeltYield(rank) + " ingots";
         case ROOMY -> var10000 = "+" + rank * 6 + "% capacity";
         case QUICK -> var10000 = String.format("-%.2fs, -%.2fs up", (double)rank * 0.075, (double)rank * 0.15);
         case GOURMET -> var10000 = "+" + rank * 5 + "% digest XP";
         default -> var10000 = "";
      }

      return var10000;
   }

   protected void m_7856_() {
      ScreenLoop.start();
      int x = this.left();
      int y = this.top();
      this.m_142416_(Button.m_253074_(Component.m_237113_("Legacy"), (btn) -> {
         GulpConfig.legacyPerkMenu = true;
         GulpConfig.save();
         this.f_96541_.m_91152_(new PerksScreen());
      }).m_252987_(x + 92, y + 2, 64, 20).m_253136_());
      this.m_142416_(Button.m_253074_(Component.m_237113_("Back"), (btn) -> this.f_96541_.m_91152_(new StomachScreen())).m_252987_(x + 8, y + 272 - 24, 56, 20).m_253136_());
      this.m_142416_(Button.m_253074_(Component.m_237113_("Close"), (btn) -> this.m_7379_()).m_252987_(x + 320 - 64, y + 272 - 24, 56, 20).m_253136_());
   }

   private int hoveredNode(double mouseX, double mouseY) {
      for(int i = 0; i < this.order.size(); ++i) {
         double dx = mouseX - (double)this.nodeX(i);
         double dy = mouseY - (double)this.nodeY(i);
         if (dx * dx + dy * dy <= (double)361.0F) {
            return i;
         }
      }

      return -1;
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      if (button == 0) {
         int i = this.hoveredNode(mouseX, mouseY);
         if (i >= 0) {
            int count = m_96637_() ? 100 : (m_96638_() ? 5 : 1);
            Net.send(6, ((Perk)this.order.get(i)).ordinal() | count << 8);
            return true;
         }
      }

      return super.m_6375_(mouseX, mouseY, button);
   }

   public boolean m_7043_() {
      return false;
   }

   public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(g);
      int x = this.left();
      int y = this.top();
      int cx = this.centerX();
      int cy = this.centerY();
      int n = this.order.size();
      int hovered = this.hoveredNode((double)mouseX, (double)mouseY);
      g.m_280509_(x, y, x + 320, y + 272, -434891752);
      g.m_280509_(x, y, x + 320, y + 1, -10066330);
      g.m_280509_(x, y + 272 - 1, x + 320, y + 272, -10066330);
      g.m_280509_(x, y, x + 1, y + 272, -10066330);
      g.m_280509_(x + 320 - 1, y, x + 320, y + 272, -10066330);

      for(int i = 0; i < this.starX.length; ++i) {
         g.m_280509_(x + this.starX[i], y + this.starY[i], x + this.starX[i] + 1, y + this.starY[i] + 1, 1090519039);
      }

      for(int k = 0; k < 120; ++k) {
         double a = (-Math.PI / 2D) + (Math.PI * 2D) * (double)k / (double)120.0F;
         int px = cx + (int)Math.round(Math.cos(a) * (double)138.0F);
         int py = cy + (int)Math.round(Math.sin(a) * (double)84.0F);
         int sector = (int)Math.floor((a + (Math.PI / 2D)) / (Math.PI * 2D) * (double)n + (double)0.5F) % n;
         g.m_280509_(px, py, px + 2, py + 2, colorOf((Perk)this.order.get(sector)) & 16777215 | 1879048192);
      }

      for(int k = 0; k < 60; ++k) {
         double a = (Math.PI * 2D) * (double)k / (double)60.0F;
         int px = cx + (int)Math.round(Math.cos(a) * (double)40.0F);
         int py = cy + (int)Math.round(Math.sin(a) * (double)28.0F);
         g.m_280509_(px, py, px + 1, py + 1, -13158590);
      }

      int points = ClientState.points();
      disc(g, cx, cy, 24, -14803416);
      ring(g, cx, cy, 25, 64, points > 0 ? 64 : 0, -11443, -13882312);
      PoseStack pose = g.m_280168_();
      pose.m_85836_();
      pose.m_252880_((float)cx, (float)(cy - 12), 0.0F);
      pose.m_85841_(2.0F, 2.0F, 1.0F);
      g.m_280137_(this.f_96547_, String.valueOf(points), 0, 0, points > 0 ? -11443 : -7697772);
      pose.m_85849_();
      g.m_280137_(this.f_96547_, points == 1 ? "point" : "points", cx, cy + 8, -5592406);

      for(int i = 0; i < n; ++i) {
         Perk perk = (Perk)this.order.get(i);
         int nx = this.nodeX(i);
         int ny = this.nodeY(i);
         int rank = ClientState.rank(perk);
         int max = perk.maxRank;
         boolean maxed = rank >= max;
         boolean canBuy = points > 0 && !maxed;
         int color = maxed ? -11443 : colorOf(perk);
         boolean hot = i == hovered;
         disc(g, nx, ny, 13, hot ? -13816518 : -15000796);
         int lit = rank == 0 ? 0 : Math.max(1, Math.round((float)(48 * rank) / (float)max));
         ring(g, nx, ny, 17, 48, lit, hot ? brighten(color) : color, -13290175);
         g.m_280480_(PerksScreen.iconFor(perk), nx - 8, ny - 8);
         String label = rank + "/" + max;
         int tw = this.f_96547_.m_92895_(label);
         g.m_280509_(nx - tw / 2 - 2, ny + 17 - 4, nx + tw / 2 + 3, ny + 17 + 5, -15658728);
         g.m_280056_(this.f_96547_, label, nx - tw / 2, ny + 17 - 3, maxed ? -11443 : (rank > 0 ? -1 : -7829368), false);
         if (canBuy) {
            int bx = nx + 13 - 2;
            int by = ny - 13 - 4;
            g.m_280509_(bx, by, bx + 8, by + 8, -13730510);
            g.m_280509_(bx + 2, by + 4, bx + 6, by + 5, -1);
            g.m_280509_(bx + 3, by + 2, bx + 4, by + 7, -1);
         }
      }

      g.m_280056_(this.f_96547_, "Stomach Perks", x + 8, y + 8, -1, true);
      String hint = "Click +1   Shift +5   Ctrl max";
      g.m_280056_(this.f_96547_, hint, x + 320 - 8 - this.f_96547_.m_92895_(hint), y + 8, -8882046, false);
      String[] names = new String[]{"Growth", "Loot", "Survival"};
      int[] colors = new int[]{-10758496, -10967297, -38037};

      for(int i = 0; i < names.length; ++i) {
         g.m_280509_(x + 8, y + 27 + i * 10, x + 13, y + 32 + i * 10, colors[i]);
         g.m_280056_(this.f_96547_, names[i], x + 16, y + 25 + i * 10, -5592406, false);
      }

      g.m_280509_(x + 8, y + 192, x + 320 - 8, y + 192 + 52, -16119282);
      if (hovered >= 0) {
         Perk perk = (Perk)this.order.get(hovered);
         int rank = ClientState.rank(perk);
         g.m_280056_(this.f_96547_, perk.title + "  " + rank + "/" + perk.maxRank, x + 12, y + 192 + 3, rank >= perk.maxRank ? -11443 : colorOf(perk), false);
         List<FormattedCharSequence> desc = this.f_96547_.m_92923_(Component.m_237113_(perk.description), 296);

         for(int l = 0; l < Math.min(2, desc.size()); ++l) {
            g.m_280649_(this.f_96547_, (FormattedCharSequence)desc.get(l), x + 12, y + 192 + 13 + l * 9, -6250336, false);
         }

         g.m_280056_(this.f_96547_, this.f_96547_.m_92834_("Current: " + effect(perk, rank), 296), x + 12, y + 192 + 32, -1, false);
         if (ClientState.level() < perk.minLevel) {
            g.m_280056_(this.f_96547_, "Unlocks at stomach level " + perk.minLevel, x + 12, y + 192 + 41, -2056112, false);
         } else if (rank < perk.maxRank) {
            g.m_280056_(this.f_96547_, this.f_96547_.m_92834_("Next: " + effect(perk, rank + 1), 296), x + 12, y + 192 + 41, -6430552, false);
         } else {
            g.m_280056_(this.f_96547_, "Max rank", x + 12, y + 192 + 41, -11443, false);
         }
      } else {
         g.m_280137_(this.f_96547_, "Hover a perk to see what it does.", x + 160, y + 192 + 22, -8882046);
      }

      super.m_88315_(g, mouseX, mouseY, partialTick);
   }
}
