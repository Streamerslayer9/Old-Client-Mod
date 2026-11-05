package com.example.gulp;

import java.util.concurrent.ThreadLocalRandom;

public enum VillagerTrait {
   NONE("", 0.5F, (double)1.0F, (double)1.0F, 1.0F, 1.0F),
   SHY("Shy", 0.4F, (double)1.25F, (double)1.5F, 1.0F, 1.15F),
   CONFIDENT("Confident", 0.6F, 0.8, (double)0.75F, 1.15F, 1.0F);

   public final String label;
   public final float comfortHpFraction;
   public final double heartsMult;
   public final double cooldownMult;
   public final float healMult;
   public final float holdMult;

   private VillagerTrait(String label, float comfortHpFraction, double heartsMult, double cooldownMult, float healMult, float holdMult) {
      this.label = label;
      this.comfortHpFraction = comfortHpFraction;
      this.heartsMult = heartsMult;
      this.cooldownMult = cooldownMult;
      this.healMult = healMult;
      this.holdMult = holdMult;
   }

   public static VillagerTrait byId(int id) {
      VillagerTrait[] v = values();
      return id >= 0 && id < v.length ? v[id] : NONE;
   }

   public String comfortOfferLine() {
      String var10000;
      switch (this) {
         case CONFIDENT -> var10000 = "Whoa, you're hurt! Come here, let me patch you up in my tummy!";
         case SHY -> var10000 = "Um... you look hurt. Would you like to rest inside me for a bit? I'll be careful...";
         case NONE -> var10000 = "You look hurt. Would you like to rest in my tummy until you feel better?";
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public String holdLine() {
      String var10000;
      switch (this) {
         case CONFIDENT -> var10000 = "Got you! Just relax, I've done this before.";
         case SHY -> var10000 = "Okay... there. Is it alright in there? Tell me if you want out...";
         case NONE -> var10000 = "There you go. Rest up.";
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public String finishLine() {
      String var10000;
      switch (this) {
         case CONFIDENT -> var10000 = "Good as new! Anytime you need a tummy, you know where to find me.";
         case SHY -> var10000 = "Oh, you're better... I'm glad. Um, thank you for trusting me.";
         case NONE -> var10000 = "All done. Take care of yourself.";
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public String tradeDiscountLine() {
      String var10000;
      switch (this) {
         case CONFIDENT -> var10000 = "For you? Best prices in the village!";
         case SHY -> var10000 = "Um... I made it a little cheaper for you. Since you're nice to me...";
         case NONE -> var10000 = "Friends get a better price.";
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public String tradeSurchargeLine() {
      String var10000;
      switch (this) {
         case CONFIDENT -> var10000 = "Prices are what they are. Take it or leave it.";
         case SHY -> var10000 = "S-sorry... it costs a bit more for people I don't know well.";
         case NONE -> var10000 = "I don't know you well enough for the usual price.";
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public String snubLine() {
      String var10000;
      switch (this) {
         case CONFIDENT -> var10000 = "Hurt? Sounds like your problem. Maybe be nicer to people next time.";
         case SHY -> var10000 = "Um... you look hurt, but... I don't really want to be near you right now.";
         case NONE -> var10000 = "I'm not helping you.";
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public String swallowedWhileHoldingLine() {
      String var10000;
      switch (this) {
         case CONFIDENT -> var10000 = "Whoa! Hey! I was in the middle of something here!";
         case SHY -> var10000 = "E-eep! I had someone in there! Please be careful with them!";
         case NONE -> var10000 = "Wait, I wasn't done! I had someone in there!";
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public String storageRefuseLine() {
      String var10000;
      switch (this) {
         case CONFIDENT -> var10000 = "Hmm, no. I don't know you well enough to let you rummage through my things.";
         case SHY -> var10000 = "Um... I'd rather you didn't look through my things. Sorry...";
         case NONE -> var10000 = "I'd rather keep my things to myself for now.";
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public String cuddleOfferLine() {
      String var10000;
      switch (this) {
         case CONFIDENT -> var10000 = "Hey you! Feel like a cuddle in my tummy? I'm in a great mood!";
         case SHY -> var10000 = "Um... would you like to... stay in my tummy for a little while? Only if you want to...";
         case NONE -> var10000 = "Would you like a cosy rest in my tummy?";
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public String cuddleHoldLine() {
      String var10000;
      switch (this) {
         case CONFIDENT -> var10000 = "Come here, you. There. Nice and cosy, right?";
         case SHY -> var10000 = "T-there... is that okay? I'm happy you said yes...";
         case NONE -> var10000 = "There you go. Nice and cosy.";
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public String cuddleFinishLine() {
      String var10000;
      switch (this) {
         case CONFIDENT -> var10000 = "That was great! We should do that again soon.";
         case SHY -> var10000 = "Thank you for staying... that meant a lot to me.";
         case NONE -> var10000 = "That was nice. Thanks for the company.";
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public String earlyExitLine() {
      String var10000;
      switch (this) {
         case CONFIDENT -> var10000 = "Already? Fine, but don't push yourself!";
         case SHY -> var10000 = "O-okay, I hope I didn't do anything wrong...";
         case NONE -> var10000 = "Okay, take it easy.";
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public String holdIdleLine() {
      String[] var10000;
      switch (this) {
         case CONFIDENT -> var10000 = new String[]{"Doing okay in there? Good.", "You're my favourite passenger, you know that?", "Don't worry about a thing. I've got you.", "Stay as long as you need!"};
         case SHY -> var10000 = new String[]{"Is it okay in there...? I hope it's okay...", "I'm trying to be gentle... tell me if I'm not...", "Um... I'm glad you trust me.", "You can stay as long as you like... I don't mind..."};
         case NONE -> var10000 = new String[]{"Doing alright in there?", "Rest as long as you need to.", "Nearly there."};
         default -> throw new IncompatibleClassChangeError();
      }

      String[] lines = var10000;
      return lines[ThreadLocalRandom.current().nextInt(lines.length)];
   }

   public String convertAcceptLine() {
      String var10000;
      switch (this) {
         case CONFIDENT -> var10000 = "Me, a pred? Heh, I thought you'd never ask! Come here, I'll take good care of you.";
         case SHY -> var10000 = "Y-you want me to... be a pred? For you? ...O-okay. I trust you. I'll try my best, I promise.";
         case NONE -> var10000 = "A pred, huh? Sure, why not. I trust you, so let's give it a try.";
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public String convertRefuseLine() {
      String var10000;
      switch (this) {
         case CONFIDENT -> var10000 = "Whoa there. We're not close enough for that. Come back when I know you better.";
         case SHY -> var10000 = "U-um... I'm sorry, I don't know you well enough for that yet... maybe later?";
         case NONE -> var10000 = "That's a big thing to ask. Let's get to know each other better first.";
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public String convertAlreadyLine() {
      String var10000;
      switch (this) {
         case CONFIDENT -> var10000 = "Ha! I'm already one, remember? Hop in any time.";
         case SHY -> var10000 = "U-um... I already am one, remember? ...You're always welcome.";
         case NONE -> var10000 = "I'm already a pred, you know.";
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public String convertAfterLine() {
      String var10000;
      switch (this) {
         case CONFIDENT -> var10000 = "Pred life! I've got a good feeling about this. Don't be a stranger!";
         case SHY -> var10000 = "I... I'm a pred now. It feels strange. But a good strange... thank you for believing in me.";
         case NONE -> var10000 = "It feels a little different already. Thanks for trusting me with this.";
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public String hostileGrabLine() {
      String var10000;
      switch (this) {
         case CONFIDENT -> var10000 = "I've had it with you! Get in here and think about what you've done!";
         case SHY -> var10000 = "I-I'm sorry, I can't take it anymore... you hurt me, so... please stay in there for a bit...";
         case NONE -> var10000 = "That's enough out of you. In you go.";
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public String hostileIdleLine() {
      String[] var10000;
      switch (this) {
         case CONFIDENT -> var10000 = new String[]{"Squirm all you like. You're staying until I say so.", "Maybe next time you'll be nicer to people.", "Oh, you don't like it in there? Good."};
         case SHY -> var10000 = new String[]{"I'm sorry... I just needed you to stop...", "Please stop moving... I don't want to hurt you, I just... I'm upset...", "I hope you understand why I did this..."};
         case NONE -> var10000 = new String[]{"You're staying put for a while.", "Think about it.", "I'm not letting you out just yet."};
         default -> throw new IncompatibleClassChangeError();
      }

      String[] lines = var10000;
      return lines[ThreadLocalRandom.current().nextInt(lines.length)];
   }

   public String hostileStruggleLine() {
      String var10000;
      switch (this) {
         case CONFIDENT -> var10000 = "Ha! Is that all you've got?";
         case SHY -> var10000 = "E-ow! Please don't... I'm not letting go yet...";
         case NONE -> var10000 = "Stop wriggling!";
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public String hostileEscapeLine() {
      String var10000;
      switch (this) {
         case CONFIDENT -> var10000 = "Tch! Fine, get out of my sight. And don't come back until you've learned some manners!";
         case SHY -> var10000 = "Oh... you got out... I-I'm sorry, I just... please stay away from me for a while...";
         case NONE -> var10000 = "Hmph. Fine. Just leave me alone.";
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public String hostileEndLine() {
      String var10000;
      switch (this) {
         case CONFIDENT -> var10000 = "That's enough. Out. Next time, treat me better and none of this happens.";
         case SHY -> var10000 = "O-okay... you can go now... I hope we can be friends again some day...";
         case NONE -> var10000 = "Alright, you can go. Don't make me do that again.";
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public String hostileHoldRefuseLine() {
      String var10000;
      switch (this) {
         case CONFIDENT -> var10000 = "Nice try. You're not leaving until I'm done with you.";
         case SHY -> var10000 = "S-sorry! I can't let you out yet...";
         case NONE -> var10000 = "No. You stay.";
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public String hostileDigestLine() {
      String[] var10000;
      switch (this) {
         case CONFIDENT -> var10000 = new String[]{"Well, well. Now all of this is mine. Shouldn't have been so rude!", "Delicious. And look at all this loot! Well now, it's my stuff.", "That's what you get. Thanks for the donation, it's all mine now."};
         case SHY -> var10000 = new String[]{"I-I'm sorry... I didn't want it to end like this... I guess your things are mine now...", "Oh no... it's over... I'll... I'll look after your things. They're mine now, I suppose...", "I told you to stay away... Um, I'll keep your belongings safe. They're mine now, sorry..."};
         case NONE -> var10000 = new String[]{"Well, that's that. Your things are mine now.", "You brought this on yourself. I'll take your stuff.", "Done. Well, now it's my stuff."};
         default -> throw new IncompatibleClassChangeError();
      }

      String[] lines = var10000;
      return lines[ThreadLocalRandom.current().nextInt(lines.length)];
   }

   public String hostileWarnLine() {
      String var10000;
      switch (this) {
         case CONFIDENT -> var10000 = "Getting tired already? Struggle harder, or I'll make you part of me.";
         case SHY -> var10000 = "P-please get out soon... I don't want to have to... digest you...";
         case NONE -> var10000 = "You're running out of time in there.";
         default -> throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   // $FF: synthetic method
   private static VillagerTrait[] $values() {
      return new VillagerTrait[]{NONE, SHY, CONFIDENT};
   }
}
