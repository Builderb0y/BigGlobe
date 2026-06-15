package builderb0y.bigglobe.columns.scripted.entries;

import java.util.HashSet;
import java.util.stream.Stream;

import com.google.common.collect.ObjectArrays;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.data.EmptyData;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.classes.compile.ConstantFormatException;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.classes.spec.MemberSpec;
import builderb0y.bigglobe.columns.scripted.*;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.BooleanToConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.ConstantConditionTree;
import builderb0y.scripting.bytecode.tree.conditions.IntCompareConditionTree;
import builderb0y.scripting.bytecode.tree.flow.IfElseInsnTree;
import builderb0y.scripting.bytecode.tree.flow.IfInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.BitwiseOrInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.SubtractInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

@SuppressWarnings("DataFlowIssue")
public abstract class NonConstantColumnEntry extends ColumnEntry {

	public final @VerifyNullable Valid valid;
	public final @DefaultBoolean(true) boolean cache;
	public final transient SetBasedMutableDependencyView dependencies;

	public NonConstantColumnEntry(AccessSchema params, @VerifyNullable Valid valid, boolean cache) {
		super(params);
		this.cache = cache;
		this.valid = valid;
		this.dependencies = SetBasedMutableDependencyView.from(new HashSet<>());
	}

	@Override
	public Stream<? extends Holder<? extends DependencyView>> streamDirectDependencies() {
		return Stream.concat(super.streamDirectDependencies(), this.dependencies.streamDirectDependencies());
	}

	@Override
	public void verify(ColumnEntryRegistry registry) throws DetailedException {
		super.verify(registry);
		if (this.params.is_3d() && this.cache && (this.valid == null || this.valid.min_y() == null || this.valid.max_y() == null)) {
			BigGlobeMod.LOGGER.warn("Upper or lower bound not specified for column value " + UnregisteredObjectException.getID(registry.entryOf(this)) + ", and caching is enabled. This may result in poor worldgen performance, as it may compute more Y levels than intended.");
		}
	}

	@Override
	@MustBeInvokedByOverriders
	public void createTypeInfo(ColumnEntryRegistry registry) throws DetailedException {
		super.createTypeInfo(registry);
		ClassCompileContext clazz = registry.columnCompileContext.clazz;
		ColumnEntryContext context = new ColumnEntryContext();
		context.uniquifier = clazz.memberUniquifier++;
		if (this.hasFieldSetterAndFlag()) {
			context.flagsIndex = registry.columnCompileContext.nextFlagsIndex();
		}
		context.internalName = ColumnCompileContext.internalName(registry.idOf(this), context.uniquifier);
		LazyVarInfo[] maybeY = this.params.is_3d() ? new LazyVarInfo[] { new LazyVarInfo("y", TypeInfos.INT) } : LazyVarInfo.ARRAY_FACTORY.empty();
		registry.columnCompileContext.setCompileContext(this, context);
		this.populateContext(registry, context, clazz, maybeY);
	}

	public void populateContext(ColumnEntryRegistry registry, ColumnEntryContext context, ClassCompileContext clazz, LazyVarInfo[] maybeY) {
		TypeInfo valueType = this.getTypeInfo(registry);
		context.mainGetter = clazz.newMethod(
			ACC_PUBLIC,
			"get_" + context.internalName,
			valueType,
			maybeY
		);
		if (this.hasFieldSetterAndFlag()) {
			context.valueField = clazz.newField(
				this.params.is_3d() ? ACC_PUBLIC | ACC_FINAL : ACC_PUBLIC,
				context.internalName,
				this.params.is_3d()
				? (
					valueType.isObject()
					? MappedRangeObjectArray.TYPE
					: MappedRangeNumberArray.INFO.type
				)
				: valueType
			);
			context.mainSetter = clazz.newMethod(
				ACC_PUBLIC,
				"set_" + context.internalName,
				TypeInfos.VOID,
				ObjectArrays.concat(maybeY, new LazyVarInfo("value", valueType))
			);
			context.preComputer = clazz.newMethod(
				ACC_PUBLIC,
				"preCompute_" + context.internalName,
				TypeInfos.VOID
			);
		}
		context.computer = clazz.newMethod(
			ACC_PUBLIC,
			"compute_" + context.internalName,
			valueType,
			maybeY
		);
	}

