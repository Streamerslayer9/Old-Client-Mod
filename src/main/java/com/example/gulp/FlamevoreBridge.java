package com.example.gulp;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import org.slf4j.Logger;

public final class FlamevoreBridge {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String API = "com.flame.flamevoremod.api.";
   private static Method getPred;
   private static Method getPrey;
   private static Method preyMaxHealth;
   private static Method preyIsPlayer;
   private static boolean broken;

   private FlamevoreBridge() {
   }

   public static void register() {
      if (FlamevoreCompat.isLoaded()) {
         try {
            Class<?> voreEvent = Class.forName("com.flame.flamevoremod.api.event.VoreEvent");
            Class<?> digestEvent = Class.forName("com.flame.flamevoremod.api.event.DigestEvent");
            Class<?> digested = Class.forName("com.flame.flamevoremod.api.event.DigestEvent$Digested");
            Class<?> preyInfo = Class.forName("com.flame.flamevoremod.api.PreyInfo");
            getPred = voreEvent.getMethod("getPred");
            getPrey = digestEvent.getMethod("getPrey");
            preyMaxHealth = preyInfo.getMethod("maxHealth");
            preyIsPlayer = preyInfo.getMethod("player");
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, digested, FlamevoreBridge::onDigested);
            LOGGER.info("[Gulp] Flamevore digests now give Gulp XP and perks (listening to {}).", digested.getName());
         } catch (RuntimeException | LinkageError | ReflectiveOperationException e) {
            LOGGER.warn("[Gulp] Couldn't hook into Flamevore's digest events, so Flamevore digests won't give Gulp XP.", e);
         }

      }
   }

   private static void onDigested(Event event) {
      if (!broken) {
         try {
            Object var2 = getPred.invoke(event);
            if (var2 instanceof ServerPlayer) {
               ServerPlayer pred = (ServerPlayer)var2;
               if (pred.m_20194_() != null) {
                  Stomach s = StomachManager.get(pred.m_20194_()).of(pred.m_20148_());
                  if (s.swallowSystem != SwallowSystem.FLAMEVORE) {
                     return;
                  }

                  Object prey = getPrey.invoke(event);
                  if (Boolean.TRUE.equals(preyIsPlayer.invoke(prey))) {
                     StomachLogic.gainXp(pred, s, 100.0F);
                  } else {
                     float maxHealth = ((Number)preyMaxHealth.invoke(prey)).floatValue();
                     int hearty = s.rank(Perk.HEARTY);
                     if (hearty > 0) {
                        pred.m_5634_(0.5F * (float)hearty);
                     }

                     StomachLogic.gainXp(pred, s, (float)Stomach.digestXp(maxHealth) * (1.0F + 0.05F * (float)s.rank(Perk.GOURMET)));
                  }

                  StomachLogic.sync(pred, s);
                  return;
               }
            }

         } catch (RuntimeException | ReflectiveOperationException e) {
            broken = true;
            LOGGER.warn("[Gulp] Flamevore's digest event looked different than expected, so Flamevore XP is switched off.", e);
         }
      }
   }
}
