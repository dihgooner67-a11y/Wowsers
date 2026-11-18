package com.leafia.cursed.wind;

import com.leafia.cursed.CursedMod;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class WindBlocks {
    private WindBlocks() {}
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, CursedMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CursedMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CursedMod.MODID);

    private static RegistryObject<Block> block(String name, Supplier<Block> sup) {
        RegistryObject<Block> b = BLOCKS.register(name, sup);
        ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties()));
        return b;
    }

    public static final RegistryObject<Block> TURBINE = block("wind_turbine_medium",
        () -> new WindTurbineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 10.0f).noOcclusion()));

    public static final RegistryObject<BlockEntityType<WindTurbineBlockEntity>> TURBINE_BE = BLOCK_ENTITIES.register("wind_turbine_medium",
        () -> BlockEntityType.Builder.of(WindTurbineBlockEntity::new, TURBINE.get()).build(null));
}