	@Override
	public void compile(ColumnEntryRegistry registry) throws DetailedException {
		super.compile(registry);
		ColumnEntryContext context = registry.columnCompileContext.getCompileContext(this);
		if (this.params.is_3d()) {
			if (this.hasFieldSetterAndFlag()) {
				this.compile3DCached(registry, context);
			}
			else {
				this.compile3DUncached(registry, context);
			}
		}
		else {
			if (this.hasFieldSetterAndFlag()) {
				this.compile2DCached(registry, context);
			}
			else {
				this.compile2DUncached(registry, context);
			}
		}
	}

	/**
	TYPE compute(:
		...
	)

	void preCompute(:
		int oldFlags = this.flags_xxx
		int newFlags = oldFlags | flag
		if (oldFlags != newFlags:
			this.flags_xxx = newFlags
			this.field_xxx = validWhere() ? this.compute() : fallback
		)
	)

	TYPE get(:
		this.preCompute()
		return(this.field_xxx)
	)

	void set(TYPE value:
		this.backingField = value
	)
	*/
	public void compile2DCached(ColumnEntryRegistry registry, ColumnEntryContext context) throws ColumnValueException, ScriptParsingException {
		return_(this.makeComputer(registry, context, null)).emitBytecode(context.computer);
		context.computer.endCode();
		TypeInfo valueTypeInfo = this.getTypeInfo(registry);
		InsnTree loadColumn = registry.columnCompileContext.loadColumn();
		InsnTree fallback;
		try {
			fallback = (
				this.valid != null && this.valid.fallback() != null
				? this.parseConstant(registry, this.valid.fallback())
				: ldcAbsent(valueTypeInfo)
			);
		}
		catch (ConstantFormatException exception) {
			throw new ColumnValueException(exception);
		}
		/**
		validWhere() ? compute() : fallback
		*/
		InsnTree logic = invokeInstance(loadColumn, context.computer.info);
		if (this.valid != null && this.valid.where() != null) {
			logic = new IfElseInsnTree(
				new BooleanToConditionTree(
					this.makeCaller(registry, "validWhere", this.valid.where(), null, TypeInfos.BOOLEAN)
				),
				logic,
				fallback,
				valueTypeInfo
			);
		}
		LazyVarInfo oldFlags = context.preComputer.scopes.addVariable("oldFlags", TypeInfos.INT);
		LazyVarInfo newFlags = context.preComputer.scopes.addVariable("newFlags", TypeInfos.INT);
		FieldInfo flagsField = registry.columnCompileContext.flagsField(context.flagsIndex());
		int flagsBitmask = registry.columnCompileContext.flagsFieldBitmask(context.flagsIndex());
		/**
		int oldFlags = this.flags_xxx
		int newFlags = oldFlags | flag
		if (oldFlags != newFlags:
			this.flags_xxx = newFlags
			this.field_xxx = validWhere() ? compute() : fallback
		)
		*/
		logic = seq(
			store(oldFlags, getField(loadColumn, flagsField)),
			store(newFlags, new BitwiseOrInsnTree(load(oldFlags), ldc(flagsBitmask), IOR)),
			new IfInsnTree(
				IntCompareConditionTree.notEqual(load(oldFlags), load(newFlags)),
				seq(
					putField(loadColumn, flagsField, load(newFlags)),
					putField(loadColumn, context.valueField.info, logic)
				)
			)
		);
		return_(logic).emitBytecode(context.preComputer);
		context.preComputer.endCode();
		/**
		preCompute()
		return(this.field_xxx)
		*/
		seq(
			invokeInstance(loadColumn, context.preComputer.info),
			return_(getField(loadColumn, context.valueField.info))
		)
		.emitBytecode(context.mainGetter);
		context.mainGetter.endCode();

		return_(
			putField(
				loadColumn,
				context.valueField.info,
				load("value", valueTypeInfo)
			)
		)
		.emitBytecode(context.mainSetter);
		context.mainSetter.endCode();
	}

	/**
	TYPE compute(:
		...
	)

	TYPE get(:
		return(this.validWhere() ? compute() : fallback)
	)
	*/
	public void compile2DUncached(ColumnEntryRegistry registry, ColumnEntryContext context) throws ColumnValueException, ScriptParsingException {
		return_(this.makeComputer(registry, context, null)).emitBytecode(context.computer);
		context.computer.endCode();
		TypeInfo valueTypeInfo = this.getTypeInfo(registry);
		InsnTree loadColumn = registry.columnCompileContext.loadColumn();
		InsnTree fallback;
		try {
			fallback = (
				this.valid != null && this.valid.fallback() != null
				? this.parseConstant(registry, this.valid.fallback())
				: ldcAbsent(valueTypeInfo)
			);
		}
		catch (ConstantFormatException exception) {
			throw new ColumnValueException(exception);
		}
		InsnTree logic = invokeInstance(loadColumn, context.computer.info);
		if (this.valid != null && this.valid.where() != null) {
			logic = new IfElseInsnTree(
				new BooleanToConditionTree(
					this.makeCaller(registry, "validWhere", this.valid.where(), null, TypeInfos.BOOLEAN)
				),
				logic,
				fallback,
				valueTypeInfo
			);
		}
		return_(logic).emitBytecode(context.mainGetter);
		context.mainGetter.endCode();
	}

