package com.example.gulp;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

public class VillagerPerksScreen extends Screen {
   private static final int W = 320;
   private int H = 262;
   private static final int FEED_LEVEL = 5;
   private static final int ROW_H = 30;
   private static final int ROW_STEP = 32;
   private static final int ROWS_Y = 68;
   private final Screen parent;
   private final int entityId;
   private final String villagerName;
   private final Button[] buyButtons = new Button[VillagerPerk.values().length];
   private Button storageButton;
   private int scroll;
   private int contentH;
   private boolean dragging;
   private Button climbButton;
   private Button convertButton;
   private Button backButton;
   private Button closeButton;
   private static final int TEXT_W = 264;
   private static final int LINE = 9;

   public VillagerPerksScreen(Screen parent, int entityId, String villagerName) {
      super(Component.m_237113_("Villager perks"));
      this.parent = parent;
      this.entityId = entityId;
      this.villagerName = villagerName;
   }

   private int left() {
      return (this.f_96543_ - 320) / 2;
   }

   private int top() {
      return (this.f_96544_ - this.H) / 2;
   }

   protected void m_7856_() {
      Net.send(20, this.entityId);
      int x = this.left();
      int y = this.top();
      this.backButton = (Button)this.m_142416_(Button.m_253074_(Component.m_237113_("Back"), (btn) -> this.f_96541_.m_91152_(this.parent)).m_252987_(x + 8, y + this.H - 28, 60, 20).m_253136_());
      this.closeButton = (Button)this.m_142416_(Button.m_253074_(Component.m_237113_("Close"), (btn) -> this.f_96541_.m_91152_((Screen)null)).m_252987_(x + 320 - 68, y + this.H - 28, 60, 20).m_253136_());
      this.storageButton = (Button)this.m_142416_(Button.m_253074_(Component.m_237113_("Storage"), (btn) -> Net.send(22, this.entityId)).m_252987_(x + 160 - 40, y + this.H - 28, 80, 20).m_253136_());
      this.storageButton.f_93624_ = false;
      this.climbButton = (Button)this.m_142416_(Button.m_253074_(Component.m_237113_("Climb In"), (btn) -> Net.send(24, this.entityId)).m_252987_(x + 8, y + this.H - 52, 148, 20).m_257505_(Tooltip.m_257550_(Component.m_237113_("Force yourself into their tummy, whatever kind of villager they are. How they take it depends on their type and temper."))).m_253136_());
      this.climbButton.f_93624_ = false;
      this.convertButton = (Button)this.m_142416_(Button.m_253074_(Component.m_237113_("Ask to be a Pred"), (btn) -> Net.send(25, this.entityId)).m_252987_(x + 320 - 156, y + this.H - 52, 148, 20).m_257505_(Tooltip.m_257550_(Component.m_237113_("Ask them to become a pred villager for good. They need to like you enough, and their old perks are forgotten (points refunded)."))).m_253136_());
      this.convertButton.f_93624_ = false;

      for(VillagerPerk perk : VillagerPerk.values()) {
         int i = perk.ordinal();
         this.buyButtons[i] = (Button)this.m_142416_(Button.m_253074_(Component.m_237113_("+"), (btn) -> Net.send(21, this.entityId << 4 | i)).m_252987_(x + 320 - 30, y + 68 + i * 32 + 5, 20, 20).m_253136_());
         this.buyButtons[i].f_93624_ = false;
      }

   }

   public boolean m_7043_() {
      return false;
   }

   public void m_7379_() {
      this.f_96541_.m_91152_(this.parent);
   }

   private static String num(float v) {
      return String.format(Locale.ROOT, "%.2f", v);
   }

   private List<FormattedCharSequence> wrap(String text, int maxLines) {
      List<FormattedCharSequence> all = this.f_96547_.m_92923_(FormattedText.m_130775_(text), 264);
      if (all.size() <= maxLines) {
         return all;
      } else {
         List<FormattedCharSequence> out = this.f_96547_.m_92923_(FormattedText.m_130775_(text), 264);
         int width = 264 * maxLines - this.f_96547_.m_92895_("...") - 6;
         String var10000 = this.f_96547_.m_92834_(text, Math.max(10, width));
         String cut = var10000 + "...";
         out = this.f_96547_.m_92923_(FormattedText.m_130775_(cut), 264);
         return out.size() > maxLines ? out.subList(0, maxLines) : out;
      }
   }

