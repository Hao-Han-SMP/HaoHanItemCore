# BaseEngine — Custom Item Engine

## 1. Mục tiêu

`BaseEngine` là plugin nền tảng dùng để quản lý toàn bộ custom item và cung cấp API chung cho các plugin khác.

Các plugin gameplay như:

- Magic
- Weapon
- Machine
- Quest
- Economy

không tự quản lý `ItemStack`, NBT/PersistentData, item ID hoặc event routing mà sử dụng API của `BaseEngine`.

### Mục tiêu chính

- Quản lý custom item tập trung.
- Tránh trùng ID giữa các plugin.
- Tách item definition khỏi `ItemStack` thực tế.
- Cung cấp API đơn giản cho plugin khác.
- Xác định item bằng ID thay vì display name.
- Cho phép mở rộng behavior/component trong tương lai.
- Giảm code lặp giữa các plugin.

---

# 2. Kiến trúc

```text
                    ┌──────────────────────┐
                    │      BaseEngine      │
                    │                      │
                    │  ItemRegistry        │
                    │  ItemFactory         │
                    │  ItemDefinition      │
                    │  ItemService         │
                    │  ItemSerializer      │
                    │  EventRouter         │
                    └──────────┬───────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
       ┌──────▼─────┐   ┌──────▼─────┐   ┌──────▼─────┐
       │   Magic    │   │  Machine   │   │   Weapon   │
       │   Plugin   │   │   Plugin   │   │   Plugin   │
       └────────────┘   └────────────┘   └────────────┘
```

BaseEngine là tầng infrastructure.

Các plugin phía trên chỉ tập trung vào gameplay/content.

---

# 3. Namespace và Item ID

Mỗi item phải có ID duy nhất theo format:

```text
namespace:item_id
```

Ví dụ:

```text
magic:fire_crystal
magic:mana_shard

machine:steel_plate
machine:machine_core

weapon:plasma_sword
weapon:energy_cell
```

Không nên sử dụng ID dạng:

```text
fire_crystal
steel_plate
plasma_sword
```

Namespace tránh collision giữa các plugin.

---

# 4. ItemDefinition

`ItemDefinition` là metadata mô tả một custom item.

Không nên coi `ItemStack` là nguồn dữ liệu chính.

```kotlin
data class ItemDefinition(
    val id: String,
    val material: Material,
    val displayName: String,
    val maxStackSize: Int = 64,
    val lore: List<String> = emptyList(),
    val customModelData: Int? = null,
    val properties: Map<String, Any> = emptyMap()
)
```

Ví dụ:

```kotlin
val FIRE_CRYSTAL = ItemDefinition(
    id = "magic:fire_crystal",
    material = Material.EMERALD,
    displayName = "§cFire Crystal",
    maxStackSize = 16,
    lore = listOf(
        "§7A crystal containing",
        "§7unstable fire energy."
    )
)
```

---

# 5. ItemRegistry

`ItemRegistry` chịu trách nhiệm lưu trữ và tra cứu `ItemDefinition`.

```kotlin
interface ItemRegistry {

    fun register(definition: ItemDefinition)

    fun get(id: String): ItemDefinition?

    fun require(id: String): ItemDefinition

    fun exists(id: String): Boolean

    fun unregister(id: String)

    fun all(): Collection<ItemDefinition>
}
```

### Đăng ký item

```kotlin
BaseEngine.items.register(
    ItemDefinition(
        id = "magic:fire_crystal",
        material = Material.EMERALD,
        displayName = "§cFire Crystal"
    )
)
```

### Lấy item definition

```kotlin
val definition =
    BaseEngine.items.require("magic:fire_crystal")
```

---

# 6. ItemFactory

`ItemFactory` chuyển `ItemDefinition` thành `ItemStack`.

```kotlin
interface ItemFactory {

    fun create(id: String): ItemStack

    fun create(id: String, amount: Int): ItemStack
}
```

Ví dụ:

