package builderb0y.bigglobe.noise;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.util.SemiThreadLocal;

/**
the common superinterface of all grids.
since grids can be anywhere from 1 to 3 dimensions,
their get() methods take different numbers of parameters.
as such, those methods cannot all be pushed into Grid.
however, the minimum and maximum values don't depend on the dimensionality.
so, that's what we provide here.

the minimum and maximum values of a grid could be used to apply dynamic bias to those values in
such a way that the "real" minimum or maximum value never exceeds a certain hard-coded value.
*/
public interface Grid {

	/**
	enabled by JUnit. MUST NOT BE ENABLED FROM ANYWHERE ELSE!
	when true, sub-interfaces do not create a Registry for their implementations.
	this aids in testing, since loading Registry-related classes would cause an
	error in a testing environment (cause not bootstrapped), but disabling Registry
	creation in a normal environment would also crash when creating an AutoCoder for grids.

	this field is a MutableBoolean instead of a boolean for the sole reason that
	fields in interfaces are implicitly final, which is undesired for this use case.
	*/
	@TestOnly
	public static final MutableBoolean TESTING = new MutableBoolean(false);

	public abstract double minValue();

	public abstract double maxValue();

	/**
	returns an array whose length is guaranteed to be greater than (minimumLength).
	the contents of the returned array are not specified,
	and the caller should make sure to completely overwrite them before reading them.

	the intended use case for scratch arrays are for Grid
	implementations whose bulk getter methods need a place to store
	intermediate values before accumulating them onto another array.

	the returned array should be {@link #reclaimScratchArray(double[]) reclaimed}
	after the caller no longer needs it.
	*/
	public static double[] getScratchArray(int minimumLength) {
		return ScratchArrays.get(minimumLength);
	}

	/**
	indicates that the caller no longer has any need for the provided array.
	the array may be returned by {@link #getScratchArray(int)} after this method returns.
	additionally, the contents of the array may be
	modified at any point after this method is called.
	as such, it is the responsibility of the caller to make sure that
	they do not query the contents of the array after calling this method.
	*/
	public static void reclaimScratchArray(double[] array) {
		ScratchArrays.reclaim(array);
	}

	public static class ScratchArrays {

		public static final Logger LOGGER = LoggerFactory.getLogger(BigGlobeMod.MODNAME + "/ScratchArrays");

		/**
		when true, will log attempts to {@link #get(int) get}
		or {@link #reclaim(double[]) reclaim} a scratch
		array with a length greater than {@link #maxLength}

		the default value for this field is true, but it can be
		overridden by {@systemProperty bigglobe.ScratchArrays.logging}.
		*/
		public static final boolean loggingEnabled = getProperty("logging", Boolean.TRUE, s -> {
			if ("true".equalsIgnoreCase(s)) return Boolean.TRUE;
			if ("false".equalsIgnoreCase(s)) return Boolean.FALSE;
			throw new IllegalArgumentException("Not true/false: " + s);
		});

		/**
		the minimum length of arrays that caching will be used for.
		if the requested length for {@link #get(int)} is less
		than this value, a new array will always be allocated.
		likewise, if {@link #reclaim(double[])} is called with an array whose
		length is less than this value, the array will be silently discarded.

		enforcing a minimum length like this can help with performance
		in the case where many small arrays are needed for short time periods,
		as the time to acquire a mutex and, in the case of reclaiming,
		zero out the array, is completely eliminated.
		it is true what they say about java's memory management:
		allocation is faster than re-use for small objects.

		the default value of this field is 17, but it can be overridden
		by {@systemProperty bigglobe.ScratchArrays.minLength}.
		the overridden value must be strictly non-negative (greater than or equal to 0).
		setting this field to 0 will mean all array lengths are cached.
		*/
		public static final int minLength = getProperty("minLength", 17, ScratchArrays::parseNonNegativeInt);

		/**
		the maximum length of arrays that caching will be used for.
		if the requested length for {@link #get(int)} is greater
		than this value, a new array will always be allocated,
		and a warning message will be printed if {@link #loggingEnabled} is set to true.
		likewise, if {@link #reclaim(double[])} is called with an array whose
		length is greater than this value, the array will be discarded,
		and a warning message will be printed if {@link #loggingEnabled} is set to true.

		the default value of this field is 2048, but it can be overridden
		by {@systemProperty bigglobe.ScratchArrays.maxLength}.
		the overridden value must be strictly non-negative (greater than or equal to 0).
		setting this field to 0 will effectively disable array caching.
		*/
		public static final int maxLength = getProperty("maxLength", 2048, ScratchArrays::parseNonNegativeInt);

