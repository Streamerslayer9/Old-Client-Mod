package com.example.gulp;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;

public final class StomachLogic {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final float STRUGGLE_CHANCE_PER_SECOND = 0.08F;
   private static final float UNBIRTH_STRUGGLE_CHANCE_PER_SECOND = 0.15F;
   private static final float DISSOLVE_SECONDS = 8.0F;
   private static final float STRUGGLE_BASE_DAMAGE = 2.0F;
   private static final float STRUGGLE_PER_VOLUME = 0.75F;
   private static final float STRUGGLE_MAX_DAMAGE = 5.0F;
   private static ServerPlayer digestingPlayer;
   private static LivingEntity digestingMob;
   private static int itemsGiven;
   private static boolean suppressLoot;
   private static int pendingLooting = 0;
   private static final Map<UUID, SimpleContainer> OPEN_VIEWS = new HashMap();
   static final List<CompoundTag> PENDING_ADD = new ArrayList();

   public static ServerPlayer digestingPlayer() {
      return digestingPlayer;
   }

   public static LivingEntity digestingMob() {
      return digestingMob;
   }

   public static void giveDrops(Collection<ItemEntity> drops) {
      if (digestingPlayer != null && !suppressLoot) {
         for(ItemEntity drop : drops) {
            if (!drop.m_32055_().m_41619_()) {
               itemsGiven += drop.m_32055_().m_41613_();
               ItemHandlerHelper.giveItemToPlayer(digestingPlayer, drop.m_32055_().m_41777_());
            }
         }

      }
   }

   public static int pendingLooting() {
      return pendingLooting;
   }

   private StomachLogic() {
   }