	/**
	void preCompute(:
		int oldFlags = this.flags_xxx
		int newFlags = oldFlags | flag
		if (oldFlags != newFlags:
			this.flags_xxx = newFlags
			if (validWhere():
				if (backingField.reallocate(this, minY(), maxY()):
					<computer>
				)
			)
			else (
				backingField.invalidate()
			)
		)
	)

	TYPE get(int y:
		this.preCompute()
		if (backingField.valid:
			if (y >= backingField.minCached && y < backingField.maxCached:
				return(array.array.get(y - array.minCached))
			)
			if (y >= backingField.minAccessible && y < backingField.maxAccessible:
				return(compute(y))
			)
		)
		return(fallback)
	)

	void set(int y, TYPE value:
		if (backingField.valid && y >= backingField.minCached && y < backingField.maxCached:
			array.array.set(y - array.minCached, value)
		)
	)
	*/
	public void compile3DCached(ColumnEntryRegistry registry, ColumnEntryContext context) throws ColumnValueException, ScriptParsingException {
		LazyVarInfo y = new LazyVarInfo("y", TypeInfos.INT);
		return_(this.makeComputer(registry, context, load(y))).emitBytecode(context.computer);
		context.computer.endCode();
		TypeInfo valueTypeInfo = this.getTypeInfo(registry);
		InsnTree loadColumn = registry.columnCompileContext.loadColumn();
		InsnTree getBackingField = getField(loadColumn, context.valueField.info);
		/**
		this.backingField = MappedRangeArray.new(...)
		*/
		putField(
			loadColumn,
			context.valueField.info,
			switch (valueTypeInfo.getSort()) {
				case VOID -> throw new ColumnValueException("void array");
				case BOOLEAN -> newInstance(MappedRangeNumberArray.CONSTRUCTOR, getStatic(NumberArray.INFO.EMPTY_BOOLEAN));
				case BYTE -> newInstance(MappedRangeNumberArray.CONSTRUCTOR, getStatic(NumberArray.INFO.EMPTY_BYTE));
				case CHAR -> throw new UnsupportedOperationException("char array");
				case SHORT -> newInstance(MappedRangeNumberArray.CONSTRUCTOR, getStatic(NumberArray.INFO.EMPTY_SHORT));
				case INT -> newInstance(MappedRangeNumberArray.CONSTRUCTOR, getStatic(NumberArray.INFO.EMPTY_INT));
				case LONG -> newInstance(MappedRangeNumberArray.CONSTRUCTOR, getStatic(NumberArray.INFO.EMPTY_LONG));
				case FLOAT -> newInstance(MappedRangeNumberArray.CONSTRUCTOR, getStatic(NumberArray.INFO.EMPTY_FLOAT));
				case DOUBLE -> newInstance(MappedRangeNumberArray.CONSTRUCTOR, getStatic(NumberArray.INFO.EMPTY_DOUBLE));
				case OBJECT, ARRAY -> newInstance(MappedRangeObjectArray.CONSTRUCTOR, newArrayWithLength(TypeInfo.makeArray(valueTypeInfo), ldc(0)));
			}
		)
		.emitBytecode(registry.columnCompileContext.constructor);
		InsnTree preCompute = this.makeBulkComputer(registry, context);
		/** backingField.reallocate(this, minY(), maxY()) */
		InsnTree reallocate;
		if (this.valid != null) {
			if (this.valid.min_y() != null) {
				if (this.valid.max_y() != null) {
					reallocate = invokeInstance(
						getBackingField,
						MappedRangeArray.INFO.reallocateBoth,
						loadColumn,
						this.makeCaller(registry, "validMinY", this.valid.min_y(), null, TypeInfos.INT),
						this.makeCaller(registry, "validMaxY", this.valid.max_y(), null, TypeInfos.INT)
					);
				}
				else {
					reallocate = invokeInstance(
						getBackingField,
						MappedRangeArray.INFO.reallocateMin,
						loadColumn,
						this.makeCaller(registry, "validMinY", this.valid.min_y(), null, TypeInfos.INT)
					);
				}
			}
			else {
				if (this.valid.max_y() != null) {
					reallocate = invokeInstance(
						getBackingField,
						MappedRangeArray.INFO.reallocateMax,
						loadColumn,
						this.makeCaller(registry, "validMaxY", this.valid.max_y(), null, TypeInfos.INT)
					);
				}
				else {
					reallocate = invokeInstance(
						getBackingField,
						MappedRangeArray.INFO.reallocateNone,
						loadColumn
					);
				}
			}
		}
		else {
			reallocate = invokeInstance(
				getBackingField,
				MappedRangeArray.INFO.reallocateNone,
				loadColumn
			);
		}
		/**
		if (backingField.reallocate(this, minY(), maxY()):
			<computer>
		)
		*/
		preCompute = new IfInsnTree(new BooleanToConditionTree(reallocate), preCompute);
		if (this.valid != null && this.valid.where() != null) {
			/**
			if (validWhere():
				if (backingField.reallocate(this, minY(), maxY()):
					<computer>
				)
			)
			else (
				backingField.invalidate()
			)
			*/
			preCompute = new IfElseInsnTree(
				new BooleanToConditionTree(
					this.makeCaller(registry, "validWhere", this.valid.where(), null, TypeInfos.BOOLEAN)
				),
				preCompute,
				invokeInstance(getBackingField, MappedRangeArray.INFO.invalidate),
				TypeInfos.VOID
			);
		}
		LazyVarInfo oldFlags = context.preComputer.scopes.addVariable("oldFlags", TypeInfos.INT);
		LazyVarInfo newFlags = context.preComputer.scopes.addVariable("newFlags", TypeInfos.INT);
		FieldInfo flagsField = registry.columnCompileContext.flagsField(context.flagsIndex());
		int flagsBitmask = registry.columnCompileContext.flagsFieldBitmask(context.flagsIndex());
		/**
		int oldFlags = this.flags_xxx
		int newFlags = oldFlags | flag
		if (oldFlags != newFlags:
			this.flags_xxx = newFlags
			if (validWhere():
				if (backingField.reallocate(this, minY(), maxY()):
					<computer>
				)
			)
			else (
				backingField.invalidate()
			)
		)
		*/
		preCompute = seq(
			store(oldFlags, getField(loadColumn, flagsField)),
			store(newFlags, new BitwiseOrInsnTree(load(oldFlags), ldc(flagsBitmask), IOR)),
			new IfInsnTree(
				IntCompareConditionTree.notEqual(load(oldFlags), load(newFlags)),
				seq(
					putField(loadColumn, flagsField, load(newFlags)),
					preCompute
				)
			)
		);
		return_(preCompute).emitBytecode(context.preComputer);
		context.preComputer.endCode();

		InsnTree callPrecompute = invokeInstance(
			loadColumn,
			context.preComputer.info
		);

		InsnTree fallback;
		try {
			fallback = this.parseConstant(registry, this.valid != null ? this.valid.fallback() : EmptyData.INSTANCE);
		}
		catch (ConstantFormatException exception) {
			throw new ColumnValueException(exception);
		}

		/**
		return(array.array.get(y - array.minCached))
		*/
		InsnTree getFromCache = return_(
			switch (valueTypeInfo.getSort()) {
				case VOID -> throw new IllegalStateException("void array");
				case BOOLEAN -> invokeInstance(getField(getBackingField, MappedRangeNumberArray.INFO.array), NumberArray.INFO.implGetZ, new SubtractInsnTree(load(y), getField(getBackingField, MappedRangeArray.INFO.minCached), ISUB));
				case BYTE -> invokeInstance(getField(getBackingField, MappedRangeNumberArray.INFO.array), NumberArray.INFO.implGetB, new SubtractInsnTree(load(y), getField(getBackingField, MappedRangeArray.INFO.minCached), ISUB));
				case CHAR -> throw new UnsupportedOperationException("char array");
				case SHORT -> invokeInstance(getField(getBackingField, MappedRangeNumberArray.INFO.array), NumberArray.INFO.implGetS, new SubtractInsnTree(load(y), getField(getBackingField, MappedRangeArray.INFO.minCached), ISUB));
				case INT -> invokeInstance(getField(getBackingField, MappedRangeNumberArray.INFO.array), NumberArray.INFO.implGetI, new SubtractInsnTree(load(y), getField(getBackingField, MappedRangeArray.INFO.minCached), ISUB));
				case LONG -> invokeInstance(getField(getBackingField, MappedRangeNumberArray.INFO.array), NumberArray.INFO.implGetL, new SubtractInsnTree(load(y), getField(getBackingField, MappedRangeArray.INFO.minCached), ISUB));
				case FLOAT -> invokeInstance(getField(getBackingField, MappedRangeNumberArray.INFO.array), NumberArray.INFO.implGetF, new SubtractInsnTree(load(y), getField(getBackingField, MappedRangeArray.INFO.minCached), ISUB));
				case DOUBLE -> invokeInstance(getField(getBackingField, MappedRangeNumberArray.INFO.array), NumberArray.INFO.implGetD, new SubtractInsnTree(load(y), getField(getBackingField, MappedRangeArray.INFO.minCached), ISUB));
				case OBJECT, ARRAY -> new DirectCastInsnTree(arrayLoad(getField(getBackingField, MappedRangeObjectArray.ARRAY), new SubtractInsnTree(load(y), getField(getBackingField, MappedRangeArray.INFO.minCached), ISUB)), valueTypeInfo, false);
			}
		);
		/**
		if (y >= backingField.minCached && y < backingField.maxCached:
			return(array.array.get(y - array.minCached))
		)
		*/
		getFromCache = new IfInsnTree(
			and(
				IntCompareConditionTree.greaterThanOrEqual(load(y), getField(getBackingField, MappedRangeArray.INFO.minCached)),
				IntCompareConditionTree.lessThan(load(y), getField(getBackingField, MappedRangeArray.INFO.maxCached))
			),
			getFromCache
		);

		/**
		return(compute(y))
		*/
		InsnTree cacheMiss = return_(
			invokeInstance(
				loadColumn,
				context.computer.info,
				load(y)
			)
		);
		outer:
		if (this.valid != null) {
			ConditionTree uncachedRange;
			if (this.valid.min_y() != null) {
				if (this.valid.max_y() != null) {
					uncachedRange = and(
						IntCompareConditionTree.greaterThanOrEqual(load(y), getField(getBackingField, MappedRangeArray.INFO.minAccessible)),
						IntCompareConditionTree.lessThan(load(y), getField(getBackingField, MappedRangeArray.INFO.maxAccessible))
					);
				}
				else {
					uncachedRange = IntCompareConditionTree.greaterThanOrEqual(load(y), getField(getBackingField, MappedRangeArray.INFO.minAccessible));
				}
			}
			else {
				if (this.valid.max_y() != null) {
					uncachedRange = IntCompareConditionTree.lessThan(load(y), getField(getBackingField, MappedRangeArray.INFO.maxAccessible));
				}
				else {
					break outer;
				}
			}
			/**
			if (y >= backingField.minAccessible && y < backingField.maxAccessible:
				return(compute(y))
			)
			*/
			cacheMiss = new IfInsnTree(uncachedRange, cacheMiss);
		}

		/**
		if (y >= backingField.minCached && y < backingField.maxCached:
			return(array.array.get(y - array.minCached))
		)
		if (y >= backingField.minAccessible && y < backingField.maxAccessible:
			return(compute(y))
		)
		*/
		InsnTree getWhenValid = seq(getFromCache, cacheMiss);
		if (this.valid != null && this.valid.where() != null) {
			/**
			if (backingField.valid:
				if (y >= backingField.minCached && y < backingField.maxCached:
					return(array.array.get(y - array.minCached))
				)
				if (y >= backingField.minAccessible && y < backingField.maxAccessible:
					return(compute(y))
				)
			)
			*/
			getWhenValid = new IfInsnTree(
				new BooleanToConditionTree(
					getField(getBackingField, MappedRangeArray.INFO.valid)
				),
				getWhenValid
			);
		}

		/**
		this.preCompute()
		if (backingField.valid:
			if (y >= backingField.minCached && y < backingField.maxCached:
				return(array.array.get(y - array.minCached))
			)
			if (y >= backingField.minAccessible && y < backingField.maxAccessible:
				return(compute(y))
			)
		)
		return(fallback)
		*/
		seq(callPrecompute, getWhenValid, return_(fallback)).emitBytecode(context.mainGetter);
		context.mainGetter.endCode();

		/**
		this.backingField.trySet(y, value)
		*/
		return_(
			invokeInstance(
				getBackingField,
				switch (valueTypeInfo.getSort()) {
					case VOID -> throw new IllegalStateException("void array");
					case BOOLEAN -> MappedRangeNumberArray.INFO.trySetZ;
					case BYTE -> MappedRangeNumberArray.INFO.trySetB;
					case CHAR -> throw new UnsupportedOperationException("char array");
					case SHORT -> MappedRangeNumberArray.INFO.trySetS;
					case INT -> MappedRangeNumberArray.INFO.trySetI;
					case LONG -> MappedRangeNumberArray.INFO.trySetL;
					case FLOAT -> MappedRangeNumberArray.INFO.trySetF;
					case DOUBLE -> MappedRangeNumberArray.INFO.trySetD;
					case OBJECT, ARRAY -> MappedRangeObjectArray.TRY_SET;
				},
				load("y", TypeInfos.INT),
				load("value", valueTypeInfo)
			)
		)
		.emitBytecode(context.mainSetter);
		context.mainSetter.endCode();
	}

