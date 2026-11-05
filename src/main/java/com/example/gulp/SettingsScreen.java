package com.example.gulp;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingsScreen extends Screen {
   private static final int W = 250;
   private static final int H = 204;
   private static final String SWALLOW_TIP = "Which mod does your swallowing. Gulp: eat mobs and players with V and your belly grows. Flamevore: Flamevore does it; Gulp's mob eating and belly are off, but item swallowing, level, perks and item storage stay.";
   private Button bellyButton;
   private Button roleButton;
   private Button swallowButton;
   private Button perkMenuButton;
   private Role shownRole;
   private boolean shownTips;

   public SettingsScreen() {
      super(Component.m_237113_("Gulp Settings"));
   }

   private int left() {
      return (this.f_96543_ - 250) / 2;
   }

   private int top() {
      return (this.f_96544_ - 204) / 2;
   }

   private void refresh() {
      String var10001 = GulpConfig.bellyStyle.label;
      this.bellyButton.m_93666_(Component.m_237113_("Belly style: " + var10001));
      this.perkMenuButton.m_93666_(Component.m_237113_("Perk menu: " + (GulpConfig.legacyPerkMenu ? "Legacy" : "New look")));
      this.swallowButton.m_93666_(Component.m_237113_(swallowLabel()));
      this.swallowButton.f_93623_ = ClientState.flamevoreActive();
      Role role = ClientState.role();
      this.roleButton.m_93666_(Component.m_237113_("Role: ").m_7220_(Component.m_237113_(role.label).m_130938_((st) -> st.m_178520_(role.color & 16777215))));
      if (role != this.shownRole || this.shownTips != GulpConfig.helpTooltips) {
         this.shownRole = role;
         this.shownTips = GulpConfig.helpTooltips;
         this.roleButton.m_257544_(this.shownTips ? Tooltip.m_257550_(Component.m_237113_(role.description + "\n\nClick to switch: Pred, Prey, Both, Neither.")) : null);
         this.swallowButton.m_257544_(this.shownTips ? Tooltip.m_257550_(Component.m_237113_("Which mod does your swallowing. Gulp: eat mobs and players with V and your belly grows. Flamevore: Flamevore does it; Gulp's mob eating and belly are off, but item swallowing, level, perks and item storage stay.")) : null);
      }

   }

   private static String swallowLabel() {
      if (!ClientState.flamevoreActive()) {
         return "Swallowing: Gulp (Flamevore not found)";
      } else {
         SwallowSystem system = ClientState.swallowSystem();
         String var10000 = system == SwallowSystem.UNSET ? "choose..." : system.label;
         return "Swallowing: " + var10000;
      }
   }

   protected void m_7856_() {
      ScreenLoop.start();
      int x = this.left();
      int y = this.top();
      this.bellyButton = Button.m_253074_(Component.m_237119_(), (btn) -> {
         GulpConfig.bellyStyle = GulpConfig.bellyStyle.next();
         GulpConfig.save();
         this.refresh();
      }).m_252987_(x + 15, y + 22, 220, 19).m_253136_();
      this.roleButton = Button.m_253074_(Component.m_237119_(), (btn) -> Net.send(8, ClientState.role().next().ordinal())).m_252987_(x + 15, y + 43, 220, 19).m_253136_();
      this.swallowButton = Button.m_253074_(Component.m_237119_(), (btn) -> {
         if (ClientState.flamevoreActive()) {
            if (ClientState.needsSwallowChoice()) {
               this.f_96541_.m_91152_(new SwallowChoiceScreen());
            } else {
               Net.send(19, (ClientState.usesGulpSwallow() ? SwallowSystem.FLAMEVORE : SwallowSystem.GULP).ordinal());
            }

         }
      }).m_252987_(x + 15, y + 64, 220, 19).m_253136_();
      this.perkMenuButton = Button.m_253074_(Component.m_237119_(), (btn) -> {
         GulpConfig.legacyPerkMenu = !GulpConfig.legacyPerkMenu;
         GulpConfig.save();
         this.refresh();
      }).m_252987_(x + 15, y + 85, 220, 19).m_253136_();
      this.m_142416_(this.bellyButton);
      this.m_142416_(this.roleButton);
      this.m_142416_(this.swallowButton);
      this.m_142416_(this.perkMenuButton);
      this.m_142416_(Button.m_253074_(Component.m_237113_("Sounds..."), (btn) -> this.f_96541_.m_91152_(new SoundsScreen())).m_252987_(x + 15, y + 106, 220, 19).m_253136_());
      this.m_142416_(Button.m_253074_(Component.m_237113_("Toggles..."), (btn) -> this.f_96541_.m_91152_(new TogglesScreen())).m_252987_(x + 15, y + 127, 220, 19).m_253136_());
      this.m_142416_(Button.m_253074_(Component.m_237113_("Stomach look..."), (btn) -> this.f_96541_.m_91152_(new StyleScreen())).m_252987_(x + 15, y + 148, 220, 19).m_253136_());
      this.m_142416_(Button.m_253074_(Component.m_237113_("Back"), (btn) -> {
         if (ClientState.role().canEat()) {
            this.f_96541_.m_91152_(new StomachScreen());
         } else {
            this.m_7379_();
         }

      }).m_252987_(x + 8, y + 204 - 28, 60, 20).m_253136_());
      this.m_142416_(Button.m_253074_(Component.m_237113_("Close"), (btn) -> this.m_7379_()).m_252987_(x + 250 - 68, y + 204 - 28, 60, 20).m_253136_());
      this.shownRole = null;
      this.refresh();
   }

   public void m_86600_() {
      this.refresh();
   }

   public boolean m_7043_() {
      return false;
   }

   public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(g);
      int x = this.left();
      int y = this.top();
      g.m_280509_(x, y, x + 250, y + 204, -434891756);
      g.m_280509_(x, y, x + 250, y + 1, -10066330);
      g.m_280509_(x, y + 204 - 1, x + 250, y + 204, -10066330);
      g.m_280509_(x, y, x + 1, y + 204, -10066330);
      g.m_280509_(x + 250 - 1, y, x + 250, y + 204, -10066330);
      g.m_280056_(this.f_96547_, "Settings", x + 8, y + 8, 16777215, true);
      super.m_88315_(g, mouseX, mouseY, partialTick);
   }
}
