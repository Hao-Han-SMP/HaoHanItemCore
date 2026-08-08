# BaseEngine — Recipe System

## 1. Mục tiêu

`Recipe System` là module quản lý công thức chế tạo trong `BaseEngine`.

Nó cho phép các plugin khác đăng ký công thức sử dụng:

- Minecraft item
- Custom item từ BaseEngine
- Material
- Tag
- Các loại recipe khác

Recipe không tự tạo hoặc quản lý item. Nó chỉ tham chiếu tới `ItemRegistry` của BaseEngine.

```text
BaseEngine
│
├── Item System
│   └── ItemRegistry
│
└── Recipe System
    └── RecipeRegistry
```

---

# 2. Kiến trúc

```text
                    ┌──────────────────────┐
                    │      BaseEngine      │
                    └──────────┬───────────┘
                               │
              ┌────────────────┴────────────────┐
              │                                 │
       ┌──────▼───────┐                 ┌───────▼──────┐
       │ Item System  │◄────────────────│ Recipe System│
       │              │                 │              │
       │ ItemRegistry │                 │ RecipeRegistry│
       │ ItemFactory  │                 │ RecipeService │
       └──────────────┘                 └───────┬──────┘
                                               │
                              ┌────────────────┼────────────────┐
                              │                │                │
                           Magic            Weapon           Machine
```

Recipe System phụ thuộc vào Item System để xác định ingredient và result.

---

# 3. Recipe ID

Mỗi recipe phải có ID riêng theo namespace:

```text
namespace:recipe_id
```

Ví dụ:

```text
magic:mana_crystal
machine:steel_plate
machine:machine_frame
weapon:plasma_sword
```

Recipe ID khác với Item ID.

Ví dụ:

```text
Item:
machine:steel_plate

Recipe:
machine:steel_plate
```

Hai ID có thể trùng phần cuối nhưng vẫn là hai resource khác nhau.

---

# 4. RecipeType

Recipe System hỗ trợ nhiều loại recipe.

```kotlin
enum class RecipeType {
    SHAPED,
    SHAPELESS,
    SMELTING,
    BLASTING,
    SMOKING,
    CAMPFIRE,
    STONECUTTING,
    SMITHING,
    MACHINE
}
```

Có thể mở rộng sau này mà không thay đổi Item System.

---

# 5. RecipeDefinition

Recipe được mô tả bằng `RecipeDefinition`.

```kotlin
data class RecipeDefinition(
    val id: String,
    val type: RecipeType,
    val ingredients: List<Ingredient>,
    val result: ItemResult
)
```

Với shaped recipe có thể bổ sung pattern:

```kotlin
data class ShapedRecipeDefinition(
    val id: String,
    val pattern: List<String>,
    val ingredients: Map<Char, Ingredient>,
    val result: ItemResult
)
```

---

# 6. Ingredient

Ingredient là nguyên liệu đầu vào của recipe.

Không nên giới hạn ingredient chỉ bằng `Material`.

Nên có abstraction:

```kotlin
sealed interface Ingredient {

    data class Item(
        val id: String,
        val amount: Int = 1
    ) : Ingredient

    data class Material(
        val material: org.bukkit.Material,
        val amount: Int = 1
    ) : Ingredient

    data class Tag(
        val tag: String,
        val amount: Int = 1
    ) : Ingredient
}
```

---

# 7. Custom Item Ingredient

Recipe có thể sử dụng custom item từ BaseEngine.

Ví dụ:

```kotlin
Ingredient.Item(
    id = "machine:steel_core",
    amount = 1
)
```

Recipe:

```text
machine:steel_plate
│
├── minecraft:iron_ingot × 8
└── machine:steel_core × 1
```

BaseEngine sẽ kiểm tra `machine:steel_core` thông qua:

```kotlin
BaseEngine.items.exists(
    "machine:steel_core"
)
```

---

# 8. Minecraft Item Ingredient

Có thể sử dụng item vanilla:

```kotlin
Ingredient.Item(
    id = "minecraft:iron_ingot",
    amount = 8
)
```

Hoặc Material:

```kotlin
Ingredient.Material(
    material = Material.IRON_INGOT,
    amount = 8
)
```

Tùy implementation, có thể quy chuẩn tất cả vanilla item về một namespace:

```text
minecraft:iron_ingot
minecraft:diamond
minecraft:stick
```

---

# 9. Tag Ingredient

Cho phép recipe chấp nhận nhiều item tương đương.

