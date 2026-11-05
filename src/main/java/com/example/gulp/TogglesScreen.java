package com.example.gulp;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TogglesScreen extends Screen {
   private static final int W = 250;
   private static final int H = 200;
   private static final String INSTANT_TIP = "Shows a Digest button next to each mob in your stomach that digests it right away. Off by default.";
   private static final String RIGHT_CLICK_TIP = "Right-click an item in your stomach storage to eat it. Hold to keep eating, shift for the whole stack. Turn off to get the normal right-click back (picking up half a stack).";
   private Button levelUpButton;
   private Button actionButton;
   private Button roleTagButton;
   private Button rightClickButton;
   private Button cpmButton;
   private Button instantButton;
   private Button tipsButton;
   private Boolean shownTips;

   public TogglesScreen() {
      super(Component.m_237113_("Gulp Toggles"));
   }

   private int left() {
      return (this.f_96543_ - 250) / 2;
   }

   private int top() {
      return (this.f_96544_ - 200) / 2;
   }

   private static String onOff(boolean on) {
      return on ? "ON" : "OFF";
   }

   private void refresh() {
      Button var10000 = this.levelUpButton;
      String var10001 = onOff(GulpConfig.levelUpMessages);
      var10000.m_93666_(Component.m_237113_("Level-up messages: " + var10001));
      var10000 = this.actionButton;
      var10001 = onOff(GulpConfig.actionMessages);
      var10000.m_93666_(Component.m_237113_("Action messages: " + var10001));
      var10000 = this.roleTagButton;
      var10001 = onOff(GulpConfig.showRoleTags);
      var10000.m_93666_(Component.m_237113_("Role in name tags: " + var10001));
      var10000 = this.rightClickButton;
      var10001 = onOff(GulpConfig.rightClickEat);
      var10000.m_93666_(Component.m_237113_("Right-click to eat: " + var10001));
      var10000 = this.cpmButton;
      var10001 = onOff(GulpConfig.cpmSliders);
      var10000.m_93666_(Component.m_237113_("CPM belly sliders: " + var10001 + (CpmBridge.available() ? "" : " (CPM not found)")));
      this.instantButton.m_93666_(Component.m_237113_("Instant digest button: " + onOff(ClientState.isInstantDigest())));
      this.tipsButton.m_93666_(Component.m_237113_("Hover help tips: " + onOff(GulpConfig.helpTooltips)));
      if (this.shownTips == null || this.shownTips != GulpConfig.helpTooltips) {
         this.shownTips = GulpConfig.helpTooltips;
         boolean on = GulpConfig.helpTooltips;
         this.instantButton.m_257544_(on ? Tooltip.m_257550_(Component.m_237113_("Shows a Digest button next to each mob in your stomach that digests it right away. Off by default.")) : null);
         this.rightClickButton.m_257544_(on ? Tooltip.m_257550_(Component.m_237113_("Right-click an item in your stomach storage to eat it. Hold to keep eating, shift for the whole stack. Turn off to get the normal right-click back (picking up half a stack).")) : null);
      }

   }

   protected void m_7856_() {
      ScreenLoop.start();
      int x = this.left();
      int y = this.top();
      int bw = 220;
      this.levelUpButton = Button.m_253074_(Component.m_237119_(), (btn) -> {
         GulpConfig.levelUpMessages = !GulpConfig.levelUpMessages;
         GulpConfig.save();
         this.refresh();
      }).m_252987_(x + 15, y + 22, bw, 19).m_253136_();
      this.actionButton = Button.m_253074_(Component.m_237119_(), (btn) -> {
         GulpConfig.actionMessages = !GulpConfig.actionMessages;
         GulpConfig.save();
         this.refresh();
      }).m_252987_(x + 15, y + 43, bw, 19).m_253136_();
      this.roleTagButton = Button.m_253074_(Component.m_237119_(), (btn) -> {
         GulpConfig.showRoleTags = !GulpConfig.showRoleTags;
         GulpConfig.save();
         ClientRoleTags.refreshAll();
         this.refresh();
      }).m_252987_(x + 15, y + 64, bw, 19).m_253136_();
      this.rightClickButton = Button.m_253074_(Component.m_237119_(), (btn) -> {
         GulpConfig.rightClickEat = !GulpConfig.rightClickEat;
         GulpConfig.save();
         this.refresh();
      }).m_252987_(x + 15, y + 85, bw, 19).m_253136_();
      this.cpmButton = Button.m_253074_(Component.m_237119_(), (btn) -> {
         GulpConfig.cpmSliders = !GulpConfig.cpmSliders;
         GulpConfig.save();
         this.refresh();
      }).m_252987_(x + 15, y + 106, bw, 19).m_253136_();
      this.instantButton = Button.m_253074_(Component.m_237119_(), (btn) -> Net.send(17, ClientState.isInstantDigest() ? 0 : 1)).m_252987_(x + 15, y + 127, bw, 19).m_253136_();
      this.tipsButton = Button.m_253074_(Component.m_237119_(), (btn) -> {
         GulpConfig.helpTooltips = !GulpConfig.helpTooltips;
         GulpConfig.save();
         this.refresh();
      }).m_252987_(x + 15, y + 148, bw, 19).m_253136_();
      this.m_142416_(this.levelUpButton);
      this.m_142416_(this.actionButton);
      this.m_142416_(this.roleTagButton);
      this.m_142416_(this.rightClickButton);
      this.m_142416_(this.cpmButton);
      this.m_142416_(this.instantButton);
      this.m_142416_(this.tipsButton);
      this.m_142416_(Button.m_253074_(Component.m_237113_("Back"), (btn) -> this.f_96541_.m_91152_(new SettingsScreen())).m_252987_(x + 8, y + 200 - 28, 60, 20).m_253136_());
      this.m_142416_(Button.m_253074_(Component.m_237113_("Close"), (btn) -> this.m_7379_()).m_252987_(x + 250 - 68, y + 200 - 28, 60, 20).m_253136_());
      this.shownTips = null;
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
      g.m_280509_(x, y, x + 250, y + 200, -434891756);
      g.m_280509_(x, y, x + 250, y + 1, -10066330);
      g.m_280509_(x, y + 200 - 1, x + 250, y + 200, -10066330);
      g.m_280509_(x, y, x + 1, y + 200, -10066330);
      g.m_280509_(x + 250 - 1, y, x + 250, y + 200, -10066330);
      g.m_280056_(this.f_96547_, "Toggles", x + 8, y + 8, 16777215, true);
      super.m_88315_(g, mouseX, mouseY, partialTick);
   }
}
