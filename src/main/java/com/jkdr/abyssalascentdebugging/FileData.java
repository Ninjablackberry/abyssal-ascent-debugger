package com.jkdr.abyssalascentdebugging;

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


public class FileData
{
	private String filePath;
	public String reservedHash;

	public String resolveError;

	public Boolean forceSameHash;

    private List<CustomErrorResolution> errorHandlers;

    private Boolean exists;
    private String hash;
    private Path path;
    private File file;

    public Boolean errorMarkedAsResolved = false;
    private Boolean errorActuallyResolved = false;

    private CustomErrorResolution errorResult;

    private final Logger LOGGER = LogUtils.getLogger();

	public FileData(String filePath, List<CustomErrorResolution> error, String reservedHash) {
		this.filePath = filePath;
        this.errorHandlers = error;
		this.reservedHash = reservedHash;
    
	}

    public FileData(String filePath, List<CustomErrorResolution> error) {
		// Calls the main constructor using "this()"
		this(
			filePath,
            error,
            null
		);
	}

    public File getFile() {
        if (file == null && this.getFileExists()) {
            file = new File(this.getFilePath().toString());
        }
        return file;
    }

    public Boolean getFileExists() {
        if (this.exists == null) {
            this.exists = Files.exists(this.getFilePath());
        }

        return this.exists;
    }

    public String getLocalFilePath() {
        return this.filePath;
    }

    public void setErrorActuallyResolved(boolean value) {
        if (value == false) {return;}
        this.errorActuallyResolved = value;
    }

    public boolean getActuallyResolved() {
        return this.errorActuallyResolved;
    }

    public void removeIgnoreList() {
        String warningKey = this.filePath+":"+this.getFileError().getErrorCode();
        List<? extends String> currentList = ModConfigFile.IGNORED_ERRORS.get();

        List<String> newList = new ArrayList<>(currentList);

        boolean wasRemoved = newList.remove(warningKey);

        if (wasRemoved) {
            ModConfigFile.IGNORED_ERRORS.set(newList);
        }
    }

    public void addIgnoreList() {
        String warningKey = this.filePath+":"+this.getFileError().getErrorCode();
        if (ModConfigFile.IGNORED_ERRORS.get().contains(warningKey)) {
            //Remove from ignore list
            return;
        }

        // Example: Ignoring a new warning
        List<String> newList = new ArrayList<>(ModConfigFile.IGNORED_ERRORS.get());
        if (!newList.contains(warningKey)) {
            newList.add(warningKey);
            ModConfigFile.IGNORED_ERRORS.set(newList);
        }
    }

    public String getHash() {
        try {
            if (this.hash == null) {
                this.hash = HashCreator.calculateSHA256(this.getFilePath());
            }
            return hash;
        } catch (IOException | NoSuchAlgorithmException e) {}
        return "";
    }

    public Path getFilePath() {
        if (this.path == null) {
            Path gameDir = FMLPaths.GAMEDIR.get();
            this.path = gameDir.resolve(this.filePath);
        }
        return this.path;

        
    }

    public CustomErrorResolution getFileError() {
        if (errorResult == null) {errorResult = errorResolutionCheck();}
        return errorResult;
    }

    private Boolean isCombinationDisabled(CustomErrorResolution error) {
        if (error != null) {
            String ignoreKey = this.filePath + ":" + error.getErrorCode();

            if (ModConfigFile.IGNORED_ERRORS.get().contains(ignoreKey)) {
                return true;
            }
        }
        return false;
    }

    private CustomErrorResolution errorResolutionCheck() {
        Boolean doesFileExist = this.getFileExists();
        Boolean doesFileMatch = this.fileMatchesHash();

        Pair<Integer, CustomErrorResolution> lowestSetValue = new Pair<Integer, CustomErrorResolution>(null, null);
        for (CustomErrorResolution error : errorHandlers) {
            //Previous value is in ignore list
            

            //If the error is only for the client but the current game isnt a client then this error isnt for us
            if (error.errorEnvironment == CustomErrorResolution.Environment.CLIENT && !AbyssalAscentDebugging.isSessionClient()) {continue;}
            if (lowestSetValue.getSecond() != null) {
                if (!lowestSetValue.getSecond().enforceError && error.enforceError) {
                    if (!this.isCombinationDisabled(error)) {lowestSetValue = new Pair<Integer, CustomErrorResolution>(null, null);}
                }
            }

            if (!doesFileExist) {
                if (error.errorConditions.contains(CustomErrorResolution.ErrorCondition.FILE_NOT_FOUND)) {
                    if (!this.isCombinationDisabled(error)) {lowestSetValue = new Pair<Integer, CustomErrorResolution>(0, error);}
                }
                continue; //We dont continue the cycle if the file doesnt exist.
            }
            //This logic will never get called if the file doesnt exist so if there is an error where the file is FILE_MODFIED add a FILE_NOT_FOUND error condition
            if (error.errorConditions.contains(CustomErrorResolution.ErrorCondition.FILE_NOT_READABLE) && !this.getFile().canRead()) {
                if (
                    lowestSetValue.getSecond() == null || 
                    lowestSetValue.getFirst() > 1
                ) {
                    if (!this.isCombinationDisabled(error)) {lowestSetValue = new Pair<Integer, CustomErrorResolution>(1, error);}//File doesnt exist and this error handles it.
                }
            }
            if (error.errorConditions.contains(CustomErrorResolution.ErrorCondition.FILE_NOT_WRITEABLE) && !this.getFile().canWrite()) {
                if (
                    lowestSetValue.getSecond() == null || 
                    lowestSetValue.getFirst() > 2
                ) {
                    if (!this.isCombinationDisabled(error)) {
                        lowestSetValue = new Pair<Integer, CustomErrorResolution>(2, error);}//File doesnt exist and this error handles it.
                    }
            }
            if (error.errorConditions.contains(CustomErrorResolution.ErrorCondition.FILE_EXISTS)) {
                if (
                    lowestSetValue.getSecond() == null || 
                    lowestSetValue.getFirst() > 3
                ) {
                    if (!this.isCombinationDisabled(error)) {lowestSetValue = new Pair<Integer, CustomErrorResolution>(3, error);}//File doesnt exist and this error handles it.
                    }
            }
            if (error.errorConditions.contains(CustomErrorResolution.ErrorCondition.FILE_MODFIED) && !doesFileMatch) {
                if (
                    lowestSetValue.getSecond() == null || 
                    lowestSetValue.getFirst() > 4
                ) {
                    if (!this.isCombinationDisabled(error)) {lowestSetValue = new Pair<Integer, CustomErrorResolution>(4, error);}
                }
            }

        }

        return lowestSetValue.getSecond();
    }

    public String getFileName()  {
        return this.getFilePath().getFileName().toString();
    }

	public Boolean fileMatchesHash() {
        if (!this.getFileExists()) {
            return false;
        }
        if (this.reservedHash == null) {return true;}
        String actualHash = this.getHash();
        return this.reservedHash.equalsIgnoreCase(actualHash);
    }

    public String resolutionToClient() {
        CustomErrorResolution currentError = this.getFileError();
        return this.getFileName() + "\n\nLocation: " + this.getFilePath().toString() + 
        "\n\nError Code: "+currentError.getErrorCode()+"\nAdditional information: " + currentError.getErrorReason() + "\n\n\nSolutions:\n"+currentError.getClientSolutions();
    }

    public String resolutionToServer() {
        return "";
    }
}
