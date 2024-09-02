Added in V4.3.2, this environment allows scripts to read images from the `/data/modid/bigglobe_external/` folder. Images in this folder must be .png files.

# Fields

* `ExternalImage.width` - the width of the image.
* `ExternalImage.height` - the height of the image.

# Methods

* `ExternalImage.(int x, int y)` - returns the color of the pixel at the provided x and y coordinates. `image.(0, 0)` would return the pixel at the top left corner of the image. Higher values of x correspond to moving right along the image, where as higher values of y correspond to moving down along the image. If you want to get pixels where Y = 0 is at the bottom instead of the top, where increasing y moves up instead of down, the correct way to do this is `image.(x, image.height - 1 - y)`.

# Types

* `ExternalImage` - an image.

# Casting

* `ExternalImage(String id)` - the id must contain the namespace and path of the image. The full path of the image loaded from your data pack(s) will be `/data/(namespace)/bigglobe_external/(path).png`.