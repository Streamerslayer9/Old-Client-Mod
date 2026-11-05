package com.example.gulp;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.slf4j.Logger;

@EventBusSubscriber(
   modid = "gulp",
   value = {Dist.CLIENT}
)
public final class FlamevoreKeyBlock {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static List<KeyMapping> eatKeys;

   private FlamevoreKeyBlock() {
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onClientTick(TickEvent.ClientTickEvent e) {
      if (e.phase == Phase.START) {
         if (FlamevoreCompat.isLoaded() && ClientState.flamevoreActive() && ClientState.swallowSystem() != SwallowSystem.FLAMEVORE) {
            Minecraft mc = Minecraft.m_91087_();
            if (mc.f_91074_ != null) {
               if (eatKeys == null) {
                  eatKeys = findEatKeys(mc);
               }

               for(KeyMapping key : eatKeys) {
                  while(key.m_90859_()) {
                  }

                  key.m_7249_(false);
               }

            }
         }
      }
   }

   private static List<KeyMapping> findEatKeys(Minecraft mc) {
      List<KeyMapping> found = new ArrayList();
      String id = FlamevoreCompat.modId().toLowerCase(Locale.ROOT);

      for(KeyMapping key : mc.f_91066_.f_92059_) {
         String name = key.m_90860_().toLowerCase(Locale.ROOT);
         String category = key.m_90858_().toLowerCase(Locale.ROOT);
         boolean theirs = name.contains("flamevore") || category.contains("flamevore") || !id.isEmpty() && (name.contains(id) || category.contains(id));
         if (theirs) {
            boolean eat = key.m_90861_().m_84873_() == 88 || name.contains("eat") || name.contains("swallow") || name.contains("devour");
            LOGGER.info("[Gulp] Flamevore key '{}' (category '{}', default {}){}", new Object[]{key.m_90860_(), key.m_90858_(), key.m_90861_().m_84874_(), eat ? " -> switched off while Gulp swallowing is chosen" : ""});
            if (eat) {
               found.add(key);
            }
         }
      }

      if (found.isEmpty()) {
         LOGGER.info("[Gulp] No Flamevore eat key found, so nothing is switched off.");
      }

      return found;
   }
}
