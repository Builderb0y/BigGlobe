# Inner declarations

Scripts are made out of expressions, expressions are made out of terms, and terms may include declarations. This means a declaration is an expression, and a script. Which then means you can declare something anywhere a term, expression, or script is expected.
```
print(
	int i = 1
	int increment(: i + 1)
	"$:increment()"
)
```
Here I have declared a local variable and a method inside the arguments list for a function call. You can also declare classes similarly. The scope of these two declarations (the locations where you can call it from) is the parentheses surrounding it and the arguments list.

# Recursion

Recursion is kind of supported, but also kind of weird, because my parser lacks a linking stage; it links as it parses. Which means that if function a() is declared, then function b() is declared after it, then at the time a() is being parsed, it doesn't know b() exists yet. This means that:
* a() can call a().
* a() cannot call b().
* b() can call a().
* b() can call b().

There is also another even weirder workaround for this: if b() is declared *inside* a(), then b() can call a(), and any code inside a() that follows b()'s declaration can call b(). Example:
```
void a(:
	void b(:
		a()
	)
	b()
)
```

# Capturing

Functions and extension methods capture local variables declared before the function in the same scope as the function. Capturing works by prepending secret "hidden" parameters to the function. For example:
```
int x = 42
int multiply(int y:
	x * y
)
print(multiply(2))
```
will compile into
```java
int multiply(int x, int y) {
	return x * y;
}

void main() {
	int x = 42;
	System.out.println(multiply(x, 2));
}
```
This comes with some counter-intuitive behavior for scripts, because re-assigning a parameter will only re-assign it in the current function, but mutating it will affect all functions where it is accessible. This difference can be seen here in this example:
```
class Box(int value)
Box box = new(1)
int value = 2

void changeAndPrint(:
	box.value = 3 ;mutate
	value = 4 ;re-assign
	print("$.box.value, $value") ;prints "3, 4" as expected.
)

changeAndPrint()
print("$.box.value, $value") ;prints "3, 2" because box was mutated, but value was re-assigned.
```

# Postfix operators

There are none. So `x++` will not work anywhere. You must use `++x` instead.

# Generics

I'll be honest, I kind of forgot generics existed when I was planning my type system. As such, you can declare variables of type `List`, but not `List<String>`. However, generics still play a role in scripts: any expression with a generic type will have its implicit casts turned into explicit casts. For example:
```
ArrayList list = new().$add(2)
int two = list.(0) ;valid, because list.(index) has a generic return type.

Object object = list.(0)
int two = object ;not valid. you need to do int(object) or object.as(int) instead.
```