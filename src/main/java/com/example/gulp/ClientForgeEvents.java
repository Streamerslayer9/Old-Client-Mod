package com.example.gulp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "gulp",
   value = {Dist.CLIENT}
)
public final class ClientForgeEvents {
   private static boolean choicePrompted;

   private ClientForgeEvents() {
   }

   @SubscribeEvent
   public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
      ClientState.reset();
      ClientVillagerInfo.clear();
      choicePrompted = false;
      CpmDriver.reset();
      ScreenLoop.stop();
   }

   @SubscribeEvent
   public static void onClientTick(TickEvent.ClientTickEvent e) {
      if (e.phase == Phase.END) {
         Minecraft mc = Minecraft.m_91087_();
         if (mc.f_91074_ != null) {
            CpmDriver.tick();
            if (!choicePrompted && mc.f_91080_ == null && ClientState.needsSwallowChoice()) {
               choicePrompted = true;
               mc.m_91152_(new SwallowChoiceScreen());
            }

            while(ClientModEvents.SWALLOW_KEY.m_90859_()) {
               if (ClientState.needsSwallowChoice()) {
                  if (mc.f_91080_ == null) {
                     mc.m_91152_(new SwallowChoiceScreen());
                  }
               } else {
                  Entity target = TargetFinder.find(mc.f_91074_, (double)4.0F);
                  Net.send(0, target == null ? -1 : target.m_19879_());
               }
            }

            while(ClientModEvents.STRUGGLE_KEY.m_90859_()) {
               Net.send(11);
            }

            while(ClientModEvents.MODE_KEY.m_90859_()) {
               Net.send(1);
            }

            while(ClientModEvents.RELEASE_KEY.m_90859_()) {
               Net.send(mc.f_91074_.m_6144_() ? 3 : 2);
            }

            while(ClientModEvents.SCREEN_KEY.m_90859_()) {
               if (ClientState.heldMode() > 0) {
                  if (mc.f_91080_ == null) {
                     Net.send(7);
                  }
               } else if (mc.f_91080_ == null) {
                  mc.m_91152_((Screen)(ClientState.role().canEat() ? new StomachScreen() : new SettingsScreen()));
               }
            }

         }
      }
   }
}
