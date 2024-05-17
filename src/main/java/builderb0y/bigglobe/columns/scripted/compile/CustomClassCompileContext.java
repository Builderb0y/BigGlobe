package builderb0y.bigglobe.columns.scripted.compile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ColumnEntryMemory;
import builderb0y.bigglobe.columns.scripted.types.ClassColumnValueType;
import builderb0y.bigglobe.columns.scripted.types.ClassColumnValueType.ClassColumnValueField;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.UserClassDefiner;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class CustomClassCompileContext extends DataCompileContext {

	public final ColumnCompileContext parent;

	public CustomClassCompileContext(ColumnCompileContext parent, ClassColumnValueType spec) {
		super(parent);
		this.parent = parent;
		this.mainClass = parent.mainClass.newInnerClass(
			ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC,
			Type.getInternalName(ScriptedColumn.class) + '$' + spec.name + '_' + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
			TypeInfos.OBJECT,
			TypeInfo.ARRAY_FACTORY.empty()
		);
		this.constructor = this.mainClass.newMethod(
			ACC_PUBLIC,
			"<init>",
			TypeInfos.VOID,
			Arrays.stream(spec.fields)
			.map((ClassColumnValueField field) -> new LazyVarInfo(
				field.name(),
				parent.getTypeContext(field.type()).type()
			))
			.toArray(LazyVarInfo.ARRAY_FACTORY)
		);
		invokeInstance(load("this", this.mainClass.info), MethodInfo.getConstructor(Object.class)).emitBytecode(this.constructor);
		LoadInsnTree loadSelf = load(new LazyVarInfo("this", this.mainClass.info));
		List<FieldCompileContext> fieldCompileContexts = new ArrayList<>(spec.fields.length);
		for (ClassColumnValueField field : spec.fields) {
			FieldCompileContext fieldContext = this.mainClass.newField(ACC_PUBLIC, field.name(), parent.getTypeContext(field.type()).type());
			putField(
				loadSelf,
				fieldContext.info,
				load(
					field.name(),
					parent.getTypeContext(field.type()).type()
				)
			)
			.emitBytecode(this.constructor);
		}
		UserClassDefiner.addToString(this.mainClass, spec.name, fieldCompileContexts);
	}

	@Override
	public Map<ColumnEntry, ColumnEntryMemory> getMemories() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InsnTree loadColumn() {
		throw new UnsupportedOperationException();
	}

	@Override
	public InsnTree loadSeed(@Nullable InsnTree salt) {
		throw new UnsupportedOperationException();
	}

	@Override
	public FieldInfo flagsField(int index) {
		throw new UnsupportedOperationException();
	}
}