package com.example.gulp;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.slf4j.Logger;

@EventBusSubscriber(
   modid = "gulp"
)
public final class VillagerTrade {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SURCHARGE_RANGE = 40;
   private static final long LINE_COOLDOWN = 2400L;
   private static final Map<UUID, List<Applied>> APPLIED = new HashMap();
   private static final Map<String, Long> LINE_SPOKEN = new HashMap();

   private VillagerTrade() {
   }

   static float priceChange(int hearts) {
      int start = (Integer)GulpServerConfig.MCA_TRADE_DISCOUNT_START_HEARTS.get();
      int full = (Integer)GulpServerConfig.MCA_TRADE_DISCOUNT_FULL_HEARTS.get();
      int low = (Integer)GulpServerConfig.MCA_LOW_AFFECTION_HEARTS.get();
      if (hearts > start) {
         float share = Math.min(1.0F, (float)(hearts - start) / (float)Math.max(1, full - start));
         return -share * (float)(Integer)GulpServerConfig.MCA_TRADE_MAX_DISCOUNT_PERCENT.get() / 100.0F;
      } else if (hearts < low) {
         float share = Math.min(1.0F, (float)(low - hearts) / 40.0F);
         return share * (float)(Integer)GulpServerConfig.MCA_TRADE_MAX_SURCHARGE_PERCENT.get() / 100.0F;
      } else {
         return 0.0F;
      }
   }

   static String describe(int hearts) {
      if (!(Boolean)GulpServerConfig.MCA_TRADE_ENABLED.get()) {
         return "off (mcaTradeEnabled is false)";
      } else {
         float change = priceChange(hearts);
         int percent = Math.round(Math.abs(change) * 100.0F);
         return percent == 0 ? "normal" : percent + (change < 0.0F ? "% cheaper" : "% dearer");
      }
   }

   @SubscribeEvent
   public static void onContainerOpen(PlayerContainerEvent.Open e) {
      Player var3 = e.getEntity();
      if (var3 instanceof ServerPlayer p) {
         AbstractContainerMenu var12 = e.getContainer();
         if (var12 instanceof MerchantMenu menu) {
            undoPrevious(p);
            if ((Boolean)GulpServerConfig.MCA_TRADE_ENABLED.get() && McaIntegration.active()) {
               Entity trader = traderOf(menu, p);
               if (trader == null) {
                  LOGGER.info("[Gulp] Trade screen opened, but I could not tell which villager it belongs to, so prices are normal.");
                  return;
               }

               if (!McaIntegration.isAdultVillager(trader)) {
                  LOGGER.info("[Gulp] Trade with {} ({}): not an adult MCA villager, so prices are normal.", trader.m_7755_().getString(), trader.getClass().getSimpleName());
                  return;
               }

               int hearts = McaIntegration.heartsOf(trader, p);
               float change = priceChange(hearts) - 0.05F * (float)VillagerHolding.rankOf(trader, VillagerPerk.HAGGLER);
               if (change == 0.0F) {
                  LOGGER.info("[Gulp] Trade with {}: {} hearts is in the normal price range (discount from {} hearts, surcharge below {}).", new Object[]{trader.m_7755_().getString(), hearts, GulpServerConfig.MCA_TRADE_DISCOUNT_START_HEARTS.get(), GulpServerConfig.MCA_LOW_AFFECTION_HEARTS.get()});
                  return;
               }

               MerchantOffers offers = menu.m_40075_();
               if (offers == null) {
                  return;
               }

               List<Applied> done = new ArrayList();

               for(MerchantOffer offer : offers) {
                  int base = offer.m_45352_().m_41613_();
                  int diff;
                  if (change < 0.0F) {
                     diff = -((int)Math.max(base >= 2 ? (double)1.0F : (double)0.0F, Math.floor((double)((float)base * -change))));
                  } else {
                     diff = (int)Math.floor((double)((float)base * change) + (double)0.5F);
                  }

                  if (diff != 0) {
                     offer.m_45353_(diff);
                     done.add(new Applied(offer, diff));
                  }
               }

               if (!done.isEmpty()) {
                  APPLIED.put(p.m_20148_(), done);
               }

               LOGGER.info("[Gulp] Trade with {}: {} hearts -> prices {}", new Object[]{trader.m_7755_().getString(), hearts, describe(hearts)});
               speak(p, trader, change);
               return;
            }

            return;
         }
      }

   }

   private static Entity traderOf(MerchantMenu menu, ServerPlayer p) {
      try {
         for(Field f : MerchantMenu.class.getDeclaredFields()) {
            if (Merchant.class.isAssignableFrom(f.getType())) {
               f.setAccessible(true);
               Object var7 = f.get(menu);
               if (var7 instanceof Entity) {
                  Entity en = (Entity)var7;
                  return en;
               }
            }
         }
      } catch (Throwable t) {
         LOGGER.warn("[Gulp] Could not read the trader from the trading menu: {}", t.toString());
      }

      return findTrader(p);
   }

   private static Entity findTrader(ServerPlayer p) {
      Iterator var1 = p.m_9236_().m_6249_(p, p.m_20191_().m_82400_((double)16.0F), (x) -> {
         boolean var10000;
         if (x instanceof Merchant m) {
            if (m.m_7962_() == p) {
               var10000 = true;
               return var10000;
            }
         }

         var10000 = false;
         return var10000;
      }).iterator();
      if (var1.hasNext()) {
         Entity en = (Entity)var1.next();
         return en;
      } else {
         return null;
      }
   }

   private static void undoPrevious(ServerPlayer p) {
      List<Applied> old = (List)APPLIED.remove(p.m_20148_());
      if (old != null) {
         for(Applied a : old) {
            int now = a.offer().m_45377_();
            boolean stillThere = a.diff() < 0 ? now <= a.diff() : now >= a.diff();
            if (stillThere) {
               a.offer().m_45353_(-a.diff());
            }
         }

      }
   }

   private static void speak(ServerPlayer p, Entity trader, float change) {
      long now = (long)p.m_20194_().m_129921_();
      String var10000 = String.valueOf(p.m_20148_());
      String key = var10000 + "/" + String.valueOf(trader.m_20148_());
      Long until = (Long)LINE_SPOKEN.get(key);
      if (until == null || until <= now) {
         LINE_SPOKEN.put(key, now + 2400L);
         LINE_SPOKEN.values().removeIf((t) -> t <= now);
         VillagerTrait trait = McaIntegration.traitOf(trader);
         String line = change < 0.0F ? trait.tradeDiscountLine() : trait.tradeSurchargeLine();
         int percent = Math.round(Math.abs(change) * 100.0F);
         p.m_213846_(Component.m_237113_("<").m_7220_(trader.m_7755_()).m_130946_("> " + line + " (" + (change < 0.0F ? "-" : "+") + percent + "% prices)"));
      }
   }

   private static record Applied(MerchantOffer offer, int diff) {
   }
}
