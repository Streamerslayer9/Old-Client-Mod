package com.example.gulp;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.slf4j.Logger;

@EventBusSubscriber(
   modid = "gulp",
   value = {Dist.CLIENT}
)
public final class McaScreenHooks {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static boolean logged;
   private static final int LEFT_X = 18;
   private static final int BOTTOM_DY = 36;
   private static final int BAR_DY = 18;
   private static final int BAR_H = 9;
   private static final int PERKS_GAP = 3;
   private static final boolean DEBUG = false;
   private static String debugLine = "";
   private static Screen debugScreen;
   private static Screen shownScreen;
   private static LivingEntity villager;
   private static int topY;
   private static Button perksButton;
   private static int lastRight = -1;
   private static String lastState = "";

   private McaScreenHooks() {
   }

   static void closeOnHeld() {
      Minecraft mc = Minecraft.m_91087_();
      Screen cur = mc.f_91080_;
      if (cur != null && (cur instanceof VillagerPerksScreen || isInteractScreen(cur))) {
         mc.m_91152_((Screen)null);
      }

   }

   static boolean isInteractScreen(Screen screen) {
      String name = screen.getClass().getName().toLowerCase(Locale.ROOT);
      return name.contains("mca") && name.endsWith("interactscreen");
   }

   private static LivingEntity findVillager(Screen screen, Minecraft mc) {
      for(Class<?> c = screen.getClass(); c != null && c != Screen.class; c = c.getSuperclass()) {
         for(Field f : c.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && !f.getType().isPrimitive()) {
               try {
                  f.setAccessible(true);
                  Object var8 = f.get(screen);
                  if (var8 instanceof LivingEntity) {
                     LivingEntity le = (LivingEntity)var8;
                     if (le != mc.f_91074_) {
                        return le;
                     }
                  }
               } catch (RuntimeException | ReflectiveOperationException var9) {
               }
            }
         }
      }

      Entity var11 = mc.f_91076_;
      LivingEntity var10000;
      if (var11 instanceof LivingEntity le) {
         var10000 = le;
      } else {
         var10000 = null;
      }

