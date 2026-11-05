package com.example.gulp;

import net.minecraft.client.gui.screens.Screen;

public final class PerkMenus {
   private PerkMenus() {
   }

   public static Screen create() {
      return (Screen)(GulpConfig.legacyPerkMenu ? new PerksScreen() : new PerkTreeScreen());
   }
}
