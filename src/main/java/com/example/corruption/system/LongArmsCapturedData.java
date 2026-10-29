package com.example.corruption.system;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家被长臂抓入暗域后的存档数据（主世界持久化）：
 * 记录返回坐标/维度，以及暗域房间原点（用于校验三个火炬是否点亮）。
 */
public class LongArmsCapturedData extends SavedData {
    private static final String NAME = "longarms_capture";
    private final Map<UUID, Entry> map = new HashMap<>();

    public static LongArmsCapturedData get(ServerLevel overworld) {
        return overworld.getServer().overworld().getDataStorage()
                .computeIfAbsent(new Factory<>(LongArmsCapturedData::new, LongArmsCapturedData::load, null), NAME);
    }

    public void capture(UUID id, double rx, double ry, double rz, String dim, int ox, int oy, int oz) {
        map.put(id, new Entry(true, rx, ry, rz, dim, ox, oy, oz));
        setDirty();
    }

    public Entry get(UUID id) { return map.get(id); }

    public void release(UUID id) { map.remove(id); setDirty(); }

    public static class Entry {
        public boolean captured;
        public double rx, ry, rz;
        public String dim;
        public int ox, oy, oz;

        Entry(boolean c, double rx, double ry, double rz, String dim, int ox, int oy, int oz) {
            this.captured = c; this.rx = rx; this.ry = ry; this.rz = rz;
            this.dim = dim; this.ox = ox; this.oy = oy; this.oz = oz;
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Entry> e : map.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", e.getKey());
            Entry v = e.getValue();
            t.putBoolean("c", v.captured);
            t.putDouble("rx", v.rx); t.putDouble("ry", v.ry); t.putDouble("rz", v.rz);
            t.putString("dim", v.dim);
            t.putInt("ox", v.ox); t.putInt("oy", v.oy); t.putInt("oz", v.oz);
            list.add(t);
        }
        tag.put("entries", list);
        return tag;
    }

    private static LongArmsCapturedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LongArmsCapturedData d = new LongArmsCapturedData();
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            Entry v = new Entry(t.getBoolean("c"), t.getDouble("rx"), t.getDouble("ry"), t.getDouble("rz"),
                    t.getString("dim"), t.getInt("ox"), t.getInt("oy"), t.getInt("oz"));
            d.map.put(t.getUUID("id"), v);
        }
        return d;
    }
}