		/**
		the maximum number of arrays that can be {@link #reclaim(double[]) reclaimed}.

		the default value of this field is 8, but it can be overridden
		by {@systemProperty bigglobe.ScratchArrays.maxCount}.
		the overridden value must be strictly non-negative (greater than or equal to 0).
		setting this field to 0 will effectively disable array caching.
		*/
		public static final int maxCount = getProperty("maxCount", 8, ScratchArrays::parseNonNegativeInt);

		public static <T> T getProperty(String name, T defaultValue, Function<String, T> parser) {
			String fullName = BigGlobeMod.MODID + ".ScratchArrays." + name;
			String property = System.getProperty(fullName);
			if (property != null) try {
				T value = parser.apply(property);
				LOGGER.debug("-D" + fullName + '=' + value);
				return value;
			}
			catch (RuntimeException exception) {
				LOGGER.warn("-D" + fullName + '=' + property + ", which is invalid: " + exception + "; defaulting to " + defaultValue);
				return defaultValue;
			}
			else {
				LOGGER.debug("-D" + fullName + " is not set. Defaulting to " + defaultValue);
				return defaultValue;
			}
		}

		public static int parseNonNegativeInt(String s) {
			int value = Integer.parseInt(s);
			if (value >= 0) return value;
			else throw new IllegalArgumentException("Must be greater than or equal to 0: " + value);
		}

		//////////////////////////////// now the actual fun can begin ////////////////////////////////

		public static final SemiThreadLocal<List<double[]>> SCRATCH_ARRAYS = SemiThreadLocal.soft(4, () -> new ObjectArrayList<>(maxCount));

		/**
		returns a scratch array whose length is guaranteed
		to be greater than or equal to (minimumLength).

		if the (minimumLength) is greater than {@link #maxLength},
		then a new array of length (minimumLength) will be allocated and returned.
		otherwise, we try to find a suitable array in {@link #SCRATCH_ARRAYS}.
			if an array of suitable length is found, it is returned.
			otherwise, a new array of length (minimumLength) is allocated and returned.
		*/
		public static double[] get(int minimumLength) {
			fallback: {
				if (minimumLength < minLength) {
					break fallback;
				}
				if (minimumLength > maxLength) {
					if (loggingEnabled) LOGGER.warn("Attempt to get a scratch array of length " + minimumLength + " exceeds maximum length of " + maxLength, new Throwable("Stack trace"));
					break fallback;
				}
				List<double[]> list = SCRATCH_ARRAYS.get();
				try {
					for (int index = list.size(); --index >= 0; ) {
						double[] array = list.get(index);
						if (array.length >= minimumLength) {
							if (index == list.size() - 1) list.remove(index);
							else list.set(index, list.remove(list.size() - 1));
							return array;
						}
					}
				}
				finally {
					SCRATCH_ARRAYS.reclaim(list);
				}
			}
			return new double[minimumLength];
		}

		/**
		indicates that the provided scratch array is no longer being used,
		and is free to be re-used next time a scratch array is needed.

		if the array's length is greater than {@link #maxLength},
		then no action is performed, except possibly logging
		if logging is {@link #loggingEnabled enabled}.
		otherwise:
			if the List in {@link #SCRATCH_ARRAYS} is currently NOT full
			(meaning its {@link List#size()} is less than {@link #maxCount}),
			then the array to be reclaimed is added to the List in {@link #SCRATCH_ARRAYS}.
			otherwise:
				we attempt to find an array in {@link #SCRATCH_ARRAYS}
				whose length is less than that of the array to be reclaimed.
				if such an array is found, it is replaced by the array to be reclaimed.
				otherwise, no action is performed.
		*/
		public static void reclaim(double[] array) {
			if (array.length < minLength) {
				return;
			}
			if (array.length > maxLength) {
				if (loggingEnabled) LOGGER.warn("Attempt to reclaim a scratch array of length " + array.length + " exceeds maximum length of " + maxLength, new Throwable("Stack trace"));
				return;
			}
			List<double[]> list = SCRATCH_ARRAYS.get();
			try {
				//apparently compressed RAM is now a thing some OS's have,
				//so we'll fill this with all 0's so it'll compress better.
				Arrays.fill(array, 0.0D);
				if (list.size() < maxCount) {
					list.add(array);
				}
				else for (int index = list.size(); --index >= 0; ) {
					if (list.get(index).length < array.length) {
						list.set(index, array);
						break;
					}
				}
			}
			finally {
				SCRATCH_ARRAYS.reclaim(list);
			}
		}
	}
}