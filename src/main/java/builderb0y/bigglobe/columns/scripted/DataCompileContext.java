package builderb0y.bigglobe.columns.scripted;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Type;

import net.minecraft.util.Identifier;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn.VoronoiDataBase;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.TypeContext;
import builderb0y.bigglobe.settings.VoronoiDiagram2D;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.conditions.BooleanToConditionTree;
import builderb0y.scripting.bytecode.tree.flow.IfElseInsnTree;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class DataCompileContext {

	public ColumnEntryRegistry registry;
	public ClassCompileContext mainClass;
	public MutableScriptEnvironment environment;
	public int flagsIndex;
	public List<DataCompileContext> children;
	public MethodCompileContext constructor;

	public DataCompileContext(ColumnEntryRegistry registry) {
		this.registry = registry;
		this.environment = (
			new MutableScriptEnvironment()
			.addFieldInvoke("cellX", MethodInfo.getMethod(VoronoiDataBase.class, "get_cell_x"))
			.addFieldInvoke("cellZ", MethodInfo.getMethod(VoronoiDataBase.class, "get_cell_z"))
			.addFieldInvoke("centerX", MethodInfo.getMethod(VoronoiDataBase.class, "get_center_x"))
			.addFieldInvoke("centerZ", MethodInfo.getMethod(VoronoiDataBase.class, "get_center_z"))
			.addFieldInvoke("softDistanceSquared", MethodInfo.getMethod(VoronoiDataBase.class, "get_soft_distance_squared"))
			.addFieldInvoke("softDistance", MethodInfo.getMethod(VoronoiDataBase.class, "get_soft_distance"))
			.addFieldInvoke("hardDistance", MethodInfo.getMethod(VoronoiDataBase.class, "get_hard_distance"))
			.addFieldInvoke("hardDistanceSquared", MethodInfo.getMethod(VoronoiDataBase.class, "get_hard_distance_squared"))
		);
		this.children = new ArrayList<>(8);
	}

	public TypeInfo selfType() {
		return this.mainClass.info;
	}

	public abstract TypeContext getSchemaType(AccessSchema schema);

	public abstract InsnTree loadSelf();

	public abstract TypeInfo columnType();

	public abstract InsnTree loadColumn();

	public abstract InsnTree loadSeed();

	public abstract FieldInfo flagsField(int index);

	public abstract TypeInfo voronoiBaseType();

	public void prepareForCompile() {
		this.constructor.node.visitInsn(RETURN);
		this.constructor.endCode();
		this.children.forEach(DataCompileContext::prepareForCompile);
	}

	public static String internalName(Identifier selfID, int fieldIndex) {
		StringBuilder builder = (
			new StringBuilder(selfID.getNamespace().length() + selfID.getPath().length() + 16)
			.append(selfID.getNamespace())
			.append('_')
			.append(selfID.getPath())
		);
		for (int index = 0, length = builder.length(); index < length; index++) {
			char old = builder.charAt(index);
			if (!((old >= 'a' && old <= 'z') || (old >= '0' && old <= '9'))) {
				builder.setCharAt(index, '_');
			}
		}
		return builder.append('_').append(fieldIndex).toString();
	}

	public static int flagsFieldBitmask(int index) {
		//note: *because java*, this is equivalent to 1 << (index & 31).
		//this is one of the very few places where such a weird rule is actually useful.
		return 1 << index;
	}

	public void setMethodCode(
		MethodCompileContext method,
		ScriptUsage<GenericScriptTemplateUsage> script,
		boolean includeY
	)
	throws ScriptParsingException {
		new ScriptColumnEntryParser(script, this.mainClass, method)
		.addEnvironment(MathScriptEnvironment.INSTANCE)
		.addEnvironment(this.environment)
		.configureEnvironment((MutableScriptEnvironment environment) -> {
			if (includeY) environment.addVariableLoad("y", TypeInfos.INT);
		})
		.parseEntireInput()
		.emitBytecode(method);
		method.endCode();
	}

	public void generateGuardedComputer(
		MethodCompileContext computeMethod,
		MethodInfo testMethod,
		MethodInfo actuallyComputeMethod,
		ConstantValue fallback
	)
	throws ScriptParsingException {
		new IfElseInsnTree(
			new BooleanToConditionTree(
				invokeInstance(this.loadSelf(), testMethod)
			),
			return_(invokeInstance(this.loadSelf(), actuallyComputeMethod)),
			return_(ldc(fallback)),
			TypeInfos.VOID
		)
		.emitBytecode(computeMethod);
	}

	public static class ColumnCompileContext extends DataCompileContext {

		public final Map<AccessSchema, TypeContext> accessSchemaTypeInfos = new HashMap<>(16);

		public ColumnCompileContext(ColumnEntryRegistry registry) {
			super(registry);
			this.mainClass = new ClassCompileContext(
				ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC,
				ClassType.CLASS,
				Type.getInternalName(ScriptedColumn.class) + "$Generated_" + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
				type(ScriptedColumn.class),
				new TypeInfo[0]
			);
			{
				LazyVarInfo self, seed, x, z, minY, maxY;
				this.constructor = this.mainClass.newMethod(
					ACC_PUBLIC,
					"<init>",
					TypeInfos.VOID,
					seed = new LazyVarInfo("seed", TypeInfos.LONG),
					x    = new LazyVarInfo("x",    TypeInfos.INT),
					z    = new LazyVarInfo("z",    TypeInfos.INT),
					minY = new LazyVarInfo("minY", TypeInfos.INT),
					maxY = new LazyVarInfo("maxY", TypeInfos.INT)
				);
				self = new LazyVarInfo("this", this.constructor.clazz.info);
				invokeInstance(
					load(self),
					new MethodInfo(
						ACC_PUBLIC,
						type(ScriptedColumn.class),
						"<init>",
						TypeInfos.VOID,
						TypeInfos.LONG,
						TypeInfos.INT,
						TypeInfos.INT,
						TypeInfos.INT,
						TypeInfos.INT
					),
					load(seed),
					load(x),
					load(z),
					load(minY),
					load(maxY)
				)
				.emitBytecode(this.constructor);
			}
			{
				MethodCompileContext lookup = this.mainClass.newMethod(ACC_PUBLIC | ACC_STATIC, "lookup", type(MethodHandles.Lookup.class));
				return_(invokeStatic(MethodInfo.getMethod(MethodHandles.class, "lookup"))).emitBytecode(lookup);
			}
		}

		@Override
		public TypeContext getSchemaType(AccessSchema schema) {
			//note: do not use computeIfAbsent(),
			//because schema.createType(this) could recursively create other types.
			TypeContext result = this.accessSchemaTypeInfos.get(schema);
			if (result == null) {
				result = schema.createType(this);
				this.accessSchemaTypeInfos.put(schema, result);
			}
			return result;
		}

		@Override
		public InsnTree loadSelf() {
			return load("this", this.mainClass.info);
		}

		@Override
		public TypeInfo columnType() {
			return this.mainClass.info;
		}

		@Override
		public InsnTree loadColumn() {
			return this.loadSelf();
		}

		@Override
		public InsnTree loadSeed() {
			return getField(this.loadColumn(), new FieldInfo(ACC_PUBLIC, type(ScriptedColumn.class), "seed", TypeInfos.LONG));
		}

		@Override
		public FieldInfo flagsField(int index) {
			return new FieldInfo(
				ACC_PUBLIC,
				this.mainClass.info,
				"flags_" + (index >>> 5),
				TypeInfos.INT
			);
		}

		@Override
		public TypeInfo voronoiBaseType() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void prepareForCompile() {
			for (int index = 0, max = this.flagsIndex >>> 5; index <= max; index++) {
				this.mainClass.newField(ACC_PUBLIC, "flags_" + index, TypeInfos.INT);
			}
			super.prepareForCompile();
		}
	}

	public static class VoronoiBaseCompileContext extends DataCompileContext {

		public final ColumnCompileContext parent;

		public VoronoiBaseCompileContext(ColumnCompileContext parent) {
			super(parent.registry);
			parent.children.add(this);
			this.flagsIndex = 3;
			this.parent = parent;
			this.mainClass = parent.mainClass.newInnerClass(
				ACC_PUBLIC | ACC_ABSTRACT | ACC_SYNTHETIC,
				Type.getInternalName(VoronoiDataBase.class) + "$Generated$Base_" + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
				type(VoronoiDataBase.class),
				new TypeInfo[0]
			);
			FieldCompileContext columnField = this.mainClass.newField(ACC_PUBLIC | ACC_FINAL, "column", parent.mainClass.info);

			{
				LazyVarInfo column, cell, baseSeed;
				this.constructor = this.mainClass.newMethod(
					ACC_PUBLIC,
					"<init>",
					TypeInfos.VOID,
					column = new LazyVarInfo("column", parent.columnType()),
					cell = new LazyVarInfo("cell", type(VoronoiDiagram2D.Cell.class)),
					baseSeed = new LazyVarInfo("baseSeed", TypeInfos.LONG)
				);
				LazyVarInfo self = new LazyVarInfo("this", this.constructor.clazz.info);
				invokeInstance(
					load(self),
					new MethodInfo(
						ACC_PUBLIC,
						type(VoronoiDataBase.class),
						"<init>",
						TypeInfos.VOID,
						type(VoronoiDiagram2D.Cell.class),
						TypeInfos.LONG
					),
					load(cell),
					load(baseSeed)
				)
				.emitBytecode(this.constructor);
				putField(load(self), columnField.info, load(column)).emitBytecode(this.constructor);
			}

			{
				MethodCompileContext column = this.mainClass.newMethod(ACC_PUBLIC, "column", type(ScriptedColumn.class) /* do not use synthetic subclass, because we want to override. */);
				LazyVarInfo self = new LazyVarInfo("this", column.clazz.info);
				return_(
					getField(
						load(self),
						columnField.info
					)
				)
				.emitBytecode(column);
			}
		}

		@Override
		public TypeContext getSchemaType(AccessSchema schema) {
			return this.parent.getSchemaType(schema);
		}

		@Override
		public InsnTree loadSelf() {
			return load("this", this.selfType());
		}

		@Override
		public TypeInfo columnType() {
			return this.parent.columnType();
		}

		@Override
		public InsnTree loadColumn() {
			return getField(
				this.loadSelf(),
				new FieldInfo(
					ACC_PUBLIC,
					type(VoronoiDataBase.class),
					"column",
					this.columnType()
				)
			);
		}

		@Override
		public InsnTree loadSeed() {
			return getField(
				this.loadSelf(),
				new FieldInfo(
					ACC_PUBLIC,
					type(VoronoiDataBase.class),
					"seed",
					TypeInfos.LONG
				)
			);
		}

		@Override
		public FieldInfo flagsField(int index) {
			throw new UnsupportedOperationException();
		}

		@Override
		public TypeInfo voronoiBaseType() {
			return this.selfType();
		}
	}

	public static class VoronoiImplCompileContext extends DataCompileContext {

		public final VoronoiBaseCompileContext parent;

		public VoronoiImplCompileContext(VoronoiBaseCompileContext parent, long seed) {
			super(parent.registry);
			parent.children.add(this);
			this.parent = parent;
			this.flagsIndex = VoronoiDataBase.BUILTIN_FLAG_COUNT;
			this.mainClass = parent.mainClass.newInnerClass(
				ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC,
				Type.getInternalName(VoronoiDataBase.class) + "$Generated$Impl_" + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
				parent.mainClass.info,
				new TypeInfo[0]
			);

			LazyVarInfo column, cell;
			this.constructor = this.mainClass.newMethod(
				ACC_PUBLIC,
				"<init>",
				TypeInfos.VOID,
				column = new LazyVarInfo("column", type(ScriptedColumn.class)),
				cell = new LazyVarInfo("cell", type(VoronoiDiagram2D.Cell.class))
			);
			LazyVarInfo self = new LazyVarInfo("this", this.constructor.clazz.info);
			invokeInstance(
				load(self),
				new MethodInfo(
					ACC_PUBLIC,
					parent.mainClass.info,
					"<init>",
					TypeInfos.VOID,
					type(ScriptedColumn.class),
					type(VoronoiDiagram2D.Cell.class),
					TypeInfos.LONG
				),
				load(column),
				load(cell),
				ldc(seed)
			)
			.emitBytecode(this.constructor);
		}

		@Override
		public TypeContext getSchemaType(AccessSchema schema) {
			return this.parent.getSchemaType(schema);
		}

		@Override
		public InsnTree loadSelf() {
			return load("this", this.selfType());
		}

		@Override
		public TypeInfo columnType() {
			return this.parent.columnType();
		}

		@Override
		public InsnTree loadColumn() {
			return getField(
				this.loadSelf(),
				new FieldInfo(
					ACC_PUBLIC,
					this.voronoiBaseType(),
					"column",
					this.columnType()
				)
			);
		}

		@Override
		public InsnTree loadSeed() {
			return getField(
				this.loadSelf(),
				new FieldInfo(
					ACC_PUBLIC,
					type(VoronoiDataBase.class),
					"seed",
					TypeInfos.LONG
				)
			);
		}

		@Override
		public FieldInfo flagsField(int index) {
			return new FieldInfo(
				ACC_PUBLIC,
				(index >>> 5) == 0 ? type(VoronoiDataBase.class) : this.mainClass.info,
				"flags_" + (index >>> 5),
				TypeInfos.INT
			);
		}

		@Override
		public TypeInfo voronoiBaseType() {
			return this.parent.voronoiBaseType();
		}

		@Override
		public void prepareForCompile() {
			for (int index = 1, max = this.flagsIndex >>> 5; index <= max; index++) {
				this.mainClass.newField(ACC_PUBLIC, "flags_" + index, TypeInfos.INT);
			}
			super.prepareForCompile();
		}
	}
}