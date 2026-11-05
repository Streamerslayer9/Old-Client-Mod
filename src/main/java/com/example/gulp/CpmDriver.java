package com.example.gulp;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;

public final class CpmDriver {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String[] BELLY_ALIASES = new String[]{"belly", "stomach", "stomach_scale", "belly_scale", "tummy"};
   private static final String[] MOBS_ALIASES = new String[]{"belly_mobs", "stomach_mobs"};
   private static final String[] ITEMS_ALIASES = new String[]{"belly_items", "stomach_items"};
   private static float shownMobs;
   private static float shownItems;
   private static boolean localModelHasBelly;
   private static int ticks;
   private static final Map<String, Integer> LAST_SENT = new HashMap();
   private static final Map<String, String> RESOLVED = new HashMap();

   private CpmDriver() {
   }

   public static boolean localModelHandlesBelly() {
      return localModelHasBelly;
   }

   public static void reset() {
      shownMobs = 0.0F;
      shownItems = 0.0F;
      localModelHasBelly = false;
      ticks = 0;
      LAST_SENT.clear();
      RESOLVED.clear();
   }

   public static void tick() {
      if (GulpConfig.cpmSliders && CpmBridge.available()) {
         if (flamevoreOwnsBelly()) {
            if (!LAST_SENT.isEmpty() || !RESOLVED.isEmpty()) {
               reset();
            }

            localModelHasBelly = false;
         } else if (++ticks % 2 == 0) {
            float cap = Math.max(1.0E-4F, ClientState.cap());
            float mobTarget = clamp01(ClientState.used() / cap);
            float itemTarget = clamp01(ClientState.itemFill());
            shownMobs = ease(shownMobs, mobTarget);
            shownItems = ease(shownItems, itemTarget);
            float total = clamp01(shownMobs + 0.5F * shownItems);
            boolean resend = ticks % 200 == 0;
            boolean bellyExists = push(GulpConfig.cpmBellyName, BELLY_ALIASES, total, resend);
            boolean mobsExists = push(GulpConfig.cpmMobsName, MOBS_ALIASES, shownMobs, resend);
            boolean itemsExists = push(GulpConfig.cpmItemsName, ITEMS_ALIASES, shownItems, resend);
            localModelHasBelly = bellyExists || mobsExists || itemsExists;
         }
      } else {
         localModelHasBelly = false;
      }
   }

   private static boolean flamevoreOwnsBelly() {
      return FlamevoreCompat.isLoaded() && ClientState.flamevoreActive() && ClientState.swallowSystem() == SwallowSystem.FLAMEVORE;
   }

   private static boolean push(String configured, String[] aliases, float ratio, boolean force) {
      if (configured != null && !configured.isBlank()) {
         String name = resolve(configured, aliases);
         if (name == null) {
            return false;
         } else {
            int max = CpmBridge.maxValue(name);
            if (max < 0) {
               RESOLVED.remove(configured);
               LAST_SENT.remove(name);
               return false;
            } else {
               boolean onOff = max <= 1;
               int value = onOff ? (ratio > 0.001F ? Math.max(0, max) : 0) : Math.max(0, Math.min(max, Math.round(ratio * (float)max)));
               Integer last = (Integer)LAST_SENT.get(name);
               if (last == null && value == 0) {
                  return true;
               } else {
                  boolean again = force && !onOff && value != 0;
                  if (last == null || last != value || again) {
                     CpmBridge.play(name, value);
                     LAST_SENT.put(name, value);
                  }

                  return true;
               }
            }
         }
      } else {
         return false;
      }
   }

   private static String resolve(String configured, String[] aliases) {
      String known = (String)RESOLVED.get(configured);
      if (known != null && CpmBridge.maxValue(known) >= 0) {
         return known;
      } else if (known == null && ticks % 20 != 0) {
         return null;
      } else {
         Set<String> candidates = new LinkedHashSet(variants(configured));

         for(String alias : aliases) {
            candidates.addAll(variants(alias));
         }

         for(String candidate : candidates) {
            int max = CpmBridge.maxValue(candidate);
            if (max >= 0) {
               if (!candidate.equals(known)) {
                  LOGGER.info("Vore Mod: found a CPM belly slider named '{}' (max {}) for '{}'", new Object[]{candidate, max, configured});
               }

               RESOLVED.put(configured, candidate);
               return candidate;
            }
         }

         if (known != null) {
            RESOLVED.remove(configured);
         }

         return null;
      }
   }

   private static Set<String> variants(String name) {
      Set<String> out = new LinkedHashSet();
      String trimmed = name.trim();
      if (trimmed.isEmpty()) {
         return out;
      } else {
         for(String base : new String[]{trimmed, trimmed.replace('_', ' ')}) {
            out.add(base);
            out.add(base.toLowerCase(Locale.ROOT));
            out.add(base.toUpperCase(Locale.ROOT));
            out.add(capitalize(base));
            out.add(titleCase(base));
         }

         return out;
      }
   }

   private static String capitalize(String s) {
      String var10000 = s.substring(0, 1).toUpperCase(Locale.ROOT);
      return var10000 + s.substring(1).toLowerCase(Locale.ROOT);
   }

   private static String titleCase(String s) {
      StringBuilder sb = new StringBuilder();
      boolean startOfWord = true;

      for(char c : s.toCharArray()) {
         if (c != '_' && c != ' ') {
            sb.append(startOfWord ? Character.toUpperCase(c) : Character.toLowerCase(c));
            startOfWord = false;
         } else {
            startOfWord = true;
            sb.append(c);
         }
      }

      return sb.toString();
   }

   private static float ease(float shown, float target) {
      float next = shown + (target - shown) * 0.2F;
      return Math.abs(target - next) < 0.004F ? target : next;
   }

   private static float clamp01(float v) {
      return Math.max(0.0F, Math.min(1.0F, v));
   }
}
