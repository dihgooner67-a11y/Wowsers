package com.leafia.cursed.apr;

import com.leafia.cursed.CursedMod;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registers the APR core and its structure component blocks (cursed:apr_*). */
public final class AprBlocks {
    private AprBlocks() {}
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, CursedMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CursedMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CursedMod.MODID);

    private static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 20.0f);
    }

    private static RegistryObject<Block> block(String name, java.util.function.Supplier<Block> sup) {
        RegistryObject<Block> b = BLOCKS.register(name, sup);
        ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties()));
        return b;
    }

    public static final RegistryObject<Block> CORE = block("apr_core", () -> new AprCoreBlock(props()));
    public static final RegistryObject<Block> OUTLET = block("apr_outlet", () -> new Block(props()));
    public static final RegistryObject<Block> PLATING = block("apr_plating", () -> new Block(props()));
    public static final RegistryObject<Block> PLATING_DARK = block("apr_plating_dark", () -> new Block(props()));
    public static final RegistryObject<Block> MECHANICAL = block("apr_mechanical", () -> new Block(props()));
    public static final RegistryObject<Block> COIL = block("apr_coil", () -> new Block(props()));
    public static final RegistryObject<Block> SUPPORT = block("apr_support", () -> new Block(props()));
    public static final RegistryObject<Block> HULL = block("apr_hull", () -> new Block(props()));
    public static final RegistryObject<Block> HULL_ASSEMBLED = block("apr_hull_assembled", () -> new Block(props()));
    public static final RegistryObject<Block> BLADES = block("apr_blades", () -> new Block(props()));

    public static final RegistryObject<BlockEntityType<AprCoreBlockEntity>> CORE_BE = BLOCK_ENTITIES.register("apr_core",
        () -> BlockEntityType.Builder.of(AprCoreBlockEntity::new, CORE.get()).build(null));

    private static final Map<AprStructure.Kind, RegistryObject<Block>> BY_KIND = new EnumMap<>(AprStructure.Kind.class);
    static {
        BY_KIND.put(AprStructure.Kind.PLATING, PLATING);
        BY_KIND.put(AprStructure.Kind.PLATING_DARK, PLATING_DARK);
        BY_KIND.put(AprStructure.Kind.MECHANICAL, MECHANICAL);
        BY_KIND.put(AprStructure.Kind.COIL, COIL);
        BY_KIND.put(AprStructure.Kind.SUPPORT, SUPPORT);
        BY_KIND.put(AprStructure.Kind.HULL, HULL);
        BY_KIND.put(AprStructure.Kind.BLADES, BLADES);
        BY_KIND.put(AprStructure.Kind.OUTLET, OUTLET);
    }

    /** The block a structure kind stands for (null for AIR). */
    public static Block blockFor(AprStructure.Kind kind) {
        RegistryObject<Block> o = BY_KIND.get(kind);
        return o == null ? null : o.get();
    }
}