   public static void handleAction(ServerPlayer p, int action, int arg) {
      if (p != null && p.m_6084_()) {
         if (PlayerHolding.isHeld(p)) {
            if (action != 2 && action != 3) {
               if (action == 11) {
                  PlayerHolding.struggle(p);
               } else if (action == 7) {
                  PlayerHolding.sendSharedStorage(p);
               }
            } else {
               PlayerHolding.tryRelease(p);
            }

         } else if (!p.m_5833_()) {
            StomachManager mgr = StomachManager.get(p.m_20194_());
            Stomach s = mgr.of(p.m_20148_());
            if (s.role.canEat() || action != 0 && action != 1 && action != 5 && action != 7 && action != 9 && action != 10 && action != 12 && action != 13 && action != 15 && action != 16 && action != 18 && action != 30 && action != 31 && action != 32 && action != 33) {
               switch (action) {
                  case 0:
                     swallow(p, s, arg);
                     break;
                  case 1:
                     toggleHard(p, s, s.organ());
                     break;
                  case 2:
                     if (!s.contents.isEmpty() && release(p, (CompoundTag)s.contents.get(s.contents.size() - 1))) {
                        s.contents.remove(s.contents.size() - 1);
                        sound(p, GulpSound.SPIT);
                     }
                     break;
                  case 3:
                     boolean any = false;
                     Iterator<CompoundTag> it = s.contents.iterator();

                     while(it.hasNext()) {
                        if (release(p, (CompoundTag)it.next())) {
                           it.remove();
                           any = true;
                        }
                     }

                     if (any) {
                        sound(p, GulpSound.SPIT);
                     }
                     break;
                  case 4:
                     int i = indexOf(s, arg);
                     if (i >= 0 && release(p, (CompoundTag)s.contents.get(i))) {
                        s.contents.remove(i);
                        sound(p, GulpSound.SPIT);
                     }
                     break;
                  case 5:
                     int i = indexOf(s, arg);
                     if (i >= 0) {
                        if (!s.instantDigest) {
                           say(p, Component.m_237113_("Instant digest is turned off. You can turn it on in Settings."));
                        } else if (PlayerHolding.isPlayerEntry((CompoundTag)s.contents.get(i))) {
                           say(p, Component.m_237113_("Players are only held, never digested."));
                        } else if (((CompoundTag)s.contents.get(i)).m_128471_("Safe")) {
                           say(p, Component.m_237113_("They came in by invitation, so they can't be digested."));
                        } else if (McaIntegration.isChildEntry((CompoundTag)s.contents.get(i), p.m_284548_())) {
                           say(p, Component.m_237113_("Children can never be digested."));
                        } else if (hardModeAllowed()) {
                           digest(p, s, (CompoundTag)s.contents.remove(i), true);
                           flushPending(s);
                        } else {
                           say(p, Component.m_237113_("Digesting is turned off on this server."));
                        }
                     }
                     break;
                  case 6:
                     Perk perk = Perk.byIndex(arg & 255);
                     int count = Math.max(1, Math.min(100, arg >> 8));
                     if (perk != null && s.level < perk.minLevel) {
                        say(p, Component.m_237113_(perk.title + " unlocks at stomach level " + perk.minLevel + "."));
                     } else if (perk != null) {
                        int bought = Math.min(count, Math.min(s.points, perk.maxRank - s.rank(perk)));
                        if (bought > 0) {
                           int[] var10000 = s.ranks;
                           int var10001 = perk.ordinal();
                           var10000[var10001] += bought;
                           s.points -= bought;
                           p.m_9236_().m_5594_((Player)null, p.m_20183_(), SoundEvents.f_11871_, SoundSource.PLAYERS, 0.8F, 1.2F);
                        }
                     }
                     break;
                  case 7:
                     openStorage(p, mgr, s);
                     break;
                  case 8:
                     Role newRole = Role.byId(arg);
                     if (newRole != null && newRole != s.role) {
                        s.role = newRole;
                        if (!newRole.canEat()) {
                           boolean any = false;
                           Iterator<CompoundTag> it = s.contents.iterator();

                           while(it.hasNext()) {
                              if (release(p, (CompoundTag)it.next())) {
                                 it.remove();
                                 any = true;
                              }
                           }

                           if (any) {
                              sound(p, GulpSound.SPIT);
                           }
                        }

                        say(p, Component.m_237113_("Your role is now " + newRole.label + ". " + newRole.description));
                        RolesPacket.broadcast(p.m_20194_());
                     }
                     break;
                  case 9:
                     swallowFromInventorySlot(p, s, arg);
                     break;
                  case 10:
                     if ((Boolean)GulpServerConfig.ITEM_STORAGE_ENABLED.get()) {
                        AbstractContainerMenu var30 = p.f_36096_;
                        if (var30 instanceof ChestMenu) {
                           ChestMenu cm = (ChestMenu)var30;
                           if (cm.m_39261_() == OPEN_VIEWS.get(p.m_20148_())) {
                              Container var31 = cm.m_39261_();
                              if (var31 instanceof SimpleContainer) {
                                 SimpleContainer view = (SimpleContainer)var31;
                                 StomachItems.digestFirstSlot(p, s, view, arg);
                              }
                           }
                        }
                     }
                  case 11:
                  case 14:
                  case 23:
                  case 26:
                  case 27:
                  case 28:
                  case 29:
                  default:
                     break;
                  case 12:
                  case 13:
                  case 16:
                     if ((Boolean)GulpServerConfig.ITEM_STORAGE_ENABLED.get()) {
                        AbstractContainerMenu var28 = p.f_36096_;
                        if (var28 instanceof ChestMenu) {
                           ChestMenu cm = (ChestMenu)var28;
                           if (cm.m_39261_() == OPEN_VIEWS.get(p.m_20148_())) {
                              Container var29 = cm.m_39261_();
                              if (var29 instanceof SimpleContainer) {
                                 SimpleContainer view = (SimpleContainer)var29;
                                 if (action == 12) {
                                    EnchantTools.strip(p, s, view);
                                 } else if (action == 13) {
                                    EnchantTools.transfer(p, s, view);
                                 } else {
                                    EnchantTools.destroy(p, s);
                                 }
                              }
                           }
                        }
                     }
                     break;
                  case 15:
                     if ((Boolean)GulpServerConfig.ITEM_STORAGE_ENABLED.get()) {
                        AbstractContainerMenu var26 = p.f_36096_;
                        if (var26 instanceof ChestMenu) {
                           ChestMenu cm = (ChestMenu)var26;
                           if (cm.m_39261_() == OPEN_VIEWS.get(p.m_20148_())) {
                              Container var27 = cm.m_39261_();
                              if (var27 instanceof SimpleContainer) {
                                 SimpleContainer view = (SimpleContainer)var27;
                                 EnchantTools.clickBox(p, s, view, arg & 15, arg >> 4 & 1, (arg >> 5 & 1) != 0);
                              }
                           }
                        }
                     }
                     break;
                  case 17:
                     s.instantDigest = arg != 0;
                     say(p, Component.m_237113_("Instant digest button: " + (s.instantDigest ? "ON" : "OFF")));
                     break;
                  case 18:
                     if ((Boolean)GulpServerConfig.ITEM_STORAGE_ENABLED.get()) {
                        AbstractContainerMenu var24 = p.f_36096_;
                        if (var24 instanceof ChestMenu) {
                           ChestMenu cm = (ChestMenu)var24;
                           if (cm.m_39261_() == OPEN_VIEWS.get(p.m_20148_())) {
                              Container var25 = cm.m_39261_();
                              if (var25 instanceof SimpleContainer) {
                                 SimpleContainer view = (SimpleContainer)var25;
                                 StomachItems.digestSlot(p, s, view, arg & '\uffff', (arg >> 16 & 1) != 0 ? 64 : 1);
                              }
                           }
                        }
                     }
                     break;
                  case 19:
                     SwallowSystem choice = SwallowSystem.byId(arg);
                     if (choice != null && choice != SwallowSystem.UNSET) {
                        if (!FlamevoreCompat.isLoaded()) {
                           say(p, Component.m_237113_("Flamevore isn't installed, so Gulp does the swallowing."));
                        } else if (choice != s.swallowSystem) {
                           s.swallowSystem = choice;
                           if (choice == SwallowSystem.FLAMEVORE) {
                              boolean any = false;
                              Iterator<CompoundTag> it = s.contents.iterator();

                              while(it.hasNext()) {
                                 if (release(p, (CompoundTag)it.next())) {
                                    it.remove();
                                    any = true;
                                 }
                              }

                              if (any) {
                                 sound(p, GulpSound.SPIT);
                              }

                              say(p, Component.m_237113_("Swallowing system: Flamevore. Gulp's mob eating and belly are off; your level, perks and item storage stay."));
                           } else {
                              say(p, Component.m_237113_("Swallowing system: Gulp."));
                           }
                        }
                     }
                     break;
                  case 20:
                     McaIntegration.sendVillagerInfo(p, arg);
                     break;
                  case 21:
                     VillagerHolding.buyPerk(p, arg >> 4, arg & 15);
                     break;
                  case 22:
                     VillagerStorage.open(p, arg);
                     break;
                  case 24:
                     McaIntegration.forceSwallow(p, arg);
                     break;
                  case 25:
                     McaIntegration.askConvert(p, arg);
                     break;
                  case 30:
                     if (arg >= 0 && arg < Organ.COUNT && (arg == 0 || (Boolean)GulpServerConfig.MULTI_ORGANS_ENABLED.get())) {
                        s.activeOrgan = arg;
                        say(p, Component.m_237113_("Swallowing into your " + Organ.byId(arg).lower() + " now."));
                        mgr.m_77762_();
                        sync(p, s);
                     }
                     break;
                  case 31:
                     toggleHard(p, s, arg);
                     break;
                  case 32:
                     OrganLoot.take(p, mgr, s, false);
                     break;
                  case 33:
                     OrganLoot.take(p, mgr, s, true);
               }

               mgr.m_77762_();
               sync(p, s);
            } else {
               say(p, s.role.cantSwallowMessage());
            }
         }
      }
   }

