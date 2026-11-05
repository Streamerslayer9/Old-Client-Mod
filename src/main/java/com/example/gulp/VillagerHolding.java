package com.example.gulp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

public final class VillagerHolding {
   private static final int RUB_COOLDOWN = 400;
   static final int MAX_LEVEL = 10;
   private static final String KEY = "gulp_comfort";
   private static final Map<UUID, Hold> HELD = new HashMap();

   private VillagerHolding() {
   }

   static int levelOf(Entity villager) {
      int level = VillagerData.getCompound(villager, "gulp_comfort").m_128451_("Level");
      return Math.max(1, Math.min(10, level));
   }

   static int xpOf(Entity villager) {
      return VillagerData.getCompound(villager, "gulp_comfort").m_128451_("Xp");
   }

   static int xpNeeded(int level) {
      return 30 + 20 * level;
   }

   static float healPerSecond(int level) {
      return 1.0F + 0.25F * (float)(level - 1);
   }

   static float healPerSecond(int level, VillagerTrait trait) {
      return healPerSecond(level) * trait.healMult;
   }

   static int maxTicks(int level, VillagerTrait trait) {
      return Math.round((float)maxTicks(level) * trait.holdMult);
   }

   static int rankOf(Entity villager, VillagerPerk perk) {
      int rank = VillagerData.getCompound(villager, "gulp_comfort").m_128451_("Perk_" + perk.name());
      return Math.max(0, Math.min(perk.maxRank, rank));
   }

   static int pointsFree(Entity villager) {
      int spent = 0;

      for(VillagerPerk perk : VillagerPerk.values()) {
         spent += rankOf(villager, perk);
      }

      return Math.max(0, levelOf(villager) - 1 - spent);
   }

   static float healPerSecond(int level, VillagerTrait trait, int soothing) {
      return healPerSecond(level, trait) + VillagerPerk.soothingBonus(soothing);
   }

   static int maxTicks(int level, VillagerTrait trait, int embrace) {
      return maxTicks(level, trait) + VillagerPerk.embraceBonusTicks(embrace);
   }

   static int distinctPerks(Entity villager) {
      int n = 0;

      for(VillagerPerk perk : VillagerPerk.values()) {
         if (rankOf(villager, perk) > 0) {
            ++n;
         }
      }

      return n;
   }

   static int capacityOf(Entity villager) {
      return VillagerPerk.capacityFor(levelOf(villager), rankOf(villager, VillagerPerk.ROOMY_BELLY));
   }

   private static int holdingCount(UUID villager) {
      int n = 0;

      for(Hold h : HELD.values()) {
         if (h.villager.equals(villager)) {
            ++n;
         }
      }

      return n;
   }

   static boolean isFull(Entity villager) {
      return holdingCount(villager.m_20148_()) >= capacityOf(villager);
   }

   static void onPreyRide(ServerPlayer p, Entity villager, int heldTicks) {
      if (McaIntegration.typeOf(villager) == 1) {
         int snug = rankOf(villager, VillagerPerk.SNUG_PASSENGER);
         if (snug > 0) {
            McaIntegration.addHearts(villager, p, 3 * snug);
         }

         int gift = rankOf(villager, VillagerPerk.PARTING_GIFT);
         if (gift > 0 && heldTicks >= 600) {
            long now = villager.m_9236_().m_46467_();
            CompoundTag t = VillagerData.getCompound(villager, "gulp_comfort");
            if (!t.m_128441_("GiftAt") || now - t.m_128454_("GiftAt") >= 12000L) {
               t.m_128356_("GiftAt", now);
               VillagerData.put(villager, "gulp_comfort", t);
               int xp = VillagerPerk.partingGiftXp(gift, heldTicks);
               p.m_6756_(xp);
               p.m_213846_(Component.m_237119_().m_7220_(villager.m_7755_()).m_130946_(" thanks you with a parting gift (+" + xp + " XP)."));
            }
         }
      }
   }

   static int cuddleTicks(int level) {
      return (30 + 3 * level) * 20;
   }