	/**
	TYPE get(int y:
		if (this.validWhere() && y >= this.validMinY() && y < this.validMaxY():
			return(compute(y))
		)
	)
	*/
	public void compile3DUncached(ColumnEntryRegistry registry, ColumnEntryContext context) throws ColumnValueException, ScriptParsingException {
		LazyVarInfo y = new LazyVarInfo("y", TypeInfos.INT);
		return_(this.makeComputer(registry, context, load(y))).emitBytecode(context.computer);
		context.computer.endCode();

		TypeInfo valueTypeInfo = this.getTypeInfo(registry);
		InsnTree loadColumn = registry.columnCompileContext.loadColumn();
		InsnTree computer = invokeInstance(loadColumn, context.computer.info, load(y));
		if (this.hasValid()) {
			ConditionTree condition = ConstantConditionTree.TRUE;
			if (this.valid.where() != null) {
				condition = new BooleanToConditionTree(
					this.makeCaller(registry, "validWhere", this.valid.where(), null, TypeInfos.BOOLEAN)
				);
			}
			if (this.valid.min_y() != null) {
				condition = and(
					condition,
					IntCompareConditionTree.greaterThanOrEqual(
						load(y),
						this.makeCaller(registry, "validMinY", this.valid.min_y(), null, TypeInfos.INT)
					)
				);
			}
			if (this.valid.max_y() != null) {
				condition = and(
					condition,
					IntCompareConditionTree.lessThan(
						load(y),
						this.makeCaller(registry, "validMaxY", this.valid.max_y(), null, TypeInfos.INT)
					)
				);
			}
			InsnTree fallback;
			try {
				fallback = this.parseConstant(registry, this.valid.fallback());
			}
			catch (ConstantFormatException exception) {
				throw new ColumnValueException(exception);
			}
			/**
			validWhere() && y >= validMinY() && y < validMaxY() ? <computer> : fallback
			*/
			computer = new IfElseInsnTree(
				condition,
				computer,
				fallback,
				valueTypeInfo
			);
		}
		return_(computer).emitBytecode(context.mainGetter);
		context.mainGetter.endCode();
	}

