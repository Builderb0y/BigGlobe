package builderb0y.bigglobe.columns.scripted.entries;

import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.bigglobe.columns.scripted.*;
import builderb0y.bigglobe.columns.scripted.AccessSchema.AccessContext;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.MutableDependencyView;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.scripting.bytecode.FieldCompileContext;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.BooleanToConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.ConstantConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.IntCompareConditionTree;
import builderb0y.scripting.bytecode.tree.flow.IfElseInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.NewArrayWithLengthInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.BitwiseOrInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

@AddPseudoField("decodeContext")
public abstract class AbstractColumnEntry implements ColumnEntry, MutableDependencyView {

	public static final ColumnEntryMemory.Key<MethodCompileContext>
		COMPUTE_ONE         = new ColumnEntryMemory.Key<>("computeOne"),
		COMPUTE_ALL_TEST    = new ColumnEntryMemory.Key<>("computeAllTest"),
		COMPUTE_ALL_NO_TEST = new ColumnEntryMemory.Key<>("computeAllNoTest"),
		EXTRACT             = new ColumnEntryMemory.Key<>("extract"),
		VALID_MIN_Y         = new ColumnEntryMemory.Key<>("validMinY"),
		VALID_MAX_Y         = new ColumnEntryMemory.Key<>("validMaxY");

	public final AccessSchema params;
	public final @VerifyNullable Valid valid;
	public final @DefaultBoolean(true) boolean cache;

	public final transient Set<RegistryEntry<? extends DependencyView>> dependencies;

	public AbstractColumnEntry(AccessSchema params, @VerifyNullable Valid valid, @DefaultBoolean(true) boolean cache, DecodeContext<?> decodeContext) {
		this.params = params;
		this.valid = valid;
		this.cache = cache;
		this.dependencies = new HashSet<>();
		if (params.is_3d() && cache && (valid == null || valid.min_y() == null || valid.max_y() == null)) {
			decodeContext.logger().logError("Upper or lower bound not specified, and caching is enabled. This may result in poor worldgen performance, as it may compute more Y levels than intended.");
		}
	}

	@Override
	public Set<RegistryEntry<? extends DependencyView>> getDependencies() {
		return this.dependencies;
	}

	public @Nullable DecodeContext<?> decodeContext() {
		return null;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return this.params;
	}

	@Override
	public boolean hasField() {
		return this.cache;
	}

	public boolean hasValid() {
		return this.valid != null && this.valid.isUseful(this.getAccessSchema().is_3d());
	}

	public boolean is3D() {
		return this.getAccessSchema().is_3d();
	}

