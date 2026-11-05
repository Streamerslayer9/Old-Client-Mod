package com.example.gulp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

public final class PlayerHolding {
   static final String KEY_PREV_MODE = "gulp_prev_mode";
   private static final Map<UUID, Held> HELD = new HashMap();

   public static boolean hardVore(Stomach s) {
      return s.isHard(s.organ()) && (Boolean)GulpServerConfig.HARD_MODE_ENABLED.get() && (Boolean)GulpServerConfig.PLAYER_HARD_VORE_ENABLED.get();
   }

   private PlayerHolding() {
   }

   public static boolean isHeld(ServerPlayer p) {
      return HELD.containsKey(p.m_20148_()) || VillagerHolding.isHeld(p);
   }

   public static boolean isPlayerEntry(CompoundTag entry) {
      return entry.m_128441_("PlayerUuid");
   }

   static float sizeOf(ServerPlayer victim) {
      return Math.max(0.05F, victim.m_20205_() * victim.m_20205_() * victim.m_20206_());
   }

   static Component blocker(ServerPlayer p, Stomach s, ServerPlayer victim, StomachManager mgr) {
      if (!(Boolean)GulpServerConfig.PLAYER_SWALLOWING_ENABLED.get()) {
         return Component.m_237113_("Swallowing players is turned off on this server.");
      } else if (!s.role.canEat()) {
         return s.role.cantSwallowMessage();
      } else if (victim.m_6084_() && !victim.m_5833_() && !isHeld(victim)) {
         Role victimRole = mgr.of(victim.m_20148_()).role;
         if (!victimRole.canBeEaten()) {
            return victimRole.cantBeSwallowedMessage();
         } else {
            float size = sizeOf(victim);
            String where = Organ.byId(s.organ()).lower();
            if ((double)size > s.capacityOf(s.organ())) {
               return Component.m_237113_("Too big for your " + where + "! (needs " + Stomach.fmtVolume((double)size) + ", capacity " + Stomach.fmtVolume(s.capacity()) + ")");
            } else if (s.usedIn(s.organ()) + (double)size > s.capacityOf(s.organ())) {
               return Component.m_237113_("Your " + where + " is too full.");
            } else {
               return s.contents.size() >= 100 ? Component.m_237113_("You can't hold more than 100 creatures at once.") : null;
            }
         }
      } else {
         return Component.m_237113_("Nothing swallowable in reach.");
      }
   }

   public static void request(ServerPlayer p, Stomach s, ServerPlayer victim, StomachManager mgr) {
      Component why = blocker(p, s, victim, mgr);
      if (why != null) {
         StomachLogic.say(p, why);
      } else {
         VoreRequests.send(p, victim);
      }
   }

   public static boolean swallow(ServerPlayer p, Stomach s, ServerPlayer victim, StomachManager mgr) {
      Component why = blocker(p, s, victim, mgr);
      if (why != null) {
         StomachLogic.say(p, why);
         return false;
      } else {
         float size = sizeOf(victim);
         boolean hard = hardVore(s);
         CompoundTag entry = new CompoundTag();
         CompoundTag display = new CompoundTag();
         display.m_128359_("id", "minecraft:player");
         display.m_128350_("Health", victim.m_21223_());
         entry.m_128365_("Entity", display);
         entry.m_128359_("PlayerUuid", victim.m_20148_().toString());
         entry.m_128350_("Size", size);
         entry.m_128350_("MaxHealth", victim.m_21233_());
         entry.m_128405_("Digest", 0);
         entry.m_128405_("DigestTime", hard ? (Integer)GulpServerConfig.PLAYER_DIGEST_SECONDS.get() * 20 : 0);
         entry.m_128379_("HardVore", hard);
         Component name = victim.m_7755_();
         entry.m_128359_("Name", name.getString());
         entry.m_128359_("NameJson", Serializer.m_130703_(name));
         entry.m_128405_("Uid", p.m_217043_().m_188502_());
         entry.m_128405_("HeldTicks", 0);
         s.addEntry(entry);
         int previous = victim.f_8941_.m_9290_().m_46392_();
         Held held = new Held(p.m_20148_(), previous, hard);
         HELD.put(victim.m_20148_(), held);
         victim.getPersistentData().m_128405_("gulp_prev_mode", previous);
         victim.m_143403_(GameType.SPECTATOR);
         victim.m_9213_(p);
         victim.m_213846_(Component.m_237113_("You were swallowed by ").m_7220_(p.m_7755_()).m_130946_(s.organ() == 0 ? "" : " (their " + Organ.byId(s.organ()).lower() + ")").m_130946_(hard ? " (HARD mode). The Release key won't work: press your Struggle key (default R) to try to break free before you're digested." : ". Press your Release key (default G) to get out whenever you like, or your Struggle key (default R) to rub the walls."));
         sendHeldState(victim, held, p, 0.0F);
         return true;
      }
   }

