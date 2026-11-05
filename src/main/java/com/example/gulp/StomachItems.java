package com.example.gulp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.items.ItemHandlerHelper;

public final class StomachItems {
   public static final TagKey<Item> DECONSTRUCT = ItemTags.create(new ResourceLocation("gulp", "stomach_deconstruct"));
   public static final TagKey<Item> TRASH = ItemTags.create(new ResourceLocation("gulp", "stomach_trash"));
   public static final int MEND_INTERVAL = 100;
   public static final int SMELT_INTERVAL = 80;
   private static final TagKey<Item> RAW_MATERIALS = ItemTags.create(new ResourceLocation("forge", "raw_materials"));
   private static final TagKey<Item> ORES = ItemTags.create(new ResourceLocation("forge", "ores"));
   public static final int DIGEST_THRESHOLD = 32;
   private static final int DECON_INTERVAL = 60;
   private static final int TRASH_PER_PASS = 4;
   private static final Map<Item, List<ItemStack>> PARTS = new HashMap();
   private static RecipeManager cachedFor;
   private static final int XP_PER_ITEM = 2;
   private static Map<Item, List<ItemStack>> overrideMap;

   public static int smeltPerPass(int rank) {
      return rank <= 0 ? 0 : 1 + (rank - 1) / 2;
   }

   public static int smeltYield(int rank) {
      return rank <= 0 ? 0 : 1 + (rank - 1) / 2;
   }

   public static int hungerPoints(int rank) {
      return 1 + Math.max(0, rank);
   }

   public static float hungerBars(int rank) {
      return (float)hungerPoints(rank) / 2.0F;
   }

   private StomachItems() {
   }

   public static void tick(ServerPlayer p, StomachManager mgr, Stomach s, boolean hardNow) {
      if ((Boolean)GulpServerConfig.ITEM_STORAGE_ENABLED.get()) {
         if (p.m_6084_() && !p.m_5833_() && !PlayerHolding.isHeld(p)) {
            if (p.f_36096_ == p.f_36095_) {
               boolean changed = false;
               if (hardNow && (Boolean)GulpServerConfig.ITEM_DIGESTION_ENABLED.get() && p.f_19797_ % 60 == 0) {
                  changed |= breakDownOne(p, s);
               }

               int mend = s.rank(Perk.MENDING);
               if (mend > 0 && p.f_19797_ % 100 == 0) {
                  int amount = mend * 2;

                  for(int i = 0; i < s.storageSlots(); ++i) {
                     ItemStack stack = (ItemStack)s.storage.get(i);
                     if (!stack.m_41619_() && stack.m_41768_()) {
                        stack.m_41721_(Math.max(0, stack.m_41773_() - amount));
                        changed = true;
                     }
                  }
               }

               int smelt = s.rank(Perk.SMELTING);
               if (smelt > 0 && (Boolean)GulpServerConfig.SMELTING_GUT_ENABLED.get() && p.f_19797_ % 80 == 0) {
                  changed |= smeltSome(p, s, smelt);
               }

               if (changed) {
                  mgr.m_77762_();
                  StomachLogic.sync(p, s);
               }

            }
         }
      }
   }

   private static boolean smeltSome(ServerPlayer p, Stomach s, int rank) {
      int budget = smeltPerPass(rank);
      int yield = smeltYield(rank);
      MinecraftServer server = p.m_20194_();
      boolean any = false;
      int slots = s.storageSlots();

      for(int i = 0; i < slots && budget > 0; ++i) {
         ItemStack stack = (ItemStack)s.storage.get(i);
         if (!stack.m_41619_() && !stack.m_41782_() && (stack.m_204117_(RAW_MATERIALS) || stack.m_204117_(ORES))) {
            ItemStack out = smeltResult(server, stack);
            if (!out.m_41619_()) {
               while(budget > 0 && !stack.m_41619_()) {
                  ItemStack product = out.m_41777_();
                  product.m_41764_(Math.min(product.m_41741_(), Math.max(1, out.m_41613_()) * yield));
                  if (!canFit(s, product, stack)) {
                     return any;
                  }

                  stack.m_41774_(1);
                  if (stack.m_41619_()) {
                     s.storage.set(i, ItemStack.f_41583_);
                  }

                  ItemStack left = s.insertItem(product);
                  if (!left.m_41619_()) {
                     ItemHandlerHelper.giveItemToPlayer(p, left);
                  }

                  --budget;
                  any = true;
               }
            }
         }
      }

      return any;
   }