Ví dụ:

```kotlin
Ingredient.Tag(
    tag = "forge:ingots/iron",
    amount = 8
)
```

Có nghĩa recipe chấp nhận bất kỳ item nào thuộc tag:

```text
forge:ingots/iron
```

Điều này đặc biệt hữu ích khi nhiều plugin cung cấp cùng loại nguyên liệu.

---

# 10. ItemResult

Kết quả recipe cũng nên tham chiếu tới Item System.

```kotlin
data class ItemResult(
    val item: String,
    val amount: Int = 1
)
```

Ví dụ:

```kotlin
ItemResult(
    item = "machine:steel_plate",
    amount = 1
)
```

Khi tạo output:

```kotlin
BaseEngine.items.create(
    "machine:steel_plate",
    1
)
```

Recipe System không tự tạo `ItemStack`.

---

# 11. Shaped Recipe

Ví dụ recipe:

```text
I I I
I C I
I I I
```

Có thể định nghĩa:

```kotlin
ShapedRecipeDefinition(
    id = "machine:steel_plate",

    pattern = listOf(
        "III",
        "ICI",
        "III"
    ),

    ingredients = mapOf(
        'I' to Ingredient.Item(
            "minecraft:iron_ingot",
            1
        ),

        'C' to Ingredient.Item(
            "machine:steel_core",
            1
        )
    ),

    result = ItemResult(
        item = "machine:steel_plate",
        amount = 1
    )
)
```

---

# 12. Shapeless Recipe

Recipe không quan tâm vị trí:

```kotlin
RecipeDefinition(
    id = "magic:mana_crystal",

    type = RecipeType.SHAPELESS,

    ingredients = listOf(
        Ingredient.Item(
            "minecraft:amethyst_shard",
            4
        ),

        Ingredient.Item(
            "magic:mana_dust",
            2
        )
    ),

    result = ItemResult(
        item = "magic:mana_crystal",
        amount = 1
    )
)
```

---

# 13. Smelting Recipe

Recipe có input và output:

```text
Steel Core
    │
    ▼
 Furnace
    │
    ▼
Refined Steel
```

Ví dụ:

```kotlin
RecipeDefinition(
    id = "machine:refined_steel",

    type = RecipeType.SMELTING,

    ingredients = listOf(
        Ingredient.Item(
            "machine:steel_core",
            1
        )
    ),

    result = ItemResult(
        item = "machine:refined_steel",
        amount = 1
    )
)
```

Có thể bổ sung:

```kotlin
val experience: Float
val cookingTime: Int
```

---

# 14. Machine Recipe

BaseEngine không nhất thiết chỉ quản lý Minecraft crafting.

Các plugin có thể đăng ký custom machine recipe.

Ví dụ:

```text
                    Steel Core
                        │
                        ▼
Iron × 8 ───────► Industrial Press
                        │
                        ▼
                  Steel Plate
```

Definition:

```kotlin
RecipeDefinition(
    id = "machine:steel_plate",

    type = RecipeType.MACHINE,

    ingredients = listOf(
        Ingredient.Item(
            "minecraft:iron_ingot",
            8
        ),

        Ingredient.Item(
            "machine:steel_core",
            1
        )
    ),

    result = ItemResult(
        item = "machine:steel_plate",
        amount = 1
    )
)
```

Plugin Machine có thể tự quyết định cách thực thi recipe.

BaseEngine chỉ quản lý definition và lookup.

---

# 15. RecipeRegistry

```kotlin
interface RecipeRegistry {

    fun register(
        recipe: RecipeDefinition
    )

    fun get(
        id: String
    ): RecipeDefinition?

    fun require(
        id: String
    ): RecipeDefinition

    fun exists(
        id: String
    ): Boolean

    fun unregister(
        id: String
    )

    fun all():
        Collection<RecipeDefinition>
}
```

Đăng ký:

```kotlin
BaseEngine.recipes.register(
    STEEL_PLATE_RECIPE
)
```

---

# 16. RecipeService

`RecipeService` cung cấp các thao tác lookup hữu ích.

```kotlin
interface RecipeService {

    fun register(
        recipe: RecipeDefinition
    )

    fun get(
        id: String
    ): RecipeDefinition?

    fun findByResult(
        itemId: String
    ): List<RecipeDefinition>

    fun findByIngredient(
        itemId: String
    ): List<RecipeDefinition>

    fun all():
        Collection<RecipeDefinition>
}
```

