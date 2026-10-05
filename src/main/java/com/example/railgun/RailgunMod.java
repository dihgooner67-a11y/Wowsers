package com.example.railgun;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(RailgunMod.MOD_ID)
public class RailgunMod {
    public static final String MOD_ID = "railgun";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MOD_ID);

    /** Unbreakable glowing animated starfield block for the Infinite Void domain. */
    public static final RegistryObject<Block> VOID_STARS = BLOCKS.register("void_stars",
            () -> new Block(BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f).noLootTable().lightLevel(s -> 11)));

    /** The one custom effect particle every power uses (see Vfx / FxParticle). */
    public static final RegistryObject<ParticleType<FxOptions>> FX = PARTICLES.register("fx",
            () -> new ParticleType<FxOptions>(false, FxOptions.DESERIALIZER) {
                @Override public Codec<FxOptions> codec() { return FxOptions.CODEC; }
            });

    private static Item.Properties epic() { return new Item.Properties().stacksTo(1).rarity(Rarity.EPIC); }

    public static final RegistryObject<Item> RAILGUN = ITEMS.register("railgun", () -> new RailgunItem(epic()));
    public static final RegistryObject<Item> VOID_ECLIPSE = ITEMS.register("void_eclipse", () -> new DomainItem(epic(), DomainType.POCKET));
    public static final RegistryObject<Item> FLAME_GLOVE = ITEMS.register("flame_glove", () -> new GloveItem(epic().fireResistant()));
    public static final RegistryObject<Item> MALEVOLENT_SHRINE = ITEMS.register("malevolent_shrine", () -> new DomainItem(epic(), DomainType.SHRINE));
    public static final RegistryObject<Item> INFINITE_VOID = ITEMS.register("infinite_void", () -> new DomainItem(epic(), DomainType.VOID));
    public static final RegistryObject<Item> HOMETOWN_MEMORIES = ITEMS.register("hometown_memories", () -> new DomainItem(epic(), DomainType.HOMETOWN));
    public static final RegistryObject<Item> HOLLOW_PURPLE = ITEMS.register("hollow_purple", () -> new PurpleItem(epic()));
    public static final RegistryObject<Item> MECH_BEAM = ITEMS.register("mech_beam", () -> new MechItem(epic().fireResistant()));

    public static final RegistryObject<Item> BLACK_HOLE = ITEMS.register("unlimited_black_hole", () -> new BlackHoleItem(epic()));
    public static final RegistryObject<Item> CURSED_FISTS = ITEMS.register("cursed_fists", () -> new CursedFistsItem(epic().fireResistant()));

    public RailgunMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        BLOCKS.register(bus);
        PARTICLES.register(bus);
        bus.addListener(this::creativeTab);
        Net.init();
    }

    private void creativeTab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.COMBAT) {
            e.accept(RAILGUN); e.accept(FLAME_GLOVE); e.accept(HOLLOW_PURPLE); e.accept(MECH_BEAM);
            e.accept(VOID_ECLIPSE); e.accept(MALEVOLENT_SHRINE); e.accept(INFINITE_VOID); e.accept(HOMETOWN_MEMORIES);
            e.accept(BLACK_HOLE); e.accept(CURSED_FISTS);
        }
    }
}
