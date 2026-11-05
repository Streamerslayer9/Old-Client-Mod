package com.example.gulp;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class VillagerInfoPacket {
   public final int entityId;
   public final boolean adult;
   public final int type;
   public final int trait;
   public final int[] ranks;
   public final int points;
   public final int level;
   public final int xp;
   public final int xpNeeded;

   public VillagerInfoPacket(int entityId, boolean adult, int type, int trait, int[] ranks, int points, int level, int xp, int xpNeeded) {
      this.entityId = entityId;
      this.adult = adult;
      this.type = type;
      this.trait = trait;
      this.ranks = ranks;
      this.points = points;
      this.level = level;
      this.xp = xp;
      this.xpNeeded = xpNeeded;
   }

   public static void encode(VillagerInfoPacket m, FriendlyByteBuf buf) {
      buf.writeInt(m.entityId);
      buf.writeBoolean(m.adult);
      buf.writeByte(m.type);
      buf.writeByte(m.trait);

      for(int r : m.ranks) {
         buf.writeByte(r);
      }

      buf.writeByte(m.points);
      buf.writeByte(m.level);
      buf.writeInt(m.xp);
      buf.writeInt(m.xpNeeded);
   }

   public static VillagerInfoPacket decode(FriendlyByteBuf buf) {
      int entityId = buf.readInt();
      boolean adult = buf.readBoolean();
      int type = buf.readByte();
      int trait = buf.readByte();
      int[] ranks = new int[VillagerPerk.values().length];

      for(int i = 0; i < ranks.length; ++i) {
         ranks[i] = buf.readByte();
      }

      int points = buf.readByte();
      return new VillagerInfoPacket(entityId, adult, type, trait, ranks, points, buf.readByte(), buf.readInt(), buf.readInt());
   }

   public static void handle(VillagerInfoPacket m, Supplier<NetworkEvent.Context> ctx) {
      NetworkEvent.Context c = (NetworkEvent.Context)ctx.get();
      c.enqueueWork(() -> ClientVillagerInfo.put(m));
      c.setPacketHandled(true);
   }
}
