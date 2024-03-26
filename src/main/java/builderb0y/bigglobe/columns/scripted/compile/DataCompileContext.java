package builderb0y.bigglobe.columns.scripted.compile;

import java.util.*;

import net.minecraft.util.Identifier;

import builderb0y.bigglobe.columns.scripted.ColumnValueDependencyHolder;
import builderb0y.bigglobe.columns.scripted.ScriptColumnEntryParser;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ColumnEntryMemory;
import builderb0y.bigglobe.scripting.environments.MinecraftScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class DataCompileContext {

	public DataCompileContext parent;
	public List<DataCompileContext> children;
	public ClassCompileContext mainClass;
	public MethodCompileContext constructor;
	public int flagsIndex;

	public DataCompileContext(DataCompileContext parent) {
		this.parent = parent;
		this.children = new ArrayList<>(8);
		if (parent != null) parent.children.add(this);
	}

	public abstract Map<ColumnEntry, ColumnEntryMemory> getMemories();

	public TypeInfo selfType() {
		return this.mainClass.info;
	}

	public ColumnCompileContext root() {
		DataCompileContext context = this;
		for (DataCompileContext next; (next = context.parent) != null; context = next);
		return (ColumnCompileContext)(context);
	}

	public InsnTree loadSelf() {
		return load("this", this.mainClass.info);
	}

	public abstract InsnTree loadColumn();

	public abstract InsnTree loadSeed(InsnTree salt);

	public abstract FieldInfo flagsField(int index);

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
		boolean includeY,
		ColumnValueDependencyHolder dependencies
	)
	throws ScriptParsingException {
		new ScriptColumnEntryParser(script, this.mainClass, method)
		.addEnvironment(MathScriptEnvironment.INSTANCE)
		.addEnvironment(MinecraftScriptEnvironment.create())
		.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
		.addEnvironment(ScriptedColumn.baseEnvironment(this.loadColumn()))
		.configureEnvironment((MutableScriptEnvironment environment) -> {
			if (includeY) environment.addVariableLoad("y", TypeInfos.INT);
			this.root().registry.setupInternalEnvironment(environment, this, dependencies);
		})
		.parseEntireInput()
		.emitBytecode(method);
		method.endCode();
	}
}