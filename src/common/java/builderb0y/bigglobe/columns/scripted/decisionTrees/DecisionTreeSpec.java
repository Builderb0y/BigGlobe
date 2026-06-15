package builderb0y.bigglobe.columns.scripted.decisionTrees;

import net.minecraft.core.Holder;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.classes.compile.ConstantFormatException;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.codecs.TypelessCoderRegistry;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SimpleDependencyView;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;

@UseCoder(name = "REGISTRY", in = DecisionTreeSpec.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface DecisionTreeSpec extends CoderRegistryTyped<DecisionTreeSpec>, SimpleDependencyView {

	public static final TypelessCoderRegistry<DecisionTreeSpec> REGISTRY = new TypelessCoderRegistry<>(ReifiedType.from(DecisionTreeSpec.class), BigGlobeAutoCodec.AUTO_CODEC);
	public static final Object INITIALIZER = new Object() {{
		REGISTRY.register(ConditionDecisionTreeSpec.class);
		REGISTRY.register(   BorderDecisionTreeSpec.class);
		REGISTRY.register(   ResultDecisionTreeSpec.class);
	}};

	public abstract InsnTree emitTree(DecisionTreeContext context, Holder<DecisionTreeSpec> self) throws ScriptParsingException, ConstantFormatException;
}