      return var10000;
   }

   private static void dbg(Screen screen, String msg) {
      debugScreen = screen;
      debugLine = msg;
   }

   @SubscribeEvent
   public static void onInit(ScreenEvent.Init.Post e) {
      Screen screen = e.getScreen();
      if (!isInteractScreen(screen)) {
         if (screen == shownScreen) {
            shownScreen = null;
         }

         String n = screen.getClass().getName().toLowerCase(Locale.ROOT);
      } else {
         Minecraft mc = Minecraft.m_91087_();
         LivingEntity v = findVillager(screen, mc);
         shownScreen = null;
         perksButton = null;
         lastRight = -1;
         if (v == null) {
            dbg(screen, "FAIL: no villager found on screen");
         } else {
            villager = v;
            shownScreen = screen;
            Net.send(20, v.m_19879_());
            List<AbstractWidget> right = new ArrayList();
            int total = 0;
            StringBuilder all = new StringBuilder();

            for(GuiEventListener l : e.getListenersList()) {
               if (l instanceof AbstractWidget) {
                  AbstractWidget w = (AbstractWidget)l;
                  ++total;
                  if (all.length() < 150) {
                     all.append(w.m_252754_()).append(',').append(w.m_252907_()).append(w.f_93624_ ? "" : "(hid)").append(' ');
                  }

                  if (w.f_93624_ && (double)w.m_252754_() >= (double)screen.f_96543_ * 0.6) {
                     right.add(w);
                  }
               }
            }

            right.sort(Comparator.comparingInt(AbstractWidget::m_252907_));
            boolean mainMenu = false;
            int step = 0;
            int bh;
            String why;
            int bx;
            int by;
            int bw;
            if (right.size() >= 2) {
               AbstractWidget talk = (AbstractWidget)right.get(0);
               step = ((AbstractWidget)right.get(1)).m_252907_() - talk.m_252907_();
               topY = talk.m_252907_();

               for(int i = 1; i < right.size(); ++i) {
                  if (((AbstractWidget)right.get(i)).m_252907_() - ((AbstractWidget)right.get(i - 1)).m_252907_() >= step * 2) {
                     mainMenu = true;
                  }
               }

               bw = talk.m_5711_() * 3 / 4;
               bx = talk.m_252754_() - bw - 3;
               by = talk.m_252907_();
               bh = talk.m_93694_();
               why = mainMenu ? "OK: anchored to Talk" : "sub-menu (no big gap): button hidden";
            } else {
               bw = 60;
               bh = 18;
               bx = screen.f_96543_ - 90 - 3 - bw;
               by = 17;
               mainMenu = true;
               int var10000 = right.size();
               why = "FALLBACK: only " + var10000 + " right-side buttons of " + total + " widgets";
            }

            int id = v.m_19879_();
            String name = v.m_7755_().getString();
            perksButton = Button.m_253074_(Component.m_237113_("Perks"), (btn) -> mc.m_91152_(new VillagerPerksScreen(screen, id, name))).m_252987_(bx, by, bw, bh).m_253136_();
            perksButton.f_93624_ = mainMenu;
            e.addListener(perksButton);
            dbg(screen, why + " | " + total + " widgets at init");
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGH
   )
   public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre e) {
      if (e.getButton() == 0 && e.getScreen() == shownScreen && perksButton != null && villager != null) {
         if (perksButton.f_93624_ && perksButton.f_93623_ && perksButton.m_5953_(e.getMouseX(), e.getMouseY())) {
            LOGGER.info("[Gulp] Perks button clicked for villager {}", villager.m_7755_().getString());
            perksButton.m_7435_(Minecraft.m_91087_().m_91106_());
            perksButton.m_5691_();
            e.setCanceled(true);
         }
      }
   }

   private static boolean isBackButton(AbstractWidget w) {
      Component msg = w.m_6035_();
      ComponentContents var3 = msg.m_214077_();
      if (var3 instanceof TranslatableContents tc) {
         if (tc.m_237508_().toLowerCase(Locale.ROOT).contains("back")) {
            return true;
         }
      }

      return msg.getString().trim().equalsIgnoreCase("back");
   }

   private static void reanchor(Screen screen) {
      if (perksButton != null) {
         List<AbstractWidget> right = new ArrayList();
         int total = 0;
         boolean back = false;
         StringBuilder labels = new StringBuilder();

         for(GuiEventListener l : screen.m_6702_()) {
            if (l instanceof AbstractWidget) {
               AbstractWidget w = (AbstractWidget)l;
               if (w != perksButton) {
                  ++total;
                  if (w.f_93624_) {
                     if (labels.length() < 200) {
                        labels.append('[').append(w.m_6035_().getString()).append("] ");
                     }

                     if (isBackButton(w)) {
                        back = true;
                     }

                     if ((double)w.m_252754_() >= (double)screen.f_96543_ * 0.6) {
                        right.add(w);
                     }
                  }
               }
            }
         }

         if (right.size() < 2) {
            perksButton.f_93624_ = false;
         } else {
            right.sort(Comparator.comparingInt(AbstractWidget::m_252907_));
            AbstractWidget talk = (AbstractWidget)right.get(0);
            int step = ((AbstractWidget)right.get(1)).m_252907_() - talk.m_252907_();
            boolean mainMenu = !back;
            int bw = talk.m_5711_() * 3 / 4;
            perksButton.m_93674_(bw);
            perksButton.m_252865_(talk.m_252754_() - bw - 3);
            perksButton.m_253211_(talk.m_252907_());
            perksButton.setHeight(talk.m_93694_());
            perksButton.f_93624_ = mainMenu;
            String state = (mainMenu ? "main" : "sub-page") + " | back=" + back + " | " + String.valueOf(labels);
            if (!state.equals(lastState)) {
               lastState = state;
               LOGGER.info("[Gulp] MCA villager screen is now: {}", state);
            }

            int var10001 = talk.m_252754_();
            dbg(screen, "anchored to Talk at " + var10001 + "," + talk.m_252907_() + " | " + total + " widgets, " + right.size() + " right, mainMenu=" + mainMenu);
         }
      }
   }

   private static int box(GuiGraphics g, Font font, String text, int x, int y) {
      int w = font.m_92895_(text) + 8;
      g.m_280509_(x, y, x + w, y + 16, -267386864);
      g.m_280509_(x, y, x + w, y + 1, -12838280);
      g.m_280509_(x, y + 15, x + w, y + 16, -12838280);
      g.m_280509_(x, y, x + 1, y + 16, -12838280);
      g.m_280509_(x + w - 1, y, x + w, y + 16, -12838280);
      g.m_280056_(font, text, x + 4, y + 4, 16777215, false);
      return w;
   }

   @SubscribeEvent
   public static void onRender(ScreenEvent.Render.Post e) {
      if (e.getScreen() == shownScreen && villager != null) {
         reanchor(e.getScreen());
         VillagerInfoPacket info = ClientVillagerInfo.get(villager.m_19879_());
         Font font = Minecraft.m_91087_().f_91062_;
         GuiGraphics g = e.getGuiGraphics();
         if (perksButton != null && (info == null || !info.adult)) {
            perksButton.f_93624_ = false;
         }

         if (perksButton != null) {
            perksButton.m_88315_(g, e.getMouseX(), e.getMouseY(), e.getPartialTick());
         }

         if (info != null && info.adult) {
            String kind = info.type == 2 ? "Pred" : (info.type == 1 ? "Prey" : "Normal");
            VillagerTrait trait = VillagerTrait.byId(info.trait);
            if (trait != VillagerTrait.NONE) {
               kind = kind + " (" + trait.label.toLowerCase(Locale.ROOT) + ")";
            }

            int typeY = e.getScreen().f_96544_ - 36;
            int boxW = box(g, font, "Type: " + kind, 18, typeY);
            int by = typeY + 18;
            boolean max = info.level >= 10;
            float frac = max ? 1.0F : Math.min(1.0F, (float)info.xp / (float)Math.max(1, info.xpNeeded));
            g.m_280509_(17, by - 1, 18 + boxW + 1, by + 9 + 1, -16777216);
            g.m_280509_(18, by, 18 + boxW, by + 9, -14540254);
            g.m_280509_(18, by, 18 + (int)((float)boxW * frac), by + 9, -1652662);
            String lv = "Lv " + info.level;
            g.m_280056_(font, lv, 18 + (boxW - font.m_92895_(lv)) / 2, by + 1, 16777215, true);
         }
      }
   }
}