   private static int indexOf(Stomach s, int uid) {
      for(int i = 0; i < s.contents.size(); ++i) {
         if (((CompoundTag)s.contents.get(i)).m_128451_("Uid") == uid) {
            return i;
         }
      }

      return -1;
   }

   private static void swallow(ServerPlayer p, Stomach s, int targetId) {
      if (s.cooldown <= 0) {
         Entity target = null;
         if (targetId >= 0) {
            Entity candidate = p.m_284548_().m_6815_(targetId);
            if (candidate != null && TargetFinder.valid(candidate) && TargetFinder.withinReach(p, candidate, (double)2.0F)) {
               target = candidate;
            }
         }

         if (target == null) {
            target = TargetFinder.find(p, (double)4.0F);
         }

         if (target instanceof ItemEntity) {
            ItemEntity item = (ItemEntity)target;
            swallowItem(p, s, item);
         } else if (!s.usesGulpSwallow()) {
            say(p, notGulpSwallowMessage(s));
         } else if (target instanceof ServerPlayer) {
            ServerPlayer victim = (ServerPlayer)target;
            PlayerHolding.request(p, s, victim, StomachManager.get(p.m_20194_()));
         } else if (target instanceof LivingEntity) {
            LivingEntity le = (LivingEntity)target;
            if (le instanceof TamableAnimal) {
               TamableAnimal pet = (TamableAnimal)le;
               if (pet.m_21824_() && pet.m_21805_() != null && !pet.m_21805_().equals(p.m_20148_())) {
                  say(p, Component.m_237113_("That pet belongs to someone else."));
                  return;
               }
            }

            if (le instanceof AbstractHorse) {
               AbstractHorse horse = (AbstractHorse)le;
               if (horse.m_30614_() && horse.m_21805_() != null && !horse.m_21805_().equals(p.m_20148_())) {
                  say(p, Component.m_237113_("That animal belongs to someone else."));
                  return;
               }
            }

            swallowLiving(p, s, le, false);
         } else {
            say(p, Component.m_237113_("Nothing swallowable in reach."));
         }
      }
   }

