# Term

A term is the simplest construct my parser can parse.

If a term starts with an ASCII letter, an underscore, or a tickmark, then the term is an identifier term starting at that character (see Low level text format.md).

If a term starts with an ASCII number (0 through 9), then the term is the numeric literal starting at that character (see Low level text format.md).

If a term starts with a single or double quote mark, then the term is the string literal starting at that character (see Low level text format.md).

If a term starts with an open parentheses `(`, then the term is that parentheses, followed by a script (see the bottom of this file), followed by a closing parentheses `)`.

If a term starts with any other character, an exception is thrown.

## Identifier terms

An identifier term is a term that starts with an identifier, and may match more text following the identifier. Identifier terms can take the following forms:

* `keyword ...`
	* The keyword takes full control of the parser and only returns control to it after parsing everything it intended to parse. See the documentation for individual keywords for information on what they match.
* `Type(expression)` explicitly casts expression to Type.
* `Type name = expression` declares a new variable named name of type Type and initializes it to expression.
* `Type name = new(arguments)` syntax sugar for `Type name = Type.new(arguments)`
	* Extra member operations can be appended to the end. For example, `HashMap map = new().$put("a", 1).$put("b", 2)`.
* `Type name := expression` same as with `=`, but also returns the newly initialized value.
	* This makes it possible to do `int x = int y := int z := 0`.
* `Type*(name1 = value1, name2 = value2, ...)` (new in V4.3.0) declares multiple variables of the same type without needing to specify the type more than once.
	* The comma separating the name/value pairs is optional.
* `Type functionName(Type1 param1, Type2 param2, ...: body)` function declaration. It can be called with `functionName(param1, param2, ...)`.
	* As of V4.3.0, this supports multi-declaration syntax, so you can also do `Type functionName(Type1*(param1, param2, ...), Type2 param3, ...: body)`.
* `ReturnType SelfType.methodName(Type1 param1, Type2 param2...: body)` extension method declaration. It can be called with `object.methodName(param1, param2, ...)`.
	* Like function declarations, these also support multi-declaration syntax in V4.3.0+, and it works exactly the same way.
* `Type` constant for a Class object representing Type.
	* This is intended primarily for calling `Type.staticMethod()` or `Type.new()`, but it can also be used for debugging. For example, `print(Type)` will show you exactly what the underlying class of Type is, as sometimes the exposed name does not match the actual name.
* `functionName(arguments)` calls the function with the provided arguments.
* `variableName` gets the value of the variable.

These all seem like wildly unrelated things, but they all count as identifier terms.

# Member expressions

A member expression takes one of the following forms:

* `term.field` gets the field named field from the object represented by term, or throws a NullPointerException if term is null.
* `term.method(arguments)` invokes the method named method on the object represented by term, or throws a NullPointerException if term is null.
* `term.?field` gets the field named field from the object represented by term, or null/false/0 if term is null.
* `term.?method(arguments)` invokes the method named method on the object represented by term, or returns null/false/0 if term is null.
* `term.=field(value)` syntax sugar for `term.field = value`.
* `term.$method(arguments)` same as `term.method(arguments)`, but returns the term object instead of the return value of method().

The characters `?`, `=`, and `$` all stack with each other, and can appear in any order as long as `.` is first. For example, `foo.?=$bar(value)` would first check if foo is null. If it's not null, then it assigns value to the bar field on foo. Regardless of whether foo is null or not, it is returned for the next bigger expression to use.

# Prefix expressions

A prefix expression takes one of the following forms:
* `+ member` asserts that member's type is numeric, and returns the member unchanged.
* `- member` asserts that member's type is numeric, and returns the numeric negation of the member.
* `~ member` asserts that member's type is an integer (byte/short/int/long), and returns the bitwise negation of the member.
* `! member` asserts that member's type is boolean, and returns the logical negation of member.
* `++ member` asserts that member's type is numeric, and increments member by 1.
* `-- member` asserts that member's type is numeric, and decrements member by 1.
* `:++ member` asserts that member's type is numeric, increments member by 1, and returns the *new* value of member. In other words, the value the member has *after* being incremented.
* `:-- member` asserts that member's type is numeric, decrements member by 1, and returns the *new* value of member. In other words, the value the member has *after* being decremented.
* `++: member` asserts that member's type is numeric, increments member by 1, and returns the *old* value of member. In other words, the value the member has *before* being incremented.
* `--: member` asserts that member's type is numeric, decrements member by 1, and returns the *old* value of member. In other words, the value the member has *before* being decremented.

# Elvis expressions

