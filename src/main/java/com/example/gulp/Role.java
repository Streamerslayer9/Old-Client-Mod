package com.example.gulp;

import net.minecraft.network.chat.Component;

public enum Role {
   PRED("Pred", -43691, "Predator: can swallow, can't be swallowed", "You can swallow others. Others can't swallow you."),
   PREY("Prey", -11141121, "Prey: can be swallowed, can't swallow", "Others can swallow you, but only if you accept their request. You can't swallow anyone."),
   BOTH("Both", -22016, "Pred & Prey: can swallow and be swallowed", "You can swallow others, and they can swallow you (only if you accept their request)."),
   NEITHER("Neither", -5592406, "Vore is off", "All vore is off for you: you can't swallow anyone, nobody can swallow you, and the stomach menu is hidden.");

   public final String label;
   public final int color;
   public final String hint;
   public final String description;

   private Role(String label, int color, String hint, String description) {
      this.label = label;
      this.color = color;
      this.hint = hint;
      this.description = description;
   }

   public boolean canEat() {
      return this == PRED || this == BOTH;
   }

   public boolean canBeEaten() {
      return this == PREY || this == BOTH;
   }

   public Role next() {
      return values()[(this.ordinal() + 1) % values().length];
   }

   public static Role byId(int id) {
      Role[] all = values();
      return id >= 0 && id < all.length ? all[id] : null;
   }

   public static Role byName(String name) {
      for(Role r : values()) {
         if (r.name().equalsIgnoreCase(name)) {
            return r;
         }
      }

      return null;
   }

   public Component cantSwallowMessage() {
      return Component.m_237113_(this == NEITHER ? "Vore is turned off for you (role: Neither). Change your role in Settings." : "You're set to Prey, so you can't swallow anything. Change your role in Settings.");
   }

   public Component cantBeSwallowedMessage() {
      return Component.m_237113_(this == PRED ? "This player is a predator and cannot be swallowed" : "This player has opted out of vore and cannot be swallowed");
   }

   // $FF: synthetic method
   private static Role[] $values() {
      return new Role[]{PRED, PREY, BOTH, NEITHER};
   }
}
