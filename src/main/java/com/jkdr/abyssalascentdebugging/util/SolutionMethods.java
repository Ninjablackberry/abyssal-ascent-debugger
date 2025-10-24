package com.jkdr.abyssalascentdebugging.util;

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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.file.StandardCopyOption;

import com.jkdr.abyssalascentdebugging.FileData;
import com.jkdr.abyssalascentdebugging.AbyssalAscentDebugging;

public class SolutionMethods
{
    private final static Logger LOGGER = LogUtils.getLogger();
    private static final String relativePath = "assets/abyssalascentdebugging/staticFileReplacements/";

    public static Boolean genericFileDelete(FileData data) {
        Path destinationPath = data.getFilePath();

        if (!data.getFileExists()) {return false;}

        try {
            Files.delete(destinationPath);
            
            return true;
        } catch (IOException e) {
            LOGGER.warn("An error occured {}", e);
            return false;
        }
    }

    public static Boolean fixImmersivePortalsConfig(FileData data) {
        String currentFilePath = relativePath + data.getFileName();

        Path destinationPath = data.getFilePath();

        try (InputStream in = AbyssalAscentDebugging.class.getClassLoader().getResourceAsStream(currentFilePath)) {

            if (in == null) {
                LOGGER.error("CRITICAL: Could not find internal fix file: [{}]. This is a mod bug!", currentFilePath);
                return false; // Abort
            }

            Files.copy(in, destinationPath, StandardCopyOption.REPLACE_EXISTING);

            LOGGER.info("Successfully replaced file: {}", destinationPath);
            
            return true;

        } catch (IOException e) {
            LOGGER.error("Failed to replace file: {}", destinationPath, e);
            return false;
        } catch (Exception e) {
            LOGGER.error("An unknown error occurred during file replacement.", e);
            return false;
        }
    }
}
