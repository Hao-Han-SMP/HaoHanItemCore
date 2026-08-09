# Tài liệu API HaoHanItemCore

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

### Danh Sách Phương Thức trong `HaoHanItemCore`

#### `static HaoHanItemCore get()`
* **Tác dụng:** Lấy instance duy nhất của `HaoHanItemCore`.
* **Tham số:** Không có.
* **Trả về:** `HaoHanItemCore` — Instance đang hoạt động.
* **Lưu ý:** Ném `IllegalStateException` nếu plugin chưa hoàn tất giai đoạn `onLoad()`.

#### `ItemRegistry getItemRegistry()`
* **Tác dụng:** Truy cập registry lưu trữ danh sách định nghĩa item (`ItemDefinition`).
* **Trả về:** `ItemRegistry`

#### `ItemFactory getItemFactory()`
* **Tác dụng:** Truy cập factory khởi tạo `ItemStack` từ định nghĩa.
* **Trả về:** `ItemFactory`

#### `ItemService getItemService()`
* **Tác dụng:** Truy cập service tổng hợp (Facade). Đây là lựa chọn **khuyến nghị** cho các plugin bên ngoài.
* **Trả về:** `ItemService`

#### `RecipeRegistry getRecipeRegistry()`
* **Tác dụng:** Truy cập registry quản lý công thức chế tạo (`RecipeDefinition`).
* **Trả về:** `RecipeRegistry`

#### `RecipeService getRecipeService()`
* **Tác dụng:** Truy cập service tra cứu và tìm kiếm công thức chế tạo.
* **Trả về:** `RecipeService`

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

| Phương thức | Tác dụng | Giá trị trả về |
| :--- | :--- | :--- |
| `String getId()` | Lấy ID đầy đủ dạng `namespace:key` | `String` (VD: `"lunar:anorthosite_pickaxe"`) |
| `String getNamespace()` | Lấy phần namespace (trước `:`) | `String` (VD: `"lunar"`) |
| `String getKey()` | Lấy phần key (sau `:`) | `String` (VD: `"anorthosite_pickaxe"`) |
| `Material getMaterial()` | Material Minecraft gốc của item | `Material` (VD: `Material.DIAMOND_PICKAXE`) |
| `String getDisplayName()` | Tên hiển thị (hỗ trợ mã màu `§` hoặc `&`) | `String` |
| `int getMaxStackSize()` | Số lượng xếp chồng tối đa (1 - 99) | `int` |
| `List<String> getLore()` | Danh sách các dòng mô tả cơ bản | `List<String>` |
| `Integer getCustomModelData()`| Giá trị CustomModelData (nếu dùng resourcepack cũ) | `Integer` (có thể `null`) |
| `String getItemModel()` | Mô hình 1.21.4+ (`NamespacedKey` dạng String) | `String` (có thể `null`) |
| `Map<String, Object> getProperties()` | Map các thuộc tính tùy biến | `Map<String, Object>` |
| `ItemType getType()` | Thể loại vập phẩm | `ItemType` |
| `ItemBehavior getBehavior()` | Đối tượng lắng nghe hành vi custom | `ItemBehavior` (có thể `null`) |
| `List<ItemComponent> getComponents()` | Các component chức năng tùy biến | `List<ItemComponent>` |
| `List<ItemInfoSection> getInfoSections()` | Danh sách các mục lore định hình sẵn | `List<ItemInfoSection>` |
| `boolean hasBehavior()` | Kiểm tra vật phẩm có hành vi custom không | `boolean` |
| `static boolean isValidId(String id)` | Kiểm tra định dạng ID đúng dạng `namespace:key` | `boolean` |

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

#### Các Property Keys Tự Động Xử Lý (Built-in Properties)

| Property Key | Kiểu Dữ Liệu | Tác Dụng |
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

#### Phương Thức trong `ItemInstanceData`