---

# 17. Find Recipe By Result

Ví dụ:

```kotlin
BaseEngine.recipes.findByResult(
    "machine:steel_plate"
)
```

Có thể trả về:

```text
machine:steel_plate
├── Crafting Recipe
├── Industrial Press Recipe
└── Advanced Alloy Recipe
```

Điều này cho phép GUI hiển thị:

```text
             Steel Plate

Recipe 1
[Iron] [Iron] [Iron]
[Iron] [Core] [Iron]
[Iron] [Iron] [Iron]

Recipe 2
Iron × 4 + Steel Core
        ↓
Industrial Press
        ↓
Steel Plate
```

---

# 18. Find Recipe By Ingredient

Ví dụ:

```kotlin
BaseEngine.recipes.findByIngredient(
    "machine:steel_core"
)
```

Có thể trả về:

```text
machine:steel_core
       │
       ├── machine:steel_plate
       ├── machine:machine_frame
       └── weapon:plasma_sword
```

Từ đó có thể xây hệ thống:

- Recipe Viewer
- Item Usage
- Recipe Book
- Dependency Viewer
- Crafting Guide

---

# 19. Recipe Dependency Graph

Recipe System có thể xây dependency graph:

```text
machine:machine_frame
        │
        ├── machine:steel_plate
        │       │
        │       ├── machine:steel_core
        │       │       │
        │       │       └── minecraft:iron_ingot
        │       │
        │       └── minecraft:iron_ingot
        │
        └── machine:steel_core
```

Điều này cho phép truy vấn:

> Muốn chế tạo `machine:machine_frame` cần những gì?

Hoặc:

> `minecraft:iron_ingot` được sử dụng để chế tạo những item nào?

---

# 20. Recipe Viewer

Recipe System nên cung cấp API để GUI có thể sử dụng.

Ví dụ:

```kotlin
val recipes =
    BaseEngine.recipes.findByResult(
        "machine:steel_plate"
    )
```

GUI:

```text
┌──────────────────────────────────────┐
│              Steel Plate             │
│                                      │
│  Recipe 1                            │
│                                      │
│  [Iron] [Iron] [Iron]                │
│  [Iron] [Core] [Iron]                │
│  [Iron] [Iron] [Iron]                │
│                 ↓                    │
│             [Steel Plate]            │
│                                      │
│  ◀ Recipe 1 / 3                 ▶   │
└──────────────────────────────────────┘
```

Các item cuối dòng có thể click để chuyển sang item đó.

Ví dụ:

```text
Iron Ingot
    │
    └── click
          ▼
      Iron Ingot GUI

Steel Core
    │
    └── click
          ▼
      Steel Core GUI
```

---

# 21. Recipe Validation

Khi đăng ký recipe, BaseEngine nên validate:

```text
Recipe ID có hợp lệ?
        │
        ▼
Result có tồn tại?
        │
        ▼
Ingredient có tồn tại?
        │
        ▼
Ingredient amount > 0?
        │
        ▼
Pattern có hợp lệ?
        │
        ▼
Register
```

Ví dụ nếu recipe tham chiếu:

```text
machine:unknown_item
```

BaseEngine có thể cảnh báo:

```text
[BaseEngine] Failed to register recipe:
machine:steel_plate

Unknown ingredient:
machine:unknown_item
```

---

# 22. Không nên để Recipe System phụ thuộc implementation của Item

Recipe chỉ nên biết:

```text
Item ID
```

Ví dụ:

```kotlin
Ingredient.Item(
    "machine:steel_core",
    1
)
```

Không nên:

```kotlin
Ingredient.ItemStack(
    someItemStack
)
```

Lý do:

- Recipe definition có thể serialize.
- Recipe có thể lưu config.
- Recipe có thể load lại.
- Recipe không phụ thuộc lifecycle của ItemStack.
- Dễ xây GUI.
- Dễ xây dependency graph.

---

# 23. Serialization

Recipe có thể được lưu dưới dạng YAML/JSON.

Ví dụ YAML:

```yaml
id: machine:steel_plate
type: SHAPED

pattern:
  - "III"
  - "ICI"
  - "III"

ingredients:
  I:
    type: item
    id: minecraft:iron_ingot
    amount: 1

  C:
    type: item
    id: machine:steel_core
    amount: 1

result:
  item: machine:steel_plate
  amount: 1
```