   static boolean swallowLiving(ServerPlayer p, Stomach s, LivingEntity le, boolean safe) {
      if (!s.usesGulpSwallow()) {
         say(p, notGulpSwallowMessage(s));
         return false;
      } else if (McaIntegration.isProtectedChild(le)) {
         say(p, Component.m_237119_().m_7220_(le.m_7755_()).m_130946_(" is a child and can never be swallowed."));
         return false;
      } else {
         List<ServerPlayer> inside = VillagerHolding.heldBy(le, p.m_20194_());
         boolean nested = !inside.isEmpty() && (Boolean)GulpServerConfig.NESTED_SWALLOW_ENABLED.get() && (Boolean)GulpServerConfig.PLAYER_SWALLOWING_ENABLED.get() && s.role.canEat();
         float size = Math.max(0.05F, le.m_20205_() * le.m_20205_() * le.m_20206_());
         if (nested) {
            for(ServerPlayer a : inside) {
               size += PlayerHolding.sizeOf(a);
            }
         }

         String withInside = nested ? " with a player inside" : "";
         int organ = s.organ();
         String where = Organ.byId(organ).lower();
         if ((double)size > s.capacityOf(organ)) {
            say(p, Component.m_237113_("Too big for your " + where + withInside + "! (needs " + Stomach.fmtVolume((double)size) + ", capacity " + Stomach.fmtVolume(s.capacity()) + ")"));
            return false;
         } else if (s.usedIn(organ) + (double)size > s.capacityOf(organ)) {
            say(p, Component.m_237113_("Your " + where + " is too full" + (nested ? " for a villager with a player inside." : ".")));
            return false;
         } else if (s.contents.size() >= 100) {
            say(p, Component.m_237113_("You can't hold more than 100 creatures at once."));
            return false;
         } else {
            le.m_8127_();
            if (!inside.isEmpty() && !nested) {
               VillagerHolding.onVillagerSwallowed(le, p);
            }

            McaIntegration.onSwallow(p, s, le, safe);
            CompoundTag tag = new CompoundTag();
            if (!le.m_20223_(tag)) {
               return false;
            } else {
               CompoundTag entry = new CompoundTag();
               entry.m_128365_("Entity", tag);
               entry.m_128350_("Size", size);
               entry.m_128350_("MaxHealth", le.m_21233_());
               entry.m_128405_("Digest", 0);
               entry.m_128405_("DigestTime", 100 + (int)((double)150.0F * Math.sqrt((double)size)));
               Component displayName = le.m_7755_();
               entry.m_128359_("Name", displayName.getString());
               entry.m_128359_("NameJson", Serializer.m_130703_(displayName));
               entry.m_128405_("Uid", p.m_217043_().m_188502_());
               entry.m_128379_("Safe", safe);
               McaIntegration.tagEntry(entry, le, p);
               if (nested) {
                  ListTag who = new ListTag();

                  for(ServerPlayer a : inside) {
                     who.add(StringTag.m_129297_(a.m_20148_().toString()));
                     VillagerHolding.nest(a, le, p, PlayerHolding.sizeOf(a));
                  }

                  entry.m_128365_("NestedPlayers", who);
                  entry.m_128350_("NestedExtra", size - Math.max(0.05F, le.m_20205_() * le.m_20205_() * le.m_20206_()));
               }

               le.m_146870_();
               s.addEntry(entry);
               finishSwallow(p, s, Component.m_237119_().m_7220_(displayName).m_130946_(VillagerHolding.nestedLabel(entry)));
               if (nested) {
                  p.m_213846_(Component.m_237113_("<").m_7220_(displayName).m_130946_("> " + McaIntegration.traitOf(le).swallowedWhileHoldingLine()));
               }

               return true;
            }
         }
      }
   }

   private static void toggleHard(ServerPlayer p, Stomach s, int organ) {
      if (organ >= 0 && organ < Organ.COUNT && (organ == 0 || (Boolean)GulpServerConfig.MULTI_ORGANS_ENABLED.get())) {
         boolean now = s.isHard(organ);
         String name = organ == 0 ? "Stomach" : Organ.byId(organ).label;
         if (!now && !hardModeAllowed()) {
            say(p, Component.m_237113_("Hard mode is turned off on this server."));
         } else {
            s.setHard(organ, !now);
            say(p, Component.m_237113_(name + (!now ? " is in hard mode: contents will be digested for loot." : " is in soft mode: contents are held safely.")));
         }
      }
   }

   static void unbirthNausea(ServerPlayer p) {
      int max = (Integer)GulpServerConfig.UNBIRTH_NAUSEA_MAX_SECONDS.get();
      int seconds = 1 + p.m_217043_().m_188503_(max);
      p.m_7292_(new MobEffectInstance(MobEffects.f_19604_, seconds * 20, 0));
   }

   static Component notGulpSwallowMessage(Stomach s) {
      return s.swallowSystem == SwallowSystem.UNSET ? Component.m_237113_("Choose your swallowing system first (press V, or open Settings).") : Component.m_237113_("Flamevore is doing your swallowing. Switch to Gulp in Settings to eat mobs with Gulp.");
   }

   static void finishSwallow(ServerPlayer p, Stomach s, Component what) {
      int quick = s.rank(Perk.QUICK);
      float reduction = (p.m_146909_() < -30.0F ? 3.0F : 1.5F) * (float)quick;
      s.cooldown = Math.max(10, 40 - Math.round(reduction));
      sound(p, GulpSound.SWALLOW);
      int organ = s.organ();
      Component msg = Component.m_237113_("Gulp! Swallowed ").m_7220_(what);
      if (organ != 0) {
         MutableComponent var10000 = msg.m_6881_();
         String var10001 = Organ.byId(organ).lower();
         msg = var10000.m_130946_(" into your " + var10001 + (organ == 4 ? " (it takes a while to reach your stomach)" : ""));
      }

      say(p, msg);
   }

   private static void swallowItem(ServerPlayer p, Stomach s, ItemEntity item) {
      if (!(Boolean)GulpServerConfig.ITEM_STORAGE_ENABLED.get()) {
         say(p, Component.m_237113_("Item storage is turned off on this server."));
      } else if (p.f_36096_ == p.f_36095_) {
         ItemStack stack = item.m_32055_();
         int before = stack.m_41613_();
         ItemStack left = s.insertItem(stack);
         int taken = before - left.m_41613_();
         if (taken <= 0) {
            say(p, Component.m_237113_("Your item storage is full."));
         } else {
            if (left.m_41619_()) {
               item.m_146870_();
            } else {
               item.m_32045_(left);
            }

            finishSwallow(p, s, Component.m_237113_(taken + "x ").m_7220_(stack.m_41786_()));
         }
      }
   }

