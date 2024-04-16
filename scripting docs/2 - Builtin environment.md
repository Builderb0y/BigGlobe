Script environments provide things for scripts to make use of. These things include:
* Variables - referenced by an identifier. Example: `foo`
* Fields - referenced by member expressions. Example: `foo.bar`
* Functions - referenced by an identifier, followed by an opening parentheses, followed by a comma-separated list of scripts for arguments, followed by a closing parentheses. Example: `foo(x, y, z)`
* Methods - referenced by a member expression followed by the same parentheses and arguments as functions. Example: `foo.bar(x, y, z)`
* Keywords - referenced by an identifier and usually followed by some other text. What text is matched depends on the keyword in question. Example: `if (condition: body)`
* Member keywords - referenced by a member expression where the member is the name of a keyword, also followed by more text. Example: `condition.if (body)`
* Types - a category for all other variables. For example, true and false have a type of boolean, and "hello world!" has a type of String. Every time you declare a variable, it starts with the type of that variable. For example, `String text = "hello world!"`.
* Casters - handles conversion between two different types. For example, a caster for int -> long will be able to specify how to convert an int to a long. This allows you to do things like
	```
	int x = 2
	long y = x ;implicit conversion
	long z = long(x) ;explicit conversion, not that it makes any difference in this case.
	```

What follows below is the "builtin" script environment. This environment is always present on every script, though in rare cases some functionality may be limited or removed. I will try to ensure that such cases are also documented.

Information about other environments can be found in the "environments" directory. Information about which environments are present on each script can be found in the "present" directory.

# Variables

* true
* false
* yes - alias for true
* no - alias for false
* null - the object constant null represents the absense of a value. null can be implicitly cast to any type.
* noop - does nothing and returns void. Can be used as a placeholder in certain situations. For example:
```
void notYetImplemented(int value:
	noop
)
```

# Types

## Primitive types

* boolean - true or false.
* byte - numeric; -2^7 to 2^7 - 1. 8 bits.
* short - numeric; -2^15 to 2^15 - 1. 16 bits.
* int - numeric; -2^31 to 2^31 - 1. 32 bits.
* long - numeric; -2^63 to 2^63 - 1. 64 bits.
* float - numeric; floating point; 32 bits. Corresponds to an IEEE 754 32-bit floating point value type.
* double - numeric; floating point; 64 bits. Corresponds to an IEEE 754 64-bit floating point value type.
* char - technically numeric; has no way to specify literals for it yet. Mostly unused. 16 bits; unsigned integer type.
* void - represents the absence of a type. Can only be used as a return type for functions. You cannot declare variables or parameters of this type.

## Object types

* Boolean - wrapper for boolean.
* Byte - wrapper for byte.
* Short - wrapper for short.
* Integer - wrapper for int.
* Long - wrapper for long.
* Float - wrapper for float.
* Double - wrapper for double.
* Character - wrapper for char.
* Void - can't have instances of this type. Someone please remind me why I exposed this.
* Number - common super class of Byte, Short, Integer, Long, Float, and Double.
* Object - common super class of all other non-primitive types.
* Comparable - common super interface of all non-primitive values that can be compared with operators like `< > <= >=` and so on.
* String - stores text. You can specify String's directly as literals, and interpolate them. See Low level text format.md.
* Class - represents a type itself. you can call `print(object.getClass())` on any object to see what it really is under the hood.
	* For anyone who's used java before, no you cannot use reflection.

# Functions

* `return(value)` - ceases execution of a script or a user-defined method, and returns flow control to whatever called it. Example usage:
	```
	boolean find(List list, Object target:
		for (Object element in list:
			if (element == target:
				return(true) ;skips the rest of the elements in the list.
			)
		)
		false
	)
	```
	Note that you can also use `list.contains(target)` for this.
	Additionally, if you want to cease execution of a void-returning method, you can do so with `return()`.
* print - In singleplayer, sends the player a chat message. In multiplayer, logs text to the server console. This function is intended for debugging. print() can take up to 256 arguments, and will convert them to String's and concatenate them as necessary.
* floorInt - casts a float or double to an int, rounding towards negative infinity. Note that directly casting a float or double to an int with `int(myFloat)` will also round towards negative infinity.
* floorLong - casts a float or double to a long, rounding towards negative infinity. Note that directly casting a float or double to a long with `long(myFloat)` will also round towards negative infinity.
* ceilInt - casts a float or double to an int, rounding towards positive infinity.
* ceilLong - casts a float or double to a long, rounding towards positive infinity.
* roundInt - casts a float or double to an int, rounding towards the nearest int, with ties broken by rounding towards positive infinity.
* roundLong - casts a float or double to a long, rounding towards the nearest long, with ties broken by rounding towards positive infinity.
* truncInt - casts a float or double to an int, rounding towards 0.
* truncLong - casts a float or double to a long, rounding towards 0.

