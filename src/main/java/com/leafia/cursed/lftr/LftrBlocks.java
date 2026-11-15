package com.leafia.cursed.lftr;

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

/** LFTR (molten salt reactor) blocks: fuel element, control rod, control rod extension. */
public final class LftrBlocks {
    private LftrBlocks() {}
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, CursedMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CursedMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CursedMod.MODID);

    private static BlockBehaviour.Properties props() { return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 12.0f); }

    private static RegistryObject<Block> block(String name, Supplier<Block> sup) {
        RegistryObject<Block> b = BLOCKS.register(name, sup);
        ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties()));
        return b;
    }

    public static final RegistryObject<Block> ELEMENT = block("lftr_element", () -> new MsrElementBlock(props()));
    public static final RegistryObject<Block> CONTROL = block("lftr_control", () -> new MsrControlBlock(props()));
    public static final RegistryObject<Block> EXCHANGER = block("lftr_heat_exchanger", () -> new MsrHeatExchangerBlock(props()));
    public static final RegistryObject<Block> EXTENSION = block("lftr_extension", () -> new Block(props()));

    public static final RegistryObject<BlockEntityType<MsrElementBlockEntity>> ELEMENT_BE = BLOCK_ENTITIES.register("lftr_element",
        () -> BlockEntityType.Builder.of(MsrElementBlockEntity::new, ELEMENT.get()).build(null));
    public static final RegistryObject<BlockEntityType<MsrHeatExchangerBlockEntity>> EXCHANGER_BE = BLOCK_ENTITIES.register("lftr_heat_exchanger",
        () -> BlockEntityType.Builder.of(MsrHeatExchangerBlockEntity::new, EXCHANGER.get()).build(null));
    public static final RegistryObject<BlockEntityType<MsrControlBlockEntity>> CONTROL_BE = BLOCK_ENTITIES.register("lftr_control",
        () -> BlockEntityType.Builder.of(MsrControlBlockEntity::new, CONTROL.get()).build(null));
}
