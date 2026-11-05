package com.example.gulp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;

public class VillagerData extends SavedData {
   private final Map<UUID, CompoundTag> map = new HashMap();

   public static VillagerData get(MinecraftServer server) {
      return (VillagerData)server.m_129783_().m_8895_().m_164861_(VillagerData::load, VillagerData::new, "gulp_villagers");
   }

   private static VillagerData of(Entity villager) {
      MinecraftServer server = villager.m_9236_().m_7654_();
      return server == null ? null : get(server);
   }

   static CompoundTag getCompound(Entity villager, String key) {
      VillagerData d = of(villager);
      if (d == null) {
         return new CompoundTag();
      } else {
         CompoundTag mine = (CompoundTag)d.map.get(villager.m_20148_());
         return mine == null ? new CompoundTag() : mine.m_128469_(key).m_6426_();
      }
   }

   static boolean getFlag(Entity villager, String key, String name) {
      VillagerData d = of(villager);
      if (d == null) {
         return false;
      } else {
         CompoundTag mine = (CompoundTag)d.map.get(villager.m_20148_());
         return mine != null && mine.m_128425_(key, 10) && mine.m_128469_(key).m_128471_(name);
      }
   }

   static Tag getTag(Entity villager, String key) {
      VillagerData d = of(villager);
      if (d == null) {
         return null;
      } else {
         CompoundTag mine = (CompoundTag)d.map.get(villager.m_20148_());
         Tag t = mine == null ? null : mine.m_128423_(key);
         return t == null ? null : t.m_6426_();
      }
   }

   static void put(Entity villager, String key, Tag tag) {
      VillagerData d = of(villager);
      if (d != null) {
         ((CompoundTag)d.map.computeIfAbsent(villager.m_20148_(), (k) -> new CompoundTag())).m_128365_(key, tag.m_6426_());
         d.m_77762_();
      }
   }

   static void remove(Entity villager, String key) {
      VillagerData d = of(villager);
      if (d != null) {
         CompoundTag mine = (CompoundTag)d.map.get(villager.m_20148_());
         if (mine != null && mine.m_128441_(key)) {
            mine.m_128473_(key);
            d.m_77762_();
         }

      }
   }

   static void forget(Entity villager) {
      VillagerData d = of(villager);
      if (d != null && d.map.remove(villager.m_20148_()) != null) {
         d.m_77762_();
      }

   }

   public CompoundTag m_7176_(CompoundTag nbt) {
      CompoundTag all = new CompoundTag();
      this.map.forEach((id, tag) -> all.m_128365_(id.toString(), tag));
      nbt.m_128365_("Villagers", all);
      return nbt;
   }

   public static VillagerData load(CompoundTag nbt) {
      VillagerData d = new VillagerData();
      CompoundTag all = nbt.m_128469_("Villagers");

      for(String key : all.m_128431_()) {
         try {
            d.map.put(UUID.fromString(key), all.m_128469_(key));
         } catch (IllegalArgumentException var6) {
         }
      }

      return d;
   }
}
