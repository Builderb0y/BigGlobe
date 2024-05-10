Almost anywhere you can specify a script in json with a string or array of strings, you can also use an object with the following properties:

* `debug_name` - a string representing additional text to insert into the generated script's class name. This text will show up in debuggers and profilers like visualVM.
* `source` - the script's source code. Identical to the string or array of strings you replaced with an object. Only intended to be used when `debug_name` is specified.
* `template` - the name of a template file. For example, if you have a template at `/data/(modid)/bigglobe_script_templates/(name).json` then the template string you specify here would be `(modid):(name)`.
* `inputs` - another object mapping input names (as specified in the template file) to snippets of code which can compute the value associated with that name.

When using an object like this, you must specify EITHER source or template, but not both.

# Template file format

* `script` - a string or array of strings containing the script's source code. You cannot replace this with an object.
* `inputs` - a list of objects specifying any inputs the template needs to function. Inputs are provided by the script using this template. The objects in this list contain the following properties:
	* `name` - the name of the input, and the name of the variable which will be exposed to the template's script.
	* `type` - a string representing the type of the variable. The type must be known to the current script environment, and the current script environment is determined by the script using this template.

All inputs for the script will be exposed to the script as 2 different variables:

* The first way matches the name of the input. This version is evaluated once, as the first thing the script does. Inputs referenced like this are computed in the order they are declared in the template file.
* The second way is prefixed with a dollar sign, and since a dollar sign is not a valid identifier character, you will also need to surround it with tick marks. This version is re-computed every time it's used, which can be useful if it uses random numbers to choose a value.

# Example

Say you want a template for a scripted configured feature which places a sphere of some type of block with some radius. The template file for that would look like this:

```json
{
	"script": [
		"for (int offsetX, int offsetY, int offsetZ in range(floorInt(-RADIUS), ceilInt(RADIUS)):",
			"if (offsetX ^ 2 + offsetY ^ 2 + offsetZ ^ 2 < RADIUS ^ 2:",
				"setBlockState(originX + offsetX, originY + offsetY, originZ + offsetZ, `$STATE`)",
			")",
		")",
		"true"
	],
	"inputs": [
		{ "name": "RADIUS", "type": "double" },
		{ "name": "STATE", "type": "BlockState" }
	]
}
```

A configured feature which then wants to place a sphere whose radius is between 2 and 4 out of a mixture of cobblestone and mossy cobblestone would look like this:

```json
{
	"type": "bigglobe:script",
	"config": {
		"script": {
			"template": "modid:sphere",
			"inputs": {
				"RADIUS": "random.nextDouble(2.0L, 4.0L)",
				"STATE": "random.if ('minecraft:cobblestone') else ('minecraft:mossy_cobblestone')"
			}
		}
	}
}
```