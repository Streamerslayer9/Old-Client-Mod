package com.example.gulp;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.slf4j.Logger;

public final class McaSkins {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static boolean resolved;
   private static boolean ok;
   private static Method useVillagerRenderer;
   private static Method getVillager;
   private static Method getSkin;
   private static Method getColor;
   private static Method canUse;
   private static Class<?> skinLayerClass;
   private static Class<?> clothingLayerClass;
   private static final Map<Class<?>, Method> HAS_CUSTOM_SKIN = new HashMap();
   private static Field layersField;

   private McaSkins() {
   }

   private static boolean ready() {
      if (resolved) {
         return ok;
      } else {
         resolved = true;

         try {
            if (!ModList.get().isLoaded("mca")) {
               return false;
            }

            Class<?> client = McaIntegration.mcaClass("net.mca.MCAClient");
            useVillagerRenderer = client.getMethod("useVillagerRenderer", UUID.class);
            getVillager = McaIntegration.mcaClass("net.mca.client.model.CommonVillagerModel").getMethod("getVillager", Entity.class);
            skinLayerClass = McaIntegration.mcaClass("net.mca.client.render.layer.SkinLayer");
            clothingLayerClass = McaIntegration.mcaClass("net.mca.client.render.layer.ClothingLayer");
            Class<?> base = McaIntegration.mcaClass("net.mca.client.render.layer.VillagerLayer");
            getSkin = byName(base, "getSkin", 1);
            getColor = byName(base, "getColor", 2);
            canUse = byName(base, "canUse", 1);
            layersField = ObfuscationReflectionHelper.findField(LivingEntityRenderer.class, "layers");
            ok = true;
         } catch (Throwable t) {
            ok = false;
            LOGGER.warn("Vore Mod: MCA is installed but its skin layers aren't what I expected, so the belly will use the normal skin ({}).", t.toString());
         }

         return ok;
      }
   }

   private static Method byName(Class<?> c, String name, int parameters) throws NoSuchMethodException {
      for(Method m : c.getMethods()) {
         if (m.getName().equals(name) && m.getParameterCount() == parameters) {
            return m;
         }
      }

      String var10002 = c.getName();
      throw new NoSuchMethodException(var10002 + "." + name);
   }

   public static List<Object> findLayers(Object renderer) {
      List<Object> found = new ArrayList();
      if (ready() && renderer instanceof LivingEntityRenderer) {
         try {
            Object layers = layersField.get(renderer);
            if (layers instanceof List) {
               for(Object layer : (List)layers) {
                  if (skinLayerClass.isInstance(layer)) {
                     found.add(0, layer);
                  } else if (clothingLayerClass.isInstance(layer)) {
                     found.add(layer);
                  }
               }
            }
         } catch (Throwable t) {
            LOGGER.debug("Vore Mod: couldn't read the player renderer's layers: {}", t.toString());
         }

         return found;
      } else {
         return found;
      }
   }

   public static List<Pass> passes(AbstractClientPlayer player, List<Object> mcaLayers, float partialTick) {
      if (mcaLayers != null && !mcaLayers.isEmpty() && ready()) {
         try {
            if (!(Boolean)useVillagerRenderer.invoke((Object)null, player.m_20148_())) {
               return null;
            } else {
               Object villager = getVillager.invoke((Object)null, player);
               Method hasCustomSkin = (Method)HAS_CUSTOM_SKIN.computeIfAbsent(villager.getClass(), (c) -> {
                  try {
                     return byName(c, "hasCustomSkin", 0);
                  } catch (NoSuchMethodException var2) {
                     return null;
                  }
               });
               if (hasCustomSkin != null && (Boolean)hasCustomSkin.invoke(villager)) {
                  return null;
               } else {
                  List<Pass> passes = new ArrayList();

                  for(Object layer : mcaLayers) {
                     Object texture = getSkin.invoke(layer, player);
                     if (texture instanceof ResourceLocation) {
                        ResourceLocation location = (ResourceLocation)texture;
                        if ((Boolean)canUse.invoke(layer, location)) {
                           float[] color = (float[])getColor.invoke(layer, player, partialTick);
                           passes.add(new Pass(location, color[0], color[1], color[2], clothingLayerClass.isInstance(layer)));
                        }
                     }
                  }

                  return passes;
               }
            }
         } catch (Throwable var11) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static record Pass(ResourceLocation texture, float r, float g, float b, boolean clothing) {
   }
}
