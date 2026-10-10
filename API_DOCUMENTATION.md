# Tai Lieu API Chi Tiet: HaoHanItemCore

Tai lieu nay cung cap chi tiet toan dien ve cac lop (classes), giao dien (interfaces), cac thanh phan mo rong (components), va cac phuong thuc (methods) trong API cua HaoHanItemCore.

HaoHanItemCore la he thong cot loi quan ly Custom Items, Custom Recipes va Custom Blocks the he moi cho HaoHan SMP, giup cac plugin ve tinh (nhu Lunar, Magic, Tech, Quest, Armor, Tools, v.v.) de dang tich hop, tra cuu va mo rong tinh nang.

---

## Muc Luc API

1. [Diem Khoi Dau (API Entrypoint) — HaoHanItemCore](#1-diem-khoi-dau-api-entrypoint--haohanitemcore)
2. [He Thong Dinh Nghia Custom Item](#2-he-thong-dinh-nghia-custom-item)
   - [2.1 Enum ItemType](#21-enum-itemtype)
   - [2.2 Class ItemDefinition & Builder](#22-class-itemdefinition--builder)
   - [2.3 Cac Thanh Phan Lap Trinh (ItemComponent)](#23-cac-thanh-phan-lap-trinh-itemcomponent)
     - [AbilityComponent](#abilitycomponent)
     - [AttributeComponent](#attributecomponent)
     - [DurabilityComponent](#durabilitycomponent)
     - [MiningComponent](#miningcomponent)
     - [TierComponent](#tiercomponent)
   - [2.4 Cau Truc Lore Nang Cao (ItemInfoSection)](#24-cau-truc-lore-nang-cao-iteminfosection)
   - [2.5 Du Lieu Tung Vat Pham (ItemInstanceData)](#25-du-lieu-tung-vat-pham-iteminstancedata)
3. [Registry & Services Quan Ly Item](#3-registry--services-quan-ly-item)
   - [3.1 ItemRegistry](#31-itemregistry)
   - [3.2 ItemFactory](#32-itemfactory)
   - [3.3 ItemService (Facade Chinh)](#33-itemservice-facade-chinh)
4. [He Thong Hanh Vi Custom (ItemBehavior & ItemContext)](#4-he-thong-hanh-vi-custom-itembehavior--itemcontext)
5. [He Thong Recipe (Cong Thuc Che Tao)](#5-he-thong-recipe-cong-thuc-che-tao)
   - [5.1 RecipeType, Ingredient, ItemResult](#51-recipetype-ingredient-itemresult)
   - [5.2 RecipeDefinition & ShapedRecipeDefinition](#52-recipedefinition--shapedrecipedefinition)
   - [5.3 RecipeRegistry](#53-reciperegistry)
   - [5.4 RecipeService](#54-recipeservice)
6. [He Thong Custom Block (Barrier + BlockDisplay Engine)](#6-he-thong-custom-block-barrier--blockdisplay-engine)
   - [6.1 CustomBlockDefinition & Builder](#61-customblockdefinition--builder)
   - [6.2 CustomBlockModel](#62-customblockmodel)
   - [6.3 ToolTier & ToolTierRegistry](#63-tooltier--tooltierregistry)
   - [6.4 MiningCondition & MiningContext](#64-miningcondition--miningcontext)
   - [6.5 BlockDropProvider](#65-blockdropprovider)
   - [6.6 CustomBlockSound & CustomBlockSoundGroup](#66-customblocksound--customblocksoundgroup)
   - [6.7 Interaction System & CustomBlockActionRegistry](#67-interaction-system--customblockactionregistry)
   - [6.8 CustomBlockRegistry](#68-customblockregistry)
   - [6.9 CustomBlockService](#69-customblockservice)
7. [Vi Du Tich Hop Day Du (Full Integration Examples)](#7-vi-du-tich-hop-day-du-full-integration-examples)

---

## 1. Diem Khoi Dau (API Entrypoint) — HaoHanItemCore

Class Singleton chinh cung cap quyen truy cap vao tat ca cac quan ly con (Registry, Factory, Service) cua plugin.

### Lay Singleton Instance
```java
import vn.haohan.itemcore.api.HaoHanItemCore;

HaoHanItemCore core = HaoHanItemCore.get();
```

### Bang Phuong Thuc trong HaoHanItemCore

| Ten phuong thuc | Kieu du lieu tra ve | Tham so | Mo ta / Tac dung |
| :--- | :--- | :--- | :--- |
| `get()` | `static HaoHanItemCore` | *Khong co* | Lay instance duy nhat cua HaoHanItemCore. Nem IllegalStateException neu plugin chua khoi tao. |
| `getItemRegistry()` | `ItemRegistry` | *Khong co* | Truy cap registry luu tru danh sach dinh nghia item (ItemDefinition). |
| `getItemFactory()` | `ItemFactory` | *Khong co* | Truy cap factory khoi tao ItemStack tu dinh nghia. |
| `getItemService()` | `ItemService` | *Khong co* | Truy cap service tong hop (Facade khuyen nghi dung cho plugin ngoai). |
| `getRecipeRegistry()` | `RecipeRegistry` | *Khong co* | Truy cap registry quan ly cong thuc che tao (RecipeDefinition). |
| `getRecipeService()` | `RecipeService` | *Khong co* | Truy cap service tra cuu va tim kiem cong thuc che tao. |
| `getCustomBlockService()` | `CustomBlockService` | *Khong co* | Truy cap service quan ly Custom Block (Physical Barrier + Client Virtual BlockDisplay). |
| `getToolTierRegistry()` | `ToolTierRegistry` | *Khong co* | Truy cap registry quan ly he thong Tool Tier va multiplier toc do dao. |

---

## 2. He Thong Dinh Nghia Custom Item

### 2.1 Enum ItemType
Phan loai chuc nang chinh cua vat pham custom:
* `MATERIAL`: Nguyen lieu che tao co ban.
* `TOOL`: Cong cu khai thac (cup, riu, xeng,...).
* `WEAPON`: Vu khi tan cong (kiem, no, gay phep,...).
* `ARMOR`: Trang phuc, giap bao ve.
* `FOOD`: Thuc an, duoc pham.
* `MACHINE_COMPONENT`: Linh kien may moc, thiet bi ky thuat.
* `CURRENCY`: Tien te, tien te su kien.
* `BLOCK`: Khai bao vat pham dai dien cho Custom Block.
* `SPECIAL`: Vat pham dac biet/kich hoat su kien.

---

### 2.2 Class ItemDefinition & Builder

ItemDefinition la **Source of Truth** chua toan bo thuoc tinh, thong so, lore, model va component cua mot Custom Item.

#### Bang Phuong Thuc Getter cua ItemDefinition

| Ten phuong thuc | Kieu du lieu tra ve | Tham so | Tac dung |
| :--- | :--- | :--- | :--- |
| `getId()` | `String` | *Khong co* | Lay ID day du dang namespace:key (VD: "lunar:anorthosite_pickaxe"). |
| `getNamespace()` | `String` | *Khong co* | Lay phan namespace truoc dau : (VD: "lunar"). |
| `getKey()` | `String` | *Khong co* | Lay phan key sau dau : (VD: "anorthosite_pickaxe"). |
| `getMaterial()` | `Material` | *Khong co* | Material Minecraft goc cua vat pham (VD: Material.DIAMOND_PICKAXE). |
| `getDisplayName()` | `String` | *Khong co* | Ten hien thi (ho tro ma mau § hoac &). |
| `getMaxStackSize()` | `int` | *Khong co* | So luong xep chong toi da (1 den 99). |
| `getLore()` | `List<String>` | *Khong co* | Danh sach cac dong mo ta co ban cua vat pham. |
| `getCustomModelData()`| `Integer` | *Khong co* | Gia tri CustomModelData (tra ve null neu khong dung). |
| `getItemModel()` | `String` | *Khong co* | Mo hinh 1.21.4+ (NamespacedKey dang String, co the null). |
| `getProperties()` | `Map<String, Object>` | *Khong co* | Map cac thuoc tinh tuy bien di kem dinh nghia. |
| `getType()` | `ItemType` | *Khong co* | The loai phan loai vat pham. |
| `getBehavior()` | `ItemBehavior` | *Khong co* | Doi tuong xu ly hanh vi custom (tra ve null neu khong co). |
| `getComponents()` | `List<ItemComponent>` | *Khong co* | Danh sach cac component chuc nang tuy bien. |
| `getInfoSections()` | `List<ItemInfoSection>` | *Khong co* | Danh sach cac muc lore dinh hinh san theo nhom. |
| `hasBehavior()` | `boolean` | *Khong co* | Kiem tra vat pham co gan doi tuong hanh vi custom khong. |
| `isUsable()` | `boolean` | *Khong co* | Kiem tra xem vat pham co su dung duoc (chuot phai) hay khong. |
| `isValidId(id)` | `static boolean` | `String id` | Kiem tra dinh dang String ID co dung dang namespace:key. |

#### Xay Dung ItemDefinition Bang Builder Pattern

```java
import org.bukkit.Material;
import vn.haohan.itemcore.api.block.ToolType;
import vn.haohan.itemcore.api.item.*;

ItemDefinition customPickaxe = ItemDefinition.builder("lunar:anorthosite_pickaxe")
    .material(Material.DIAMOND_PICKAXE)
    .displayName("§bCup Anorthosite Supercharged")
    .maxStackSize(1)
    .type(ItemType.TOOL)
    .toolType(ToolType.PICKAXE)
    .tier("lunar", 4)
    .miningSpeed(2.5f)
    .addLore("§7Dung de khai thac cac loai quang Mat Trang.")
    .property("max_damage", 1561)
    .infoSection("§eKy nang kich hoat", List.of("§7Chuot phai: Khai thac dien rong 3x3"))
    .build();
```

#### Bang Phuong Thuc Builder cua ItemDefinition

| Ten phuong thuc | Kieu doi so | Gia tri mac dinh | Mo ta |
| :--- | :--- | :--- | :--- |
| `ItemDefinition.builder(id)` | `String` | *Bat buoc* | Tao Builder moi voi ID dinh danh duy nhat dang namespace:key. |
| `.material(material)` | `Material` | `Material.PAPER` | Chat lieu Minecraft goc lam nen tang cho vat pham. |
| `.displayName(displayName)` | `String` | Trung voi `id` | Ten hien thi cua vat pham (ho tro ma mau § hoac &). |
| `.maxStackSize(maxStackSize)` | `int` | `64` | So luong xep chong toi da (1 den 99). |
| `.lore(lore)` | `List<String>` | *List rong* | Thiet lap toan bo danh sach dong mo ta (lore). |
| `.addLore(line)` | `String` | *Khong co* | Them mot dong mo ta moi vao lore. |
| `.customModelData(data)` | `Integer` | `null` | Gia tri CustomModelData cho Resource Pack. |
| `.model(model)` | `String` | `null` | Mo hinh 1.21.4+ (NamespacedKey dang String). |
| `.tier(tier)` | `int` | *Khong co* | Gan cap bac so nguyen cho vat pham. |
| `.tier(name, tier)` | `String, int` | *Khong co* | Gan ten tier va level cho vat pham vao PDC va lore. |
| `.toolType(toolType)` | `ToolType` | *Khong co* | Khai bao loai cong cu (PICKAXE, AXE, SHOVEL, HOE, SWORD, HAND, ANY). |
| `.miningSpeed(speed)` | `float` | *Khong co* | Khai bao bonus toc do dao bo sung cho cong cu. |
| `.property(key, value)` | `String, Object` | *Khong co* | Them mot thuoc tinh tuy bien vao properties. |
| `.properties(properties)` | `Map<String, Object>`| *Map rong* | Thiet lap toan bo Map cac thuoc tinh tuy bien. |
| `.type(type)` | `ItemType` | `ItemType.MATERIAL` | Phan loai the loai vat pham. |
| `.behavior(behavior)` | `ItemBehavior` | `null` | Gan hanh vi xu ly custom logic. |
| `.usable(usable)` | `boolean` | `true` | Thiet lap vat pham co the su dung (chuot phai) hay khong. |
| `.component(component)` | `ItemComponent` | *Khong co* | Them mot component chuc nang tuy bien truc tiep len item. |
| `.components(components)` | `List<ItemComponent>`| *List rong* | Thiet lap danh sach component chuc nang tuy bien. |
| `.infoSection(title, lines)` | `String, List<String>` | *Khong co* | Them mot phan thong tin lore dinh hinh san theo nhom. |
| `.build()` | - | - | Xay dung doi tuong ItemDefinition bat bien. |

#### Cac Property Keys Tu Dong Xu Ly (Built-in Properties)

| Property Key | Kieu du lieu | Tac dung |
| :--- | :--- | :--- |
| `max_damage` | `int` | Thiet lap do ben toi da cua item (Damageable). |
| `jukebox_playable` | `String` (Key) | Dang ky dia nhac jukebox custom (VD: "haohan:space_theme"). |
| `equippable_asset_id` | `String` (Key) | Dang ky model giap hien thi 3D tren nhan vat (VD: "haohan:spacesuit"). |
| `custom_block_id` | `String` (ID) | Gan ID CustomBlock de tu dong dat khoi vat ly Material.BARRIER va kich hoat client virtual BlockDisplay. |
| `custom_block_model` | `String` (BlockState) | Khai bao visual blockstate hien thi tren BlockDisplay (VD: "minecraft:copper_block"). |
| `custom_block_drop` | `String` (ID) | ID custom item se roi ra khi pha/no khoi. |

---

### 2.3 Cac Thanh Phan Lap Trinh (ItemComponent)

Interface `ItemComponent` cho phep mo rong cac tinh nang dong duoc ap dung truc tiep len `ItemStack` khi khoi tao hoac kiem tra.

```java
public interface ItemComponent {
    default void apply(ItemStack item, ItemDefinition definition) {}
    default void appendLore(List<String> lore) {}
}
```

#### AbilityComponent
Gan ten ky nang dac biet vao lore cua vat pham.
* Constructor: `AbilityComponent(String abilityId, String displayName)`

#### AttributeComponent
Them thuoc tinh chi so Paper/Minecraft truc tiep len item (sat thuong, toc do danh, giap,...).
* Constructor:
  - `AttributeComponent(Attribute attribute, double amount)` (mac dinh: ADD_NUMBER, MAINHAND)
  - `AttributeComponent(Attribute attribute, double amount, AttributeModifier.Operation operation, EquipmentSlotGroup slotGroup)`

#### DurabilityComponent
Khai bao do ben custom doc lap voi do ben goc Minecraft. Item se duoc dat thanh `Unbreakable` va an tag vanilla.
* Constructor: `DurabilityComponent(int maxDurability)`

#### MiningComponent
Dinh nghia thong so cap do va toc do khai thac cho cong cu.
* Constructor: `MiningComponent(int miningLevel, float speedMultiplier)`

#### TierComponent
Hien thi cap do vat pham tren lore va luu tru truc tiep vao PDC (tool_tier_level va tool_tier_name).
* Constructor: `TierComponent(int tier)` hoac `TierComponent(String tierName, int tier)`

---

### 2.4 Cau Truc Lore Nang Cao (ItemInfoSection)

`ItemInfoSection` (Record) giup nhom cac dong lore theo tieu de mot cach chuan hoa va tham my.
* Constructor: `ItemInfoSection(String title, List<String> lines)`

---

### 2.5 Du Lieu Tung Vat Pham (ItemInstanceData)

Quan ly **trang thai dong rieng biet cua tung vat pham cu the** luu giu trong `PersistentDataContainer` (PDC).

| Ten phuong thuc | Kieu du lieu tra ve | Tham so | Tac dung |
| :--- | :--- | :--- | :--- |
| `durability(item, defaultVal)` | `int` | `ItemStack item`, `int defaultValue` | Doc do ben custom con lai cua vat pham. |
| `setDurability(item, value)` | `void` | `ItemStack item`, `int value` | Ghi do ben custom moi vao PDC cua vat pham. |
| `upgradeLevel(item)` | `int` | `ItemStack item` | Doc cap do cuong hoa/nang cap cua vat pham (mac dinh 0). |
| `setUpgradeLevel(item, level)`| `void` | `ItemStack item`, `int level` | Ghi cap do cuong hoa/nang cap moi vao PDC. |

---

## 3. Registry & Services Quan Ly Item

### 3.1 ItemRegistry
Luu tru va tra cuu toan bo `ItemDefinition`. Ho tro `register`, `get`, `require`, `exists`, `unregister`, `all`, `getByNamespace`, `search`, `size`, `clear`.

### 3.2 ItemFactory
Khoi tao `ItemStack` chuan tu `ItemDefinition` hoac `id` voi day du metadata, lore va Random UUID (neu `maxStackSize == 1`).

### 3.3 ItemService (Facade Chinh)
Cung cap cac ham tien ich: `create`, `isItem`, `isCustomItem`, `getId`, `getDefinition`, `exists`, `getProperties`, `getInstanceData`, `validateAndUpdate`.

---

## 4. He Thong Hanh Vi Custom (ItemBehavior & ItemContext)

Giao dien `ItemBehavior` giup gan logic tuong tac truc tiep cho Custom Item:
- `onUse(ItemContext context)`: Chuot phai vat pham.
- `onInteract(ItemContext context)`: Chuot trai tuong tac vat pham.
- `onBreak(ItemContext context)`: Khi player pha khoi bang vat pham.
- `onCraft(ItemContext context)`: Khi vat pham duoc che tao.
- `onInventoryClick(ItemContext context)`: Khi click trong inventory/GUI.
- `onDrop(ItemContext context)`: Khi vut vat pham ra dat.
- `onPickup(ItemContext context)`: Khi nhat vat pham tu duoi dat.

---

## 5. He Thong Recipe (Cong Thuc Che Tao)

Ho tro cac kieu cong thuc: `SHAPED`, `SHAPELESS`, `SMELTING`, `BLASTING`, `SMOKING`, `CAMPFIRE`, `STONECUTTING`, `SMITHING`, `MACHINE`.
Nguyen lieu ho tro `Ingredient.ItemIngredient`, `Ingredient.MaterialIngredient`, `Ingredient.TagIngredient`.
Dau ra tra ve `ItemResult`.

---

## 6. He Thong Custom Block (Barrier + BlockDisplay Engine)

He thong Custom Block the he moi loai bo hoan toan NoteBlock, su dung khoi vat ly **Material.BARRIER** ket hop voi ao hoa hien thi **Client-side Virtual BlockDisplay** thong qua PacketEvents.

### 6.1 CustomBlockDefinition & Builder

Dinh nghia bat bien chua toan bo thong so khoi, do cung, cong cu dao phu hop, am thanh, roi do va tuong tac.

```java
CustomBlockModel model = new CustomBlockModel(Bukkit.createBlockData(Material.COPPER_BLOCK));

CustomBlockDefinition def = CustomBlockDefinition.builder("haohan:copper_casing")
    .model(model)
    .hardness(3.5f)
    .toolType(ToolType.PICKAXE)
    .miningCondition(MiningCondition.requiresTool(ToolType.PICKAXE, ToolTier.IRON))
    .dropProvider(BlockDropProvider.builder()
        .defaultDrop("haohan:copper_casing", 1)
        .silkTouchDrop("haohan:copper_casing")
        .fortune(true)
        .build())
    .soundGroup(CustomBlockSoundGroup.COPPER)
    .build();
```

#### Bang Phuong Thuc Builder cua CustomBlockDefinition

| Ten phuong thuc | Kieu doi so | Mo ta |
| :--- | :--- | :--- |
| `CustomBlockDefinition.builder(id)` | `String` | Khoi tao builder voi ID duy nhat. |
| `.model(model)` | `CustomBlockModel` | Thiet lap model BlockDisplay hien thi client-side. |
| `.hardness(hardness)` | `float` | Thiet lap do cung cua khoi (thoi gian dao co ban tinh bang giay). |
| `.instantBreak()` | - | Khoi bi pha ngay lap tuc khi cham vao (hardness = 0.0f). |
| `.unbreakable()` | - | Khoi khong the bi pha huy boi nguoi choi (hardness = -1.0f). |
| `.toolType(toolType)` | `ToolType` | Khai bao cong cu hieu qua (PICKAXE, AXE, SHOVEL, HOE, SWORD, HAND, ANY). |
| `.preferredTools(types)` | `ToolType...` | Khai bao danh sach cac loai cong cu hieu qua. |
| `.miningCondition(condition)`| `MiningCondition` | Dieu kien de duoc tinh la dao hop le va duoc roi do. |
| `.dropProvider(dropProvider)` | `BlockDropProvider`| Bo tinh toan roi do (ho tro Silk Touch va Fortune). |
| `.dropItemId(dropItemId)` | `String` | ID custom item roi ra don gian (mac dinh roi chinh item dat neu null). |
| `.soundGroup(soundGroup)` | `CustomBlockSoundGroup`| Nhom am thanh cua khoi (place, break, hit, step, fall). |
| `.interactHandler(handler)` | `CustomBlockInteractHandler` | Handler xu ly khi nguoi choi click chuot phai vao khoi. |
| `.stepHandler(handler)` | `CustomBlockStepHandler` | Handler xu ly khi nguoi choi buoc len tren khoi. |
| `.actionTrigger(event, actions)`| `String, List<Map<String,Object>>` | Khai bao danh sach trigger hanh dong tu YAML. |
| `.property(key, value)` | `String, Object` | Gan thuoc tinh tuy bien bo sung. |
| `.properties(map)` | `Map<String, Object>` | Gan map thuoc tinh tuy bien bo sung. |
| `.build()` | - | Xay dung doi tuong CustomBlockDefinition bat bien. |

---

### 6.2 CustomBlockModel

Xac dinh blockstate hoac material hien thi cua client-side virtual BlockDisplay. Tu dong ap dung scale chuan **1.00005f** va translation offset **-0.000025f** de triet tieu hoan toan z-fighting.

```java
// Khoi tao tu Bukkit BlockData
CustomBlockModel model1 = new CustomBlockModel(Bukkit.createBlockData(Material.COPPER_BLOCK));

// Hoac khoi tao nhanh qua Material
CustomBlockModel model2 = CustomBlockModel.of(Material.GOLD_BLOCK);
```

---

### 6.3 ToolTier & ToolTierRegistry

He thong quan ly cap bac cong cu va he so nhan toc do dao. Ho tro ca tier vanilla va custom tier khai bao qua file YAML (`tiers.yml`).

#### Enum ToolTier Built-in
`WOOD` (2.0x, level 0), `STONE` (4.0x, level 1), `IRON` (6.0x, level 2), `DIAMOND` (8.0x, level 3), `NETHERITE` (9.0x, level 4), `GOLD` (12.0x, level 0).

#### Bang Phuong Thuc trong ToolTierRegistry

| Ten phuong thuc | Kieu tra ve | Tham so | Mo ta |
| :--- | :--- | :--- | :--- |
| `register(name, tier)` | `void` | `String name, ToolTier tier` | Dang ky custom ToolTier vao he thong. |
| `get(name)` | `ToolTier` | `String name` | Lay ToolTier theo ten (khong phan biet hoa thuong). |
| `findTier(item)` | `ToolTier` | `ItemStack item` | Tu dong nhan dien ToolTier cua item qua PDC, custom definition hoac vanilla material. |
| `all()` | `Map<String, ToolTier>` | *Khong co* | Lay toan bo danh sach cac tier da dang ky. |

---

### 6.4 MiningCondition & MiningContext

Dieu kien xac dinh xem nguoi choi co du dieu kien dao khoi va duoc phep nhan item roi ra hay khong.

```java
// Yeu cau toi thieu cup Sat
MiningCondition cond1 = MiningCondition.requiresTool(ToolType.PICKAXE, ToolTier.IRON);

// Yeu cau loai cong cu bat ky (hoac riu)
MiningCondition cond2 = MiningCondition.requiresToolType(ToolType.AXE);

// Luon hop le (moi cong cu va tay khong deu roi do)
MiningCondition cond3 = MiningCondition.ALWAYS_TRUE;
```

---

### 6.5 BlockDropProvider

Bo quan ly roi do khi pha khoi, ho tro drop mac dinh, drop khi co phu phep Silk Touch, va nhan so luong theo cap phu phep Fortune.

```java
BlockDropProvider dropProvider = BlockDropProvider.builder()
    .defaultDrop("haohan:ruby", 1)
    .silkTouchDrop("haohan:ruby_ore")
    .fortune(true) // Tu dong tinh toan he so Fortune vanilla
    .build();

// Tinh toan vat pham roi ra thuc te
List<ItemStack> drops = dropProvider.calculateDrops(miningContext);
```

---

### 6.6 CustomBlockSound & CustomBlockSoundGroup

He thong quan ly am thanh chat luong cao cho Custom Block. Tu dong chan am thanh chat lieu Barrier vanilla va phat am thanh tuy chinh khi dat khoi, buoc di, go nhe, va pha khoi.

```java
// Su dung cac nhom am thanh co san
CustomBlockSoundGroup group1 = CustomBlockSoundGroup.STONE;
CustomBlockSoundGroup group2 = CustomBlockSoundGroup.WOOD;
CustomBlockSoundGroup group3 = CustomBlockSoundGroup.DEEPSLATE;
CustomBlockSoundGroup group4 = CustomBlockSoundGroup.COPPER;

// Hoac tu tao nhom am thanh rieng
CustomBlockSoundGroup customGroup = CustomBlockSoundGroup.builder()
    .place(new CustomBlockSound("haohan:block.crystal.place", 1.0f, 1.0f))
    .breakSound(new CustomBlockSound("haohan:block.crystal.break", 1.0f, 1.0f))
    .hit(new CustomBlockSound("haohan:block.crystal.hit", 0.5f, 1.2f))
    .step(new CustomBlockSound("haohan:block.crystal.step", 0.3f, 1.0f))
    .build();
```

---

### 6.7 Interaction System & CustomBlockActionRegistry

Cung cap kha nang xu ly tuong tac linh hoat khi nguoi choi click chuot phai (`CustomBlockInteractHandler`) hoac di chuyen len tren khoi (`CustomBlockStepHandler`).

```java
// Gan truc tiep qua Java Handler
CustomBlockDefinition def = CustomBlockDefinition.builder("haohan:healing_pad")
    .model(CustomBlockModel.of(Material.EMERALD_BLOCK))
    .stepHandler(ctx -> {
        ctx.player().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1));
    })
    .interactHandler(ctx -> {
        ctx.player().sendMessage("§aBan da kich hoat Healing Pad!");
    })
    .build();
```

---

### 6.8 CustomBlockRegistry

Registry quan ly tap trung toan bo dinh nghia custom block da dang ky tren server. Thread-safe 100%.

| Ten phuong thuc | Kieu tra ve | Tham so | Mo ta |
| :--- | :--- | :--- | :--- |
| `register(def)` | `void` | `CustomBlockDefinition def` | Dang ky dinh nghia custom block. |
| `get(id)` | `Optional<CustomBlockDefinition>` | `String id` | Tra cuu dinh nghia custom block theo ID. |
| `unregister(id)` | `void` | `String id` | Huy dang ky custom block. |
| `all()` | `Collection<CustomBlockDefinition>` | *Khong co* | Lay toan bo danh sach dinh nghia custom block. |
| `exists(id)` | `boolean` | `String id` | Kiem tra ID custom block da dang ky chua. |

---

### 6.9 CustomBlockService

Facade API cap cao tuong tac voi the gioi game.

| Phuong thuc | Kieu tra ve | Tham so | Mo ta / Tac dung |
| :--- | :--- | :--- | :--- |
| `isCustomBlock(block)` | `boolean` | `Block block` | Kiem tra xem khoi tai block co phai la custom block khong (O(1)). |
| `getCustomBlockId(block)` | `String` | `Block block` | Lay ID dinh danh cua custom block tai khoi chi dinh. |
| `getDefinition(block)` | `Optional<CustomBlockDefinition>` | `Block block` | Lay CustomBlockDefinition cua custom block tai vi tri. |
| `placeBlock(location, id)` | `boolean` | `Location loc, String id` | Dat khoi vat ly BARRIER, luu tru du lieu PDC va spawn BlockDisplay toi client. |
| `removeBlock(location)` | `boolean` | `Location loc` | Pha bo custom block, go bo hien thi tren client va don dep luu tru. |
| `getRegistry()` | `CustomBlockRegistry` | *Khong co* | Lay registry quan ly dinh nghia custom block. |
| `getActionRegistry()` | `CustomBlockActionRegistry` | *Khong co* | Lay registry quan ly cac action trigger tuy bien. |

---

## 7. Vi Du Tich Hop Day Du (Full Integration Examples)

### Vi Du 1: Dang Ky Custom Block Day Du Voi Am Thanh, Roi Do Va Tuong Tac

```java
package vn.haohan.myplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import vn.haohan.itemcore.api.HaoHanItemCore;
import vn.haohan.itemcore.api.block.*;

public class MyBlockPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        var core = HaoHanItemCore.get();
        var blockRegistry = core.getCustomBlockService().getRegistry();

        // 1. Tao Custom Block Definition
        CustomBlockModel model = new CustomBlockModel(Bukkit.createBlockData(Material.DEEPSLATE_REDSTONE_ORE));

        CustomBlockDefinition rubyOre = CustomBlockDefinition.builder("haohan:ruby_ore")
                .model(model)
                .hardness(4.5f)
                .toolType(ToolType.PICKAXE)
                .miningCondition(MiningCondition.requiresTool(ToolType.PICKAXE, ToolTier.IRON))
                .dropProvider(BlockDropProvider.builder()
                        .defaultDrop("haohan:ruby", 1)
                        .silkTouchDrop("haohan:ruby_ore")
                        .fortune(true)
                        .build())
                .soundGroup(CustomBlockSoundGroup.DEEPSLATE)
                .interactHandler(ctx -> {
                    ctx.player().sendMessage("§cQuang Ruby toa ra mot luong nhiet nhe khi ban cham vao.");
                })
                .build();

        blockRegistry.register(rubyOre);
        getLogger().info("Da dang ky Custom Block Ruby Ore thanh cong!");
    }
}
```

### Vi Du 2: Xu Ly Su Kien Lang Nghe & Kiem Tra Vat Pham Trong Game

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

        // Kiem tra xem vat pham co phai la "haohan:space_helmet" khong
        if (itemService.isItem(item, "haohan:space_helmet")) {
            // Tu dong kiem tra va nang cap component neu config vua duoc reload
            ItemStack updatedItem = itemService.validateAndUpdate(item);
            
            player.sendMessage("§aBan dang cam Mu Phi Hanh Gia chuan!");
        }
    }
}
```
