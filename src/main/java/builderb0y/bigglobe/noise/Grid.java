package builderb0y.bigglobe.noise;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.TestOnly;

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
}