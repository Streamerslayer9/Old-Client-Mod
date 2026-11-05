package com.example.gulp;

import java.util.Locale;
import net.minecraft.nbt.CompoundTag;

public enum Organ {
   STOMACH("Stomach"),
   BREAST("Breast"),
   COCK("Cock"),
   UNBIRTH("Unbirth"),
   TAIL("Tail");

   public static final int COUNT = values().length;
   public static final int STOMACH_ID = 0;
   public static final int UNBIRTH_ID = 3;
   public static final int TAIL_ID = 4;
   public final String label;

   private Organ(String label) {
      this.label = label;
   }

   public String lower() {
      return this.label.toLowerCase(Locale.ROOT);
   }

   public static Organ byId(int id) {
      return id >= 0 && id < COUNT ? values()[id] : STOMACH;
   }

   public static int of(CompoundTag entry) {
      int o = entry.m_128451_("Organ");
      return o >= 0 && o < COUNT ? o : 0;
   }

   // $FF: synthetic method
   private static Organ[] $values() {
      return new Organ[]{STOMACH, BREAST, COCK, UNBIRTH, TAIL};
   }
}
