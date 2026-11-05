package com.example.gulp;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class HeldPacket {
   public final int mode;
   public final String holder;
   public final float progress;
   public final float chance;

   public HeldPacket(int mode, String holder, float progress, float chance) {
      this.mode = mode;
      this.holder = holder;
      this.progress = progress;
      this.chance = chance;
   }

   public static void encode(HeldPacket m, FriendlyByteBuf buf) {
      buf.writeByte(m.mode);
      buf.m_130070_(m.holder);
      buf.writeFloat(m.progress);
      buf.writeFloat(m.chance);
   }

   public static HeldPacket decode(FriendlyByteBuf buf) {
      return new HeldPacket(buf.readByte(), buf.m_130277_(), buf.readFloat(), buf.readFloat());
   }

   public static void handle(HeldPacket m, Supplier<NetworkEvent.Context> ctx) {
      NetworkEvent.Context c = (NetworkEvent.Context)ctx.get();
      c.enqueueWork(() -> ClientState.setHeld(m.mode, m.holder, m.progress, m.chance));
      c.setPacketHandled(true);
   }
}
