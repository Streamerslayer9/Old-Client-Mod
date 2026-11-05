package com.example.gulp;

public final class CpmBridge {
   private static volatile Client client;

   private CpmBridge() {
   }

   static void setClient(Client c) {
      client = c;
   }

   public static boolean available() {
      return client != null;
   }

   public static int maxValue(String animation) {
      Client c = client;
      if (c == null) {
         return -1;
      } else {
         try {
            return c.maxValue(animation);
         } catch (Throwable var3) {
            return -1;
         }
      }
   }

   public static boolean play(String animation, int value) {
      Client c = client;
      if (c == null) {
         return false;
      } else {
         try {
            return c.play(animation, value);
         } catch (Throwable var4) {
            return false;
         }
      }
   }

   public interface Client {
      boolean play(String var1, int var2);

      int maxValue(String var1);
   }
}
