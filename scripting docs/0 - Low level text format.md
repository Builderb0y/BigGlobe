# Comments

Comments are treated as whitespace, and all logic which ignores whitespace will also ignore comments. A comment starts with a semicolon, and (optionally) a 2nd character to tweak its behavior.

## Simple multi-line comments

If the 2nd character is another semicolon, then the comment extends until another double-semicolon is encountered.
```
this text is not part of a comment
;;
this text is part of a comment
;and so is this text
and this text too
;;
this text is not part of a comment
```

## Advanced multi-line comments

If the 2nd character is an opening parentheses, then the comment extends until a **matching** close parentheses is encountered.
```
not comment
;(
	comment
	(
		more comment
	)
	still comment
)
no longer a comment
```
The newlines and indentation are irrelevant for determining what counts as a matching close parentheses. All that matters is that the number of closing parentheses matches the number of opening parentheses.

## Single-line comments

If the 2nd character is neither of the above characters, then the comment extends until the end of the line.
```
not comment
;comment
not comment
```

# Identifiers

Identifiers come in 2 forms: normal identifiers and escaped identifiers.

Normal identifiers work how you'd expect in most other languages, you just type a word and it counts as an identifier. Examples of this type of identifier include:
```
foo
BAR
foo_bar
foobar123
```
Identifiers of this type may contain underscores and numbers, but they cannot *start* with a number.

Escaped identifiers are defined as a tickmark, followed by some text, followed by another tickmark. Escaped identifiers may contain any character, including whitespace, numbers at the start, and even symbols. However, escaped identifiers cannot contain newlines. Examples of escaped identifiers include:
```
`foo`
`foo bar`
`minecraft:snow[levels=8]`
`bigglobe:overworld/surface_y`
```
Note that if a comment is encountered inside an identifier, it counts as part of the identifier. What you see is what you get.
```
Input: `foo;(hello!)bar`
Output: foo;(hello!)bar
```

# Number literals

A number literal is composed of an optional radix (also known as a base), an integer part, an optional fractional part, an optional exponent, and some optional suffixes.

## Basic number literals

Basic number literals work exactly how you'd expect from most other languages, they are base 10, and may include a fractional part.
```
0
123
123.45
```
Note that an integer part is always required, and if the number is fractional (meaning it has a radix point in it), then it is required to have a fractional part. So `1.` and `.5` are NOT valid number literals.

Underscores are allowed anywhere inside number literals, with the intention of using them to separate digits. For example, `1_000_000` is a literal for the number 1 million.

## Specifying a radix

Unlike other languages which use prefixes like `0x` for hexadecimal, and `0b` for binary, in my language, you simply type the radix you want, followed by an uppercase or lowercase x, followed by the number written in that radix. The radix must be between 2 and 16 (inclusive).
```
16xFF  ;radix: 16, decimal value: 255
2x1001 ;radix:  2, decimal value:   9
4x3    ;radix:  4, decimal value:   3
```
Radixes stack with fractional values, so `2x11.1` has a radix of 2 and a decimal value of 3.5.

## Specifying an exponent

The letter p (case insensitive) is used to denote an exponent. P was chosen because E would've conflicted with hexadecimal numbers. It stands for "precision". Though if you prefer you can also think of it as ex**P**onent.
```
2p3 ;2 * 10^3 = 2000
```

The base of the exponent is the same as the radix of the number.
```
2x1p8 ;1 * 2^8 = 256
```

The exponent can also be negative, for numbers with a fractional part.
```
5.0p-3 ;5 * 10^-3 = 0.005
```

Interestingly, the exponent can have its own radix. I don't remember writing the logic for this, and I can't imagine a situation where this would be useful, but it exists nevertheless.
```
4x10p2x10 ;4 * 4 ^ 2 = 64
```
If the exponent does not have a radix, it defaults to 10.

## Suffixes

A number literal without a suffix will, in general, default to the smallest precision necessary to represent the number exactly, with the following rules:
* If the literal has a fractional part, it will be a float if a float can exactly represent the number. If a float cannot exactly represent the number, then the literal is treated as a double. 0.5 is a float literal, where as 0.1 is a double literal. It is recommended to use I and L suffixes for numbers with fractional parts, to avoid confusion about which numbers can and can't be represented exactly with floats.
* If the number does NOT have a fractional part, then it will be an int if the number is within the range of an int. Otherwise, if the number is within the range of a long, then it will be treated as a long literal. If a long can't represent it exactly either, then an exception is thrown.
* New in V4.3.3: If the number does NOT have a fractional part and can be represented exactly by a byte or short, it will be treated as an int. This was added because I kept foot-gunning myself with random numbers that I expected to be ints, but they were actually bytes.

All suffixes are case-insensitive.

### U

Specifies that the number is unsigned. **This does NOT mean that normal arithmetic operations will treat it as an unsigned value!** It just means the range that it uses for validity checking and auto-typing is shifted. For example, if the default precision of a number without a suffix were allowed to be a byte or short (like it was in old versions), then `255` would be a `short` literal, because the `byte` range is -128 to +127 (inclusive). However, `255u` would be a `byte` literal, because the *unsighed* `byte` range is 0 to 255 (also inclusive).

The unsigned suffix is not allowed for numbers which have a fractional part.

