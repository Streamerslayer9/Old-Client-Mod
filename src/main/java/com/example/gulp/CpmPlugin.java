package com.example.gulp;

import com.tom.cpm.api.CPMPlugin;
import com.tom.cpm.api.ICPMPlugin;
import com.tom.cpm.api.IClientAPI;
import com.tom.cpm.api.ICommonAPI;

@CPMPlugin
public class CpmPlugin implements ICPMPlugin {
   public void initClient(final IClientAPI api) {
      CpmBridge.setClient(new CpmBridge.Client() {
         public boolean play(String animation, int value) {
            return api.playAnimation(animation, value);
         }

         public int maxValue(String animation) {
            return api.getAnimationMaxValue(animation);
         }
      });
   }

   public void initCommon(ICommonAPI api) {
   }

   public String getOwnerModId() {
      return "gulp";
   }
}
