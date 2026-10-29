package com.example.corruption.system;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 每个玩家的腐化值（0-100），持久化在主世界 SavedData。
 * 存储 base（开始时的值）和 since（对应游戏时间），随游戏时间线性增长：
 * 每 CORRUPTION_TICKS_PER_POINT（默认 1200 ticks = 60 秒）+1 点。
 */
public class CorruptionSavedData extends SavedData {
    private static final String DATA_NAME = "corruption_data";
    public static final int TICKS_PER_POINT = 1200;
    public static final int MAX = 100;

    private final Map<UUID, long[]> records = new HashMap<>(); // [base, since]

    public static CorruptionSavedData get(ServerLevel overworld) {
        return overworld.getServer().overworld().getDataStorage()
                .computeIfAbsent(new Factory<>(CorruptionSavedData::new, CorruptionSavedData::load, null), DATA_NAME);
    }

    /** 读取当前腐化值（含随时间的增长），不写入。 */
    public int get(UUID id, long gameTime) {
        long[] r = records.get(id);
        if (r == null) return 0;
        return Mth.clamp((int) (r[0] + (gameTime - r[1]) / TICKS_PER_POINT), 0, MAX);
    }

    /** 写入新的腐化值（外部事件可以加值/清零），以当前时间为基准重新计时。 */
    public void set(UUID id, int value, long gameTime) {
        records.put(id, new long[]{Mth.clamp(value, 0, MAX), gameTime});
        setDirty();
    }

    /** 玩家“洗白”（例如击败实体后）。 */
    public void reset(UUID id, long gameTime) {
        set(id, 0, gameTime);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, long[]> e : records.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", e.getKey());
            t.putLong("base", e.getValue()[0]);
            t.putLong("since", e.getValue()[1]);
            list.add(t);
        }
        tag.put("records", list);
        return tag;
    }

    private static CorruptionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CorruptionSavedData data = new CorruptionSavedData();
        ListTag list = tag.getList("records", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            data.records.put(t.getUUID("id"), new long[]{t.getLong("base"), t.getLong("since")});
        }
        return data;
    }
}