If multiple suffxes are present, U must be first. This restriction may be relaxed in future versions of Big Globe.

### F, D

Invalid. Will throw an exception if encountered. In other languages, this would have specified `float` or `double` precision. However, both of these characters conflict with hexadecimal numbers. So, I and L were used instead, and F and D throw an exception when used with a radix which doesn't support them, like decimal. F and D are valid characters inside a hexadecimal literal.

### L

Specifies that the number should have 64 bits. If the number contains a fractional part, then it is treated as a `double` literal. Otherwise, it is treated as a `long` literal.

### I

Specifies that the number should have 32 bits. If the number contains a fractional part, then it is treated as a `float` literal. Otherwise, it is treated as an `int` literal.

### S

Specifies that the number should have 16 bits. If the number contains a fractional part, an exception is thrown. Otherwise, the number is treated as a `short` literal.

### Y

Specifies that the number should have 8 bits. If the number contains a fractional part, an exception is thrown. Otherwise, the number is treated as a `byte` literal.

# Operators

An operator *character* is one of the following: `!#%&*+,-./:<=>?@\^|~`

Operator characters do NOT include parentheses, square brackets, curly braces, tick marks, or underscores.

The significance of this is that an *operator* is composed of one or more operator characters with no whitespace between them. Examples of operators include:
```
+
+-
=+
!@==*
```
Not all of these operators will do anything meaningful, and some may cause a compile error when encountered. Meanwhile, in the text `!()`, only the exclamation mark counts as an operator.

If a comment is encountered inside an operator, it splits the operator into two operators, as if there had been whitespace there.

# String literals

A String literal is defined as a single or double quote mark, followed by some text, followed by another single or double quote mark. The types of quotes must match, so `"a"` and `'a'` are both valid String literals, where as `"a'` and `'a"` are not. String literals can be multi-line.

## Interpolation

Strings may contain interpolated expressions. Interpolation is when another expression is embedded inside a String literal, and the result is the same as the concatenation of the text before the expression, the value of the expression, and the text after the expression. An interpolation expression always starts with a dollar sign `$`, and its behavior can be further tweaked with an optional second or third character following it.

### Term interpolation

In the absence of any other special characters, a dollar sign on its own will match a *term* (see Basic syntax.md). Because of this, it is possible to match both variables and functions. It is also possible to use parentheses to manually specify the exact expression you want to match.
```
int x = 5
print("The value is $x") ;prints "The value is 5" (without quotes).

int add(int y: x + y)
print("The sum is $add(5)") ;prints "The sum is 10" (without quotes).

int add = 15
print("The expression is $(add)()") ;prints "The expression is 15()" (without quotes)
```

### Member interpolation

If the 2nd character is a dot `.` then the interpolation will match a *member expression* (see Basic syntax.md)
```
class Box(int value)
Box box = new(42)
print("The box contains $.box.value") ;prints "The box contains 42" (without quotes)
```

Note that a term is also a member expression, so the term examples will work with `$.` too.

### Self-description

If the 2nd character is a colon `:` then the matched expression is another term, HOWEVER the final String will contain the source code of the term, followed by a colon, a space, and the evaluation of the term.
```
int x = 1
int y = 2
int z = 3
print("$:x, $:y, $:z") ;prints "x: 1, y: 2, z: 3" (without quotes).
```
In this case, if the *third* character is a dot `.` then the matched expression is a member expression.
```
class Box(int value)
Box box = new(42)
print("$:.box.value") ;prints "box.value: 42" (without quotes).
```

### Escaping the dollar sign

Putting 2 dollar signs in a row will result in one dollar sign being included in the String literal. And if you want 2 dollar signs in the String literal, type 4 of them in your source code.
```
print("The price is $$5") ;prints "the price is $5" (without quotes)
```

# Embedding into JSON

Most script instances are represented by either a string, or an array of strings. when using an array, the elements are concatenated, with a newline character separating them. As such, if you want to specify a String literal in a JSON script, it is recommended to use single quote marks, as double quote marks will need to be escaped with backslashes to be JSON-compliant.

## Formatting and indentation

If you like tabs for indentation, then pretty much your only option is to indent the JSON strings themselves, since JSON strings can't contain tab characters.
```json
{
	"script": [
		"int getSum(:",
			"int sum = 1",
			"for (int number in range[0, 10]:",
				"sum += number",
			")",
			"sum",
		")",
		"print(getSum())"
	]
}
```
If you like spaces, another option opens up:
```json
{
	"script": [
		"int getSum(:",
		"    int sum = 1",
		"    for (int number in range[0, 10]:",
		"        sum += number",
		"    )",
		"    sum",
		")",
		"print(getSum())"
	]
}
```
And if you want to go full chaotic evil, GSON, the library Minecraft uses to parse JSON files, has a bug in it: it doesn't actually care what characters you put in JSON strings, even if they violate the JSON specs. This includes tabs, and even newlines. So *technically* you could do this:
```json
{
	"script": "
		int getSum(:
			int sum = 1
			for (int number in range[0, 10]:
				sum += number
			)
			sum
		)
		print(getSum())
	"
}
```
I would not count on this continuing to work forever though. It is entirely possible that GSON might fix this bug some day, and it will be outside the scope of things I can un-fix. If you do this, you accept the risk that your code might break some day and you will need to re-format it.