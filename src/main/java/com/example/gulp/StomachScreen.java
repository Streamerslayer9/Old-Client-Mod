package com.example.gulp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.ForgeRegistries;

public class StomachScreen extends Screen {
   private static final int W = 250;
   private static final int ROWS = 5;
   private static final int ROW_H = 30;
   private static final int HEADER = 62;
   private static final int FOOTER = 56;
   private static final int H = 268;
   private static final Map<String, ItemStack> ICONS = new HashMap();
   private static Map<EntityType<?>, SpawnEggItem> allEggs;
   private int page = 0;
   private String lastSig = "";

   private static int rows() {
      return ClientState.activeOrgan() == Organ.BREAST.ordinal() ? 4 : 5;
   }

   public StomachScreen() {
      super(Component.m_237113_("Stomach"));
   }

   private int left() {
      return (this.f_96543_ - 250) / 2;
   }

   private int top() {
      return (this.f_96544_ - 268) / 2;
   }

   private static SpawnEggItem eggFor(EntityType<?> type) {
      SpawnEggItem egg = SpawnEggItem.m_43213_(type);
      if (egg != null) {
         return egg;
      } else {
         if (allEggs == null) {
            allEggs = new HashMap();

            for(Item item : ForgeRegistries.ITEMS) {
               if (item instanceof SpawnEggItem) {
                  SpawnEggItem e = (SpawnEggItem)item;

                  try {
                     EntityType<?> t = e.m_43228_((CompoundTag)null);
                     if (t != null) {
                        allEggs.putIfAbsent(t, e);
                     }
                  } catch (Exception var6) {
                  }
               }
            }
         }

         return (SpawnEggItem)allEggs.get(type);
      }
   }

   private static ItemStack iconFor(String typeId) {
      return (ItemStack)ICONS.computeIfAbsent(typeId, (id) -> {
         if ("minecraft:player".equals(id)) {
            return new ItemStack(Items.f_42680_);
         } else {
            Optional<EntityType<?>> type = EntityType.m_20632_(id);
            if (type.isPresent()) {
               SpawnEggItem egg = eggFor((EntityType)type.get());
               if (egg != null) {
                  return new ItemStack(egg);
               }
            }

            return ItemStack.f_41583_;
         }
      });
   }

