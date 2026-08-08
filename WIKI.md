# HaoHanItemManager — Wiki & Hướng dẫn sử dụng

> **HaoHanItemManager** là plugin nền tảng quản lý tập trung Custom Item & Recipe cho Minecraft Paper Server.
> Tất cả plugin gameplay (Magic, Weapon, Machine, Quest...) chỉ cần phụ thuộc HaoHanItemManager thay vì tự xử lý item/recipe.

---

## Mục lục

1. [Cài đặt](#1-cài-đặt)
2. [Cấu trúc thư mục](#2-cấu-trúc-thư-mục)
3. [Tạo Custom Item (YAML)](#3-tạo-custom-item-yaml)
4. [Tạo Recipe (YAML)](#4-tạo-recipe-yaml)
5. [Commands](#5-commands)
6. [Permissions](#6-permissions)
7. [GUI](#7-gui)
8. [API cho Plugin khác](#8-api-cho-plugin-khác)
9. [Item Behavior](#9-item-behavior)
10. [Built-in Property Keys](#10-built-in-property-keys)
11. [Custom Block System (NoteBlock)](#11-custom-block-system-noteblock)
12. [Custom Armor Model System](#12-custom-armor-model-system)
13. [Tự động Sanitization & Upgrade Item](#13-tự-động-sanitization--upgrade-item)
14. [Ví dụ hoàn chỉnh — Plugin mẫu](#14-ví-dụ-hoàn-chỉnh--plugin-mẫu)
15. [FAQ](#15-faq)

---

## 1. Cài đặt

### Yêu cầu

| Yêu cầu | Phiên bản |
|---|---|
| Minecraft Server | Paper 1.21.11+ |
| Java | 21+ |

### Cài đặt

1. Build plugin: `mvn clean package`
2. Copy file `target/HaoHanItemManager-1.0.0.jar` vào thư mục `plugins/` của server
3. Khởi động server
4. HaoHanItemManager sẽ tự tạo thư mục `plugins/HaoHanItemManager/` với các file config mẫu

### Cấu trúc sau khi cài đặt

```
plugins/
└── HaoHanItemManager/
    ├── config.yml
    ├── items/
    │   └── example.yml
    └── recipes/
        └── example.yml
```

---

## 2. Cấu trúc thư mục

```
plugins/HaoHanItemManager/
│
├── config.yml              ← Cấu hình chung
│
├── items/                  ← Thư mục chứa item definitions
│   ├── magic.yml           ← Items cho namespace "magic"
│   ├── weapon.yml          ← Items cho namespace "weapon"
│   └── ...
│
└── recipes/                ← Thư mục chứa recipes (hỗ trợ thư mục con)
    ├── magic/
    │   └── mana_crystal.yml
    └── ...
```

> 💡 **Tips**: Mỗi file trong `items/` tương ứng với một namespace. Recipes có thể đặt trong thư mục con để dễ quản lý.

---

## 3. Tạo Custom Item (YAML)

### Tất cả các field

| Field | Bắt buộc | Mô tả | Mặc định |
|---|---|---|---|
| `material` | ✅ | Bukkit Material name | PAPER |
| `display-name` | ❌ | Tên hiển thị, hỗ trợ `§` color codes | ID |
| `lore` | ❌ | Danh sách dòng lore | rỗng |
| `custom-model-data` | ❌ | Custom model data number | null |
| `max-stack-size` | ❌ | Số lượng tối đa trong 1 stack | 64 |
| `type` | ❌ | Loại item | MATERIAL |
| `properties` | ❌ | Map key-value tùy ý (hỗ trợ built-in keys, xem mục 10) | rỗng |

### Item Types

`MATERIAL` · `TOOL` · `WEAPON` · `ARMOR` · `FOOD` · `MACHINE_COMPONENT` · `CURRENCY` · `SPECIAL`

### Ví dụ

```yaml
magic:
  fire_crystal:
    material: EMERALD
    display-name: "§cFire Crystal"
    lore:
      - "§7A crystal containing unstable fire energy."
    custom-model-data: 1001
    max-stack-size: 16
    type: MATERIAL
    properties:
      element: fire
      tier: 1
```

> ⚠️ Sau khi chỉnh sửa YAML, dùng `/im reload` để reload mà không cần restart.

---

## 4. Tạo Recipe (YAML)

### Recipe Types hỗ trợ

| Type | Hoạt động tại |
|---|---|
| `SHAPED` | Crafting Table (pattern cố định) |
| `SHAPELESS` | Crafting Table (không quan tâm vị trí) |
| `SMELTING` / `BLASTING` / `SMOKING` / `CAMPFIRE` | Lò tương ứng |
| `STONECUTTING` | Stonecutter |
| `SMITHING` | Smithing Table |
| `MACHINE` | Custom (plugin tự xử lý) |

### Shaped Recipe mẫu

```yaml
id: magic:mana_crystal
type: SHAPED
pattern:
  - " S "
  - "SBS"
  - " S "
ingredients:
  S:
    item: "magic:mana_shard"
  B:
    item: "minecraft:blaze_rod"
result:
  item: "magic:mana_crystal"
  amount: 1
```

---

## 5. Commands

| Command | Mô tả | Permission |
|---|---|---|
| `/im items` | Liệt kê tất cả custom items | `baseengine.use` |
| `/im item <id>` | Xem chi tiết item | `baseengine.use` |
| `/im give <player> <id> [amount]` | Cho player item | `baseengine.admin` |
| `/im recipes` | Liệt kê recipes | `baseengine.use` |
| `/im recipe <id>` | Xem recipe GUI | `baseengine.use` |
| `/im search <keyword>` | Tìm kiếm | `baseengine.use` |
| `/im browse` | Mở Item Browser GUI | `baseengine.use` |
| `/im reload` | Reload config | `baseengine.admin` |

**Aliases:** `/im` = `/itemmanager` = `/haohanitemmanage`

---

## 6. Permissions

| Permission | Mô tả | Mặc định |
|---|---|---|
| `baseengine.use` | Commands cơ bản | `true` |
| `baseengine.admin` | Commands admin (give, reload) | `op` |

---

## 7. GUI

- **Item Browser** (`/im browse`): Hiển thị tất cả custom items (45/trang), click → Recipe Viewer.
- **Recipe Viewer** (`/im recipe <id>`): Crafting grid, navigation, click ingredient để xem recipe của ingredient đó.

---

## 8. API cho Plugin khác

### Dependency

```yaml
# paper-plugin.yml
dependencies:
  server:
    HaoHanItemManager:
      load: BEFORE
      required: true
```

### Maven

```xml
<dependency>
    <groupId>vn.haohan</groupId>
    <artifactId>HaoHanItemManager</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

### Quick API Reference

```java
HaoHanItemManager api = HaoHanItemManager.get();

// Tạo item
ItemStack item = api.getItemService().create("magic:fire_crystal");
ItemStack items = api.getItemService().create("magic:fire_crystal", 4);

// Kiểm tra item
boolean isCustom = api.getItemService().isCustomItem(stack);
boolean isCrystal = api.getItemService().isItem(stack, "magic:fire_crystal");
String id = api.getItemService().getId(stack);
Map<String, Object> props = api.getItemService().getProperties(stack);

// Upgrade/đồng bộ thủ công
api.getItemService().validateAndUpdate(stack);

// Đăng ký item
api.getItemRegistry().register(definition);

// Đăng ký recipe
api.getRecipeRegistry().register(recipe);
```

---

## 9. Item Behavior

```java
public class FireCrystalBehavior implements ItemBehavior {

    @Override
    public void onUse(ItemContext ctx) {
        ctx.player().sendMessage("§cFire energy released!");
    }
}

// Đăng ký
ItemDefinition.builder("magic:fire_crystal")
    .material(Material.EMERALD)
    .behavior(new FireCrystalBehavior())
    .build();
```

### Các method được hỗ trợ

| Method | Trigger |
|---|---|
| `onUse()` | Right-click |
| `onInteract()` | Left-click |
| `onBreak()` | Phá block |
| `onCraft()` | Craft thành công |
| `onInventoryClick()` | Click trong inventory |
| `onDrop()` | Drop item |
| `onPickup()` | Nhặt item |

---

## 10. Built-in Property Keys

Một số **property key** được `DefaultItemFactory` xử lý tự động khi tạo hoặc upgrade `ItemStack`:

| Property Key | Kiểu | Mô tả |
|---|---|---|
| `max_damage` | `int` | Đặt MaxDamage (độ bền) cho item Damageable. |
| `jukebox_playable` | `String` (NamespacedKey) | Gắn component `jukebox_playable` — dùng cho đĩa nhạc custom. Ví dụ: `"haohan:my_song"`. |
| `equippable_asset_id` | `String` (NamespacedKey) | Gắn `minecraft:equippable` với custom armor model asset. Slot tự xác định theo Material suffix. Ví dụ: `"haohan:spacesuit"`. |
| `custom_block_data` | `String` (blockstate) | Gắn `minecraft:block_state` để client dự đoán block state khi đặt. Dùng cho custom block qua NoteBlock. |

### Ví dụ

```java
// Giáp có model 3D custom
ItemDefinition.builder("haohan:spacesuit_helmet")
    .material(Material.NETHERITE_HELMET)
    .property("equippable_asset_id", "haohan:spacesuit")
    .build();

// Custom block qua NoteBlock
ItemDefinition.builder("haohan:anorthosite_ore")
    .material(Material.NOTE_BLOCK)
    .property("custom_block_data", "minecraft:note_block[note=24,instrument=pling,powered=true]")
    .build();

// Đĩa nhạc custom
ItemDefinition.builder("haohan:my_disc")
    .material(Material.MUSIC_DISC_13)
    .property("jukebox_playable", "haohan:my_song")
    .build();

// Item có độ bền custom
ItemDefinition.builder("haohan:oxygen_tank")
    .material(Material.CARROT_ON_A_STICK)
    .maxStackSize(1)
    .property("max_damage", 1500)
    .build();
```

---

## 11. Custom Block System (NoteBlock)

Tạo **Custom Block** bằng cách chiếm dụng block state của `minecraft:note_block`.

### Tính năng

- Texture tùy chỉnh qua resource pack.
- **Không flicker** khi đặt xuống (client dự đoán state ngay lập tức).
- Block state không bị thay đổi bởi tương tác vật lý.
- Vẫn cho phép đặt block xung quanh bình thường.

### Cách hoạt động

Engine lắng nghe:
- **`BlockPlaceEvent` (HIGH)**: Cưỡng bức block state đúng + gửi `sendBlockChange()` cho player.
- **`PlayerInteractEvent`** (NoteBlock): Nếu player gảy note → schedule 0-tick reset về custom state.
- **`BlockPhysicsEvent`** (NoteBlock): Nếu physics thay đổi state → restore cuối tick.

### Đăng ký

```java
ItemDefinition.builder("haohan:anorthosite_ore")
    .material(Material.NOTE_BLOCK)
    .displayName("§7Anorthosite Ore")
    .type(ItemType.SPECIAL)
    .property("custom_block_data", "minecraft:note_block[note=24,instrument=pling,powered=true]")
    .build();
```

> ⚠️ Tất cả `NotePlayEvent` bị cancel — toàn bộ note_block đều tắt âm thanh. Đây là trade-off khi dùng note_block làm custom block.

---

## 12. Custom Armor Model System

Từ Minecraft 1.21+, giáp custom khi mặc dùng component **`minecraft:equippable`** thay vì Custom Model Data.

### Cấu trúc Resource Pack

```
assets/<namespace>/
├── equipment/
│   └── <asset_id>.json         ← Layer definition
└── textures/entity/equipment/
    ├── humanoid/
    │   └── <asset_id>.png      ← Texture thân/đầu/tay/giày
    └── humanoid_leggings/
        └── <asset_id>.png      ← Texture quần
```

**`equipment/spacesuit.json`:**
```json
{
  "layers": {
    "humanoid": [{ "texture": "haohan:spacesuit" }],
    "humanoid_leggings": [{ "texture": "haohan:spacesuit" }]
  }
}
```

### Đăng ký

```java
registry.register(ItemDefinition.builder("haohan:spacesuit_helmet")
    .material(Material.NETHERITE_HELMET)
    .customModelData(1001)
    .type(ItemType.ARMOR)
    .property("equippable_asset_id", "haohan:spacesuit")
    .build());
```

Engine tự xác định slot từ Material suffix:
`_HELMET` → HEAD · `_CHESTPLATE` → CHEST · `_LEGGINGS` → LEGS · `_BOOTS` → FEET

---

## 13. Tự động Sanitization & Upgrade Item

Engine tự động **validate và upgrade** components cho custom items tại:

| Event | Mô tả |
|---|---|
| `PlayerJoinEvent` | Toàn bộ inventory + armor khi login |
| `InventoryOpenEvent` | Khi player mở inventory/chest/shulker |
| `InventoryClickEvent` | Item được click và cursor item |
| `EntityPickupItemEvent` | Item nhặt từ đất |
| `PrepareItemCraftEvent` | Kết quả craft |

### Upgrade thủ công

```java
HaoHanItemManager.get().getItemService().validateAndUpdate(itemStack);
```

### Những gì được đồng bộ

- **Item Model** → theo ID của item
- **Max Stack Size** → theo definition
- **Max Damage** → nếu có property `max_damage`
- **Jukebox Playable** → nếu có property `jukebox_playable`
- **Equippable Component** → nếu có property `equippable_asset_id` và chưa được gắn

> Item vanilla (không có PersistentData của engine) được bỏ qua hoàn toàn.

---

## 14. Ví dụ hoàn chỉnh — Plugin mẫu

### paper-plugin.yml

```yaml
name: MagicPlugin
version: '1.0.0'
main: com.example.magic.MagicPlugin
api-version: '1.21'
dependencies:
  server:
    HaoHanItemManager:
      load: BEFORE
      required: true
```

### MagicPlugin.java

```java
public class MagicPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        ModItems.register();
        ModRecipes.register();
    }
}
```

### ModItems.java

```java
public class ModItems {
    public static void register() {
        ItemRegistry registry = HaoHanItemManager.get().getItemRegistry();

        registry.register(ItemDefinition.builder("magic:fire_crystal")
            .material(Material.EMERALD)
            .displayName("§cFire Crystal")
            .lore(List.of("§7A crystal containing unstable fire energy."))
            .customModelData(1001)
            .maxStackSize(16)
            .type(ItemType.MATERIAL)
            .property("element", "fire")
            .behavior(new FireCrystalBehavior())
            .build());
    }
}
```

---

## 15. FAQ

**Q: Reload không restart?** → `/im reload`

**Q: ID trùng?** → Dùng namespace khác nhau. Engine throw `IllegalArgumentException` khi đăng ký ID trùng.

**Q: Item custom vẫn nhận diện được trong hopper/chest?** → Có, dựa vào PersistentDataContainer. Khi inventory mở, engine tự động upgrade nếu cần.

**Q: Custom armor model không hiện khi mặc?**
1. Kiểm tra property `equippable_asset_id` đã khai báo.
2. Kiểm tra file `equipment/<asset_id>.json` và texture tồn tại trong resource pack.
3. Với item có từ trước update: mở inventory và click item một lần để trigger upgrade.

**Q: Custom block flicker texture?** → Đảm bảo property `custom_block_data` khai báo đúng format blockstate. Engine tự gắn `minecraft:block_state` để client dự đoán đúng ngay lập tức.

**Q: MACHINE recipe hoạt động thế nào?** → Engine chỉ lưu definition, không đăng ký Bukkit. Plugin con tự xử lý việc kiểm tra và thực thi recipe.
