package com.leafia.cursed.item;

import com.leafia.cursed.CursedItems;
import com.leafia.cursed.fuel.RodItems;
import com.leafia.cursed.fuel.RodSim;
import com.leafia.cursed.fuel.RodState;
import com.leafia.cursed.fuel.RodType;
import com.leafia.cursed.fuel.RodTypes;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import javax.annotation.Nullable;

/** One of the addon's 59 fuel rods. Heat/depletion live in the stack NBT, same keys as the 1.12.2 addon. */
public class RodItem extends Item {
    public final RodType type;

    public RodItem(RodType type) {
        super(new Item.Properties().stacksTo(1));
        this.type = type;
    }

    public static RodState readState(ItemStack stack) {
        RodState s = new RodState();
        CompoundTag t = stack.getTag();
        if (t == null || !t.contains("heat")) return s;
        s.heat = t.getDouble("heat");
        s.depletion = t.getDouble("depletion");
        s.decay = t.getDouble("decay");
        s.incoming = t.getDouble("incoming");
        s.cooled = t.getDouble("cooled");
        s.melting = t.getBoolean("melting");
        s.nuke = t.getBoolean("nuke");
        s.generosityTimer = t.contains("generosityTimer") ? t.getInt("generosityTimer") : 90;
        s.spillage = t.getInt("spillage");
        return s;
    }

    public static void writeState(ItemStack stack, RodState s) {
        CompoundTag t = stack.getOrCreateTag();
        t.putDouble("heat", s.heat);
        t.putDouble("depletion", s.depletion);
        t.putDouble("decay", s.decay);
        t.putDouble("incoming", s.incoming);
        t.putDouble("cooled", s.cooled);
        t.putBoolean("melting", s.melting);
        t.putBoolean("nuke", s.nuke);
        t.putInt("generosityTimer", s.generosityTimer);
        t.putInt("spillage", s.spillage);
    }

    /** A fresh stack of the given rod type carrying this state. */
    public static ItemStack stackOf(RodType type, RodState state) {
        var obj = RodItems.BY_ID.get(type.functionId);
        ItemStack st = new ItemStack(obj.get());
        writeState(st, state);
        return st;
    }

    private boolean finiteLife() { return type.life > 0.0 && type.life < 1.0E300; }

    @Override public boolean isBarVisible(ItemStack stack) { return finiteLife() && readState(stack).depletion > 0.0; }

    @Override public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * (float) Math.min(readState(stack).depletion / type.life, 1.0));
    }

    @Override public int getBarColor(ItemStack stack) {
        float f = (float) Math.min(readState(stack).depletion / type.life, 1.0);
        return Mth.hsvToRgb(Math.max(0.0f, 1.0f - f) / 3.0f, 1.0f, 1.0f);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        RodState s = readState(stack);
        if (type.meltingPoint > 0.0) tip.add(Component.literal("\u00a77Melting point: \u00a7c" + fmt(type.meltingPoint) + "\u00b0C"));
        if (finiteLife()) tip.add(Component.literal("\u00a77Depletion: \u00a7e" + String.format("%.1f%%", Math.min(s.depletion / type.life, 1.0) * 100.0)));
        if (s.heat > 20.5) tip.add(Component.literal("\u00a77Heat: \u00a76" + fmt(s.heat) + "\u00b0C"));
        if (s.melting) tip.add(Component.literal("\u00a74\u00a7lMELTING"));
        tip.add(Component.literal("\u00a78Reactivity " + type.reactivity + ", emission " + type.emission));
        if (type.decayProduct != null) {
            RodType next = RodTypes.ALL.get(type.decayProduct);
            if (next != null) tip.add(Component.literal("\u00a78Depletes into: " + next.label));
        }
    }

    private static String fmt(double v) { return v >= 1.0E9 ? String.format("%.2e", v) : String.format("%.0f", v); }
}