	public InsnTree makeCaller(ColumnEntryRegistry registry, String prefix, ScriptUsage code, @Nullable InsnTree loadY, TypeInfo returnType) throws ScriptParsingException {
		MethodCompileContext method = registry.columnCompileContext.clazz.newMethod(
			ACC_PUBLIC,
			prefix + "_" + registry.columnCompileContext.getCompileContext(this).internalName,
			returnType,
			loadY != null ? new LazyVarInfo[] { new LazyVarInfo("y", TypeInfos.INT) } : LazyVarInfo.ARRAY_FACTORY.empty()
		);
		LoadInsnTree loadColumn = load("this", registry.columnCompileContext.columnTypeInfo());
		registry.setMethodCode(
			method,
			code,
			loadColumn,
			loadY != null ? load("y", TypeInfos.INT) : null,
			null,
			registry.idOf(this),
			this.dependencies,
			MemberSpec.NO_EXTRAS
		);
		return invokeInstance(loadColumn, method.info, nonnull(loadY));
	}

	public abstract InsnTree makeComputer(ColumnEntryRegistry registry, ColumnEntryContext context, @Nullable InsnTree loadY) throws ScriptParsingException;

	public abstract InsnTree makeBulkComputer(ColumnEntryRegistry registry, ColumnEntryContext context) throws ScriptParsingException;

	@Override
	public boolean hasFieldSetterAndFlag() {
		return this.cache;
	}

	public boolean hasValid() {
		return this.valid != null && this.valid.isUseful(this.params.is_3d());
	}
}