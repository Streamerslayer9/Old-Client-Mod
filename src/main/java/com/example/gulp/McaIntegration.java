package com.example.gulp;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

public final class McaIntegration {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String KIND_KEY = "gulp_kind";
   private static final int WITNESS_RANGE = 16;
   private static final int MAX_WITNESSES = 12;
   private static final int MAX_PREDATOR = 200;
   private static Boolean modLoaded;
   private static boolean broken;
   private static Class<?> villagerClass;
   private static Method getVillagerBrain;
   private static Method getMemoriesForPlayer;
   private static Method getHearts;
   private static Method rewardHearts;
   private static Method getPersonality;
   private static Method isGuard;
   private static final List<Scare> SCARED = new ArrayList();
   private static final Map<UUID, Long> LAST_REFUSAL_MESSAGE = new HashMap();
   private static int problemsLogged;
   private static final Map<UUID, Long> HOSTILE_PLAYER_COOLDOWN = new HashMap();
   private static final Map<UUID, Long> HOSTILE_VILLAGER_COOLDOWN = new HashMap();
   private static final Map<UUID, Long> FORCE_COOLDOWN = new HashMap();
   private static final Map<Class<?>, Optional<Method>> AGE_METHODS = new ConcurrentHashMap();
   private static final String MEMORY_TAG = "GulpMemory";
   private static final Map<Class<?>, Optional<Method>> MEMORY_GETTERS = new ConcurrentHashMap();
   private static boolean memoryProblemLogged;
   private static final String[] FAN_LINES = new String[]{"Whoa, it's so warm and cozy in here!", "Best ride in the whole village!", "Don't let me out yet, this is fun.", "I can hear your heartbeat. It's kind of soothing."};
   private static final String[] NEUTRAL_LINES = new String[]{"It's... surprisingly comfortable in here.", "I trust you, but please don't take too long.", "It's warm. Strange, but I don't mind.", "Thanks for keeping me safe in here."};
   private static final String[] FEARFUL_LINES = new String[]{"I'm still nervous, but I know you'd never hurt me.", "It's okay. I'm okay. I trust you.", "Can we go back out soon? ...but thank you for being gentle."};
   private static final String REGULAR_LINE = "I'm getting used to this, you know. You carry me often.";
   private static final Map<UUID, Offer> OFFERS = new HashMap();
   private static final Map<UUID, Long> VILLAGER_COOLDOWN = new HashMap();
   private static final Map<UUID, Long> PLAYER_COOLDOWN = new HashMap();
   private static final Map<UUID, Offer> COMFORT_OFFERS = new HashMap();
   private static final Map<UUID, Long> COMFORT_PLAYER_COOLDOWN = new HashMap();
   private static final Map<UUID, Long> SNUB_COOLDOWN = new HashMap();
   private static final Map<UUID, Offer> CUDDLE_OFFERS = new HashMap();
   private static final Map<UUID, Long> CUDDLE_VILLAGER_COOLDOWN = new HashMap();
   private static final Map<UUID, Long> CUDDLE_PLAYER_COOLDOWN = new HashMap();

   private McaIntegration() {
   }

   public static boolean active() {
      if (!(Boolean)GulpServerConfig.MCA_ENABLED.get()) {
         return false;
      } else {
         if (modLoaded == null) {
            modLoaded = ModList.get().isLoaded("mca");
         }

         return modLoaded && !broken && ensureReflection();
      }
   }

   public static Component status() {
      if (!(Boolean)GulpServerConfig.MCA_ENABLED.get()) {
         return Component.m_237113_("[Gulp] MCA: reactions are turned off in the server config.").m_130940_(ChatFormatting.GRAY);
      } else {
         if (modLoaded == null) {
            modLoaded = ModList.get().isLoaded("mca");
         }

         if (!modLoaded) {
            return Component.m_237113_("[Gulp] MCA: not found (Minecraft Comes Alive isn't installed).").m_130940_(ChatFormatting.GRAY);
         } else {
            return active() ? Component.m_237113_("[Gulp] MCA connected: villager reactions are on.").m_130940_(ChatFormatting.GREEN) : Component.m_237113_("[Gulp] MCA found, but it didn't match what I expected, so reactions are off (see latest.log).").m_130940_(ChatFormatting.RED);
         }
      }
   }

   static Class<?> mcaClass(String name) throws ClassNotFoundException {
      String moved = name.replaceFirst("^net\\.mca\\.", "net.conczin.mca.");
      String[] candidates = new String[]{name, "forge." + name, moved, "forge." + moved};
      ClassNotFoundException last = null;

      for(String candidate : candidates) {
         try {
            return Class.forName(candidate);
         } catch (ClassNotFoundException e) {
            last = e;
         }
      }

      throw last;
   }

   private static boolean ensureReflection() {
      if (villagerClass != null) {
         return true;
      } else {
         try {
            villagerClass = mcaClass("net.mca.entity.VillagerEntityMCA");
            getVillagerBrain = villagerClass.getMethod("getVillagerBrain");
            Class<?> brainClass = mcaClass("net.mca.entity.ai.brain.VillagerBrain");
            getMemoriesForPlayer = method(brainClass, "getMemoriesForPlayer", 1);
            rewardHearts = method(brainClass, "rewardHearts", 2);
            getPersonality = method(brainClass, "getPersonality", 0);
            getHearts = method(mcaClass("net.mca.entity.ai.Memories"), "getHearts", 0);

            try {
               isGuard = method(villagerClass, "isGuard", 0);
            } catch (NoSuchMethodException var2) {
               isGuard = null;
            }

            LOGGER.info("Vore Mod: MCA found, villager reactions are ready.");
            return true;
         } catch (Throwable t) {
            broken = true;
            villagerClass = null;
            LOGGER.warn("Vore Mod: MCA is installed but its code isn't what I expected, so the MCA reactions are switched off.", t);
            return false;
         }
      }
   }

   private static void problem(String what, Throwable t) {
      if (problemsLogged++ < 5) {
         LOGGER.warn("Vore Mod: MCA reaction problem: {}", what, t);
      }

   }

   private static Method method(Class<?> c, String name, int parameters) throws NoSuchMethodException {
      for(Method m : c.getMethods()) {
         if (m.getName().equals(name) && m.getParameterCount() == parameters) {
            return m;
         }
      }

      String var10002 = c.getName();
      throw new NoSuchMethodException(var10002 + "." + name);
   }

   public static boolean isVillager(Entity e) {
      return e != null && villagerClass != null && villagerClass.isInstance(e);
   }

   static boolean isAdultVillager(Entity e) {
      boolean var10000;
      if (active() && isVillager(e) && e instanceof LivingEntity le) {
         if (le.m_6084_() && !isProtectedChild(e)) {
            var10000 = true;
            return var10000;
         }
      }

      var10000 = false;
      return var10000;
   }

   static int heartsOf(Entity villager, Player player) {
      return heartsWith(villager, player);
   }

   private static Object brainOf(Entity villager) throws ReflectiveOperationException {
      return getVillagerBrain.invoke(villager);
   }

   private static int heartsWith(Entity villager, Player player) {
      try {
         Object memories = getMemoriesForPlayer.invoke(brainOf(villager), player);
         return (Integer)getHearts.invoke(memories);
      } catch (Throwable var3) {
         return 0;
      }
   }

