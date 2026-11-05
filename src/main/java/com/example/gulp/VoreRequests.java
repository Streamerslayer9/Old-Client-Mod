package com.example.gulp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class VoreRequests {
   public static final int EXPIRE_TICKS = 600;
   public static final int DECLINE_BLOCK_TICKS = 1200;
   public static final double MAX_DISTANCE = (double)8.0F;
   private static final Map<UUID, Request> PENDING = new HashMap();
   private static final Map<String, Integer> DECLINED = new HashMap();

   private VoreRequests() {
   }

   private static String key(UUID pred, UUID target) {
      String var10000 = String.valueOf(pred);
      return var10000 + ">" + String.valueOf(target);
   }

   public static void send(ServerPlayer pred, ServerPlayer victim) {
      int now = pred.m_20194_().m_129921_();
      PENDING.values().removeIf((r) -> r.expires() <= now);
      DECLINED.values().removeIf((t) -> t <= now);
      if (DECLINED.containsKey(key(pred.m_20148_(), victim.m_20148_()))) {
         StomachLogic.say(pred, Component.m_237119_().m_7220_(victim.m_7755_()).m_130946_(" declined your request a moment ago. Give them a minute."));
      } else {
         Request existing = (Request)PENDING.get(victim.m_20148_());
         if (existing != null) {
            StomachLogic.say(pred, Component.m_237119_().m_7220_(victim.m_7755_()).m_130946_(existing.pred().equals(pred.m_20148_()) ? " hasn't answered your request yet." : " already has a request waiting."));
         } else {
            boolean hard = PlayerHolding.hardVore(StomachManager.get(pred.m_20194_()).of(pred.m_20148_()));
            PENDING.put(victim.m_20148_(), new Request(pred.m_20148_(), now + 600, hard));
            MutableComponent accept = Component.m_237113_("[Accept]").m_130948_(Style.f_131099_.m_131140_(ChatFormatting.GREEN).m_131136_(true).m_131142_(new ClickEvent(Action.RUN_COMMAND, "/gulp accept")).m_131144_(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.f_130831_, Component.m_237113_("Let them swallow you"))));
            MutableComponent decline = Component.m_237113_("[Decline]").m_130948_(Style.f_131099_.m_131140_(ChatFormatting.RED).m_131136_(true).m_131142_(new ClickEvent(Action.RUN_COMMAND, "/gulp decline")).m_131144_(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.f_130831_, Component.m_237113_("Say no"))));
            String terms;
            if (hard) {
               int minutes = Math.max(1, Math.round((float)(Integer)GulpServerConfig.PLAYER_DIGEST_SECONDS.get() / 60.0F));
               terms = "HARD mode: you will be digested over about " + minutes + " min and can't just leave. You can struggle (R) to try to break free. " + ((Boolean)GulpServerConfig.PLAYER_DIGESTION_KILLS.get() ? "If the timer runs out you will DIE. " : "If the timer runs out you are let out, weakened. ");
            } else {
               terms = "Soft mode: you're only held, and can leave any time with Release (G). ";
            }

            victim.m_213846_(Component.m_237119_().m_7220_(pred.m_7755_()).m_130946_(" wants to swallow you. ").m_7220_(Component.m_237113_(terms).m_130940_(hard ? ChatFormatting.RED : ChatFormatting.YELLOW)).m_7220_(accept).m_130946_(" ").m_7220_(decline).m_7220_(Component.m_237113_(" (30 seconds, or type /gulp accept or /gulp decline)").m_130940_(ChatFormatting.GRAY)));
            victim.m_6330_(SoundEvents.f_11871_, SoundSource.PLAYERS, 0.8F, 1.5F);
            StomachLogic.say(pred, Component.m_237113_("Asked ").m_7220_(victim.m_7755_()).m_130946_(". They have 30 seconds to answer."));
         }
      }
   }

   public static void respond(ServerPlayer victim, boolean accept) {
      MinecraftServer server = victim.m_20194_();
      int now = server.m_129921_();
      Request r = (Request)PENDING.remove(victim.m_20148_());
      if (r != null && r.expires() > now) {
         ServerPlayer pred = server.m_6846_().m_11259_(r.pred());
         if (pred != null && pred.m_6084_()) {
            if (!accept) {
               DECLINED.put(key(pred.m_20148_(), victim.m_20148_()), now + 1200);
               victim.m_213846_(Component.m_237113_("You declined."));
               StomachLogic.say(pred, Component.m_237119_().m_7220_(victim.m_7755_()).m_130946_(" declined your request."));
            } else if (pred.m_9236_() == victim.m_9236_() && !(pred.m_20280_(victim) > (double)64.0F)) {
               StomachManager mgr = StomachManager.get(server);
               Stomach ps = mgr.of(pred.m_20148_());
               if (PlayerHolding.hardVore(ps) != r.hard()) {
                  victim.m_213846_(Component.m_237113_("The swallower changed mode since they asked, so nothing happened. Ask again."));
                  StomachLogic.say(pred, Component.m_237119_().m_7220_(victim.m_7755_()).m_130946_(" accepted, but you changed mode. Ask again."));
               } else {
                  if (PlayerHolding.swallow(pred, ps, victim, mgr)) {
                     StomachLogic.finishSwallow(pred, ps, victim.m_7755_());
                     mgr.m_77762_();
                     StomachLogic.sync(pred, ps);
                  } else {
                     victim.m_213846_(Component.m_237113_("You couldn't be swallowed right now."));
                  }

               }
            } else {
               victim.m_213846_(Component.m_237113_("They're too far away now."));
               StomachLogic.say(pred, Component.m_237119_().m_7220_(victim.m_7755_()).m_130946_(" accepted, but you're too far away. Ask again up close."));
            }
         } else {
            victim.m_213846_(Component.m_237113_("They're no longer around."));
         }
      } else {
         victim.m_213846_(Component.m_237113_("You have no swallow request waiting."));
      }
   }

   private static record Request(UUID pred, int expires, boolean hard) {
   }
}
