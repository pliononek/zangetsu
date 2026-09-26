package com.zangetsu.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final String KEY_CATEGORY = "key.category.zangetsu";

    public static final KeyMapping BANKAI_KEY = new KeyMapping(
            "key.zangetsu.bankai",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            KEY_CATEGORY
    );

    public static final KeyMapping SHUNPO_KEY = new KeyMapping(
            "key.zangetsu.shunpo",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            KEY_CATEGORY
    );

    public static final KeyMapping INFUSION_KEY = new KeyMapping(
            "key.zangetsu.infusion",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KEY_CATEGORY
    );

    public static final KeyMapping TOGGLE_SLASH_KEY = new KeyMapping(
            "key.zangetsu.toggle_slash",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KEY_CATEGORY
    );
}