   private static ItemStack smeltResult(MinecraftServer server, ItemStack ore) {
      RecipeManager rm = server.m_129894_();
      SimpleContainer box = new SimpleContainer(new ItemStack[]{single(ore)});
      RegistryAccess access = server.m_206579_();
      Optional<BlastingRecipe> blast = rm.m_44015_(RecipeType.f_44109_, box, server.m_129783_());
      if (blast.isPresent()) {
         return ((BlastingRecipe)blast.get()).m_8043_(access).m_41777_();
      } else {
         Optional<SmeltingRecipe> smelt = rm.m_44015_(RecipeType.f_44108_, box, server.m_129783_());
         return (ItemStack)smelt.map((r) -> r.m_8043_(access).m_41777_()).orElse(ItemStack.f_41583_);
      }
   }

   private static ItemStack single(ItemStack ore) {
      ItemStack one = ore.m_41777_();
      one.m_41764_(1);
      return one;
   }

   private static boolean canFit(Stomach s, ItemStack product, ItemStack source) {
      int need = product.m_41613_();
      int slots = s.storageSlots();

      for(int i = 0; i < slots && need > 0; ++i) {
         ItemStack in = (ItemStack)s.storage.get(i);
         if (!in.m_41619_() && (in != source || source.m_41613_() != 1)) {
            if (ItemStack.m_150942_(in, product)) {
               need -= in.m_41741_() - in.m_41613_();
            }
         } else {
            need -= product.m_41741_();
         }
      }

      return need <= 0;
   }

   public static void digestFirstSlot(ServerPlayer p, Stomach s, SimpleContainer view, int count) {
      if (view.m_8020_(0).m_41619_()) {
         shiftForward(view);
      }

      digestSlot(p, s, view, 0, count);
      if (view.m_8020_(0).m_41619_()) {
         shiftForward(view);
      }

   }

   private static void shiftForward(SimpleContainer view) {
      int size = view.m_6643_();

      int gap;
      for(gap = 0; gap < size && view.m_8020_(gap).m_41619_(); ++gap) {
      }

      if (gap != 0 && gap < size) {
         for(int i = 0; i + gap < size; ++i) {
            view.m_6836_(i, view.m_8020_(i + gap));
         }

         for(int i = size - gap; i < size; ++i) {
            view.m_6836_(i, ItemStack.f_41583_);
         }

         view.m_6596_();
      }
   }

   public static void digestSlot(ServerPlayer p, Stomach s, SimpleContainer view, int slot, int count) {
      if (slot >= 0 && slot < view.m_6643_()) {
         ItemStack stack = view.m_8020_(slot);
         if (stack.m_41619_()) {
            StomachLogic.say(p, Component.m_237113_("That slot is empty."));
         } else if (!(Boolean)GulpServerConfig.UNIVERSAL_DIGESTION_ENABLED.get()) {
            StomachLogic.say(p, Component.m_237113_("Eating non-food items is turned off on this server."));
         } else if (!stack.m_41782_() || !stack.m_41783_().m_128441_("BlockEntityTag") && !stack.m_41783_().m_128441_("Items")) {
            if (stack.m_41793_() && !(Boolean)GulpServerConfig.DECONSTRUCT_ENCHANTED.get()) {
               StomachLogic.say(p, Component.m_237113_("Enchanted items are protected on this server."));
            } else {
               if (breakable(stack)) {
                  List<ItemStack> parts = partsOf(p.m_20194_(), stack.m_41720_());
                  if (parts != null) {
                     breakDownSlot(p, s, view, slot, count, parts);
                     return;
                  }
               }

               int n = Math.max(1, Math.min(Math.min(count, 64), stack.m_41613_()));
               Component name = stack.m_41786_();
               FoodProperties food = stack.getFoodProperties(p);
               int nutrition = food == null ? 0 : food.m_38744_();
               view.m_7407_(slot, n);
               int rank = s.rank(Perk.UNIVERSAL);
               s.digestProgress += n;
               int payouts = s.digestProgress / 32;
               s.digestProgress %= 32;
               int hunger = payouts * hungerPoints(rank);
               if (hunger > 0) {
                  p.m_36324_().m_38707_(hunger, 0.0F);
               }

               float xp = (float)(n * (2 + nutrition));
               StomachLogic.gainXp(p, s, xp);
               Component msg = Component.m_237113_("Ate " + n + "x ").m_7220_(name);
               MutableComponent var16;
               if (hunger > 0) {
                  var16 = msg.m_6881_().m_7220_(Component.m_237113_(String.format(" (+%.1f hunger)", (float)hunger / 2.0F)));
               } else {
                  var16 = msg.m_6881_().m_7220_(Component.m_237113_(" (" + s.digestProgress + "/32)"));
               }

               StomachLogic.say(p, var16);
               StomachLogic.sound(p, GulpSound.DIGEST_BUTTON);
               p.f_36096_.m_38946_();
            }
         } else {
            StomachLogic.say(p, Component.m_237113_("Can't digest something that holds other items."));
         }
      }
   }

