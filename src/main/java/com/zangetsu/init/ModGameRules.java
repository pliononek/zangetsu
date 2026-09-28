package com.zangetsu.init;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public class ModGameRules {
    public static GameRules.Key<GameRules.BooleanValue> RULE_GETSUGA_DESTRUCTION;
    public static GameRules.Key<GameRules.BooleanValue> RULE_GETSUGA_DESTRUCTION_LOWER;
    public static GameRules.Key<GameRules.BooleanValue> RULE_ZANGETSU_GRIEFING;

    @SafeVarargs
    private static void sync(MinecraftServer server, boolean val, GameRules.Key<GameRules.BooleanValue>... others) {
        if (server == null) return;
        for (GameRules.Key<GameRules.BooleanValue> key : others) {
            if (key != null) {
                GameRules.BooleanValue rule = server.getGameRules().getRule(key);
                if (rule != null && rule.get() != val) {
                    rule.set(val, server);
                }
            }
        }
    }

    public static void init() {
        RULE_GETSUGA_DESTRUCTION = GameRules.register(
                "getsugaDestruction",
                GameRules.Category.MISC,
                GameRules.BooleanValue.create(true, (server, value) -> {
                    sync(server, value.get(), RULE_GETSUGA_DESTRUCTION_LOWER, RULE_ZANGETSU_GRIEFING);
                })
        );

        RULE_GETSUGA_DESTRUCTION_LOWER = GameRules.register(
                "getsugadestruction",
                GameRules.Category.MISC,
                GameRules.BooleanValue.create(true, (server, value) -> {
                    sync(server, value.get(), RULE_GETSUGA_DESTRUCTION, RULE_ZANGETSU_GRIEFING);
                })
        );

        RULE_ZANGETSU_GRIEFING = GameRules.register(
                "zangetsuGriefing",
                GameRules.Category.MISC,
                GameRules.BooleanValue.create(true, (server, value) -> {
                    sync(server, value.get(), RULE_GETSUGA_DESTRUCTION, RULE_GETSUGA_DESTRUCTION_LOWER);
                })
        );
    }

    public static boolean isGetsugaDestructionAllowed(Level level) {
        if (level == null) return true;
        GameRules rules = level.getGameRules();
        boolean primary = RULE_GETSUGA_DESTRUCTION == null || rules.getBoolean(RULE_GETSUGA_DESTRUCTION);
        boolean lower = RULE_GETSUGA_DESTRUCTION_LOWER == null || rules.getBoolean(RULE_GETSUGA_DESTRUCTION_LOWER);
        boolean griefing = RULE_ZANGETSU_GRIEFING == null || rules.getBoolean(RULE_ZANGETSU_GRIEFING);
        return primary && lower && griefing;
    }
}
