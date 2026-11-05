package com.example.gulp;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "gulp",
   value = {Dist.CLIENT}
)
public final class ClientRoleTags {
   private ClientRoleTags() {
   }

   @SubscribeEvent
   public static void onNameFormat(PlayerEvent.NameFormat e) {
      Player p = e.getEntity();
      if (p.m_9236_().f_46443_ && GulpConfig.showRoleTags) {
         if (ClientState.usesGulpSwallow()) {
            Role role = ClientState.roleOf(p.m_20148_());
            if (role != null) {
               e.setDisplayname(Component.m_237119_().m_7220_(e.getDisplayname()).m_130946_(" ").m_7220_(Component.m_237113_("[" + role.label + "]").m_130938_((st) -> st.m_178520_(role.color & 16777215))));
            }
         }
      }
   }

   @SubscribeEvent
   public static void onClientTick(TickEvent.ClientTickEvent e) {
      if (e.phase == Phase.END) {
         ClientLevel level = Minecraft.m_91087_().f_91073_;
         if (level != null && level.m_46467_() % 20L == 0L) {
            refreshAll();
         }

      }
   }

   @SubscribeEvent
   public static void onClientCommands(RegisterClientCommandsEvent e) {
      e.getDispatcher().register((LiteralArgumentBuilder)Commands.m_82127_("gulproles").executes((c) -> {
         ClientLevel level = Minecraft.m_91087_().f_91073_;
         if (level == null) {
            return 0;
         } else {
            for(Player p : level.m_6907_()) {
               Role role = ClientState.roleOf(p.m_20148_());
               ((CommandSourceStack)c.getSource()).m_288197_(() -> {
                  String var10000 = p.m_7755_().getString();
                  return Component.m_237113_(var10000 + "  uuid " + String.valueOf(p.m_20148_()) + "  role " + (role == null ? "UNKNOWN (the server hasn't sent it)" : role.label) + "  tags " + (GulpConfig.showRoleTags ? "on" : "off"));
               }, false);
            }

            return 1;
         }
      }));
   }

   public static void refreshAll() {
      ClientLevel level = Minecraft.m_91087_().f_91073_;
      if (level != null) {
         for(Player p : level.m_6907_()) {
            p.refreshDisplayName();
         }

      }
   }
}
