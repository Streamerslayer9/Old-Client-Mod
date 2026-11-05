package com.example.gulp;

import java.util.concurrent.ThreadLocalRandom;

record ForcedOutcome(int hearts, int holdSeconds, float healScale, boolean scare, boolean rewards, String enterLine, String endLine, String exitLine, String[] idleLines) {
   String idleLine() {
      return this.idleLines[ThreadLocalRandom.current().nextInt(this.idleLines.length)];
   }

   private static String pick(String... lines) {
      return lines[ThreadLocalRandom.current().nextInt(lines.length)];
   }

   static ForcedOutcome of(int type, VillagerTrait temper) {
      boolean shy = temper == VillagerTrait.SHY;
      boolean bold = temper == VillagerTrait.CONFIDENT;
      if (type == 2) {
         return shy ? new ForcedOutcome(1, 0, 1.0F, false, true, pick("O-oh! You just... climbed in? Um... okay... I-I don't mind, really...", "Eep! Y-you could have asked first... but you're welcome in here..."), pick("I-I hope it was alright in there... you can visit again, if you like...", "Um... that was nice. You're always welcome..."), pick("Oh... was it too much? Sorry...", "Already? O-okay... come back any time..."), new String[]{"Is it comfy in there? ...Please say it's comfy...", "I'm trying not to wriggle. Sorry if I do...", "I can feel you moving... it's kind of nice, actually..."}) : new ForcedOutcome(bold ? 4 : 2, 0, 1.0F, false, true, pick("Ha! Someone's eager! Fine by me, make yourself at home!", "Oh, you just hop in uninvited? I like your style!"), pick("Out you go! The door's always open, you know that.", "That was fun! Come back any time."), pick("Leaving already? You barely got comfy!", "Aw, so soon? Fine, fine. Off you go."), new String[]{"Comfy? Of course it is. I run a good tummy.", "Don't mind the gurgling, that's just me being happy.", "Take your time in there. I've got nowhere to be."});
      } else if (type == 1) {
         return shy ? new ForcedOutcome(-12, 10, 0.0F, true, false, pick("E-eeek! Y-you can't just... get out, get out, get out!", "N-no no no! I didn't say yes! Please, please get out!"), pick("P-please don't ever do that again, I was so scared...", "I... I need to sit down. That was too much..."), pick("Y-you're leaving? Thank goodness...", "O-okay... okay... I'm okay..."), new String[]{"Please... please come out soon...", "I-I can't breathe properly...", "This is not okay... this is not okay..."}) : new ForcedOutcome(-8, 20, 0.5F, false, false, pick("Hey! I'm not that kind of villager! Who said you could climb in?!", "Excuse me?! Out! Out of my tummy!"), pick("That's enough. Out. And ask next time!", "Right, time's up. Don't push your luck."), pick("Good. Don't try that again.", "Finally. Some manners, please."), new String[]{"I'm giving you ten more seconds, then you're out.", "You're lucky I'm in a good mood. Barely.", "This is NOT how you ask someone."});
      } else if (shy) {
         return new ForcedOutcome(-9, 12, 0.0F, true, false, pick("W-what?! Why are you in my... oh no, oh no, oh no...", "H-hey! W-what are you doing in there?!"), pick("Please... don't ever do that again...", "I-I'm shaking... please don't..."), pick("U-um... okay, good... bye...", "Th-thank you for getting out..."), new String[]{"Is this... is this really happening?", "P-please hurry...", "I don't know what to do..."});
      } else {
         return bold ? new ForcedOutcome(-5, 25, 0.5F, false, false, pick("Well, that's... certainly new. Make it quick, friend.", "You climbed into me. Seriously? Okay. Quickly."), pick("Alright, that's enough of that. Out you go.", "Time's up, adventurer. Out."), pick("Fine, off you go. Weird way to say hello.", "Huh. Don't make it a habit."), new String[]{"I have errands, you know.", "This is not on my schedule today.", "Are you done yet?"}) : new ForcedOutcome(-6, 20, 0.5F, false, false, pick("Um. You can't just climb in there without asking. This is strange.", "Excuse me? That's... not normal. At all."), pick("Okay, that's enough. Please don't do that again.", "Right. Out. Let's pretend this didn't happen."), pick("Right. Let's pretend that didn't happen.", "Okay... off you go."), new String[]{"How long is this going to take?", "This is really awkward.", "I'm not sure how I feel about this."});
      }
   }
}
