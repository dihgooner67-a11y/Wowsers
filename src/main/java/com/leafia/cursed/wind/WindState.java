package com.leafia.cursed.wind;

import com.leafia.cursed.CursedMod;
import java.util.Random;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The one global wind (server side), ticked with the overworld and saved with the world. */
@Mod.EventBusSubscriber(modid = CursedMod.MODID)
public final class WindState {
    private WindState() {}

    public static final WindSim SIM = new WindSim(new Random());
    private static WindSavedData data;

    /** Persists the wind in the overworld's data storage. */
    public static class WindSavedData extends SavedData {
        public static WindSavedData load(CompoundTag t) {
            SIM.power = t.getDouble("power"); SIM.angle = t.getDouble("angle"); SIM.lastAngle = t.getDouble("lastAngle");
            SIM.startPower = t.getDouble("startPower"); SIM.endPower = t.getDouble("endPower");
            SIM.startAngle = t.getDouble("startAngle"); SIM.endAngle = t.getDouble("endAngle");
            SIM.strongWind = t.getBoolean("strong");
            SIM.shortCounter = t.getInt("shortCounter"); SIM.shortLength = Math.max(1, t.getInt("shortLength"));
            SIM.longCounter = t.getInt("longCounter"); SIM.longLength = Math.max(1, t.getInt("longLength"));
            SIM.angleCounter = t.getInt("angleCounter"); SIM.angleLength = Math.max(1, t.getInt("angleLength"));
            return new WindSavedData();
        }

        @Override public CompoundTag save(CompoundTag t) {
            t.putDouble("power", SIM.power); t.putDouble("angle", SIM.angle); t.putDouble("lastAngle", SIM.lastAngle);
            t.putDouble("startPower", SIM.startPower); t.putDouble("endPower", SIM.endPower);
            t.putDouble("startAngle", SIM.startAngle); t.putDouble("endAngle", SIM.endAngle);
            t.putBoolean("strong", SIM.strongWind);
            t.putInt("shortCounter", SIM.shortCounter); t.putInt("shortLength", SIM.shortLength);
            t.putInt("longCounter", SIM.longCounter); t.putInt("longLength", SIM.longLength);
            t.putInt("angleCounter", SIM.angleCounter); t.putInt("angleLength", SIM.angleLength);
            return t;
        }
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load e) {
        if (e.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD)
            data = level.getDataStorage().computeIfAbsent(WindSavedData::load, WindSavedData::new, "cursed_wind");
    }

    @SubscribeEvent
    public static void onTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.level.isClientSide || e.level.dimension() != Level.OVERWORLD) return;
        SIM.update();
        if (data != null && e.level.getGameTime() % 600L == 0L) data.setDirty();
    }

    /** Only the overworld has wind (as in the addon). */
    public static double dimensionMultiplier(Level level) { return level.dimension() == Level.OVERWORLD ? 1.0 : 0.0; }
}
