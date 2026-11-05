package com.example.gulp;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
   modid = "gulp",
   value = {Dist.CLIENT}
)
public final class ClientScreenEvents {
   private static final int BAR_W = 90;
   private static final int PANEL_OFFSET_Y = 24;
   private static final int BOX = 24;
   private static final int BLUE = -12945208;
   private static int rightHoldSlot = -1;
   private static int rightHoldTicks;

   private ClientScreenEvents() {
   }

   @SubscribeEvent
   public static void onInventoryKey(ScreenEvent.KeyPressed.Pre e) {
      Screen var2 = e.getScreen();
      if (var2 instanceof InventoryScreen screen) {
         if (ClientModEvents.INVENTORY_SWALLOW_KEY.isActiveAndMatches(InputConstants.m_84827_(e.getKeyCode(), e.getScanCode()))) {
            Slot slot = screen.getSlotUnderMouse();
            if (slot != null && slot.m_6657_() && slot.f_40218_ instanceof Inventory && slot.getSlotIndex() < 36) {
               Net.send(9, slot.f_40219_ | (Screen.m_96637_() ? 65536 : 0));
               e.setCanceled(true);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onScreenInit(ScreenEvent.Init.Post e) {
      Screen var2 = e.getScreen();
      if (var2 instanceof ContainerScreen screen) {
         if ("Stomach Storage".equals(screen.m_96636_().getString())) {
            int width = 52;
            int x = screen.getGuiLeft() - width - 4;
            if (x < 2) {
               x = screen.getGuiLeft() + screen.getXSize() + 4;
            }

            Button back = Button.m_253074_(Component.m_237113_("Back"), (btn) -> {
               Minecraft mc = Minecraft.m_91087_();
               if (mc.f_91074_ != null) {
                  mc.f_91074_.m_6915_();
                  mc.m_91152_(new StomachScreen());
               }
            }).m_252987_(x, screen.getGuiTop(), width, 20).m_253136_();
            e.addListener(back);
            int rank = ClientState.rank(Perk.STRIP);
            int lost = Math.max(0, 3 - rank);
            int barY = screen.getGuiTop() + 24 + 24 + 4;
            Button strip = new SmallTextButton(x, barY, width, 14, Component.m_237113_("Strip"), (btn) -> Net.send(12));
            Button transfer = new SmallTextButton(x, barY + 16, width, 14, Component.m_237113_("Transfer"), (btn) -> Net.send(13));
            SmallTextButton[] ref = new SmallTextButton[1];
            SmallTextButton destroy = new SmallTextButton(x, barY + 32, width, 14, Component.m_237113_("Destroy"), (btn) -> {
               if (ref[0].confirmed()) {
                  Net.send(16);
               }

            });
            ref[0] = destroy;
            destroy.f_93623_ = rank >= 1;
            destroy.m_257544_(tip(rank < 1 ? "Needs the Enchant Stripper perk (stomach level 50)." : "Permanently delete all enchantments on the item in the first box. Nothing is made. Click twice to confirm."));
            e.addListener(destroy);
            strip.f_93623_ = rank >= 1;
            transfer.f_93623_ = rank >= 1 && !ClientState.isHard();
            strip.m_257544_(tip(rank < 1 ? "Needs the Enchant Stripper perk (stomach level 50)." : "Strip the enchantments off the gear in the first box onto a plain book in the second box (the book is used up). Levels lost per enchantment: " + lost + "."));
            transfer.m_257544_(tip(rank < 1 ? "Needs the Enchant Stripper perk (stomach level 50)." : (ClientState.isHard() ? "Soft mode only." : "Move the enchantments from the first box onto the item in the second box. Levels lost per enchantment: " + lost + ".")));
            e.addListener(strip);
            e.addListener(transfer);
            int bw = 34;
            int bh = 12;
            int by = screen.getGuiTop() + 3;
            int right = screen.getGuiLeft() + screen.getXSize() - 7;
            Button digest64 = new SmallTextButton(right - bw, by, bw, bh, Component.m_237113_("Eat 64"), (btn) -> Net.send(10, 64));
            digest64.m_257544_(tip("Eat up to 64 of the item in the first slot. Or just right-click any item in the storage to eat it (hold to keep eating, shift for the whole stack)."));
            Button digest1 = new SmallTextButton(right - bw * 2 - 2, by, bw, bh, Component.m_237113_("Eat 1"), (btn) -> Net.send(10, 1));
            digest1.m_257544_(tip("Eat 1 of the item in the first slot. Or just right-click any item in the storage to eat it."));
            e.addListener(digest1);
            e.addListener(digest64);
         }
      }
   }

   private static Tooltip tip(String text) {
      return GulpConfig.helpTooltips ? Tooltip.m_257550_(Component.m_237113_(text)) : null;
   }

   private static int panelX(ContainerScreen screen) {
      int x = screen.getGuiLeft() - 52 - 4;
      return x < 2 ? screen.getGuiLeft() + screen.getXSize() + 4 : x;
   }

   private static int boxAt(ContainerScreen screen, double mx, double my) {
      int px = panelX(screen);
      int py = screen.getGuiTop() + 24;

      for(int i = 0; i < 2; ++i) {
         int bx = px + i * 28;
         if (mx >= (double)bx && mx < (double)(bx + 24) && my >= (double)py && my < (double)(py + 24)) {
            return i;
         }
      }

      return -1;
   }

   @SubscribeEvent
   public static void onBoxClick(ScreenEvent.MouseButtonPressed.Pre e) {
      Screen var2 = e.getScreen();
      if (var2 instanceof ContainerScreen screen) {
         if ("Stomach Storage".equals(screen.m_96636_().getString())) {
            int box = boxAt(screen, e.getMouseX(), e.getMouseY());
            if (box >= 0 && (e.getButton() == 0 || e.getButton() == 1)) {
               Net.send(15, box | e.getButton() << 4 | (Screen.m_96638_() ? 32 : 0));
               e.setCanceled(true);
            }
         }
      }
   }

   private static boolean isStorage(ContainerScreen screen) {
      return "Stomach Storage".equals(screen.m_96636_().getString());
   }

   @SubscribeEvent
   public static void onSlotRightClick(ScreenEvent.MouseButtonPressed.Pre e) {
      if (GulpConfig.rightClickEat && e.getButton() == 1) {
         Screen var2 = e.getScreen();
         if (var2 instanceof ContainerScreen) {
            ContainerScreen screen = (ContainerScreen)var2;
            if (isStorage(screen)) {
               if (!((ChestMenu)screen.m_6262_()).m_142621_().m_41619_()) {
                  return;
               }

               if (boxAt(screen, e.getMouseX(), e.getMouseY()) >= 0) {
                  return;
               }

               Slot slot = screen.getSlotUnderMouse();
               if (slot != null && slot.f_40219_ < ((ChestMenu)screen.m_6262_()).m_39265_() * 9 && slot.m_6657_()) {
                  Net.send(18, slot.f_40219_ | (Screen.m_96638_() ? 65536 : 0));
                  rightHoldSlot = slot.f_40219_;
                  rightHoldTicks = 0;
                  e.setCanceled(true);
                  return;
               }

               return;
            }
         }

      }
   }

   @SubscribeEvent
   public static void onSlotRightRelease(ScreenEvent.MouseButtonReleased.Pre e) {
      if (e.getButton() == 1 && rightHoldSlot >= 0) {
         rightHoldSlot = -1;
         e.setCanceled(true);
      }

   }

   @SubscribeEvent
   public static void onHoldTick(TickEvent.ClientTickEvent e) {
      if (e.phase == Phase.END && rightHoldSlot >= 0) {
         Minecraft mc = Minecraft.m_91087_();
         Screen var3 = mc.f_91080_;
         if (var3 instanceof ContainerScreen) {
            ContainerScreen screen = (ContainerScreen)var3;
            if (isStorage(screen) && GLFW.glfwGetMouseButton(mc.m_91268_().m_85439_(), 1) == 1) {
               if (!((ChestMenu)screen.m_6262_()).m_38853_(rightHoldSlot).m_6657_()) {
                  rightHoldSlot = -1;
                  return;
               }

               ++rightHoldTicks;
               if (rightHoldTicks >= 8 && (rightHoldTicks - 8) % 2 == 0) {
                  Net.send(18, rightHoldSlot | (Screen.m_96638_() ? 65536 : 0));
               }

               return;
            }
         }

         rightHoldSlot = -1;
      }
   }

   @SubscribeEvent
   public static void onBoxRelease(ScreenEvent.MouseButtonReleased.Pre e) {
      Screen var2 = e.getScreen();
      if (var2 instanceof ContainerScreen screen) {
         if ("Stomach Storage".equals(screen.m_96636_().getString())) {
            if (boxAt(screen, e.getMouseX(), e.getMouseY()) >= 0) {
               e.setCanceled(true);
            }

         }
      }
   }

   private static void renderBox(GuiGraphics g, Font font, int bx, int by, ItemStack stack, String empty, int mx, int my) {
      boolean hover = mx >= bx && mx < bx + 24 && my >= by && my < by + 24;
      g.m_280509_(bx, by, bx + 24, by + 24, hover ? -9787152 : -12945208);
      g.m_280509_(bx + 1, by + 1, bx + 24 - 1, by + 24 - 1, -15721942);
      if (!stack.m_41619_()) {
         g.m_280480_(stack, bx + 4, by + 4);
         g.m_280370_(font, stack, bx + 4, by + 4);
      }

      if (mx >= bx && mx < bx + 24 && my >= by && my < by + 24) {
         if (!stack.m_41619_()) {
            g.m_280153_(font, stack, mx, my);
         } else if (GulpConfig.helpTooltips) {
            g.m_280557_(font, Component.m_237113_(empty), mx, my);
         }
      }

   }

   private static int trackerY(ContainerScreen screen) {
      int y = screen.getGuiTop() + screen.getYSize() + 4;
      return y + 22 > screen.f_96544_ ? Math.max(2, screen.getGuiTop() - 24) : y;
   }

   @SubscribeEvent
   public static void onStorageRender(ScreenEvent.Render.Post e) {
      Screen var2 = e.getScreen();
      if (var2 instanceof ContainerScreen screen) {
         if ("Stomach Storage".equals(screen.m_96636_().getString())) {
            Font font = Minecraft.m_91087_().f_91062_;
            GuiGraphics g = e.getGuiGraphics();
            int px = panelX(screen);
            int py = screen.getGuiTop() + 24;
            renderBox(g, font, px, py, ClientState.stripSlot(0), "Source box: click the enchanted gear in here", e.getMouseX(), e.getMouseY());
            renderBox(g, font, px + 24 + 4, py, ClientState.stripSlot(1), "Target box: click the item to enchant in here (Strip puts the book here)", e.getMouseX(), e.getMouseY());
            int progress = ClientState.digestProgress();
            int threshold = 32;
            String text = "Digestion Progress: " + progress + " / " + threshold + " Items";
            int w = Math.max(font.m_92895_(text), 90);
            int x = screen.getGuiLeft() + (screen.getXSize() - w) / 2;
            int y = trackerY(screen);
            g.m_280056_(font, text, x, y, 16777215, true);
            int by = y + 11;
            g.m_280509_(x, by, x + w, by + 4, -16777216);
            g.m_280509_(x + 1, by + 1, x + w - 1, by + 3, -13421773);
            int fill = (int)((float)(w - 2) * Math.min(1.0F, (float)progress / (float)threshold));
            g.m_280509_(x + 1, by + 1, x + 1 + fill, by + 3, -10635171);
            if (GulpConfig.helpTooltips && e.getMouseX() >= x && e.getMouseX() <= x + w && e.getMouseY() >= y && e.getMouseY() <= by + 4) {
               float bars = StomachItems.hungerBars(ClientState.rank(Perk.UNIVERSAL));
               String tip = String.format("Eats %d items to restore hunger. Current Payout: %.1f Hunger Bars per %d items.", threshold, bars, threshold);
               g.m_280245_(font, font.m_92923_(Component.m_237113_(tip), 190), e.getMouseX(), e.getMouseY());
            }

         }
      }
   }

   private static final class SmallTextButton extends Button {
      private static final float TEXT_SCALE = 0.8F;
      private long armedUntil;

      SmallTextButton(int x, int y, int w, int h, Component label, Button.OnPress onPress) {
         super(x, y, w, h, label, onPress, Button.f_252438_);
      }

      boolean confirmed() {
         long now = System.currentTimeMillis();
         if (now < this.armedUntil) {
            this.armedUntil = 0L;
            return true;
         } else {
            this.armedUntil = now + 3000L;
            return false;
         }
      }

      public void m_280139_(GuiGraphics g, Font font, int color) {
         boolean armed = System.currentTimeMillis() < this.armedUntil;
         Component label = (Component)(armed ? Component.m_237113_("Sure?") : this.m_6035_());
         if (armed) {
            color = -43691;
         }

         float cx = (float)this.m_252754_() + (float)this.m_5711_() / 2.0F;
         float var10000 = (float)this.m_252907_();
         float var10001 = (float)this.m_93694_();
         Objects.requireNonNull(font);
         float cy = var10000 + (var10001 - 9.0F * 0.8F) / 2.0F + 0.5F;
         g.m_280168_().m_85836_();
         g.m_280168_().m_252880_(cx, cy, 0.0F);
         g.m_280168_().m_85841_(0.8F, 0.8F, 1.0F);
         g.m_280430_(font, label, -font.m_92852_(label) / 2, 0, color);
         g.m_280168_().m_85849_();
      }
   }
}
