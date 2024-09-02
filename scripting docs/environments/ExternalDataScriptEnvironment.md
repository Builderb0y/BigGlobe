Added in V4.3.2, this environment allows scripts to read raw data from the `/data/modid/bigglobe_external/` folder. Files in this folder must be .dat files. In a sense, this is similar to ExternalImageScriptEnvironment, but more flexible to allow other types of data to reprsented besides just images.

# Fields

* `ExternalData.stride` - the current stride of the data.
* `ExternalData.offset` - the current offset of the data.

# Methods

* `ExternalData.stride(int newStride)` - returns a new ExternalData with the same data and offset as this one, but with the specified stride.
* `ExternalData.offset(int newOffset)` - returns a new ExternalData with the same data and stride as this one, but with the specified offset.
* `ExternalData.format(Class format)` - returns a new ExternalData with the same data and offset as this one, but with a stride set to the number of bytes used to represent the provided class. The provided class must be one of the following:
	* byte (stride = 1)
	* short (stride = 2)
	* char (stride = 2)
	* int (stride = 4)
	* long (stride = 8)
	* float (stride = 4)
	* double (stride = 8)
* `ExternalData.getByte(int index)` - returns the byte at `index * stride + offset`.
* `ExternalData.getShort(int index)` - interprets 2 bytes at `index * stride + offset` and `index * stride + offset + 1` as a single short, and returns it.
* `ExternalData.getChar(int index)` - interprets 2 bytes at `index * stride + offset` and `index * stride + offset + 1` as a single char, and returns it.
* `ExternalData.getInt(int index)` - interprets 4 bytes at `index * stride + offset` through `index * stride + offset + 3` as a single int, and returns it.
* `ExternalData.getLong(int index)` - interprets 8 bytes at `index * stride + offset` through `index * stride + offset + 7` as a single long, and returns it.
* `ExternalData.getFloat(int index)` - interprets 4 bytes at `index * stride + offset` through `index * stride + offset + 3` as a single float, and returns it.
* `ExternalData.getDouble(int index)` - interprets 8 bytes at `index * stride + offset` through `index * stride + offset + 7` as a single double, and returns it.

# Types

* `ExternalData` - represents some data loaded from `/data/modid/bigglobe_external/`.

# Casting

* `ExternalData(String id)` - the id must contain the namespace and path of the data to be loaded. The full path of the data loaded from your data pack(s) will be `/data/(namespace)/bigglobe_external/(path).dat`.

# Notes

When interpreting multiple bytes as a single number, the data stored in the .dat file is assumed to be in big endian order. In other words, the most significant byte should appear first in the file. Java's DataOutputStream uses this ordering by default, so if you're using that, you should have no issues. On the other hand, care must be taken to ensure the byte order is correct when creating files with other APIs or languages.

The stride defaults to 1, so if you are reading multiple multi-byte numbers sequentially, they will overlap. To show this visually, say we have the following bytes in our file, in hexadecimal:
```
01 02 03 04
```
Calling `getShort(0)` will return 0102, but calling `getShort(1)` will return 0203, not 0304 as one might expect, given that it's the "next" short in the file. Always be careful with your indexes, or set the stride when creating your ExternalData instance.
```
ExternalData data = ExternalData('modid:one_two_three_four').format(short)
```
Now, calling `data.getShort(0)` will return 0102, and calling `data.getShort(1)` will return 0304.

Getting a byte will return a value between -128 and +127, not 0 to 255. If a byte in the range 0 to 255 is desired, consider doing `getByte(index) & 16xFF` Similarly, shorts are in the range -32768 to +32767. If a short in the range 0 to 65535 is desired, bitwise and it with `16xFFFF`.