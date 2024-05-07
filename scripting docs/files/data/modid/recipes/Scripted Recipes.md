Scripted recipes allow you to use code to add a recipe with non-trivial inputs and outputs. Right now, this only works for crafting recipes, not smelting/stonecutting/smithing/etc.

# Properties

* `type` - must be "bigglobe:scripted" for this recipe to count as a scripted recipe.
* `category` - must be "building", "redstone", "equipment", or "misc", just like vanilla.
* `width` - the minimum number of columns that the recipe will fit in. 
* `height` - the minimum number of rows that the recipe will fit in.

	The vanilla crafting table has 3 rows and 3 columns, but the crafting grid in the player's inventory only has 2 rows and 2 columns. If your scripted recipe has a width or height greater than 2, you will not be able to make it in the crafting grid in the player's inventory, even if the recipe requires 4 or fewer items.
* `matches` - a script which returns true or false based on whether or not all the ingredients of this recipe are present in the crafting grid, and are in the right places.
* `output` - a script which returns the ItemStack which should be crafted by the ingredients in `input`.
* `remainder` (optional) - a script which determines which items should stay in the crafting grid after the recipe has been crafted. For example, if your recipe takes a bucket of water, you might want to leave the bucket itself behind after crafting the recipe.

All 3 of the above scripts have the following script environments present:
* JavaUtilScriptEnvironment
* NbtScriptEnvironment
* ItemScriptEnvironment
* CraftingGridScriptEnvironment

Additionally, the following variables are also available:
* `input` - a CraftingGrid which holds the items currently in the crafting table. Note that the input cannot be modified by scripts. If you try to call set() on it, your script will terminate with an exception, and an error message will be logged to your game console.
* `output` - only for `remainder`, this is an (initially) empty CraftingGrid for you to put leftover items in. This CraftingGrid can be modified however you want.