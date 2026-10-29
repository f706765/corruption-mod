# Corruption — 渐进式心理恐怖模组（NeoForge 1.21.1）

隐藏的 0–100 腐化值驱动一切恐怖事件。没有跳脸，只有"好像有什么不对"。

## 当前实现（首版清单）

| # | 功能 | 状态 |
|---|---|---|
| 1 | 腐化值系统：`SavedData` 持久化每个玩家腐化值，随游戏时间增长（每 60 秒 +1，上限 100） | ✅ |
| 2 | 核心实体「凝视者」：形态 B，两个 Goal —— 注视玩家 + 玩家靠近 6 格内后退 | ✅ |
| 3 | whisper 音效：腐化 ≥50 时每秒 6% 概率在玩家 6–10 格外播放（可变距离衰减） | ✅ |
| 4 | 火把熄灭事件：腐化 ≥25 时每秒 10% 概率熄灭玩家附近随机一支火把（含烟雾粒子） | ✅ |
| 5 | **暗角黑雾（视觉）**：腐化 ≥50 时玩家视野四周（半径 3–5 格）环形生成黑雾粒子，腐化越高越密 | ✅ `v1.0.1` |
| 6 | **心跳声（听觉）**：腐化 ≥75 时循环播放 `corruption:heartbeat`，频率随腐化升高加快 | ✅ `v1.0.1` |
| 7 | **镜头摇晃（视觉）**：腐化 ≥90 时轻微倾斜/摇晃摄像机，模拟理智边缘眩晕 | ✅ `v1.0.1` |
| 8 | **`/corruption` 测试指令**：`get` 查看、`set <0-100>` 快速拉高/清零腐化值 | ✅ `v1.0.1` |
| 9 | **新怪物「长臂」**：全身漆黑、长臂长腿，蹲在树后只露手；盯着那棵树超过 10 秒 → 它察觉并冲过来把你抓进漆黑暗域，手上获得火把，任务：重新点亮 3 个诅咒火炬即可逃脱 | ✅ `v1.0.3` |
| 10 | **腐化迹象（视觉）**：腐化 ≥40 时玩家身体周围浮现灰/黑粒子，越腐化越密 | ✅ `v1.0.4` |
| 11 | **腐化傀儡**：腐化达到 100 时被锁死、无法移动（只能转头张望），唯一下场是死亡 | ✅ `v1.0.4` |
| 12 | **死亡重生 + 腐化尸体**：腐化较高时死亡，原躯体被腐化操控留下一具「腐化尸体」实体并主动攻击（命中致流血）；复活后腐化清零 | ✅ `v1.0.4` |
| 13 | **裹尸与埋葬**：用「布」包裹尸体 3 次（重度腐化须先用「铅块」包住），再用「铲子」埋入土中（地表堆起小坟包），即可镇压腐化 | ✅ `v1.0.4` |
| 14 | **钳子**：右键使用，把身上腐化硬掰下（-20，最低 0），代价是撕裂伤口痛失 3 颗心 | ✅ `v1.0.4` |
| 15 | **流血（有血）效果**：每 5 秒 -半颗心、行走变慢、屏幕暗红血雾 + 可视距离缩短到 ~4 格；用「绷带」包扎或「火药 + 打火石」自焚简易包扎解除 | ✅ `v1.0.4` |
| 16 | **铅块**：新方块（4 铁锭合成），用于包裹重度腐化尸体 | ✅ `v1.0.4` |
| 17 | **怪物刷怪蛋**：凝视者、长臂刷怪蛋均进入创造栏「刷怪蛋」页签，可直接生成 | ✅ `v1.0.3` |

## 构建方法（一键，无需手动装 Gradle）

工程已带 **Gradle Wrapper**（Gradle 发行版已指向腾讯镜像加速）：

```bash
cd corruption-mod
./gradlew build        # Linux / macOS
gradlew.bat build      # Windows
```

产物在 `build/libs/corruption-1.0.4.jar`，放入 `.minecraft/mods`（NeoForge 1.21.1）即可。
首次构建会自动下载 JDK 21（若有）、NeoForge 依赖与 Minecraft，需联网（约几分钟）。

> 说明：在**联网正常的本机**上 `./gradlew build` 一条命令即可；
> 若你那边也连不上 `maven.neoforged.net`，可在 `~/.gradle/init.gradle` 加一段把该仓库重定向到
> `https://ipv4.mirrors.ustc.edu.cn/bmclapi/`（注意：该镜像未同步 fml 等子依赖，仅在主仓库可达时可省略）。

### 方式二：GitHub Actions 自动出包（无需本机装环境）

工程已内置工作流 `.github/workflows/build.yml`。在**你自己**的 GitHub 账号下：

```bash
# 1) 在 GitHub 网页或用 gh 建一个仓库（如 corruption-mod）
# 2) 本地初始化并推送本工程
cd corruption-mod
git init && git add -A && git commit -m "init"
git branch -M main
git remote add origin https://github.com/<你的用户名>/corruption-mod.git
git push -u origin main
```

