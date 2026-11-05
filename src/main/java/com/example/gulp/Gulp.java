package com.example.gulp;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod("gulp")
public class Gulp {
   public static final String ID = "gulp";
   public static final String STORAGE_TITLE = "Stomach Storage";
   public static final DeferredRegister<Item> ITEMS;
   public static final RegistryObject<Item> BUTTER;
   public static final DeferredRegister<SoundEvent> SOUNDS;
   public static final RegistryObject<SoundEvent> SWALLOW;
   public static final RegistryObject<SoundEvent> DIGEST;
   public static final RegistryObject<SoundEvent> RELEASE;
   public static final RegistryObject<SoundEvent> DIGEST_BUTTON;
   public static final RegistryObject<SoundEvent> SCREEN_LOOP;
   public static final RegistryObject<SoundEvent> SWALLOW_LOUD;
   public static final RegistryObject<SoundEvent> RELEASE_LOUD;
   public static final RegistryObject<SoundEvent> DIGEST_LOUD;
   public static final RegistryObject<SoundEvent> DIGEST_BUTTON_LOUD;

   private static RegistryObject<SoundEvent> sound(String name) {
      return SOUNDS.register(name, () -> SoundEvent.m_262824_(new ResourceLocation("gulp", name)));
   }

   public Gulp() {
      IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
      SOUNDS.register(modBus);
      ITEMS.register(modBus);
      modBus.addListener(this::creativeTabs);
      ModLoadingContext.get().registerConfig(Type.SERVER, GulpServerConfig.SPEC);
      modBus.addListener(this::commonSetup);
   }

   private void creativeTabs(BuildCreativeModeTabContentsEvent event) {
      if (event.getTabKey() == CreativeModeTabs.f_256839_) {
         event.accept(BUTTER);
      }

   }

   private void commonSetup(FMLCommonSetupEvent event) {
      event.enqueueWork(Net::register);
      event.enqueueWork(FlamevoreBridge::register);
   }

   static {
      ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "gulp");
      BUTTER = ITEMS.register("butter", () -> new Item((new Item.Properties()).m_41489_((new FoodProperties.Builder()).m_38760_(3).m_38758_(0.5F).m_38767_())));
      SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "gulp");
      SWALLOW = sound("swallow");
      DIGEST = sound("digest");
      RELEASE = sound("release");
      DIGEST_BUTTON = sound("digest_button");
      SCREEN_LOOP = sound("screen_loop");
      SWALLOW_LOUD = sound("swallow_loud");
      RELEASE_LOUD = sound("release_loud");
      DIGEST_LOUD = sound("digest_loud");
      DIGEST_BUTTON_LOUD = sound("digest_button_loud");
   }
}
