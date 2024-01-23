package builderb0y.bigglobe.columns.scripted.compile;

import java.util.Map;

import org.objectweb.asm.Type;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.schemas.AccessSchema;
import builderb0y.bigglobe.columns.scripted.schemas.AccessSchema.TypeContext;
import builderb0y.bigglobe.columns.scripted.schemas.ClassAccessSchema;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class CustomClassCompileContext extends DataCompileContext {

	public final ColumnCompileContext parent;

	public CustomClassCompileContext(ColumnCompileContext parent, ClassAccessSchema schema) {
		parent.children.add(this);
		this.parent = parent;
		this.mainClass = new ClassCompileContext(
			ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC,
			ClassType.CLASS,
			Type.getObjectType(Type.getInternalName(ScriptedColumn.class) + '$' + schema.class_name + '_' + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement()),
			TypeInfos.OBJECT,
			TypeInfo.ARRAY_FACTORY.empty()
		);
		this.constructor = this.mainClass.newMethod(
			ACC_PUBLIC,
			"<init>",
			TypeInfos.VOID,
			schema
			.exports
			.entrySet()
			.stream()
			.map((Map.Entry<String, AccessSchema> entry) -> new LazyVarInfo(
				entry.getKey(),
				parent.getSchemaType(entry.getValue()).exposedType()
			))
			.toArray(LazyVarInfo.ARRAY_FACTORY)
		);
		LoadInsnTree loadSelf = load(new LazyVarInfo("this", this.mainClass.info));
		for (Map.Entry<String, AccessSchema> entry : schema.exports.entrySet()) {
			FieldCompileContext field = this.mainClass.newField(ACC_PUBLIC, entry.getKey(), parent.getSchemaType(entry.getValue()).exposedType());
			putField(
				loadSelf,
				field.info,
				load(
					entry.getKey(),
					parent.getSchemaType(entry.getValue()).exposedType()
				)
			)
			.emitBytecode(this.constructor);
		}
	}

	@Override
	public ColumnCompileContext root() {
		return this.parent.root();
	}

	@Override
	public MutableScriptEnvironment environment() {
		return this.parent.environment().addAll(this.environment);
	}

	@Override
	public InsnTree loadSelf() {
		return load("this", this.selfType());
	}

	@Override
	public InsnTree loadColumn() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InsnTree loadSeed() {
		throw new UnsupportedOperationException();
	}

	@Override
	public FieldInfo flagsField(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypeInfo voronoiBaseType() {
		throw new UnsupportedOperationException();
	}
}