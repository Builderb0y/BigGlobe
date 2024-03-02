package builderb0y.bigglobe.columns.scripted.compile;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Type;

import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.VoronoiDataBase;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.AccessSchema.AccessContext;
import builderb0y.bigglobe.columns.scripted.types.ColumnValueType;
import builderb0y.bigglobe.columns.scripted.types.ColumnValueType.TypeContext;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ColumnCompileContext extends DataCompileContext {

	public final ColumnEntryRegistry registry;
	public final Map<ColumnValueType, TypeContext> columnValueTypeInfos = new HashMap<>(16);
	public final Map<AccessSchema, AccessContext> accessSchemaTypeInfos = new HashMap<>(16);

	public ColumnCompileContext(ColumnEntryRegistry registry) {
		super(null);
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
			LazyVarInfo self;
			this.constructor = this.mainClass.newMethod(
				ACC_PUBLIC,
				"<init>",
				TypeInfos.VOID,
				ScriptedColumn.PARAMETER_VAR_INFOS
			);
			self = new LazyVarInfo("this", this.mainClass.info);
			invokeInstance(
				load(self),
				new MethodInfo(
					ACC_PUBLIC,
					type(ScriptedColumn.class),
					"<init>",
					TypeInfos.VOID,
					ScriptedColumn.PARAMETER_TYPE_INFOS
				),
				ScriptedColumn.LOADERS
			)
			.emitBytecode(this.constructor);
		}
		{
			MethodCompileContext lookup = this.mainClass.newMethod(ACC_PUBLIC | ACC_STATIC, "lookup", type(MethodHandles.Lookup.class));
			return_(invokeStatic(MethodInfo.getMethod(MethodHandles.class, "lookup"))).emitBytecode(lookup);
			lookup.endCode();
		}
		{
			InsnTree loadSelf = load("this", this.mainClass.info);
			MethodCompileContext blankCopy = this.mainClass.newMethod(ACC_PUBLIC, "blankCopy", type(ScriptedColumn.class));
			return_(
				newInstance(
					this.constructor.info,
					Arrays
					.stream(ScriptedColumn.CONSTRUCTOR_PARAMETERS)
					.map(Parameter::getName)
					.map((String name) -> FieldInfo.getField(ScriptedColumn.class, name))
					.map((FieldInfo field) -> getField(loadSelf, field))
					.toArray(InsnTree[]::new)
				)
			)
			.emitBytecode(blankCopy);
			blankCopy.endCode();
		}
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

	public TypeInfo columnType() {
		return this.mainClass.info;
	}

	@Override
	public InsnTree loadColumn() {
		return this.loadSelf();
	}

	@Override
	public InsnTree loadSeed(InsnTree salt) {
		return ScriptedColumn.INFO.seed(this.loadColumn());
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
	public void prepareForCompile() {
		MethodCompileContext clear = this.mainClass.newMethod(ACC_PUBLIC, "clear", TypeInfos.VOID);
		for (int index = 0, max = this.flagsIndex >>> 5; index <= max; index++) {
			FieldCompileContext flagsField = this.mainClass.newField(ACC_PUBLIC, "flags_" + index, TypeInfos.INT);
			putField(this.loadSelf(), flagsField.info, ldc(0)).emitBytecode(clear);
		}
		clear.node.visitInsn(RETURN);
		clear.endCode();
		super.prepareForCompile();
	}
}