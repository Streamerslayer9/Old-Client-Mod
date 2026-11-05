package com.example.gulp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public final class VillagerXp {
   private static final int TALK_XP = 2;
   private static final long TALK_COOLDOWN = 6000L;
   private static final long GIFT_WINDOW = 1200L;
   private static final Map<Talk, Long> TALKED = new HashMap();
   private static final Map<UUID, Pending> PENDING = new HashMap();

   private VillagerXp() {
   }

   private static boolean enabled() {
      return (Boolean)GulpServerConfig.MCA_VILLAGER_XP_ENABLED.get();
   }

   static void onInteract(ServerPlayer p, Entity target) {
      if (McaIntegration.isAdultVillager(target)) {
         McaIntegration.stampKind(target, p);
      }

      if (enabled() && McaIntegration.isAdultVillager(target)) {
         long now = (long)p.m_20194_().m_129921_();
         Talk key = new Talk(p.m_20148_(), target.m_20148_());
         Long next = (Long)TALKED.get(key);
         if (next == null || next <= now) {
            TALKED.put(key, now + 6000L);
            VillagerHolding.addXp(target, p, 2 + VillagerHolding.rankOf(target, VillagerPerk.CHATTY), true);
            int warm = VillagerHolding.rankOf(target, VillagerPerk.WARM_WELCOME);
            if (warm > 0) {
               McaIntegration.addHearts(target, p, warm);
            }
         }

         ItemStack inHand = p.m_21205_();
         if (!inHand.m_41619_()) {
            PENDING.put(p.m_20148_(), new Pending(target.m_20148_(), inHand.m_41777_(), McaIntegration.heartsOf(target, p), now + 1200L));
         } else {
            PENDING.remove(p.m_20148_());
         }

      }
   }

   static void tick(MinecraftServer server) {
      long now = (long)server.m_129921_();
      TALKED.values().removeIf((t) -> t <= now);
      if (!PENDING.isEmpty()) {
         Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator();

         while(it.hasNext()) {
            Map.Entry<UUID, Pending> en = (Map.Entry)it.next();
            Pending pend = (Pending)en.getValue();
            ServerPlayer p = server.m_6846_().m_11259_((UUID)en.getKey());
            if (p != null && pend.expires() > now && enabled()) {
               Entity v = p.m_284548_().m_8791_(pend.villager());
               if (McaIntegration.isAdultVillager(v) && !(p.m_20280_(v) > (double)1024.0F)) {
                  ItemStack held = p.m_21205_();
                  boolean used = held.m_41619_() || !ItemStack.m_41656_(held, pend.held()) || held.m_41613_() < pend.held().m_41613_();
                  if (used) {
                     int gained = McaIntegration.heartsOf(v, p) - pend.hearts();
                     it.remove();
                     if (gained > 0) {
                        VillagerHolding.addXp(v, p, Math.min(7, 3 + gained / 4), true);
                     }
                  }
               } else {
                  it.remove();
               }
            } else {
               it.remove();
            }
         }

      }
   }

   static void onRide(ServerPlayer p, Entity villager, int heldTicks) {
      if (enabled() && McaIntegration.isAdultVillager(villager)) {
         VillagerHolding.addXp(villager, p, Math.min(10, 3 + heldTicks / 600), true);
      }
   }

   private static record Talk(UUID player, UUID villager) {
   }

   private static record Pending(UUID villager, ItemStack held, int hearts, long expires) {
   }
}