Điều này cho phép content plugin có thể định nghĩa recipe bằng configuration thay vì hard-code.

---

# 24. Ví dụ hoàn chỉnh

## Item

```kotlin
val STEEL_CORE = ItemDefinition(
    id = "machine:steel_core",
    material = Material.IRON_NUGGET,
    displayName = "§7Steel Core"
)

val STEEL_PLATE = ItemDefinition(
    id = "machine:steel_plate",
    material = Material.IRON_INGOT,
    displayName = "§fSteel Plate"
)
```

## Register Items

```kotlin
BaseEngine.items.register(STEEL_CORE)
BaseEngine.items.register(STEEL_PLATE)
```

## Recipe

```kotlin
val STEEL_PLATE_RECIPE =
    ShapedRecipeDefinition(
        id = "machine:steel_plate",

        pattern = listOf(
            "III",
            "ICI",
            "III"
        ),

        ingredients = mapOf(
            'I' to Ingredient.Item(
                "minecraft:iron_ingot"
            ),

            'C' to Ingredient.Item(
                "machine:steel_core"
            )
        ),

        result = ItemResult(
            item = "machine:steel_plate",
            amount = 1
        )
    )
```

## Register Recipe

```kotlin
BaseEngine.recipes.register(
    STEEL_PLATE_RECIPE
)
```

---

# 25. Mối quan hệ Item ↔ Recipe

```text
                         BaseEngine
                             │
                ┌────────────┴────────────┐
                │                         │
                ▼                         ▼
          ItemRegistry              RecipeRegistry
                │                         │
                │                         │
       machine:steel_core        machine:steel_plate
       machine:steel_plate                │
                                          │
                              ┌───────────┴───────────┐
                              │                       │
                       minecraft:iron_ingot   machine:steel_core
```

Item Registry là source of truth cho item.

Recipe Registry là source of truth cho recipe.

Recipe chỉ tham chiếu Item ID.

---

# 26. Nguyên tắc thiết kế

### Recipe không tạo Item

```text
Recipe System
      │
      ▼
Item ID
      │
      ▼
Item System
      │
      ▼
ItemStack
```

### Recipe không phụ thuộc GUI

Recipe chỉ cung cấp data.

GUI tự quyết định cách hiển thị.

### Recipe không phụ thuộc plugin cụ thể

Recipe có thể được đăng ký bởi:

```text
Magic
Machine
Weapon
Technology
Alchemy
```

nhưng tất cả dùng cùng API.

### Recipe ID phải unique

```text
magic:mana_crystal
machine:steel_plate
weapon:plasma_sword
```

---

# 27. Roadmap

## V1 — Core Recipe

```text
[x] RecipeDefinition
[x] RecipeRegistry
[x] RecipeService
[x] RecipeType
[x] Ingredient
[x] ItemResult
[x] Custom Item support
[x] Vanilla Item support
```

## V1.1 — Minecraft Integration

```text
[ ] Bukkit/Paper CraftingRecipe integration
[ ] Smelting integration
[ ] Smithing integration
[ ] Stonecutting integration
```

## V1.2 — Advanced

```text
[ ] Tag Ingredient
[ ] Machine Recipe
[ ] Recipe validation
[ ] Recipe serialization
```

## V1.3 — Recipe Viewer

```text
[ ] Find recipes by result
[ ] Find recipes by ingredient
[ ] Recipe dependency graph
[ ] Recipe GUI API
[ ] Recipe pagination
[ ] Item → Recipe navigation
```

---

# 28. Nguyên tắc cốt lõi

```text
Item System
    = WHAT IS THE ITEM?

Recipe System
    = HOW CAN THE ITEM BE CREATED?

ItemRegistry
    = Item definitions

RecipeRegistry
    = Recipe definitions

Recipe
    → references Item IDs

ItemFactory
    → creates ItemStack

RecipeService
    → queries recipes
```

Kiến trúc cuối:

```text
                         BaseEngine
                             │
            ┌────────────────┼────────────────┐
            │                │                │
            ▼                ▼                ▼
       Item System      Recipe System     Event System
            │                │
            │                │
            └───────┬────────┘
                    ▼
              Other Plugins
                    │
        ┌───────────┼───────────┐
        ▼           ▼           ▼
      Magic      Machine      Weapon
```

`Item System` quản lý item.

`Recipe System` quản lý cách tạo item.

Hai hệ thống độc lập về implementation nhưng liên kết với nhau thông qua **namespaced Item ID**.
