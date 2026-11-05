package com.example.gulp;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class SyncPacket {
   public boolean hard;
   public boolean instantDigest;
   public int level;
   public int xp;
   public int xpNeeded;
   public int points;
   public int role;
   public int swallowSystem;
   public boolean flamevoreOnServer;
   public int predator;
   public float itemFill;
   public int digestProgress;
   public ItemStack strip0;
   public ItemStack strip1;
   public float used;
   public float cap;
   public boolean multiOrgans;
   public int breastMilk;
   public int breastButter;
   public float breastChurn;
   public int activeOrgan;
   public int hardMask;
   public float[] organUsed;
   public int[] ranks;
   public List<ClientState.EntryInfo> entries;

   public SyncPacket() {
      this.strip0 = ItemStack.f_41583_;
      this.strip1 = ItemStack.f_41583_;
      this.organUsed = new float[Organ.COUNT];
      this.ranks = new int[Perk.values().length];
      this.entries = new ArrayList();
   }

   public static SyncPacket from(Stomach s) {
      SyncPacket m = new SyncPacket();
      m.hard = s.isHard(s.organ()) && (Boolean)GulpServerConfig.HARD_MODE_ENABLED.get();
      m.multiOrgans = (Boolean)GulpServerConfig.MULTI_ORGANS_ENABLED.get();
      m.breastMilk = s.breastMilk;
      m.breastButter = s.breastButter;
      m.breastChurn = Math.min(1.0F, (float)s.breastChurn / (float)Math.max(1, (Integer)GulpServerConfig.BREAST_CHURN_SECONDS.get() * 20));
      m.activeOrgan = s.organ();

      for(int i = 0; i < Organ.COUNT; ++i) {
         if (s.isHard(i) && (Boolean)GulpServerConfig.HARD_MODE_ENABLED.get()) {
            m.hardMask |= 1 << i;
         }

         m.organUsed[i] = (float)s.usedIn(i);
      }

      m.instantDigest = s.instantDigest;
      m.level = s.level;
      m.xp = s.xp;
      m.xpNeeded = Stomach.xpForNext(s.level);
      m.used = (float)s.used();
      m.cap = (float)s.capacity();
      m.points = s.points;
      m.role = s.role.ordinal();
      m.swallowSystem = s.swallowSystem.ordinal();
      m.flamevoreOnServer = FlamevoreCompat.isLoaded();
      m.predator = s.predator;
      m.itemFill = s.usesGulpSwallow() ? s.itemFill() : 0.0F;
      m.digestProgress = s.digestProgress;
      m.strip0 = ((ItemStack)s.stripSlots.get(0)).m_41777_();
      m.strip1 = ((ItemStack)s.stripSlots.get(1)).m_41777_();

      for(Perk p : Perk.values()) {
         m.ranks[p.ordinal()] = s.rank(p);
      }

      for(CompoundTag e : s.contents) {
         CompoundTag ent = e.m_128469_("Entity");
         int dt = Math.max(1, e.m_128451_("DigestTime"));
         m.entries.add(new ClientState.EntryInfo(e.m_128451_("Uid"), StomachLogic.entryName(e), ent.m_128461_("id"), e.m_128457_("Size"), ent.m_128457_("Health"), e.m_128457_("MaxHealth"), Math.min(1.0F, (float)e.m_128451_("Digest") / (float)dt), Organ.of(e), Math.min(1.0F, (float)e.m_128451_("Transit") / (float)Math.max(1, (Integer)GulpServerConfig.TAIL_TRANSIT_SECONDS.get() * 20))));
      }

      return m;
   }

   public static void encode(SyncPacket m, FriendlyByteBuf buf) {
      buf.writeBoolean(m.hard);
      buf.writeBoolean(m.instantDigest);
      buf.writeInt(m.level);
      buf.writeInt(m.xp);
      buf.writeInt(m.xpNeeded);
      buf.writeFloat(m.used);
      buf.writeFloat(m.cap);
      buf.writeBoolean(m.multiOrgans);
      buf.m_130130_(m.breastMilk);
      buf.m_130130_(m.breastButter);
      buf.writeFloat(m.breastChurn);
      buf.writeByte(m.activeOrgan);
      buf.writeByte(m.hardMask);

      for(float f : m.organUsed) {
         buf.writeFloat(f);
      }

      buf.writeInt(m.points);
      buf.writeByte(m.role);
      buf.writeByte(m.swallowSystem);
      buf.writeBoolean(m.flamevoreOnServer);
      buf.writeInt(m.predator);
      buf.writeFloat(m.itemFill);
      buf.writeInt(m.digestProgress);
      buf.m_130055_(m.strip0);
      buf.m_130055_(m.strip1);
      buf.writeInt(m.ranks.length);

      for(int r : m.ranks) {
         buf.writeInt(r);
      }

      buf.writeInt(m.entries.size());

      for(ClientState.EntryInfo e : m.entries) {
         buf.writeInt(e.uid());
         buf.m_130070_(Serializer.m_130703_(e.name()));
         buf.m_130070_(e.typeId());
         buf.writeFloat(e.size());
         buf.writeFloat(e.hp());
         buf.writeFloat(e.maxHp());
         buf.writeFloat(e.digest());
         buf.writeByte(e.organ());
         buf.writeFloat(e.transit());
      }

   }

   public static SyncPacket decode(FriendlyByteBuf buf) {
      SyncPacket m = new SyncPacket();
      m.hard = buf.readBoolean();
      m.instantDigest = buf.readBoolean();
      m.level = buf.readInt();
      m.xp = buf.readInt();
      m.xpNeeded = buf.readInt();
      m.used = buf.readFloat();
      m.cap = buf.readFloat();
      m.multiOrgans = buf.readBoolean();
      m.breastMilk = buf.m_130242_();
      m.breastButter = buf.m_130242_();
      m.breastChurn = buf.readFloat();
      m.activeOrgan = buf.readByte();
      m.hardMask = buf.readByte();

      for(int i = 0; i < m.organUsed.length; ++i) {
         m.organUsed[i] = buf.readFloat();
      }

      m.points = buf.readInt();
      m.role = buf.readByte();
      m.swallowSystem = buf.readByte();
      m.flamevoreOnServer = buf.readBoolean();
      m.predator = buf.readInt();
      m.itemFill = buf.readFloat();
      m.digestProgress = buf.readInt();
      m.strip0 = buf.m_130267_();
      m.strip1 = buf.m_130267_();
      int perkCount = buf.readInt();

      for(int i = 0; i < perkCount; ++i) {
         int v = buf.readInt();
         if (i < m.ranks.length) {
            m.ranks[i] = v;
         }
      }

      int n = buf.readInt();

      for(int i = 0; i < n; ++i) {
         int uid = buf.readInt();
         Component name = Serializer.m_130701_(buf.m_130277_());
         if (name == null) {
            name = Component.m_237119_();
         }

         String type = buf.m_130277_();
         float size = buf.readFloat();
         float hp = buf.readFloat();
         float maxHp = buf.readFloat();
         float dig = buf.readFloat();
         int organ = buf.readByte();
         float transit = buf.readFloat();
         m.entries.add(new ClientState.EntryInfo(uid, name, type, size, hp, maxHp, dig, organ, transit));
      }

      return m;
   }

   public static void handle(SyncPacket m, Supplier<NetworkEvent.Context> ctx) {
      NetworkEvent.Context c = (NetworkEvent.Context)ctx.get();
      c.enqueueWork(() -> ClientState.apply(m));
      c.setPacketHandled(true);
   }
}
