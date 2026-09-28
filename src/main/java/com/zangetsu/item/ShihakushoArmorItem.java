package com.zangetsu.item;

import com.zangetsu.init.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Unbreakable;

import java.util.List;

public class ShihakushoArmorItem extends ArmorItem {
    private final boolean bankai;

    public ShihakushoArmorItem(Holder<ArmorMaterial> material, Type type, boolean bankai) {
        super(material, type, new Item.Properties()
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                .fireResistant());
        this.bankai = bankai;
    }

    public boolean isBankai() {
        return bankai;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    public static boolean isWearingFullShikaiSet(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.SHIKAI_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.SHIKAI_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.SHIKAI_BOOTS.get());
    }

    public static boolean isWearingFullBankaiSet(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.BANKAI_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.BANKAI_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.BANKAI_BOOTS.get());
    }

    public static void transformToBankai(Player player) {
        swapArmor(player, EquipmentSlot.CHEST, ModItems.SHIKAI_CHESTPLATE.get(), ModItems.BANKAI_CHESTPLATE.get());
        swapArmor(player, EquipmentSlot.LEGS, ModItems.SHIKAI_LEGGINGS.get(), ModItems.BANKAI_LEGGINGS.get());
        swapArmor(player, EquipmentSlot.FEET, ModItems.SHIKAI_BOOTS.get(), ModItems.BANKAI_BOOTS.get());
    }

    public static void revertToShikai(Player player) {
        swapArmor(player, EquipmentSlot.CHEST, ModItems.BANKAI_CHESTPLATE.get(), ModItems.SHIKAI_CHESTPLATE.get());
        swapArmor(player, EquipmentSlot.LEGS, ModItems.BANKAI_LEGGINGS.get(), ModItems.SHIKAI_LEGGINGS.get());
        swapArmor(player, EquipmentSlot.FEET, ModItems.BANKAI_BOOTS.get(), ModItems.SHIKAI_BOOTS.get());
    }

    private static void swapArmor(Player player, EquipmentSlot slot, Item fromItem, Item toItem) {
        ItemStack current = player.getItemBySlot(slot);
        if (current.is(fromItem)) {
            ItemStack transformed = new ItemStack(toItem);
            if (current.has(DataComponents.ENCHANTMENTS)) {
                transformed.set(DataComponents.ENCHANTMENTS, current.get(DataComponents.ENCHANTMENTS));
            }
            if (current.has(DataComponents.CUSTOM_NAME)) {
                transformed.set(DataComponents.CUSTOM_NAME, current.get(DataComponents.CUSTOM_NAME));
            }
            player.setItemSlot(slot, transformed);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        if (bankai) {
            tooltipComponents.add(Component.literal("§4§l[TENSA ZANGETSU BANKAI]"));
            tooltipComponents.add(Component.literal("§7Szata skondensowanego ciśnienia duchowego Ichigo Kurosakiego."));
            tooltipComponents.add(Component.literal("§c• Bankai Speed: §fStała Szybkość II"));
            tooltipComponents.add(Component.literal("§c• Spiritual Power: §fOdporność II & Siła II"));
            tooltipComponents.add(Component.literal("§c• Getsuga Boost: §f+30% obrażeń Getsuga Tenshō"));
            tooltipComponents.add(Component.literal("§c• Shunpo Mastery: §fKoszt Reiatsu zmniejszony do 5"));
            tooltipComponents.add(Component.literal("§8Niezniszczalny pancerz wykuty z Reiatsu"));
        } else {
            tooltipComponents.add(Component.literal("§b§l[SHINIGAMI SHIHAKUSHO]"));
            tooltipComponents.add(Component.literal("§7Tradycyjne szaty boga śmierci noszone przy Zangetsu Shikai."));
            tooltipComponents.add(Component.literal("§b• Reiatsu Regen: §f+2.0 Reiatsu/s (pełny set)"));
            tooltipComponents.add(Component.literal("§b• Soul Capacity: §f+150 Max Reiatsu"));
            tooltipComponents.add(Component.literal("§b• Getsuga Mastery: §f-20% koszt Reiatsu"));
            tooltipComponents.add(Component.literal("§b• Defense: §fOdporność I"));
            tooltipComponents.add(Component.literal("§8Niezniszczalny pancerz duchowy"));
        }
    }
}
