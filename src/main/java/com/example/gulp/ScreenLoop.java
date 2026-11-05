package com.example.gulp;

import net.minecraft.client.Minecraft;

public final class ScreenLoop {
   private static ScreenLoopSound current;

   private ScreenLoop() {
   }

   private static boolean enabled() {
      return GulpConfig.useCustomSounds && GulpConfig.isOn(GulpSound.SCREEN_LOOP);
   }

   public static void start() {
      if (enabled()) {
         if (current == null || current.m_7801_()) {
            current = new ScreenLoopSound();
            Minecraft.m_91087_().m_91106_().m_120367_(current);
         }

      }
   }

   public static void stop() {
      if (current != null) {
         Minecraft.m_91087_().m_91106_().m_120399_(current);
         current = null;
      }

   }

   public static void refresh() {
      if (enabled()) {
         start();
      } else {
         stop();
      }

   }
}
