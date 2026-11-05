package com.example.gulp;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

public final class HazardItems {
   public static final TagKey<Item> HAZARDOUS = ItemTags.create(new ResourceLocation("gulp", "stomach_hazardous"));

   private HazardItems() {
   }

   public static boolean isHazardous(ItemStack stack) {
      if (stack.m_41619_()) {
         return false;
      } else {
         Item item = stack.m_41720_();
         return item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem || item instanceof ShearsItem || item instanceof ArrowItem || stack.m_204117_(HAZARDOUS);
      }
   }
}
