package com.jkdr.abyssalascentdebugging.util;

import com.jkdr.abyssalascentdebugging.util.HashCreator;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import java.util.ArrayList;
import com.jkdr.abyssalascentdebugging.config.ModConfigFile;
import java.io.File;

import java.util.List;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;

import com.mojang.datafixers.util.Pair;
import com.jkdr.abyssalascentdebugging.util.CustomErrorResolution;
import com.jkdr.abyssalascentdebugging.AbyssalAscentDebugging;
import com.jkdr.abyssalascentdebugging.FileData;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class AutoResolutionMethod
{
	public String name;
    public final Predicate<FileData> method;

    private final Logger LOGGER = LogUtils.getLogger();

	public AutoResolutionMethod(Predicate<FileData> method, String name) {
        this.name = name;
        this.method = method;
	}

    public AutoResolutionMethod(Predicate<FileData> method) {
        this(
            method,
            "Auto Resolve"
        );
	}
}
