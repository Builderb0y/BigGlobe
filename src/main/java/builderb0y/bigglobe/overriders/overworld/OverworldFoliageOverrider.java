package builderb0y.bigglobe.overriders.overworld;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.OverworldColumn;
import builderb0y.bigglobe.overriders.ScriptStructures;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class OverworldFoliageOverrider {

	@Wrapper
	public static class Holder extends OverworldDataOverrider.Holder {

		public Holder(String script) throws ScriptParsingException {
			super(
				new ScriptParser<>(OverworldDataOverrider.class, script)
				.addEnvironment(OverworldFoliageOverrider.Environment.INSTANCE)
			);
		}
	}

	public static class Environment extends OverworldDataOverrider.Environment {

		public static final Environment INSTANCE = new Environment();

		public Environment() {
			super();
			this
			.addVariableLoad("rawGeneration", 3, TypeInfos.BOOLEAN)
			.addVariableLoad("structureStarts", 1, type(ScriptStructures.class));

			InsnTree columnLoader = load("column", 2, type(OverworldColumn.class));
			this.addDistanceFunctions(columnLoader);
			this.addVariableGetField(columnLoader, FieldInfo.getField(OverworldColumn.class, "foliage"));
		}
	}
}