package com.example.gulp;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public class VolumeSlider extends AbstractSliderButton {
   private final GulpSound sound;

   public VolumeSlider(int x, int y, int width, int height, GulpSound sound) {
      super(x, y, width, height, Component.m_237119_(), (double)GulpConfig.volume(sound) / (double)2.0F);
      this.sound = sound;
      this.m_5695_();
   }

   protected void m_5695_() {
      String var10001 = this.sound.label;
      this.m_93666_(Component.m_237113_(var10001 + ": " + Math.round(GulpConfig.volume(this.sound) * 100.0F) + "%"));
   }

   protected void m_5697_() {
      this.f_93577_ = (double)Math.round(this.f_93577_ * (double)40.0F) / (double)40.0F;
      GulpConfig.setVolume(this.sound, (float)(this.f_93577_ * (double)2.0F));
   }

   public void m_7691_(double mouseX, double mouseY) {
      super.m_7691_(mouseX, mouseY);
      GulpConfig.save();
      ClientSounds.preview(this.sound);
   }
}
