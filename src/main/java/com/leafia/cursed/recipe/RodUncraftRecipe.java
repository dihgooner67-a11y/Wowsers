package com.leafia.cursed.recipe;

import com.leafia.cursed.CursedMod;
import com.leafia.cursed.fuel.RodState;
import com.leafia.cursed.item.RodItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Put a single addon rod in the crafting grid to get its base item (billet) back, as long as the rod has used up
 * less than 5% of its life (addon: LeafiaRodCrafting "generic_fuel_handler").
 */
public class RodUncraftRecipe extends CustomRecipe {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, CursedMod.MODID);
    public static final RegistryObject<RecipeSerializer<RodUncraftRecipe>> SERIALIZER =
        SERIALIZERS.register("rod_uncraft", () -> new SimpleCraftingRecipeSerializer<>(RodUncraftRecipe::new));

    public RodUncraftRecipe(ResourceLocation id, CraftingBookCategory category) { super(id, category); }

    private static ItemStack onlyStack(CraftingContainer inv) {
        ItemStack found = ItemStack.EMPTY;
        for (ItemStack s : inv.getItems()) {
            if (s.isEmpty()) continue;
            if (!found.isEmpty()) return ItemStack.EMPTY;
            found = s;
        }
        return found;
    }

    private static boolean fresh(RodItem rod, ItemStack stack) {
        RodState s = RodItem.readState(stack);
        return !(rod.type.life > 0.0 && s.depletion >= rod.type.life * 0.05);
    }

    @Override public boolean matches(CraftingContainer inv, Level level) {
        ItemStack s = onlyStack(inv);
        return s.getItem() instanceof RodItem r && r.type.baseItem != null && fresh(r, s);
    }

    @Override public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
        ItemStack s = onlyStack(inv);
        if (!(s.getItem() instanceof RodItem r) || r.type.baseItem == null || !fresh(r, s)) return ItemStack.EMPTY;
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("hbm", r.type.baseItem));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override public boolean canCraftInDimensions(int w, int h) { return w * h >= 1; }

    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }
}