   private static void swallowFromInventorySlot(ServerPlayer p, Stomach s, int arg) {
      if (!(Boolean)GulpServerConfig.ITEM_STORAGE_ENABLED.get()) {
         say(p, Component.m_237113_("Item storage is turned off on this server."));
      } else if (p.f_36096_ == p.f_36095_) {
         int slotId = arg & '\uffff';
         boolean justOne = (arg & 65536) != 0;
         if (slotId >= 0 && slotId < p.f_36095_.f_38839_.size()) {
            Slot slot = p.f_36095_.m_38853_(slotId);
            if (slot.f_40218_ instanceof Inventory && slot.getSlotIndex() < 36 && slot.m_6657_()) {
               ItemStack stack = slot.m_7993_();
               boolean hazardous = HazardItems.isHazardous(stack);
               int wanted = justOne ? 1 : stack.m_41613_();
               ItemStack offer = stack.m_41777_();
               offer.m_41764_(wanted);
               ItemStack left = s.insertItem(offer);
               int taken = wanted - left.m_41613_();
               if (taken <= 0) {
                  say(p, Component.m_237113_("Your item storage is full."));
               } else {
                  Component name = stack.m_41786_();
                  slot.m_6201_(taken);
                  p.f_36096_.m_38946_();
                  sound(p, GulpSound.SWALLOW);
                  say(p, Component.m_237113_("Gulp! Swallowed " + taken + "x ").m_7220_(name));
                  if (hazardous) {
                     hazardHurt(p, name.getString() + " hurt on the way down.");
                  }

               }
            }
         }
      }
   }

   static void hazardHurt(ServerPlayer p, String message) {
      if (!p.m_7500_()) {
         float damage = Math.min(((Double)GulpServerConfig.HAZARD_SELF_DAMAGE.get()).floatValue(), p.m_21223_() - 4.0F);
         if (damage > 0.0F) {
            p.m_6469_(p.m_269291_().m_269264_(), damage);
            say(p, Component.m_237113_("Ouch! " + message));
         }

      }
   }

   private static void openStorage(ServerPlayer p, StomachManager mgr, Stomach s) {
      if (!(Boolean)GulpServerConfig.ITEM_STORAGE_ENABLED.get()) {
         say(p, Component.m_237113_("Item storage is turned off on this server."));
      } else {
         int rows = s.storageRows();
         int slots = rows * 9;
         SimpleContainer view = new SimpleContainer(slots);

         for(int i = 0; i < slots; ++i) {
            view.m_6836_(i, (ItemStack)s.storage.get(i));
         }

         OPEN_VIEWS.put(p.m_20148_(), view);
         view.m_19164_((c) -> {
            for(int i = 0; i < slots; ++i) {
               s.storage.set(i, view.m_8020_(i));
            }

            mgr.m_77762_();
            sync(p, s);
         });
         MenuType var10000;
         switch (rows) {
            case 1 -> var10000 = MenuType.f_39957_;
            case 2 -> var10000 = MenuType.f_39958_;
            case 3 -> var10000 = MenuType.f_39959_;
            case 4 -> var10000 = MenuType.f_39960_;
            case 5 -> var10000 = MenuType.f_39961_;
            default -> var10000 = MenuType.f_39962_;
         }

         MenuType<ChestMenu> type = var10000;
         p.m_5893_(new SimpleMenuProvider((id, inv, player) -> new StorageMenu(type, id, inv, view, rows), Component.m_237113_("Stomach Storage")));
      }
   }

