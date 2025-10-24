package com.jkdr.abyssalascentdebugging;

// --- Import the new classes we need ---
// --- FIX: Correct the import path for FatalErrorScreen ---
import com.jkdr.abyssalascentdebugging.util.FatalErrorScreen;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
// --- NEW IMPORTS ---
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
// ... (your existing imports) ...
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent; // --- NEW IMPORT ---
import net.minecraftforge.common.MinecraftForge;
// --- FIX: Add missing imports ---
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLLoader;
// ... (your existing imports) ...
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent; // --- Import Client Setup ---
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
// ... (your existing imports) ...
import org.slf4j.Logger;

import java.io.IOException;
// ... (your existing imports) ...
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;

import java.util.List;
import java.util.ArrayList;

import com.jkdr.abyssalascentdebugging.FileData;
import com.jkdr.abyssalascentdebugging.FileCheckSystemConfig;
import com.jkdr.abyssalascentdebugging.util.CustomErrorResolution;
import net.minecraftforge.fml.ModLoadingContext;

import com.jkdr.abyssalascentdebugging.config.ModConfigFile;


// The value here should match an entry in the META-INF/mods.toml file
@Mod(AbyssalAscentDebugging.MODID)
public class AbyssalAscentDebugging
{

    public static final String MODID = "abyssalascentdebugging";
    private static final Logger LOGGER = LogUtils.getLogger();


    private boolean hasClientCheckRun = false;
    private static Boolean isClient;
    

    public static Boolean isSessionClient() {
        if (isClient == null) {
            isClient = FMLLoader.getDist() == Dist.DEDICATED_SERVER;
        }
        return isClient;
    }


    public AbyssalAscentDebugging() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        FileCheckSystemConfig.initSolutions();
        
        modBus.addListener(this::commonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModConfigFile.SPEC, "abyssalascentdebugging-common.toml");

        if (FMLLoader.getDist() == Dist.CLIENT) {
            MinecraftForge.EVENT_BUS.addListener(this::onScreenInit);
        }
    }

    // This runs on BOTH client and server
    @SubscribeEvent
    public void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Common setup running.");

        //We check if the environment is a dedicated server (this method will crash the server instead of displaying a screen)
        if (FMLLoader.getDist() == Dist.DEDICATED_SERVER) {
            LOGGER.info("Running file integrity check on server...");
            event.enqueueWork(() -> this.runFileCheckAndCrashServer());
        }
    }

    @SubscribeEvent
    public void onScreenInit(final ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen && !this.hasClientCheckRun) {
            LOGGER.info("Main menu screen loaded. Running client file integrity check...");
            
            //Variable to ensure this check doesnt run every time a screen is displayed.
            this.hasClientCheckRun = true;
            
            this.runFileCheckAndShowScreen();
        }
    }


    private List<FileData> performFileCheck() {
        List<FileData> errorData = new ArrayList<>();
        
        FileCheckSystemConfig.fileDataList.forEach((fileData) -> {
            if (fileData.getFileError() != null) {errorData.add(fileData);}
        });

        return errorData;
    }

    /**errorType
     * Called by the SERVER.
     * Performs the check and crashes the server if errors are found.
     */
    private void runFileCheckAndCrashServer() {
        List<FileData> errorList = this.performFileCheck();
        
        if (!errorList.isEmpty()) {
            // --- FIX: Use .error() instead of .fatal() ---
            LOGGER.error("!!! --- FILE INTEGRITY CHECK FAILED --- !!!");
            LOGGER.error("Server startup will be aborted.");
            // This is the correct way to stop a server from starting
            throw new RuntimeException("Server startup aborted due to file integrity errors. Check logs for details.");
        } else {
            LOGGER.info("File integrity check PASSED on server. Proceeding with startup.");
        }
    }

    /**
     * Called by the CLIENT.
     * Performs the check and shows an error screen if errors are found.
     */
    private void runFileCheckAndShowScreen() {
        List<FileData> errorList = this.performFileCheck();

        if (!errorList.isEmpty()) {
            Minecraft.getInstance().setScreen(new FatalErrorScreen(errorList));
        } else {
            LOGGER.info("File integrity check PASSED on client.");
        }
    }
}

