# Fields

* `craftingGrid.width` - the number of columns in the crafting grid.
* `craftingGrid.height` - the number of rows in the crafting grid.

# Methods

* `craftingGrid.get(x, y)` - returns the ItemStack in this slot of the crafting grid.
* `craftingGrid.set(x, y, ItemStack)` - sets the ItemStack in this slot of the crafting grid and returns the ItemStack which used to be in that slot before the method was called.

# Types

* `CraftingGrid` - represents an inventory where items can be used for crafting. This type implements List, so anything you can do with a List, you can also do with a CraftingGrid. Note however that CraftingGrid will automatically convert null ItemStack's to empty ItemStack's when calling get() or set().