   private static void breakDownSlot(ServerPlayer p, Stomach s, SimpleContainer view, int slot, int count, List<ItemStack> parts) {
      ItemStack stack = view.m_8020_(slot);
      int n = Math.max(1, Math.min(Math.min(count, 64), stack.m_41613_()));
      Component name = stack.m_41786_();
      double condition = (double)1.0F - (double)stack.m_41773_() / (double)Math.max(1, stack.m_41776_());
      view.m_7407_(slot, n);

      for(int i = 0; i < n; ++i) {
         for(ItemStack part : parts) {
            int amount = Math.max(1, (int)Math.round((double)part.m_41613_() * ((double)0.5F + (double)0.5F * condition)));
            ItemStack left = view.m_19173_(new ItemStack(part.m_41720_(), amount));
            if (!left.m_41619_()) {
               ItemHandlerHelper.giveItemToPlayer(p, left);
            }
         }
      }

      StomachLogic.gainXp(p, s, (float)(n * 2));
      StomachLogic.say(p, Component.m_237113_("Broke down " + n + "x ").m_7220_(name));
      StomachLogic.sound(p, GulpSound.DIGEST_BUTTON);
      p.f_36096_.m_38946_();
   }

   private static boolean breakDownOne(ServerPlayer p, Stomach s) {
      int slots = s.storageSlots();
      MinecraftServer server = p.m_20194_();

      for(int i = 0; i < slots; ++i) {
         ItemStack stack = (ItemStack)s.storage.get(i);
         if (!stack.m_41619_() && breakable(stack) && (!stack.m_41793_() || (Boolean)GulpServerConfig.DECONSTRUCT_ENCHANTED.get())) {
            List<ItemStack> parts = partsOf(server, stack.m_41720_());
            if (parts != null) {
               double condition = (double)1.0F - (double)stack.m_41773_() / (double)Math.max(1, stack.m_41776_());
               Component name = stack.m_41786_();
               stack.m_41774_(1);
               if (stack.m_41619_()) {
                  s.storage.set(i, ItemStack.f_41583_);
               }

               for(ItemStack part : parts) {
                  int count = Math.max(1, (int)Math.round((double)part.m_41613_() * ((double)0.5F + (double)0.5F * condition)));
                  stash(p, s, new ItemStack(part.m_41720_(), count));
               }

               StomachLogic.say(p, Component.m_237113_("Broke down ").m_7220_(name));
               return true;
            }
         }
      }

      if (!(Boolean)GulpServerConfig.TRASH_DISSOLVE_ENABLED.get()) {
         return false;
      } else {
         for(int i = 0; i < slots; ++i) {
            ItemStack stack = (ItemStack)s.storage.get(i);
            if (!stack.m_41619_() && stack.m_204117_(TRASH) && !stack.m_41782_()) {
               stack.m_41774_(4);
               if (stack.m_41619_()) {
                  s.storage.set(i, ItemStack.f_41583_);
               }

               return true;
            }
         }

         return false;
      }
   }

