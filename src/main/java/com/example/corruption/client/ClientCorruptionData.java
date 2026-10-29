package com.example.corruption.client;

/**
 * 客户端持有的本地玩家腐化值，由 {@link com.example.corruption.network.CorruptionSyncPacket} 更新。
 * 所有客户端视觉/听觉特效都从这里读值，避免在服务端执行客户端逻辑。
 */
public final class ClientCorruptionData {
    private static int corruption = 0;

    public static int getCorruption() {
        return corruption;
    }

    public static void setCorruption(int value) {
        corruption = value;
    }

    private ClientCorruptionData() {}
}
