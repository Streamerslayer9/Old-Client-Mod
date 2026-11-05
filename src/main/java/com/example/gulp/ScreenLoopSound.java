package com.example.gulp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class ScreenLoopSound extends AbstractTickableSoundInstance {
   private static final float BASE_VOLUME = 0.25F;

   public ScreenLoopSound() {
      super((SoundEvent)Gulp.SCREEN_LOOP.get(), SoundSource.PLAYERS, SoundInstance.m_235150_());
      this.f_119578_ = true;
      this.f_119579_ = 0;
      this.f_119573_ = currentVolume();
      this.f_119582_ = true;
      this.f_119580_ = Attenuation.NONE;
   }

   private static float currentVolume() {
      return Math.min(1.0F, 0.25F * GulpConfig.volume(GulpSound.SCREEN_LOOP));
   }

   public boolean m_7784_() {
      return true;
   }

   public void m_7788_() {
      Object screen = Minecraft.m_91087_().f_91080_;
      if (!(screen instanceof StomachScreen) && !(screen instanceof PerksScreen) && !(screen instanceof PerkTreeScreen) && !(screen instanceof SettingsScreen) && !(screen instanceof TogglesScreen) && !(screen instanceof SoundsScreen)) {
         this.m_119609_();
      } else {
         this.f_119573_ = currentVolume();
      }
   }
}
