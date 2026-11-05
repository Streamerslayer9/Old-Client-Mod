package com.example.gulp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public class RolesPacket {
   public final Map<UUID, Integer> roles;

   public RolesPacket(Map<UUID, Integer> roles) {
      this.roles = roles;
   }

   public static void broadcast(MinecraftServer server) {
      StomachManager mgr = StomachManager.get(server);
      Map<UUID, Integer> map = new HashMap();

      for(ServerPlayer p : server.m_6846_().m_11314_()) {
         map.put(p.m_20148_(), mgr.of(p.m_20148_()).role.ordinal());
      }

      Net.CHANNEL.send(PacketDistributor.ALL.noArg(), new RolesPacket(map));
   }

   public static void encode(RolesPacket m, FriendlyByteBuf buf) {
      buf.m_130130_(m.roles.size());
      m.roles.forEach((id, role) -> {
         buf.m_130077_(id);
         buf.writeByte(role);
      });
   }

   public static RolesPacket decode(FriendlyByteBuf buf) {
      int n = buf.m_130242_();
      Map<UUID, Integer> map = new HashMap();

      for(int i = 0; i < n; ++i) {
         map.put(buf.m_130259_(), buf.readByte());
      }

      return new RolesPacket(map);
   }

   public static void handle(RolesPacket m, Supplier<NetworkEvent.Context> ctx) {
      NetworkEvent.Context c = (NetworkEvent.Context)ctx.get();
      c.enqueueWork(() -> {
         ClientState.setRoles(m.roles);
         DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientRoleTags.refreshAll());
      });
      c.setPacketHandled(true);
   }
}
