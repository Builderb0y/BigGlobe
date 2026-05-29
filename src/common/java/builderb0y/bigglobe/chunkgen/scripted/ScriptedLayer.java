package builderb0y.bigglobe.chunkgen.scripted;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.objectweb.asm.Type;

import net.minecraft.core.Holder;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted2.ScriptColumnEntryParser;
import builderb0y.bigglobe.columns.scripted2.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted2.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted2.ExternalEnvironmentParams;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.scripting.ScriptCatcher;
import builderb0y.bigglobe.scripting.environments.ColorScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.GridScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.MinecraftScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ScriptedLayer extends Layer {

	public final Impl.Catcher script;

	public ScriptedLayer(
		@VerifyNullable Valid valid,
		Holder<Layer> @DefaultEmpty [] children,
		SurfaceScript.@VerifyNullable Catcher before_children,
		SurfaceScript.@VerifyNullable Catcher after_children,
		Impl.Catcher script
	) {
		super(valid, children, before_children, after_children);
		this.script = script;
	}

	@Override
	public void buildDependencyStream(Stream.Builder<Holder<? extends DependencyView>> builder) {
		this.script.streamDirectDependencies().forEach(builder);
	}

	@Override
	public void emitSelfSegments(ScriptedColumn column, BlockSegmentList blocks) {
		BlockSegmentList split = blocks.split(this.validMinY(column), this.validMaxY(column));
		if (split != null) {
			this.script.emitSegments(column, split);
			blocks.mergeAndKeepEverywhere(split);
		}
	}

	public static interface Impl extends Script {

		public abstract void emitSegments(ScriptedColumn column, BlockSegmentList blocks);

		@Wrapper
		public static class Catcher extends ScriptCatcher<Impl> implements ScriptedLayer.Impl, SetBasedMutableDependencyView {

			public final Set<Holder<? extends DependencyView>> dependencies = new HashSet<>();

			public Catcher(ScriptUsage usage) {
				super(usage);
				this.addAllDependencies(usage);
			}

			@Override
			public Set<Holder<? extends DependencyView>> getDependencies() {
				return this.dependencies;
			}

			@Override
			public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
				ClassCompileContext clazz = new ClassCompileContext(
					ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC | ACC_SUPER,
					ClassType.CLASS,
					Type.getInternalName(ScriptedLayer.Impl.class) + '$' + (this.usage.debug_name != null ? this.usage.debug_name : "Generated") + '_' + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
					TypeInfos.OBJECT,
					new TypeInfo[] { type(ScriptedLayer.Impl.class) }
				);
				clazz.addNoArgConstructor(ACC_PUBLIC);
				LazyVarInfo[] bridgeParams = {
					new LazyVarInfo("column", type(ScriptedColumn.class)),
					new LazyVarInfo("blocks", type(BlockSegmentList.class))
				};
				LazyVarInfo[] actualParams = {
					new LazyVarInfo("column", registry.columnCompileContext.columnTypeInfo()),
					new LazyVarInfo("blocks", type(BlockSegmentList.class))
				};
				MethodCompileContext actualMethod = clazz.newMethod(ACC_PUBLIC, "emitSegments", TypeInfos.VOID, actualParams);
				MethodCompileContext bridgeMethod = clazz.newMethod(ACC_PUBLIC, "emitSegments", TypeInfos.VOID, bridgeParams);
				return_(
					invokeInstance(
						load("this", clazz.info),
						actualMethod.info,
						new DirectCastInsnTree(load("column", type(ScriptedColumn.class)), registry.columnCompileContext.columnTypeInfo(), false),
						load("blocks", type(BlockSegmentList.class))
					)
				)
					.emitBytecode(bridgeMethod);
				bridgeMethod.endCode();

				LoadInsnTree loadColumn = load("column", registry.columnCompileContext.columnTypeInfo());
				ScriptColumnEntryParser parser = new ScriptColumnEntryParser(this.usage, clazz, actualMethod, registry.parserFlags()).configureEnvironment((MutableScriptEnvironment environment) -> {
					environment
					.addAll(MathScriptEnvironment.INSTANCE)
					.configure(JavaUtilScriptEnvironment.withoutRandom())
					.addAll(StatelessRandomScriptEnvironment.INSTANCE)
					.configure(MinecraftScriptEnvironment.create())
					.configure(GridScriptEnvironment.createWithSeed(ScriptedColumn.INFO.baseSeed(loadColumn)))
					.configure(ScriptedColumn.baseEnvironment(loadColumn, null, loadColumn.variable.type))
					.addFunctionInvokes(load("blocks", type(BlockSegmentList.class)), BlockSegmentList.class, "getBlockState", "setBlockState", "setBlockStates", "getTopOfSegment", "getBottomOfSegment")
					.addVariableInvokes(load("blocks", type(BlockSegmentList.class)), BlockSegmentList.class, "minY", "maxY")
					.addAll(ColorScriptEnvironment.ENVIRONMENT)
					;
					registry.setupEnvironment(
						environment,
						new ExternalEnvironmentParams()
						.withColumn(loadColumn)
						.trackDependencies(this)
					);
				});
				parser.parseEntireInput().emitBytecode(actualMethod);
				actualMethod.endCode();

				MethodCompileContext getSource = clazz.newMethod(ACC_PUBLIC, "getSource", TypeInfos.STRING);
				return_(ldc(clazz.newConstant(this.usage.getSource(), TypeInfos.STRING))).emitBytecode(getSource);
				getSource.endCode();

				MethodCompileContext getDebugName = clazz.newMethod(ACC_PUBLIC, "getDebugName", TypeInfos.STRING);
				return_(ldc(this.usage.debug_name, TypeInfos.STRING)).emitBytecode(getDebugName);
				getDebugName.endCode();

				try {
					this.script = (ScriptedLayer.Impl)(new ScriptClassLoader(registry.loader).defineClass(clazz, ExpressionParser.CLASS_DUMP_DIRECTORY, this.usage.getSource()).getDeclaredConstructors()[0].newInstance((Object[])(null)));
				}
				catch (Throwable throwable) {
					throw new ScriptParsingException(parser.fatalError().toString(), throwable, null);
				}
			}

			@Override
			public void emitSegments(ScriptedColumn column, BlockSegmentList blocks) {
				NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
				int used = manager.used;
				try {
					this.script.emitSegments(column, blocks);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
				}
				finally {
					manager.used = used;
				}
			}
		}
	}
}