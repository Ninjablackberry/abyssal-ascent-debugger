package com.jkdr.abyssalascentdebugging.util;

import com.jkdr.abyssalascentdebugging.util.HashCreator;
import com.jkdr.abyssalascentdebugging.AbyssalAscentDebugging;
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
import java.util.EnumSet;

import java.lang.Runnable;
import java.util.List;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.mojang.datafixers.util.Pair;
import com.jkdr.abyssalascentdebugging.FileData;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.jkdr.abyssalascentdebugging.util.AutoResolutionMethod;

public class CustomErrorResolution
{
	public static enum ErrorCondition {
		FILE_NOT_FOUND,
		FILE_MODFIED,
		FILE_EXISTS,
		FILE_NOT_READABLE,
		FILE_NOT_WRITEABLE
	}

	public static enum Environment {
		CLIENT,
		SERVER,
		COMMON
	}

	private String errorReason;
	private String errorCode;
	private String errorName;
	public Boolean enforceError;

	private List<String> commonSolutions;
	private List<String> serverSpecificSolutions;
	private List<String> clientSpecificSolutions;

	private List<String> aiCommonSolutions;
	private List<String> aiFeedServerSpecificSolutions;
	private List<String> aiFeedClientSpecificSolutions;

	public EnumSet<ErrorCondition> errorConditions; 
	public Environment errorEnvironment;
	private AutoResolutionMethod autoResolution;
	
	private static final String aiPrefixNotice = "BELOW IS A SOLUTION TO THE USERS ERROR (THESE START FROM CLIENT SPECIFIC TO COMMON : Ensure to prioritise top to bottom methods and only list THE TOP TWO unless the user specifes the solution to the error didnt work)\n";

	

    private final Logger LOGGER = LogUtils.getLogger();

	public CustomErrorResolution(String errorCode, String errorName, EnumSet<ErrorCondition> condition, Environment env, String errorReason, Boolean enforceError, AutoResolutionMethod autoResolution) {
		this.errorReason = errorReason;
		this.errorCode = errorCode;
		this.errorEnvironment = env;
		this.errorConditions = condition;
		this.enforceError = enforceError;
		this.autoResolution = autoResolution;

		if (env == Environment.SERVER && AbyssalAscentDebugging.isSessionClient()) {this.enforceError = false;}
	}

	public CustomErrorResolution(String errorCode, String errorName, EnumSet<ErrorCondition> condition, Environment env, String errorReason, Boolean enforceError) {
		// Calls the main constructor using "this()"
		this(
			errorCode,
            errorName,
            condition,
			env,
			errorReason,
			enforceError,
			null
		);
	}

	public Boolean invokeAutoResolutionMethod(FileData data) {
		if (this.autoResolution != null) {
			Boolean result = this.autoResolution.method.test(data);
			data.setErrorActuallyResolved(result);
			return result; // Call .test() and return its boolean
		}
		return false;
	}

	public String getAutoResolutionName() {
		if (this.autoResolution == null) {return null;}
		return this.autoResolution.name;
	}
	
	public Boolean hasAutoResolutionMethod() {
		return (this.autoResolution != null);
	}

	public String getErrorCode() {
		return this.errorCode;
	}

	public String getErrorReason() {
		return this.errorReason;
	}

	public String getError() {
		return "(" + this.errorCode + ")" + this.errorName;
	}

	public void addGenericSolution(String solution, CustomErrorResolution... othersToApply) {
		if (othersToApply != null) {for (CustomErrorResolution other : othersToApply) {other.addGenericSolution(solution);}}
		this.commonSolutions = addToList(this.commonSolutions, solution);
	}

	public void addServerSolution(String solution, CustomErrorResolution... othersToApply) {
		if (othersToApply != null) {for (CustomErrorResolution other : othersToApply) {other.addServerSolution(solution);}}
		this.serverSpecificSolutions = addToList(this.serverSpecificSolutions, solution);
	}

