package com.example.gulp;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;

public class Stomach {
   public static final int MAX_LEVEL = 99;
   public static final int MAX_MOBS = 100;
   public static final float SOFT_SECONDS_PER_LEVEL = 240.0F;
   public boolean hard = false;
   public final boolean[] hardOrgans;
   public int activeOrgan;
   public int breastMilk;
   public int breastButter;
   public int breastChurn;
   public boolean instantDigest;
   public int level;
   public int xp;
   public int points;
   public final int[] ranks;
   public final List<CompoundTag> contents;
   public final NonNullList<ItemStack> storage;
   public final NonNullList<ItemStack> stripSlots;
   public SwallowSystem swallowSystem;
   public Role role;
   public int predator;
   public int digestProgress;
   public transient int cooldown;

   public Stomach() {
      this.hardOrgans = new boolean[Organ.COUNT];
      this.activeOrgan = 0;
      this.instantDigest = false;
      this.level = 1;
      this.xp = 0;
      this.points = 0;
      this.ranks = new int[Perk.values().length];
      this.contents = new ArrayList();
      this.storage = NonNullList.m_122780_(54, ItemStack.f_41583_);
      this.stripSlots = NonNullList.m_122780_(2, ItemStack.f_41583_);
      this.swallowSystem = SwallowSystem.UNSET;
      this.role = Role.PRED;
      this.predator = 0;
      this.digestProgress = 0;
      this.cooldown = 0;
   }

   public static String fmtVolume(double v) {
      return v >= (double)100.0F ? String.format("%.0f", v) : String.format("%.1f", v);
   }

   public boolean usesGulpSwallow() {
      return !FlamevoreCompat.isLoaded() || this.swallowSystem == SwallowSystem.GULP;
   }

   public static int xpForNext(int level) {
      return level <= 10 ? (int)(Math.round((double)100.0F * Math.pow((double)level, (double)1.5F) / (double)10.0F) * 10L) : 3160 + 40 * (level - 10);
   }

   public static int digestXp(float maxHealth) {
      return 20 + (int)(4.0F * maxHealth);
   }

   public int rank(Perk perk) {
      return this.ranks[perk.ordinal()];
   }

   public double capacity() {
      double base = (double)1.0F + (double)0.75F * (double)this.level + 0.08 * (double)this.level * (double)this.level;
      return base * ((double)1.0F + 0.06 * (double)this.rank(Perk.ROOMY));
   }

   public int storageRows() {
      return Math.min(6, 1 + this.level / 17);
   }

   public int storageSlots() {
      return this.storageRows() * 9;
   }

   public ItemStack insertItem(ItemStack stack) {
      ItemStack remaining = stack.m_41777_();
      int slots = this.storageSlots();

      for(int i = 0; i < slots && !remaining.m_41619_(); ++i) {
         ItemStack in = (ItemStack)this.storage.get(i);
         if (!in.m_41619_() && ItemStack.m_150942_(in, remaining) && in.m_41613_() < in.m_41741_()) {
            int move = Math.min(remaining.m_41613_(), in.m_41741_() - in.m_41613_());
            in.m_41769_(move);
            remaining.m_41774_(move);
         }
      }

      for(int i = 0; i < slots && !remaining.m_41619_(); ++i) {
         if (((ItemStack)this.storage.get(i)).m_41619_()) {
            this.storage.set(i, remaining.m_41620_(Math.min(remaining.m_41613_(), remaining.m_41741_())));
         }
      }

      return remaining;
   }

   public float itemFill() {
      int slots = this.storageSlots();
      float total = 0.0F;

      for(int i = 0; i < slots; ++i) {
         ItemStack stack = (ItemStack)this.storage.get(i);
         total += weightOf(stack);
      }

      return Math.min(1.0F, total / (float)slots);
   }

   public static float weightOf(ItemStack stack) {
      return weightOf(stack, 0);
   }

   private static float weightOf(ItemStack stack, int depth) {
      if (stack.m_41619_()) {
         return 0.0F;
      } else {
         float weight = (float)stack.m_41613_() / (float)stack.m_41741_();
         if (depth < 3 && stack.m_41782_()) {
            ListTag inner = null;
            if (stack.m_41720_() instanceof BundleItem) {
               inner = stack.m_41783_().m_128437_("Items", 10);
            } else if (Block.m_49814_(stack.m_41720_()) instanceof ShulkerBoxBlock) {
               inner = stack.m_41783_().m_128469_("BlockEntityTag").m_128437_("Items", 10);
            }

            if (inner != null) {
               float sum = 0.0F;

               for(int i = 0; i < inner.size(); ++i) {
                  sum += weightOf(ItemStack.m_41712_(inner.m_128728_(i)), depth + 1);
               }

               weight += sum * ((Double)GulpServerConfig.NESTED_WEIGHT_MULTIPLIER.get()).floatValue() * (float)stack.m_41613_();
            }

            return weight;
         } else {
            return weight;
         }
      }
   }

   public int organ() {
      return (Boolean)GulpServerConfig.MULTI_ORGANS_ENABLED.get() ? this.activeOrgan : 0;
   }

   public boolean isHard(int organ) {
      return organ == 0 ? this.hard : this.hardOrgans[Math.max(0, Math.min(Organ.COUNT - 1, organ))];
   }

   public void setHard(int organ, boolean value) {
      if (organ == 0) {
         this.hard = value;
      } else {
         this.hardOrgans[Math.max(0, Math.min(Organ.COUNT - 1, organ))] = value;
      }

   }