	@Override
	public void setupMemory(ColumnEntryMemory memory, DataCompileContext context) {
		ColumnEntry.super.setupMemory(memory, context);
		String internalName = memory.getTyped(ColumnEntryMemory.INTERNAL_NAME);
		AccessContext accessContext = memory.getTyped(ColumnEntryMemory.ACCESS_CONTEXT);
		if (this.is3D()) {
			if (this.hasField()) {
				MethodCompileContext computeAllTest = context.mainClass.newMethod(ACC_PUBLIC, "computeAllTest_" + internalName, TypeInfos.VOID);
				memory.putTyped(COMPUTE_ALL_TEST, computeAllTest);
				MethodCompileContext computeAllNoTest = context.mainClass.newMethod(ACC_PUBLIC, "computeAllNoTest_" + internalName, TypeInfos.VOID);
				memory.putTyped(COMPUTE_ALL_NO_TEST, computeAllNoTest);
				MethodCompileContext extractMethod = context.mainClass.newMethod(ACC_PUBLIC, "extract_" + internalName, accessContext.exposedType(), new LazyVarInfo("y", TypeInfos.INT));
				memory.putTyped(EXTRACT, extractMethod);
			}
			if (this.hasValid()) {
				if (this.valid.where() != null) {
					MethodCompileContext test = context.mainClass.newMethod(ACC_PUBLIC, "test_" + internalName, TypeInfos.BOOLEAN);
					memory.putTyped(ColumnEntryMemory.VALID_WHERE, test);
				}
				if (this.valid.min_y() != null) {
					MethodCompileContext minY = context.mainClass.newMethod(ACC_PUBLIC, "minY_" + internalName, TypeInfos.INT);
					memory.putTyped(VALID_MIN_Y, minY);
				}
				if (this.valid.max_y() != null) {
					MethodCompileContext maxY = context.mainClass.newMethod(ACC_PUBLIC, "maxY_" + internalName, TypeInfos.INT);
					memory.putTyped(VALID_MAX_Y, maxY);
				}
			}
			if (this.hasField() || this.hasValid()) {
				MethodCompileContext computeOneMethod = context.mainClass.newMethod(ACC_PUBLIC, "computeOne_" + internalName, accessContext.exposedType(), new LazyVarInfo("y", TypeInfos.INT));
				memory.putTyped(COMPUTE_ONE, computeOneMethod);
			}
		}
		else {
			if (this.hasValid()) {
				MethodCompileContext actualComputer = context.mainClass.newMethod(ACC_PUBLIC, "computeTest_" + internalName, accessContext.exposedType());
				memory.putTyped(ColumnEntryMemory.COMPUTE_TEST, actualComputer);
				MethodCompileContext testMethod = context.mainClass.newMethod(ACC_PUBLIC, "test_" + internalName, TypeInfos.BOOLEAN);
				memory.putTyped(ColumnEntryMemory.VALID_WHERE, testMethod);
			}
			if (this.hasField()) {
				MethodCompileContext computeNoTest = context.mainClass.newMethod(ACC_PUBLIC, "computeNoTest_" + internalName, accessContext.commonType());
				memory.putTyped(ColumnEntryMemory.COMPUTE_NO_TEST, computeNoTest);
			}
		}
	}

	@Override
	public void populateField(ColumnEntryMemory memory, DataCompileContext context, FieldCompileContext getterMethod) {
		ColumnEntry.super.populateField(memory, context, getterMethod);
		if (this.is3D()) {
			AccessContext accessContext = memory.getTyped(ColumnEntryMemory.ACCESS_CONTEXT);
			boolean primitive = accessContext.exposedType().isPrimitive();
			Class<?> arrayClass = primitive ? MappedRangeNumberArray.class : MappedRangeObjectArray.class;
			context.constructor.appendCode(
				"valueField = Array.new(empty)",
				(MutableScriptEnvironment environment) -> {
					environment
					.addVariableRenamedGetField(context.loadSelf(), "valueField", memory.getTyped(ColumnEntryMemory.FIELD).info)
					.addType("Array", arrayClass)
					.addQualifiedConstructor(arrayClass)
					.addVariable(
						"empty",
						primitive
						? getStatic(FieldInfo.getField(NumberArray.class, "EMPTY_" + accessContext.exposedType().getSort().name()))
						: new NewArrayWithLengthInsnTree(accessContext.exposedType(), ldc(0))
					)
					;
				}
			);
		}
	}