   static CompoundTag adopt(ServerPlayer p, ServerPlayer victim, int previousMode) {
      CompoundTag entry = new CompoundTag();
      CompoundTag display = new CompoundTag();
      display.m_128359_("id", "minecraft:player");
      display.m_128350_("Health", victim.m_21223_());
      entry.m_128365_("Entity", display);
      entry.m_128359_("PlayerUuid", victim.m_20148_().toString());
      entry.m_128350_("Size", sizeOf(victim));
      entry.m_128350_("MaxHealth", victim.m_21233_());
      entry.m_128405_("Digest", 0);
      entry.m_128405_("DigestTime", 0);
      entry.m_128379_("HardVore", false);
      Component name = victim.m_7755_();
      entry.m_128359_("Name", name.getString());
      entry.m_128359_("NameJson", Serializer.m_130703_(name));
      entry.m_128405_("Uid", p.m_217043_().m_188502_());
      entry.m_128405_("HeldTicks", 0);
      Held held = new Held(p.m_20148_(), previousMode, false);
      HELD.put(victim.m_20148_(), held);
      victim.getPersistentData().m_128405_("gulp_prev_mode", previousMode);
      victim.m_143403_(GameType.SPECTATOR);
      victim.m_9213_(p);
      sendHeldState(victim, held, p, 0.0F);
      return entry;
   }

   public static boolean releaseEntry(ServerPlayer swallower, CompoundTag entry) {
      UUID id = victimId(entry);
      if (id == null) {
         return true;
      } else {
         Held held = (Held)HELD.remove(id);
         ServerPlayer victim = swallower.m_20194_().m_6846_().m_11259_(id);
         if (victim != null) {
            restore(victim, held, swallower);
         }

         return true;
      }
   }

   public static void tryRelease(ServerPlayer victim) {
      if (VillagerHolding.isHeld(victim)) {
         VillagerHolding.tryRelease(victim);
      } else {
         Held held = (Held)HELD.get(victim.m_20148_());
         if (held != null) {
            if (held.hard) {
               StomachLogic.say(victim, Component.m_237113_("Hard mode: you can't just leave. Struggle to try to break free!"));
            } else {
               escape(victim);
            }
         }
      }
   }

   public static void escape(ServerPlayer victim) {
      if (VillagerHolding.isHeld(victim)) {
         VillagerHolding.release(victim, "You were let out.", false);
      } else {
         Held held = (Held)HELD.get(victim.m_20148_());
         if (held != null) {
            MinecraftServer server = victim.m_20194_();
            ServerPlayer holder = server.m_6846_().m_11259_(held.swallower());
            StomachManager mgr = StomachManager.get(server);
            Stomach hs = mgr.of(held.swallower());
            String id = victim.m_20148_().toString();
            hs.contents.removeIf((e) -> isPlayerEntry(e) && id.equals(e.m_128461_("PlayerUuid")));
            HELD.remove(victim.m_20148_());
            restore(victim, held, holder);
            mgr.m_77762_();
            if (holder != null) {
               StomachLogic.say(holder, Component.m_237119_().m_7220_(victim.m_7755_()).m_130946_(" got out."));
               StomachLogic.sync(holder, hs);
               StomachLogic.sound(holder, GulpSound.SPIT);
            }

         }
      }
   }

   public static void tickHeld(ServerPlayer victim) {
      VillagerHolding.tick(victim);
      Held held = (Held)HELD.get(victim.m_20148_());
      if (held != null) {
         if (held.struggleCd > 0) {
            --held.struggleCd;
         }

         if (held.rubCd > 0) {
            --held.rubCd;
         }

         ServerPlayer holder = victim.m_20194_().m_6846_().m_11259_(held.swallower());
         if (holder != null && holder.m_6084_() && !holder.m_5833_()) {
            if (victim.m_8954_() != holder) {
               victim.m_9213_(holder);
            }

         } else {
            escape(victim);
         }
      }
   }

