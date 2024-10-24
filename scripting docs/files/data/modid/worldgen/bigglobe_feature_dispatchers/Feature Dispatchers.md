Feature Dispatchers are responsible for decorating chunks as they're being generated. They have the following properties:

* `type` - can be one of:
	* `group` - delegates to some other dispatchers. This type has the following property:
		* `dispatchers` - an array of strings representing the namespace and path of other feature dispatchers to delegate to. They will be run in the order they are declared in this array.
	* `tag` (upcoming) - similar to the `group` type, but instead of declaring the child dispatchers inside the parent dispatcher, the parent dispatcher loads its children from a tag. The order which child dispatchers run in is sorted alphabetically, first by path, then by namespace. This type has the following additional property:
		* `tag` - the tag to load children from, without a `#` prefix.
	* `script` - uses a script to place other features or blocks. This type has the following properties:
		* `script` - the script which places other features or blocks. This script has the following environments present:
			* JavaUtilScriptEnvironment
			* MathScriptEnvironment
			* MinecraftScriptEnvironment (with world)
			* CoordinatorScriptEnvironment
			* NbtScriptEnvironment
			* RandomScriptEnvironment (with random)
			* StatelessRandomScriptEnvironment
			* GridScriptEnvironment (with implicit seed)
			* StructureTemplateScriptEnvironment
			* ColumnEntryRegistry
			* ColorScriptEnvironment
			* ExternalImageScriptEnvironment
			* ExternalDataScriptEnvironment

			And the following variables:
			* `minModifiableX/Y/Z`, `maxModifiableX/Y/Z` - the bounds of the chunk being generated. Note that these coords are inclusive, not exclusive.
			* `minAccessibleX/Y/Z`, `maxAccessibleX/Y/Z` - the bounds of the area which you can query blocks in. For raw dispatchers, this matches the area of the chunk being generated. For normal dispatchers, this matches a 3x3 chunk area surrounding the chunk being generated. Like the modifiable coords, these coords are inclusive too.
			* `distantHorizons` - true if this chunk is being generated for distant horizons or voxy, false for normal chunks used by normal minecraft. This can be used to skip features that won't be visible at a distance. Note: at the time of writing this, feature generation does not happen for voxy chunks, but this may change in the future.