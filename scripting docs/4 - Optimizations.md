# Constant folding

Expressions of the form `(unary operator) constant` or `constant (binary operator) constant` are computed at compile-time, meaning that writing `int x = 2 + 2` will NOT be any slower than writing `int x = 4`.

# Exponents

Expressions of the form `something ^ 2` get replaced with multiplication. No functions are called in this process, and no temporary local variables are used. This means it's about as fast as it can possibly be. Use this as much as you want. There is no need to re-invent the wheel with your own square() function.

Expressions of the form `something ^ (constant integer)` get replaced with repeated multiplication and squaring. And division if the exponent is negative. For example, `x ^ -5` gets replaced with `1.0 / (((x ^ 2) ^ 2) * x)`.

Expressions of the form `(constant float or double base) ^ (non-constant float or double exponent)` get replaced with `exp(exponent * ln(base))`. In my testing, this was faster than using `pow()`.

# Constant factories

Some of Minecraft's objects like blocks or block states can be specified with strings. See MinecraftScriptEnvironment.md for more details. The point here is that if that string is constant, then the object will be too. It will only be looked up in Minecraft's registries once, not once every time it's used.

# Cascading casting

Some structures like if statements, ternaries, switch statements, etc... support a concept called cascading casting. This means that when you try to cast them to something, every individual branch gets cast to that thing, as opposed to calculating each branch and then casting.

This means that if you did something like
```
BlockState state = condition ? 'minecraft:mossy_cobblestone' : 'minecraft:cobblestone'
```
then this would get compiled into
```
BlockState state = condition ? BlockState('minecraft:mossy_cobblestone') : BlockState('minecraft:cobblestone')
```
instead of
```
BlockState state = BlockState(condition ? 'minecraft:mossy_cobblestone' : 'minecraft:cobblestone')
```
And the significance of that is that since both strings are constant, both block states are also constant; they are only ever looked up once, not once every time this line of code runs.

By comparison,
```
String string = 'minecraft:stone'
BlockState state = string
```
This would *not* count as constant, and the state would be looked up every time. Avoid doing this if you can. In fact, you will receive a warning in your log file when you do this, just in case you did it by accident. If you really did mean to do it on purpose, you can suppress this warning with an explicit cast, like so: `BlockState state = BlockState(string)`.