   private static String displayName(ClientState.EntryInfo e) {
      String name = e.name().getString();
      if (!name.isEmpty() && !name.startsWith("entity.")) {
         return name;
      } else {
         String id = e.typeId();
         String path = id.substring(id.indexOf(58) + 1);
         StringBuilder sb = new StringBuilder();

         for(String part : path.split("[_/]+")) {
            if (!part.isEmpty()) {
               if (sb.length() > 0) {
                  sb.append(' ');
               }

               sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
         }

         return sb.length() > 0 ? sb.toString() : name;
      }
   }

   private static List<ClientState.EntryInfo> shown() {
      int organ = ClientState.activeOrgan();
      List<ClientState.EntryInfo> out = new ArrayList();

      for(ClientState.EntryInfo e : ClientState.entries()) {
         if (e.organ() == organ) {
            out.add(e);
         }
      }

      return out;
   }

   protected void m_7856_() {
      ScreenLoop.start();
      List<ClientState.EntryInfo> list = shown();
      int pages = Math.max(1, (list.size() + rows() - 1) / rows());
      this.page = Math.max(0, Math.min(this.page, pages - 1));
      String var10001 = ClientState.signature();
      this.lastSig = var10001 + "#" + this.page;
      int x = this.left();
      int y = this.top();
      int start = this.page * rows();

      for(int i = 0; i < rows() && start + i < list.size(); ++i) {
         ClientState.EntryInfo e = (ClientState.EntryInfo)list.get(start + i);
         int ry = y + 62 + i * 30;
         this.m_142416_(Button.m_253074_(Component.m_237113_("Release"), (btn) -> Net.send(4, e.uid())).m_252987_(x + 250 - 108, ry + 4, 50, 20).m_253136_());
         if (ClientState.isInstantDigest()) {
            Button digest = Button.m_253074_(Component.m_237113_("Digest"), (btn) -> Net.send(5, e.uid())).m_252987_(x + 250 - 56, ry + 4, 48, 20).m_253136_();
            digest.f_93623_ = !"minecraft:player".equals(e.typeId());
            this.m_142416_(digest);
         }
      }

      if (ClientState.activeOrgan() == Organ.BREAST.ordinal()) {
         int sy = y + 62 + 120 + 5;
         Button milk = Button.m_253074_(Component.m_237113_("Take Milk"), (btn) -> Net.send(32)).m_252987_(x + 250 - 140, sy, 64, 20).m_253136_();
         milk.f_93623_ = ClientState.breastMilk() > 0;
         this.m_142416_(milk);
         Button butter = Button.m_253074_(Component.m_237113_("Take Butter"), (btn) -> Net.send(33)).m_252987_(x + 250 - 72, sy, 64, 20).m_253136_();
         butter.f_93623_ = ClientState.breastButter() > 0;
         this.m_142416_(butter);
      }

      if (ClientState.multiOrgans()) {
         for(int i = 0; i < Organ.COUNT; ++i) {
            Button tab = Button.m_253074_(Component.m_237113_(Organ.byId(i).label), (btn) -> {
               this.page = 0;
               Net.send(30, i);
            }).m_252987_(x + 8 + i * 47, y + 34, 46, 16).m_253136_();
            tab.f_93623_ = i != ClientState.activeOrgan();
            this.m_142416_(tab);
         }
      }

      int fy = y + 62 + 150 + 6;
      this.m_142416_(Button.m_253074_(Component.m_237113_(ClientState.isHard() ? "Hard mode" : "Soft mode"), (btn) -> Net.send(31, ClientState.activeOrgan())).m_252987_(x + 8, fy, 76, 20).m_253136_());
      int var18 = ClientState.points();
      this.m_142416_(Button.m_253074_(Component.m_237113_("Perks (" + var18 + ")"), (btn) -> this.f_96541_.m_91152_(PerkMenus.create())).m_252987_(x + 87, fy, 76, 20).m_253136_());
      this.m_142416_(Button.m_253074_(Component.m_237113_("Storage"), (btn) -> Net.send(7)).m_252987_(x + 166, fy, 76, 20).m_253136_());
      Button prev = Button.m_253074_(Component.m_237113_("<"), (btn) -> {
         --this.page;
         this.m_169413_();
         this.m_7856_();
      }).m_252987_(x + 8, fy + 26, 20, 20).m_253136_();
      Button next = Button.m_253074_(Component.m_237113_(">"), (btn) -> {
         ++this.page;
         this.m_169413_();
         this.m_7856_();
      }).m_252987_(x + 32, fy + 26, 20, 20).m_253136_();
      prev.f_93623_ = this.page > 0;
      next.f_93623_ = this.page < pages - 1;
      this.m_142416_(prev);
      this.m_142416_(next);
      this.m_142416_(Button.m_253074_(Component.m_237113_("Settings"), (btn) -> this.f_96541_.m_91152_(new SettingsScreen())).m_252987_(x + 112, fy + 26, 66, 20).m_253136_());
      this.m_142416_(Button.m_253074_(Component.m_237113_("Close"), (btn) -> this.m_7379_()).m_252987_(x + 250 - 68, fy + 26, 60, 20).m_253136_());
   }

   public void m_86600_() {
      String var10000 = this.lastSig;
      String var10001 = ClientState.signature();
      if (!var10000.equals(var10001 + "#" + this.page)) {
         this.m_169413_();
         this.m_7856_();
      }

   }

   public boolean m_7043_() {
      return false;
   }

   public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(g);
      int x = this.left();
      int y = this.top();
      StomachStyle.paintWall(g, x, y, 250, 268);
      StomachStyle.paintAcid(g, x + 1, y + 134, 248, 133);
      int organ = ClientState.activeOrgan();
      Font var10001 = this.f_96547_;
      String var10002 = Organ.byId(organ).label;
      g.m_280056_(var10001, var10002 + "  Lv." + ClientState.level(), x + 8, y + 7, 16777215, true);
      String acidLabel = "acid: digestion";
      String legend = "red: health   ";
      int lx = x + 250 - 8 - this.f_96547_.m_92895_(legend) - this.f_96547_.m_92895_(acidLabel);
      g.m_280056_(this.f_96547_, legend, lx, y + 7, 10066329, false);
      g.m_280056_(this.f_96547_, acidLabel, lx + this.f_96547_.m_92895_(legend), y + 7, StomachStyle.acidArgb(), false);
      int bx = x + 8;
      int bw = 234;
      int by = y + 21;
      g.m_280509_(bx - 1, by - 1, bx + bw + 1, by + 11, -16777216);
      g.m_280509_(bx, by, bx + bw, by + 10, -13421773);
      int fill = (int)((float)bw * Math.min(1.0F, ClientState.usedIn(organ) / ClientState.cap()));
      g.m_280509_(bx, by, bx + fill, by + 10, ClientState.isHard() ? -4173778 : -10635171);
      g.m_280056_(this.f_96547_, Stomach.fmtVolume((double)ClientState.usedIn(organ)) + " / " + Stomach.fmtVolume((double)ClientState.cap()), bx + 4, by + 1, 16777215, true);
      if (ClientState.predator() > 0) {
         var10001 = this.f_96547_;
         int var25 = ClientState.predator();
         g.m_280056_(var10001, "Villagers fear you: " + var25, x + 8, y + 52, ClientState.predator() >= 30 ? -38037 : -2056112, false);
      }

      if (organ == Organ.BREAST.ordinal()) {
         int sy = y + 62 + 120;
         g.m_280509_(x + 6, sy + 1, x + 250 - 6, sy + 30 - 1, 1090502720);
         g.m_280056_(this.f_96547_, "Milk " + ClientState.breastMilk() + "   Butter " + ClientState.breastButter(), x + 10, sy + 5, 16771488, false);
         g.m_280509_(x + 10, sy + 18, x + 106, sy + 22, -14540254);
         g.m_280509_(x + 10, sy + 18, x + 10 + (int)(96.0F * ClientState.breastChurn()), sy + 22, -1652662);
      }

      List<ClientState.EntryInfo> list = shown();
      int start = this.page * rows();
      if (list.isEmpty()) {
         g.m_280653_(this.f_96547_, Component.m_237113_("Nothing in your " + Organ.byId(organ).lower() + "."), x + 125, y + 62 + 60, 11184810);
      }

      for(int i = 0; i < rows() && start + i < list.size(); ++i) {
         ClientState.EntryInfo e = (ClientState.EntryInfo)list.get(start + i);
         int ry = y + 62 + i * 30;
         g.m_280509_(x + 6, ry + 1, x + 250 - 6, ry + 30 - 1, 1090519039);
         ItemStack icon = iconFor(e.typeId());
         if (!icon.m_41619_()) {
            g.m_280480_(icon, x + 10, ry + 7);
         }

         g.m_280056_(this.f_96547_, this.f_96547_.m_92834_(displayName(e), 80), x + 32, ry + 4, 16777215, true);
         float hpRatio = e.maxHp() > 0.0F ? Math.min(1.0F, e.hp() / e.maxHp()) : 0.0F;
         g.m_280509_(x + 32, ry + 15, x + 112, ry + 19, -14540254);
         g.m_280509_(x + 32, ry + 15, x + 32 + (int)(80.0F * hpRatio), ry + 19, -3129280);
         g.m_280509_(x + 32, ry + 21, x + 112, ry + 24, -14540254);
         g.m_280509_(x + 32, ry + 21, x + 32 + (int)(80.0F * e.digest()), ry + 24, StomachStyle.acidArgb());
         if (e.organ() == 4) {
            g.m_280509_(x + 32, ry + 26, x + 112, ry + 28, -14540254);
            g.m_280509_(x + 32, ry + 26, x + 32 + (int)(80.0F * e.transit()), ry + 28, -11890462);
         }
      }

      int pages = Math.max(1, (list.size() + rows() - 1) / rows());
      g.m_280056_(this.f_96547_, "Page " + (this.page + 1) + "/" + pages, x + 58, y + 268 - 20, 11184810, true);
      super.m_88315_(g, mouseX, mouseY, partialTick);
   }
}
