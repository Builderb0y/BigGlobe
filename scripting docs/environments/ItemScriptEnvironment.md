# Variables

# Fields

* `item.id` - a String formatted as namespace:path which represents the item.
* `ItemStack.EMPTY` - a non-null, but empty, ItemStack. An empty stack is one which has a quantity of 0, or which has an item of "minecraft:air".
* `itemStack.item` - returns the Item associated with this ItemStack.
* `itemStack.empty` - returns true if this stack is empty, false otherwise.
* `itemStack.maxCount` - the maximum quantity of items that this type allows. Most items will allow stack quantities up to 64, but eggs and ender pearls only allow 16. Tools and weapons only allow 1.
* `itemStack.stackable` - true if the maxCount is greater than 1, false otherwise.
* `itemStack.count` - the quantity of the ItemStack.
* `itemStack.damage` - if this ItemStack has durability, then this field is the number of durability points that the item has received so far. It is NOT the number of durability points remaining before the item breaks. If this ItemStack does not have durability, then the damage is always 0.
* `itemStack.maxDamage` - if this ItemStack has durability, then this field is the number of durability points the item has initially. maxDamage - damage is the amount of remaining durability on the item. If the item does not have durability, then the maxDamage is 0.
* `itemStack.damageable` - true if this ItemStack has durability, false otherwise.
* `itemStack.nbt` - converts the ItemStack to NBT data. The returned NBT data will have an "id" property which represents the Item's id, and a "count" property which represents the stack's quantity. It may also contain other data depending on the item in question. **Modifying the NBT data will have no effect on the ItemStack it came from!** The NBT data is converted each time this field is used, so for performance reasons, it's best not to use it more times than necessary. In other words, replace
	```
	String id = itemStack.nbt.id
	int count = itemStack.nbt.count.asInt()
	```
	with
	```
	NbtCompound nbt = itemStack.nbt
	String id = nbt.id
	int count = nbt.count.asInt()
	```

# Functions

# Methods

* `ItemStack.new(Item)` - creates a new ItemStack with the specified Item, with a quantity of 1, and no NBT data.
* `ItemStack.new(Item item, int quantity)` - creates a new ItemStack with the specified item an quantity, with no NBT data.
* `ItemStack.new(Item item, NbtCompound nbt)` - creates a new ItemStack with the specified item and NBT data. If the NBT data contains a "count" property, then this determines the quantity of the returned ItemStack. Otherwise, the quantity is 1. Also, the provided item takes priority over the "id" property in the NBT data, if present.
* `ItemStack.new(Item item, int count, NbtCompound nbt)` - creates a new ItemStack with the specified item, count, and NBT data. The provided item and count parameters take priority over the "id" and "count" properties in the NBT data, if present.
* `ItemStack.new(NbtCompound)` - creates a new ItemStack with the provided NBT data, which is expected to have an "id" property. If the NBT data has a "count" property, then this determines the quantity of the returned ItemStack. Otherwise, the quantity is 1.
* `Item.isIn(ItemTag)` - returns true if the item is in the provided tag, false otherwise.
* `Item.getDefaultStack()` - returns an ItemStack containing the item. Some of Minecraft's items may return an ItemStack with NBT data on it, but most won't.
* `ItemTag.random(Random)` - returns a random Item in this tag chosen by the provided Random.
* `ItemTag.random(long seed)` - returns a random Item in this tag which depends on the provided seed.

# Keywords

# Member keywords

# Types

* `Item` - a type of item. For example, a sword.
* `ItemStack` an item with more information on it about its current state. This might include quantity or NBT data. For example, 5 sticks, or an enchanted sword.
* `ItemTag` a tag containing items which are usually related in some way.

# Casting

* `String -> Item` - use the namespace and path of the item. For example, `Item stick = 'minecraft:stick'`.
* `String -> ItemTag` - use the namespace and path of the item tag. For example, `ItemTag beaconPayment = 'minecraft:beacon_payment_items'`.

# Notes