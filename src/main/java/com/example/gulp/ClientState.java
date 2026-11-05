package com.example.gulp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class ClientState {
   private static boolean hard;
   private static boolean instantDigest;
   private static int heldMode;
   private static String heldHolder = "";
   private static float heldProgress;
   private static float heldChance;
   private static Role role;
   private static SwallowSystem swallowSystem;
   private static boolean serverFlamevore;
   private static boolean synced;
   private static final Map<UUID, Role> roles;
   private static int predator;
   private static float itemFill;
   private static int digestProgress;
   private static ItemStack strip0;
   private static ItemStack strip1;
   private static int level;
   private static int xp;
   private static int xpNeeded;
   private static int points;
   private static float used;
   private static float cap;
   private static boolean multiOrgans;
   private static int breastMilk;
   private static int breastButter;
   private static float breastChurn;
   private static int activeOrgan;
   private static int hardMask;
   private static float[] organUsed;
   private static int[] ranks;
   private static List<EntryInfo> entries;
   public static BiConsumer<Integer, Integer> levelUpListener;
   public static Consumer<Component> messageListener;
   public static SoundListener soundListener;
   private static final Map<UUID, Float> BELLY_TARGET;
   private static final Map<UUID, Float> BELLY_SHOWN;
   private static final Map<UUID, Long> BELLY_TIME;

   private ClientState() {
   }

   public static void apply(SyncPacket m) {
      hard = m.hard;
      instantDigest = m.instantDigest;
      level = m.level;
      xp = m.xp;
      xpNeeded = m.xpNeeded;
      used = m.used;
      cap = m.cap;
      multiOrgans = m.multiOrgans;
      breastMilk = m.breastMilk;
      breastButter = m.breastButter;
      breastChurn = m.breastChurn;
      activeOrgan = m.activeOrgan;
      hardMask = m.hardMask;
      organUsed = m.organUsed;
      points = m.points;
      role = Role.byId(m.role) != null ? Role.byId(m.role) : Role.PRED;
      SwallowSystem system = SwallowSystem.byId(m.swallowSystem);
      swallowSystem = system != null ? system : SwallowSystem.UNSET;
      serverFlamevore = m.flamevoreOnServer;
      synced = true;
      predator = m.predator;
      itemFill = m.itemFill;
      digestProgress = m.digestProgress;
      strip0 = m.strip0;
      strip1 = m.strip1;
      ranks = m.ranks;
      entries = m.entries;
   }

   public static void fireLevelUp(int level, int points) {
      if (levelUpListener != null) {
         levelUpListener.accept(level, points);
      }

   }

   public static void fireMessage(Component text) {
      if (messageListener != null) {
         messageListener.accept(text);
      }

   }

   public static void firePlaySound(int type, double x, double y, double z) {
      if (soundListener != null) {
         soundListener.play(type, x, y, z);
      }

   }

   public static void reset() {
      hard = false;
      instantDigest = false;
      level = 1;
      xp = 0;
      xpNeeded = Stomach.xpForNext(1);
      points = 0;
      role = Role.PRED;
      swallowSystem = SwallowSystem.UNSET;
      serverFlamevore = false;
      synced = false;
      roles.clear();
      heldMode = 0;
      predator = 0;
      itemFill = 0.0F;
      digestProgress = 0;
      strip0 = ItemStack.f_41583_;
      strip1 = ItemStack.f_41583_;
      used = 0.0F;
      cap = 1.75F;
      multiOrgans = false;
      breastMilk = 0;
      breastButter = 0;
      breastChurn = 0.0F;
      activeOrgan = 0;
      hardMask = 0;
      organUsed = new float[Organ.COUNT];
      ranks = new int[Perk.values().length];
      entries = new ArrayList();
      BELLY_TARGET.clear();
      BELLY_SHOWN.clear();
      BELLY_TIME.clear();
   }

   public static boolean isHard() {
      return hard;
   }

   public static boolean isInstantDigest() {
      return instantDigest;
   }

   public static Role role() {
      return role;
   }

   public static SwallowSystem swallowSystem() {
      return swallowSystem;
   }

   public static boolean usesGulpSwallow() {
      return !serverFlamevore || swallowSystem == SwallowSystem.GULP;
   }

   public static boolean flamevoreActive() {
      return serverFlamevore;
   }

   public static boolean needsSwallowChoice() {
      return synced && serverFlamevore && swallowSystem == SwallowSystem.UNSET;
   }

   public static void setHeld(int mode, String holder, float progress, float chance) {
      if (mode != 0 && heldMode == 0) {
         McaScreenHooks.closeOnHeld();
      }

      heldMode = mode;
      heldHolder = holder;
      heldProgress = progress;
      heldChance = chance;
   }

   public static int heldMode() {
      return heldMode;
   }

   public static String heldHolder() {
      return heldHolder;
   }

   public static float heldProgress() {
      return heldProgress;
   }

   public static float heldChance() {
      return heldChance;
   }

   public static Role roleOf(UUID id) {
      return (Role)roles.get(id);
   }

   public static void setRoles(Map<UUID, Integer> map) {
      roles.clear();
      map.forEach((id, r) -> {
         Role role = Role.byId(r);
         if (role != null) {
            roles.put(id, role);
         }

      });
   }

   public static int predator() {
      return predator;
   }

   public static float itemFill() {
      return itemFill;
   }

   public static int digestProgress() {
      return digestProgress;
   }

   public static ItemStack stripSlot(int i) {
      return i == 0 ? strip0 : strip1;
   }

   public static int level() {
      return level;
   }

   public static int xp() {
      return xp;
   }

   public static int xpNeeded() {
      return xpNeeded;
   }

   public static int points() {
      return points;
   }

   public static float used() {
      return used;
   }

   public static float cap() {
      return cap;
   }

   public static boolean multiOrgans() {
      return multiOrgans;
   }

   public static int breastMilk() {
      return breastMilk;
   }

   public static int breastButter() {
      return breastButter;
   }

   public static float breastChurn() {
      return breastChurn;
   }

   public static int activeOrgan() {
      return activeOrgan;
   }

   public static boolean isHard(int organ) {
      return (hardMask & 1 << organ) != 0;
   }

   public static float usedIn(int organ) {
      return organ >= 0 && organ < organUsed.length ? organUsed[organ] : 0.0F;
   }

   public static int rank(Perk p) {
      return ranks[p.ordinal()];
   }

   public static List<EntryInfo> entries() {
      return Collections.unmodifiableList(entries);
   }

   public static String signature() {
      StringBuilder sb = new StringBuilder();
      sb.append(hard).append(instantDigest).append(activeOrgan).append(hardMask).append(breastMilk).append(',').append(breastButter).append('|').append(points).append('|').append(Arrays.toString(ranks)).append('|');

      for(EntryInfo e : entries) {
         sb.append(e.uid()).append(':').append(e.organ()).append(',');
      }

      return sb.toString();
   }

   public static void setBelly(UUID id, float ratio) {
      BELLY_TARGET.put(id, ratio);
   }

   public static float bellyAmount(UUID id) {
      return (Float)BELLY_TARGET.getOrDefault(id, 0.0F);
   }

   public static float smoothedBelly(UUID id) {
      float target = bellyAmount(id);
      float cur = (Float)BELLY_SHOWN.getOrDefault(id, 0.0F);
      long now = System.nanoTime();
      Long last = (Long)BELLY_TIME.put(id, now);
      float seconds = last == null ? 0.016F : Math.min(0.25F, (float)(now - last) / 1.0E9F);
      cur += (target - cur) * (1.0F - (float)Math.exp((double)(-seconds / 0.35F)));
      if (Math.abs(target - cur) < 0.002F) {
         cur = target;
      }

      BELLY_SHOWN.put(id, cur);
      return cur;
   }

   static {
      role = Role.PRED;
      swallowSystem = SwallowSystem.UNSET;
      roles = new HashMap();
      strip0 = ItemStack.f_41583_;
      strip1 = ItemStack.f_41583_;
      level = 1;
      xpNeeded = 100;
      cap = 1.75F;
      organUsed = new float[Organ.COUNT];
      ranks = new int[Perk.values().length];
      entries = new ArrayList();
      BELLY_TARGET = new HashMap();
      BELLY_SHOWN = new HashMap();
      BELLY_TIME = new HashMap();
   }

   public static record EntryInfo(int uid, Component name, String typeId, float size, float hp, float maxHp, float digest, int organ, float transit) {
   }

   public interface SoundListener {
      void play(int var1, double var2, double var4, double var6);
   }
}