`member ?: elvis` returns member if member is non-null (for object types), non-NaN (for floats/doubles), or non-zero (for integer types), and elvis otherwise. Note that in the event that 3 or more expressions are elvis'd together like in the case of `a ?: b ?: c`, the order of operations is right-to-left. Not that it makes much difference, but still.

Elvis expressions interact with nullable member expressions to provide an alternate default value than 0/null/false. Consider the following example:
```
class Box(int value)
Box box = new(0)
int x = box.?value ?: 42
```
One might expect that since box is non-null and box.value is 0, that the elvis operator would see that 0 and delegate to 42 instead, but in this case `?:` and `.?` work together to only delegate to 42 when box itself is null. Since box is not null in this case, x will be 0, not 42.

# Exponent expressions

`value ^ exponent` will return value raised to the power of exponent. Note that like elvis expressions, exponent expressions are also right-to-left. Unlike elvis expressions, the order matters quite a bit here. So keep that in mind.

Another thing to keep in mind with exponents is that they are approximated. Several optimizations have been made for performance reasons. If you need something to be accurate to within 1 ulp of the correct result, consider using `pow(value, exponent)` instead. On the other hand, if one part in a trillion is sufficient, then `value ^ exponent` will be fine. See also: Optimizations.md.

# Product expressions

A product expression takes one of the following forms:
* `exponent * exponent` returns the numeric product of the two values.
* `exponent << exponent` returns the left exponent bit shifted left by the right exponent number of bits.
* `exponent <<< exponent` returns the left exponent bit shifted left by the right exponent number of bits.
* `exponent / exponent` returns the left exponent divided by the right exponent. If the division is done on integer types, then it is always rounded towards negative infinity, not towards zero.
* `exponent >> exponent` returns the left exponent bit shifted right by the right exponent number of bits.
* `exponent >>> exponent` returns the left exponent bit shifted right by the right exponent number of bits.
* `exponent % exponent` returns the modulo of the left exponent and the right exponent. This is NOT the same as the remainder for negative numbers. There is currently no operator for remainder operations because they suck and I hate them.

## Notes on bit shifting

* 2 arrows is signed, 3 arrows is unsigned.
* If the right operand is negative, the shift is done in the opposite direction by the negative of the right operand number of bits. For example, `2 << -1 == 2 >> 1`.
* If the right operand is larger than the size of the number being shifted, that operand does NOT wrap around, but the final returned value does. For example, `1 << 100 == 0`.
* If the left operand is a float or double, then the shift must be signed. Left shifting a float or double by N bits is equivalent to `value * 2 ^ N`, and right shifting is equivalent to `value / 2 ^ N` or `value * 2 ^ -N`.
* In all cases, the right operand must be an integer type.

# Sum expressions

A sum expression takes one of the following forms:
* `product + product` returns the sum of the two products.
* `product - product` returns the left product minus the right product.
* `product & product` returns the bitwise and of the two products.
* `product | product` returns the bitwise inclusive or of the two products.
* `product # product` returns the bitwise exclusive or or the two products.

# Compare expressions

A compare expression takes one of the following forms:
* `sum < sum` returns true if the left sum is strictly less than the right sum, false otherwise.
* `sum <= sum` returns true if the left sum is less than or equal to the right sum, false otherwise.
* `sum > sum` returns true if the left sum is strictly greater than the right sum, false otherwise.
* `sum >= sum` returns true if the left sum is greater than or equal to the right sum, false otherwise.
* `sum == sum` for numbers, returns true if the two sums are numerically equivalent, false otherwise. For objects, returns true if the two objects equal each other. Note that two different objects can equal each other.
* `sum != sum` for numbers, returns true if the two sums are NOT numerically equivalent, false otherwise. For objects, returns true if the two objects do not equal each other. Note that the same object always equals itself.
* `sum === sum` for numbers, returns true if the two numbers have the same bit pattern, false otherwise. For objects, returns true if the two objects are the same object, false otherwise.
* `sum !== sum` for numbers, returns true if the two numbers do NOT have the same bit pattern, false otherwise. For objects, returns true if the two objects are NOT the same object, false otherwise.
* `sum !> sum` equivalent to `!(leftSum > rightSum)`. This is not necessarily equivalent to `leftSum <= rightSum` for floats and doubles when one (or both) of the operands are NaN. In this case, the former will return true, while the latter will return false.
* `sum !< sum` equivalent to `!(leftSum < rightSum)`. This is not necessarily equivalent to `leftSum >= rightSum` for floats and doubles when one (or both) of the operands are NaN. In this case, the former will return true, while the latter will return false.
* `sum !>= sum` equivalent to `!(leftSum >= rightSum)`. This is not necessarily equivalent to `leftSum < rightSum` for floats and doubles when one (or both) of the operands are NaN. In this case, the former will return true, while the latter will return false.
* `sum !<= sum` equivalent to `!(leftSum <= rightSum)`. This is not necessarily equivalent to `leftSum > rightSum` for floats and doubles when one (or both) of the operands are NaN. In this case, the former will return true, while the latter will return false.

