package com.example.gulp;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class SharedStorageScreen extends Screen {
   private static final int COLS = 9;
   private static final int SLOT = 18;
   private final String host;
   private List<ItemStack> items;
   private int refreshTimer;

   private SharedStorageScreen(String host, List<ItemStack> items) {
      super(Component.m_237113_(host + "'s Stomach"));
      this.host = host;
      this.items = items;
   }

   public static void show(String host, List<ItemStack> items) {
      Minecraft mc = Minecraft.m_91087_();
      Screen var4 = mc.f_91080_;
      if (var4 instanceof SharedStorageScreen open) {
         open.items = items;
      } else if (mc.f_91080_ == null) {
         mc.m_91152_(new SharedStorageScreen(host, items));
      }

   }

   public boolean m_7043_() {
      return false;
   }

   public void m_86600_() {
      if (ClientState.heldMode() == 0) {
         this.m_7379_();
      } else {
         if (++this.refreshTimer >= 40) {
            this.refreshTimer = 0;
            Net.send(7);
         }

      }
   }

   public boolean m_7933_(int key, int scan, int mods) {
      if (ClientModEvents.SCREEN_KEY.m_90832_(key, scan)) {
         this.m_7379_();
         return true;
      } else {
         return super.m_7933_(key, scan, mods);
      }
   }

   public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(g);
      int rows = Math.max(1, (this.items.size() + 9 - 1) / 9);
      int w = 176;
      int h = rows * 18 + 34;
      int x = (this.f_96543_ - w) / 2;
      int y = (this.f_96544_ - h) / 2;
      g.m_280509_(x - 1, y - 1, x + w + 1, y + h + 1, -16777216);
      g.m_280509_(x, y, x + w, y + h, -3750202);
      g.m_280056_(this.f_96547_, this.host + "'s Stomach (view only)", x + 7, y + 6, -12566464, false);
      ItemStack hovered = ItemStack.f_41583_;

      for(int i = 0; i < Math.max(this.items.size(), 1); ++i) {
         int sx = x + 7 + i % 9 * 18;
         int sy = y + 20 + i / 9 * 18;
         g.m_280509_(sx, sy, sx + 18, sy + 18, -7631989);
         g.m_280509_(sx + 1, sy + 1, sx + 18 - 1, sy + 18 - 1, -12632257);
         if (i < this.items.size()) {
            ItemStack stack = (ItemStack)this.items.get(i);
            if (!stack.m_41619_()) {
               g.m_280480_(stack, sx + 1, sy + 1);
               g.m_280370_(this.f_96547_, stack, sx + 1, sy + 1);
               if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) {
                  hovered = stack;
               }
            }
         }
      }

      super.m_88315_(g, mouseX, mouseY, partialTick);
      if (!hovered.m_41619_()) {
         g.m_280153_(this.f_96547_, hovered, mouseX, mouseY);
      }

   }
}
