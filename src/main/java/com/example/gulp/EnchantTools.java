package com.example.gulp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.items.ItemHandlerHelper;

final class EnchantTools {
   private EnchantTools() {
   }

   static int levelsLost(int rank) {
      return Math.max(0, 3 - rank);
   }

   static void clickBox(ServerPlayer p, Stomach s, SimpleContainer view, int box, int button, boolean shift) {
      if (box >= 0 && box <= 1) {
         if (s.rank(Perk.STRIP) < 1) {
            StomachLogic.say(p, Component.m_237113_("You need the Enchant Stripper perk."));
         } else {
            AbstractContainerMenu menu = p.f_36096_;
            ItemStack inBox = (ItemStack)s.stripSlots.get(box);
            ItemStack carried = menu.m_142621_();
            if (shift) {
               if (!inBox.m_41619_()) {
                  s.stripSlots.set(box, ItemStack.f_41583_);
                  ItemStack left = view.m_19173_(inBox);
                  if (!left.m_41619_()) {
                     ItemHandlerHelper.giveItemToPlayer(p, left);
                  }

                  view.m_6596_();
               }
            } else if (carried.m_41619_()) {
               if (!inBox.m_41619_()) {
                  menu.m_142503_(inBox);
                  s.stripSlots.set(box, ItemStack.f_41583_);
               }
            } else if (inBox.m_41619_()) {
               ItemStack placed = carried.m_41620_(button == 1 ? 1 : carried.m_41613_());
               s.stripSlots.set(box, placed);
               if (carried.m_41619_()) {
                  menu.m_142503_(ItemStack.f_41583_);
               }
            } else if (ItemStack.m_150942_(inBox, carried) && inBox.m_41613_() < inBox.m_41741_()) {
               int move = Math.min(button == 1 ? 1 : carried.m_41613_(), inBox.m_41741_() - inBox.m_41613_());
               inBox.m_41769_(move);
               carried.m_41774_(move);
               if (carried.m_41619_()) {
                  menu.m_142503_(ItemStack.f_41583_);
               }
            } else {
               s.stripSlots.set(box, carried);
               menu.m_142503_(inBox);
            }

            menu.m_150429_();
         }
      }
   }

   static void strip(ServerPlayer p, Stomach s, SimpleContainer view) {
      int rank = s.rank(Perk.STRIP);
      if (rank < 1) {
         StomachLogic.say(p, Component.m_237113_("You need the Enchant Stripper perk."));
      } else {
         ItemStack src = (ItemStack)s.stripSlots.get(0);
         ItemStack bookBox = (ItemStack)s.stripSlots.get(1);
         if (!src.m_41619_() && src.m_41613_() == 1 && !src.m_150930_(Items.f_42690_) && src.m_41793_()) {
            if (!bookBox.m_150930_(Items.f_42517_)) {
               StomachLogic.say(p, Component.m_237113_("Put a plain book in the second box. Stripping uses it up."));
            } else {
               int lost = levelsLost(rank);
               ItemStack book = new ItemStack(Items.f_42690_);
               int kept = 0;

               for(Map.Entry<Enchantment, Integer> e : EnchantmentHelper.m_44831_(src).entrySet()) {
                  int level = (Integer)e.getValue() - lost;
                  if (level >= 1) {
                     EnchantedBookItem.m_41153_(book, new EnchantmentInstance((Enchantment)e.getKey(), level));
                     ++kept;
                  }
               }

               if (kept == 0) {
                  StomachLogic.say(p, Component.m_237113_("At your rank every enchantment on this would be lost. Raise the perk first."));
               } else {
                  Component name = src.m_41786_();
                  EnchantmentHelper.m_44865_(new HashMap(), src);
                  if (bookBox.m_41613_() == 1) {
                     s.stripSlots.set(1, book);
                  } else {
                     bookBox.m_41774_(1);
                     ItemStack left = view.m_19173_(book);
                     if (!left.m_41619_()) {
                        ItemHandlerHelper.giveItemToPlayer(p, left);
                     }

                     view.m_6596_();
                  }

                  String var10001 = name.getString();
                  StomachLogic.say(p, Component.m_237113_("Stripped " + var10001 + " onto a book" + (lost > 0 ? " (-" + lost + " level" + (lost > 1 ? "s" : "") + ")" : "")));
                  StomachLogic.sound(p, GulpSound.DIGEST_BUTTON);
               }
            }
         } else {
            StomachLogic.say(p, Component.m_237113_("Put one piece of enchanted gear in the first box."));
         }
      }
   }

