Configured features are found in `/data/(modid)/worldgen/configured_feature` when `type` is set to `bigglobe:script`.

# Json structure

`config > rotate_randomly` can be set to true or false (defaults to false) to rotate the configured feature randomly on placement. This works by spoofing which blocks are at which coordinates when the script tries to query them, and by modifying the coordinates the script tries to place blocks at.

`config > flip_randomly` can be set to true or false (defaults to false) to flip the configured feature randomly along the x axis or the z axis. Or both. Or neither. It's random. This value stacks with `config > rotate_randomly`. When both are true, there are 8 possible ways the structure could be rotated or flipped. This corresponds to the [dihedral group of order 4](https://en.wikipedia.org/wiki/Dihedral_group).

# The script

`config > script` has the following environments applied:

* JavaUtilScriptEnvironment (with random)
* MathScriptEnvironment
* MinecraftScriptEnvironment (with world)
* CoordinatorScriptEnvironment (with world)
* NbtScriptEnvironment
* RandomScriptEnvironment (with random)
* StatelessRandomScriptEnvironment
* GridScriptEnvironment (with implicit seed)
* StructureTemplateScriptEnvironment (with world)
* ColumnEntryRegistry (x, y, and z default to originX, originY, and originZ)
* ColorScriptEnvironment
* ExternalImageScriptEnvironment
* ExternalDataScriptEnvironment

## Additional variables

* `int originX`, `originY`, and `originZ` - the x, y, and z coordinates that this configured feature was placed at.
* `boolean distantHorizons` true if we are currently generating terrain for distant horizons fake chunks, false otherwise. This can be used to skip details that aren't visible from a distance.

## Additional functions

* `void finish()` stops the script immediately, leaving behind any blocks which have already been placed.
* `void abort()` stops the script immediately, and does not place any blocks which have been queued. This function is only useful when `config > queue` is set to `basic` or `delayed`. If queue is set to `none`, then any blocks which have already been placed are left behind.