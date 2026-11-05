package com.example.gulp;

import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(
   modid = "gulp",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public final class ClientModEvents {
   public static final ModelLayerLocation BELLY_LAYER = new ModelLayerLocation(new ResourceLocation("gulp", "belly"), "main");
   public static final KeyMapping SWALLOW_KEY;
   public static final KeyMapping MODE_KEY;
   public static final KeyMapping RELEASE_KEY;
   public static final KeyMapping INVENTORY_SWALLOW_KEY;
   public static final KeyMapping STRUGGLE_KEY;
   public static final KeyMapping SCREEN_KEY;

   private ClientModEvents() {
   }

   @SubscribeEvent
   public static void onClientSetup(FMLClientSetupEvent e) {
      GulpConfig.load();
      ClientState.soundListener = ClientSounds::playWorld;
      ClientState.messageListener = (text) -> {
         if (GulpConfig.actionMessages) {
            Minecraft mc = Minecraft.m_91087_();
            if (mc.f_91074_ != null) {
               mc.f_91074_.m_5661_(text, true);
            }

         }
      };
      ClientState.levelUpListener = (level, points) -> {
         if (GulpConfig.levelUpMessages) {
            Minecraft mc = Minecraft.m_91087_();
            mc.f_91065_.m_93076_().m_93785_(Component.m_237113_("Stomach reached level " + level + "! You have " + points + " perk point" + (points == 1 ? "" : "s") + " to spend (open the stomach screen)."));
         }
      };
   }

   @SubscribeEvent
   public static void registerKeys(RegisterKeyMappingsEvent e) {
      e.register(SWALLOW_KEY);
      e.register(MODE_KEY);
      e.register(RELEASE_KEY);
      e.register(STRUGGLE_KEY);
      e.register(SCREEN_KEY);
      e.register(INVENTORY_SWALLOW_KEY);
   }

   @SubscribeEvent
   public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
      e.registerLayerDefinition(BELLY_LAYER, BellyModel::createLayer);
   }

   @SubscribeEvent
   public static void addLayers(EntityRenderersEvent.AddLayers e) {
      for(String skin : e.getSkins()) {
         EntityRenderer<? extends Player> renderer = e.getSkin(skin);
         if (renderer instanceof PlayerRenderer playerRenderer) {
            playerRenderer.m_115326_(new BellyLayer(playerRenderer, e.getEntityModels()));
         }
      }

   }

   @SubscribeEvent
   public static void registerOverlays(RegisterGuiOverlaysEvent e) {
      e.registerAboveAll("stomach", (gui, graphics, partialTick, width, height) -> GulpHud.render(graphics, width, height));
   }

   static {
      SWALLOW_KEY = new KeyMapping("key.gulp.swallow", Type.KEYSYM, 86, "key.category.gulp");
      MODE_KEY = new KeyMapping("key.gulp.mode", Type.KEYSYM, 66, "key.category.gulp");
      RELEASE_KEY = new KeyMapping("key.gulp.release", Type.KEYSYM, 71, "key.category.gulp");
      INVENTORY_SWALLOW_KEY = new KeyMapping("key.gulp.inventory_swallow", KeyConflictContext.GUI, Type.KEYSYM, 86, "key.category.gulp");
      STRUGGLE_KEY = new KeyMapping("key.gulp.struggle", Type.KEYSYM, 82, "key.category.gulp");
      SCREEN_KEY = new KeyMapping("key.gulp.screen", Type.KEYSYM, 78, "key.category.gulp");
   }
}
