package builderb0y.bigglobe.columns.scripted.compile;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Type;

import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.VoronoiDataBase;
import builderb0y.bigglobe.columns.scripted.schemas.AccessSchema;
import builderb0y.bigglobe.columns.scripted.schemas.AccessSchema.AccessContext;
import builderb0y.bigglobe.columns.scripted.types.ColumnValueType;
import builderb0y.bigglobe.columns.scripted.types.ColumnValueType.TypeContext;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ColumnCompileContext extends DataCompileContext {

	public final ColumnEntryRegistry registry;
	public final Map<ColumnValueType, TypeContext> columnValueTypeInfos = new HashMap<>(16);
	public final Map<AccessSchema, AccessContext> accessSchemaTypeInfos = new HashMap<>(16);

	public ColumnCompileContext(ColumnEntryRegistry registry) {
		this.registry = registry;

		this
		.environment
		.addFieldInvoke("cellX", MethodInfo.getMethod(VoronoiDataBase.class, "get_cell_x"))
		.addFieldInvoke("cellZ", MethodInfo.getMethod(VoronoiDataBase.class, "get_cell_z"))
		.addFieldInvoke("centerX", MethodInfo.getMethod(VoronoiDataBase.class, "get_center_x"))
		.addFieldInvoke("centerZ", MethodInfo.getMethod(VoronoiDataBase.class, "get_center_z"))
		.addFieldInvoke("softDistanceSquared", MethodInfo.getMethod(VoronoiDataBase.class, "get_soft_distance_squared"))
		.addFieldInvoke("softDistance", MethodInfo.getMethod(VoronoiDataBase.class, "get_soft_distance"))
		.addFieldInvoke("hardDistance", MethodInfo.getMethod(VoronoiDataBase.class, "get_hard_distance"))
		.addFieldInvoke("hardDistanceSquared", MethodInfo.getMethod(VoronoiDataBase.class, "get_hard_distance_squared"))
		.addFieldInvoke("euclideanDistanceSquared", MethodInfo.getMethod(VoronoiDataBase.class, "get_euclidean_distance_squared"))
		.addFieldInvoke("euclideanDistance", MethodInfo.getMethod(VoronoiDataBase.class, "get_euclidean_distance"))
		;

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
				x = new LazyVarInfo("x", TypeInfos.INT),
				z = new LazyVarInfo("z", TypeInfos.INT),
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
			lookup.endCode();
		}
	}

	@Override
	public ColumnCompileContext root() {
		return this;
	}

	@Override
	public MutableScriptEnvironment environment() {
		return new MutableScriptEnvironment().addAll(this.environment);
	}

	public TypeContext getTypeContext(ColumnValueType type) {
		//note: do not use computeIfAbsent(),
		//because schema.createType(this) could recursively create other types.
		TypeContext result = this.columnValueTypeInfos.get(type);
		if (result == null) {
			this.columnValueTypeInfos.put(type, result = type.createType(this));
		}
		return result;
	}

	public AccessContext getAccessContext(AccessSchema schema) {
		//note: do not use computeIfAbsent(),
		//because schema.createType(this) could recursively create other types.
		AccessContext result = this.accessSchemaTypeInfos.get(schema);
		if (result == null) {
			this.accessSchemaTypeInfos.put(schema, result = schema.createType(this));
		}
		return result;
	}

	@Override
	public InsnTree loadSelf() {
		return load("this", this.mainClass.info);
	}

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