```kotlin
val crystal =
    BaseEngine.itemFactory.create(
        "magic:fire_crystal",
        4
    )
```

Factory chịu trách nhiệm:

- Material
- Amount
- Display name
- Lore
- CustomModelData
- PersistentData
- Các metadata khác

Plugin không cần tự tạo `ItemStack`.

---

# 7. Custom Item Identity

Không được xác định item bằng display name.

### Không nên

```kotlin
if (item.itemMeta.displayName == "§cFire Crystal") {
    ...
}
```

Display name có thể thay đổi và không phải identity của item.

### Nên

Lưu ID vào PersistentData.

```text
baseengine:item_id
    = "magic:fire_crystal"
```

Ví dụ:

```text
ItemStack
│
├── Material
├── DisplayName
├── Lore
├── CustomModelData
└── PersistentData
      └── baseengine:item_id
              └── magic:fire_crystal
```

Nhờ vậy item vẫn được nhận diện chính xác kể cả khi:

- đổi tên
- đổi lore
- đổi texture
- đổi model

---

# 8. ItemService

Có thể cung cấp một facade đơn giản cho plugin khác.

```kotlin
interface ItemService {

    fun create(id: String): ItemStack

    fun create(id: String, amount: Int): ItemStack

    fun is(item: ItemStack?, id: String): Boolean

    fun id(item: ItemStack?): String?

    fun definition(id: String): ItemDefinition

    fun exists(id: String): Boolean
}
```

Plugin khác có thể sử dụng:

```kotlin
val item =
    BaseEngine.items.create(
        "weapon:plasma_sword"
    )
```

Check item:

```kotlin
if (BaseEngine.items.is(
        item,
        "weapon:plasma_sword"
    )
) {
    // ...
}
```

Lấy ID:

```kotlin
val id =
    BaseEngine.items.id(item)
```

Kết quả:

```text
weapon:plasma_sword
```

---

# 9. ItemType

Có thể phân loại item bằng `ItemType`.

```kotlin
enum class ItemType {
    MATERIAL,
    TOOL,
    WEAPON,
    ARMOR,
    FOOD,
    MACHINE_COMPONENT,
    CURRENCY,
    SPECIAL
}
```

Ví dụ:

```kotlin
ItemDefinition(
    id = "machine:steel_plate",
    type = ItemType.MACHINE_COMPONENT,
    ...
)
```

ItemType có thể được sử dụng cho:

- GUI
- Filter
- Search
- Category
- Recipe system
- Debug
- Documentation

---

# 10. Item Behavior

Một custom item có thể có behavior riêng.

```kotlin
interface ItemBehavior {

    fun onUse(context: ItemContext)

    fun onInteract(context: ItemContext)

    fun onBreak(context: ItemContext)

    fun onCraft(context: ItemContext)
}
```

Ví dụ:

```kotlin
class PlasmaSwordBehavior : ItemBehavior {

    override fun onUse(context: ItemContext) {
        val player = context.player

        player.sendMessage("§bPlasma activated!")
    }
}
```

Definition:

```kotlin
ItemDefinition(
    id = "weapon:plasma_sword",
    ...
    behavior = PlasmaSwordBehavior()
)
```

---

# 11. ItemContext

Behavior không nên nhận quá nhiều parameter riêng lẻ.

Sử dụng `ItemContext`.

```kotlin
data class ItemContext(
    val player: Player,
    val item: ItemStack,
    val definition: ItemDefinition,
    val event: Event?
)
```

Ví dụ:

```kotlin
override fun onUse(context: ItemContext) {

    val player = context.player

    player.sendMessage(
        "§cFire!"
    )
}
```

---

# 12. Event Routing

BaseEngine có thể tự nhận Minecraft event và route tới behavior tương ứng.

```text
PlayerInteractEvent
        │
        ▼
    BaseEngine
        │
        ▼
    Item ID
        │
        ▼
weapon:plasma_sword
        │
        ▼
PlasmaSwordBehavior
```

