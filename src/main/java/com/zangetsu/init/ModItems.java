package com.zangetsu.init;

import com.zangetsu.ZangetsuMod;
import com.zangetsu.item.AsauchiItem;
import com.zangetsu.item.TensaZangetsuItem;
import com.zangetsu.item.ZangetsuShikaiItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ZangetsuMod.MODID);

    public static final DeferredHolder<Item, AsauchiItem> ASAUCHI =
            ITEMS.register("asauchi", AsauchiItem::new);

    public static final DeferredHolder<Item, ZangetsuShikaiItem> ZANGETSU_SHIKAI =
            ITEMS.register("zangetsu_shikai", ZangetsuShikaiItem::new);

    public static final DeferredHolder<Item, TensaZangetsuItem> TENSA_ZANGETSU =
            ITEMS.register("tensa_zangetsu", TensaZangetsuItem::new);

    public static final DeferredHolder<Item, com.zangetsu.item.DomainExpansionItem> DOMAIN_EXPANSION =
            ITEMS.register("domain_expansion", com.zangetsu.item.DomainExpansionItem::new);

    public static final DeferredHolder<Item, SwordItem> HOLLOW_ZANGETSU =
            ITEMS.register("hollow_zangetsu", () -> new SwordItem(Tiers.NETHERITE, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 3, -2.2f))
                    .component(net.minecraft.core.component.DataComponents.UNBREAKABLE, new net.minecraft.world.item.component.Unbreakable(true))
                    .fireResistant()) {
                @Override
                public boolean isDamageable(ItemStack stack) { return false; }
                @Override
                public boolean isBarVisible(ItemStack stack) { return false; }
                @Override
                public boolean hurtEnemy(ItemStack stack, net.minecraft.world.entity.LivingEntity target, net.minecraft.world.entity.LivingEntity attacker) { return true; }
                @Override
                public boolean mineBlock(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos, net.minecraft.world.entity.LivingEntity entity) { return true; }
            });

    // --- Shikai Shinigami Shihakusho ---
    public static final DeferredHolder<Item, com.zangetsu.item.ShihakushoArmorItem> SHIKAI_CHESTPLATE =
            ITEMS.register("shikai_chestplate", () -> new com.zangetsu.item.ShihakushoArmorItem(
                    ModArmorMaterials.SHIKAI_SHIHAKUSHO, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, false));

    public static final DeferredHolder<Item, com.zangetsu.item.ShihakushoArmorItem> SHIKAI_LEGGINGS =
            ITEMS.register("shikai_leggings", () -> new com.zangetsu.item.ShihakushoArmorItem(
                    ModArmorMaterials.SHIKAI_SHIHAKUSHO, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, false));

    public static final DeferredHolder<Item, com.zangetsu.item.ShihakushoArmorItem> SHIKAI_BOOTS =
            ITEMS.register("shikai_boots", () -> new com.zangetsu.item.ShihakushoArmorItem(
                    ModArmorMaterials.SHIKAI_SHIHAKUSHO, net.minecraft.world.item.ArmorItem.Type.BOOTS, false));

    // --- Bankai Tensa Zangetsu Shihakusho ---
    public static final DeferredHolder<Item, com.zangetsu.item.ShihakushoArmorItem> BANKAI_CHESTPLATE =
            ITEMS.register("bankai_chestplate", () -> new com.zangetsu.item.ShihakushoArmorItem(
                    ModArmorMaterials.BANKAI_SHIHAKUSHO, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, true));

    public static final DeferredHolder<Item, com.zangetsu.item.ShihakushoArmorItem> BANKAI_LEGGINGS =
            ITEMS.register("bankai_leggings", () -> new com.zangetsu.item.ShihakushoArmorItem(
                    ModArmorMaterials.BANKAI_SHIHAKUSHO, net.minecraft.world.item.ArmorItem.Type.LEGGINGS, true));

    public static final DeferredHolder<Item, com.zangetsu.item.ShihakushoArmorItem> BANKAI_BOOTS =
            ITEMS.register("bankai_boots", () -> new com.zangetsu.item.ShihakushoArmorItem(
                    ModArmorMaterials.BANKAI_SHIHAKUSHO, net.minecraft.world.item.ArmorItem.Type.BOOTS, true));
}
