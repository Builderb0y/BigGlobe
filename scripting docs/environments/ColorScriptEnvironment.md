This script environment allows an int to represent a color. How it does this is not explicitly defined, but there are several fields and functions available to extract and re-combine the components of the color.

For technical people who have worked with colors and bitwise encoding functions before, the following are guaranteed:
* Each component of the color (red, green, blue, and alpha) will have 8 bits of precision.
* All representable values per-component are equally distributed in sRGB color space. In other words, colors are never treated as linear, and they are fixed-point.

# Fields

* `int.redI`, `int.greenI`, `int.blueI`, and `int.alphaI` - returns the specified component of the color, as an int, in the range [0, 255].
* `int.redF`, `int.greenF`, `int.blueF`, and `int.alphaF` - returns the specified component of the color, as a float, in the range [0, 1]. This is equivalent to `int.(component)I / 255.0I`.
* `int.redD`, `int.greenD`, `int.blueD`, and `int.alphaD` - returns the specified component of the color, as a double, in the range [0, 1]. This is equivalent to `int.(component)I / 255.0L`.

# Functions

* `packI(int red, int green, int blue)` - creates an int to represent the specified color. The red, green, and blue parameters are clamped to the [0, 255] range before packing is performed.
* `packF(float red, float green, float blue)` - creates an int to represent the closest representable color to the provided color. The red, green, and blue parameters are clamped to the [0, 1] range before packing is performed.
* `packD(double red, double green, double blue)` - creates an int to represent the closest representable color to the provided color. The red, green, and blue parameters are clamped to the [0, 1] range before packing is performed.
* `packI(int red, int green, int blue, int alpha)` - creates an int to represent the specified color. The red, green, blue, and alpha parameters are clamped to the [0, 255] range before packing is performed.
* `packF(float red, float green, float blue, float alpha)` - creates an int to represent the closest representable color to the provided color. The red, green, blue, and alpha parameters are clamped to the [0, 1] range before packing is performed.
* `packD(double red, double green, double blue, double alpha)` - creates an int to represent the closest representable color to the provided color. The red, green, blue, and alpha parameters are clamped to the [0, 1] range before packing is performed.