   public static boolean tickEntry(ServerPlayer swallower, CompoundTag entry) {
      UUID id = victimId(entry);
      Held held = id == null ? null : (Held)HELD.get(id);
      if (held != null && held.swallower().equals(swallower.m_20148_())) {
         int ticks = entry.m_128451_("HeldTicks") + 1;
         entry.m_128405_("HeldTicks", ticks);
         ServerPlayer victim = swallower.m_20194_().m_6846_().m_11259_(id);
         if (held.hard) {
            int d = entry.m_128451_("Digest") + 1;
            entry.m_128405_("Digest", d);
            int total = Math.max(1, entry.m_128451_("DigestTime"));
            if (d >= total) {
               finishDigestion(swallower, entry, victim);
               return false;
            } else {
               if (ticks % 20 == 0 && victim != null) {
                  entry.m_128469_("Entity").m_128350_("Health", victim.m_21223_());
                  sendHeldState(victim, held, swallower, (float)d / (float)total);
               }

               return true;
            }
         } else if (ticks > (Integer)GulpServerConfig.PLAYER_MAX_HOLD_SECONDS.get() * 20) {
            if (victim != null) {
               victim.m_213846_(Component.m_237113_("You were let out."));
            }

            releaseEntry(swallower, entry);
            return false;
         } else {
            if (ticks % 20 == 0 && victim != null) {
               entry.m_128469_("Entity").m_128350_("Health", victim.m_21223_());
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private static void finishDigestion(ServerPlayer swallower, CompoundTag entry, ServerPlayer victim) {
      Stomach s = StomachManager.get(swallower.m_20194_()).of(swallower.m_20148_());
      Component name = victim != null ? victim.m_7755_() : StomachLogic.entryName(entry);
      int organ = Organ.of(entry);
      releaseEntry(swallower, entry);
      if (victim != null) {
         if ((Boolean)GulpServerConfig.PLAYER_DIGESTION_KILLS.get()) {
            victim.m_213846_(Component.m_237113_("You were digested."));
            victim.m_6469_(victim.m_269291_().m_287172_(), Float.MAX_VALUE);
         } else {
            victim.m_213846_(Component.m_237113_("Their stomach is done with you. You were let out, weak and wobbly."));
            victim.m_7292_(new MobEffectInstance(MobEffects.f_19613_, 600, 1));
            victim.m_7292_(new MobEffectInstance(MobEffects.f_19597_, 300, 0));
         }
      }

      OrganLoot.playerDigested(swallower, s, entry, organ);
      StomachLogic.gainXp(swallower, s, 100.0F);
      swallower.m_36324_().m_38707_(4, 0.8F);
      StomachLogic.say(swallower, Component.m_237113_("Finished digesting ").m_7220_(name));
      StomachLogic.sound(swallower, GulpSound.DIGEST);
   }

   private static float escapeChance(float progress) {
      float base = ((Double)GulpServerConfig.PLAYER_STRUGGLE_CHANCE.get()).floatValue();
      return base * (1.0F - 0.5F * Math.max(0.0F, Math.min(1.0F, progress)));
   }

   private static CompoundTag findEntry(Stomach s, UUID victimId) {
      String id = victimId.toString();

      for(CompoundTag e : s.contents) {
         if (isPlayerEntry(e) && id.equals(e.m_128461_("PlayerUuid"))) {
            return e;
         }
      }

      return null;
   }

   private static void sendHeldState(ServerPlayer victim, Held held, ServerPlayer host, float progress) {
      Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> victim), new HeldPacket(held.hard ? 2 : 1, host.m_7755_().getString(), progress, held.hard ? escapeChance(progress) : 0.0F));
   }

   public static void sendSharedStorage(ServerPlayer victim) {
      Held held = (Held)HELD.get(victim.m_20148_());
      if (held != null) {
         if ((Boolean)GulpServerConfig.ITEM_STORAGE_ENABLED.get() && (Boolean)GulpServerConfig.SHARED_STORAGE_VIEW_ENABLED.get()) {
            MinecraftServer server = victim.m_20194_();
            ServerPlayer host = server.m_6846_().m_11259_(held.swallower());
            if (host != null) {
               Stomach hs = StomachManager.get(server).of(held.swallower());
               List<ItemStack> items = new ArrayList();

               for(int i = 0; i < hs.storageSlots(); ++i) {
                  items.add(((ItemStack)hs.storage.get(i)).m_41777_());
               }

               Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> victim), new StorageViewPacket(host.m_7755_().getString(), items));
            }
         } else {
            StomachLogic.say(victim, Component.m_237113_("Looking at their stomach storage is turned off on this server."));
         }
      }
   }

