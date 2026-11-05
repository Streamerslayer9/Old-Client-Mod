package com.example.gulp;

import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public enum GulpSound {
   SWALLOW("Swallow", () -> (SoundEvent)Gulp.SWALLOW.get(), () -> (SoundEvent)Gulp.SWALLOW_LOUD.get(), () -> SoundEvents.f_11912_),
   SPIT("Spit out", () -> (SoundEvent)Gulp.RELEASE.get(), () -> (SoundEvent)Gulp.RELEASE_LOUD.get(), () -> SoundEvents.f_12388_),
   DIGEST("Digestion", () -> (SoundEvent)Gulp.DIGEST.get(), () -> (SoundEvent)Gulp.DIGEST_LOUD.get(), () -> SoundEvents.f_12321_),
   DIGEST_BUTTON("Digest button", () -> (SoundEvent)Gulp.DIGEST_BUTTON.get(), () -> (SoundEvent)Gulp.DIGEST_BUTTON_LOUD.get(), () -> SoundEvents.f_12321_),
   SCREEN_LOOP("Screen music", () -> (SoundEvent)Gulp.SCREEN_LOOP.get(), () -> null, () -> null);

   public final String label;
   private final Supplier<SoundEvent> custom;
   private final Supplier<SoundEvent> loud;
   private final Supplier<SoundEvent> vanilla;

   private GulpSound(String label, Supplier<SoundEvent> custom, Supplier<SoundEvent> loud, Supplier<SoundEvent> vanilla) {
      this.label = label;
      this.custom = custom;
      this.loud = loud;
      this.vanilla = vanilla;
   }

   public SoundEvent custom() {
      return (SoundEvent)this.custom.get();
   }

   public SoundEvent loud() {
      return (SoundEvent)this.loud.get();
   }

   public SoundEvent vanilla() {
      return (SoundEvent)this.vanilla.get();
   }

   public static GulpSound byIndex(int i) {
      GulpSound[] v = values();
      return i >= 0 && i < v.length ? v[i] : null;
   }

   // $FF: synthetic method
   private static GulpSound[] $values() {
      return new GulpSound[]{SWALLOW, SPIT, DIGEST, DIGEST_BUTTON, SCREEN_LOOP};
   }
}
