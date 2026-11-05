package com.example.gulp;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class StorageViewPacket {
   public final String host;
   public final List<ItemStack> items;

   public StorageViewPacket(String host, List<ItemStack> items) {
      this.host = host;
      this.items = items;
   }

   public static void encode(StorageViewPacket m, FriendlyByteBuf buf) {
      buf.m_130070_(m.host);
      buf.m_130130_(m.items.size());

      for(ItemStack stack : m.items) {
         buf.m_130055_(stack);
      }

   }

   public static StorageViewPacket decode(FriendlyByteBuf buf) {
      String host = buf.m_130277_();
      int n = buf.m_130242_();
      List<ItemStack> items = new ArrayList();

      for(int i = 0; i < n; ++i) {
         items.add(buf.m_130267_());
      }

      return new StorageViewPacket(host, items);
   }

   public static void handle(StorageViewPacket m, Supplier<NetworkEvent.Context> ctx) {
      NetworkEvent.Context c = (NetworkEvent.Context)ctx.get();
      c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SharedStorageScreen.show(m.host, m.items)));
      c.setPacketHandled(true);
   }
}