New in V4.3.0: prepending a `.` to the beginning of the operator will make it explicitly cast the left operand to the right operand's type before doing the comparison. Likewise, appending a `.` to the end of the operator will make it explicitly cast the right operand to the left operand's type before doing the comparison.

Changed in V4.3.0: when used on floats and doubles, === and !== used to treat NaNs with different bit patterns as not equal. Now, they are collapsed into a single canonical NaN bit pattern before being compared, which ensures that all NaN values are considered equal to all other NaN values according to this operator.

Notes on comparing objects with `<`, `<=`, `>`, `>=`, `!>`, `!<`, `!>=`, or `!<=`:
* These operators require that both objects implement Comparable.
* If one or both objects are null, then the comparison will return false, and the negated comparison will return true. It's as if null is analogous to NaN.

Notes on comparing objects with `==`, `!=`, `===`, or `!==`:
* These operators do NOT require that both objects implement Comparable.
* If exactly one object is null, then `==` and `===` will return false, and `!=` and `!==` will return true.
* If both objects are null, then `==` and `===` will return true, and `!=` and `!==` will return false.
	* This behavior differs from that of `<=` and `>=`, as both of those return false when both operands are null.

Examples:
```
(nan <   0  ) == false
(nan >   0  ) == false
(nan ==  0  ) == false
(nan !=  0  ) == true
(nan ==  nan) == false
(nan !=  nan) == true
(nan === nan) == true
(nan !<  0  ) == true
(nan !>  0  ) == true
```

# Boolean expressions

A boolean expression takes one of the following forms:
* `compare && compare` returns true if both compares are true, false otherwise. If the left compare evaluates to false, then false is returned immediately and the right compare is NOT evaluated.
* `compare || compare` returns true if at least one compare is true, false otherwise. If the left compare evaluates to true, then true is returned immediately and the right compare is NOT evaluated.
* `compare ## compare` returns true if the two compares evaluate to different states, false otherwise.
* `compare !&& compare` returns true if at least one compare is false, false otherwise. If the left compare evaluates to false then true is returned immediately and the right compare is NOT evaluated.
* `compare !|| compare` returns true if neither compare is true, false otherwise. If the left compare evaluates to true, then false is returned immediately and the right compare is NOT evaluated.
* `compare !## compare` returns true if both compares evaluate to the same state.

# Ternary expressions

`booleanExpression ? singleExpressionIfTrue : singleExpressionIfFalse` returns singleExpressionIfTrue if booleanExpression evaluates to true, and singleExpressionIfFalse if booleanExpression evaluates to false.

# Assignment expressions

* `variable = value` stores value in variable. Nothing special here.
* `variable (operator)= value` is syntax sugar for `variable = variable (operator) value` when (operator) is one of: `+ - * / % ^ & | # && || ## << >> <<< >>>`. variable is only evaluated once, for anyone concerned with its evaluation having side effects.
	* There must not be any whitespace or comments between (operator) and `=`.
* `variable := value` stores value in variable, and returns the new value that just got stored.
* `variable :(operator) value` is syntax sugar for `variable := variable (operator) value` for the same set of operators as listed before.
* `variable =: value` stores value in variable, and returns the old value from just before it was overwritten.
* `variable (operator): value` is syntax sugar for `variable =: variable (operator) value`.

# Single expressions

This is just an alias for assignment expressions.

# Compound expressions

`singleExpression1,, singleExpression2` evaluates singleExpression1 and returns the value from singleExpression2. The `,,` operator can be used for clarification, or to resolve ambiguity. And every now and then it also helps with a footgun I accidentally added. Basically, you can sometimes end up with cases like this:
```
int foo(:
	int y = 2
	-y ;implicit return
)
```
One would expect this to return -2, but instead you'll get a syntax error stating that it can't find a variable named y. This is because whitespace is ignored, so as far as my parser is concerned, that is equivalent to
```
int foo(:
	int y = (2 - y)
)
```
And since you're attempting to use a variable before assigning a value to it, you get that error. You can avoid this footgun by doing
```
int foo(:
	int y = 2,,
	-y
)
```
instead. This will return -2 as expected.

# Statement lists

`compoundExpression1 compoundExpression2` evaluates compoundExpression1 and returns the value from compoundExpression2.

# Scripts

An alias for statement lists.