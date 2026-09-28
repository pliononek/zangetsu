package com.zangetsu.init;

import com.zangetsu.ZangetsuMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ZangetsuMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ZANGETSU_TAB =
            CREATIVE_MODE_TABS.register("zangetsu_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.zangetsu"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> new ItemStack(ModItems.TENSA_ZANGETSU.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.ASAUCHI.get());
                        output.accept(ModItems.ZANGETSU_SHIKAI.get());
                        output.accept(ModItems.HOLLOW_ZANGETSU.get());
                        output.accept(ModItems.TENSA_ZANGETSU.get());
                        output.accept(ModItems.DOMAIN_EXPANSION.get());
                        output.accept(ModItems.SHIKAI_CHESTPLATE.get());
                        output.accept(ModItems.SHIKAI_LEGGINGS.get());
                        output.accept(ModItems.SHIKAI_BOOTS.get());
                        output.accept(ModItems.BANKAI_CHESTPLATE.get());
                        output.accept(ModItems.BANKAI_LEGGINGS.get());
                        output.accept(ModItems.BANKAI_BOOTS.get());
                    }).build());
}
