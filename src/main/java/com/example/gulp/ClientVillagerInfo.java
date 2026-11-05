package com.example.gulp;

import java.util.HashMap;
import java.util.Map;

public final class ClientVillagerInfo {
   private static final Map<Integer, VillagerInfoPacket> INFO = new HashMap();

   private ClientVillagerInfo() {
   }

   public static void put(VillagerInfoPacket m) {
      INFO.put(m.entityId, m);
   }

   public static VillagerInfoPacket get(int entityId) {
      return (VillagerInfoPacket)INFO.get(entityId);
   }

   public static void clear() {
      INFO.clear();
   }
}
