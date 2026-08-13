# Tài Liệu API Chi Tiết: HaoHanItemCore

Tài liệu này cung cấp chi tiết toàn diện về các lớp (classes), giao diện (interfaces), các thành phần mở rộng (components), và các phương thức (methods) trong API của **HaoHanItemCore**.

**HaoHanItemCore** là hệ thống cốt lõi quản lý Custom Items và Custom Recipes cho **HaoHan SMP**, giúp các plugin vệ tinh (như Lunar, Magic, Tech, Quest, Armor, Tools, v.v.) dễ dàng tích hợp, tra cứu và mở rộng tính năng.

---

## 📋 Mục Lục API

1. [Điểm Khởi Đầu (API Entrypoint) — `HaoHanItemCore`](#1-điểm-khởi-đầu-api-entrypoint--haohanitemcore)
2. [Hệ Thống Định Nghĩa Custom Item](#2-hệ-thống-định-nghĩa-custom-item)
   - [2.1 Enum `ItemType`](#21-enum-itemtype)
   - [2.2 Class `ItemDefinition` & Builder](#22-class-itemdefinition--builder)
   - [2.3 Các Thành Phần Lập Trình (`ItemComponent`)](#23-các-thành-phần-lập-trình-itemcomponent)
     - [`AbilityComponent`](#abilitycomponent)
     - [`AttributeComponent`](#attributecomponent)
     - [`DurabilityComponent`](#durabilitycomponent)
     - [`MiningComponent`](#miningcomponent)
     - [`TierComponent`](#tiercomponent)
   - [2.4 Cấu Trúc Lore Nâng Cao (`ItemInfoSection`)](#24-cấu-trúc-lore-nâng-cao-iteminfosection)
   - [2.5 Dữ Liệu Từng Vật Phẩm (`ItemInstanceData`)](#25-dữ-liệu-từng-vật-phẩm-iteminstancedata)
3. [Registry & Services Quản Lý Item](#3-registry--services-quản-lý-item)
   - [3.1 `ItemRegistry`](#31-itemregistry)
   - [3.2 `ItemFactory`](#32-itemfactory)
   - [3.3 `ItemService` (Facade Chính)](#33-itemservice-facade-chính)
4. [Hệ Thống Hành Vi Custom (`ItemBehavior` & `ItemContext`)](#4-hệ-thống-hành-vi-custom-itembehavior--itemcontext)
5. [Hệ Thống Recipe (Công Thức Chế Tạo)](#5-hệ-thống-recipe-công-thức-chế-tạo)
   - [5.1 `RecipeType`, `Ingredient`, `ItemResult`](#51-recipetype-ingredient-itemresult)
   - [5.2 `RecipeDefinition` & `ShapedRecipeDefinition`](#52-recipedefinition--shapedrecipedefinition)
   - [5.3 `RecipeRegistry`](#53-reciperegistry)
   - [5.4 `RecipeService`](#54-recipeservice)
6. [Ví Dụ Tích Hợp Đầy Đủ (Full Integration Examples)](#6-ví-dụ-tích-hợp-đầy-đủ-full-integration-examples)

---

## 1. Điểm Khởi Đầu (API Entrypoint) — `HaoHanItemCore`

Class Singleton chính cung cấp quyền truy cập vào tất cả các quản lý con (Registry, Factory, Service) của plugin.

### Lấy Singleton Instance
```java
import vn.haohan.itemcore.api.HaoHanItemCore;

HaoHanItemCore core = HaoHanItemCore.get();
```

### Bảng Phương Thức trong `HaoHanItemCore`

| Tên phương thức | Kiểu dữ liệu trả về | Tham số | Mô tả / Tác dụng |
| :--- | :--- | :--- | :--- |
| `get()` | `static HaoHanItemCore` | *Không có* | Lấy instance duy nhất của `HaoHanItemCore`. Ném `IllegalStateException` nếu plugin chưa khởi tạo. |
| `getItemRegistry()` | `ItemRegistry` | *Không có* | Truy cập registry lưu trữ danh sách định nghĩa item (`ItemDefinition`). |
| `getItemFactory()` | `ItemFactory` | *Không có* | Truy cập factory khởi tạo `ItemStack` từ định nghĩa. |
| `getItemService()` | `ItemService` | *Không có* | Truy cập service tổng hợp (Facade khuyến nghị dùng cho plugin ngoài). |
| `getRecipeRegistry()` | `RecipeRegistry` | *Không có* | Truy cập registry quản lý công thức chế tạo (`RecipeDefinition`). |
| `getRecipeService()` | `RecipeService` | *Không có* | Truy cập service tra cứu và tìm kiếm công thức chế tạo. |

---

## 2. Hệ Thống Định Nghĩa Custom Item

### 2.1 Enum `ItemType`
Phân loại chức năng chính của vập phẩm custom:
* `MATERIAL`: Nguyên liệu chế tạo cơ bản.
* `TOOL`: Công cụ khai thác (cúp, rìu, xẻng,...).
* `WEAPON`: Vũ khí tấn công (kiếm, nỏ, gậy phép,...).
* `ARMOR`: Trang phục, giáp bảo vệ.
* `FOOD`: Thức ăn, dược phẩm.
* `MACHINE_COMPONENT`: Linh kiện máy móc, thiết bị kỹ thuật.
* `CURRENCY`: Tiền tệ, tiền tệ sự kiện.
* `SPECIAL`: Vật phẩm đặc biệt/kích hoạt sự kiện.

---

### 2.2 Class `ItemDefinition` & Builder

`ItemDefinition` là **Source of Truth** chứa toàn bộ thuộc tính, thông số, lore, model và component của một Custom Item.

#### Bảng Phương Thức Getter của `ItemDefinition`

| Tên phương thức | Kiểu dữ liệu trả về | Tham số | Tác dụng |
| :--- | :--- | :--- | :--- |
| `getId()` | `String` | *Không có* | Lấy ID đầy đủ dạng `namespace:key` (VD: `"lunar:anorthosite_pickaxe"`). |
| `getNamespace()` | `String` | *Không có* | Lấy phần namespace trước dấu `:` (VD: `"lunar"`). |
| `getKey()` | `String` | *Không có* | Lấy phần key sau dấu `:` (VD: `"anorthosite_pickaxe"`). |
| `getMaterial()` | `Material` | *Không có* | Material Minecraft gốc của vật phẩm (VD: `Material.DIAMOND_PICKAXE`). |
| `getDisplayName()` | `String` | *Không có* | Tên hiển thị (hỗ trợ mã màu `§` hoặc `&`). |
| `getMaxStackSize()` | `int` | *Không có* | Số lượng xếp chồng tối đa (1 đến 99). |
| `getLore()` | `List<String>` | *Không có* | Danh sách các dòng mô tả cơ bản của vập phẩm. |
| `getCustomModelData()`| `Integer` | *Không có* | Giá trị CustomModelData (trả về `null` nếu không dùng). |
| `getItemModel()` | `String` | *Không có* | Mô hình 1.21.4+ (`NamespacedKey` dạng String, có thể `null`). |
| `getProperties()` | `Map<String, Object>` | *Không có* | Map các thuộc tính tùy biến đi kèm định nghĩa. |
| `getType()` | `ItemType` | *Không có* | Thể loại phân loại vập phẩm. |
| `getBehavior()` | `ItemBehavior` | *Không có* | Đối tượng xử lý hành vi custom (trả về `null` nếu không có). |
| `getComponents()` | `List<ItemComponent>` | *Không có* | Danh sách các component chức năng tùy biến. |
| `getInfoSections()` | `List<ItemInfoSection>` | *Không có* | Danh sách các mục lore định hình sẵn theo nhóm. |
| `hasBehavior()` | `boolean` | *Không có* | Kiểm tra vật phẩm có gắn đối tượng hành vi custom không. |
| `isUsable()` | `boolean` | *Không có* | Kiểm tra xem vật phẩm có sử dụng được (chuột phải) hay không. |
| `isValidId(id)` | `static boolean` | `String id` | Kiểm tra định dạng String ID có đúng dạng `namespace:key`. |

#### Xây Dựng `ItemDefinition` Bằng Builder Pattern

```java
import org.bukkit.Material;
import vn.haohan.itemcore.api.item.*;

ItemDefinition customPickaxe = ItemDefinition.builder("lunar:anorthosite_pickaxe")
    .material(Material.DIAMOND_PICKAXE)
    .displayName("§bCúp Anorthosite Supercharged")
    .maxStackSize(1)
    .type(ItemType.TOOL)
    .addLore("§7Dùng để khai thác các loại quặng Mặt Trăng.")
    .property("max_damage", 1561)
    .property("custom_block_drop", "lunar:raw_anorthosite")
    .component(new MiningComponent(4, 1.5f))
    .component(new TierComponent(3, "Mặt Trăng"))
    .infoSection("§eKỹ năng kích hoạt", List.of("§7Chuột phải: Khai thác diện rộng 3x3"))
    .build();
```

#### Bảng Thuộc Tính Đăng Ký (Builder Methods) của `ItemDefinition`

Dưới đây là chi tiết các thuộc tính bạn có thể đăng ký khi xây dựng `ItemDefinition` thông qua Builder:

| Tên phương thức | Kiểu đối số | Giá trị mặc định | Mô tả |
| :--- | :--- | :--- | :--- |
| `ItemDefinition.builder(id)` | `String` | *Bắt buộc* | Tạo Builder mới với ID định danh duy nhất của Custom Item dạng `namespace:key` (VD: `"lunar:anorthosite_pickaxe"`). |
| `.material(material)` | `Material` | `Material.PAPER` | Chất liệu Minecraft gốc làm nền tảng cho vật phẩm (VD: `Material.DIAMOND_PICKAXE`). |
| `.displayName(displayName)` | `String` | Trùng với `id` | Tên hiển thị của vật phẩm (hỗ trợ mã màu `§` hoặc `&`). |
| `.maxStackSize(maxStackSize)` | `int` | `64` | Số lượng xếp chồng tối đa của vật phẩm trong một ô đồ (chỉ cho phép từ `1` đến `99`). |
| `.lore(lore)` | `List<String>` | *List rỗng* | Thiết lập toàn bộ danh sách các dòng mô tả (lore) của vật phẩm. |
| `.addLore(line)` | `String` | *Không có* | Thêm một dòng mô tả mới vào lore hiện tại. |
| `.customModelData(customModelData)` | `Integer` | `null` | Giá trị CustomModelData dùng cho tài nguyên texture tùy chỉnh của Resource Pack. |
| `.model(model)` | `String` | `null` | Mô hình 1.21.4+ (`NamespacedKey` dạng String). |
| `.property(key, value)` | `String, Object` | *Không có* | Thêm một thuộc tính tùy biến (Metadata) đi kèm định nghĩa. |
| `.properties(properties)` | `Map<String, Object>`| *Map rỗng* | Thiết lập toàn bộ Map các thuộc tính tùy biến đi kèm định nghĩa. |
| `.type(type)` | `ItemType` | `ItemType.MATERIAL` | Phân loại thể loại vật phẩm (`TOOL`, `ARMOR`, `CONSUMABLE`, `CURRENCY`, v.v.). |
| `.behavior(behavior)` | `ItemBehavior` | `null` | Gắn hành vi xử lý custom logic (Click chuột, tương tác, v.v.). |
| `.usable(usable)` | `boolean` | `true` | Thiết lập vật phẩm có thể sử dụng (chuột phải) hay không. Đặt `false` cho `KNOWLEDGE_BOOK` để tránh bị mất sách khi click chuột phải. |
| `.component(component)` | `ItemComponent` | *Không có* | Thêm một component chức năng tùy biến trực tiếp lên item. |
| `.components(components)` | `List<ItemComponent>`| *List rỗng* | Thiết lập danh sách component chức năng tùy biến. |
| `.infoSection(title, lines)` | `String, List<String>` | *Không có* | Thêm một phần thông tin lore định hình sẵn theo nhóm. |
| `.infoSection(section)` | `ItemInfoSection` | *Không có* | Thêm một đối tượng phần thông tin lore định hình sẵn. |
| `.infoSections(sections)` | `List<ItemInfoSection>`| *List rỗng* | Thiết lập danh sách các phần thông tin lore định hình sẵn. |

#### Các Property Keys Tự Động Xử Lý (Built-in Properties)

| Property Key | Kiểu dữ liệu | Tác dụng |
| :--- | :--- | :--- |
| `max_damage` | `int` | Thiết lập độ bền tối đa của item (chỉ áp dụng cho `Damageable`). |
| `jukebox_playable` | `String` (Key) | Đăng ký đĩa nhạc jukebox custom (VD: `"haohan:space_theme"`). |
| `equippable_asset_id` | `String` (Key) | Đăng ký model giáp hiển thị 3D trên nhân vật (VD: `"haohan:spacesuit"`). |
| `custom_block_data` | `String` (BlockState) | Gắn `minecraft:block_state` cho client xử lý Custom Block (VD: `minecraft:note_block[note=1,instrument=harp]`). |
| `custom_block_drop` | `String` (ID) | ID custom item sẽ rơi ra khi phá custom block này. |

---

### 2.3 Các Thành Phần Lập Trình (`ItemComponent`)

Interface `ItemComponent` cho phép mở rộng các tính năng động được áp dụng trực tiếp lên `ItemStack` khi khởi tạo hoặc kiểm tra.

```java
public interface ItemComponent {
    default void apply(ItemStack item, ItemDefinition definition) {}
    default void appendLore(List<String> lore) {}
}
```

#### `AbilityComponent`
Gắn tên kỹ năng đặc biệt vào lore của vật phẩm.
* **Constructor:** `AbilityComponent(String abilityId, String displayName)`
* **Code Mẫu:**
  ```java
  var ability = new AbilityComponent("laser_beam", "§c§lBắn Tia Laser");
  ```

#### `AttributeComponent`
Thêm thuộc tính chỉ số Paper/Minecraft trực tiếp lên item (sát thương, tốc độ đánh, giáp,...).
* **Constructor:**
  - `AttributeComponent(Attribute attribute, double amount)` *(mặc định: ADD_NUMBER, MAINHAND)*
  - `AttributeComponent(Attribute attribute, double amount, AttributeModifier.Operation operation, EquipmentSlotGroup slotGroup)`
* **Code Mẫu:**
  ```java
  import org.bukkit.attribute.Attribute;
  import org.bukkit.attribute.AttributeModifier;
  import org.bukkit.inventory.EquipmentSlotGroup;

  // Cộng 8.5 Sát thương cho tay chính
  var dmg = new AttributeComponent(Attribute.GENERIC_ATTACK_DAMAGE, 8.5);

  // Tăng 10% Tốc độ di chuyển khi mặc giáp
  var speed = new AttributeComponent(
      Attribute.GENERIC_MOVEMENT_SPEED, 
      0.10, 
      AttributeModifier.Operation.ADD_SCALED_ERROR, 
      EquipmentSlotGroup.ARMOR
  );
  ```

#### `DurabilityComponent`
Khai báo độ bền custom độc lập với độ bền gốc Minecraft. Item sẽ được đặt thành `Unbreakable` và ẩn tag unbreakable vanilla.
* **Constructor:** `DurabilityComponent(int maxDurability)`
* **Code Mẫu:**
  ```java
  var durability = new DurabilityComponent(2500); // 2500 độ bền custom
  ```

#### `MiningComponent`
Định nghĩa thông số cấp độ và tốc độ khai thác cho công cụ.
* **Constructor:** `MiningComponent(int miningLevel, float speedMultiplier)`
* **Code Mẫu:**
  ```java
  var mining = new MiningComponent(4, 2.0f); // Level 4 (Netherite+), tốc độ x2.0
  ```

#### `TierComponent`
Hiển thị cấp độ vập phẩm trên lore.
* **Constructor:** `TierComponent(int tier, String tierName)`
* **Code Mẫu:**
  ```java
  var tier = new TierComponent(5, "Huyền Thoại");
  ```

---

### 2.4 Cấu Trúc Lore Nâng Cao (`ItemInfoSection`)

`ItemInfoSection` (Record) giúp nhóm các dòng lore theo tiêu đề một cách chuẩn hóa và thẩm mỹ.

* **Constructor:** `ItemInfoSection(String title, List<String> lines)`
* **Code Mẫu:**
  ```java
  ItemInfoSection statsSection = new ItemInfoSection(
      "§6§lTHÔNG SỐ VẬT PHẨM",
      List.of("§7- Tốc độ đánh: §af+15%", "§7- Sức sát thương: §c+50")
  );
  ```

---

### 2.5 Dữ Liệu Từng Vật Phẩm (`ItemInstanceData`)

Khác với `ItemDefinition` (là thuộc tính chung định sẵn), `ItemInstanceData` quản lý **trạng thái động riêng biệt của từng vật phẩm cụ thể** lưu giữ trong `PersistentDataContainer` (PDC).

#### Bảng Phương Thức trong `ItemInstanceData`

| Tên phương thức | Kiểu dữ liệu trả về | Tham số | Tác dụng |
| :--- | :--- | :--- | :--- |
| `durability(item, defaultVal)` | `int` | `ItemStack item`, `int defaultValue` | Đọc độ bền custom còn lại của vật phẩm. |
| `setDurability(item, value)` | `void` | `ItemStack item`, `int value` | Ghi độ bền custom mới vào PDC của vật phẩm. |
| `upgradeLevel(item)` | `int` | `ItemStack item` | Đọc cấp độ cường hóa/nâng cấp của vật phẩm (mặc định 0). |
| `setUpgradeLevel(item, level)`| `void` | `ItemStack item`, `int level` | Ghi cấp độ cường hóa/nâng cấp mới vào PDC. |

#### Code Mẫu Thao Tác Trạng Thái Động

```java
ItemInstanceData instanceData = HaoHanItemCore.get().getItemService().getInstanceData();

// Trừ 1 độ bền custom khi dùng
int currentDurability = instanceData.durability(itemStack, 1000);
instanceData.setDurability(itemStack, currentDurability - 1);

// Nâng cấp level item
int currentLevel = instanceData.upgradeLevel(itemStack);
instanceData.setUpgradeLevel(itemStack, currentLevel + 1);
```

---

## 3. Registry & Services Quản Lý Item

### 3.1 `ItemRegistry`

Nơi lưu trữ và tra cứu toàn bộ `ItemDefinition`.

#### Bảng Phương Thức trong `ItemRegistry`

| Tên phương thức | Kiểu dữ liệu trả về | Tham số | Mô tả / Tác dụng |
| :--- | :--- | :--- | :--- |
| `register(definition)` | `void` | `ItemDefinition definition` | Đăng ký item mới vào hệ thống. Lỗi nếu ID trùng. |
| `get(id)` | `ItemDefinition` | `String id` | Tìm ItemDefinition theo ID, trả về `null` nếu không tìm thấy. |
| `require(id)` | `ItemDefinition` | `String id` | Tìm theo ID, ném `IllegalArgumentException` nếu không thấy. |
| `exists(id)` | `boolean` | `String id` | Kiểm tra ID đã đăng ký trong registry chưa. |
| `unregister(id)` | `void` | `String id` | Hủy đăng ký một item khỏi registry. |
| `all()` | `Collection<ItemDefinition>` | *Không có* | Lấy tất cả ItemDefinition đã được đăng ký. |
| `getByNamespace(ns)` | `List<ItemDefinition>` | `String namespace` | Lấy danh sách item thuộc một namespace cụ thể. |
| `search(keyword)` | `List<ItemDefinition>` | `String keyword` | Tìm kiếm item theo từ khóa trong ID hoặc Display Name. |
| `size()` | `int` | *Không có* | Lấy tổng số lượng item đang có trong registry. |
| `clear()` | `void` | *Không có* | Xóa sạch toàn bộ danh sách item đã đăng ký. |

---

### 3.2 `ItemFactory`

Chịu trách nhiệm khởi tạo `ItemStack` chuẩn từ `ItemDefinition` hoặc `id`.

#### Bảng Phương Thức trong `ItemFactory`

| Tên phương thức | Kiểu dữ liệu trả về | Tham số | Mô tả / Tác dụng |
| :--- | :--- | :--- | :--- |
| `create(id)` | `ItemStack` | `String id` | Tạo `ItemStack` số lượng 1 từ Custom ID. |
| `create(id, amount)` | `ItemStack` | `String id`, `int amount` | Tạo `ItemStack` với số lượng chỉ định từ Custom ID. |
| `create(definition)` | `ItemStack` | `ItemDefinition definition` | Tạo `ItemStack` số lượng 1 từ đối tượng định nghĩa. |
| `create(definition, amount)` | `ItemStack` | `ItemDefinition def`, `int amount` | Tạo `ItemStack` với số lượng chỉ định từ định nghĩa. |

---

### 3.3 `ItemService` (Facade Chính)

Đây là interface **quan trọng nhất và tiện lợi nhất** cho các plugin khác tích hợp.

#### Bảng Phương Thức trong `ItemService`

| Tên phương thức | Kiểu dữ liệu trả về | Tham số | Mô tả / Tác dụng |
| :--- | :--- | :--- | :--- |
| `create(id)` | `ItemStack` | `String id` | Tạo `ItemStack` số lượng 1 với đầy đủ metadata. |
| `create(id, amount)` | `ItemStack` | `String id`, `int amount` | Tạo `ItemStack` với số lượng cụ thể. |
| `isItem(item, id)` | `boolean` | `ItemStack item`, `String id` | Kiểm tra `ItemStack` có đúng là custom item với ID chỉ định. |
| `isCustomItem(item)` | `boolean` | `ItemStack item` | Kiểm tra `ItemStack` có phải là bất kỳ custom item nào không. |
| `getId(item)` | `String` | `ItemStack item` | Lấy Custom Item ID từ PDC của item, `null` nếu là vanilla. |
| `getDefinition(id)` | `ItemDefinition` | `String id` | Lấy `ItemDefinition` theo ID chỉ định. |
| `exists(id)` | `boolean` | `String id` | Kiểm tra xem ID item có tồn tại trong hệ thống không. |
| `getProperties(item)` | `Map<String, Object>` | `ItemStack item` | Lấy Map thuộc tính từ định nghĩa của item đó. |
| `getInstanceData()` | `ItemInstanceData` | *Không có* | Lấy helper truy vấn dữ liệu độ bền / cấp độ cường hóa. |
| `validateAndUpdate(item)` | `ItemStack` | `ItemStack item` | Kiểm tra & đồng bộ các component theo config mới nhất. |

#### Chi Tiết Phương Thức Quan Trọng:

* **`create(String id, int amount)`**:
  - Tạo `ItemStack` hoàn chỉnh với full DisplayName, Lore, Components, và PDC Tag `haohanitemcore:item_id`.
  - Tự động gán **Random UUID PDC Tag** nếu vật phẩm không thể xếp chồng (`maxStackSize == 1`) để ngăn Minecraft tự gộp item.

* **`isItem(ItemStack item, String id)`**:
  - **Tác dụng:** Kiểm tra chính xác vật phẩm trên tay player có đúng là Custom Item với ID chỉ định hay không.
  - **Code Mẫu:**
    ```java
    if (itemService.isItem(handItem, "lunar:anorthosite_pickaxe")) {
        // Thực hiện logic riêng
    }
    ```

* **`validateAndUpdate(ItemStack item)`**:
  - **Tác dụng:** Đồng bộ vật phẩm trong tay/hòm đồ của player với config mới nhất trên server mà **không làm mất dữ liệu độ bền hay cường hóa custom**.
  - **Tự động đồng bộ:** CustomModelData, ItemModel, Built-in Equippable, Jukebox, MaxDamage, UUID tracking.

---

## 4. Hệ Thống Hành Vi Custom (`ItemBehavior` & `ItemContext`)

Cho phép gán xử lý logic trực tiếp vào từng `ItemDefinition`.

### Record `ItemContext`
Chứa đầy đủ ngữ cảnh khi một sự kiện xảy ra:
- `player()`: Người chơi tương tác.
- `item()`: `ItemStack` được sử dụng.
- `definition()`: `ItemDefinition` tương ứng.
- `event()`: Sự kiện Bukkit gốc (`PlayerInteractEvent`, `BlockBreakEvent`, v.v.).

### Bảng Phương Thức trong `ItemBehavior`

| Tên phương thức | Kiểu dữ liệu trả về | Tham số | Sự kiện Bukkit kích hoạt |
| :--- | :--- | :--- | :--- |
| `onUse(context)` | `default void` | `ItemContext context` | Chuột phải vật phẩm (`AIR` / `BLOCK`). |
| `onInteract(context)` | `default void` | `ItemContext context` | Chuột trái tương tác vật phẩm. |
| `onBreak(context)` | `default void` | `ItemContext context` | Khi player phá khối bằng vật phẩm này. |
| `onCraft(context)` | `default void` | `ItemContext context` | Khi vật phẩm được chế tạo thành công. |
| `onInventoryClick(context)` | `default void` | `ItemContext context` | Khi player click vật phẩm trong GUI / hòm đồ. |
| `onDrop(context)` | `default void` | `ItemContext context` | Khi player vứt vật phẩm ra đất. |
| `onPickup(context)` | `default void` | `ItemContext context` | Khi player nhặt vật phẩm từ dưới đất lên. |

### Code Mẫu Tạo Class Behavior Custom

```java
import vn.haohan.itemcore.api.item.ItemBehavior;
import vn.haohan.itemcore.api.item.ItemContext;

public class OxygenTankBehavior implements ItemBehavior {
    @Override
    public void onUse(ItemContext context) {
        var player = context.player();
        player.setRemainingAir(player.getMaximumAir());
        player.sendMessage("§a[HaoHanCore] Đã nạp đầy oxy!");
    }
}
```

---

## 5. Hệ Thống Recipe (Công Thức Chế Tạo)

### 5.1 `RecipeType`, `Ingredient`, `ItemResult`

#### Enum `RecipeType`
`SHAPED` (Có hình dạng) · `SHAPELESS` (Không hình dạng) · `SMELTING` (Nung thường) · `BLASTING` (Nung cao tần) · `SMOKING` (Hun khói) · `CAMPFIRE` (Lửa trại) · `STONECUTTING` (Cắt đá) · `SMITHING` (Bàn rèn) · `MACHINE` (Máy móc custom).

#### Sealed Interface `Ingredient`
Nguyên liệu chế tạo hỗ trợ 3 dạng:
1. `Ingredient.ItemIngredient(String id, int amount)`: Dùng Custom Item ID hoặc `minecraft:xxx`.
2. `Ingredient.MaterialIngredient(Material material, int amount)`: Dùng Material Bukkit trực tiếp.
3. `Ingredient.TagIngredient(String tag, int amount)`: Dùng Tag Minecraft (VD: `"minecraft:logs"`).

#### Record `ItemResult`
Vật phẩm đầu ra: `ItemResult(String item, int amount)` (Tham số `item` có thể là Custom ID hoặc `minecraft:xxx`).

---

### 5.2 `RecipeDefinition` & `ShapedRecipeDefinition`

#### Định Nghĩa Công Thức Thường (Shapeless / Smelting / Smithing...)

```java
import vn.haohan.itemcore.api.recipe.*;

// Công thức nung quặng custom
RecipeDefinition smeltingRecipe = new RecipeDefinition(
    "lunar:smelt_anorthosite",                 // ID công thức
    RecipeType.SMELTING,                      // Loại công thức
    List.of(new Ingredient.ItemIngredient("lunar:raw_anorthosite", 1)), // Nguyên liệu
    new ItemResult("lunar:anorthosite_ingot", 1), // Đầu ra
    0.7f,                                     // Điểm kinh nghiệm
    200                                       // Thời gian nung (ticks)
);
```

#### Định Nghĩa Công Thức Có Hình Dạng (`ShapedRecipeDefinition`)

```java
Map<Character, Ingredient> ingredients = Map.of(
    'A', new Ingredient.ItemIngredient("lunar:anorthosite_ingot", 1),
    'S', new Ingredient.MaterialIngredient(Material.STICK, 1)
);

ShapedRecipeDefinition shapedRecipe = new ShapedRecipeDefinition(
    "lunar:anorthosite_pickaxe_recipe",
    List.of(
        "AAA",
        " S ",
        " S "
    ),
    ingredients,
    new ItemResult("lunar:anorthosite_pickaxe", 1)
);
```

---

### 5.3 `RecipeRegistry`

Đăng ký và quản lý tất cả các công thức chế tạo trong hệ thống.

#### Bảng Phương Thức trong `RecipeRegistry`

| Tên phương thức | Kiểu dữ liệu trả về | Tham số | Mô tả / Tác dụng |
| :--- | :--- | :--- | :--- |
| `register(recipe)` | `void` | `RecipeDefinition recipe` | Đăng ký công thức chế tạo mới vào registry. |
| `get(id)` | `RecipeDefinition` | `String id` | Lấy RecipeDefinition theo ID, `null` nếu không tìm thấy. |
| `require(id)` | `RecipeDefinition` | `String id` | Lấy công thức theo ID, ném Exception nếu không tồn tại. |
| `exists(id)` | `boolean` | `String id` | Kiểm tra công thức ID đã được đăng ký chưa. |
| `unregister(id)` | `void` | `String id` | Xóa đăng ký của một công thức chế tạo. |
| `all()` | `Collection<RecipeDefinition>` | *Không có* | Lấy danh sách tất cả công thức chế tạo đã đăng ký. |
| `size()` | `int` | *Không có* | Lấy tổng số lượng công thức chế tạo có trên server. |
| `clear()` | `void` | *Không có* | Xóa toàn bộ công thức chế tạo khỏi registry. |

---

### 5.4 `RecipeService`

Service hỗ trợ tra cứu công thức chế tạo cho GUI hoặc hệ thống máy móc.

#### Bảng Phương Thức trong `RecipeService`

| Tên phương thức | Kiểu dữ liệu trả về | Tham số | Mô tả / Tác dụng |
| :--- | :--- | :--- | :--- |
| `findByResult(itemId)` | `List<RecipeDefinition>` | `String itemId` | Tìm tất cả công thức tạo ra item ID chỉ định. |
| `findByIngredient(itemId)`| `List<RecipeDefinition>` | `String itemId` | Tìm các công thức sử dụng item ID này làm nguyên liệu. |
| `findByType(type)` | `List<RecipeDefinition>` | `RecipeType type` | Tìm danh sách các công thức thuộc loại chỉ định. |
| `all()` | `Collection<RecipeDefinition>` | *Không có* | Lấy toàn bộ danh sách công thức chế tạo. |
| `search(keyword)` | `List<RecipeDefinition>` | `String keyword` | Tìm kiếm công thức theo từ khóa tên hoặc ID. |

---

## 6. Ví Dụ Tích Hợp Đầy Đủ (Full Integration Examples)

### Ví Dụ 1: Đăng Ký Item & Công Thức Chế Tạo Từ Plugin Khác

```java
package vn.haohan.myplugin;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import vn.haohan.itemcore.api.HaoHanItemCore;
import vn.haohan.itemcore.api.item.*;
import vn.haohan.itemcore.api.recipe.*;

import java.util.List;
import java.util.Map;

public class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        var itemRegistry = HaoHanItemCore.get().getItemRegistry();
        var recipeRegistry = HaoHanItemCore.get().getRecipeRegistry();

        // 1. Tạo và đăng ký Custom Item
        ItemDefinition spaceHelmet = ItemDefinition.builder("haohan:space_helmet")
                .material(Material.NETHERITE_HELMET)
                .displayName("§bMũ Phi Hành Gia")
                .maxStackSize(1)
                .type(ItemType.ARMOR)
                .addLore("§7Bảo vệ người chơi khỏi môi trường chân không.")
                .property("equippable_asset_id", "haohan:spacesuit")
                .component(new TierComponent(4, "Vũ Trụ"))
                .build();

        itemRegistry.register(spaceHelmet);

        // 2. Tạo và đăng ký Công Thức Chế Tạo
        ShapedRecipeDefinition recipe = new ShapedRecipeDefinition(
                "haohan:space_helmet_recipe",
                List.of(
                        "GGG",
                        "G G",
                        "   "
                ),
                Map.of('G', new Ingredient.MaterialIngredient(Material.GLASS, 1)),
                new ItemResult("haohan:space_helmet", 1)
        );

        recipeRegistry.register(recipe);
        
        getLogger().info("Đã đăng ký Mũ Phi Hành Gia thành công!");
    }
}
```

### Ví Dụ 2: Xử Lý Sự Kiện Lắng Nghe & Cập Nhật Vật Phẩm Trong Game

```java
package vn.haohan.myplugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import vn.haohan.itemcore.api.HaoHanItemCore;

public class ItemInteractListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        var itemService = HaoHanItemCore.get().getItemService();

        // Kiểm tra xem vật phẩm có phải là "haohan:space_helmet" không
        if (itemService.isItem(item, "haohan:space_helmet")) {
            // Tự động kiểm tra và nâng cấp component nếu config vừa được reload
            ItemStack updatedItem = itemService.validateAndUpdate(item);
            
            player.sendMessage("§aBạn đang cầm Mũ Phi Hành Gia chuẩn!");
        }
    }
}
```