推送后 GitHub 自动触发 Actions：拉代码 → 装 JDK 21 → `./gradlew build` → 把 `build/libs/*.jar`
作为 **Artifact** 上传。进入仓库 **Actions → 对应运行 → Artifacts** 即可下载 `corruption-mod-jar`。
（也可 `gh run download -n corruption-mod-jar` 拉到本地。）

IDE 开发：用 IntelliJ IDEA 直接打开工程（`ModDevGradle` 插件会自动配置），运行 `runClient` 调试。

## 项目结构

```
src/main/java/com/example/corruption/
├── CorruptionMod.java            # 主类：注册 + 事件总线
├── registry/
│   ├── ModEntities.java          # 实体注册（watcher / long_arms）
│   ├── ModAttributes.java        # 属性：速度 0.19（慢）、感知 64（远）
│   ├── ModBlocks.java            # 诅咒火炬方块
│   ├── ModSounds.java            # whisper + heartbeat（createVariableRangeEvent）
│   └── ModItems.java             # 刷怪蛋 + 诅咒火炬物品
├── system/
│   ├── CorruptionSavedData.java  # 腐化值持久化（主世界 SavedData）
│   ├── LongArmsCapturedData.java # 被抓玩家存档（返回坐标/维度 + 暗域房间原点）
│   └── LongArmsRoom.java         # 暗域房间：建造 / 抓捕 / 逃脱
├── entity/
│   ├── WatcherEntity.java        # 凝视者
│   ├── LongArmsEntity.java       # 长臂：树后潜伏 + 注视检测 + 抓捕
│   ├── StareAtPlayerGoal.java    # Goal 1：注视玩家
│   └── BackAwayGoal.java         # Goal 2：玩家 <6 格时后退
├── block/
│   └── CursedTorchBlock.java     # 诅咒火炬（未点亮/点亮两态，点亮即发光）
├── event/
│   ├── CorruptionEvents.java     # 每秒调度：火把熄灭（25+）、低语（50+）、腐化值同步、/corruption 与 /longarms 指令
│   └── LongArmsEvents.java       # 自然生成调度（森林/针叶林夜晚）
├── network/
│   └── CorruptionSyncPacket.java # 服务端→客户端：同步本地玩家腐化值
└── client/
    ├── WatcherModel.java         # 简单人形模型（setupAnim 全静止 = 不自然）
    ├── WatcherRenderer.java      # cutout 渲染：贴图透明处完全透出
    ├── WatcherModelLayer.java
    ├── LongArmsModel.java        # 长臂瘦高模型（长臂长腿）
    ├── LongArmsRenderer.java
    ├── LongArmsModelLayer.java
    ├── ClientCorruptionData.java # 客户端持有的本地腐化值（收包更新）
    └── CorruptionClientEffects.java # 暗角粒子/心跳/镜头摇晃（仅客户端）

src/main/resources/assets/corruption/
├── sounds.json                   # whisper + heartbeat 定义
├── sounds/whisper.ogg            # 单声道 OGG（粉噪+低频嗡鸣，7 秒）
├── sounds/heartbeat.ogg          # 单声道 OGG（低频 lub-dub 双拍）
├── textures/entity/watcher.png   # 深色残缺人形贴图
├── textures/entity/longarms.png  # 纯黑长臂贴图
├── textures/block/cursed_torch.png / cursed_torch_lit.png
└── lang/                         # en_us / zh_cn
```

## 测试方法

1. `runClient` 启动，创建生存模式世界
2. 腐化值随时间自动增长：约 25 分钟到 25（火把开始熄灭）、50 分钟到 50（低语/暗角）、75（心跳）、90（镜头摇晃）
3. **快速验证特效（推荐）**：控制台执行 `/corruption set 50`（暗角）、`/corruption set 75`（+心跳）、`/corruption set 90`（+摇晃）、`/corruption get`（查看当前值）
4. 加速自动增长：临时把 `CorruptionSavedData.TICKS_PER_POINT` 改小（如 20）
5. 刷怪蛋生成凝视者，或 `/summon corruption:watcher`

## 后续路线（代码已留扩展点）

- **25–49 不安期**：身后短暂生成不可交互实体、假乱码消息
- **50–74 心理期**：假加入/离开消息、门自开关、脚步延迟（暗角已实装）
- **75–94 敌意期**：实体绕后 AI、breath / silence 音效（心跳已实装）
- **95–100 终局**：攻击模式（瞬移、贴地爬行），`WatcherEntity.aiStep()` 已留位
- 形态 A/C：按腐化值切换贴图与 AI（`WatcherEntity.corruptionOf()` 已可用）

## 已知限制

- 腐化值目前只随时间增长，尚无"黑暗中加速 / 白天减缓"等修正
- 暗角粒子使用 `SQUID_INK`（黑色），若想更"雾"可换成 `SMOKE` 或自定义粒子
