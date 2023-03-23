package builderb0y.bigglobe.features;

import java.util.*;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

import builderb0y.autocodec.annotations.Hidden;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.chunkgen.FeatureColumns;
import builderb0y.bigglobe.chunkgen.FeatureColumns.ColumnSupplier;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.scripting.*;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.scripting.bytecode.CastingSupport2;
import builderb0y.scripting.bytecode.ClassCompileContext;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ScriptedFeature extends Feature<ScriptedFeature.Config> {

	public static final String[] NO_INPUTS = {};

	public ScriptedFeature(Codec<Config> configCodec) {
		super(configCodec);
	}

	public ScriptedFeature() {
		this(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(Config.class));
	}

	@Override
	public boolean generate(FeatureContext<Config> context) {
		return generate(context, context.getConfig().script);
	}

	public static boolean generate(FeatureContext<?> context, FeatureScript.Holder script) {
		BlockPos origin = context.getOrigin();
		ColumnSupplier oldSupplier = FeatureColumns.FEATURE_COLUMNS.get();
		WorldColumn column = FeatureColumns.get(context.getWorld(), origin.getX(), origin.getZ(), oldSupplier);
		try {
			FeatureColumns.FEATURE_COLUMNS.set(ColumnSupplier.fixedPosition(column));
			return script.generate(
				new WorldWrapper(context.getWorld(), Permuter.from(context.getRandom())),
				origin.getX(),
				origin.getY(),
				origin.getZ(),
				column
			);
		}
		finally {
			FeatureColumns.FEATURE_COLUMNS.set(oldSupplier);
		}
	}

	public static record Config(FeatureScript.Holder script) implements FeatureConfig {}

	public static interface FeatureScript extends Script {

		public abstract boolean generate(
			WorldWrapper world,
			int originX,
			int originY,
			int originZ,
			WorldColumn column
		);

		@Wrapper
		public static class Holder extends ScriptHolder<FeatureScript> implements FeatureScript {

			@Hidden
			public Holder(String script, Map<String, String> inputs, String[] expectedInputs) throws ScriptParsingException {
				super(
					setupParser(
						new ScriptParserWithInputs<>(
							FeatureScript.class,
							script
						),
						inputs,
						expectedInputs
					)
					.parse()
				);
			}

			public Holder(String script) throws ScriptParsingException {
				this(script, Collections.emptyMap(), NO_INPUTS);
			}

			@Override
			public boolean generate(WorldWrapper world, int originX, int originY, int originZ, WorldColumn column) {
				try {
					return this.script.generate(world, originX, originY, originZ, column);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return false;
				}
			}
		}
	}

	public static class ScriptParserWithInputs<I> extends ScriptParser<I> {

		public final Map<String, InsnTree> initializers = new LinkedHashMap<>(4);

		public ScriptParserWithInputs(Class<I> implementingClass, String input) {
			super(implementingClass, input);
		}

		@Override
		public InsnTree parseEntireInput() throws ScriptParsingException {
			return seq(
				Stream.concat(
					this.initializers.values().stream(),
					Stream.of(super.parseEntireInput())
				)
				.toArray(InsnTree.ARRAY_FACTORY)
			);
		}
	}

	public static <P extends ExpressionParser> P setupParser(P parser, Map<String, String> inputs, String[] expectedInputs) throws ScriptParsingException {
		if (inputs.size() != expectedInputs.length || !inputs.keySet().containsAll(Arrays.asList(expectedInputs))) {
			throw new ScriptParsingException("Input mismatch: Expected " + Arrays.toString(expectedInputs) + ", got " + inputs.keySet(), null);
		}
		parser
		.addEnvironment(JavaUtilScriptEnvironment.ALL)
		.addEnvironment(MathScriptEnvironment.INSTANCE)
		.addEnvironment(new MinecraftScriptEnvironment(
			load("world", 1, WorldWrapper.TYPE)
		))
		.addEnvironment(NbtScriptEnvironment.INSTANCE)
		.addEnvironment(
			new MutableScriptEnvironment()
			.addVariableLoad("originX", 2, TypeInfos.INT)
			.addVariableLoad("originY", 3, TypeInfos.INT)
			.addVariableLoad("originZ", 4, TypeInfos.INT)
		)
		.addEnvironment(new RandomScriptEnvironment(
			getField(
				load("world", 1, WorldWrapper.TYPE),
				field(
					Opcodes.ACC_PUBLIC,
					WorldWrapper.TYPE,
					"permuter",
					type(Permuter.class)
				)
			)
		))
		.addEnvironment(new ColumnYScriptEnvironment(
			load("column", 5, ColumnYScriptEnvironment.WORLD_COLUMN_TYPE),
			CastingSupport2.primitiveCast(load("originY", 3, TypeInfos.INT), TypeInfos.DOUBLE),
			false
		));
		if (!inputs.isEmpty()) {
			MutableScriptEnvironment environment = parser.environment.mutable();
			for (String inputName : expectedInputs) {
				String inputSource = inputs.get(inputName);
				assert inputSource != null;
				ClassCompileContext classCopy = new ClassCompileContext(parser.clazz.node.access, parser.clazz.info);
				MethodCompileContext methodCopy = new MethodCompileContext(classCopy, new MethodNode(), parser.method.info);
				ExpressionParser parserCopy = setupParser(new ExpressionParser(inputSource, classCopy, methodCopy), Collections.emptyMap(), NO_INPUTS);
				InsnTree inputTree = parserCopy.nextScript();
				VariableDeclarationInsnTree declaration = parser.environment.user().newVariable(inputName, inputTree.getTypeInfo());
				InsnTree initializer = seq(declaration, store(declaration.loader.variable, inputTree));
				environment
				.addVariable(inputName, load(declaration.loader.variable))
				.addVariable('$' + inputName, inputTree);
				((ScriptParserWithInputs<?>)(parser)).initializers.put(inputName, initializer);
			}
		}
		return parser;
	}
}