# Keywords

* `var name = expression` declares a new variable whose name is name, and whose type is inferred automatically from the type of expression. The expression is a *single* expression (see Basic syntax.md).
* `class Name(Type1 name1 Type2 name2)` declares a new class named Name containing 2 fields. The first named name1 of type Type1, and the second named name2 of type Type2. Instances of Name can be created with `Name.new(name1, name2)` from that point onward in the source code. You can also do `Name.new()` to initialize all fields to 0/null/false. Fields may be accessed with `name.name1` and `name.name2`, and modified with `name.name1 = someValue` and `name.name2 = someValue`.
	* The fields in the class declaration may be separated by single or double commas, for clarity. For example, `class Name(int a int b, int c,, int d)` is a valid class declaration.
	* Fields may be given default values with the following syntax: `class Name(int a = 2, int b)`. This now makes it so that `Name.new()` will initialize a to 2, but leave b at 0. It also unlocks a 3rd constructor: `Name.new(3)`, which sets a to 2 and b to 3. **The default field values must be compile-time constants!**
* `if (condition: body)` evaluates body if, and only if, condition evaluates to true. condition may be a script (see Basic syntax.md), as may body. condition must be of type boolean.
	* An else branch may be appended to the end `if (condition: trueBody) else (falseBody)` and in this case, trueBody will be evaluated if, and only if, condition evaluates to true, and falseBody will be evaluated if, and only if, condition evaluates to false.
	* Multiple if/else branches can be chained together:	
		```
		if (condition1: body1)
		else if (condition2: body2)
		else if (condition3: body3)
		else (body4)
		```
	* If an if statement has an associated else statement, then the type of the whole thing is the most specific common super class of all of the bodies. For example:
		```
		int value = if (condition:
			value1
		)
		else (
			value2
		)
		```
		This makes it an alternative to ternary expressions, which is sometimes useful.
* `unless (condition: body)` - syntax sugar for `if (!condition: body)`. Works with else statements just like if does.
* `while (condition: body)` - repeats body over and over again for as long as the condition remains true. The condition is checked once before evaluating body, so if condition is false to begin with, then body will never be run. To force the body to run at least once, use a `do while` loop instead. The flow control of this construct looks a bit like this:
	1. Check condition.
		* If true, advance to step 2.
		* If false, go to step 4.
	2. Evaluate body.
	3. Go back to step 1.
	4. Done.
* `until (condition: body)` - syntax sugar for `while (!condition: body)`.
* `do while (condition: body)` and `do until (condition: body)` work very simmilarly to while and until, but they evaluate the body first, before checking the condition. The flow control of `do while` looks a bit like this:
	1. Evaluate body.
	2. Check condition.
		* If true, go back to step 1.
		* If false, advance to step 3.
	3. Done.
* `repeat (numberOfTimes: body)` evaluates body numberOfTimes times. The evaluation of numberOfTimes only happens once, just for anyone concerned with side effects. If numberOfTimes is negative, the body is run backwards. Ok not really. It actually doesn't run at all in this case.
* for - there are actually many different structures all wrapped up in one keyword here.
	* Traditional for loop: `for (initializer, condition, step: body)` is syntax sugar for:
		```
		(
			initializer
			while (condition:
				body
				step
			)
		)
		```
		Example usage:
		```
		for (int x = 1, x <= 10, ++x:
			print(x) ;prints the numbers 1 through 10.
		)
		```
	* Enhanced for loop (over an Iterable): `for (Type element in iterable: body)`
	* Enhanced for loop (over a Map): `for (Type1 key, Type2 value in map: body)`
	* Enhanced for loop (over a range): `for (int value in range[1, 10]: body)`
		* Ranges can provide ints, longs, floats, and doubles.
		* Square brackets are inclusive, and parentheses are exclusive. It is also perfectly valid to mix and match them. For example, it is quite common to do `for (int value in range[0, 10): body)`.
		* You can iterate in reverse order by negating the range: `for (int value in -range[1, 10]: body)`. Note that the first value in the range is still the lower bound (now the element encountered last), and the 2nd value is still the upper bound (now the element encountered first).
		* You can set the step size by modulus'ing the range: `for (int value in range[0, 10] % 2: print(value))` this will print the *even* numbers between 0 and 10.
		* All of this is hard-coded syntax btw. range isn't a keyword or a function on its own, there is no Range class which contains the min/max/step/ascending vs. descending state, and there are no operator overloads at play here.
		* If the min, max, or step is not a compile-time constant, it will only ever be evaluated once.
	* Multi-loops (at the time of writing this, these are not in a released version of Big Globe yet, but they will be in 4.0): `for (Type1 element1 in list1, Type2 element2 in list2: body)` this is almost syntactically equivalent to declaring an outer loop over list1 containing an inner loop over list2, with one exception: breaking the loop breaks the *outer* loop, where as continuing the loop continues the *inner* loop.
		* Multi-loops can be combined to create as many nested loops as you want.
		* Multi-loops can use Iterable's, Map's, and ranges interchangeably, and even different iteration methods for different levels of the loop.
		* Multi-loops do not work with traditional for loop syntax.
	* Multi-variable range loops (also not until 4.0): `for (int x, int y, int z in range[-1, 1]: body)`
		* Like multi-loops, this works like declaring an outer loop for x, a middle loop for y, and an inner loop for z.
		* Also like multi-loops, continuing will continue the inner loop, and breaking will break the outer loop.
