package com.example.gulp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public final class ClientSounds {
   private ClientSounds() {
   }

   private static Choice choose(GulpSound sound) {
      if (!GulpConfig.isOn(sound)) {
         return null;
      } else {
         float percent = GulpConfig.volume(sound);
         if (percent <= 0.0F) {
            return null;
         } else if (!GulpConfig.useCustomSounds) {
            SoundEvent vanilla = sound.vanilla();
            return vanilla == null ? null : new Choice(vanilla, Math.min(percent, 1.0F));
         } else if (percent <= 1.0F) {
            return new Choice(sound.custom(), percent);
         } else {
            SoundEvent loud = sound.loud();
            return loud == null ? new Choice(sound.custom(), 1.0F) : new Choice(loud, percent / 2.0F);
         }
      }
   }

   public static void playWorld(int type, double x, double y, double z) {
      GulpSound sound = GulpSound.byIndex(type);
      if (sound != null) {
         Choice c = choose(sound);
         Minecraft mc = Minecraft.m_91087_();
         if (c != null && mc.f_91073_ != null) {
            mc.f_91073_.m_7785_(x, y, z, c.event(), SoundSource.PLAYERS, c.volume(), 1.0F, false);
         }
      }
   }

   public static void preview(GulpSound sound) {
      if (sound != GulpSound.SCREEN_LOOP) {
         Choice c = choose(sound);
         if (c != null) {
            Minecraft.m_91087_().m_91106_().m_120367_(SimpleSoundInstance.m_119755_(c.event(), 1.0F, c.volume()));
         }
      }
   }

   private static record Choice(SoundEvent event, float volume) {
   }
}