	@Override
	public void populateGetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext getterMethod) {
		ColumnEntry.super.populateGetter(memory, context, getterMethod);
		if (this.is3D()) {
			if (this.hasField()) {
				this.populateGetterWithField3D(memory, context, getterMethod);
			}
			else {
				this.populateGetterWithoutField3D(memory, context, getterMethod);
			}
		}
		else {
			if (this.hasField()) {
				this.populateGetterWithField2D(memory, context, getterMethod);
			}
			else {
				this.populateGetterWithoutField2D(memory, context, getterMethod);
			}
		}
	}

	public void populateGetterWithField3D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext getterMethod) {
		MethodCompileContext preComputeMethod = memory.getTyped(ColumnEntryMemory.PRE_COMPUTER);
		MethodCompileContext extractMethod = memory.getTyped(EXTRACT);

		invokeInstance(context.loadSelf(), preComputeMethod.info).emitBytecode(getterMethod);
		return_(invokeInstance(context.loadSelf(), extractMethod.info, load("y", TypeInfos.INT))).emitBytecode(getterMethod);
		getterMethod.endCode();

		this.populateExtract(memory, context, extractMethod);
	}

	public void populateGetterWithoutField3D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext getterMethod) {
		if (this.hasValid()) {
			ConditionTree condition = ConstantConditionTree.TRUE;
			InsnTree y = load("y", TypeInfos.INT);
			if (this.valid.where() != null) {
				MethodCompileContext test = memory.getTyped(ColumnEntryMemory.VALID_WHERE);
				condition = and(condition, new BooleanToConditionTree(invokeInstance(context.loadSelf(), test.info)));
			}
			if (this.valid.min_y() != null) {
				MethodCompileContext minY = memory.getTyped(VALID_MIN_Y);
				condition = and(condition, IntCompareConditionTree.greaterThanOrEqual(y, invokeInstance(context.loadSelf(), minY.info)));
			}
			if (this.valid.max_y() != null) {
				MethodCompileContext maxY = memory.getTyped(VALID_MAX_Y);
				condition = and(condition, IntCompareConditionTree.lessThan(y, invokeInstance(context.loadSelf(), maxY.info)));
			}
			MethodCompileContext computeOneMethod = memory.getTyped(COMPUTE_ONE);

			LazyVarInfo self = new LazyVarInfo("this", getterMethod.clazz.info);
			new IfElseInsnTree(
				condition,
				return_(invokeInstance(load(self), computeOneMethod.info, y)),
				return_(this.valid.getFallback(this.params.type(), context.root())),
				TypeInfos.VOID
			)
			.emitBytecode(getterMethod);
			getterMethod.endCode();
		}
	}

	public void populateGetterWithField2D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext getterMethod) {
		MethodCompileContext preComputeMethod = memory.getTyped(ColumnEntryMemory.PRE_COMPUTER);
		FieldCompileContext valueField = memory.getTyped(ColumnEntryMemory.FIELD);
		invokeInstance(context.loadSelf(), preComputeMethod.info).emitBytecode(getterMethod);
		return_(getField(context.loadSelf(), valueField.info)).emitBytecode(getterMethod);
		getterMethod.endCode();

		if (this.hasValid()) {
			MethodCompileContext computeTest = memory.getTyped(ColumnEntryMemory.COMPUTE_TEST);
			MethodCompileContext computeNoTest = memory.getTyped(ColumnEntryMemory.COMPUTE_NO_TEST);
			MethodCompileContext testMethod = memory.getTyped(ColumnEntryMemory.VALID_WHERE);

			computeTest.setCode(
				"return(test() ? compute() : fallback)",
				(MutableScriptEnvironment environment) -> {
					environment
					.addFunctionInvoke("test", context.loadSelf(), testMethod.info)
					.addFunctionInvoke("compute", context.loadSelf(), computeNoTest.info)
					.addVariable("fallback", this.valid.getFallback(this.params.type(), context.root()))
					;
				}
			);
		}
	}

	public void populateGetterWithoutField2D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext getterMethod) {
		if (this.hasValid()) {
			MethodCompileContext computeNoTest = memory.getTyped(ColumnEntryMemory.COMPUTE_NO_TEST);
			MethodCompileContext testMethod = memory.getTyped(ColumnEntryMemory.VALID_WHERE);

			getterMethod.setCode(
				"return(test() ? compute() : fallback)",
				(MutableScriptEnvironment environment) -> {
					environment
					.addFunctionInvoke("test", context.loadSelf(), testMethod.info)
					.addFunctionInvoke("compute", context.loadSelf(), computeNoTest.info)
					.addVariable("fallback", this.valid.getFallback(this.params.type(), context.root()))
					;
				}
			);
		}
	}

	@Override
	@MustBeInvokedByOverriders
	public void populatePreComputer(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext preComputeMethod) {
		ColumnEntry.super.populatePreComputer(memory, context, preComputeMethod);
		if (this.is3D()) {
			this.populatePreCompute3D(memory, context, preComputeMethod);
		}
		else {
			this.populatePreCompute2D(memory, context, preComputeMethod);
		}
	}

	public void populatePreCompute2D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext preComputeMethod) {
		int flagsIndex = memory.getTyped(ColumnEntryMemory.FLAGS_INDEX);
		FieldCompileContext valueField = memory.getTyped(ColumnEntryMemory.FIELD);
		MethodCompileContext compute = memory.getTyped(this.hasValid() ? ColumnEntryMemory.COMPUTE_TEST : ColumnEntryMemory.COMPUTE_NO_TEST);
		preComputeMethod.setCode(
			"""
			int oldFlags = flagsField
			int newFlags = oldFlags | flagsBitmask
			if (oldFlags != newFlags:
				flagsField = newFlags
				value = compute()
			)
			""",
			(MutableScriptEnvironment environment) -> {
				environment
				.addVariableRenamedGetField(context.loadSelf(), "flagsField", context.flagsField(flagsIndex))
				.addVariableConstant("flagsBitmask", DataCompileContext.flagsFieldBitmask(flagsIndex))
				.addFunctionInvoke("compute", context.loadSelf(), compute.info)
				.addVariableRenamedGetField(context.loadSelf(), "value", valueField.info)
				;
			}
		);
	}

	public void populatePreCompute3D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext preComputeMethod) {
		int flagsIndex = memory.getTyped(ColumnEntryMemory.FLAGS_INDEX);
		FieldCompileContext valueField = memory.getTyped(ColumnEntryMemory.FIELD);
		MethodCompileContext compute = memory.getTyped(this.hasValid() ? COMPUTE_ALL_TEST : COMPUTE_ALL_NO_TEST);
		preComputeMethod.setCode(
			"""
			int oldFlags = flagsField
			int newFlags = oldFlags | flagsBitmask
			if (oldFlags != newFlags:
				flagsField = newFlags
				compute()
			)
			""",
			(MutableScriptEnvironment environment) -> {
				environment
				.addVariableRenamedGetField(context.loadSelf(), "flagsField", context.flagsField(flagsIndex))
				.addVariableConstant("flagsBitmask", DataCompileContext.flagsFieldBitmask(flagsIndex))
				.addFunctionInvoke("compute", context.loadSelf(), compute.info)
				.addVariableRenamedGetField(context.loadSelf(), "value", valueField.info)
				;
			}
		);

		MethodCompileContext computeAllTestMethod = memory.getTyped(COMPUTE_ALL_TEST);
		MethodCompileContext computeAllNoTestMethod = memory.getTyped(COMPUTE_ALL_NO_TEST);
		String computeSource = this.getComputeSource();
		computeAllTestMethod.setCode(computeSource, (MutableScriptEnvironment environment) -> {
			environment
			.addVariableRenamedGetField(context.loadSelf(), "valueField", valueField.info)
			.addMethodInvokes(MappedRangeArray.class, "reallocateNone", "reallocateMin", "reallocateMax", "reallocateBoth", "invalidate")
			.addVariable("this", context.loadSelf())
			.addVariable("column", context.loadColumn())
			.addFunctionInvoke("actuallyCompute", context.loadSelf(), computeAllNoTestMethod.info)
			;
			if (this.valid != null) {
				if (this.valid.where() != null) {
					MethodCompileContext test = memory.getTyped(ColumnEntryMemory.VALID_WHERE);
					environment.addFunctionInvoke("test", context.loadSelf(), test.info);
				}
				if (this.valid.min_y() != null) {
					MethodCompileContext minY = memory.getTyped(VALID_MIN_Y);
					environment.addFunctionInvoke("minY", context.loadSelf(), minY.info);
				}
				if (this.valid.max_y() != null) {
					MethodCompileContext maxY = memory.getTyped(VALID_MAX_Y);
					environment.addFunctionInvoke("maxY", context.loadSelf(), maxY.info);
				}
			}
		});
		this.populateComputeAll(memory, context, computeAllNoTestMethod);
	}

	public void populateExtract(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext extractMethod) {
		AccessContext accessContext = memory.getTyped(ColumnEntryMemory.ACCESS_CONTEXT);
		extractMethod.setCode(
			this.extractSource(),
			(MutableScriptEnvironment environment) -> {
				environment
				.addVariableLoad("y", TypeInfos.INT)
				.addVariable("arrayField", getField(context.loadSelf(), memory.getTyped(ColumnEntryMemory.FIELD).info))
				.addFieldGet("valid", MappedRangeArray.VALID)
				.addFieldGet("minCached", MappedRangeArray.MIN_CACHED)
				.addFieldGet("maxCached", MappedRangeArray.MAX_CACHED)
				.addFieldGet("minAccessible", MappedRangeArray.MIN_ACCESSIBLE)
				.addFieldGet("maxAccessible", MappedRangeArray.MAX_ACCESSIBLE)
				.addFieldGet("array", MappedRangeNumberArray.ARRAY)
				.addMethodInvoke("get", switch (accessContext.exposedType().getSort()) {
					case BYTE    -> MappedRangeNumberArray.GET_B;
					case SHORT   -> MappedRangeNumberArray.GET_S;
					case INT     -> MappedRangeNumberArray.GET_I;
					case LONG    -> MappedRangeNumberArray.GET_L;
					case FLOAT   -> MappedRangeNumberArray.GET_F;
					case DOUBLE  -> MappedRangeNumberArray.GET_D;
					case BOOLEAN -> MappedRangeNumberArray.GET_Z;
					default -> throw new IllegalStateException("Unsupported type: " + accessContext);
				})
				.addVariable("fallback", this.valid != null ? this.valid.getFallback(this.params.type(), context.root()) : ldc(0))
				.addFunctionInvoke("compute", context.loadSelf(), memory.getTyped(COMPUTE_ONE).info)
				;
			}
		);
	}

	public String extractSource() {
		if (this.hasValid()) {
			if (this.valid.where() != null) {
				if (this.valid.min_y() != null) {
					if (this.valid.max_y() != null) {
						return """
							var array = arrayField
							unless (array.valid: return(fallback))
							if (y >= array.minCached && y < array.maxCached:
								return(array.array.get(y - array.minCached))
							)
							if (y >= array.minAccessible && y < array.maxAccessible:
								return(compute(y))
							)
							return(fallback)
						""";
					}
					else {
						return """
							var array = arrayField
							unless (array.valid: return(fallback))
							if (y >= array.minCached && y < array.maxCached:
								return(array.array.get(y - array.minCached))
							)
							if (y >= array.minAccessible:
								return(compute(y))
							)
							return(fallback)
						""";
					}
				}
				else {
					if (this.valid.max_y() != null) {
						return """
							var array = arrayField
							unless (array.valid: return(fallback))
							if (y >= array.minCached && y < array.maxCached:
								return(array.array.get(y - array.minCached))
							)
							if (y < array.maxAccessible:
								return(compute(y))
							)
							return(fallback)
						""";
					}
					else {
						return """
							var array = arrayField
							unless (array.valid: return(fallback))
							if (y >= array.minCached && y < array.maxCached:
								return(array.array.get(y - array.minCached))
							)
							return(compute(y))
						""";
					}
				}
			}
			else {
				if (this.valid.min_y() != null) {
					if (this.valid.max_y() != null) {
						return """
							var array = arrayField
							if (y >= array.minCached && y < array.maxCached:
								return(array.array.get(y - array.minCached))
							)
							if (y >= array.minAccessible && y < array.maxAccessible:
								return(compute(y))
							)
							return(fallback)
						""";
					}
					else {
						return """
							var array = arrayField
							if (y >= array.minCached && y < array.maxCached:
								return(array.array.get(y - array.minCached))
							)
							if (y >= array.minAccessible:
								return(compute(y))
							)
							return(fallback)
						""";
					}
				}
				else {
					if (this.valid.max_y() != null) {
						return """
							var array = arrayField
							if (y >= array.minCached && y < array.maxCached:
								return(array.array.get(y - array.minCached))
							)
							if (y < array.maxAccessible:
								return(compute(y))
							)
							return(fallback)
						""";
					}
					else {
						return """
							var array = arrayField
							if (y >= array.minCached && y < array.maxCached:
								return(array.array.get(y - array.minCached))
							)
							return(compute(y))
						""";
					}
				}
			}
		}
		else {
			return """
				var array = arrayField
				if (y >= array.minCached && y < array.maxCached:
					return(array.array.get(y - array.minCached))
				)
				return(compute(y))
			""";
		}
	}

	public String getComputeSource() {
		if (this.hasValid()) {
			if (this.valid.where() != null) {
				if (this.valid.min_y() != null) {
					if (this.valid.max_y() != null) {
						return """
							if (test():
								valueField.reallocateBoth(column, minY(), maxY())
								actuallyCompute()
							)
							else (
								valueField.invalidate()
							)
						""";
					}
					else {
						return """
							if (test():
								valueField.reallocateMin(column, minY())
								actuallyCompute()
							)
							else (
								valueField.invalidate()
							)
						""";
					}
				}
				else {
					if (this.valid.max_y() != null) {
						return """
							if (test():
								valueField.reallocateMax(column, maxY())
								actuallyCompute()
							)
							else (
								valueField.invalidate()
							)
						""";
					}
					else {
						return """
							if (test():
								valueField.reallocateNone(column, )
								actuallyCompute()
							)
							else (
								valueField.invalidate()
							)
						""";
					}
				}
			}
			else {
				if (this.valid.min_y() != null) {
					if (this.valid.max_y() != null) {
						return """
							valueField.reallocateBoth(column, minY(), maxY())
							actuallyCompute()
						""";
					}
					else {
						return """
							valueField.reallocateMin(column, minY())
							actuallyCompute()
						""";
					}
				}
				else {
					if (this.valid.max_y() != null) {
						return """
							valueField.reallocateMax(column, maxY())
							actuallyCompute()
						""";
					}
					else {
						return """
							valueField.reallocateNone(column)
							actuallyCompute()
						""";
					}
				}
			}
		}
		else {
			return """
				valueField.reallocateNone(column)
				actuallyCompute()
			""";
		}
	}

	public void populateComputeAll(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeAllMethod) {
		AccessContext accessContext = memory.getTyped(ColumnEntryMemory.ACCESS_CONTEXT);
		computeAllMethod.setCode(
			"""
			var array = valueField
			int minY = array.minCached
			int maxY = array.maxCached
			var actualArray = array.array
			for (int y = minY, y < maxY, ++y:
				actualArray.set(y - minY, compute(y))
			)
			""",
			(MutableScriptEnvironment environment) -> {
				environment
				.addVariableRenamedGetField(context.loadSelf(), "valueField", memory.getTyped(ColumnEntryMemory.FIELD).info)
				.addFieldGet("minCached", MappedRangeArray.MIN_CACHED)
				.addFieldGet("maxCached", MappedRangeArray.MAX_CACHED)
				.addFieldGet("array", MappedRangeNumberArray.ARRAY)
				.addMethodInvoke("set", switch (accessContext.exposedType().getSort()) {
					case BYTE -> MappedRangeNumberArray.SET_B;
					case SHORT -> MappedRangeNumberArray.SET_S;
					case INT -> MappedRangeNumberArray.SET_I;
					case LONG -> MappedRangeNumberArray.SET_L;
					case FLOAT -> MappedRangeNumberArray.SET_F;
					case DOUBLE -> MappedRangeNumberArray.SET_D;
					case BOOLEAN -> MappedRangeNumberArray.SET_Z;
					default -> throw new IllegalStateException("Unsupported type: " + accessContext);
				})
				.addFunctionInvoke("compute", context.loadSelf(), memory.getTyped(COMPUTE_ONE).info)
				;
			}
		);
	}

	@Override
	public void populateSetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext setterMethod) {
		ColumnEntry.super.populateSetter(memory, context, setterMethod);
		if (this.getAccessSchema().is_3d()) {
			this.populateSetter3D(memory, context, setterMethod);
		}
		else {
			this.populateSetter2D(memory, context, setterMethod);
		}
	}

	public void populateSetter2D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext setterMethod) {
		LazyVarInfo value = new LazyVarInfo("value", memory.getTyped(ColumnEntryMemory.ACCESS_CONTEXT).exposedType());
		FieldCompileContext valueField = memory.getTyped(ColumnEntryMemory.FIELD);
		int flagsIndex = memory.getTyped(ColumnEntryMemory.FLAGS_INDEX);
		FieldInfo flagsField = context.flagsField(flagsIndex);
		int bitMask = DataCompileContext.flagsFieldBitmask(flagsIndex);
		//flagsField = flagsField | bitMask
		putField(context.loadSelf(), flagsField, new BitwiseOrInsnTree(getField(context.loadSelf(), flagsField), ldc(bitMask), IOR)).emitBytecode(setterMethod);
		//valueField = value
		putField(context.loadSelf(), valueField.info, load(value)).emitBytecode(setterMethod);
		//return
		return_(noop).emitBytecode(setterMethod);
		setterMethod.endCode();
	}

	public void populateSetter3D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext setterMethod) {
		AccessContext accessContext = memory.getTyped(ColumnEntryMemory.ACCESS_CONTEXT);
		Integer flagsIndex = memory.getTyped(ColumnEntryMemory.FLAGS_INDEX);
		setterMethod.setCode(
			"""
			flags = flags | flagBitmask
			var array = valueField
			if (y >= array.minCached && y < array.maxCached:
				array.array.set(y - array.minCached, value)
			)
			""",
			(MutableScriptEnvironment environment) -> {
				environment
				.addVariableRenamedGetField(context.loadSelf(), "valueField", memory.getTyped(ColumnEntryMemory.FIELD).info)
				.addVariableRenamedGetField(context.loadSelf(), "flags", context.flagsField(flagsIndex))
				.addVariableConstant("flagBitmask", DataCompileContext.flagsFieldBitmask(flagsIndex))
				.addVariableLoad("y", TypeInfos.INT)
				.addVariableLoad("value", accessContext.exposedType())
				.addFieldGet("minCached", MappedRangeArray.MIN_CACHED)
				.addFieldGet("maxCached", MappedRangeArray.MAX_CACHED)
				.addFieldGet("array", MappedRangeNumberArray.ARRAY)
				.addMethodInvoke("set", switch (accessContext.exposedType().getSort()) {
					case BYTE    -> MappedRangeNumberArray.SET_B;
					case SHORT   -> MappedRangeNumberArray.SET_S;
					case INT     -> MappedRangeNumberArray.SET_I;
					case LONG    -> MappedRangeNumberArray.SET_L;
					case FLOAT   -> MappedRangeNumberArray.SET_F;
					case DOUBLE  -> MappedRangeNumberArray.SET_D;
					case BOOLEAN -> MappedRangeNumberArray.SET_Z;
					default -> throw new IllegalStateException("Unsupported type: " + accessContext);
				})
				;
			}
		);
	}

	@Override
	public void emitComputer(ColumnEntryMemory memory, DataCompileContext context) throws ScriptParsingException {
		if (this.getAccessSchema().is_3d()) {
			this.emitCompute3D(memory, context);
		}
		else {
			this.emitCompute2D(memory, context);
		}
	}

	public void emitCompute2D(ColumnEntryMemory memory, DataCompileContext context) throws ScriptParsingException {
		if (this.valid != null && this.valid.where() != null) {
			context.setMethodCode(memory.getTyped(ColumnEntryMemory.VALID_WHERE), this.valid.where(), false, this, memory.getTyped(ColumnEntryMemory.ACCESSOR_ID));
		}
		this.populateCompute2D(memory, context, memory.getTyped(this.hasField() ? ColumnEntryMemory.COMPUTE_NO_TEST : ColumnEntryMemory.GETTER));
	}

	public abstract void populateCompute2D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException;

	public void emitCompute3D(ColumnEntryMemory memory, DataCompileContext context) throws ScriptParsingException {
		if (this.hasValid()) {
			if (this.valid.where() != null) {
				context.setMethodCode(memory.getTyped(ColumnEntryMemory.VALID_WHERE), this.valid.where(), false, this, memory.getTyped(ColumnEntryMemory.ACCESSOR_ID));
			}
			if (this.valid.min_y() != null) {
				context.setMethodCode(memory.getTyped(VALID_MIN_Y), this.valid.min_y(), false, this, memory.getTyped(ColumnEntryMemory.ACCESSOR_ID));
			}
			if (this.valid.max_y() != null) {
				context.setMethodCode(memory.getTyped(VALID_MAX_Y), this.valid.max_y(), false, this, memory.getTyped(ColumnEntryMemory.ACCESSOR_ID));
			}
		}
		this.populateCompute3D(memory, context, memory.getTyped(this.hasField() || this.hasValid() ? COMPUTE_ONE : ColumnEntryMemory.GETTER));
	}

	public abstract void populateCompute3D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException;
}