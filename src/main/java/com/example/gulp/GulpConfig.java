package com.example.gulp;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;
import net.minecraftforge.fml.loading.FMLPaths;

public class GulpConfig {
   public static BellyStyle bellyStyle;
   public static boolean wallCustom;
   public static boolean acidCustom;
   public static float wallHue;
   public static float wallShade;
   public static float acidHue;
   public static float acidShade;
   public static StomachStyle.Pattern stomachPattern;
   public static boolean levelUpMessages;
   public static boolean showRoleTags;
   public static boolean rightClickEat;
   public static boolean actionMessages;
   public static boolean helpTooltips;
   public static boolean cpmSliders;
   public static String cpmBellyName;
   public static String cpmMobsName;
   public static String cpmItemsName;
   public static boolean legacyPerkMenu;
   public static boolean useCustomSounds;
   private static final boolean[] SOUND_ON;
   private static final float[] SOUND_VOLUME;

   public static void resetStomachLook() {
      wallCustom = false;
      acidCustom = false;
      wallHue = 0.0F;
      wallShade = 0.15F;
      acidHue = 0.09F;
      acidShade = 0.8F;
      stomachPattern = StomachStyle.Pattern.NONE;
   }

   private static float unit(Properties props, String key, float fallback) {
      try {
         return Math.max(0.0F, Math.min(1.0F, Float.parseFloat(props.getProperty(key, Float.toString(fallback)))));
      } catch (NumberFormatException var4) {
         return fallback;
      }
   }

   public static float volume(GulpSound s) {
      return SOUND_VOLUME[s.ordinal()];
   }

   public static void setVolume(GulpSound s, float v) {
      SOUND_VOLUME[s.ordinal()] = Math.max(0.0F, Math.min(2.0F, v));
   }

   public static boolean isOn(GulpSound s) {
      return SOUND_ON[s.ordinal()];
   }

   public static void setOn(GulpSound s, boolean on) {
      SOUND_ON[s.ordinal()] = on;
   }

   private static Path file() {
      return FMLPaths.CONFIGDIR.get().resolve("gulp-client.properties");
   }

   public static void load() {
      try {
         Path f = file();
         if (!Files.exists(f, new LinkOption[0])) {
            return;
         }

         Properties props = new Properties();
         InputStream in = Files.newInputStream(f);

         try {
            props.load(in);
         } catch (Throwable var8) {
            if (in != null) {
               try {
                  in.close();
               } catch (Throwable var6) {
                  var8.addSuppressed(var6);
               }
            }

            throw var8;
         }

         if (in != null) {
            in.close();
         }

         bellyStyle = GulpConfig.BellyStyle.valueOf(props.getProperty("bellyStyle", GulpConfig.BellyStyle.STOMACH.name()));
         levelUpMessages = Boolean.parseBoolean(props.getProperty("levelUpMessages", "true"));
         actionMessages = Boolean.parseBoolean(props.getProperty("actionMessages", "true"));
         showRoleTags = Boolean.parseBoolean(props.getProperty("showRoleTags", "true"));
         rightClickEat = Boolean.parseBoolean(props.getProperty("rightClickEat", "true"));
         cpmSliders = Boolean.parseBoolean(props.getProperty("cpmSliders", "true"));
         helpTooltips = Boolean.parseBoolean(props.getProperty("helpTooltips", "true"));
         cpmBellyName = props.getProperty("cpmBellyName", "belly").trim();
         cpmMobsName = props.getProperty("cpmMobsName", "belly_mobs").trim();
         cpmItemsName = props.getProperty("cpmItemsName", "belly_items").trim();
         legacyPerkMenu = Boolean.parseBoolean(props.getProperty("legacyPerkMenu", "false"));
         wallCustom = Boolean.parseBoolean(props.getProperty("wallCustom", "false"));
         acidCustom = Boolean.parseBoolean(props.getProperty("acidCustom", "false"));
         wallHue = unit(props, "wallHue", wallHue);
         wallShade = unit(props, "wallShade", wallShade);
         acidHue = unit(props, "acidHue", acidHue);
         acidShade = unit(props, "acidShade", acidShade);

         try {
            stomachPattern = StomachStyle.Pattern.valueOf(props.getProperty("stomachPattern", "NONE"));
         } catch (IllegalArgumentException var7) {
            stomachPattern = StomachStyle.Pattern.NONE;
         }

         useCustomSounds = Boolean.parseBoolean(props.getProperty("customSounds", "true"));

         for(GulpSound s : GulpSound.values()) {
            setOn(s, Boolean.parseBoolean(props.getProperty("sound." + s.name(), "true")));
            setVolume(s, Float.parseFloat(props.getProperty("volume." + s.name(), "1.0")));
         }
      } catch (Exception var9) {
      }

   }

