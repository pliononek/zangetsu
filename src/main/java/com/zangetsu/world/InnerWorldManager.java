package com.zangetsu.world;

import com.zangetsu.ZangetsuMod;
import com.zangetsu.entity.InnerZangetsuEntity;
import com.zangetsu.init.ModDataAttachments;
import com.zangetsu.init.ModEntities;
import com.zangetsu.init.ModItems;
import com.zangetsu.init.ModSounds;
import com.zangetsu.init.ReiatsuData;
import com.zangetsu.network.SyncReiatsuPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class InnerWorldManager {
    public static final ResourceKey<Level> INNER_WORLD_KEY =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(ZangetsuMod.MODID, "inner_world"));

    public static void buildArena(ServerLevel level) {
        BlockPos center = new BlockPos(0, 100, 0);

        // 1. Build Main Skyscraper Arena (X: -13 to 13, Z: -26 to 26)
        BlockState deepslate = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        BlockState blackstone = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState grayConcrete = Blocks.GRAY_CONCRETE.defaultBlockState();
        BlockState whiteConcrete = Blocks.WHITE_CONCRETE.defaultBlockState();
        BlockState tintedGlass = Blocks.TINTED_GLASS.defaultBlockState();
        BlockState cyanGlass = Blocks.CYAN_STAINED_GLASS.defaultBlockState();
        BlockState seaLantern = Blocks.SEA_LANTERN.defaultBlockState();
        BlockState ironBars = Blocks.IRON_BARS.defaultBlockState();
        BlockState barrier = Blocks.BARRIER.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int x = -13; x <= 13; x++) {
            for (int z = -26; z <= 26; z++) {
                BlockPos floorPos = center.offset(x, 0, z);

                // Perimeter railing and protective barriers
                if (Math.abs(x) == 13 || Math.abs(z) == 26) {
                    level.setBlock(floorPos, deepslate, 2);
                    level.setBlock(floorPos.above(), ironBars, 2);
                    level.setBlock(floorPos.above(2), barrier, 2);
                    level.setBlock(floorPos.above(3), barrier, 2);
                    level.setBlock(floorPos.above(4), barrier, 2);
                } else {
                    // Skyscraper window grid & structural girders
                    if (z % 4 == 0 || Math.abs(x) == 6) {
                        // Structural steel beam
                        level.setBlock(floorPos, blackstone, 2);
                        level.setBlock(floorPos.below(), grayConcrete, 2);
                    } else if (z % 2 == 0) {
                        // Glass office window looking into lit interior
                        level.setBlock(floorPos, tintedGlass, 2);
                        level.setBlock(floorPos.below(), seaLantern, 2);
                        level.setBlock(floorPos.below(2), whiteConcrete, 2);
                    } else {
                        level.setBlock(floorPos, grayConcrete, 2);
                        level.setBlock(floorPos.below(), deepslate, 2);
                    }
                }

                // Clear playing volume above
                for (int y = 1; y <= 8; y++) {
                    if (Math.abs(x) < 13 && Math.abs(z) < 26) {
                        level.setBlock(floorPos.above(y), air, 2);
                    }
                }
            }
        }

        // Rooftop Antennas & Spires at arena edges
        for (int zEnd : new int[]{-26, 26}) {
            for (int xOffset : new int[]{-10, -4, 4, 10}) {
                BlockPos antennaBase = center.offset(xOffset, 1, zEnd);
                level.setBlock(antennaBase, Blocks.LIGHTNING_ROD.defaultBlockState(), 2);
                level.setBlock(antennaBase.above(), Blocks.END_ROD.defaultBlockState(), 2);
            }
        }

        // 2. Panoramic Background Sideways Cityscape (8 Massive Horizontal Skyscrapers)
        // Building 1: Left Close Modern Tower (Z-axis horizontal)
        buildZTower(level, -35, -23, 86, 116, -65, 65, whiteConcrete, cyanGlass, seaLantern);

        // Building 2: Right Close Corporate Highrise (Z-axis horizontal)
        buildZTower(level, 23, 35, 90, 120, -65, 65, blackstone, tintedGlass, Blocks.OCHRE_FROGLIGHT.defaultBlockState());

        // Building 3: Left High Titan Tower (Z-axis horizontal)
        buildZTower(level, -65, -45, 130, 160, -80, 80, grayConcrete, tintedGlass, seaLantern);

        // Building 4: Right High Titan Tower (Z-axis horizontal)
        buildZTower(level, 45, 65, 128, 158, -80, 80, deepslate, cyanGlass, seaLantern);

        // Building 5: Upper Perpendicular Overpass Tower (X-axis horizontal)
        buildXTower(level, -75, 75, 125, 137, 45, 58, whiteConcrete, cyanGlass, seaLantern);

        // Building 6: Rear Perpendicular Underpass Tower (X-axis horizontal)
        buildXTower(level, -75, 75, 72, 84, -60, -47, blackstone, tintedGlass, deepslate);

        // Building 7: Abyss Bottom Left Tower (Z-axis horizontal)
        buildZTower(level, -48, -26, 42, 66, -70, 70, deepslate, tintedGlass, grayConcrete);

        // Building 8: Abyss Bottom Right Tower (Z-axis horizontal)
        buildZTower(level, 26, 48, 38, 62, -70, 70, whiteConcrete, cyanGlass, seaLantern);
    }

    private static void buildZTower(ServerLevel level, int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
                                    BlockState frame, BlockState glass, BlockState light) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                boolean isEdgeX = (x == minX || x == maxX);
                boolean isEdgeY = (y == minY || y == maxY);

                for (int z = minZ; z <= maxZ; z += 2) {
                    pos.set(x, y, z);
                    if (isEdgeX || isEdgeY) {
                        if (z % 6 == 0) {
                            level.setBlock(pos, light, 2);
                        } else if (z % 3 == 0) {
                            level.setBlock(pos, frame, 2);
                        } else {
                            level.setBlock(pos, glass, 2);
                        }
                    }
                }
            }
        }
    }

    private static void buildXTower(ServerLevel level, int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
                                    BlockState frame, BlockState glass, BlockState light) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int z = minZ; z <= maxZ; z++) {
            for (int y = minY; y <= maxY; y++) {
                boolean isEdgeZ = (z == minZ || z == maxZ);
                boolean isEdgeY = (y == minY || y == maxY);

                for (int x = minX; x <= maxX; x += 2) {
                    pos.set(x, y, z);
                    if (isEdgeZ || isEdgeY) {
                        if (x % 6 == 0) {
                            level.setBlock(pos, light, 2);
                        } else if (x % 3 == 0) {
                            level.setBlock(pos, frame, 2);
                        } else {
                            level.setBlock(pos, glass, 2);
                        }
                    }
                }
            }
        }
    }

    public static void enterInnerWorld(ServerPlayer player) {
        ServerLevel innerWorld = player.server.getLevel(INNER_WORLD_KEY);
        if (innerWorld == null) {
            ZangetsuMod.LOGGER.error("Failed to find Inner World dimension!");
            return;
        }

        // Save origin location
        ReiatsuData data = player.getData(ModDataAttachments.REIATSU);
        data.setReturnLocation(
                player.level().dimension().location().toString(),
                player.getX(),
                player.getY(),
                player.getZ()
        );

        // Build panoramic sideways skyscraper arena and set clear bright noon daylight!
        buildArena(innerWorld);
        innerWorld.setWeatherParameters(100000, 0, false, false); // Clear sunny sky
        innerWorld.setDayTime(6000); // High noon sun

        // Teleport player
        player.teleportTo(innerWorld, 0.5, 101.0, -18.5, 0.0f, 0.0f);
        player.fallDistance = 0.0f;

        // Clean up any remaining bosses across the entire dimension
        for (Entity entity : innerWorld.getAllEntities()) {
            if (entity instanceof InnerZangetsuEntity b) {
                b.cleanupAndDiscard();
            }
        }

        // Determine trial mode: Asauchi = Shikai Trial, Shikai/Bankai = Hardcore Bankai Trial
        ItemStack held = player.getMainHandItem();
        if (!held.is(ModItems.ASAUCHI.get()) && !held.is(ModItems.ZANGETSU_SHIKAI.get())) {
            held = player.getOffhandItem();
        }
        boolean isBankaiTrial = held.is(ModItems.ZANGETSU_SHIKAI.get()) || held.is(ModItems.TENSA_ZANGETSU.get());

        // Summon fresh Hollow Ichigo
        InnerZangetsuEntity boss = new InnerZangetsuEntity(ModEntities.INNER_ZANGETSU.get(), innerWorld, isBankaiTrial);
        boss.setPos(0.5, 101.0, 18.5);
        boss.setTarget(player);
        innerWorld.addFreshEntity(boss);

        // Audio & Atmosphere
        innerWorld.playSound(null, player.blockPosition(), ModSounds.INNER_LAUGH.get(), SoundSource.HOSTILE, 2.5f, 1.0f);
        if (isBankaiTrial) {
            player.sendSystemMessage(Component.literal("§4§l========================================"));
            player.sendSystemMessage(Component.literal("§c§l[PRÓBA BANKAI] §fTwój wewnętrzny Hollow dobył §4§lTENSA ZANGETSU§f!"));
            player.sendSystemMessage(Component.literal("§eMusisz go pokonać w bezlitosnej walce, aby posiąść moc Bankai!"));
            player.sendSystemMessage(Component.literal("§4§l========================================"));
            innerWorld.playSound(null, player.blockPosition(), ModSounds.BANKAI.get(), SoundSource.HOSTILE, 2.5f, 0.9f);
        } else {
            player.sendSystemMessage(Component.translatable("message.zangetsu.trial_start"));
        }
    }

    public static void returnFromInnerWorld(ServerPlayer player, boolean victory) {
        // Discard any bosses remaining in Inner World immediately
        ServerLevel innerWorld = player.server.getLevel(INNER_WORLD_KEY);
        if (innerWorld != null) {
            for (Entity entity : innerWorld.getAllEntities()) {
                if (entity instanceof InnerZangetsuEntity b) {
                    b.cleanupAndDiscard();
                }
            }
        }

        ReiatsuData data = player.getData(ModDataAttachments.REIATSU);
        ResourceLocation targetDimLoc = ResourceLocation.parse(data.getReturnDim());
        ResourceKey<Level> targetKey = ResourceKey.create(Registries.DIMENSION, targetDimLoc);
        ServerLevel targetLevel = player.server.getLevel(targetKey);

        if (targetLevel == null) {
            targetLevel = player.server.overworld();
        }

        double destX = data.getReturnX();
        double destY = data.getReturnY();
        double destZ = data.getReturnZ();

        player.teleportTo(targetLevel, destX, destY, destZ, player.getYRot(), player.getXRot());
        player.fallDistance = 0.0f;

        if (victory) {
            ItemStack mainHand = player.getMainHandItem();
            data.setBankaiUnlocked(true);
            player.getPersistentData().putBoolean("ZangetsuBankaiUnlocked", true);

            // Immediate sync of unlock state to client!
            PacketDistributor.sendToPlayer(player, new SyncReiatsuPayload(
                    data.getCurrent(),
                    data.getMax(),
                    data.isInfusionActive(),
                    player.getPersistentData().getBoolean("ZangetsuSlashVertical"),
                    true
            ));

            if (mainHand.is(ModItems.ASAUCHI.get())) {
                player.setItemInHand(player.getUsedItemHand(), new ItemStack(ModItems.ZANGETSU_SHIKAI.get()));
                player.sendSystemMessage(Component.translatable("message.zangetsu.trial_victory"));
            } else if (!mainHand.is(ModItems.ZANGETSU_SHIKAI.get()) && !mainHand.is(ModItems.TENSA_ZANGETSU.get())) {
                player.getInventory().add(new ItemStack(ModItems.ZANGETSU_SHIKAI.get()));
            }

            player.sendSystemMessage(Component.literal("§4§l========================================"));
            player.sendSystemMessage(Component.literal("§6§l[BANKAI ODBLOKOWANE!] §aPokonałeś swojego wewnętrznego Hollowa!"));
            player.sendSystemMessage(Component.literal("§fPrzytrzymaj §e[B]§f trzymając Zangetsu, aby uwolnić §4§lTENSA ZANGETSU§f!"));
            player.sendSystemMessage(Component.literal("§4§l========================================"));
            targetLevel.playSound(null, player.blockPosition(), ModSounds.BANKAI.get(), SoundSource.PLAYERS, 2.5f, 1.0f);
            targetLevel.playSound(null, player.blockPosition(), ModSounds.REIATSU_BURST.get(), SoundSource.PLAYERS, 2.0f, 1.0f);
        } else {
            player.setHealth(2.0f); // 1 heart
            player.sendSystemMessage(Component.translatable("message.zangetsu.trial_failed"));
            targetLevel.playSound(null, player.blockPosition(), ModSounds.REIATSU_BURST.get(), SoundSource.PLAYERS, 1.5f, 0.7f);
        }
    }
}
