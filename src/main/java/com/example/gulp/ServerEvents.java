package com.example.gulp;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.server.ServerLifecycleHooks;

@EventBusSubscriber(
   modid = "gulp"
)
public final class ServerEvents {
   private ServerEvents() {
   }

   @SubscribeEvent
   public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
      if (e.phase == Phase.END && e.side == LogicalSide.SERVER) {
         Player var2 = e.player;
         if (var2 instanceof ServerPlayer) {
            ServerPlayer p = (ServerPlayer)var2;
            PlayerHolding.tickHeld(p);
            StomachManager mgr = StomachManager.get(p.m_20194_());
            StomachLogic.tickPlayer(mgr, p, mgr.of(p.m_20148_()));
         }
      }
   }

   @SubscribeEvent
   public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
      Player var2 = e.getEntity();
      if (var2 instanceof ServerPlayer p) {
         PlayerHolding.onLogin(p);
         StomachLogic.sync(p, StomachManager.get(p.m_20194_()).of(p.m_20148_()));
         RolesPacket.broadcast(p.m_20194_());
      }
   }

   @SubscribeEvent
   public static void onPotionDrunk(LivingEntityUseItemEvent.Finish e) {
      LivingEntity var2 = e.getEntity();
      if (var2 instanceof ServerPlayer p) {
         if (e.getItem().m_41720_() instanceof PotionItem) {
            PlayerHolding.transferPotion(p, e.getItem());
            return;
         }
      }

   }

   @SubscribeEvent
   public static void onSleepFinished(SleepFinishedTimeEvent e) {
      LevelAccessor var2 = e.getLevel();
      if (var2 instanceof ServerLevel level) {
         for(ServerPlayer p : level.m_6907_()) {
            if (p.m_5803_()) {
               PlayerHolding.sleepThroughNight(p);
            }
         }

      }
   }

   @SubscribeEvent
   public static void onRegisterCommands(RegisterCommandsEvent e) {
      e.getDispatcher().register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("gulp").then(Commands.m_82127_("accept").executes((c) -> {
         VoreRequests.respond(((CommandSourceStack)c.getSource()).m_81375_(), true);
         return 1;
      }))).then(Commands.m_82127_("decline").executes((c) -> {
         VoreRequests.respond(((CommandSourceStack)c.getSource()).m_81375_(), false);
         return 1;
      }))).then(Commands.m_82127_("villagerinfo").executes((c) -> {
         McaIntegration.debugNearest(((CommandSourceStack)c.getSource()).m_81375_());
         return 1;
      }))).then(Commands.m_82127_("villagers").executes((c) -> {
         McaIntegration.listNearby(((CommandSourceStack)c.getSource()).m_81375_());
         return 1;
      }))).then(((LiteralArgumentBuilder)Commands.m_82127_("comfort").then(Commands.m_82127_("accept").executes((c) -> {
         McaIntegration.respondComfort(((CommandSourceStack)c.getSource()).m_81375_(), true);
         return 1;
      }))).then(Commands.m_82127_("decline").executes((c) -> {
         McaIntegration.respondComfort(((CommandSourceStack)c.getSource()).m_81375_(), false);
         return 1;
      })))).then(((LiteralArgumentBuilder)Commands.m_82127_("cuddle").then(Commands.m_82127_("accept").executes((c) -> {
         McaIntegration.respondCuddle(((CommandSourceStack)c.getSource()).m_81375_(), true);
         return 1;
      }))).then(Commands.m_82127_("decline").executes((c) -> {
         McaIntegration.respondCuddle(((CommandSourceStack)c.getSource()).m_81375_(), false);
         return 1;
      })))).then(((LiteralArgumentBuilder)Commands.m_82127_("carry").then(Commands.m_82127_("accept").executes((c) -> {
         McaIntegration.respondCarry(((CommandSourceStack)c.getSource()).m_81375_(), true);
         return 1;
      }))).then(Commands.m_82127_("decline").executes((c) -> {
         McaIntegration.respondCarry(((CommandSourceStack)c.getSource()).m_81375_(), false);
         return 1;
      }))));
   }

   @SubscribeEvent
   public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
      Player var2 = e.getEntity();
      if (var2 instanceof ServerPlayer p) {
         PlayerHolding.onLogout(p);
      }

   }

   @SubscribeEvent
   public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent e) {
      Player var2 = e.getEntity();
      if (var2 instanceof ServerPlayer p) {
         PlayerHolding.releaseAllHeldBy(p);
      }

   }

   @SubscribeEvent
   public static void onVillagerClick(PlayerInteractEvent.EntityInteract e) {
      refuseIfAfraid(e, e.getTarget());
   }

   @SubscribeEvent
   public static void onVillagerClickAt(PlayerInteractEvent.EntityInteractSpecific e) {
      refuseIfAfraid(e, e.getTarget());
   }

   private static void refuseIfAfraid(PlayerInteractEvent e, Entity target) {
      if (!e.getLevel().m_5776_() && e.getHand() == InteractionHand.MAIN_HAND) {
         Player var3 = e.getEntity();
         if (var3 instanceof ServerPlayer) {
            ServerPlayer p = (ServerPlayer)var3;
            Component refusal = McaIntegration.refusal(p, target);
            if (refusal == null) {
               VillagerXp.onInteract(p, target);
            } else {
               if (McaIntegration.shouldTell(p)) {
                  StomachLogic.say(p, refusal);
               }

               e.setCanceled(true);
               e.setCancellationResult(InteractionResult.SUCCESS);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onServerTick(TickEvent.ServerTickEvent e) {
      if (e.phase == Phase.END) {
         MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
         if (server != null && server.m_129921_() % 10 == 0) {
            McaIntegration.tickScared(server);
         }

         if (server != null && server.m_129921_() % 100 == 0) {
            McaIntegration.tickAffection(server);
         }

         if (server != null && server.m_129921_() % 100 == 20) {
            McaIntegration.tickComfort(server);
         }

         if (server != null && server.m_129921_() % 100 == 70) {
            McaIntegration.tickCuddle(server);
         }

         if (server != null && server.m_129921_() % 100 == 45) {
            McaIntegration.tickHostile(server);
         }

         if (server != null && server.m_129921_() % 20 == 5) {
            VillagerXp.tick(server);
         }

         if (server != null && server.m_129921_() % 100 == 50) {
            RolesPacket.broadcast(server);
         }

      }
   }

   @SubscribeEvent
   public static void onServerStopping(ServerStoppingEvent e) {
      PlayerHolding.onServerStopping(e.getServer());
   }

   @SubscribeEvent
   public static void onDeath(LivingDeathEvent e) {
      LivingEntity var2 = e.getEntity();
      if (var2 instanceof ServerPlayer p) {
         StomachLogic.releaseAll(p);
      }

   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onLethalDamage(LivingDamageEvent e) {
      LivingEntity var2 = e.getEntity();
      if (var2 instanceof ServerPlayer p) {
         if (!(p.m_21223_() - e.getAmount() > 0.0F)) {
            if (!p.m_21205_().m_150930_(Items.f_42747_) && !p.m_21206_().m_150930_(Items.f_42747_)) {
               StomachLogic.stomachItemsToInventory(p);
               return;
            }

            return;
         }
      }

   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onDeathStomachItems(LivingDeathEvent e) {
      LivingEntity var2 = e.getEntity();
      if (var2 instanceof ServerPlayer p) {
         StomachLogic.stomachItemsToInventory(p);
      }

   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onPlayerDrops(LivingDropsEvent e) {
      LivingEntity var2 = e.getEntity();
      if (var2 instanceof ServerPlayer p) {
         StomachLogic.addStomachDrops(p, e.getDrops());
      }

   }

   @SubscribeEvent
   public static void onDigestDrops(LivingDropsEvent e) {
      if (StomachLogic.digestingPlayer() != null && e.getEntity() == StomachLogic.digestingMob()) {
         StomachLogic.giveDrops(e.getDrops());
         e.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onDigestXp(LivingExperienceDropEvent e) {
      ServerPlayer player = StomachLogic.digestingPlayer();
      if (player != null && e.getEntity() == StomachLogic.digestingMob()) {
         if (e.getDroppedExperience() > 0) {
            player.m_6756_(e.getDroppedExperience());
         }

         e.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void onLooting(LootingLevelEvent e) {
      int bonus = StomachLogic.pendingLooting();
      if (bonus > 0 && e.getDamageSource() != null && e.getDamageSource().m_7639_() instanceof ServerPlayer) {
         e.setLootingLevel(e.getLootingLevel() + bonus);
      }

   }
}
