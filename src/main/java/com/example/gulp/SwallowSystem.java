package com.example.gulp;

public enum SwallowSystem {
   UNSET("Not chosen"),
   GULP("Gulp"),
   FLAMEVORE("Flamevore");

   public final String label;

   private SwallowSystem(String label) {
      this.label = label;
   }

   public static SwallowSystem byId(int id) {
      SwallowSystem[] all = values();
      return id >= 0 && id < all.length ? all[id] : null;
   }

   public static SwallowSystem byName(String name) {
      for(SwallowSystem s : values()) {
         if (s.name().equals(name)) {
            return s;
         }
      }

      return null;
   }

   // $FF: synthetic method
   private static SwallowSystem[] $values() {
      return new SwallowSystem[]{UNSET, GULP, FLAMEVORE};
   }
}