   public static void tickPlayer(StomachManager mgr, ServerPlayer p, Stomach s) {
      if (s.cooldown > 0) {
         --s.cooldown;
      }

      if (s.predator > 0 && p.f_19797_ % ((Integer)GulpServerConfig.MCA_PREDATOR_DECAY_SECONDS.get() * 20) == 0) {
         --s.predator;
         sync(p, s);
         mgr.m_77762_();
      }

      StomachItems.tick(p, mgr, s, s.hard && hardModeAllowed());
      OrganLoot.tickBreast(p, mgr, s, s.isHard(Organ.BREAST.ordinal()) && hardModeAllowed());
      if (!s.contents.isEmpty()) {
         boolean changed = false;
         boolean dissolving = false;
         boolean hardNow = s.hard && hardModeAllowed();
         int healAmount = s.rank(Perk.HEALING);
         Iterator<CompoundTag> it = s.contents.iterator();
         int tailTicks = (Integer)GulpServerConfig.TAIL_TRANSIT_SECONDS.get() * 20;

         while(it.hasNext()) {
            CompoundTag e = (CompoundTag)it.next();
            int organ = Organ.of(e);
            if (organ == 4) {
               int t = Math.min(tailTicks, e.m_128451_("Transit") + 1);
               e.m_128405_("Transit", t);
               if (t >= tailTicks && s.usedIn(0) + (double)e.m_128457_("Size") <= s.capacityOf(0)) {
                  e.m_128405_("Organ", 0);
                  e.m_128473_("Transit");
                  e.m_128473_("TransitWait");
                  organ = 0;
                  changed = true;
                  say(p, Component.m_237119_().m_7220_(entryName(e)).m_130946_(" reached your stomach."));
                  sound(p, GulpSound.SWALLOW);
               } else if (t >= tailTicks && !e.m_128471_("TransitWait")) {
                  e.m_128379_("TransitWait", true);
                  say(p, Component.m_237119_().m_7220_(entryName(e)).m_130946_(" is waiting in your tail: your stomach is full."));
               }
            }

            boolean hardE = s.isHard(organ) && hardModeAllowed();
            if (PlayerHolding.isPlayerEntry(e)) {
               if (!PlayerHolding.tickEntry(p, e)) {
                  it.remove();
                  changed = true;
               }
            } else {
               e.m_128405_("HeldTicks", e.m_128451_("HeldTicks") + 1);
               if (e.m_128441_("NestedPlayers") && !VillagerHolding.hasNested(e)) {
                  VillagerHolding.clearNested(e);
               }

               if (McaIntegration.isChildEntry(e, p.m_284548_())) {
                  if (release(p, e)) {
                     it.remove();
                     changed = true;
                     say(p, Component.m_237119_().m_7220_(entryName(e)).m_130946_(" is a child and was let out. Children can never be swallowed."));
                  }
               } else {
                  McaIntegration.tickInside(p, e, hardE);
                  if (!e.m_128471_("Safe")) {
                     if (organ == 3 && p.f_19797_ % 20 == 0 && p.m_217043_().m_188501_() < 0.15F) {
                        unbirthNausea(p);
                        say(p, Component.m_237119_().m_7220_(entryName(e)).m_130946_(" squirms inside you!"));
                     }

                     if (hardE) {
                        int d = e.m_128451_("Digest");
                        int digestTime = e.m_128451_("DigestTime");
                        if (d < digestTime) {
                           ++d;
                           e.m_128405_("Digest", d);
                        }

                        if (d >= digestTime) {
                           CompoundTag ent = e.m_128469_("Entity");
                           float maxHealth = Math.max(1.0F, e.m_128457_("MaxHealth"));
                           if (!e.m_128471_("Dissolving")) {
                              e.m_128379_("Dissolving", true);
                              if (ent.m_128457_("Health") <= 0.0F) {
                                 ent.m_128350_("Health", maxHealth);
                              }
                           }

                           float health = ent.m_128457_("Health") - maxHealth / 160.0F;
                           if (health <= 0.0F) {
                              it.remove();
                              digest(p, s, e, false);
                              changed = true;
                              continue;
                           }

                           ent.m_128350_("Health", health);
                           dissolving = true;
                        }

                        if (p.f_19797_ % 20 == 0 && p.m_21223_() > 4.0F && p.m_217043_().m_188501_() < 0.08F) {
                           float strength = Math.min(5.0F, 2.0F + 0.75F * e.m_128457_("Size"));
                           float damage = strength * Math.max(0.0F, 1.0F - 0.05F * (float)s.rank(Perk.IRON)) * ((Double)GulpServerConfig.STRUGGLE_DAMAGE_MULTIPLIER.get()).floatValue();
                           damage = Math.min(damage, p.m_21223_() - 4.0F);
                           if (damage > 0.0F) {
                              p.m_6469_(p.m_9236_().m_269111_().m_269264_(), damage);
                              say(p, Component.m_237119_().m_7220_(entryName(e)).m_130946_(" struggles inside you!"));
                           }
                        }
                     } else if (healAmount > 0 && p.f_19797_ % 100 == 0) {
                        CompoundTag ent = e.m_128469_("Entity");
                        float maxHealth = e.m_128457_("MaxHealth");
                        float heal = Math.max(1.0F, maxHealth * 0.01F * (float)healAmount);
                        ent.m_128350_("Health", Math.min(maxHealth, ent.m_128457_("Health") + heal));
                     }
                  }
               }
            }
         }

         flushPending(s);
         if (!hardNow && p.f_19797_ % 20 == 0) {
            float fill = (float)Math.min((double)1.0F, s.used() / s.capacity());
            gainXp(p, s, fill * (float)Stomach.xpForNext(s.level) / 240.0F);
         }

         if (p.f_19797_ % 20 == 0) {
            mgr.m_77762_();
         }

         int syncEvery = s.contents.size() > 20 ? 20 : (dissolving ? 5 : 10);
         if (changed || p.f_19797_ % syncEvery == 0) {
            sync(p, s);
         }

      }
   }

   private static void flushPending(Stomach s) {
      if (!PENDING_ADD.isEmpty()) {
         s.contents.addAll(PENDING_ADD);
         PENDING_ADD.clear();
      }
   }

