package com.tom.cpm.api;

/** Compile-time stand-in for the few Custom Player Models client API methods Gulp uses. Not included in the jar. */
public interface IClientAPI {
   boolean playAnimation(String animation, int value);

   int getAnimationMaxValue(String animation);
}
