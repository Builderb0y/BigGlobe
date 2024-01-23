package builderb0y.bigglobe.columns.scripted.compile;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.Identifier;

import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptColumnEntryParser;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.VoronoiDataBase;
import builderb0y.bigglobe.columns.scripted.schemas.AccessSchema;
import builderb0y.bigglobe.columns.scripted.schemas.AccessSchema.TypeContext;
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

	public ClassCompileContext mainClass;
	public MutableScriptEnvironment environment;
	public int flagsIndex;
	public List<DataCompileContext> children;
	public MethodCompileContext constructor;

	public DataCompileContext() {
		this.environment = new MutableScriptEnvironment();
		this.children = new ArrayList<>(8);
	}

	public TypeInfo selfType() {
		return this.mainClass.info;
	}

	public abstract ColumnCompileContext root();

	public abstract MutableScriptEnvironment environment();

	public abstract InsnTree loadSelf();

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
		.addEnvironment(MinecraftScriptEnvironment.create())
		.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
		.addEnvironment(this.environment)
		.configureEnvironment((MutableScriptEnvironment environment) -> {
			if (includeY) environment.addVariableLoad("y", TypeInfos.INT);
		})
		.parseEntireInput()
		.emitBytecode(method);
		method.endCode();
	}
}