   private static void digest(ServerPlayer p, Stomach s, CompoundTag entry, boolean manual) {
      if (VillagerHolding.hasNested(entry)) {
         VillagerHolding.transferNested(p, entry);
      }

      ServerLevel w = p.m_284548_();
      int lootRank = s.rank(Perk.RICH);
      int looting = lootRank / 2 + (lootRank % 2 == 1 && p.m_217043_().m_188501_() < 0.5F ? 1 : 0);
      int organ = Organ.of(entry);
      EntityType.m_20642_(entry.m_128469_("Entity"), w).ifPresent((e) -> {
         e.m_20084_(UUID.randomUUID());
         e.m_7678_(p.m_20185_(), p.m_20186_(), p.m_20189_(), 0.0F, 0.0F);
         if (e instanceof LivingEntity le) {
            McaIntegration.onDigest(p, s, le);
            le.m_20225_(true);
            suppressLoot = OrganLoot.mobDigested(p, s, entry, organ);
            digestingPlayer = p;
            digestingMob = le;
            pendingLooting = looting;
            itemsGiven = 0;

            try {
               le.m_6469_(w.m_269111_().m_269075_(p), Math.max(10000.0F, le.m_21233_() * 2.0F));
               if (le.m_6084_()) {
                  le.m_6074_();
               }
            } catch (RuntimeException ex) {
               LOGGER.warn("Vore Mod: {} threw an error while being digested; letting it die normally instead.", le.m_6095_(), ex);

               try {
                  w.m_7967_(le);
                  le.m_6074_();
               } catch (RuntimeException ex2) {
                  LOGGER.warn("Vore Mod: that mob also couldn't be killed normally.", ex2);
               }
            } finally {
               pendingLooting = 0;
               digestingPlayer = null;
               digestingMob = null;
               suppressLoot = false;
            }

            if (le.m_6084_()) {
               le.m_20225_(false);
               w.m_7967_(le);
            }
         }

      });
      if (itemsGiven > 0) {
         p.m_9236_().m_5594_((Player)null, p.m_20183_(), SoundEvents.f_12019_, SoundSource.PLAYERS, 0.25F, 1.3F);
         itemsGiven = 0;
      }

      int hearty = s.rank(Perk.HEARTY);
      if (hearty > 0) {
         p.m_5634_(0.5F * (float)hearty);
      }

      sound(p, manual ? GulpSound.DIGEST_BUTTON : GulpSound.DIGEST);
      gainXp(p, s, (float)Stomach.digestXp(entry.m_128457_("MaxHealth")) * (1.0F + 0.05F * (float)s.rank(Perk.GOURMET)));
   }

   public static boolean release(ServerPlayer p, CompoundTag entry) {
      if (PlayerHolding.isPlayerEntry(entry)) {
         return PlayerHolding.releaseEntry(p, entry);
      } else {
         ServerLevel w = p.m_284548_();
         Entity e = (Entity)EntityType.m_20642_(entry.m_128469_("Entity"), w).orElse((Object)null);
         if (e == null) {
            return false;
         } else {
            Vec3 fwd = p.m_20252_(1.0F).m_82490_((double)1.5F);
            float yaw = p.m_146908_() + 180.0F;
            e.m_7678_(p.m_20185_() + fwd.f_82479_, p.m_20186_() + (double)0.5F, p.m_20189_() + fwd.f_82481_, yaw, 0.0F);
            if (!w.m_45786_(e)) {
               e.m_7678_(p.m_20185_(), p.m_20186_() + 0.1, p.m_20189_(), yaw, 0.0F);
            }

            e.m_20256_(Vec3.f_82478_);
            e.f_19789_ = 0.0F;
            boolean added = w.m_7967_(e);
            if (!added) {
               e.m_20084_(UUID.randomUUID());
               added = w.m_7967_(e);
            }

            if (added) {
               McaIntegration.onRelease(p, StomachManager.get(p.m_20194_()).of(p.m_20148_()), e, entry.m_128451_("HeldTicks"));
            }

            return added;
         }
      }
   }

   public static void releaseAll(ServerPlayer p) {
      StomachManager mgr = StomachManager.get(p.m_20194_());
      Stomach s = mgr.of(p.m_20148_());
      s.contents.removeIf((e) -> release(p, e));
      mgr.m_77762_();
      sync(p, s);
   }

   static void stomachItemsToInventory(ServerPlayer p) {
      if (!p.m_9236_().m_46469_().m_46207_(GameRules.f_46133_)) {
         StomachManager mgr = StomachManager.get(p.m_20194_());
         Stomach s = mgr.of(p.m_20148_());
         boolean changed = false;

         for(int i = 0; i < s.storage.size(); ++i) {
            ItemStack stack = (ItemStack)s.storage.get(i);
            if (!stack.m_41619_()) {
               ItemStack rest = stack.m_41777_();
               p.m_150109_().m_36054_(rest);
               s.storage.set(i, rest.m_41619_() ? ItemStack.f_41583_ : rest);
               changed = true;
            }
         }

         if (changed) {
            mgr.m_77762_();
         }

      }
   }

