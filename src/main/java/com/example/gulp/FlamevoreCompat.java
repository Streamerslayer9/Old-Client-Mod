package com.example.gulp;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import org.slf4j.Logger;

public final class FlamevoreCompat {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String MATCH = "flamevore";
   private static Boolean loaded;
   private static String modId = "";

   private FlamevoreCompat() {
   }

   public static boolean isLoaded() {
      if (loaded == null) {
         ModList list = ModList.get();
         if (list == null) {
            return false;
         }

         detect(list);
      }

      return loaded;
   }

   public static String modId() {
      isLoaded();
      return modId;
   }

   private static void detect(ModList list) {
      List<String> all = new ArrayList();
      String found = null;

      for(IModInfo info : list.getMods()) {
         String id = info.getModId();
         all.add(id);
         if (found == null && !"gulp".equals(id) && (id.toLowerCase(Locale.ROOT).contains("flamevore") || String.valueOf(info.getDisplayName()).toLowerCase(Locale.ROOT).contains("flamevore"))) {
            found = id;
         }
      }

      loaded = found != null;
      modId = found != null ? found : "";
      if (found != null) {
         LOGGER.info("[Gulp] Flamevore found (mod id '{}'): the swallowing system choice is on.", found);
      } else {
         LOGGER.info("[Gulp] Flamevore not found among the installed mods: {}", String.join(", ", all));
      }

   }
}