Plugin Weapon không cần tạo listener riêng cho từng item.

BaseEngine chịu trách nhiệm:

1. Nhận event.
2. Xác định item.
3. Lấy `ItemDefinition`.
4. Lấy `ItemBehavior`.
5. Tạo `ItemContext`.
6. Gọi behavior tương ứng.

---

# 13. Component System

Component system là phần mở rộng cho tương lai.

Một item có thể được cấu thành từ nhiều component.

Ví dụ:

```kotlin
ItemDefinition(
    id = "weapon:plasma_sword",

    components = listOf(
        DamageComponent(12.0),
        DurabilityComponent(500),
        EnergyComponent(1000),
        CooldownComponent(20),
        FireComponent(3)
    )
)
```

Kết quả:

```text
Plasma Sword
│
├── Damage: 12
├── Durability: 500
├── Energy: 1000
├── Cooldown: 1 sec
└── Fire: 3 sec
```

Thay vì tạo nhiều class:

```text
PlasmaSword
FirePlasmaSword
ChargedPlasmaSword
LegendaryPlasmaSword
...
```

có thể compose item bằng component.

---

# 14. Plugin Lifecycle

BaseEngine cần có lifecycle rõ ràng.

```text
BaseEngine
    │
    ├── onLoad()
    │
    ├── Initialize Registry
    │
    ├── Load dependent plugins
    │       │
    │       └── Register items
    │
    ├── Registry ready
    │
    └── onEnable()
```

Plugin có thể cung cấp module:

```kotlin
interface EngineModule {

    fun registerItems(
        registry: ItemRegistry
    )

    fun registerRecipes(
        recipeRegistry: RecipeRegistry
    )

    fun registerBehaviors()

    fun onEnable()
}
```

---

# 15. Ví dụ Plugin

Một plugin Magic:

```kotlin
object ModItems {

    val FIRE_CRYSTAL = ItemDefinition(
        id = "magic:fire_crystal",
        material = Material.EMERALD,
        displayName = "§cFire Crystal",
        maxStackSize = 16
    )

    fun register() {
        BaseEngine.items.register(
            FIRE_CRYSTAL
        )
    }
}
```

Plugin chỉ cần gọi:

```kotlin
ModItems.register()
```

Sau đó các plugin khác có thể sử dụng:

```kotlin
val crystal =
    BaseEngine.items.create(
        "magic:fire_crystal",
        8
    )
```

---

# 16. Dependency

Các plugin gameplay phụ thuộc vào BaseEngine:

```text
BaseEngine
    ↑
    ├── Magic
    ├── Machine
    ├── Weapon
    ├── Quest
    └── Economy
```

Plugin bắt buộc sử dụng:

```yaml
depend:
  - BaseEngine
```

Plugin tùy chọn:

```yaml
softdepend:
  - BaseEngine
```

---

# 17. API Public

API public nên được giữ nhỏ.

Các API quan trọng nhất:

```text
BaseEngine
│
├── items
│   ├── register()
│   ├── unregister()
│   ├── get()
│   ├── require()
│   ├── create()
│   ├── is()
│   ├── id()
│   └── exists()
│
├── registry
│
├── events
│
└── services
```

Mục tiêu:

```kotlin
BaseEngine.items.create("magic:fire_crystal")
```

thay vì plugin tự xử lý:

```text
PersistentData handling
ItemStack creation
Item ID parsing
Registry implementation
Item validation
Event handling
```

---

# 18. Cấu trúc package đề xuất

