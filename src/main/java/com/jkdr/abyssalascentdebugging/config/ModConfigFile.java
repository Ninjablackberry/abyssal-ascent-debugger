package com.jkdr.abyssalascentdebugging.config; // Or your preferred package

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;


public class ModConfigFile {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> IGNORED_ERRORS;

    static {
        BUILDER.push("Error Management");

        IGNORED_ERRORS = BUILDER
                .comment(
                    "A list of errors that the user has chosen to ignore.",
                    "Errors are stored in the format 'filepath:errorcode'.",
                    "Do not edit this manually unless you know what you are doing."
                )
                .defineList("ignoredErrors", new ArrayList<>(), obj -> obj instanceof String);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}