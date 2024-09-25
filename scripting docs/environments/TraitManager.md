This environment is automatically available to any script which provides the ColumnEntryRegistry environment.

This environment is data-driven, so all the fields and methods it provides to scripts are defined by you! This is done by adding json files to the `/data/(modid)/worldgen/bigglobe_world_traits` directory. All files in this directory are expected to be formatted as a world trait. Additionally, all files in this directory are exposed to scripts as methods (and sometimes fields).

# The `world_traits` variable

`world_traits` is a variable which indicates that the following field or method is the name of a world trait. For example, all of Big Globe's chunk generators have a world trait named `bigglobe:biome`. To access this trait, simply do
```
world_traits.`bigglobe:biome`(x, y, z)
```

If a trait is 2D, then the Y level is not relevant and must be omitted. Similarly, if the trait cannot be accessed at arbitrary positions from the current context, then x and z must be omitted. In general, the coordinates you provide depend on their count.
* If you specify 1 argument, that argument is assumed to be the Y level.
* If you specify 2 arguments, those arguments are assumed to be X and Z coordinates.
* If you specify 3 arguments, those arguments are assumed to be X, Y, and Z, in that order.
* If you specify 0 arguments (or try to access the trait as a field instead of a method) then it is assumed that you're not specifying a coordinate.

You will get a compile-time error if the coordinates you provide do not align with what is needed in the current context.

## A subtle point

world_traits cannot be renamed or stored in a variable. Attempting to do so will make it unable to do its job.
```
var traits = world_traits ;technically works.
var biome = traits.`bigglobe:biome`(x, y, z) ;does not work.
```
If you know a bit of java and are curious why this restriction is currently in place, the short answer is that `world_traits` behaves more like a class name than a variable name, and the traits themselves behave more like static methods than object methods. This isn't actually the case internally, but it's a useful way to think about things for the purpose of scripting. The *actual* WorldTraits object is actually stored on the ScriptedColumn itself. A simple call like
```
world_traits.`bigglobe:biome`(y)
```
compiles into
```java
column.worldTraits().get_bigglobe_biome(column, y)
```
A more complicated call involving x and z guarantees that x, y, and z will be evaluated exactly once, and in that exact order.