   public static void save() {
      try {
         Properties props = new Properties();
         props.setProperty("bellyStyle", bellyStyle.name());
         props.setProperty("levelUpMessages", Boolean.toString(levelUpMessages));
         props.setProperty("actionMessages", Boolean.toString(actionMessages));
         props.setProperty("showRoleTags", Boolean.toString(showRoleTags));
         props.setProperty("rightClickEat", Boolean.toString(rightClickEat));
         props.setProperty("cpmSliders", Boolean.toString(cpmSliders));
         props.setProperty("helpTooltips", Boolean.toString(helpTooltips));
         props.setProperty("cpmBellyName", cpmBellyName);
         props.setProperty("cpmMobsName", cpmMobsName);
         props.setProperty("cpmItemsName", cpmItemsName);
         props.setProperty("legacyPerkMenu", Boolean.toString(legacyPerkMenu));
         props.setProperty("wallCustom", Boolean.toString(wallCustom));
         props.setProperty("acidCustom", Boolean.toString(acidCustom));
         props.setProperty("wallHue", Float.toString(wallHue));
         props.setProperty("wallShade", Float.toString(wallShade));
         props.setProperty("acidHue", Float.toString(acidHue));
         props.setProperty("acidShade", Float.toString(acidShade));
         props.setProperty("stomachPattern", stomachPattern.name());
         props.setProperty("customSounds", Boolean.toString(useCustomSounds));

         for(GulpSound s : GulpSound.values()) {
            props.setProperty("sound." + s.name(), Boolean.toString(isOn(s)));
            props.setProperty("volume." + s.name(), Float.toString(volume(s)));
         }

         OutputStream out = Files.newOutputStream(file());

         try {
            props.store(out, "Gulp client settings");
         } catch (Throwable var6) {
            if (out != null) {
               try {
                  out.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (out != null) {
            out.close();
         }
      } catch (Exception var7) {
      }

   }

   static {
      bellyStyle = GulpConfig.BellyStyle.STOMACH;
      wallCustom = false;
      acidCustom = false;
      wallHue = 0.0F;
      wallShade = 0.15F;
      acidHue = 0.09F;
      acidShade = 0.8F;
      stomachPattern = StomachStyle.Pattern.NONE;
      levelUpMessages = true;
      showRoleTags = true;
      rightClickEat = true;
      actionMessages = true;
      helpTooltips = true;
      cpmSliders = true;
      cpmBellyName = "belly";
      cpmMobsName = "belly_mobs";
      cpmItemsName = "belly_items";
      legacyPerkMenu = false;
      useCustomSounds = true;
      SOUND_ON = new boolean[GulpSound.values().length];
      Arrays.fill(SOUND_ON, true);
      SOUND_VOLUME = new float[GulpSound.values().length];
      Arrays.fill(SOUND_VOLUME, 1.0F);
   }

   public static enum BellyStyle {
      CLASSIC("Classic (whole torso)"),
      STOMACH("Stomach only"),
      OFF("Off (my model has one)");

      public final String label;

      private BellyStyle(String label) {
         this.label = label;
      }

      public BellyStyle next() {
         BellyStyle[] v = values();
         return v[(this.ordinal() + 1) % v.length];
      }

      // $FF: synthetic method
      private static BellyStyle[] $values() {
         return new BellyStyle[]{CLASSIC, STOMACH, OFF};
      }
   }
}
