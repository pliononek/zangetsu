package com.zangetsu.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class AsauchiItem extends SwordItem {
    public AsauchiItem() {
        super(Tiers.NETHERITE, new Properties()
                .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 7, -2.4f))
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                .fireResistant());
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true; // Unbreakable: does not consume durability on attack
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        return true; // Unbreakable: does not consume durability on mining
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            player.sendSystemMessage(Component.literal("§7[Jinzen Ritual] §fArrange a §e3x3 of White Carpets§f, stand in the center holding Asauchi, and §bhold Sneak (Shift) for 3 seconds§f to enter your Inner World."));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