   private static String personalityOf(Entity villager) {
      try {
         Object personality = getPersonality.invoke(brainOf(villager));
         String var10000;
         if (personality instanceof Enum<?> e) {
            var10000 = e.name();
         } else {
            var10000 = "";
         }

         return var10000;
      } catch (Throwable t) {
         problem("couldn't read a villager's personality", t);
         return "";
      }
   }

   private static VoreType voreTypeOf(Entity villager) {
      if (VillagerData.getFlag(villager, "gulp_kind", "Pred")) {
         return McaIntegration.VoreType.PRED;
      } else {
         VoreType var10000;
         switch (Math.floorMod(villager.m_20148_().hashCode() * 31 + 7, 4)) {
            case 0 -> var10000 = McaIntegration.VoreType.PRED;
            case 1 -> var10000 = McaIntegration.VoreType.PREY;
            default -> var10000 = McaIntegration.VoreType.NORMAL;
         }

         return var10000;
      }
   }

   static VillagerTrait traitOf(Entity villager) {
      if (voreTypeOf(villager) == McaIntegration.VoreType.NORMAL) {
         return VillagerTrait.NONE;
      } else {
         VillagerTrait var10000;
         switch (attitude(villager)) {
            case FAN -> var10000 = VillagerTrait.CONFIDENT;
            case FEARFUL -> var10000 = VillagerTrait.SHY;
            case NEUTRAL -> var10000 = (villager.m_20148_().getLeastSignificantBits() >>> 20 & 1L) == 0L ? VillagerTrait.CONFIDENT : VillagerTrait.SHY;
            default -> throw new IncompatibleClassChangeError();
         }

         return var10000;
      }
   }