   public double usedIn(int organ) {
      double total = (double)0.0F;

      for(CompoundTag e : this.contents) {
         if (Organ.of(e) == organ) {
            total += (double)e.m_128457_("Size");
         }
      }

      return total;
   }

   public double capacityOf(int organ) {
      return this.capacity();
   }

   public double used() {
      return this.usedIn(0);
   }

   public void addEntry(CompoundTag entry) {
      if (!entry.m_128441_("Organ")) {
         entry.m_128405_("Organ", this.organ());
      }

      if (Organ.of(entry) == 4 && !entry.m_128441_("Transit")) {
         entry.m_128405_("Transit", 0);
      }

      this.contents.add(entry);
   }

   public static int cumulativePoints(int level) {
      return (int)((long)(Math.max(1, level) - 1) * (long)Perk.totalRanks() / 98L);
   }

   public int addXp(int amount) {
      if (this.level >= 99) {
         return 0;
      } else {
         this.xp += amount;

         int gained;
         for(gained = 0; this.level < 99 && this.xp >= xpForNext(this.level); ++gained) {
            this.xp -= xpForNext(this.level);
            ++this.level;
            this.points += cumulativePoints(this.level) - cumulativePoints(this.level - 1);
         }

         return gained;
      }
   }

   public CompoundTag toNbt() {
      CompoundTag nbt = new CompoundTag();
      nbt.m_128379_("Hard", this.hard);
      int mask = 0;

      for(int i = 1; i < Organ.COUNT; ++i) {
         if (this.hardOrgans[i]) {
            mask |= 1 << i;
         }
      }

      nbt.m_128405_("HardOrgans", mask);
      nbt.m_128405_("ActiveOrgan", this.activeOrgan);
      nbt.m_128405_("BreastMilk", this.breastMilk);
      nbt.m_128405_("BreastButter", this.breastButter);
      nbt.m_128405_("BreastChurn", this.breastChurn);
      nbt.m_128405_("Level", this.level);
      nbt.m_128405_("Xp", this.xp);
      nbt.m_128405_("Points", this.points);
      CompoundTag perks = new CompoundTag();

      for(Perk p : Perk.values()) {
         perks.m_128405_(p.name(), this.ranks[p.ordinal()]);
      }

      nbt.m_128365_("Perks", perks);
      nbt.m_128359_("Role", this.role.name());
      nbt.m_128359_("SwallowSystem", this.swallowSystem.name());
      nbt.m_128379_("InstantDigest", this.instantDigest);
      nbt.m_128405_("Predator", this.predator);
      nbt.m_128405_("DigestProgress", this.digestProgress);
      nbt.m_128365_("Storage", ContainerHelper.m_18973_(new CompoundTag(), this.storage));
      nbt.m_128365_("StripSlots", ContainerHelper.m_18973_(new CompoundTag(), this.stripSlots));
      ListTag list = new ListTag();
      list.addAll(this.contents);
      nbt.m_128365_("Contents", list);
      return nbt;
   }

   public static Stomach fromNbt(CompoundTag nbt) {
      Stomach s = new Stomach();
      s.hard = nbt.m_128471_("Hard");
      int mask = nbt.m_128451_("HardOrgans");

      for(int i = 1; i < Organ.COUNT; ++i) {
         s.hardOrgans[i] = (mask & 1 << i) != 0;
      }

      s.activeOrgan = Math.max(0, Math.min(Organ.COUNT - 1, nbt.m_128451_("ActiveOrgan")));
      s.breastMilk = nbt.m_128451_("BreastMilk");
      s.breastButter = nbt.m_128451_("BreastButter");
      s.breastChurn = nbt.m_128451_("BreastChurn");
      s.level = Math.max(1, nbt.m_128451_("Level"));
      s.xp = nbt.m_128451_("Xp");
      s.points = nbt.m_128441_("Points") ? nbt.m_128451_("Points") : s.level - 1;
      Role saved = nbt.m_128441_("Role") ? Role.byName(nbt.m_128461_("Role")) : null;
      s.role = saved != null ? saved : (nbt.m_128471_("AllowSwallowed") ? Role.BOTH : Role.PRED);
      s.instantDigest = nbt.m_128471_("InstantDigest");
      SwallowSystem savedSystem = nbt.m_128441_("SwallowSystem") ? SwallowSystem.byName(nbt.m_128461_("SwallowSystem")) : null;
      s.swallowSystem = savedSystem != null ? savedSystem : SwallowSystem.UNSET;
      s.predator = Math.max(0, nbt.m_128451_("Predator"));
      s.digestProgress = Math.max(0, Math.min(31, nbt.m_128451_("DigestProgress")));
      ContainerHelper.m_18980_(nbt.m_128469_("Storage"), s.storage);
      ContainerHelper.m_18980_(nbt.m_128469_("StripSlots"), s.stripSlots);
      CompoundTag perks = nbt.m_128469_("Perks");

      for(Perk p : Perk.values()) {
         s.ranks[p.ordinal()] = Math.min(p.maxRank, perks.m_128451_(p.name()));
      }

      int spent = 0;

      for(int r : s.ranks) {
         spent += r;
      }

      int shouldHave = cumulativePoints(s.level);
      if (s.points + spent < shouldHave) {
         s.points = shouldHave - spent;
      }

      ListTag list = nbt.m_128437_("Contents", 10);

      for(int i = 0; i < list.size(); ++i) {
         s.contents.add(list.m_128728_(i));
      }

      return s;
   }
}