```text
baseengine/
│
├── api/
│   ├── BaseEngine.kt
│   │
│   ├── item/
│   │   ├── ItemDefinition.kt
│   │   ├── ItemRegistry.kt
│   │   ├── ItemFactory.kt
│   │   ├── ItemService.kt
│   │   ├── ItemBehavior.kt
│   │   └── ItemContext.kt
│   │
│   └── registry/
│       └── Registry.kt
│
├── item/
│   ├── DefaultItemRegistry.kt
│   ├── DefaultItemFactory.kt
│   └── DefaultItemService.kt
│
├── data/
│   ├── PersistentData.kt
│   └── ItemData.kt
│
├── event/
│   └── ItemEventRouter.kt
│
├── component/
│   ├── ItemComponent.kt
│   ├── DamageComponent.kt
│   ├── DurabilityComponent.kt
│   └── ...
│
└── internal/
    └── ...
```

---

# 19. Nguyên tắc thiết kế

## 19.1. Item ID là identity

```text
magic:fire_crystal
```

là identity duy nhất.

Không sử dụng:

```text
displayName
lore
material
customModelData
```

để xác định item.

---

## 19.2. Registry là source of truth

Không để mỗi plugin tự giữ một registry riêng.

```text
BaseEngine ItemRegistry
        │
        ├── Magic items
        ├── Weapon items
        ├── Machine items
        └── Other items
```

---

## 19.3. Plugin chỉ định nghĩa content

Plugin Weapon nên chứa:

```text
Plasma Sword
Laser Gun
Energy Cell
```

Không nên chứa logic chung như:

```text
PersistentData handling
ItemStack creation
Item ID parsing
Registry implementation
```

Những phần này thuộc BaseEngine.

---

## 19.4. API nhỏ trước

Version đầu tiên chỉ cần:

```text
ItemDefinition
ItemRegistry
ItemFactory
ItemService
PersistentData
```

Không cần ngay lập tức xây:

```text
Component framework
Recipe framework
ResourcePack framework
Complex event framework
Dependency injection
Database
```

Chỉ thêm khi có use-case thực tế.

---

# 20. Roadmap

## Version 1.0 — Core

```text
[x] ItemDefinition
[x] ItemRegistry
[x] ItemFactory
[x] ItemService
[x] Namespaced ID
[x] PersistentData
[x] ItemStack identity
```

## Version 1.1 — Behavior

```text
[ ] ItemBehavior
[ ] ItemContext
[ ] EventRouter
```

## Version 1.2 — Component

```text
[ ] ItemComponent
[ ] DamageComponent
[ ] DurabilityComponent
[ ] EnergyComponent
[ ] CooldownComponent
```

## Version 1.3 — Resource

```text
[ ] CustomModelData manager
[ ] ResourcePack integration
[ ] Texture management
[ ] Model management
```

## Version 2.0 — Ecosystem

```text
[ ] RecipeRegistry
[ ] BlockRegistry
[ ] EntityRegistry
[ ] GUI API
[ ] Serialization
[ ] Configuration system
```

---

# 21. Mục tiêu cuối cùng

Các plugin gameplay chỉ cần quan tâm tới:

```text
WHAT

"Fire Crystal là gì?"
"Plasma Sword làm gì?"
"Steel Plate dùng để làm gì?"
```

Còn BaseEngine chịu trách nhiệm:

```text
HOW

Item được tạo như thế nào?
Item được lưu ID như thế nào?
Item được nhận diện như thế nào?
Item được serialize như thế nào?
Event được route như thế nào?
Metadata được quản lý như thế nào?
```

Kiến trúc cuối:

```text
                 ┌───────────────────────┐
                 │      BASE ENGINE      │
                 │                       │
                 │      Item API         │
                 │      Registry         │
                 │      Factory          │
                 │      PersistentData   │
                 │      Event Router     │
                 │                       │
                 └───────────┬───────────┘
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
          ▼                  ▼                  ▼
       MAGIC              WEAPON             MACHINE
          │                  │                  │
       Content            Content            Content
       Behavior           Behavior           Behavior
       Recipe             Recipe             Recipe
```

## Nguyên tắc cốt lõi

```text
BaseEngine = HOW
Plugins    = WHAT
```

`BaseEngine` quản lý **infrastructure**, plugin khác quản lý **content và gameplay**.