   static int typeOf(Entity villager) {
      byte var10000;
      switch (voreTypeOf(villager)) {
         case NORMAL -> var10000 = 0;
         case PREY -> var10000 = 1;
         case PRED -> var10000 = 2;
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   static boolean isPredVillager(Entity villager) {
      return voreTypeOf(villager) == McaIntegration.VoreType.PRED;
   }

   private static String voreKind(Entity villager) {
      VillagerTrait trait = traitOf(villager);
      if (trait != VillagerTrait.NONE) {
         String var4 = voreTypeOf(villager).name().toLowerCase(Locale.ROOT);
         return var4 + ", " + trait.label.toLowerCase(Locale.ROOT);
      } else {
         String var10000;
         switch (attitude(villager)) {
            case FAN -> var10000 = "confident";
            case FEARFUL -> var10000 = "shy";
            case NEUTRAL -> var10000 = "easygoing";
            default -> throw new IncompatibleClassChangeError();
         }

         String temper = var10000;
         var10000 = voreTypeOf(villager).name().toLowerCase(Locale.ROOT);
         return var10000 + ", " + temper;
      }
   }

   private static boolean guard(Entity villager) {
      try {
         return isGuard != null && (Boolean)isGuard.invoke(villager);
      } catch (Throwable var2) {
         return false;
      }
   }

   private static Attitude attitude(Entity villager) {
      Attitude var10000;
      switch (personalityOf(villager)) {
         case "PEPPY":
         case "CONFIDENT":
         case "ATHLETIC":
         case "ODD":
            var10000 = McaIntegration.Attitude.FAN;
            break;
         case "SENSITIVE":
         case "SHY":
         case "GLOOMY":
         case "GRUMPY":
            var10000 = McaIntegration.Attitude.FEARFUL;
            break;
         default:
            var10000 = McaIntegration.Attitude.NEUTRAL;
      }

      return var10000;
   }

   private static Attitude witnessAttitude(Entity villager) {
      if (voreTypeOf(villager) == McaIntegration.VoreType.NORMAL) {
         return attitude(villager);
      } else {
         return traitOf(villager) == VillagerTrait.SHY ? McaIntegration.Attitude.FEARFUL : McaIntegration.Attitude.FAN;
      }
   }

   static void stampKind(Entity villager, ServerPlayer p) {
      VoreType type = voreTypeOf(villager);
      if (type != McaIntegration.VoreType.NORMAL) {
         String var10000 = type.name().toLowerCase(Locale.ROOT);
         String id = "gulp.kind." + var10000 + "." + (traitOf(villager) == VillagerTrait.SHY ? "shy" : "confident");
         if (memoryCount(villager, p.m_20148_(), id) == 0) {
            remember(villager, p.m_20148_(), id);
         }

      }
   }

   public static void tickHostile(MinecraftServer server) {
      if ((Boolean)GulpServerConfig.MCA_HOSTILE_ENABLED.get() && active()) {
         long now = (long)server.m_129921_();
         HOSTILE_PLAYER_COOLDOWN.values().removeIf((t) -> t <= now);
         HOSTILE_VILLAGER_COOLDOWN.values().removeIf((t) -> t <= now);
         StomachManager mgr = StomachManager.get(server);

         for(ServerPlayer p : server.m_6846_().m_11314_()) {
            if (p.m_6084_() && !p.m_7500_() && !p.m_5833_() && !PlayerHolding.isHeld(p) && !HOSTILE_PLAYER_COOLDOWN.containsKey(p.m_20148_()) && mgr.of(p.m_20148_()).role.canBeEaten()) {
               for(Entity e : p.m_9236_().m_6249_(p, p.m_20191_().m_82400_((double)5.0F), McaIntegration::isVillager)) {
                  if (e instanceof LivingEntity) {
                     LivingEntity le = (LivingEntity)e;
                     if (le.m_6084_() && !isProtectedChild(le) && isAdultVillager(le) && voreTypeOf(le) == McaIntegration.VoreType.PRED && !HOSTILE_VILLAGER_COOLDOWN.containsKey(le.m_20148_()) && !VillagerHolding.isFull(le)) {
                        VillagerTrait trait = traitOf(le);
                        int low = (Integer)GulpServerConfig.MCA_LOW_AFFECTION_HEARTS.get();
                        if (heartsWith(le, p) <= (trait == VillagerTrait.SHY ? low * 2 : low) && !(p.m_217043_().m_188501_() > (trait == VillagerTrait.SHY ? 0.25F : 0.5F)) && VillagerHolding.holdHostile(p, le)) {
                           long until = now + (long)(Integer)GulpServerConfig.MCA_HOSTILE_COOLDOWN.get() * 20L;
                           HOSTILE_PLAYER_COOLDOWN.put(p.m_20148_(), until);
                           HOSTILE_VILLAGER_COOLDOWN.put(le.m_20148_(), until);
                           remember(le, p.m_20148_(), "gulp.hostile");
                           break;
                        }
                     }
                  }
               }
            }
         }

      }
   }

   static VillagerTrait temperOf(Entity villager) {
      if (voreTypeOf(villager) != McaIntegration.VoreType.NORMAL) {
         return traitOf(villager);
      } else {
         VillagerTrait var10000;
         switch (attitude(villager)) {
            case FAN -> var10000 = VillagerTrait.CONFIDENT;
            case FEARFUL -> var10000 = VillagerTrait.SHY;
            case NEUTRAL -> var10000 = VillagerTrait.NONE;
            default -> throw new IncompatibleClassChangeError();
         }

         return var10000;
      }
   }

   private static LivingEntity nearbyAdult(ServerPlayer p, int entityId) {
      if (!active()) {
         return null;
      } else {
         Entity e = p.m_9236_().m_6815_(entityId);
         if (e instanceof LivingEntity) {
            LivingEntity le = (LivingEntity)e;
            if (isVillager(e) && le.m_6084_() && !(p.m_20280_(e) > (double)36.0F)) {
               if (isAdultVillager(e) && !isProtectedChild(e)) {
                  return le;
               }

               StomachLogic.say(p, Component.m_237113_("Only adult villagers can do this."));
               return null;
            }
         }

         StomachLogic.say(p, Component.m_237113_("Get closer to the villager first."));
         return null;
      }
   }

   static void forceSwallow(ServerPlayer p, int entityId) {
      if (!(Boolean)GulpServerConfig.MCA_FORCE_SWALLOW_ENABLED.get()) {
         StomachLogic.say(p, Component.m_237113_("Climbing into villagers is turned off on this server."));
      } else {
         LivingEntity le = nearbyAdult(p, entityId);
         if (le != null) {
            if (!PlayerHolding.isHeld(p) && !VillagerHolding.isHeld(p) && !p.m_5833_()) {
               if (!StomachManager.get(p.m_20194_()).of(p.m_20148_()).role.canBeEaten()) {
                  StomachLogic.say(p, Component.m_237113_("Your role can't be swallowed. Set it to Prey or Both in Settings first."));
               } else {
                  long now = (long)p.m_20194_().m_129921_();
                  Long until = (Long)FORCE_COOLDOWN.get(p.m_20148_());
                  if (until != null && until > now) {
                     long var10001 = until - now + 19L;
                     StomachLogic.say(p, Component.m_237113_("Give it a rest: you can climb in again in " + var10001 / 20L + " seconds."));
                  } else {
                     VillagerTrait temper = temperOf(le);
                     ForcedOutcome o = ForcedOutcome.of(typeOf(le), temper);
                     if (VillagerHolding.holdForced(p, le, o)) {
                        FORCE_COOLDOWN.put(p.m_20148_(), now + (long)(Integer)GulpServerConfig.MCA_FORCE_SWALLOW_COOLDOWN.get() * 20L);
                        int hearts = o.hearts();
                        if (hearts < 0) {
                           int have = heartsWith(le, p);
                           if (have >= (Integer)GulpServerConfig.MCA_AFFECTION_HEARTS.get()) {
                              hearts = Math.round((float)hearts * 0.5F);
                           } else if (have <= (Integer)GulpServerConfig.MCA_LOW_AFFECTION_HEARTS.get()) {
                              hearts = Math.round((float)hearts * 1.5F);
                           }
                        }

                        reward(le, p, hearts);
                        remember(le, p.m_20148_(), "gulp.forced");
                        if (o.scare()) {
                           scare(le, p);
                        }

                     }
                  }
               }
            }
         }
      }
   }

   static void askConvert(ServerPlayer p, int entityId) {
      if (!(Boolean)GulpServerConfig.MCA_CONVERT_ENABLED.get()) {
         StomachLogic.say(p, Component.m_237113_("Asking villagers to become preds is turned off on this server."));
      } else {
         LivingEntity le = nearbyAdult(p, entityId);
         if (le != null) {
            VillagerTrait temper = temperOf(le);
            if (voreTypeOf(le) == McaIntegration.VoreType.PRED) {
               p.m_213846_(Component.m_237113_("<").m_7220_(le.m_7755_()).m_130946_("> " + temper.convertAlreadyLine()));
            } else {
               int need = (int)Math.round((double)(Integer)GulpServerConfig.MCA_CONVERT_HEARTS.get() * temper.heartsMult);
               int have = heartsWith(le, p);
               if (have < need) {
                  p.m_213846_(Component.m_237113_("<").m_7220_(le.m_7755_()).m_130946_("> " + temper.convertRefuseLine()));
                  p.m_213846_(Component.m_237113_("They need " + need + " hearts with you to agree. You have " + have + ".").m_130940_(ChatFormatting.GRAY));
               } else {
                  VillagerHolding.resetPerks(le);
                  CompoundTag kind = new CompoundTag();
                  kind.m_128379_("Pred", true);
                  VillagerData.put(le, "gulp_kind", kind);
                  remember(le, p.m_20148_(), "gulp.converted");
                  reward(le, p, 5);
                  p.m_213846_(Component.m_237113_("<").m_7220_(le.m_7755_()).m_130946_("> " + temper.convertAcceptLine()));
                  p.m_213846_(Component.m_237119_().m_7220_(le.m_7755_()).m_130946_(" is a pred villager now! Their perk points were given back.").m_130940_(ChatFormatting.GOLD));
                  p.m_213846_(Component.m_237113_("<").m_7220_(le.m_7755_()).m_130946_("> " + temper.convertAfterLine()));
                  p.m_6330_(SoundEvents.f_12504_, SoundSource.NEUTRAL, 1.0F, 1.0F);
                  Level var8 = le.m_9236_();
                  if (var8 instanceof ServerLevel) {
                     ServerLevel w = (ServerLevel)var8;
                     w.m_8767_(ParticleTypes.f_123748_, le.m_20185_(), le.m_20186_() + (double)1.0F, le.m_20189_(), 12, 0.4, (double)0.5F, 0.4, (double)0.0F);
                  }

                  sendVillagerInfo(p, entityId);
               }
            }
         }
      }
   }

   private static void reward(Entity villager, ServerPlayer player, int hearts) {
      int scaled = Math.round((float)hearts * ((Double)GulpServerConfig.MCA_REACTION_MULTIPLIER.get()).floatValue());
      if (scaled != 0) {
         try {
            rewardHearts.invoke(brainOf(villager), player, scaled);
            LOGGER.info("Vore Mod: villager {} hearts {} for {}", new Object[]{villager.m_7755_().getString(), (scaled > 0 ? "+" : "") + scaled, player.m_7755_().getString()});
         } catch (Throwable t) {
            problem("couldn't change a villager's hearts", t);
         }

      }
   }

   private static void addPredator(ServerPlayer p, Stomach s, int amount) {
      if (amount > 0) {
         int lock = (Integer)GulpServerConfig.MCA_PREDATOR_LOCK.get();
         int before = s.predator;
         s.predator = Math.min(200, s.predator + amount);
         if (before < lock && s.predator >= lock) {
            StomachLogic.say(p, Component.m_237113_("Villagers are afraid of you now."));
         }

      }
   }

   private static void scare(Entity villager, ServerPlayer player) {
      if ((Boolean)GulpServerConfig.MCA_PANIC.get() && villager instanceof LivingEntity le) {
         if (!guard(villager)) {
            SCARED.add(new Scare(le, player.m_20148_(), le.m_9236_().m_46467_() + 160L));
            return;
         }
      }

   }

   private static void reactNearby(ServerPlayer p, Entity subject, boolean hard) {
      int count = 0;

      for(Entity w : p.m_9236_().m_6249_(p, p.m_20191_().m_82400_((double)16.0F), (e) -> e != subject && isVillager(e))) {
         ++count;
         if (count > 12) {
            break;
         }

         Attitude a = witnessAttitude(w);
         VoreType kind = voreTypeOf(w);
         remember(w, p.m_20148_(), "gulp.witnessed.swallow");
         int hearts = hard ? (a == McaIntegration.Attitude.FAN ? -3 : (a == McaIntegration.Attitude.FEARFUL ? -15 : -10)) : (a == McaIntegration.Attitude.FAN ? 5 : (a == McaIntegration.Attitude.FEARFUL ? -8 : -2));
         if (kind == McaIntegration.VoreType.PRED) {
            hearts += hard ? 4 : 2;
         } else if (kind == McaIntegration.VoreType.PREY) {
            hearts += hard ? -4 : 3;
         }

         reward(w, p, hearts);
         boolean calmPred = kind == McaIntegration.VoreType.PRED && a == McaIntegration.Attitude.FAN;
         if (hard && !calmPred || a == McaIntegration.Attitude.FEARFUL) {
            scare(w, p);
         }
      }

   }

   public static void onSwallow(ServerPlayer p, Stomach s, LivingEntity swallowed) {
      onSwallow(p, s, swallowed, false);
   }

   public static void onSwallow(ServerPlayer p, Stomach s, LivingEntity swallowed, boolean invited) {
      boolean on = active();
      if (on && isVillager(swallowed)) {
         boolean hard = s.isHard(s.organ()) && (Boolean)GulpServerConfig.HARD_MODE_ENABLED.get() && !invited;
         Attitude a = attitude(swallowed);
         if (invited) {
            remember(swallowed, p.m_20148_(), "gulp.invited");
         }

         remember(swallowed, p.m_20148_(), "gulp.swallowed");
         remember(swallowed, p.m_20148_(), hard ? "gulp.swallowed.hard" : "gulp.swallowed.soft");
         LOGGER.info("Vore Mod: MCA villager {} swallowed (personality {}, attitude {}, {} mode)", new Object[]{swallowed.m_7755_().getString(), personalityOf(swallowed), a, hard ? "hard" : "soft"});
         int hearts = invited ? (a == McaIntegration.Attitude.FAN ? 15 : 8) : (hard ? (a == McaIntegration.Attitude.FAN ? -10 : (a == McaIntegration.Attitude.FEARFUL ? -50 : -40)) : (a == McaIntegration.Attitude.FAN ? 15 : (a == McaIntegration.Attitude.FEARFUL ? -25 : -10)));
         reward(swallowed, p, hearts);
         int predator = invited ? 0 : (hard ? (a == McaIntegration.Attitude.FAN ? 8 : (a == McaIntegration.Attitude.FEARFUL ? 20 : 15)) : (a == McaIntegration.Attitude.FAN ? 0 : (a == McaIntegration.Attitude.FEARFUL ? 5 : 3)));
         addPredator(p, s, predator);
         reactNearby(p, swallowed, hard);
      } else {
         if (swallowed.m_6095_().toString().contains("villager")) {
            LOGGER.info("Vore Mod: swallowed {} ({}) but no MCA reaction: MCA reactions {}, is MCA villager: {}", new Object[]{swallowed.m_7755_().getString(), swallowed.getClass().getName(), on ? "on" : "off (config off, MCA missing, or MCA didn't match)", isVillager(swallowed)});
         }

      }
   }

   public static void onRelease(ServerPlayer p, Stomach s, Entity released, int heldTicks) {
      if (active() && isVillager(released)) {
         if (!s.isHard(s.organ())) {
            VillagerXp.onRide(p, released, heldTicks);
         }

         remember(released, p.m_20148_(), "gulp.released");
         if (heldTicks >= 1200) {
            remember(released, p.m_20148_(), "gulp.long_ride");
         }

         if (!s.isHard(s.organ())) {
            VillagerHolding.onPreyRide(p, released, heldTicks);
         }

         Attitude a = attitude(released);
         if (a == McaIntegration.Attitude.FAN) {
            reward(released, p, Math.min(25, 5 + heldTicks / 200));
         } else if (!s.isHard(s.organ())) {
            reward(released, p, a == McaIntegration.Attitude.FEARFUL ? 3 : 5);
         }

      }
   }

   public static void onDigest(ServerPlayer p, Stomach s, LivingEntity digested) {
      if (active() && isVillager(digested)) {
         Attitude a = attitude(digested);
         addPredator(p, s, a == McaIntegration.Attitude.FAN ? 10 : (a == McaIntegration.Attitude.FEARFUL ? 35 : 25));

         for(Entity w : p.m_9236_().m_6249_(p, p.m_20191_().m_82400_((double)16.0F), (e) -> e != digested && isVillager(e))) {
            if (voreTypeOf(w) != McaIntegration.VoreType.PRED || witnessAttitude(w) != McaIntegration.Attitude.FAN) {
               scare(w, p);
            }

            remember(w, p.m_20148_(), "gulp.witnessed.digest");
         }

      }
   }

   public static Component refusal(ServerPlayer p, Entity target) {
      if (active() && (Boolean)GulpServerConfig.MCA_LOCKOUT_ENABLED.get() && isVillager(target)) {
         Stomach s = StomachManager.get(p.m_20194_()).of(p.m_20148_());
         if (s.predator <= 0) {
            return null;
         } else if (s.predator >= (Integer)GulpServerConfig.MCA_PREDATOR_LOCK.get() && attitude(target) != McaIntegration.Attitude.FAN) {
            return Component.m_237119_().m_7220_(target.m_7755_()).m_130946_(" is too afraid of you to talk.");
         } else {
            return heartsWith(target, p) <= (Integer)GulpServerConfig.MCA_HEARTS_LOCK.get() ? Component.m_237119_().m_7220_(target.m_7755_()).m_130946_(" wants nothing to do with you right now.") : null;
         }
      } else {
         return null;
      }
   }

   public static boolean shouldTell(ServerPlayer p) {
      long now = System.currentTimeMillis();
      Long last = (Long)LAST_REFUSAL_MESSAGE.put(p.m_20148_(), now);
      return last == null || now - last > 1500L;
   }

   public static void tickScared(MinecraftServer server) {
      if (!SCARED.isEmpty()) {
         Iterator<Scare> it = SCARED.iterator();

         while(it.hasNext()) {
            Scare s = (Scare)it.next();
            LivingEntity v = s.villager();
            if (!v.m_213877_() && v.m_6084_() && v.m_9236_().m_46467_() <= s.until()) {
               try {
                  ServerPlayer p = server.m_6846_().m_11259_(s.player());
                  Brain<?> brain = v.m_6274_();
                  if (p != null) {
                     brain.m_21882_(MemoryModuleType.f_26382_, p, 40L);
                  }

                  brain.m_21889_(Activity.f_37984_);
               } catch (Throwable var6) {
                  it.remove();
               }
            } else {
               it.remove();
            }
         }

      }
   }

   public static boolean isProtectedChild(Entity e) {
      if (e instanceof LivingEntity le) {
         if (le instanceof AbstractVillager && le.m_6162_()) {
            return true;
         } else {
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(le.m_6095_());
            if (key != null && key.m_135827_().equals("mca")) {
               try {
                  Optional<Method> getter = (Optional)AGE_METHODS.computeIfAbsent(le.getClass(), (c) -> {
                     try {
                        return Optional.of(c.getMethod("getAgeState"));
                     } catch (NoSuchMethodException var2) {
                        return Optional.empty();
                     }
                  });
                  if (getter.isPresent()) {
                     Object var5 = ((Method)getter.get()).invoke(le);
                     if (var5 instanceof Enum) {
                        Enum<?> age = (Enum)var5;
                        switch (age.name()) {
                           case "BABY":
                           case "TODDLER":
                           case "CHILD":
                           case "TEEN":
                              return true;
                           case "ADULT":
                              return false;
                        }
                     }
                  }
               } catch (Throwable t) {
                  problem("couldn't read a villager's age", t);
               }

               return le.m_6162_();
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public static boolean isChildEntry(CompoundTag entry, ServerLevel level) {
      if (entry.m_128471_("ChildChecked")) {
         return entry.m_128471_("ChildEntry");
      } else {
         CompoundTag saved = entry.m_128469_("Entity");
         String id = saved.m_128461_("id");
         boolean child = false;
         if (id.startsWith("mca:") || id.equals("minecraft:villager") || id.equals("minecraft:wandering_trader")) {
            child = (Boolean)EntityType.m_20642_(saved, level).map(McaIntegration::isProtectedChild).orElse(false);
         }

         entry.m_128379_("ChildChecked", true);
         entry.m_128379_("ChildEntry", child);
         return child;
      }
   }

   static void remember(Entity villager, UUID player, String id) {
      if ((Boolean)GulpServerConfig.MCA_MEMORY_ENABLED.get()) {
         CompoundTag all = VillagerData.getCompound(villager, "GulpMemory");
         CompoundTag mine = all.m_128469_(player.toString());
         mine.m_128405_(id, mine.m_128451_(id) + 1);
         mine.m_128356_(id + ".last", villager.m_9236_().m_46467_());
         all.m_128365_(player.toString(), mine);
         VillagerData.put(villager, "GulpMemory", all);

         try {
            Optional<Method> getter = (Optional)MEMORY_GETTERS.computeIfAbsent(villager.getClass(), (c) -> {
               for(Method m : c.getMethods()) {
                  if (m.getParameterCount() == 0 && m.getReturnType().getSimpleName().equals("LongTermMemory")) {
                     return Optional.of(m);
                  }
               }

               return Optional.empty();
            });
            if (getter.isEmpty()) {
               throw new NoSuchMethodException("no LongTermMemory getter on " + villager.getClass().getName());
            }

            Object memory = ((Method)getter.get()).invoke(villager);
            Method remember = null;

            for(Method m : memory.getClass().getMethods()) {
               if (m.getName().equals("remember") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class) {
                  remember = m;
               }
            }

            if (remember == null) {
               throw new NoSuchMethodException("no remember(String) on " + memory.getClass().getName());
            }

            remember.invoke(memory, id + "." + String.valueOf(player));
         } catch (Throwable t) {
            if (!memoryProblemLogged) {
               memoryProblemLogged = true;
               LOGGER.warn("Vore Mod: couldn't write to MCA's long-term memory, so villagers only remember in the mod's own data. (MCA: Conversations won't see these memories.)", t);
            }
         }

         int times = mine.m_128451_(id);
         if (id.equals("gulp.swallowed") && times == 3) {
            remember(villager, player, "gulp.swallowed.many");
         } else if (id.equals("gulp.swallowed") && times == 10) {
            remember(villager, player, "gulp.swallowed.lots");
         } else if (id.equals("gulp.invited") && times == 3) {
            remember(villager, player, "gulp.invited.many");
         }

      }
   }

   private static int memoryCount(Entity villager, UUID player, String id) {
      return VillagerData.getCompound(villager, "GulpMemory").m_128469_(player.toString()).m_128451_(id);
   }

   static void tagEntry(CompoundTag entry, Entity villager, ServerPlayer p) {
      if (active() && isVillager(villager)) {
         CompoundTag info = new CompoundTag();
         info.m_128359_("Attitude", attitude(villager).name());
         info.m_128405_("Hearts", heartsWith(villager, p));
         info.m_128405_("Times", memoryCount(villager, p.m_20148_(), "gulp.swallowed"));
         entry.m_128365_("McaInfo", info);
         entry.m_128405_("CommentIn", 600 + p.m_217043_().m_188503_(600));
      }
   }

   public static void tickInside(ServerPlayer p, CompoundTag entry, boolean hardNow) {
      CompoundTag info = entry.m_128469_("McaInfo");
      if (!info.m_128456_() && (Boolean)GulpServerConfig.MCA_AFFECTION_ENABLED.get()) {
         if (!hardNow || entry.m_128471_("Safe")) {
            if (info.m_128451_("Hearts") >= (Integer)GulpServerConfig.MCA_AFFECTION_HEARTS.get()) {
               int left = entry.m_128451_("CommentIn") - 1;
               if (left > 0) {
                  entry.m_128405_("CommentIn", left);
               } else {
                  entry.m_128405_("CommentIn", 1200 + p.m_217043_().m_188503_(1200));
                  String[] var10000;
                  switch (info.m_128461_("Attitude")) {
                     case "FAN" -> var10000 = FAN_LINES;
                     case "FEARFUL" -> var10000 = FEARFUL_LINES;
                     default -> var10000 = NEUTRAL_LINES;
                  }

                  String[] pool = var10000;
                  line = info.m_128451_("Times") >= 3 && p.m_217043_().m_188503_(4) == 0 ? "I'm getting used to this, you know. You carry me often." : pool[p.m_217043_().m_188503_(pool.length)];
                  p.m_213846_(Component.m_237113_("<").m_7220_(StomachLogic.entryName(entry)).m_130946_("> " + line));
               }
            }
         }
      }
   }

   public static void tickAffection(MinecraftServer server) {
      if ((Boolean)GulpServerConfig.MCA_AFFECTION_ENABLED.get() && active()) {
         long now = (long)server.m_129921_();
         OFFERS.values().removeIf((o) -> o.expires() <= now);
         VILLAGER_COOLDOWN.values().removeIf((t) -> t <= now);
         PLAYER_COOLDOWN.values().removeIf((t) -> t <= now);
         StomachManager mgr = StomachManager.get(server);

         for(ServerPlayer p : server.m_6846_().m_11314_()) {
            if (!p.m_5833_() && !PlayerHolding.isHeld(p) && !OFFERS.containsKey(p.m_20148_()) && !PLAYER_COOLDOWN.containsKey(p.m_20148_())) {
               Stomach s = mgr.of(p.m_20148_());
               if (s.role.canEat() && s.usesGulpSwallow() && (!s.isHard(s.organ()) || !(Boolean)GulpServerConfig.HARD_MODE_ENABLED.get())) {
                  for(Entity e : p.m_9236_().m_6249_(p, p.m_20191_().m_82400_((double)6.0F), McaIntegration::isVillager)) {
                     if (e instanceof LivingEntity) {
                        LivingEntity le = (LivingEntity)e;
                        if (le.m_6084_() && !isProtectedChild(le) && !VILLAGER_COOLDOWN.containsKey(le.m_20148_())) {
                           Attitude a = attitude(le);
                           VoreType type = voreTypeOf(le);
                           if (type != McaIntegration.VoreType.PRED) {
                              if (type == McaIntegration.VoreType.PREY) {
                                 a = traitOf(le) == VillagerTrait.SHY ? McaIntegration.Attitude.FEARFUL : McaIntegration.Attitude.FAN;
                              }

                              if (a != McaIntegration.Attitude.FEARFUL || type == McaIntegration.VoreType.PREY) {
                                 int eager = VillagerHolding.rankOf(le, VillagerPerk.EAGER_RIDER);
                                 int needHearts = Math.round((float)((Integer)GulpServerConfig.MCA_AFFECTION_HEARTS.get() * (a == McaIntegration.Attitude.FEARFUL ? 2 : 1)) * (1.0F - 0.1F * (float)eager));
                                 if (heartsWith(le, p) >= needHearts && !(p.m_217043_().m_188501_() > (a == McaIntegration.Attitude.FAN ? 0.5F : (a == McaIntegration.Attitude.FEARFUL ? 0.1F : 0.25F)) * (1.0F + 0.35F * (float)eager))) {
                                    float size = Math.max(0.05F, le.m_20205_() * le.m_20205_() * le.m_20206_());
                                    if (!((double)size > s.capacityOf(s.organ())) && !(s.usedIn(s.organ()) + (double)size > s.capacityOf(s.organ())) && s.contents.size() < 100) {
                                       makeOffer(p, le, a, memoryCount(le, p.m_20148_(), "gulp.swallowed") > 0, now);
                                       break;
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

      }
   }

   private static void makeOffer(ServerPlayer p, LivingEntity villager, Attitude a, boolean again, long now) {
      OFFERS.put(p.m_20148_(), new Offer(villager.m_20148_(), now + 600L));
      VILLAGER_COOLDOWN.put(villager.m_20148_(), now + (long)(Integer)GulpServerConfig.MCA_REQUEST_COOLDOWN_SECONDS.get() * 20L);
      PLAYER_COOLDOWN.put(p.m_20148_(), now + 1200L);
      String text = a == McaIntegration.Attitude.FAN ? (again ? "Hey! Could you carry me in your tummy again? That was so much fun!" : "Hey! Could you carry me in your tummy for a bit? I'd love to try it!") : (again ? "I trust you... would you carry me safely in your stomach again?" : "I trust you... would you carry me safely in your stomach for a little while?");
      MutableComponent accept = Component.m_237113_("[Accept]").m_130948_(Style.f_131099_.m_131140_(ChatFormatting.GREEN).m_131136_(true).m_131142_(new ClickEvent(Action.RUN_COMMAND, "/gulp carry accept")).m_131144_(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.f_130831_, Component.m_237113_("Carry them safely (soft mode)"))));
      MutableComponent decline = Component.m_237113_("[Decline]").m_130948_(Style.f_131099_.m_131140_(ChatFormatting.RED).m_131136_(true).m_131142_(new ClickEvent(Action.RUN_COMMAND, "/gulp carry decline")).m_131144_(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.f_130831_, Component.m_237113_("Politely say no"))));
      p.m_213846_(Component.m_237113_("<").m_7220_(villager.m_7755_()).m_130946_("> " + text + " ").m_7220_(accept).m_130946_(" ").m_7220_(decline));
      p.m_6330_(SoundEvents.f_12509_, SoundSource.NEUTRAL, 1.0F, 1.0F);
   }

   public static void respondCarry(ServerPlayer p, boolean accept) {
      Offer offer = (Offer)OFFERS.remove(p.m_20148_());
      long now = (long)p.m_20194_().m_129921_();
      if (offer != null && offer.expires() > now) {
         Entity e = p.m_284548_().m_8791_(offer.villager());
         if (e instanceof LivingEntity) {
            LivingEntity le = (LivingEntity)e;
            if (le.m_6084_() && !(p.m_20280_(le) > (double)64.0F)) {
               if (!accept) {
                  p.m_213846_(Component.m_237113_("<").m_7220_(le.m_7755_()).m_130946_("> That's okay, maybe another time."));
                  return;
               }

               Stomach s = StomachManager.get(p.m_20194_()).of(p.m_20148_());
               if (!s.usesGulpSwallow()) {
                  StomachLogic.say(p, StomachLogic.notGulpSwallowMessage(s));
                  return;
               }

               if (s.role.canEat() && (!s.isHard(s.organ()) || !(Boolean)GulpServerConfig.HARD_MODE_ENABLED.get())) {
                  if (StomachLogic.swallowLiving(p, s, le, true)) {
                     StomachManager.get(p.m_20194_()).m_77762_();
                     StomachLogic.sync(p, s);
                  }

                  return;
               }

               StomachLogic.say(p, Component.m_237113_("Switch to soft mode (and a role that can swallow) first. Rides by invitation are always safe."));
               return;
            }
         }

         p.m_213846_(Component.m_237113_("They've wandered off."));
      } else {
         p.m_213846_(Component.m_237113_("Nobody is waiting for an answer."));
      }
   }

   static void sendVillagerInfo(ServerPlayer p, int entityId) {
      if (active()) {
         Entity e = p.m_9236_().m_6815_(entityId);
         if (e != null && isVillager(e) && !(p.m_20280_(e) > (double)1024.0F)) {
            byte var10000;
            switch (voreTypeOf(e)) {
               case NORMAL -> var10000 = 0;
               case PREY -> var10000 = 1;
               case PRED -> var10000 = 2;
               default -> throw new IncompatibleClassChangeError();
            }

            int type = var10000;
            int level = VillagerHolding.levelOf(e);
            int trait = traitOf(e).ordinal();
            int[] ranks = new int[VillagerPerk.values().length];

            for(VillagerPerk perk : VillagerPerk.values()) {
               ranks[perk.ordinal()] = VillagerHolding.rankOf(e, perk);
            }

            int points = VillagerHolding.pointsFree(e);
            PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> p);
            Net.CHANNEL.send(target, new VillagerInfoPacket(entityId, isAdultVillager(e), type, trait, ranks, points, level, VillagerHolding.xpOf(e), VillagerHolding.xpNeeded(level)));
         }
      }
   }

   private static String comfortNote(Entity villager) {
      return voreTypeOf(villager) == McaIntegration.VoreType.PRED ? " | comfort lv " + VillagerHolding.levelOf(villager) : "";
   }

   static void addHearts(Entity villager, ServerPlayer player, int hearts) {
      if (active() && isVillager(villager)) {
         reward(villager, player, hearts);
      }

   }

   public static void tickComfort(MinecraftServer server) {
      if ((Boolean)GulpServerConfig.MCA_COMFORT_ENABLED.get() && active()) {
         long now = (long)server.m_129921_();
         COMFORT_OFFERS.values().removeIf((o) -> o.expires() <= now);
         COMFORT_PLAYER_COOLDOWN.values().removeIf((t) -> t <= now);
         StomachManager mgr = StomachManager.get(server);

         for(ServerPlayer p : server.m_6846_().m_11314_()) {
            if (p.m_6084_() && !p.m_5833_() && !PlayerHolding.isHeld(p) && !COMFORT_OFFERS.containsKey(p.m_20148_()) && !COMFORT_PLAYER_COOLDOWN.containsKey(p.m_20148_()) && !(p.m_21223_() > p.m_21233_() * VillagerTrait.CONFIDENT.comfortHpFraction) && mgr.of(p.m_20148_()).role.canBeEaten()) {
               for(Entity e : p.m_9236_().m_6249_(p, p.m_20191_().m_82400_((double)6.0F), McaIntegration::isVillager)) {
                  if (e instanceof LivingEntity) {
                     LivingEntity le = (LivingEntity)e;
                     if (le.m_6084_() && !isProtectedChild(le) && voreTypeOf(le) == McaIntegration.VoreType.PRED && !VillagerHolding.isFull(le)) {
                        VillagerTrait trait = traitOf(le);
                        if (!(p.m_21223_() > p.m_21233_() * trait.comfortHpFraction)) {
                           if (heartsWith(le, p) > (Integer)GulpServerConfig.MCA_LOW_AFFECTION_HEARTS.get()) {
                              makeComfortOffer(p, le, trait, now);
                              break;
                           }

                           if (snub(p, le, trait, now)) {
                              break;
                           }
                        }
                     }
                  }
               }
            }
         }

      }
   }

   private static boolean snub(ServerPlayer p, LivingEntity villager, VillagerTrait trait, long now) {
      SNUB_COOLDOWN.values().removeIf((t) -> t <= now);
      if (SNUB_COOLDOWN.containsKey(villager.m_20148_())) {
         return false;
      } else {
         SNUB_COOLDOWN.put(villager.m_20148_(), now + 3600L);
         p.m_213846_(Component.m_237113_("<").m_7220_(villager.m_7755_()).m_130946_("> " + trait.snubLine()));
         return true;
      }
   }

   static void pauseComfort(ServerPlayer p, int ticks) {
      if (ticks > 0 && p.m_20194_() != null) {
         COMFORT_PLAYER_COOLDOWN.put(p.m_20148_(), (long)p.m_20194_().m_129921_() + (long)ticks);
      }
   }

   private static void makeComfortOffer(ServerPlayer p, LivingEntity villager, VillagerTrait trait, long now) {
      COMFORT_OFFERS.put(p.m_20148_(), new Offer(villager.m_20148_(), now + 600L));
      String text = trait.comfortOfferLine();
      MutableComponent accept = Component.m_237113_("[Accept]").m_130948_(Style.f_131099_.m_131140_(ChatFormatting.GREEN).m_131136_(true).m_131142_(new ClickEvent(Action.RUN_COMMAND, "/gulp comfort accept")).m_131144_(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.f_130831_, Component.m_237113_("Rest safely until you're healed. You can climb out any time."))));
      MutableComponent decline = Component.m_237113_("[Decline]").m_130948_(Style.f_131099_.m_131140_(ChatFormatting.RED).m_131136_(true).m_131142_(new ClickEvent(Action.RUN_COMMAND, "/gulp comfort decline")).m_131144_(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.f_130831_, Component.m_237113_("No thank you"))));
      p.m_213846_(Component.m_237113_("<").m_7220_(villager.m_7755_()).m_130946_("> " + text + " ").m_7220_(accept).m_130946_(" ").m_7220_(decline));
      p.m_6330_(SoundEvents.f_12509_, SoundSource.NEUTRAL, 1.0F, 1.0F);
   }

   public static void respondComfort(ServerPlayer p, boolean accept) {
      Offer offer = (Offer)COMFORT_OFFERS.remove(p.m_20148_());
      long now = (long)p.m_20194_().m_129921_();
      if (offer != null && offer.expires() > now) {
         Entity e = p.m_284548_().m_8791_(offer.villager());
         if (e instanceof LivingEntity) {
            LivingEntity le = (LivingEntity)e;
            if (le.m_6084_() && !(p.m_20280_(le) > (double)64.0F)) {
               if (!accept) {
                  pauseComfort(p, (Integer)GulpServerConfig.MCA_COMFORT_DECLINE_PAUSE_SECONDS.get() * 20);
                  p.m_213846_(Component.m_237113_("<").m_7220_(le.m_7755_()).m_130946_("> Of course. I'm here if you change your mind."));
                  return;
               }

               if (!PlayerHolding.isHeld(p) && !p.m_5833_()) {
                  if (!StomachManager.get(p.m_20194_()).of(p.m_20148_()).role.canBeEaten()) {
                     StomachLogic.say(p, Component.m_237113_("Your role can't be swallowed. Set it to Prey or Both in Settings first."));
                     return;
                  }

                  VillagerHolding.hold(p, le);
                  return;
               }

               return;
            }
         }

         p.m_213846_(Component.m_237113_("They've wandered off."));
      } else {
         p.m_213846_(Component.m_237113_("Nobody is waiting for an answer."));
      }
   }

   public static void tickCuddle(MinecraftServer server) {
      if ((Boolean)GulpServerConfig.MCA_CUDDLE_ENABLED.get() && active()) {
         long now = (long)server.m_129921_();
         CUDDLE_OFFERS.values().removeIf((o) -> o.expires() <= now);
         CUDDLE_VILLAGER_COOLDOWN.values().removeIf((t) -> t <= now);
         CUDDLE_PLAYER_COOLDOWN.values().removeIf((t) -> t <= now);
         StomachManager mgr = StomachManager.get(server);

         for(ServerPlayer p : server.m_6846_().m_11314_()) {
            UUID id = p.m_20148_();
            if (p.m_6084_() && !p.m_5833_() && !PlayerHolding.isHeld(p) && !CUDDLE_OFFERS.containsKey(id) && !COMFORT_OFFERS.containsKey(id) && !OFFERS.containsKey(id) && !CUDDLE_PLAYER_COOLDOWN.containsKey(id) && !(p.m_21223_() <= p.m_21233_() * VillagerTrait.CONFIDENT.comfortHpFraction) && mgr.of(id).role.canBeEaten()) {
               for(Entity e : p.m_9236_().m_6249_(p, p.m_20191_().m_82400_((double)6.0F), McaIntegration::isVillager)) {
                  if (e instanceof LivingEntity) {
                     LivingEntity le = (LivingEntity)e;
                     if (le.m_6084_() && !isProtectedChild(le) && voreTypeOf(le) == McaIntegration.VoreType.PRED && !CUDDLE_VILLAGER_COOLDOWN.containsKey(le.m_20148_()) && !VillagerHolding.isFull(le) && memoryCount(le, id, "gulp.witnessed.digest") <= 0) {
                        VillagerTrait trait = traitOf(le);
                        boolean history = memoryCount(le, id, "gulp.comforted") + memoryCount(le, id, "gulp.cuddled") + memoryCount(le, id, "gulp.swallowed") > 0;
                        long need = Math.round((double)(Integer)GulpServerConfig.MCA_AFFECTION_HEARTS.get() * trait.heartsMult * (history ? (double)1.0F : (double)1.5F));
                        if ((long)heartsWith(le, p) >= need && !(p.m_217043_().m_188501_() > (trait == VillagerTrait.CONFIDENT ? 0.3F : 0.1F))) {
                           makeCuddleOffer(p, le, trait, now);
                           break;
                        }
                     }
                  }
               }
            }
         }

      }
   }

   private static void makeCuddleOffer(ServerPlayer p, LivingEntity villager, VillagerTrait trait, long now) {
      CUDDLE_OFFERS.put(p.m_20148_(), new Offer(villager.m_20148_(), now + 600L));
      CUDDLE_VILLAGER_COOLDOWN.put(villager.m_20148_(), now + Math.round((double)((long)(Integer)GulpServerConfig.MCA_REQUEST_COOLDOWN_SECONDS.get() * 20L) * trait.cooldownMult * (double)2.0F));
      CUDDLE_PLAYER_COOLDOWN.put(p.m_20148_(), now + 12000L);
      MutableComponent accept = Component.m_237113_("[Accept]").m_130948_(Style.f_131099_.m_131140_(ChatFormatting.GREEN).m_131136_(true).m_131142_(new ClickEvent(Action.RUN_COMMAND, "/gulp cuddle accept")).m_131144_(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.f_130831_, Component.m_237113_("A cosy rest in their tummy. You can climb out any time."))));
      MutableComponent decline = Component.m_237113_("[Decline]").m_130948_(Style.f_131099_.m_131140_(ChatFormatting.RED).m_131136_(true).m_131142_(new ClickEvent(Action.RUN_COMMAND, "/gulp cuddle decline")).m_131144_(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.f_130831_, Component.m_237113_("No thank you"))));
      p.m_213846_(Component.m_237113_("<").m_7220_(villager.m_7755_()).m_130946_("> " + trait.cuddleOfferLine() + " ").m_7220_(accept).m_130946_(" ").m_7220_(decline));
      p.m_6330_(SoundEvents.f_12509_, SoundSource.NEUTRAL, 1.0F, 1.0F);
   }

   public static void respondCuddle(ServerPlayer p, boolean accept) {
      Offer offer = (Offer)CUDDLE_OFFERS.remove(p.m_20148_());
      long now = (long)p.m_20194_().m_129921_();
      if (offer != null && offer.expires() > now) {
         Entity e = p.m_284548_().m_8791_(offer.villager());
         if (e instanceof LivingEntity) {
            LivingEntity le = (LivingEntity)e;
            if (le.m_6084_() && !(p.m_20280_(le) > (double)64.0F)) {
               if (!accept) {
                  p.m_213846_(Component.m_237113_("<").m_7220_(le.m_7755_()).m_130946_("> No problem. The offer stands."));
                  return;
               }

               if (!PlayerHolding.isHeld(p) && !p.m_5833_()) {
                  if (!StomachManager.get(p.m_20194_()).of(p.m_20148_()).role.canBeEaten()) {
                     StomachLogic.say(p, Component.m_237113_("Your role can't be swallowed. Set it to Prey or Both in Settings first."));
                     return;
                  }

                  VillagerHolding.hold(p, le, true);
                  return;
               }

               return;
            }
         }

         p.m_213846_(Component.m_237113_("They've wandered off."));
      } else {
         p.m_213846_(Component.m_237113_("Nobody is waiting for an answer."));
      }
   }

   public static void listNearby(ServerPlayer p) {
      p.m_213846_(Component.m_237113_("[Gulp] villagers nearby:").m_130940_(ChatFormatting.AQUA));
      if (!active()) {
         p.m_213846_(Component.m_237113_("- MCA integration is off or didn't match MCA (see latest.log).").m_130940_(ChatFormatting.YELLOW));
      } else {
         List<Entity> found = new ArrayList(p.m_9236_().m_6249_(p, p.m_20191_().m_82400_((double)24.0F), McaIntegration::isVillager));
         if (found.isEmpty()) {
            p.m_213846_(Component.m_237113_("- No MCA villager within 24 blocks.").m_130940_(ChatFormatting.YELLOW));
         } else {
            found.sort(Comparator.comparingDouble((v) -> p.m_20280_(v)));
            int shown = 0;

            for(Entity e : found) {
               ++shown;
               if (shown > 20) {
                  int var5 = found.size();
                  p.m_213846_(Component.m_237113_("...and " + (var5 - 20) + " more."));
                  break;
               }

               MutableComponent var10001 = Component.m_237113_("- ").m_7220_(e.m_7755_());
               String var10002 = voreKind(e);
               p.m_213846_(var10001.m_130946_(" | " + var10002 + comfortNote(e) + " | hearts " + heartsWith(e, p) + " | " + Math.round(p.m_20270_(e)) + " blocks" + (isProtectedChild(e) ? " | child" : "")));
            }

         }
      }
   }

   public static void debugNearest(ServerPlayer p) {
      Stomach s = StomachManager.get(p.m_20194_()).of(p.m_20148_());
      long now = (long)p.m_20194_().m_129921_();
      List<String> why = new ArrayList();
      if (!(Boolean)GulpServerConfig.MCA_AFFECTION_ENABLED.get()) {
         why.add("mcaAffectionEnabled is false in the config.");
      }

      if (!active()) {
         why.add("MCA integration is off or didn't match MCA (see latest.log).");
      }

      if (!s.role.canEat()) {
         why.add("Your role is " + s.role.label + ", which can't swallow.");
      }

      if (!s.usesGulpSwallow()) {
         why.add("Gulp isn't your swallowing system (Settings > Swallowing).");
      }

      if (s.isHard(s.organ()) && (Boolean)GulpServerConfig.HARD_MODE_ENABLED.get()) {
         why.add("You're in hard mode: requests only come in soft mode.");
      }

      if (OFFERS.containsKey(p.m_20148_())) {
         why.add("A request is already waiting for your answer.");
      }

      Long pc = (Long)PLAYER_COOLDOWN.get(p.m_20148_());
      if (pc != null && pc > now) {
         long var10001 = pc - now;
         why.add("You had a request recently: wait " + var10001 / 20L + "s.");
      }

      Entity nearest = null;
      double best = Double.MAX_VALUE;
      if (active()) {
         for(Entity e : p.m_9236_().m_6249_(p, p.m_20191_().m_82400_((double)8.0F), McaIntegration::isVillager)) {
            double d = p.m_20280_(e);
            if (d < best) {
               best = d;
               nearest = e;
            }
         }
      }

      p.m_213846_(Component.m_237113_("[Gulp] villager check:").m_130940_(ChatFormatting.AQUA));
      if (nearest == null) {
         why.add(active() ? "No MCA villager within 8 blocks." : "No villager check possible.");
      } else if (nearest instanceof LivingEntity) {
         LivingEntity le = (LivingEntity)nearest;
         int hearts = heartsWith(le, p);
         int need = (Integer)GulpServerConfig.MCA_AFFECTION_HEARTS.get();
         Attitude a = attitude(le);
         MutableComponent var21 = Component.m_237113_("Nearest: ").m_7220_(le.m_7755_());
         String var10002 = personalityOf(le);
         p.m_213846_(var21.m_130946_(" | personality " + var10002 + " (" + String.valueOf(a) + ") | kind " + voreKind(le) + comfortNote(le) + " | hearts " + hearts + " (needs " + need + ") | child: " + isProtectedChild(le)));
         p.m_213846_(Component.m_237113_("Trade prices with you: " + VillagerTrade.describe(hearts)));
         if (isProtectedChild(le)) {
            why.add("They are a child: never asked, never swallowed.");
         }

         VoreType type = voreTypeOf(le);
         if (type == McaIntegration.VoreType.PRED) {
            why.add("They are a pred type: preds don't ask to be carried.");
         }

         if (a == McaIntegration.Attitude.FEARFUL && type != McaIntegration.VoreType.PREY) {
            why.add("Shy villagers only ask if they are a prey type.");
         }

         if (a == McaIntegration.Attitude.FEARFUL) {
            need *= 2;
         }

         if (hearts < need) {
            why.add("Hearts " + hearts + " is below " + need + ". Talk to them or give gifts, or lower mcaAffectionHearts.");
         }

         Long vc = (Long)VILLAGER_COOLDOWN.get(le.m_20148_());
         if (vc != null && vc > now) {
            long var22 = vc - now;
            why.add("They asked recently: wait " + var22 / 20L + "s.");
         }

         float size = Math.max(0.05F, le.m_20205_() * le.m_20205_() * le.m_20206_());
         if ((double)size > s.capacityOf(s.organ()) || s.usedIn(s.organ()) + (double)size > s.capacityOf(s.organ()) || s.contents.size() >= 100) {
            why.add("They wouldn't fit in your stomach right now.");
         }
      }

      if (why.isEmpty()) {
         p.m_213846_(Component.m_237113_("Nothing is blocking a request. They ask on a random check every 5 seconds.").m_130940_(ChatFormatting.GREEN));
      } else {
         for(String w : why) {
            p.m_213846_(Component.m_237113_("- " + w).m_130940_(ChatFormatting.YELLOW));
         }
      }

   }

   private static enum Attitude {
      FAN,
      NEUTRAL,
      FEARFUL;

      // $FF: synthetic method
      private static Attitude[] $values() {
         return new Attitude[]{FAN, NEUTRAL, FEARFUL};
      }
   }

   private static enum VoreType {
      NORMAL,
      PREY,
      PRED;

      // $FF: synthetic method
      private static VoreType[] $values() {
         return new VoreType[]{NORMAL, PREY, PRED};
      }
   }

   private static record Scare(LivingEntity villager, UUID player, long until) {
   }

   private static record Offer(UUID villager, long expires) {
   }
}
