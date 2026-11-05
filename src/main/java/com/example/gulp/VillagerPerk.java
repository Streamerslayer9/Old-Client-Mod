package com.example.gulp;

import java.util.ArrayList;
import java.util.List;

public enum VillagerPerk {
   SOOTHING("Soothing Touch", "Heals you +0.5 HP every second per rank.", 3, false, 2),
   EMBRACE("Long Embrace", "Holds you +12 seconds longer per rank.", 3, false, 2),
   WARM_BELLY("Warm Belly", "Feeds you while holding you: more hunger per rank.", 3, false, 2),
   ROOMY_BELLY("Roomy Belly", "Holds one more player at once per rank.", 2, false, 2),
   SECOND_WIND("Second Wind", "When a rest ends well you get Regeneration for 5 seconds per rank.", 3, true, 2),
   EAGER_RIDER("Eager Rider", "Asks to be carried more often and needs fewer hearts to trust you.", 3, false, 1),
   SNUG_PASSENGER("Snug Passenger", "Likes you more after every safe ride: +3 hearts per rank.", 3, false, 1),
   PARTING_GIFT("Parting Gift", "After a ride of 30+ seconds they give you XP (once every 10 minutes).", 3, true, 1),
   HAGGLER("Haggler", "Gives you a better price when you trade: 5% off per rank.", 3, false, 0),
   CHATTY("Chatty", "Earns +1 XP per rank every time you talk to them.", 3, false, 0),
   WARM_WELCOME("Warm Welcome", "Likes you a little more every time you talk: +1 heart per rank.", 3, true, 0);

   public final String title;
   public final String description;
   public final int maxRank;
   public final boolean active;
   public final int forType;

   private VillagerPerk(String title, String description, int maxRank, boolean active, int forType) {
      this.title = title;
      this.description = description;
      this.maxRank = maxRank;
      this.active = active;
      this.forType = forType;
   }

   public static VillagerPerk byIndex(int i) {
      VillagerPerk[] v = values();
      return i >= 0 && i < v.length ? v[i] : null;
   }

   public static List<VillagerPerk> perksFor(int type) {
      List<VillagerPerk> list = new ArrayList();

      for(VillagerPerk p : values()) {
         if (p.forType == type) {
            list.add(p);
         }
      }

      return list;
   }

   public static int totalRanks(int type) {
      int total = 0;

      for(VillagerPerk p : perksFor(type)) {
         total += p.maxRank;
      }

      return total;
   }

   public static float soothingBonus(int rank) {
      return 0.5F * (float)rank;
   }

   public static int slotsFor(int level) {
      return Math.min(3, 1 + Math.max(1, level) / 4);
   }

   public static int capacityFor(int level, int roomyRank) {
      return Math.min(6, 1 + Math.max(1, level) / 5 + roomyRank);
   }

   public static int secondWindTicks(int rank) {
      return 100 * rank;
   }

   public static int embraceBonusTicks(int rank) {
      return 240 * rank;
   }

   public static int partingGiftXp(int rank, int heldTicks) {
      return rank * Math.min(10, 2 + heldTicks / 600);
   }

   // $FF: synthetic method
   private static VillagerPerk[] $values() {
      return new VillagerPerk[]{SOOTHING, EMBRACE, WARM_BELLY, ROOMY_BELLY, SECOND_WIND, EAGER_RIDER, SNUG_PASSENGER, PARTING_GIFT, HAGGLER, CHATTY, WARM_WELCOME};
   }
}
