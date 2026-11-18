package com.leafia.cursed.heat;

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

/** FORGE-A: heat machines. */
public final class HeatBlocks {
    private HeatBlocks() {}
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, CursedMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CursedMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CursedMod.MODID);

    private static RegistryObject<Block> block(String name, Supplier<Block> sup) {
        RegistryObject<Block> b = BLOCKS.register(name, sup);
        ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties()));
        return b;
    }

    public static final RegistryObject<Block> RTG_HEATER = block("rtg_heater",
        () -> new RtgHeaterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 20.0f)));

    public static final RegistryObject<Block> HP_BOILER = block("hp_boiler",
        () -> new HpBoilerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 20.0f)));

    public static final RegistryObject<Block> COOLANT_HEATEX = block("coolant_heatex",
        () -> new CoolantHeatexBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 20.0f)));

    public static final RegistryObject<BlockEntityType<CoolantHeatexBlockEntity>> COOLANT_HEATEX_BE = BLOCK_ENTITIES.register("coolant_heatex",
        () -> BlockEntityType.Builder.of(CoolantHeatexBlockEntity::new, COOLANT_HEATEX.get()).build(null));
    public static final RegistryObject<BlockEntityType<HpBoilerBlockEntity>> HP_BOILER_BE = BLOCK_ENTITIES.register("hp_boiler",
        () -> BlockEntityType.Builder.of(HpBoilerBlockEntity::new, HP_BOILER.get()).build(null));
    public static final RegistryObject<BlockEntityType<RtgHeaterBlockEntity>> RTG_HEATER_BE = BLOCK_ENTITIES.register("rtg_heater",
        () -> BlockEntityType.Builder.of(RtgHeaterBlockEntity::new, RTG_HEATER.get()).build(null));
}