   static void buyPerk(ServerPlayer p, int entityId, int perkIndex) {
      Entity v = p.m_9236_().m_6815_(entityId);
      VillagerPerk perk = VillagerPerk.byIndex(perkIndex);
      if (perk != null && v != null && !(p.m_20280_(v) > (double)1024.0F)) {
         if (!McaIntegration.isAdultVillager(v)) {
            p.m_213846_(Component.m_237113_("Only adult villagers have perks."));
         } else if (perk.forType != McaIntegration.typeOf(v)) {
            p.m_213846_(Component.m_237113_("That perk is not for this kind of villager."));
         } else {
            int rank = rankOf(v, perk);
            if (rank < perk.maxRank) {
               if (rank == 0 && distinctPerks(v) >= VillagerPerk.slotsFor(levelOf(v))) {
                  p.m_213846_(Component.m_237113_("No free perk slot. ").m_7220_(v.m_7755_()).m_130946_(" gets another one at a higher level."));
               } else if (pointsFree(v) <= 0) {
                  p.m_213846_(Component.m_237113_("No perk points left. Villagers get one for every level they gain."));
               } else {
                  CompoundTag t = VillagerData.getCompound(v, "gulp_comfort");
                  t.m_128405_("Perk_" + perk.name(), rank + 1);
                  VillagerData.put(v, "gulp_comfort", t);
                  p.m_213846_(Component.m_237119_().m_7220_(v.m_7755_()).m_130946_(" learned " + perk.title + " (rank " + (rank + 1) + ")."));
                  McaIntegration.sendVillagerInfo(p, entityId);
               }
            }
         }
      }
   }

   static int maxTicks(int level) {
      return (60 + 6 * (level - 1)) * 20;
   }

   private static void awardXp(Entity villager, ServerPlayer p, float healed) {
      addXp(villager, p, 10 + Math.round(healed), false);
   }

   static int addXp(Entity villager, ServerPlayer p, int amount, boolean capped) {
      if (amount > 0 && !McaIntegration.isProtectedChild(villager)) {
         int level = levelOf(villager);
         if (level >= 10) {
            return 0;
         } else {
            CompoundTag t = VillagerData.getCompound(villager, "gulp_comfort");
            if (capped) {
               long day = villager.m_9236_().m_46468_() / 24000L;
               int used = t.m_128454_("Day") == day ? t.m_128451_("DayXp") : 0;
               amount = Math.min(amount, (Integer)GulpServerConfig.MCA_VILLAGER_XP_DAILY_CAP.get() - used);
               if (amount <= 0) {
                  return 0;
               }

               t.m_128356_("Day", day);
               t.m_128405_("DayXp", used + amount);
            }

            int xp = xpOf(villager) + amount;

            boolean up;
            for(up = false; level < 10 && xp >= xpNeeded(level); up = true) {
               xp -= xpNeeded(level);
               ++level;
            }

            t.m_128405_("Level", level);
            t.m_128405_("Xp", level >= 10 ? 0 : xp);
            VillagerData.put(villager, "gulp_comfort", t);
            if (up) {
               p.m_213846_(Component.m_237119_().m_7220_(villager.m_7755_()).m_130946_(" levelled up (level " + level + ")."));
            }

            return amount;
         }
      } else {
         return 0;
      }
   }

   public static boolean isHeld(ServerPlayer p) {
      return HELD.containsKey(p.m_20148_());
   }

   public static boolean hold(ServerPlayer p, LivingEntity villager) {
      return hold(p, villager, false);
   }

   public static boolean hold(ServerPlayer p, LivingEntity villager, boolean cuddle) {
      return start(p, villager, cuddle, (ForcedOutcome)null, false);
   }

   static boolean holdHostile(ServerPlayer p, LivingEntity villager) {
      return start(p, villager, true, (ForcedOutcome)null, true);
   }

   static boolean holdForced(ServerPlayer p, LivingEntity villager, ForcedOutcome outcome) {
      return start(p, villager, true, outcome, false);
   }

