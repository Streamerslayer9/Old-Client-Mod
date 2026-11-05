package com.example.gulp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "gulp"
)
public final class VillagerStorage {
   private static final String KEY = "gulp_storage";
   static final int MAX_ROWS = 4;
   private static final String TITLE = "Villager storage";
   private static final Map<UUID, Box> OPEN = new HashMap();

   private VillagerStorage() {
   }

   static int rowsFor(int level) {
      return Math.max(1, Math.min(4, 1 + level / 3));
   }

   private static void save(Entity villager, Container box) {
      ListTag list = new ListTag();

      for(int i = 0; i < box.m_6643_(); ++i) {
         ItemStack st = box.m_8020_(i);
         if (!st.m_41619_()) {
            CompoundTag t = new CompoundTag();
            t.m_128405_("Slot", i);
            st.m_41739_(t);
            list.add(t);
         }
      }

      VillagerData.put(villager, "gulp_storage", list);
   }

   private static void load(Entity villager, Box box) {
      box.loading = true;
      Tag var4 = VillagerData.getTag(villager, "gulp_storage");
      ListTag var10000;
      if (var4 instanceof ListTag l) {
         var10000 = l;
      } else {
         var10000 = new ListTag();
      }

      ListTag list = var10000;

      for(int i = 0; i < list.size(); ++i) {
         CompoundTag t = list.m_128728_(i);
         int slot = t.m_128451_("Slot");
         if (slot >= 0 && slot < box.m_6643_()) {
            box.m_6836_(slot, ItemStack.m_41712_(t));
         }
      }

      box.loading = false;
   }

   static void open(ServerPlayer p, int entityId) {
      if (!(Boolean)GulpServerConfig.MCA_VILLAGER_STORAGE_ENABLED.get()) {
         StomachLogic.say(p, Component.m_237113_("Villager storage is turned off on this server."));
      } else {
         Entity v = p.m_9236_().m_6815_(entityId);
         if (v != null && !(p.m_20280_(v) > (double)1024.0F)) {
            if (McaIntegration.isAdultVillager(v) && McaIntegration.isPredVillager(v)) {
               if (p.m_20280_(v) > (double)64.0F) {
                  StomachLogic.say(p, Component.m_237113_("Get closer to " + v.m_7755_().getString() + " first."));
               } else if (McaIntegration.heartsOf(v, p) < (Integer)GulpServerConfig.MCA_LOW_AFFECTION_HEARTS.get()) {
                  p.m_213846_(Component.m_237113_("<").m_7220_(v.m_7755_()).m_130946_("> " + McaIntegration.traitOf(v).storageRefuseLine()));
               } else {
                  int rows = rowsFor(VillagerHolding.levelOf(v));
                  Box box = (Box)OPEN.get(v.m_20148_());
                  if (box == null) {
                     box = new Box(v, rows * 9);
                     load(v, box);
                     OPEN.put(v.m_20148_(), box);
                  }

                  int shownRows = box.m_6643_() / 9;
                  MenuType var10000;
                  switch (shownRows) {
                     case 1 -> var10000 = MenuType.f_39957_;
                     case 2 -> var10000 = MenuType.f_39958_;
                     case 3 -> var10000 = MenuType.f_39959_;
                     default -> var10000 = MenuType.f_39960_;
                  }

                  MenuType<ChestMenu> type = var10000;
                  p.m_5893_(new SimpleMenuProvider((id, inv, player) -> new ChestMenu(type, id, inv, box, shownRows), Component.m_237113_(v.m_7755_().getString() + " - Villager storage")));
               }
            } else {
               StomachLogic.say(p, Component.m_237113_("Only adult pred villagers have storage."));
            }
         }
      }
   }

   static int stash(Entity villager, ServerPlayer p) {
      List<ItemStack> items = new ArrayList();
      Inventory inv = p.m_150109_();

      for(int i = 0; i < inv.m_6643_(); ++i) {
         ItemStack st = inv.m_8020_(i);
         if (!st.m_41619_()) {
            items.add(st.m_41777_());
         }
      }

      inv.m_6211_();
      if ((Boolean)GulpServerConfig.MCA_VILLAGER_STORAGE_ENABLED.get()) {
         Box box = (Box)OPEN.get(villager.m_20148_());
         if (box == null) {
            box = new Box(villager, rowsFor(VillagerHolding.levelOf(villager)) * 9);
            load(villager, box);
         }

         for(ItemStack st : items) {
            ItemStack left = box.m_19173_(st);
            if (!left.m_41619_()) {
               villager.m_19983_(left);
            }
         }

         return items.size();
      } else {
         for(ItemStack st : items) {
            villager.m_19983_(st);
         }

         return items.size();
      }
   }

   @SubscribeEvent
   public static void onDeath(LivingDeathEvent e) {
      Entity v = e.getEntity();
      if (!v.m_9236_().f_46443_ && McaIntegration.isVillager(v)) {
         Tag var4 = VillagerData.getTag(v, "gulp_storage");
         ListTag var10000;
         if (var4 instanceof ListTag) {
            ListTag l = (ListTag)var4;
            var10000 = l;
         } else {
            var10000 = new ListTag();
         }

         ListTag list = var10000;
         OPEN.remove(v.m_20148_());

         for(int i = 0; i < list.size(); ++i) {
            ItemStack st = ItemStack.m_41712_(list.m_128728_(i));
            if (!st.m_41619_()) {
               v.m_19983_(st);
            }
         }

         VillagerData.forget(v);
      }
   }

   private static final class Box extends SimpleContainer {
      private final Entity villager;
      private int openers;
      private boolean loading;

      Box(Entity villager, int slots) {
         super(slots);
         this.villager = villager;
      }

      public void m_6596_() {
         super.m_6596_();
         if (!this.loading) {
            VillagerStorage.save(this.villager, this);
         }

      }

      public boolean m_6542_(Player player) {
         return this.villager.m_6084_() && player.m_20280_(this.villager) <= (double)64.0F;
      }

      public void m_5856_(Player player) {
         ++this.openers;
      }

      public void m_5785_(Player player) {
         --this.openers;
         if (this.openers <= 0) {
            VillagerStorage.OPEN.remove(this.villager.m_20148_(), this);
         }

      }
   }
}
