package com.tom.cpm.api;

/** Compile-time stand-in for Custom Player Models' plugin interface. Not included in the jar. */
public interface ICPMPlugin {
   void initClient(IClientAPI api);

   void initCommon(ICommonAPI api);

   String getOwnerModId();
}
