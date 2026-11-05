package com.example.gulp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class GulpHud {
   private GulpHud() {
   }

   private static void right(GuiGraphics g, Minecraft mc, String text, int rightEdge, int y, int color) {
      g.m_280488_(mc.f_91062_, text, rightEdge - mc.f_91062_.m_92895_(text), y, color);
   }

   private static void renderHeldOverlay(GuiGraphics g, Minecraft mc, int screenWidth) {
      int mode = ClientState.heldMode();
      if (mode != 0) {
         String key = ClientModEvents.STRUGGLE_KEY.m_90863_().getString();
         int cx = screenWidth / 2;
         int y = 24;
         if (mode == 1) {
            g.m_280137_(mc.f_91062_, "Swallowed by " + ClientState.heldHolder() + " [SOFT]", cx, y, -171);
            g.m_280137_(mc.f_91062_, "[" + key + "] rub the walls   [" + ClientModEvents.RELEASE_KEY.m_90863_().getString() + "] leave   [" + ClientModEvents.SCREEN_KEY.m_90863_().getString() + "] view storage", cx, y + 11, -2236963);
         } else if (mode == 3) {
            g.m_280137_(mc.f_91062_, "Swallowed by " + ClientState.heldHolder() + " [ANGRY]", cx, y, -30635);
            g.m_280137_(mc.f_91062_, String.format("[%s] struggle (%.0f%% to break free)   They won't let you just leave", key, ClientState.heldChance()), cx, y + 11, -2236963);
            if (ClientState.heldProgress() > 0.0F) {
               g.m_280137_(mc.f_91062_, "Break free in time, or they will digest you!", cx, y + 22, -43691);
            }

         } else {
            g.m_280137_(mc.f_91062_, "Swallowed by " + ClientState.heldHolder() + " [HARD]", cx, y, -43691);
            int w = 120;
            int x = cx - w / 2;
            g.m_280509_(x - 1, y + 11, x + w + 1, y + 21, -16777216);
            g.m_280509_(x, y + 12, x + w, y + 20, -13421773);
            g.m_280509_(x, y + 12, x + (int)((float)w * Math.min(1.0F, ClientState.heldProgress())), y + 20, -4173778);
            g.m_280137_(mc.f_91062_, String.format("Digested %.0f%%   [%s] struggle (%.0f%% to break free)", ClientState.heldProgress() * 100.0F, key, ClientState.heldChance()), cx, y + 24, -2236963);
            g.m_280137_(mc.f_91062_, "[" + ClientModEvents.SCREEN_KEY.m_90863_().getString() + "] view their storage", cx, y + 35, -5592406);
         }
      }
   }

   public static void render(GuiGraphics g, int screenWidth, int screenHeight) {
      Minecraft mc = Minecraft.m_91087_();
      if (!mc.f_91066_.f_92062_ && mc.f_91074_ != null) {
         renderHeldOverlay(g, mc, screenWidth);
         if (ClientState.role().canEat()) {
            int w = 100;
            int rightEdge = screenWidth - 8;
            int x = rightEdge - w;
            int y = screenHeight - 34;
            if (ClientState.points() > 0) {
               right(g, mc, "Perk points: " + ClientState.points() + " [" + ClientModEvents.SCREEN_KEY.m_90863_().getString() + "]", rightEdge, y - 24, 16770634);
            }

            if (!ClientState.usesGulpSwallow()) {
               right(g, mc, "Stomach Lv." + ClientState.level(), rightEdge, y - 12, 16777215);
               int xp = (int)((float)w * ((float)ClientState.xp() / (float)Math.max(1, ClientState.xpNeeded())));
               g.m_280509_(x - 1, y - 1, x + w + 1, y + 4, -16777216);
               g.m_280509_(x, y, x + w, y + 3, -14540254);
               g.m_280509_(x, y, x + xp, y + 3, -1652662);
            } else {
               int organ = ClientState.activeOrgan();
               String var10002 = Organ.byId(organ).label;
               right(g, mc, var10002 + " Lv." + ClientState.level() + (ClientState.isHard() ? "  [HARD]" : "  [SOFT]"), rightEdge, y - 12, 16777215);
               g.m_280509_(x - 1, y - 1, x + w + 1, y + 9, -16777216);
               g.m_280509_(x, y, x + w, y + 8, -13421773);
               int fill = (int)((float)w * Math.min(1.0F, ClientState.usedIn(organ) / ClientState.cap()));
               g.m_280509_(x, y, x + fill, y + 8, ClientState.isHard() ? -4173778 : -10635171);
               right(g, mc, Stomach.fmtVolume((double)ClientState.usedIn(organ)) + " / " + Stomach.fmtVolume((double)ClientState.cap()) + "  (" + ClientState.entries().size() + " held)", rightEdge, y + 11, 14540253);
               int xpFill = (int)((float)w * ((float)ClientState.xp() / (float)Math.max(1, ClientState.xpNeeded())));
               g.m_280509_(x, y + 22, x + w, y + 25, -14540254);
               g.m_280509_(x, y + 22, x + xpFill, y + 25, -1652662);
            }
         }
      }
   }
}