   private static boolean lethalHostile(ServerPlayer p) {
      return (Boolean)GulpServerConfig.MCA_HOSTILE_KILLS.get() && StomachManager.get(p.m_20194_()).of(p.m_20148_()).role.canBeEaten();
   }

   private static void digestPlayer(ServerPlayer p, LivingEntity v, VillagerTrait trait) {
      Component name = p.m_7755_();
      release(p, "You were digested.", false);
      VillagerStorage.stash(v, p);
      Component line = Component.m_237113_("<").m_7220_(v.m_7755_()).m_130946_("> " + trait.hostileDigestLine());

      for(ServerPlayer o : p.m_20194_().m_6846_().m_11314_()) {
         if (o == p || o.m_9236_() == v.m_9236_() && o.m_20280_(v) <= (double)1024.0F) {
            o.m_213846_(line);
         }
      }

      McaIntegration.remember(v, p.m_20148_(), "gulp.digested");
      p.m_20194_().m_6846_().m_240416_(Component.m_237119_().m_7220_(name).m_130946_(" was digested by ").m_7220_(v.m_7755_()).m_130946_("."), false);
      p.m_6469_(p.m_269291_().m_287172_(), Float.MAX_VALUE);
   }

   private static float hostileChance(Entity villager) {
      float base = ((Double)GulpServerConfig.PLAYER_STRUGGLE_CHANCE.get()).floatValue();
      return Math.max(5.0F, base * (1.0F - 0.03F * (float)(levelOf(villager) - 1)));
   }

   static void resetPerks(Entity villager) {
      CompoundTag t = VillagerData.getCompound(villager, "gulp_comfort");

      for(VillagerPerk perk : VillagerPerk.values()) {
         t.m_128473_("Perk_" + perk.name());
      }

      VillagerData.put(villager, "gulp_comfort", t);
   }