   static void destroy(ServerPlayer p, Stomach s) {
      if (s.rank(Perk.STRIP) < 1) {
         StomachLogic.say(p, Component.m_237113_("You need the Enchant Stripper perk."));
      } else {
         ItemStack src = (ItemStack)s.stripSlots.get(0);
         boolean book = src.m_150930_(Items.f_42690_);
         if (!src.m_41619_() && src.m_41613_() == 1 && (book || src.m_41793_())) {
            Component name = src.m_41786_();
            if (book) {
               s.stripSlots.set(0, new ItemStack(Items.f_42517_));
            } else {
               EnchantmentHelper.m_44865_(new HashMap(), src);
            }

            StomachLogic.say(p, Component.m_237113_("Destroyed the enchantments on " + name.getString() + "."));
            StomachLogic.sound(p, GulpSound.DIGEST_BUTTON);
         } else {
            StomachLogic.say(p, Component.m_237113_("Put one enchanted item in the first box."));
         }
      }
   }

   static void transfer(ServerPlayer p, Stomach s, SimpleContainer view) {
      if (s.hard && (Boolean)GulpServerConfig.HARD_MODE_ENABLED.get()) {
         StomachLogic.say(p, Component.m_237113_("Transferring enchantments only works in soft mode."));
      } else {
         int rank = s.rank(Perk.STRIP);
         if (rank < 1) {
            StomachLogic.say(p, Component.m_237113_("You need the Enchant Stripper perk."));
         } else {
            ItemStack src = (ItemStack)s.stripSlots.get(0);
            ItemStack tgt = (ItemStack)s.stripSlots.get(1);
            boolean srcBook = src.m_150930_(Items.f_42690_);
            if (!src.m_41619_() && !tgt.m_41619_() && src.m_41613_() == 1 && tgt.m_41613_() == 1 && (srcBook || src.m_41793_()) && !tgt.m_150930_(Items.f_42690_)) {
               Map<Enchantment, Integer> from = new LinkedHashMap(EnchantmentHelper.m_44831_(src));
               Map<Enchantment, Integer> to = new LinkedHashMap(EnchantmentHelper.m_44831_(tgt));
               int lost = levelsLost(rank);
               int moved = 0;
               Iterator<Map.Entry<Enchantment, Integer>> it = from.entrySet().iterator();

               while(it.hasNext()) {
                  Map.Entry<Enchantment, Integer> e = (Map.Entry)it.next();
                  Enchantment ench = (Enchantment)e.getKey();
                  int level = (Integer)e.getValue() - lost;
                  if (level >= 1 && ench.m_6081_(tgt)) {
                     boolean conflict = false;

                     for(Enchantment other : to.keySet()) {
                        if (other != ench && !ench.m_44695_(other)) {
                           conflict = true;
                        }
                     }

                     Integer existing = (Integer)to.get(ench);
                     if (!conflict && (existing == null || existing < level)) {
                        to.put(ench, level);
                        it.remove();
                        ++moved;
                     }
                  }
               }

               if (moved == 0) {
                  StomachLogic.say(p, Component.m_237113_("Nothing could move: the item can't take them, or they conflict."));
               } else {
                  EnchantmentHelper.m_44865_(to, tgt);
                  if (srcBook) {
                     src.m_41749_("StoredEnchantments");
                     if (from.isEmpty()) {
                        s.stripSlots.set(0, new ItemStack(Items.f_42517_));
                     } else {
                        for(Map.Entry<Enchantment, Integer> e : from.entrySet()) {
                           EnchantedBookItem.m_41153_(src, new EnchantmentInstance((Enchantment)e.getKey(), (Integer)e.getValue()));
                        }
                     }
                  } else {
                     EnchantmentHelper.m_44865_(from, src);
                  }

                  StomachLogic.say(p, Component.m_237113_("Moved " + moved + " enchantment" + (moved > 1 ? "s" : "") + " onto " + tgt.m_41786_().getString()));
                  StomachLogic.sound(p, GulpSound.DIGEST_BUTTON);
               }
            } else {
               StomachLogic.say(p, Component.m_237113_("Put the enchanted item in the first box and the item to enchant in the second."));
            }
         }
      }
   }
}