	public void addClientSolution(String solution, CustomErrorResolution... othersToApply) {
		if (othersToApply != null) {for (CustomErrorResolution other : othersToApply) {other.addClientSolution(solution);}}
		this.clientSpecificSolutions = addToList(this.clientSpecificSolutions, solution);
	}

	public void addAIGenericSolution(String solution, CustomErrorResolution... othersToApply) {
		if (othersToApply != null) {for (CustomErrorResolution other : othersToApply) {other.addAIGenericSolution(solution);}}
		this.aiCommonSolutions = addToList(this.aiCommonSolutions, solution);
	}

	public void addAIServerSolution(String solution, CustomErrorResolution... othersToApply) {
		if (othersToApply != null) {for (CustomErrorResolution other : othersToApply) {other.addAIServerSolution(solution);}}
		this.aiFeedServerSpecificSolutions = addToList(this.aiFeedServerSpecificSolutions, solution);
	}

	public void addAIClientSolution(String solution, CustomErrorResolution... othersToApply) {
		if (othersToApply != null) {for (CustomErrorResolution other : othersToApply) {other.addAIClientSolution(solution);}}
		this.aiFeedClientSpecificSolutions = addToList(this.aiFeedClientSpecificSolutions, solution);
	}



	public String getClientSolutions() {
		//Ensure all lists are defined

		List<String> combinedList = new ArrayList<>();
		if (this.clientSpecificSolutions != null) {combinedList.addAll(this.clientSpecificSolutions);}
		if (this.commonSolutions != null) {combinedList.addAll(this.commonSolutions);}

		if (combinedList.size() == 0) {combinedList.add("Sorry! no solutions have been added :(");}

		return String.join("\n", convertToNumberedList(combinedList));
	}

	public String getServerSolutions() {
		//Ensure all lists are defined
		List<String> combinedList = new ArrayList<>();
		if (this.serverSpecificSolutions != null) {combinedList.addAll(this.serverSpecificSolutions);}
		if (this.commonSolutions != null) {combinedList.addAll(this.commonSolutions);}

		if (combinedList.size() == 0) {combinedList.add("Sorry! no solutions have been added :(");}

		return String.join("\n", convertToNumberedList(combinedList));
	}




	public String getClientAIFeed() {
		//Ensure all lists are defined
		List<String> combinedList = new ArrayList<>();
		if (this.aiFeedClientSpecificSolutions != null) {combinedList.addAll(this.aiFeedClientSpecificSolutions);}
		if (this.aiCommonSolutions != null) {combinedList.addAll(this.aiCommonSolutions);}

		if (combinedList.size() == 0) {combinedList.add("No manual solutions provided by the developers, you will need to help the user yourself");}
		combinedList.add("As a last resort the user can ignore this warning.");

		return aiPrefixNotice + String.join("\n\n", convertToNumberedList(combinedList));
	}

	public String getServerAIFeed() {
		//Ensure all lists are defined
		List<String> combinedList = new ArrayList<>();
		if (this.aiFeedServerSpecificSolutions != null) {combinedList.addAll(this.aiFeedServerSpecificSolutions);}
		if (this.aiCommonSolutions != null) {combinedList.addAll(this.aiCommonSolutions);}

		if (combinedList.size() == 0) {combinedList.add("No manual solutions provided by the developers, you will need to help the user yourself");}

		return aiPrefixNotice+ String.join("\n\n", convertToNumberedList(combinedList));
	}

	private static List<String> convertToNumberedList(List<String> list) {
		if (list.size() == 1) {return list;}
		return IntStream.range(0, list.size())
			.mapToObj(i -> (i + 1) + ". " + list.get(i))
			.collect(Collectors.toList());
	}

	private static List<String> addToList(List<String> list, String element) {
		if (list == null) {list = new ArrayList<>();}
		if (element != null) {list.add(element);}
		return list;
	}
}