   static void addStomachDrops(ServerPlayer p, Collection<ItemEntity> drops) {
      if (!p.m_9236_().m_46469_().m_46207_(GameRules.f_46133_)) {
         StomachManager mgr = StomachManager.get(p.m_20194_());
         Stomach s = mgr.of(p.m_20148_());
         boolean changed = false;

         for(int i = 0; i < s.storage.size(); ++i) {
            ItemStack stack = (ItemStack)s.storage.get(i);
            if (!stack.m_41619_()) {
               ItemEntity ie = new ItemEntity(p.m_9236_(), p.m_20185_(), p.m_20186_(), p.m_20189_(), stack.m_41777_());
               ie.m_32060_();
               drops.add(ie);
               s.storage.set(i, ItemStack.f_41583_);
               changed = true;
            }
         }

         if (changed) {
            mgr.m_77762_();
            sync(p, s);
         }

      }
   }

   private static void levelUp(ServerPlayer p, Stomach s) {
      p.m_9236_().m_5594_((Player)null, p.m_20183_(), SoundEvents.f_12275_, SoundSource.PLAYERS, 0.7F, 1.0F);
      Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new LevelUpPacket(s.level, s.points));
   }

   static void say(ServerPlayer p, Component text) {
      Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new MessagePacket(text));
   }

   static void sound(ServerPlayer p, GulpSound type) {
      Net.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), new SoundPacket(type.ordinal(), p.m_20185_(), p.m_20186_(), p.m_20189_()));
   }

   public static Component entryName(CompoundTag entry) {
      Component base = null;
      if (entry.m_128441_("NameJson")) {
         base = Serializer.m_130701_(entry.m_128461_("NameJson"));
      }

      if (base == null) {
         base = Component.m_237113_(entry.m_128461_("Name"));
      }

      String inside = VillagerHolding.nestedLabel(entry);
      return (Component)(inside.isEmpty() ? base : Component.m_237119_().m_7220_(base).m_130946_(inside));
   }

   private static boolean hardModeAllowed() {
      return (Boolean)GulpServerConfig.HARD_MODE_ENABLED.get();
   }

   static void gainXp(ServerPlayer p, Stomach s, float amount) {
      float scaled = amount * ((Double)GulpServerConfig.XP_MULTIPLIER.get()).floatValue();
      int whole = (int)scaled;
      if (p.m_217043_().m_188501_() < scaled - (float)whole) {
         ++whole;
      }

      if (whole > 0 && s.addXp(whole) > 0) {
         levelUp(p, s);
      }

   }

   private static String fmt(double d) {
      return String.format("%.1f", d);
   }

   public static void sync(ServerPlayer p, Stomach s) {
      for(CompoundTag e : s.contents) {
         if (!e.m_128441_("Uid")) {
            e.m_128405_("Uid", p.m_217043_().m_188502_());
         }
      }

      Net.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), SyncPacket.from(s));
      float ratio = s.usesGulpSwallow() ? (float)Math.min((double)1.0F, s.used() / s.capacity()) : 0.0F;
      Net.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), new BellyPacket(p.m_20148_(), ratio));
   }

   private static final class StorageMenu extends ChestMenu {
      private int origin;

      StorageMenu(MenuType<?> type, int id, Inventory inv, Container view, int rows) {
         super(type, id, inv, view, rows);
      }

      private static int hz(ItemStack st) {
         return HazardItems.isHazardous(st) ? st.m_41613_() : 0;
      }

      private int inside() {
         int n = 0;

         for(int i = 0; i < this.m_39261_().m_6643_(); ++i) {
            n += hz(this.m_39261_().m_8020_(i));
         }

         return n;
      }

      private int outside() {
         int n = 0;

         for(int i = this.m_39261_().m_6643_(); i < this.f_38839_.size(); ++i) {
            n += hz(((Slot)this.f_38839_.get(i)).m_7993_());
         }

         return n;
      }

      public void m_150399_(int slotId, int button, ClickType type, Player player) {
         if (player instanceof ServerPlayer sp) {
            if (!sp.m_7500_()) {
               int insideBefore = this.inside();
               int outsideBefore = this.outside();
               int carriedBefore = hz(this.m_142621_());
               super.m_150399_(slotId, button, type, player);
               int insideDelta = this.inside() - insideBefore;
               int outsideDelta = this.outside() - outsideBefore;
               int carriedAfter = hz(this.m_142621_());
               boolean crossed = false;
               if (carriedAfter == carriedBefore) {
                  crossed = insideDelta > 0 && outsideDelta < 0 || insideDelta < 0 && outsideDelta > 0;
                  if (carriedAfter == 0) {
                     this.origin = 0;
                  }
               } else if (carriedAfter > carriedBefore) {
                  this.origin = insideDelta < 0 ? 1 : (outsideDelta < 0 ? 2 : this.origin);
               } else {
                  crossed = insideDelta > 0 && this.origin == 2 || outsideDelta > 0 && this.origin == 1;
                  if (carriedAfter == 0) {
                     this.origin = 0;
                  }
               }

               if (crossed) {
                  StomachLogic.hazardHurt(sp, "Handling something sharp inside your stomach hurts.");
               }

               return;
            }
         }

         super.m_150399_(slotId, button, type, player);
      }
   }
}