| Phương thức | Tác dụng | Tham số | Giá trị trả về |
| :--- | :--- | :--- | :--- |
| `durability(ItemStack item, int defaultValue)` | Đọc độ bền còn lại của vật phẩm | `ItemStack`, `int default` | `int` |
| `setDurability(ItemStack item, int value)` | Ghi độ bền mới cho vật phẩm | `ItemStack`, `int value` | `void` |
| `upgradeLevel(ItemStack item)` | Đọc cấp độ cường hóa/nâng cấp | `ItemStack` | `int` |
| `setUpgradeLevel(ItemStack item, int level)` | Ghi cấp độ cường hóa/nâng cấp mới | `ItemStack`, `int level` | `void` |

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

```java
public interface ItemRegistry {
    void register(ItemDefinition definition);
    ItemDefinition get(String id);
    ItemDefinition require(String id);
    boolean exists(String id);
    void unregister(String id);
    Collection<ItemDefinition> all();
    List<ItemDefinition> getByNamespace(String namespace);
    List<ItemDefinition> search(String keyword);
    int size();
    void clear();
}
```

* **`register(ItemDefinition def)`**: Đăng ký item. Trống ném `IllegalArgumentException` nếu ID đã tồn tại.
* **`require(String id)`**: Giống `get(id)` nhưng ném exception nếu không tìm thấy item.
* **`getByNamespace(String ns)`**: Lấy danh sách tất cả item của một plugin/hệ thống (VD: `"lunar"`).

---

### 3.2 `ItemFactory`

Chịu trách nhiệm khởi tạo `ItemStack` chuẩn từ `ItemDefinition` hoặc `id`.

```java
public interface ItemFactory {
    ItemStack create(String id);
    ItemStack create(String id, int amount);
    ItemStack create(ItemDefinition definition);
    ItemStack create(ItemDefinition definition, int amount);
}
```

---

### 3.3 `ItemService` (Facade Chính)

Đây là interface **quan trọng nhất và tiện lợi nhất** cho các plugin khác tích hợp.

#### Danh Sách Phương Thức trong `ItemService`

```java
public interface ItemService {
    ItemStack create(String id);
    ItemStack create(String id, int amount);
    boolean isItem(ItemStack item, String id);
    boolean isCustomItem(ItemStack item);
    String getId(ItemStack item);
    ItemDefinition getDefinition(String id);
    boolean exists(String id);
    Map<String, Object> getProperties(ItemStack item);
    ItemInstanceData getInstanceData();
    ItemStack validateAndUpdate(ItemStack item);
}
```

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

### Interface `ItemBehavior`

```java
public interface ItemBehavior {
    default void onUse(ItemContext context) {}             // Chuột phải (AIR / BLOCK)
    default void onInteract(ItemContext context) {}        // Chuột trái
    default void onBreak(ItemContext context) {}           // Phá khối thành công
    default void onCraft(ItemContext context) {}           // Chế tạo thành công
    default void onInventoryClick(ItemContext context) {}   // Click trong GUI/Hòm đồ
    default void onDrop(ItemContext context) {}            // Vứt item ra đất
    default void onPickup(ItemContext context) {}          // Nhặt item từ đất
}
```

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

```java
public interface RecipeRegistry {
    void register(RecipeDefinition recipe);
    RecipeDefinition get(String id);
    RecipeDefinition require(String id);
    boolean exists(String id);
    void unregister(String id);
    Collection<RecipeDefinition> all();
    int size();
    void clear();
}
```

---

### 5.4 `RecipeService`

Service hỗ trợ tra cứu công thức chế tạo cho GUI hoặc hệ thống máy móc.

```java
public interface RecipeService {
    List<RecipeDefinition> findByResult(String itemId);      // Tìm các công thức chế tạo ra item này
    List<RecipeDefinition> findByIngredient(String itemId);  // Tìm các công thức sử dụng item này làm nguyên liệu
    List<RecipeDefinition> findByType(RecipeType type);       // Tìm công thức theo loại
    Collection<RecipeDefinition> all();
    List<RecipeDefinition> search(String keyword);            // Tìm kiếm theo từ khóa
}
```

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