* `block (body)` can be thought of as a manual loop. If the body continues, then execution jumps back to the start of the block. And if the body breaks, then execution jumps to the end of the block. If the body does neither and reaches the end of the block, then the block is implicitly broken. You do not need to explicitly break at the end of each block.
* `break()` exits the inner-most loop that is currently executing, skipping any code that may be between the current execution point and the end of the loop. Example usage:
	```
	for (int x in range[0, 10):
		if (x == 2: break())
		print(x)
	)
	print('done!')
	```
	This will print 0, 1, and 'done!'. If you wish to break out of a nested loop, you can give that loop a name:
	```
	for outer (int x in range[0, 10):
		for (int y in range[0, 10):
			if (y == 2: break(outer))
			print('$:y')
		)
		print('$:x')
	)
	print('done!')
	```
	This will print 'y: 0', 'y: 1', and 'done!'. It works mostly the same for all loop types.
	* `while name (...)`
	* `do until name (...)`
	* `repeat name (...)`
	* `block name (...)`
* `continue()` skips only the current iteration of the inner-most loop which is currently running, skipping any remaining code in the loop which hasn't executed yet.
	```
	for (int x in range[0, 10]:
		if (x & 1 != 0: continue())
		print(x)
	)
	```
	This will print the *even* numbers between 0 and 10. Like break(), you can also continue labeled loops with `continue(name)`.
* switch - allows you to handle more than 2 possible options for some value.
	```
	String count = switch (number:
		case (0: 'none')
		case (1: 'one')
		case (2: 'a couple')
		case (3, 4, 5: 'a few')
		default ('quite a few')
	)
	```
	This one does pretty much exactly what you'd expect:
	* If number is 0, then count is 'none'.
	* If number is 1, then count is 'one'.
	* If number is 2, then count is 'a couple'.
	* If number is 3, 4, or 5, then count is 'a few'.
	* If number is anything else, then count is 'quite a few'.
* compare is a switch-like construction for comparing 2 numbers:
	```
	String comparison = compare(a, b:
		case (>: 'a is greater than b')
		case (<: 'a is less than b')
		case (=: 'a is equal to b')
	)
	```
	* If a and b are floats or doubles, then they have the possibility of being NaN, and in this case, you also need to specify a `!` case, which gets executed when the two numbers can't be compared because one or both of them is NaN.
	* compare() can also compare instances of Comparable, and it works just like the standard `< > ==` operators.
	* If you only provide one number, then the 2nd number is implicitly 0.
		* Providing only one instance of Comparable is not allowed, because there is no standardized way to get a "zero" Comparable instance for arbitrary types.
	* If you've studied these documents well, you might suspect that `>:` and the other cases are operators. But they're not! You are allowed to have a space between them. This is (at the time of writing this) the only place where the expression parser does not group them together.
* noscope (4.0+) allows you to group expressions together without declaring a new scope. Any variables declared in the noscope block will be available in the same scope as the noscope keyword itself.
	```
	(int x = 2)
	print(x) ;error: x is not accessible here

	noscope(int x = 2)
	print(x) ;works just fine

	;a practical example:
	if (noscope(int y := ...) > 2:
		print(y)
	)
	```

# Member keywords

* `condition.if (body)` works just like `if (condition: body)` except that *it returns condition*, not the result of body. Additionally, it'll also do this even if you don't have an else block (though you can definitely include an else block if you want to).
* `condition.unless (body)` see above. This does exactly what you'd expect. Note that it still returns condition as-is, it is not negated despite the fact that body is only evaluated if condition is false.
* `object.is(Type)` returns true if object is non-null and can be cast to Type, false otherwise. If you've used java before, this is literally just instanceof.
* `object.isnt(Type)` returns true if object is null or can't be cast to Type, false otherwise. If you've used java before, this is a negated instanceof check.
* `value.as(Type)` is alternate syntax for `Type(value)`. The value is explicitly cast to Type.
* `number.isBetween[min, max]` returns true if number is between min and max. Just like enhanced for loops over ranges, square brackets specify an inclusive interval, and parentheses specify an exclusive interval. And yes, you can mix and match them. `x.isBetween[5, 10)` is basically syntax sugar for `x >= 5 && x < 10`. As usual, x is only ever evaluated once, not twice.