   private static void sendHeldNone(ServerPlayer victim) {
      Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> victim), new HeldPacket(0, "", 0.0F, 0.0F));
   }

   private static Component bracketed(ServerPlayer p, String rest) {
      return Component.m_237113_("[").m_7220_(p.m_7755_()).m_130946_("] " + rest);
   }

   public static void struggle(ServerPlayer victim) {
      if (VillagerHolding.isHeld(victim)) {
         VillagerHolding.rub(victim);
      } else {
         Held held = (Held)HELD.get(victim.m_20148_());
         if (held != null && held.struggleCd <= 0) {
            held.struggleCd = 20;
            MinecraftServer server = victim.m_20194_();
            ServerPlayer host = server.m_6846_().m_11259_(held.swallower());
            if (host != null) {
               CompoundTag entry = findEntry(StomachManager.get(server).of(held.swallower()), victim.m_20148_());
               boolean unbirth = entry != null && Organ.of(entry) == 3;
               if (!held.hard) {
                  wallRub(victim, held, host, unbirth);
               } else {
                  if (unbirth) {
                     StomachLogic.unbirthNausea(victim);
                  }

                  float progress = entry == null ? 0.0F : (float)entry.m_128451_("Digest") / (float)Math.max(1, entry.m_128451_("DigestTime"));
                  float chance = escapeChance(progress);
                  if (victim.m_217043_().m_188501_() * 100.0F < chance) {
                     StomachLogic.say(host, bracketed(victim, "broke free!"));
                     escape(victim);
                  } else {
                     StomachLogic.say(host, bracketed(victim, "squirms in your gut"));
                     StomachLogic.say(victim, Component.m_237113_(String.format("You struggle, but can't break free (%.0f%% chance)", chance)));
                     StomachLogic.sound(host, GulpSound.DIGEST);
                  }

               }
            }
         }
      }
   }

   private static void wallRub(ServerPlayer victim, Held held, ServerPlayer host, boolean unbirth) {
      if (held.rubCd > 0) {
         StomachLogic.say(victim, Component.m_237113_("Give it a moment before you rub again."));
      } else {
         held.rubCd = (Integer)GulpServerConfig.PLAYER_WALL_RUB_COOLDOWN_SECONDS.get() * 20;
         if (unbirth) {
            StomachLogic.unbirthNausea(victim);
         }

         host.m_36324_().m_38707_(2, 0.5F);
         host.m_7292_(new MobEffectInstance(MobEffects.f_19605_, 120, 0));
         host.m_7292_(new MobEffectInstance(MobEffects.f_19596_, 200, 0));
         StomachLogic.say(host, bracketed(victim, "rubs your stomach walls. You feel great."));
         StomachLogic.say(victim, Component.m_237113_("You rub the walls. ").m_7220_(host.m_7755_()).m_130946_(" looks pleased."));
         StomachLogic.sound(host, GulpSound.DIGEST);
      }
   }

   public static void transferPotion(ServerPlayer host, ItemStack potion) {
      List<MobEffectInstance> effects = PotionUtils.m_43547_(potion);
      if (!effects.isEmpty()) {
         Stomach s = StomachManager.get(host.m_20194_()).of(host.m_20148_());

         for(CompoundTag entry : s.contents) {
            if (isPlayerEntry(entry)) {
               UUID id = victimId(entry);
               Held held = id == null ? null : (Held)HELD.get(id);
               ServerPlayer victim = id == null ? null : host.m_20194_().m_6846_().m_11259_(id);
               if (held != null && victim != null) {
                  boolean any = false;

                  for(MobEffectInstance inst : effects) {
                     MobEffect fx = inst.m_19544_();
                     if (fx != MobEffects.f_19602_ && fx != MobEffects.f_19615_ && (fx.m_19483_() != MobEffectCategory.HARMFUL || held.hard)) {
                        if (fx.m_8093_()) {
                           fx.m_19461_(host, host, victim, inst.m_19564_(), (double)1.0F);
                        } else {
                           victim.m_7292_(new MobEffectInstance(inst));
                        }

                        any = true;
                     }
                  }

                  if (any) {
                     victim.m_213846_(Component.m_237119_().m_7220_(host.m_7755_()).m_130946_(" drank a potion. You feel its effects too."));
                  }
               }
            }
         }

      }
   }

   public static void sleepThroughNight(ServerPlayer host) {
      Stomach s = StomachManager.get(host.m_20194_()).of(host.m_20148_());

      for(CompoundTag entry : s.contents) {
         if (isPlayerEntry(entry)) {
            UUID id = victimId(entry);
            ServerPlayer victim = id != null && HELD.containsKey(id) ? host.m_20194_().m_6846_().m_11259_(id) : null;
            if (victim != null) {
               victim.m_7166_(Stats.f_12988_.m_12902_(Stats.f_12992_));
               victim.m_213846_(Component.m_237113_("You slept through the night inside ").m_7220_(host.m_7755_()).m_130946_("'s stomach."));
               StomachLogic.say(host, bracketed(victim, "slept soundly in your gut"));
            }
         }
      }

   }

   public static void releaseAllHeldBy(ServerPlayer swallower) {
      StomachManager mgr = StomachManager.get(swallower.m_20194_());
      Stomach s = mgr.of(swallower.m_20148_());
      boolean any = s.contents.removeIf((e) -> isPlayerEntry(e) && releaseEntry(swallower, e));
      if (any) {
         mgr.m_77762_();
         StomachLogic.sync(swallower, s);
      }

   }

   public static void onLogout(ServerPlayer p) {
      if (isHeld(p)) {
         escape(p);
      }

      releaseAllHeldBy(p);
   }

   public static void onLogin(ServerPlayer p) {
      StomachManager mgr = StomachManager.get(p.m_20194_());
      Stomach s = mgr.of(p.m_20148_());
      s.contents.removeIf((e) -> {
         if (!isPlayerEntry(e)) {
            return false;
         } else {
            UUID id = victimId(e);
            return id == null || !HELD.containsKey(id);
         }
      });
      if (!isHeld(p) && p.getPersistentData().m_128441_("gulp_prev_mode")) {
         restore(p, (Held)null, (ServerPlayer)null);
      }

   }

   public static void onServerStopping(MinecraftServer server) {
      VillagerHolding.releaseAll(server);

      for(UUID id : new ArrayList(HELD.keySet())) {
         ServerPlayer victim = server.m_6846_().m_11259_(id);
         if (victim != null) {
            escape(victim);
         }
      }

      HELD.clear();
   }

   private static UUID victimId(CompoundTag entry) {
      try {
         return UUID.fromString(entry.m_128461_("PlayerUuid"));
      } catch (IllegalArgumentException var2) {
         return null;
      }
   }

   private static void restore(ServerPlayer victim, Held held, ServerPlayer anchor) {
      int modeId = held != null ? held.previousMode() : victim.getPersistentData().m_128451_("gulp_prev_mode");
      GameType mode = GameType.m_46393_(modeId);
      if (mode == GameType.SPECTATOR) {
         mode = GameType.SURVIVAL;
      }

      victim.m_9213_(victim);
      victim.m_143403_(mode);
      victim.getPersistentData().m_128473_("gulp_prev_mode");
      if (anchor != null) {
         ServerLevel w = anchor.m_284548_();
         Vec3 fwd = anchor.m_20252_(1.0F).m_82490_((double)1.5F);
         double x = anchor.m_20185_() + fwd.f_82479_;
         double y = anchor.m_20186_();
         double z = anchor.m_20189_() + fwd.f_82481_;
         AABB box = victim.m_20191_().m_82386_(x - victim.m_20185_(), y - victim.m_20186_(), z - victim.m_20189_());
         if (!w.m_45772_(box)) {
            x = anchor.m_20185_();
            y = anchor.m_20186_();
            z = anchor.m_20189_();
         }

         victim.m_8999_(w, x, y, z, anchor.m_146908_() + 180.0F, 0.0F);
      }

      sendHeldNone(victim);
      victim.m_213846_(Component.m_237113_("You are out."));
   }

   private static final class Held {
      private final UUID swallower;
      private final int previousMode;
      final boolean hard;
      int struggleCd;
      int rubCd;

      Held(UUID swallower, int previousMode, boolean hard) {
         this.swallower = swallower;
         this.previousMode = previousMode;
         this.hard = hard;
      }

      UUID swallower() {
         return this.swallower;
      }

      int previousMode() {
         return this.previousMode;
      }
   }
}
