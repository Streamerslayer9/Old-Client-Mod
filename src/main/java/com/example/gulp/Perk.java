package com.example.gulp;

public enum Perk {
   ROOMY("Roomy Stomach", "+6% stomach capacity per rank.", 30),
   HEALING("Healing Stomach", "Soft mode: held mobs heal 1% max HP/rank every 5s.", 15),
   RICH("Better Loot", "Looting on digested mobs: +1 level per 2 ranks (max V).", 10),
   QUICK("Quick Gulp", "Swallow cooldown -0.075s/rank, -0.15s looking up.", 10),
   IRON("Iron Stomach", "Struggling mobs hurt 5% less per rank.", 15),
   GOURMET("Gourmet", "+5% XP from digesting mobs per rank.", 20),
   HEARTY("Hearty Meal", "Heal 0.5 HP per rank whenever you digest a mob.", 10),
   UNIVERSAL("Iron Gut", "Digest any 32 items stored in your stomach to restore hunger.", 3),
   STRIP("Enchant Stripper", "Strip enchantments off gear in your stomach into a book, or move them to another item (soft mode). Higher ranks lose fewer levels.", 3, 50),
   MENDING("Mending Gut", "Gear and tools held in your stomach storage slowly repair themselves.", 5, 60),
   SMELTING("Smelting Gut", "Raw ores in your stomach storage smelt into ingots over time. Higher ranks smelt faster and give extra ingots per ore.", 5, 70);

   public final String title;
   public final String description;
   public final int maxRank;
   public final int minLevel;

   private Perk(String title, String description, int maxRank) {
      this(title, description, maxRank, 1);
   }

   private Perk(String title, String description, int maxRank, int minLevel) {
      this.title = title;
      this.description = description;
      this.maxRank = maxRank;
      this.minLevel = minLevel;
   }

   public static int totalRanks() {
      int total = 0;

      for(Perk p : values()) {
         total += p.maxRank;
      }

      return total;
   }

   public static Perk byIndex(int i) {
      Perk[] v = values();
      return i >= 0 && i < v.length ? v[i] : null;
   }

   // $FF: synthetic method
   private static Perk[] $values() {
      return new Perk[]{ROOMY, HEALING, RICH, QUICK, IRON, GOURMET, HEARTY, UNIVERSAL, STRIP, MENDING, SMELTING};
   }
}