   private int rowHeight(String now, String next) {
      return 13 + (this.wrap(now, 2).size() + this.wrap(next, 2).size()) * 9 + 2;
   }

   private void perkRow(GuiGraphics g, int x, int y, int h, String name, String now, String next, boolean unlocked) {
      g.m_280509_(x + 8, y, x + 320 - 8, y + h, 1090519039);
      g.m_280056_(this.f_96547_, this.f_96547_.m_92834_(name, 264), x + 14, y + 4, unlocked ? 16777215 : 8947848, true);
      int ty = y + 4 + 9;

      for(FormattedCharSequence l : this.wrap(now, 2)) {
         g.m_280649_(this.f_96547_, l, x + 14, ty, unlocked ? 12303291 : 7829367, false);
         ty += 9;
      }

      for(FormattedCharSequence l : this.wrap(next, 2)) {
         g.m_280649_(this.f_96547_, l, x + 14, ty, 8947848, false);
         ty += 9;
      }

   }

   private void perkRow(GuiGraphics g, int x, int y, String name, String now, String next, boolean unlocked) {
      this.perkRow(g, x, y, this.rowHeight(now, next), name, now, next, unlocked);
   }

   private int viewTop() {
      return this.top() + 68;
   }

   private int viewBot() {
      return this.top() + this.H - 58;
   }

   private void scrollTo(double mouseY) {
      int viewH = Math.max(20, this.viewBot() - this.viewTop());
      int maxScroll = Math.max(0, this.contentH - viewH);
      int thumbH = Math.max(12, viewH * viewH / Math.max(1, this.contentH));
      double f = (mouseY - (double)this.viewTop() - (double)thumbH / (double)2.0F) / (double)Math.max(1, viewH - thumbH);
      this.scroll = (int)Math.max((double)0.0F, Math.min((double)maxScroll, f * (double)maxScroll));
   }

   public boolean m_6050_(double mx, double my, double delta) {
      this.scroll = Math.max(0, this.scroll - (int)(delta * (double)14.0F));
      return true;
   }

   public boolean m_6375_(double mx, double my, int button) {
      int tx = this.left() + 320 - 8;
      if (button == 0 && mx >= (double)tx && mx <= (double)(tx + 8) && my >= (double)this.viewTop() && my <= (double)this.viewBot()) {
         this.dragging = true;
         this.scrollTo(my);
         return true;
      } else {
         return super.m_6375_(mx, my, button);
      }
   }

   public boolean m_7979_(double mx, double my, int button, double dx, double dy) {
      if (this.dragging) {
         this.scrollTo(my);
         return true;
      } else {
         return super.m_7979_(mx, my, button, dx, dy);
      }
   }

   public boolean m_6348_(double mx, double my, int button) {
      this.dragging = false;
      return super.m_6348_(mx, my, button);
   }

   public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(g);
      int x = this.left();
      int y = this.top();
      g.m_280509_(x, y, x + 320, y + this.H, -434891756);
      g.m_280509_(x, y, x + 320, y + 1, -10066330);
      g.m_280509_(x, y + this.H - 1, x + 320, y + this.H, -10066330);
      g.m_280509_(x, y, x + 1, y + this.H, -10066330);
      g.m_280509_(x + 320 - 1, y, x + 320, y + this.H, -10066330);
      g.m_280056_(this.f_96547_, this.f_96547_.m_92834_(this.villagerName + " - Perks", 304), x + 8, y + 8, 16777215, true);
      VillagerInfoPacket info = ClientVillagerInfo.get(this.entityId);

      for(Button b : this.buyButtons) {
         if (b != null) {
            b.f_93624_ = false;
         }
      }

      boolean adultNow = info != null && info.adult;
      if (this.climbButton != null && this.convertButton != null) {
         this.climbButton.f_93624_ = adultNow;
         this.convertButton.f_93624_ = adultNow && info.type != 2;
         if (this.convertButton.f_93624_) {
            this.climbButton.m_252865_(x + 8);
         } else {
            this.climbButton.m_252865_(x + 160 - 74);
         }

         this.convertButton.m_252865_(x + 320 - 156);
         this.climbButton.m_253211_(y + this.H - 52);
         this.convertButton.m_253211_(y + this.H - 52);
      }

