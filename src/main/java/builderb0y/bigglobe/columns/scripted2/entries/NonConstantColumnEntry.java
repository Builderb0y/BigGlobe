package builderb0y.bigglobe.columns.scripted2.entries;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ObjectArrays;
import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.DefaultBoolean;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.data.EmptyData;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.columns.scripted.MappedRangeArray;
import builderb0y.bigglobe.columns.scripted.MappedRangeNumberArray;
import builderb0y.bigglobe.columns.scripted.MappedRangeObjectArray;
import builderb0y.bigglobe.columns.scripted.classes.ConstantFormatException;
import builderb0y.bigglobe.columns.scripted.classes.MemberSpec;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SetBasedMutableDependencyView;
import builderb0y.bigglobe.columns.scripted2.*;
import builderb0y.bigglobe.columns.scripted.classes.ElementSpec;
import builderb0y.bigglobe.columns.scripted.classes.TypeSpec;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.*;
import builderb0y.scripting.bytecode.tree.flow.IfElseInsnTree;
import builderb0y.scripting.bytecode.tree.flow.IfInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.StoreInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.BitwiseOrInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.SubtractInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class NonConstantColumnEntry extends ColumnEntry implements SetBasedMutableDependencyView {

	public final @VerifyNullable Valid valid;
	public final @DefaultBoolean(true) boolean cache;
	public final transient Set<RegistryEntry<? extends DependencyView>> dependencies;

	public NonConstantColumnEntry(AccessSchema params, @VerifyNullable Valid valid, boolean cache) {
		super(params);
		this.cache = cache;
		this.valid = valid;
		this.dependencies = new HashSet<>();
	}

	@Override
	public Set<RegistryEntry<? extends DependencyView>> getDependencies() {
		return this.dependencies;
	}

	@Override
	public void verify(ColumnEntryRegistry registry) throws ColumnValueException {
		super.verify(registry);
		if (this.params.is_3d() && this.cache && (this.valid == null || this.valid.min_y() == null || this.valid.max_y() == null)) {
			BigGlobeMod.LOGGER.warn("Upper or lower bound not specified for column value " + UnregisteredObjectException.getID(registry.entryOf(this)) + ", and caching is enabled. This may result in poor worldgen performance, as it may compute more Y levels than intended.");
		}
	}

	@Override
	public void createContext(ColumnEntryRegistry registry) throws ColumnValueException {
		ClassCompileContext clazz = registry.columnCompileContext.clazz;
		NonConstantColumnEntryContext context = new NonConstantColumnEntryContext();
		context.uniquifier = clazz.memberUniquifier++;
		context.internalName = ColumnCompileContext.internalName(UnregisteredObjectException.getID(registry.entryOf(this)), context.uniquifier);
		LazyVarInfo[] maybeY = this.params.is_3d() ? new LazyVarInfo[] { new LazyVarInfo("y", TypeInfos.INT) } : LazyVarInfo.ARRAY_FACTORY.empty();
		TypeInfo valueType = ElementSpec.asType(this.params.type()).getTypeInfo();
		context.mainGetter = clazz.newMethod(ACC_PUBLIC, "get_" + context.internalName, valueType, maybeY);
		this.populateContextFieldAndSetter(context, clazz, valueType, maybeY);
		registry.columnCompileContext.setCompileContext(this, context);
	}

	public void populateContextFieldAndSetter(NonConstantColumnEntryContext context, ClassCompileContext clazz, TypeInfo valueType, LazyVarInfo[] maybeY) {
		if (this.hasFieldSetterAndFlag()) {
			context.valueField = clazz.newField(
				this.params.is_3d() ? ACC_PUBLIC | ACC_FINAL : ACC_PUBLIC,
				context.internalName,
				this.params.is_3d()
				? (
					valueType.isObject()
					? MappedRangeObjectArray.TYPE
					: MappedRangeNumberArray.TYPE
				)
				: valueType
			);
			context.mainSetter = clazz.newMethod(
				ACC_PUBLIC,
				"set_" + context.internalName,
				TypeInfos.VOID,
				ObjectArrays.concat(maybeY, new LazyVarInfo("value", valueType))
			);
		}
	}

	@Override
	public void compile(ColumnEntryRegistry registry) throws ColumnValueException, ScriptParsingException {
		NonConstantColumnEntryContext context = registry.columnCompileContext.getCompileContext(this);
		if (!this.params.is_3d()) {
			this.compile2D(registry, context);
		}
		else if (this.hasFieldSetterAndFlag()) {
			this.compile3DCached(registry, context);
		}
		else {
			this.compile3DUncached(registry, context);
		}
	}

	public void compile2D(ColumnEntryRegistry registry, NonConstantColumnEntryContext context) throws ColumnValueException, ScriptParsingException {
		TypeSpec valueType = ElementSpec.asType(this.params.type());
		TypeInfo valueTypeInfo = valueType.getTypeInfo();
		InsnTree loadColumn = registry.columnCompileContext.loadColumn();
		LazyVarInfo value = context.mainGetter.scopes.addVariable("value", valueTypeInfo);
		InsnTree computer = this.makeComputer(registry, context);
		if (this.hasValid()) {
			/**
			validWhere() ? <computer> : fallback
			*/
			InsnTree fallback;
			try {
				fallback = valueType.parseConstant(registry.classHierarchy, this.valid.fallback(), loadColumn);
			}
			catch (ConstantFormatException exception) {
				throw new ColumnValueException(exception);
			}
			computer = new IfElseInsnTree(
				new BooleanToConditionTree(
					this.makeCaller(registry, "validWhere", this.valid.where(), TypeInfos.BOOLEAN)
				),
				computer,
				fallback,
				valueTypeInfo
			);
		}
		/**
		TYPE value = validWhere() ? <computer> : fallback
		*/
		computer = new StoreInsnTree(value, computer);
		if (this.hasFieldSetterAndFlag()) {
			LazyVarInfo oldFlags = context.mainGetter.scopes.addVariable("oldFlags", TypeInfos.INT);
			LazyVarInfo newFlags = context.mainGetter.scopes.addVariable("newFlags", TypeInfos.INT);
			FieldInfo flagsField = registry.columnCompileContext.flagsField(context.uniquifier);
			int flagsBitmask = registry.columnCompileContext.flagsFieldBitmask(context.uniquifier);
			/**
			TYPE value
			int oldFlags = this.flags_xxx
			int newFlags = oldFlags | flag
			if (oldFlags != newFlags:
				this.flags_xxx = newFlags
				value = validWhere() ? <computer> : fallback
			)
			else (
				value = backingField
			)
			*/
			computer = seq(
				store(oldFlags, getField(loadColumn, flagsField)),
				store(newFlags, new BitwiseOrInsnTree(load(oldFlags), ldc(flagsBitmask), IOR)),
				new IfElseInsnTree(
					IntCompareConditionTree.notEqual(load(oldFlags), load(newFlags)),
					seq(
						putField(loadColumn, flagsField, load(newFlags)),
						computer,
						putField(loadColumn, context.valueField.info, load(value))
					),
					store(value, getField(loadColumn, context.valueField.info)),
					TypeInfos.VOID
				)
			);
		}
		/**
		TYPE value
		int oldFlags = this.flags_xxx
		int newFlags = oldFlags | flag
		if (oldFlags != newFlags:
			this.flags_xxx = newFlags
			value = validWhere() ? <computer> : fallback
		)
		else (
			value = backingField
		)
		return(value)
		*/
		seq(computer, return_(load(value))).emitBytecode(context.mainGetter);
		context.mainGetter.endCode();
	}

	public void compile3DCached(ColumnEntryRegistry registry, NonConstantColumnEntryContext context) throws ColumnValueException, ScriptParsingException {
		LazyVarInfo y = new LazyVarInfo("y", TypeInfos.INT);
		TypeSpec valueType = ElementSpec.asType(this.params.type());
		TypeInfo valueTypeInfo = valueType.getTypeInfo();
		InsnTree loadColumn = registry.columnCompileContext.loadColumn();
		InsnTree getBackingField = getField(loadColumn, context.valueField.info);
		/**
		this.backingField = MappedRangeArray.new(...)
		*/
		putField(
			loadColumn,
			context.valueField.info,
			switch (valueTypeInfo.getSort()) {
				case VOID          -> throw new ColumnValueException("void array");
				case BOOLEAN       -> newInstance(MappedRangeNumberArray.CONSTRUCTOR, getStatic(NumberArray.INFO.EMPTY_BOOLEAN));
				case BYTE          -> newInstance(MappedRangeNumberArray.CONSTRUCTOR, getStatic(NumberArray.INFO.EMPTY_BYTE));
				case CHAR          -> throw new UnsupportedOperationException("char array");
				case SHORT         -> newInstance(MappedRangeNumberArray.CONSTRUCTOR, getStatic(NumberArray.INFO.EMPTY_SHORT));
				case INT           -> newInstance(MappedRangeNumberArray.CONSTRUCTOR, getStatic(NumberArray.INFO.EMPTY_INT));
				case LONG          -> newInstance(MappedRangeNumberArray.CONSTRUCTOR, getStatic(NumberArray.INFO.EMPTY_LONG));
				case FLOAT         -> newInstance(MappedRangeNumberArray.CONSTRUCTOR, getStatic(NumberArray.INFO.EMPTY_FLOAT));
				case DOUBLE        -> newInstance(MappedRangeNumberArray.CONSTRUCTOR, getStatic(NumberArray.INFO.EMPTY_DOUBLE));
				case OBJECT, ARRAY -> newInstance(MappedRangeObjectArray.CONSTRUCTOR, newArrayWithLength(TypeInfo.makeArray(valueTypeInfo), ldc(0)));
			}
		)
		.emitBytecode(registry.columnCompileContext.constructor);
		InsnTree computer = this.makeBulkComputer(registry, context);
		/** backingField.reallocate(this, minY(), maxY()) */
		InsnTree reallocate;
		if (this.valid != null) {
			if (this.valid.min_y() != null) {
				if (this.valid.max_y() != null) {
					reallocate = invokeInstance(
						getBackingField,
						MappedRangeArray.INFO.reallocateBoth,
						loadColumn,
						this.makeCaller(registry, "validMinY", this.valid.min_y(), TypeInfos.INT),
						this.makeCaller(registry, "validMaxY", this.valid.max_y(), TypeInfos.INT)
					);
				}
				else {
					reallocate = invokeInstance(
						getBackingField,
						MappedRangeArray.INFO.reallocateMin,
						loadColumn,
						this.makeCaller(registry, "validMinY", this.valid.min_y(), TypeInfos.INT)
					);
				}
			}
			else {
				if (this.valid.max_y() != null) {
					reallocate = invokeInstance(
						getBackingField,
						MappedRangeArray.INFO.reallocateMax,
						loadColumn,
						this.makeCaller(registry, "validMaxY", this.valid.max_y(), TypeInfos.INT)
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
		computer = new IfInsnTree(new BooleanToConditionTree(reallocate), computer);
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
			computer = new IfElseInsnTree(
				new BooleanToConditionTree(
					this.makeCaller(registry, "validWhere", this.valid.where(), TypeInfos.BOOLEAN)
				),
				computer,
				invokeInstance(getBackingField, MappedRangeArray.INFO.invalidate),
				TypeInfos.VOID
			);
		}
		LazyVarInfo oldFlags = context.mainGetter.scopes.addVariable("oldFlags", TypeInfos.INT);
		LazyVarInfo newFlags = context.mainGetter.scopes.addVariable("newFlags", TypeInfos.INT);
		FieldInfo flagsField = registry.columnCompileContext.flagsField(context.uniquifier);
		int flagsBitmask = registry.columnCompileContext.flagsFieldBitmask(context.uniquifier);
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
		computer = seq(
			store(oldFlags, getField(loadColumn, flagsField)),
			store(newFlags, new BitwiseOrInsnTree(load(oldFlags), ldc(flagsBitmask), IOR)),
			new IfInsnTree(
				IntCompareConditionTree.notEqual(load(oldFlags), load(newFlags)),
				seq(
					putField(loadColumn, flagsField, load(newFlags)),
					computer
				)
			)
		);
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
		InsnTree fallback;
		try {
			fallback = valueType.parseConstant(registry.classHierarchy, this.valid != null ? this.valid.fallback() : EmptyData.INSTANCE, loadColumn);
		}
		catch (ConstantFormatException exception) {
			throw new ColumnValueException(exception);
		}
		seq(
			computer,
			new IfInsnTree(
				new BooleanToConditionTree(
					getField(getBackingField, MappedRangeArray.INFO.valid)
				),
				seq(
					new IfInsnTree(
						and(
							IntCompareConditionTree.greaterThanOrEqual(load(y), getField(getBackingField, MappedRangeArray.INFO.minCached)),
							IntCompareConditionTree.lessThan(load(y), getField(getBackingField, MappedRangeArray.INFO.maxCached))
						),
						return_(
							switch (valueTypeInfo.getSort()) {
								case VOID -> throw new IllegalStateException("void array");
								case BOOLEAN -> invokeInstance(getField(getBackingField, MappedRangeNumberArray.ARRAY), NumberArray.INFO.implGetZ, new SubtractInsnTree(load(y), getField(getBackingField, MappedRangeArray.INFO.minCached), ISUB));
								case BYTE -> invokeInstance(getField(getBackingField, MappedRangeNumberArray.ARRAY), NumberArray.INFO.implGetB, new SubtractInsnTree(load(y), getField(getBackingField, MappedRangeArray.INFO.minCached), ISUB));
								case CHAR -> throw new UnsupportedOperationException("char array");
								case SHORT -> invokeInstance(getField(getBackingField, MappedRangeNumberArray.ARRAY), NumberArray.INFO.implGetS, new SubtractInsnTree(load(y), getField(getBackingField, MappedRangeArray.INFO.minCached), ISUB));
								case INT -> invokeInstance(getField(getBackingField, MappedRangeNumberArray.ARRAY), NumberArray.INFO.implGetI, new SubtractInsnTree(load(y), getField(getBackingField, MappedRangeArray.INFO.minCached), ISUB));
								case LONG -> invokeInstance(getField(getBackingField, MappedRangeNumberArray.ARRAY), NumberArray.INFO.implGetL, new SubtractInsnTree(load(y), getField(getBackingField, MappedRangeArray.INFO.minCached), ISUB));
								case FLOAT -> invokeInstance(getField(getBackingField, MappedRangeNumberArray.ARRAY), NumberArray.INFO.implGetF, new SubtractInsnTree(load(y), getField(getBackingField, MappedRangeArray.INFO.minCached), ISUB));
								case DOUBLE -> invokeInstance(getField(getBackingField, MappedRangeNumberArray.ARRAY), NumberArray.INFO.implGetD, new SubtractInsnTree(load(y), getField(getBackingField, MappedRangeArray.INFO.minCached), ISUB));
								case OBJECT, ARRAY -> new DirectCastInsnTree(arrayLoad(getField(getBackingField, MappedRangeObjectArray.ARRAY), new SubtractInsnTree(load(y), getField(getBackingField, MappedRangeArray.INFO.minCached), ISUB)), valueTypeInfo, false);
							}
						)
					),
					new IfInsnTree(
						and(
							IntCompareConditionTree.greaterThanOrEqual(load(y), getField(getBackingField, MappedRangeArray.INFO.minAccessible)),
							IntCompareConditionTree.lessThan(load(y), getField(getBackingField, MappedRangeArray.INFO.maxAccessible))
						),
						return_(this.makeComputer(registry, context))
					)
				)
			),
			return_(fallback)
		)
		.emitBytecode(context.mainGetter);
		context.mainGetter.endCode();
	}

	public void compile3DUncached(ColumnEntryRegistry registry, NonConstantColumnEntryContext context) throws ColumnValueException, ScriptParsingException {
		LazyVarInfo y = new LazyVarInfo("y", TypeInfos.INT);
		TypeSpec valueType = ElementSpec.asType(this.params.type());
		TypeInfo valueTypeInfo = valueType.getTypeInfo();
		InsnTree loadColumn = registry.columnCompileContext.loadColumn();
		InsnTree computer = this.makeComputer(registry, context);
		if (this.hasValid()) {
			ConditionTree condition = ConstantConditionTree.TRUE;
			if (this.valid.where() != null) {
				condition = new BooleanToConditionTree(
					this.makeCaller(registry, "validWhere", this.valid.where(), TypeInfos.BOOLEAN)
				);
			}
			if (this.valid.min_y() != null) {
				condition = and(
					condition,
					IntCompareConditionTree.greaterThanOrEqual(
						load(y),
						this.makeCaller(registry, "validMinY", this.valid.min_y(), TypeInfos.INT)
					)
				);
			}
			if (this.valid.max_y() != null) {
				condition = and(
					condition,
					IntCompareConditionTree.lessThan(
						load(y),
						this.makeCaller(registry, "validMaxY", this.valid.max_y(), TypeInfos.INT)
					)
				);
			}
			/**
			validWhere() && y >= validMinY() && y < validMaxY() ? <computer> : fallback
			*/
			try {
				computer = new IfElseInsnTree(
					condition,
					computer,
					valueType.parseConstant(
						registry.classHierarchy,
						this.valid.fallback(),
						loadColumn
					),
					valueTypeInfo
				);
			}
			catch (ConstantFormatException exception) {
				throw new ColumnValueException(exception);
			}
		}
		return_(computer).emitBytecode(context.mainGetter);
		context.mainGetter.endCode();
	}

	public InsnTree makeCaller(ColumnEntryRegistry registry, String prefix, ScriptUsage code, TypeInfo returnType) throws ScriptParsingException{
		MethodCompileContext method = registry.columnCompileContext.clazz.newMethod(
			ACC_PUBLIC,
			prefix + "_" + registry.columnCompileContext.<NonConstantColumnEntryContext>getCompileContext(this).internalName,
			returnType
		);
		LoadInsnTree loadColumn = load("this", registry.columnCompileContext.columnTypeInfo());
		registry.setMethodCode(
			method,
			code,
			loadColumn,
			null,
			null,
			this,
			MemberSpec.NO_EXTRAS
		);
		return invokeInstance(loadColumn, method.info);
	}

	public abstract InsnTree makeComputer(ColumnEntryRegistry registry, NonConstantColumnEntryContext context) throws ScriptParsingException;

	public abstract InsnTree makeBulkComputer(ColumnEntryRegistry registry, NonConstantColumnEntryContext context) throws ScriptParsingException;

	@Override
	public boolean hasFieldSetterAndFlag() {
		return this.cache;
	}

	public boolean hasValid() {
		return this.valid != null && this.valid.isUseful(this.params.is_3d());
	}

	public static class NonConstantColumnEntryContext extends ColumnEntryContext {

		public @Nullable FieldCompileContext valueField;
	}
}