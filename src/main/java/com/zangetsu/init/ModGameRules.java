package com.zangetsu.init;

import net.minecraft.world.level.GameRules;

public class ModGameRules {
    public static GameRules.Key<GameRules.BooleanValue> RULE_ZANGETSU_GRIEFING;

    public static void init() {
        RULE_ZANGETSU_GRIEFING = GameRules.register(
                "zangetsuGriefing",
                GameRules.Category.MISC,
                GameRules.BooleanValue.create(true)
        );
    }
}