   private static boolean breakable(ItemStack stack) {
      return stack.m_41763_() || stack.m_204117_(DECONSTRUCT) || overrides().containsKey(stack.m_41720_());
   }

   private static Map<Item, List<ItemStack>> overrides() {
      if (overrideMap != null) {
         return overrideMap;
      } else {
         Map<Item, List<ItemStack>> m = new HashMap();
         m.put(Items.f_42450_, List.of(new ItemStack(Items.f_42454_, 5)));
         m.put(Items.f_42654_, List.of(new ItemStack(Items.f_42454_, 7)));
         m.put(Items.f_42651_, List.of(new ItemStack(Items.f_42416_, 7)));
         m.put(Items.f_42652_, List.of(new ItemStack(Items.f_42417_, 7)));
         m.put(Items.f_42653_, List.of(new ItemStack(Items.f_42415_, 7)));
         netherite(m, Items.f_42393_, Items.f_42388_);
         netherite(m, Items.f_42395_, Items.f_42390_);
         netherite(m, Items.f_42396_, Items.f_42391_);
         netherite(m, Items.f_42394_, Items.f_42389_);
         netherite(m, Items.f_42397_, Items.f_42392_);
         netherite(m, Items.f_42480_, Items.f_42472_);
         netherite(m, Items.f_42481_, Items.f_42473_);
         netherite(m, Items.f_42482_, Items.f_42474_);
         netherite(m, Items.f_42483_, Items.f_42475_);
         m.put(Items.f_42147_, List.of(new ItemStack(Items.f_41913_, 2), new ItemStack(Items.f_42416_, 3)));
         m.put(Items.f_42148_, List.of(new ItemStack(Items.f_41913_, 1), new ItemStack(Items.f_42416_, 3)));
         overrideMap = m;
         return m;
      }
   }

   private static void netherite(Map<Item, List<ItemStack>> m, Item netherite, Item diamond) {
      m.put(netherite, List.of(new ItemStack(diamond, 1), new ItemStack(Items.f_42418_, 1)));
   }

   private static void stash(ServerPlayer p, Stomach s, ItemStack stack) {
      ItemStack left = s.insertItem(stack);
      if (!left.m_41619_()) {
         ItemHandlerHelper.giveItemToPlayer(p, left);
      }

   }

   private static List<ItemStack> partsOf(MinecraftServer server, Item item) {
      List<ItemStack> fixed = (List)overrides().get(item);
      if (fixed != null) {
         return fixed;
      } else {
         RecipeManager rm = server.m_129894_();
         if (rm != cachedFor) {
            PARTS.clear();
            cachedFor = rm;
         }

         if (PARTS.containsKey(item)) {
            return (List)PARTS.get(item);
         } else {
            List<ItemStack> result = null;
            RegistryAccess access = server.m_206579_();

            for(CraftingRecipe recipe : rm.m_44013_(RecipeType.f_44107_)) {
               if (!(recipe instanceof CustomRecipe)) {
                  ItemStack out = recipe.m_8043_(access);
                  if (!out.m_41619_() && out.m_41720_() == item) {
                     Map<Item, Integer> counts = new LinkedHashMap();
                     boolean selfReference = false;

                     for(Ingredient ingredient : recipe.m_7527_()) {
                        if (!ingredient.m_43947_()) {
                           ItemStack[] options = ingredient.m_43908_();
                           if (options.length != 0) {
                              Item pick = options[0].m_41720_();
                              if (pick == item) {
                                 selfReference = true;
                              }

                              counts.merge(pick, 1, Integer::sum);
                           }
                        }
                     }

                     if (!selfReference && !counts.isEmpty()) {
                        int perCraft = Math.max(1, out.m_41613_());
                        List<ItemStack> parts = new ArrayList();

                        for(Map.Entry<Item, Integer> e : counts.entrySet()) {
                           parts.add(new ItemStack((ItemLike)e.getKey(), Math.max(1, (Integer)e.getValue() / perCraft)));
                        }

                        result = parts;
                        break;
                     }
                  }
               }
            }

            PARTS.put(item, result);
            return result;
         }
      }
   }
}
