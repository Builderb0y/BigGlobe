New in V4.3.3: World traits allow chunk generators to share information with scripts. Every trait represents a "generic" value that multiple chunk generators of the same theme could have. Scripts can reference the trait instead of the column value it came from, and magically work in multiple different chunk generators or world presets. World traits behave a bit like column values, and there is lots of overlap between the two. The main difference is that traits are per-generator, where as column values apply to all worlds.

Example: The default Big Globe overworld has a specific Y level for the surface at every given position. However, some data packs may want to use different logic for how this Y level is computed. Depending on what the data pack is doing, it might not be beneficial to override the column value which controls this directly. If the data pack author instead wishes to use a different column value for the surface Y level, then they are left with the annoying task of updating all the feature dispatchers, configured features, and other data pack files to reflect the column value they want to represent the surface Y level. This is where traits come in: they can tell other scripts which column value controls the surface Y level, so that the data pack doesn't need to edit 50+ different files.

Many column values already have an associated trait for use in the WIP experimental "islands" world preset, and more traits for other column values might be added in the future.

# Json structure

* `type` - a ColumnValueType that describes the type that this trait represents.
* `is_3d` - true if this trait could have a different value depending on the Y level, false otherwise.
* `default` (optional) - a script which specifies how to obtain the value associated with this trait if the active chunk generator does NOT override it. This script has the following environments available:
	* MathScriptEnvironment
	* StatelessRandomScriptEnvironment
	* MinecraftScriptEnvironment
	* BaseColumnScriptEnvironment
	* ColorScriptEnvironment
	* ColumnEntryRegistry

	Additionally, if `is_3d` is set to true, a variable named `y` of type `int` is also available. This variable indicates the Y level being requested.