      if (this.storageButton != null) {
         this.storageButton.f_93624_ = info != null && info.type == 2;
         if (info != null) {
            Button var10000 = this.storageButton;
            int var10001 = VillagerStorage.rowsFor(info.level);
            var10000.m_93666_(Component.m_237113_("Storage (" + var10001 * 9 + ")"));
         }
      }

      if (info == null) {
         this.H = 120;
         g.m_280056_(this.f_96547_, "Loading...", x + 8, y + 28, 11184810, false);
      } else {
         int level = info.level;
         boolean max = level >= 10;
         g.m_280056_(this.f_96547_, "Level " + level + (max ? " (max)" : ""), x + 8, y + 24, 15124554, false);
         VillagerTrait trait = VillagerTrait.byId(info.trait);
         String kindWord = info.type == 2 ? "pred" : (info.type == 1 ? "prey" : "villager");
         String note = trait != VillagerTrait.NONE ? trait.label + " " + kindWord : "Normal villager";
         g.m_280056_(this.f_96547_, note, x + 320 - 8 - this.f_96547_.m_92895_(note), y + 24, 10066329, false);
         int bx = x + 8;
         int by = y + 36;
         int bw = 304;
         g.m_280509_(bx - 1, by - 1, bx + bw + 1, by + 7, -16777216);
         g.m_280509_(bx, by, bx + bw, by + 6, -14540254);
         float frac = max ? 1.0F : Math.min(1.0F, (float)info.xp / (float)Math.max(1, info.xpNeeded));
         g.m_280509_(bx, by, bx + (int)((float)bw * frac), by + 6, -1652662);
         if (!max) {
            g.m_280056_(this.f_96547_, info.xp + " / " + info.xpNeeded + " XP to the next level", bx, by + 10, 10066329, false);
         }

         boolean pred = true;
         if (pred) {
            int var52 = info.points;
            String pts = var52 + (info.points == 1 ? " perk point" : " perk points");
            g.m_280056_(this.f_96547_, pts, bx + bw - this.f_96547_.m_92895_(pts), by + 10, info.points > 0 ? 7855479 : 10066329, false);
         }

         int used = 0;

         for(int r : info.ranks) {
            if (r > 0) {
               ++used;
            }
         }

         int slots = VillagerPerk.slotsFor(level);
         int roomy = VillagerPerk.ROOMY_BELLY.ordinal() < info.ranks.length ? info.ranks[VillagerPerk.ROOMY_BELLY.ordinal()] : 0;
         if (pred) {
            String line = "Perk slots " + used + "/" + slots + "    Holds up to " + VillagerPerk.capacityFor(level, roomy) + " at once";
            g.m_280056_(this.f_96547_, line, bx, y + 57, used >= slots ? 15124554 : 10066329, false);
         }

         int row = y + 68;
         int viewTop = y + 68;
         int viewBot = y + this.H - 58;
         int viewH = Math.max(20, viewBot - viewTop);
         int off = -this.scroll;
         g.m_280588_(x + 8, viewTop, x + 320 - 8, viewBot);

         for(VillagerPerk perk : VillagerPerk.perksFor(info.type)) {
            int i = perk.ordinal();
            int rank = i < info.ranks.length ? info.ranks[i] : 0;
            String name = perk.title + (perk.active ? " (active)" : "") + "  " + rank + "/" + perk.maxRank;
            String now;
            String next;
            switch (perk) {
               case SOOTHING:
                  float var55 = VillagerHolding.healPerSecond(level, trait, rank);
                  now = "Heals you " + num(var55) + " HP every second.";
                  next = rank >= perk.maxRank ? "Fully learned." : "Next rank: " + num(VillagerHolding.healPerSecond(level, trait, rank + 1)) + " HP per second.";
                  break;
               case EMBRACE:
                  int var54 = VillagerHolding.maxTicks(level, trait, rank);
                  now = "Holds you for up to " + var54 / 20 + " seconds.";
                  next = rank >= perk.maxRank ? "Fully learned." : "Next rank: " + VillagerHolding.maxTicks(level, trait, rank + 1) / 20 + " seconds.";
                  break;
               case ROOMY_BELLY:
                  int var53 = VillagerPerk.capacityFor(level, rank);
                  now = "Holds up to " + var53 + " players at once.";
                  next = rank >= perk.maxRank ? "Fully learned." : "Next rank: " + VillagerPerk.capacityFor(level, rank + 1) + " at once.";
                  break;
               case EAGER_RIDER:
                  now = rank > 0 ? "Asks " + rank * 35 + "% more often and needs " + rank * 10 + "% fewer hearts." : perk.description;
                  next = rank >= perk.maxRank ? "Fully learned." : "Next rank: " + (rank + 1) * 35 + "% more often, " + (rank + 1) * 10 + "% fewer hearts.";
                  break;
               case SNUG_PASSENGER:
                  now = rank > 0 ? "+" + 3 * rank + " hearts with you after every safe ride." : perk.description;
                  next = rank >= perk.maxRank ? "Fully learned." : "Next rank: +" + 3 * (rank + 1) + " hearts per ride.";
                  break;
               case PARTING_GIFT:
                  now = rank > 0 ? "Up to " + VillagerPerk.partingGiftXp(rank, 6000) + " XP after a ride of 30+ s (every 10 min)." : perk.description;
                  next = rank >= perk.maxRank ? "Fully learned." : "Next rank: up to " + VillagerPerk.partingGiftXp(rank + 1, 6000) + " XP.";
                  break;
               case HAGGLER:
                  now = rank > 0 ? rank * 5 + "% off when you trade with them." : perk.description;
                  next = rank >= perk.maxRank ? "Fully learned." : "Next rank: " + (rank + 1) * 5 + "% off.";
                  break;
               case CHATTY:
                  now = rank > 0 ? "Earns " + (2 + rank) + " XP per talk instead of 2." : perk.description;
                  next = rank >= perk.maxRank ? "Fully learned." : "Next rank: " + (3 + rank) + " XP per talk.";
                  break;
               case WARM_WELCOME:
                  now = rank > 0 ? "+" + rank + " heart" + (rank > 1 ? "s" : "") + " with you every time you talk." : perk.description;
                  next = rank >= perk.maxRank ? "Fully learned." : "Next rank: +" + (rank + 1) + " hearts per talk.";
                  break;
               case SECOND_WIND:
                  now = rank > 0 ? "A rest that ends well gives you Regeneration for " + VillagerPerk.secondWindTicks(rank) / 20 + " s." : perk.description;
                  next = rank >= perk.maxRank ? "Fully learned." : "Next rank: " + VillagerPerk.secondWindTicks(rank + 1) / 20 + " seconds of Regeneration.";
                  break;
               default:
                  now = rank > 0 ? "Feeds you " + rank + " hunger every 2 seconds." : perk.description;
                  next = rank >= perk.maxRank ? "Fully learned." : "Next rank: " + (rank + 1) + " hunger every 2 seconds.";
            }

            if (pred && rank == 0 && used >= slots) {
               next = "No free perk slot (a new one at a higher level).";
            }

            int rh = this.rowHeight(now, next);
            this.perkRow(g, x, row + off, rh, name, now, next, pred && rank > 0);
            int by2 = row + off + (rh - 20) / 2;
            this.buyButtons[i].m_253211_(by2);
            boolean inView = by2 >= viewTop && by2 + 20 <= viewBot;
            this.buyButtons[i].f_93624_ = inView && pred && rank < perk.maxRank;
            this.buyButtons[i].f_93623_ = info.points > 0 && (rank > 0 || used < slots);
            off += rh + 2;
         }

         g.m_280618_();
         this.contentH = off + this.scroll;
         int maxScroll = Math.max(0, this.contentH - viewH);
         this.scroll = Math.max(0, Math.min(this.scroll, maxScroll));
         if (maxScroll > 0) {
            int tx = x + 320 - 6;
            int thumbH = Math.max(12, viewH * viewH / this.contentH);
            int thumbY = viewTop + (int)((float)(viewH - thumbH) * ((float)this.scroll / (float)maxScroll));
            g.m_280509_(tx, viewTop, tx + 4, viewBot, -14540254);
            g.m_280509_(tx, thumbY, tx + 4, thumbY + thumbH, this.dragging ? -1652662 : -6710887);
         }

         int wanted = 68 + this.contentH + 36 + 24;
         this.H = Math.max(120, Math.min(wanted, this.f_96544_ - 8));
      }

      if (this.backButton != null) {
         this.backButton.m_253211_(y + this.H - 28);
      }

      if (this.closeButton != null) {
         this.closeButton.m_253211_(y + this.H - 28);
      }

      if (this.storageButton != null) {
         this.storageButton.m_253211_(y + this.H - 28);
      }

      super.m_88315_(g, mouseX, mouseY, partialTick);
   }
}
