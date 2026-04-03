# After V4.4.0

This file has the following structure:
* (key) - the namespace and path of a trait.
* (value) - a specification for how to compute the value of this trait, and (optionally) how to set it from an overrider. The value can take one of two different forms:
	* The short way: if the value is a string or list of strings, then it is treated as a script which dictates how to get the value associated with this trait.
	* The long way: if the value is another map, then it may contain the following properties:
		* `get` - a script that dictates how to get the value associated with this trait. Basically the short way goes here.
		* `set` (optional) - a script that dictates how to set the value associated with this trait, for overriders.

	In any case, the scripts have the following environments available:
	* MathScriptEnvironment
	* StatelessRandomScriptEnvironment
	* MinecraftScriptEnvironment
	* BaseColumnScriptEnvironment
	* ColumnEntryRegistry

	If the trait is 3D, then a variable named `y` of type `int` is also available, which contains the Y level being requested.

	If the script is for the `set` property, then a variable named `value` of the same type as the trait is also available, which represents the value to be assigned to whatever underlying column value this trait delegates to.

Scripted chunk generators have a "world_traits" property which points to this file. If multiple data packs provide this file, they will be merged at runtime. If multiple data packs provide the same trait within the file, the top data pack takes priority.

# Before V4.4.0

The contents of this file were embedded into the "world_traits" property in the scripted chunk generator. This made it impossible to extend with data packs without replacing the chunk generator.