   private static boolean start(ServerPlayer p, LivingEntity villager, boolean cuddle, ForcedOutcome forced, boolean hostile) {
      if (!PlayerHolding.isHeld(p) && p.m_6084_()) {
         if (isFull(villager)) {
            p.m_213846_(Component.m_237113_("<").m_7220_(villager.m_7755_()).m_130946_("> My tummy is full right now, sorry! Give me a moment."));
            return false;
         } else {
            int previous = p.f_8941_.m_9290_().m_46392_();
            Hold hold = new Hold(villager.m_20148_(), previous, cuddle);
            hold.forced = forced;
            hold.hostile = hostile;
            HELD.put(p.m_20148_(), hold);
            if (!hostile && forced == null) {
               McaIntegration.remember(villager, p.m_20148_(), cuddle ? "gulp.cuddled" : "gulp.comforted");
            }

            p.getPersistentData().m_128405_("gulp_prev_mode", previous);
            p.m_143403_(GameType.SPECTATOR);
            p.m_9213_(villager);
            if (hostile) {
               MutableComponent var10001 = Component.m_237119_().m_7220_(villager.m_7755_());
               String var10002 = lethalHostile(p) ? " If you are still in there after " + String.valueOf(GulpServerConfig.MCA_HOSTILE_HOLD_SECONDS.get()) + " seconds, they will digest you and keep everything you carry!" : " They let go after a while.";
               p.m_213846_(var10001.m_130946_(" grabs you and swallows you whole! They won't just let you out: press your Struggle key (default R) to try to break free." + var10002));
            }

            String intro = forced != null ? (forced.rewards() ? "You climb into " : "You force your way into ") : null;
            if (!hostile) {
               if (intro != null) {
                  p.m_213846_(Component.m_237113_(intro).m_7220_(villager.m_7755_()).m_130946_("'s tummy." + (forced.holdSeconds() > 0 ? " They won't put up with it for long." : "") + " Press your Release key (default G) to get out whenever you like."));
               } else {
                  p.m_213846_(Component.m_237119_().m_7220_(villager.m_7755_()).m_130946_((cuddle ? " pulls you into a cosy cuddle in their tummy." : " is holding you in their tummy to help you heal.") + " Press your Release key (default G) to get out whenever you like, or your Struggle key (default R) to rub the walls."));
               }
            }

            Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new HeldPacket(hostile ? 3 : 1, villager.m_7755_().getString(), hostile && lethalHostile(p) ? 1.0F : 0.0F, hostile ? hostileChance(villager) : 0.0F));
            MutableComponent var8 = Component.m_237113_("<").m_7220_(villager.m_7755_());
            String var9 = hostile ? McaIntegration.traitOf(villager).hostileGrabLine() : (forced != null ? forced.enterLine() : (cuddle ? McaIntegration.traitOf(villager).cuddleHoldLine() : McaIntegration.traitOf(villager).holdLine()));
            p.m_213846_(var8.m_130946_("> " + var9));
            return true;
         }
      } else {
         return false;
      }
   }

   static void tick(ServerPlayer p) {
      Hold h = (Hold)HELD.get(p.m_20148_());
      if (h != null) {
         if (h.rubCd > 0) {
            --h.rubCd;
         }

         Entity e = p.m_284548_().m_8791_(h.villager);
         if (e == null && h.nestedIn != null) {
            nestedTick(p, h);
         } else {
            ++h.ticks;
            if (e instanceof LivingEntity) {
               LivingEntity v = (LivingEntity)e;
               if (v.m_6084_() && v.f_20916_ <= 0) {
                  if (h.nestedIn != null) {
                     h.nestedIn = null;
                     p.m_213846_(Component.m_237119_().m_7220_(v.m_7755_()).m_130946_(" is out in the world again, and still has you in their tummy."));
                     MutableComponent var10001 = Component.m_237113_("<").m_7220_(v.m_7755_());
                     String var10002 = h.cuddle ? McaIntegration.traitOf(v).cuddleHoldLine() : McaIntegration.traitOf(v).holdLine();
                     p.m_213846_(var10001.m_130946_("> " + var10002));
                  }

                  if (p.m_8954_() != v) {
                     p.m_9213_(v);
                  }

                  int level = levelOf(v);
                  VillagerTrait trait = McaIntegration.traitOf(v);
                  float scale = h.hostile ? 0.0F : (h.forced == null ? 1.0F : h.forced.healScale());
                  if (h.ticks % 20 == 0 && scale > 0.0F) {
                     float before = p.m_21223_();
                     p.m_5634_(healPerSecond(level, trait, rankOf(v, VillagerPerk.SOOTHING)) * scale);
                     h.healed += Math.max(0.0F, p.m_21223_() - before);
                     if (level >= 5 && scale >= 1.0F) {
                        p.m_36324_().m_38707_(1, 0.2F);
                     }
                  }

                  int warm = rankOf(v, VillagerPerk.WARM_BELLY);
                  if (warm > 0 && scale >= 1.0F && h.ticks % 40 == 0) {
                     p.m_36324_().m_38707_(warm, 0.1F * (float)warm);
                  }

                  if (h.ticks % 400 == 300) {
                     String idle = h.hostile ? trait.hostileIdleLine() : (h.forced != null ? h.forced.idleLine() : trait.holdIdleLine());
                     p.m_213846_(Component.m_237113_("<").m_7220_(v.m_7755_()).m_130946_("> " + idle));
                  }

                  int cuddleLimit = h.forced != null && h.forced.holdSeconds() > 0 ? h.forced.holdSeconds() * 20 : cuddleTicks(level);
                  if (h.hostile) {
                     int limit = (Integer)GulpServerConfig.MCA_HOSTILE_HOLD_SECONDS.get() * 20;
                     if (h.ticks >= limit) {
                        if (lethalHostile(p)) {
                           digestPlayer(p, v, trait);
                        } else {
                           p.m_213846_(Component.m_237113_("<").m_7220_(v.m_7755_()).m_130946_("> " + trait.hostileEndLine()));
                           release(p, "They finally let you out.", false);
                        }
                     } else if ((h.ticks == limit / 2 || h.ticks == limit - 200) && lethalHostile(p)) {
                        p.m_213846_(Component.m_237113_("<").m_7220_(v.m_7755_()).m_130946_("> " + trait.hostileWarnLine()));
                        p.m_213846_(Component.m_237113_("Time is running out: break free (Struggle key) or they will digest you!").m_130940_(ChatFormatting.RED));
                     }
                  } else if (!h.cuddle && p.m_21223_() >= p.m_21233_()) {
                     release(p, "All better. You were let out.", true);
                  } else if (h.cuddle && h.ticks >= cuddleLimit) {
                     release(p, "The cuddle is over. You were let out.", true);
                  } else if (h.ticks >= maxTicks(level, trait, rankOf(v, VillagerPerk.EMBRACE))) {
                     release(p, "You were let out.", true);
                  }

                  return;
               }
            }

            release(p, "The villager had to let you out.", false);
         }
      }
   }

   static void tryRelease(ServerPlayer p) {
      Hold h = (Hold)HELD.get(p.m_20148_());
      if (h != null && h.hostile && h.nestedIn == null) {
         StomachLogic.say(p, Component.m_237113_("They won't let you out. Struggle to try to break free!"));
         Entity e = p.m_284548_().m_8791_(h.villager);
         if (e != null) {
            p.m_213846_(Component.m_237113_("<").m_7220_(e.m_7755_()).m_130946_("> " + McaIntegration.traitOf(e).hostileHoldRefuseLine()));
         }

      } else {
         release(p, "You climbed out.", false);
      }
   }

   static void rub(ServerPlayer p) {
      Hold h = (Hold)HELD.get(p.m_20148_());
      if (h != null) {
         if (h.rubCd > 0) {
            StomachLogic.say(p, Component.m_237113_(h.hostile ? "Catch your breath before you struggle again." : "Give it a moment before you rub again."));
         } else {
            Entity e = p.m_284548_().m_8791_(h.villager);
            if (h.hostile && h.nestedIn == null && e != null) {
               h.rubCd = 20;
               float chance = hostileChance(e);
               VillagerTrait trait = McaIntegration.traitOf(e);
               if (p.m_217043_().m_188501_() * 100.0F < chance) {
                  release(p, "You broke free!", false);
                  p.m_213846_(Component.m_237113_("<").m_7220_(e.m_7755_()).m_130946_("> " + trait.hostileEscapeLine()));
               } else {
                  StomachLogic.say(p, Component.m_237113_(String.format("You struggle, but can't break free (%.0f%% chance)", chance)));
                  if (p.m_217043_().m_188503_(3) == 0) {
                     p.m_213846_(Component.m_237113_("<").m_7220_(e.m_7755_()).m_130946_("> " + trait.hostileStruggleLine()));
                  }
               }

            } else {
               h.rubCd = 400;
               if (h.forced != null && !h.forced.rewards()) {
                  if (e != null) {
                     McaIntegration.addHearts(e, p, -2);
                  }

                  StomachLogic.say(p, Component.m_237113_("You squirm around in there. They really don't like that."));
               } else {
                  if (e != null) {
                     McaIntegration.addHearts(e, p, 2);
                  }

                  StomachLogic.say(p, Component.m_237113_("You rub the walls gently. They seem pleased."));
               }
            }
         }
      }
   }

   static void release(ServerPlayer p, String message, boolean thanked) {
      Hold h = (Hold)HELD.remove(p.m_20148_());
      if (h != null) {
         if (!thanked) {
            McaIntegration.pauseComfort(p, (Integer)GulpServerConfig.MCA_COMFORT_DECLINE_PAUSE_SECONDS.get() * 20);
         }

         Entity villager = p.m_284548_().m_8791_(h.villager);
         GameType mode = GameType.m_46393_(h.previousMode);
         if (mode == GameType.SPECTATOR) {
            mode = GameType.SURVIVAL;
         }

         p.m_9213_(p);
         p.m_143403_(mode);
         p.getPersistentData().m_128473_("gulp_prev_mode");
         if (h.nestedIn != null) {
            leaveNest(p, h);
         }

         if (villager != null) {
            Level var7 = villager.m_9236_();
            if (var7 instanceof ServerLevel) {
               ServerLevel w = (ServerLevel)var7;
               Vec3 fwd = villager.m_20252_(1.0F).m_82490_((double)1.5F);
               double x = villager.m_20185_() + fwd.f_82479_;
               double y = villager.m_20186_();
               double z = villager.m_20189_() + fwd.f_82481_;
               AABB box = p.m_20191_().m_82386_(x - p.m_20185_(), y - p.m_20186_(), z - p.m_20189_());
               if (!w.m_45772_(box)) {
                  x = villager.m_20185_();
                  y = villager.m_20186_();
                  z = villager.m_20189_();
               }

               p.m_8999_(w, x, y, z, villager.m_146908_() + 180.0F, 0.0F);
               if (thanked && (h.forced == null || h.forced.rewards())) {
                  McaIntegration.addHearts(villager, p, h.cuddle ? 4 : 2);
                  if (h.cuddle) {
                     addXp(villager, p, 6, true);
                  } else {
                     awardXp(villager, p, h.healed);
                  }

                  int wind = h.cuddle ? 0 : rankOf(villager, VillagerPerk.SECOND_WIND);
                  if (wind > 0) {
                     p.m_7292_(new MobEffectInstance(MobEffects.f_19605_, VillagerPerk.secondWindTicks(wind), 0));
                     p.m_213846_(Component.m_237119_().m_7220_(villager.m_7755_()).m_130946_(" gives you a second wind."));
                  }
               }
            }
         }

         Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new HeldPacket(0, "", 0.0F, 0.0F));
         p.m_213846_(Component.m_237113_(message));
         if (villager != null && message.equals("You climbed out.")) {
            VillagerTrait trait = McaIntegration.traitOf(villager);
            MutableComponent var19 = Component.m_237113_("<").m_7220_(villager.m_7755_());
            String var20 = h.forced != null ? h.forced.exitLine() : trait.earlyExitLine();
            p.m_213846_(var19.m_130946_("> " + var20));
         } else if (villager != null && thanked) {
            VillagerTrait trait = McaIntegration.traitOf(villager);
            MutableComponent var10001 = Component.m_237113_("<").m_7220_(villager.m_7755_());
            String var10002 = h.forced != null ? h.forced.endLine() : (h.cuddle ? trait.cuddleFinishLine() : trait.finishLine());
            p.m_213846_(var10001.m_130946_("> " + var10002));
         }

      }
   }

   static List<ServerPlayer> heldBy(Entity villager, MinecraftServer server) {
      List<ServerPlayer> list = new ArrayList();
      if (server == null) {
         return list;
      } else {
         for(Map.Entry<UUID, Hold> en : HELD.entrySet()) {
            Hold h = (Hold)en.getValue();
            if (h.nestedIn == null && h.villager.equals(villager.m_20148_())) {
               ServerPlayer sp = server.m_6846_().m_11259_((UUID)en.getKey());
               if (sp != null && sp.m_6084_()) {
                  list.add(sp);
               }
            }
         }

         return list;
      }
   }

   static void nest(ServerPlayer a, LivingEntity villager, ServerPlayer b, float size) {
      Hold h = (Hold)HELD.get(a.m_20148_());
      if (h != null) {
         h.nestedIn = b.m_20148_();
         h.nestedSize = size;
         h.playerName = a.m_7755_().getString();
         h.cLevel = levelOf(villager);
         h.cTrait = McaIntegration.traitOf(villager);
         h.cSoothing = rankOf(villager, VillagerPerk.SOOTHING);
         h.cEmbrace = rankOf(villager, VillagerPerk.EMBRACE);
         h.cWarm = rankOf(villager, VillagerPerk.WARM_BELLY);
         a.m_9213_(b);
         a.m_213846_(Component.m_237119_().m_7220_(villager.m_7755_()).m_130946_(" was swallowed by ").m_7220_(b.m_7755_()).m_130946_(", with you still inside them! The healing is paused until they are let out. Press your Release key (default G) to climb out of both."));
         a.m_213846_(Component.m_237113_("<").m_7220_(villager.m_7755_()).m_130946_("> " + McaIntegration.traitOf(villager).swallowedWhileHoldingLine()));
      }
   }

   static void transferNested(ServerPlayer b, CompoundTag entry) {
      MinecraftServer server = b.m_20194_();
      UUID villagerId = entityUuid(entry);
      ListTag list = entry.m_128437_("NestedPlayers", 8);
      String villagerName = entry.m_128461_("Name");

      for(int i = 0; i < list.size(); ++i) {
         UUID u;
         try {
            u = UUID.fromString(list.m_128778_(i));
         } catch (IllegalArgumentException var10) {
            continue;
         }

         Hold h = (Hold)HELD.get(u);
         ServerPlayer a = server == null ? null : server.m_6846_().m_11259_(u);
         if (h != null && a != null && a.m_6084_() && h.nestedIn != null && h.nestedIn.equals(b.m_20148_()) && villagerId != null && h.villager.equals(villagerId)) {
            HELD.remove(u);
            Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> a), new HeldPacket(0, "", 0.0F, 0.0F));
            StomachLogic.PENDING_ADD.add(PlayerHolding.adopt(b, a, h.previousMode));
            a.m_213846_(Component.m_237113_(villagerName + " was digested, and you slipped into " + b.m_7755_().getString() + "'s stomach. Press your Release key (default G) to get out whenever you like."));
            b.m_213846_(Component.m_237113_(a.m_7755_().getString() + " slipped out of the digested villager and into your stomach."));
         }
      }

      entry.m_128473_("NestedPlayers");
      entry.m_128473_("NestedExtra");
   }

   private static UUID entityUuid(CompoundTag entry) {
      CompoundTag ent = entry.m_128469_("Entity");
      return ent.m_128403_("UUID") ? ent.m_128342_("UUID") : null;
   }

   private static List<String> nestedNames(CompoundTag entry) {
      List<String> names = new ArrayList();
      ListTag list = entry.m_128437_("NestedPlayers", 8);
      if (list.isEmpty()) {
         return names;
      } else {
         UUID villager = entityUuid(entry);

         for(int i = 0; i < list.size(); ++i) {
            try {
               Hold h = (Hold)HELD.get(UUID.fromString(list.m_128778_(i)));
               if (h != null && h.nestedIn != null && h.villager.equals(villager)) {
                  names.add(h.playerName);
               }
            } catch (IllegalArgumentException var6) {
            }
         }

         return names;
      }
   }

   static boolean hasNested(CompoundTag entry) {
      return !nestedNames(entry).isEmpty();
   }

   static void clearNested(CompoundTag entry) {
      entry.m_128350_("Size", Math.max(0.05F, entry.m_128457_("Size") - entry.m_128457_("NestedExtra")));
      entry.m_128473_("NestedPlayers");
      entry.m_128473_("NestedExtra");
   }

   static String nestedLabel(CompoundTag entry) {
      List<String> names = nestedNames(entry);
      if (names.isEmpty()) {
         return "";
      } else {
         String var10000 = String.join(", ", names);
         return " (" + var10000 + (names.size() == 1 ? " player)" : " players)");
      }
   }

   private static boolean villagerStillInside(MinecraftServer server, ServerPlayer b, Hold h) {
      for(CompoundTag e : StomachManager.get(server).of(b.m_20148_()).contents) {
         if (h.villager.equals(entityUuid(e))) {
            return true;
         }
      }

      return false;
   }

   private static void nestedTick(ServerPlayer p, Hold h) {
      MinecraftServer server = p.m_20194_();
      ServerPlayer b = server == null ? null : server.m_6846_().m_11259_(h.nestedIn);
      if (b != null && b.m_6084_() && !b.m_5833_() && villagerStillInside(server, b, h)) {
         if (b.m_9236_() != p.m_9236_()) {
            p.m_8999_(b.m_284548_(), b.m_20185_(), b.m_20186_(), b.m_20189_(), b.m_146908_(), 0.0F);
         }

         if (p.m_8954_() != b) {
            p.m_9213_(b);
         }

      } else {
         release(p, "You were let out.", false);
      }
   }

   private static void leaveNest(ServerPlayer p, Hold h) {
      MinecraftServer server = p.m_20194_();
      if (server != null && h.nestedIn != null) {
         StomachManager mgr = StomachManager.get(server);
         Stomach s = mgr.of(h.nestedIn);
         boolean changed = false;

         for(CompoundTag entry : s.contents) {
            ListTag list = entry.m_128437_("NestedPlayers", 8);

            for(int i = 0; i < list.size(); ++i) {
               if (list.m_128778_(i).equals(p.m_20148_().toString())) {
                  list.remove(i);
                  entry.m_128350_("Size", Math.max(0.05F, entry.m_128457_("Size") - h.nestedSize));
                  entry.m_128350_("NestedExtra", Math.max(0.0F, entry.m_128457_("NestedExtra") - h.nestedSize));
                  changed = true;
                  break;
               }
            }
         }

         ServerPlayer b = server.m_6846_().m_11259_(h.nestedIn);
         if (changed) {
            mgr.m_77762_();
            if (b != null) {
               b.m_213846_(Component.m_237113_(p.m_7755_().getString() + " climbed out of the villager inside you."));
               StomachLogic.sync(b, s);
            }
         }

         if (b != null) {
            p.m_8999_(b.m_284548_(), b.m_20185_(), b.m_20186_(), b.m_20189_(), b.m_146908_(), 0.0F);
         }

      }
   }

   static void onVillagerSwallowed(Entity villager, ServerPlayer swallower) {
      List<UUID> inside = new ArrayList();

      for(Map.Entry<UUID, Hold> en : HELD.entrySet()) {
         if (((Hold)en.getValue()).villager.equals(villager.m_20148_())) {
            inside.add((UUID)en.getKey());
         }
      }

      if (!inside.isEmpty() && swallower.m_20194_() != null) {
         String line = McaIntegration.traitOf(villager).swallowedWhileHoldingLine();
         String villagerName = villager.m_7755_().getString();

         for(UUID id : inside) {
            ServerPlayer held = swallower.m_20194_().m_6846_().m_11259_(id);
            if (held == null) {
               HELD.remove(id);
            } else {
               release(held, villagerName + " was swallowed by " + swallower.m_7755_().getString() + " with you inside! You tumble out, safe and sound.", false);
               held.m_213846_(Component.m_237113_("<" + villagerName + "> " + line));
               swallower.m_213846_(Component.m_237113_("You swallowed " + villagerName + ", and " + held.m_7755_().getString() + " tumbles out of their tummy beside you!"));
            }
         }

         swallower.m_213846_(Component.m_237113_("<" + villagerName + "> " + line));
      }
   }

   static void releaseAll(MinecraftServer server) {
      for(UUID id : new ArrayList(HELD.keySet())) {
         ServerPlayer p = server.m_6846_().m_11259_(id);
         if (p != null) {
            release(p, "You were let out.", false);
         }
      }

      HELD.clear();
   }

   private static final class Hold {
      final UUID villager;
      final int previousMode;
      final boolean cuddle;
      boolean hostile;
      ForcedOutcome forced;
      int ticks;
      int rubCd;
      float healed;
      UUID nestedIn;
      float nestedSize;
      String playerName = "";
      int cLevel = 1;
      VillagerTrait cTrait;
      int cSoothing;
      int cEmbrace;
      int cWarm;

      Hold(UUID villager, int previousMode, boolean cuddle) {
         this.cTrait = VillagerTrait.NONE;
         this.villager = villager;
         this.previousMode = previousMode;
         this.cuddle = cuddle;
      }
   }
}
