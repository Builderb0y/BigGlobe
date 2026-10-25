package builderb0y.bigglobe.chunkgen.scripted;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import org.objectweb.asm.Type;
import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptColumnEntryParser;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.ColorScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.GridScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.MinecraftScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
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

	public final Impl.Holder script;

	public ScriptedLayer(
		@VerifyNullable Valid valid,
		Holder<Layer> @DefaultEmpty [] children,
		SurfaceScript.@VerifyNullable Holder before_children,
		SurfaceScript.@VerifyNullable Holder after_children,
		Impl.Holder script
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
		public static class Holder extends ScriptHolder<ScriptedLayer.Impl> implements ScriptedLayer.Impl, SetBasedMutableDependencyView {

			public final Set<net.minecraft.core.Holder<? extends DependencyView>> dependencies = new HashSet<>();

			public Holder(ScriptUsage usage) {
				super(usage);
				this.addAllDependencies(usage);
			}

			@Override
			public Set<net.minecraft.core.Holder<? extends DependencyView>> getDependencies() {
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
					new LazyVarInfo("column", registry.columnContext.columnType()),
					new LazyVarInfo("blocks", type(BlockSegmentList.class))
				};
				MethodCompileContext actualMethod = clazz.newMethod(ACC_PUBLIC, "emitSegments", TypeInfos.VOID, actualParams);
				MethodCompileContext bridgeMethod = clazz.newMethod(ACC_PUBLIC, "emitSegments", TypeInfos.VOID, bridgeParams);
				return_(
					invokeInstance(
						load("this", clazz.info),
						actualMethod.info,
						new DirectCastInsnTree(load("column", type(ScriptedColumn.class)), registry.columnContext.columnType(), false),
						load("blocks", type(BlockSegmentList.class))
					)
				)
					.emitBytecode(bridgeMethod);
				bridgeMethod.endCode();

				LoadInsnTree loadColumn = load("column", registry.columnContext.columnType());
				ScriptColumnEntryParser parser = new ScriptColumnEntryParser(this.usage, clazz, actualMethod, registry.parserFlags()).configureEnvironment((MutableScriptEnvironment environment) -> {
					environment
						.addAll(MathScriptEnvironment.INSTANCE)
						.addAll(StatelessRandomScriptEnvironment.INSTANCE)
						.configure(MinecraftScriptEnvironment.create())
						.configure(GridScriptEnvironment.createWithSeed(ScriptedColumn.INFO.baseSeed(loadColumn)))
						.configure(ScriptedColumn.baseEnvironment(loadColumn))
						.addFunctionInvokes(load("blocks", type(BlockSegmentList.class)), BlockSegmentList.class, "getBlockState", "setBlockState", "setBlockStates", "getTopOfSegment", "getBottomOfSegment")
						.addVariableInvokes(load("blocks", type(BlockSegmentList.class)), BlockSegmentList.class, "minY", "maxY")
						.addAll(ColorScriptEnvironment.ENVIRONMENT)
					;
					registry.setupExternalEnvironment(
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