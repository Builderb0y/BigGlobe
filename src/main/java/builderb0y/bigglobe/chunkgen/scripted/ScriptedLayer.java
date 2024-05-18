package builderb0y.bigglobe.chunkgen.scripted;

import org.objectweb.asm.Type;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptColumnEntryParser;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ExternalEnvironmentParams;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.GridScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.MinecraftScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ScriptedLayer extends Layer {

	public final Impl.Holder script;

	public ScriptedLayer(
		@VerifyNullable Valid valid,
		Layer @DefaultEmpty [] children,
		SurfaceScript.@VerifyNullable Holder before_children,
		SurfaceScript.@VerifyNullable Holder after_children,
		Impl.Holder script
	) {
		super(valid, children, before_children, after_children);
		this.script = script;
	}

	@Override
	public void emitSelfSegments(ScriptedColumn column, BlockSegmentList blocks) {
		this.script.emitSegments(column, blocks);
	}

	public static interface Impl extends Script {

		public abstract void emitSegments(ScriptedColumn column, BlockSegmentList blocks);

		@Wrapper
		public static class Holder extends ScriptHolder<Impl> implements Impl {

			public Holder(ScriptUsage usage) {
				super(usage);
			}

			@Override
			public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
				ClassCompileContext clazz = new ClassCompileContext(
					ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC,
					ClassType.CLASS,
					Type.getInternalName(Impl.class) + '$' + (this.usage.debug_name != null ? this.usage.debug_name : "Generated") + '_' + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
					TypeInfos.OBJECT,
					new TypeInfo[] { type(Impl.class) }
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
						new DirectCastInsnTree(load("column", type(ScriptedColumn.class)), registry.columnContext.columnType()),
						load("blocks", type(BlockSegmentList.class))
					)
				)
				.emitBytecode(bridgeMethod);
				bridgeMethod.endCode();

				LoadInsnTree loadColumn = load("column", registry.columnContext.columnType());
				ScriptColumnEntryParser parser = new ScriptColumnEntryParser(this.usage, clazz, actualMethod).configureEnvironment((MutableScriptEnvironment environment) -> {
					environment
					.addAll(MathScriptEnvironment.INSTANCE)
					.addAll(StatelessRandomScriptEnvironment.INSTANCE)
					.configure(MinecraftScriptEnvironment.create())
					.configure(GridScriptEnvironment.createWithSeed(ScriptedColumn.INFO.baseSeed(loadColumn)))
					.configure(ScriptedColumn.baseEnvironment(loadColumn))
					.addFunctionInvokes(load("segments", type(BlockSegmentList.class)), BlockSegmentList.class, "getBlockState", "setBlockState", "setBlockStates", "getTopOfSegment", "getBottomOfSegment")
					.addVariableInvokes(load("segments", type(BlockSegmentList.class)), BlockSegmentList.class, "minY", "maxY")
					;
					registry.setupExternalEnvironment(environment, new ExternalEnvironmentParams().withColumn(loadColumn));
				});
				parser.parseEntireInput().emitBytecode(actualMethod);
				actualMethod.endCode();

				MethodCompileContext getSource = clazz.newMethod(ACC_PUBLIC, "getSource", TypeInfos.STRING);
				return_(ldc(this.usage.findSource())).emitBytecode(getSource);
				getSource.endCode();

				MethodCompileContext getDebugName = clazz.newMethod(ACC_PUBLIC, "getDebugName", TypeInfos.STRING);
				return_(ldc(this.usage.debug_name, TypeInfos.STRING)).emitBytecode(getDebugName);
				getDebugName.endCode();

				try {
					this.script = (ScriptedLayer.Impl)(new ScriptClassLoader(registry.loader).defineClass(clazz).getDeclaredConstructors()[0].newInstance((Object[])(null)));
				}
				catch (Throwable throwable) {
					throw new ScriptParsingException(parser.fatalError().toString(), throwable, null);
				}
			}

			@Override
			public void emitSegments(ScriptedColumn column, BlockSegmentList blocks) {
				NumberArray.Direct.Manager manager = NumberArray.Direct.Manager.INSTANCES.get();
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