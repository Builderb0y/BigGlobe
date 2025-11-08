# This file contains documentation on an UPCOMING feature intended for Big Globe 6.0.0. It is not fully finalized yet, and may change before then.

An element is the common type that includes custom classes and their various member types. All elements begin with an `"element_type"`. This determines the schema for the rest of the file. It can take on the following values:
* When `element_type` is `class/builtin`, the following property is available:
	* `java_type` - the name of the represented type in java. May be one of:
		* `array_deque`
		* `array_list`
		* `biome`
		* `biome_tag`
		* `block`
		* `block_state`
		* `block_tag`
		* `boolean`
		* `byte`
		* `collection`
		* `configured_feature`
		* `configured_feature_tag`
		* `constant_list`
		* `constant_map`
		* `constant_set`
		* `deque`
		* `double`
		* `float`
		* `hash_map`
		* `hash_set`
		* `int`
		* `iterable`
		* `iterator`
		* `linked_hash_map`
		* `linked_hash_set`
		* `linked_list`
		* `list`
		* `list_iterator`
		* `long`
		* `map`
		* `map_entry`
		* `navigable_map`
		* `navigable_set`
		* `priority_queue`
		* `queue`
		* `random_array_list`
		* `random_list`
		* `set`
		* `short`
		* `sorted_map`
		* `sorted_set`
		* `tag`
		* `tree_map`
		* `tree_set`
		* `void`
		* `wood_palette`
		* `wood_palette_tag`
	* All of these types are, as the name would imply, built-in, and defined for you in the root of the `bigglobe` namespace. Since the ElementSpec registry also defaults to the `bigglobe` namespace, these builtin types can be referenced by their short name (like `int`) instead of their long name (`bigglobe:int`).
* When `element_type` is `class/normal` or `class/voronoi`, the following additional properties are available:
	* `name` - the name that will be exposed to scripts to refer to this class. Note that when a reference to an ElementSpec is required, it still requires the full name of the file, not this short name.
	* `abstract` - a boolean indicating whether this class contains or inherits abstract members. If this class contains or inherits abstract members, but `abstract` is set to false, an data pack validation error will be thrown. If `abstract` is set to true, but this class does NOT declare or inherit any abstract members, a warning is logged. This property defaults to `false`.
	* `extends` - a reference to another ElementSpec to inherit the members of. The referenced ElementSpec must be of type `class/...`.
	* `members` - a tag containing the ElementSpec's which this class contains.
* When `element_type` is `field/normal`, the following additional properties are available:
	* `name` - the name that will be exposed to scripts to refer to this field. When a script is used for a method or similar that is contained by a class which contains or inherits this field, it will be exposed as a variable in addition to as a field.
	* `field_type` - a reference to another ElementSpec describing the type of data held by this field. The referenced ElementSpec must be of type `class/...`.
	* `default` - a script which returns the default value of this field. Constructors (explained below) may later override this value.
* When `element_type` is `constructor/normal`, the following addition properties are available:
	* `name` - the name that will be exposed to scripts to invoke this constructor. Defaults to `new`.
	* `values` - a list of strings, where each string is expected to be the shorthand name of EITHER a field or a property to assign this value to. If this value represents a property, it must be settable.
	* `code` - additional code to run after this constructor stores its values in the relevant fields and/or properties, but before the object is returned to the caller.
* When `element_type` is `method/normal`, the following additional properties are available:
	* `name` - the name that will be exposed to scripts to invoke this method. If the script is used by a class which contains or inherits this method, it will be exposed as a function in addition to a method.
	* `return_type` - a reference to an ElementSpec describing what type of data this method returns. The referenced ElementSpec must be of type `class/...`.
	* `parameters` - a list of parameters that this method takes as inputs. A single parameter has the following properties:
		* `name` - the name exposed to scripts (as a variable) to reference the value of this parameter.
		* `type` - a reference to an ElementSpec describing what type of data is expected for this parameter. The referenced ElementSpec must be of type `class/...`.
	* `code` - a script which performs this method's job, and returns its result.
* When `element_type` is `method/override`, the following additional properties are available:
	* `override` - a reference to an ElementSpec representing the method to be overridden. The referenced ElementSpec must be of type `method/...`.
	* `code` - a script which performs this method's job, and returns its result.
* When `element_type` is `method/abstract`, the following additional properties are available:
	* `name` - the name that will be exposed to scripts to invoke this method. If the script is used by a class which contains or inherits this method, it will be exposed as a function in addition to a method.
	* `return_type` - a reference to an ElementSpec describing what type of data this method returns. The referenced ElementSpec must be of type `class/...`.
	* `parameters` - a list of parameters that this method takes as inputs. A single parameter has the following properties:
		* `name` - the name exposed to scripts (as a variable) to reference the value of this parameter.
		* `type` - a reference to an ElementSpec describing what type of data is expected for this parameter. The referenced ElementSpec must be of type `class/...`.

	The main difference between normal methods and abstract methods is that abstract methods don't have code associated with them, and do not know how to do their job. It is the responsibility of the user to override abstract methods in classes that extend abstract classes in order to fully specify how each method does its job.
* When `element_type` is `property/normal`, the following additional properties are available:
	* `name` - the name that will be exposed to scripts to get or set this property. If the script is used by a class which contains or inherits this method, it will be exposed as a variable in addition to a field.
	* `property_type` - a reference to another ElementSpec describing the type of data contained by this property. The referenced ElementSpec must be of type `class/...`.
	* `is_3d` - true if this property depends on Y level, false otherwise. Defaults to false. When true, this property will be exposed as a method to scripts instead of a field, but it will still be assignable. So you can do `object.property(y) = 42`.
	* `get` - a script which returns the value of this property.
	* `set` (optional) - a script which takes the new value to be assigned to this property, and assigns it. This script returns nothing.
		* The new value to be assigned to this property is exposed to the script as a variable named `value`.
* When `element_type` is `property/override`, the following additional properties are available:
	* `override` - a reference to an ElementSpec representing the property to override. The referenced ElementSpec must be of type `property/...`.
	* `get` - a script which returns the value of this property.
	* `set` (optional) - a script which takes the new value to be assigned to this property, and assigns it. This script returns nothing.
		* The new value to be assigned to this property is exposed to the script as a variable named `value`.
		* If the property being overridden has a setter, then this property must too.
		* Likewise, if the property being overridden does NOT have a setter, then this property must not either.
* When `element_type` is `property/abstract`, the following additional properties are available:
	* `name` - the name that will be exposed to scripts to get or set this property. If the script is used by a class which contains or inherits this method, it will be exposed as a variable in addition to a field.
	* `property_type` - a reference to another ElementSpec describing the type of data contained by this property. The referenced ElementSpec must be of type `class/...`.
	* `settable` - true if this property should have an abstract setter in addition to an abstract getter, false otherwise.
	* `is_3d` - true if this property depends on Y level, false otherwise. Defaults to false. When true, this property will be exposed as a method to scripts instead of a field, but it will still be assignable. So you can do `object.property(y) = 42`.