<div align="center">

# HaoHanItemManager

Plugin engine quản lý tập trung Custom Item và Recipe cho Minecraft Paper Server, cung cấp API infrastructure cho các plugin khác.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62B47A?style=for-the-badge&logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Paper](https://img.shields.io/badge/Paper-API-222222?style=for-the-badge&logo=paper&logoColor=white)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)

Ngôn ngữ: Tiếng Việt | [Wiki Hướng Dẫn Chi Tiết](WIKI.md)

</div>

## Tổng quan

`HaoHanItemManager` là plugin Minecraft dành cho HaoHan SMP. Plugin cung cấp hệ thống quản lý custom item và recipe tập trung, xử lý nhận diện vật phẩm qua `PersistentDataContainer`, tự động đăng ký công thức rèn/chế tạo và cung cấp giao diện GUI xem công thức và danh sách vật phẩm.

### Mục tiêu chính

- Quản lý custom item và recipe tập trung, tránh xung đột ID giữa các plugin.
- Nhận diện item chính xác qua `PersistentDataContainer` (namespaced ID) thay vì display name.
- Đăng ký công thức tự động với Bukkit server (hỗ trợ Shaped, Shapeless, Smelting, Blasting, Smoking, Campfire, Stonecutting, Machine).
- Cung cấp GUI duyệt item (Item Browser) và xem công thức (Recipe Viewer) tương tác trực quan.
- Cung cấp Java API đơn giản, mạnh mẽ và linh hoạt cho các plugin khác tích hợp (`vn.haohan.itemmanager`).

## Công nghệ sử dụng

| Toolkit | Vai trò |
| --- | --- |
| Paper API (1.21.11) | Nền tảng API chính để phát triển Paper plugin (sử dụng format `paper-plugin.yml`). |
| Java 21 | Ngôn ngữ và runtime chính của plugin (tận dụng Records, Pattern Matching, Sealed Types). |
| Maven | Quản lý dependency và build file `.jar`. |
| Adventure API | Xử lý Text Component, formatting và UI messaging hiện đại. |
| Bukkit Recipe API | Đăng ký công thức chế tạo tùy chỉnh trực tiếp vào hệ thống vanilla Minecraft. |

## Thành phần dự án

| Thành phần | Mô tả |
| --- | --- |
| `HaoHanItemManager` | Plugin server, xử lý Item Registry, Factory, Recipe Registry, Event Routing, GUI và Command. |
| `WIKI.md` | Bộ tài liệu hướng dẫn chi tiết từ A-Z về cấu hình YAML, hệ thống Event/Behavior, API và FAQ. |

## Yêu cầu

- Minecraft server chạy Paper hoặc Purpur 1.21.11.
- Java 21 trở lên.
- Maven 3.9 trở lên nếu cần build từ mã nguồn.

## Cài đặt

1. Build hoặc tải file `HaoHanItemManager-1.0.0.jar`.
2. Copy file `.jar` vào thư mục `plugins/` của server.
3. Khởi động server.
4. Plugin sẽ tự động tạo thư mục cấu hình `plugins/HaoHanItemManager/` chứa file `config.yml`, `items/example.yml` và `recipes/example.yml`.

## Build từ mã nguồn

Chạy lệnh sau tại thư mục gốc của dự án:

```bash
mvn clean package
```

File `.jar` sau khi build nằm trong thư mục `target/HaoHanItemManager-1.0.0.jar`.

## Lệnh

Các lệnh quản trị dùng permission `haohanitemmanager.admin`. Người chơi OP có permission này theo mặc định. All player có permission `haohanitemmanager.use` theo mặc định.

| Lệnh | Mô tả | Permission |
| --- | --- | --- |
| `/im items` | Hiển thị danh sách tất cả custom items theo namespace. | `haohanitemmanager.use` |
| `/im item <id>` | Xem thông tin chi tiết của một item theo ID. | `haohanitemmanager.use` |
| `/im give <player> <id> [amount]` | Trao custom item cho người chơi. | `haohanitemmanager.admin` |
| `/im recipes` | Liệt kê tất cả công thức chế tạo đã đăng ký. | `haohanitemmanager.use` |
| `/im recipe <id>` | Mở GUI xem công thức chế tạo của item/recipe. | `haohanitemmanager.use` |
| `/im search <keyword>` | Tìm kiếm item hoặc recipe theo từ khóa. | `haohanitemmanager.use` |
| `/im browse` | Mở GUI duyệt danh sách tất cả custom items (phân trang). | `haohanitemmanager.use` |
| `/im reload` | Nạp lại toàn bộ file cấu hình items và recipes từ đĩa. | `haohanitemmanager.admin` |

Alias của lệnh chính: `/itemmanager`, `/haohanitemmanage`.

## Permission

| Permission | Mặc định | Mô tả |
| --- | --- | --- |
| `haohanitemmanager.admin` | OP | Cho phép sử dụng các lệnh quản trị (`give`, `reload`). |
| `haohanitemmanager.use` | Tất cả người chơi | Cho phép xem danh sách, tra cứu, xem GUI recipe và browse item. |

## Cấu trúc dữ liệu & Cấu hình

Các file cấu hình YAML được quản lý trong thư mục data của plugin:

```text
plugins/HaoHanItemManager/
├── config.yml
├── items/
│   └── example.yml
└── recipes/
    └── example.yml
```

### 1. Item Definition (`items/*.yml`)

Mỗi item có ID theo dạng `namespace:item_key`:

```yaml
example:
  fire_crystal:
    material: EMERALD
    display-name: "§cFire Crystal"
    lore:
      - "§7A crystal containing"
      - "§7unstable fire energy."
    custom-model-data: 1001
    max-stack-size: 16
    type: MATERIAL
    properties:
      element: fire
```

### 2. Recipe Definition (`recipes/*.yml`)

Hỗ trợ các loại công thức: `SHAPED`, `SHAPELESS`, `SMELTING`, `BLASTING`, `SMOKING`, `CAMPFIRE`, `STONECUTTING`, `SMITHING`, `MACHINE`.

```yaml
id: example:mana_crystal
type: SHAPED
pattern:
  - " S "
  - "SBS"
  - " S "
ingredients:
  S:
    item: "example:mana_shard"
  B:
    item: "minecraft:blaze_rod"
result:
  item: "example:mana_crystal"
  amount: 1
```

## Tích hợp API (Cho Developer)

Thêm `HaoHanItemManager` vào dependency của plugin:

```yaml
# paper-plugin.yml
dependencies:
  server:
    HaoHanItemManager:
      load: BEFORE
      required: true
```

### Lấy API Instance & Sử dụng:

```java
import vn.haohan.itemmanager.api.HaoHanItemManager;
import vn.haohan.itemmanager.api.item.ItemDefinition;

// Tạo ItemStack từ ID
ItemStack crystal = HaoHanItemManager.get().getItemService().create("example:fire_crystal", 4);

// Kiểm tra ItemStack có phải là custom item cụ thể
boolean isCrystal = HaoHanItemManager.get().getItemService().isItem(item, "example:fire_crystal");

// Đăng ký Item mới qua Code
ItemDefinition customItem = ItemDefinition.builder("magic:wand")
    .material(Material.STICK)
    .displayName("§aMagic Wand")
    .behavior(new WandBehavior())
    .build();

HaoHanItemManager.get().getItemRegistry().register(customItem);
```

Chi tiết đầy đủ về API và hướng dẫn phát triển plugin phụ thuộc có tại **[WIKI.md](WIKI.md)**.