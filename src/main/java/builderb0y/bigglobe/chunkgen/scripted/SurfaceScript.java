package builderb0y.bigglobe.chunkgen.scripted;

import org.objectweb.asm.Type;

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
import builderb0y.bigglobe.scripting.wrappers.ExternalData;
import builderb0y.bigglobe.scripting.wrappers.ExternalImage;
import builderb0y.bigglobe.scripting.wrappers.ExternalImage.ColorScriptEnvironment;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.KeywordHandler;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.parsing.UserMethodDefiner.DerivativeMethodDefiner;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface SurfaceScript extends Script {

	public abstract void generateSurface(
		ScriptedColumn mainColumn,
		ScriptedColumn adjacentColumnX,
		ScriptedColumn adjacentColumnZ,
		ScriptedColumn adjacentColumnXZ,
		BlockSegmentList segments
	);

	@Wrapper
	public static class Holder extends ScriptHolder<SurfaceScript> implements SurfaceScript {

		public Holder(ScriptUsage usage) throws ScriptParsingException {
			super(usage);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.script = createScript(this.usage, registry);
		}

		public static SurfaceScript createScript(ScriptUsage usage, ColumnEntryRegistry registry) throws ScriptParsingException {
			ClassCompileContext clazz = new ClassCompileContext(
				ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC,
				ClassType.CLASS,
				Type.getInternalName(SurfaceScript.class) + '$' + (usage.debug_name != null ? usage.debug_name : "Generated") + '_' + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
				TypeInfos.OBJECT,
				new TypeInfo[] { type(SurfaceScript.class) }
			);
			clazz.addNoArgConstructor(ACC_PUBLIC);
			LazyVarInfo[] bridgeParams = {
				new LazyVarInfo("mainColumn", type(ScriptedColumn.class)),
				new LazyVarInfo("adjacentColumnX", type(ScriptedColumn.class)),
				new LazyVarInfo("adjacentColumnZ", type(ScriptedColumn.class)),
				new LazyVarInfo("adjacentColumnXZ", type(ScriptedColumn.class)),
				new LazyVarInfo("segments", type(BlockSegmentList.class))
			};
			LazyVarInfo[] actualParams = {
				new LazyVarInfo("mainColumn",       registry.columnContext.columnType()),
				new LazyVarInfo("adjacentColumnX",  registry.columnContext.columnType()),
				new LazyVarInfo("adjacentColumnZ",  registry.columnContext.columnType()),
				new LazyVarInfo("adjacentColumnXZ", registry.columnContext.columnType()),
				new LazyVarInfo("segments",         type(BlockSegmentList.class))
			};
			MethodCompileContext actualMethod = clazz.newMethod(ACC_PUBLIC, "generateSurface", TypeInfos.VOID, actualParams);
			MethodCompileContext bridgeMethod = clazz.newMethod(ACC_PUBLIC, "generateSurface", TypeInfos.VOID, bridgeParams);

			return_(
				invokeInstance(
					load("this", clazz.info),
					actualMethod.info,
					new DirectCastInsnTree(load("mainColumn",       type(ScriptedColumn.class)), registry.columnContext.columnType()),
					new DirectCastInsnTree(load("adjacentColumnX",  type(ScriptedColumn.class)), registry.columnContext.columnType()),
					new DirectCastInsnTree(load("adjacentColumnZ",  type(ScriptedColumn.class)), registry.columnContext.columnType()),
					new DirectCastInsnTree(load("adjacentColumnXZ", type(ScriptedColumn.class)), registry.columnContext.columnType()),
					load("segments", type(BlockSegmentList.class))
				)
			)
			.emitBytecode(bridgeMethod);
			bridgeMethod.endCode();

			LoadInsnTree loadMainColumn = load("mainColumn", registry.columnContext.columnType());
			ScriptColumnEntryParser parser = new ScriptColumnEntryParser(usage, clazz, actualMethod).configureEnvironment((MutableScriptEnvironment environment) -> {
				environment
				.addAll(MathScriptEnvironment.INSTANCE)
				.addAll(StatelessRandomScriptEnvironment.INSTANCE)
				.configure(MinecraftScriptEnvironment.create())
				.configure(GridScriptEnvironment.createWithSeed(registry.columnContext.loadSeed(null)))
				.configure(ScriptedColumn.baseEnvironment(loadMainColumn))
				.addFunctionInvokes(load("segments", type(BlockSegmentList.class)), BlockSegmentList.class, "getBlockState", "setBlockState", "setBlockStates", "getTopOfSegment", "getBottomOfSegment")
				.addVariableInvokes(load("segments", type(BlockSegmentList.class)), BlockSegmentList.class, "minY", "maxY")
				.addKeyword("dx", createDxDz(registry, false))
				.addKeyword("dz", createDxDz(registry, true))
				.addAll(ColorScriptEnvironment.ENVIRONMENT)
				.addAll(ExternalImage.ENVIRONMENT)
				.addAll(ExternalData.ENVIRONMENT)
				;
				registry.setupExternalEnvironment(environment, new ExternalEnvironmentParams().withColumn(loadMainColumn));
			});
			parser.parseEntireInput().emitBytecode(actualMethod);
			actualMethod.endCode();

			MethodCompileContext getSource = clazz.newMethod(ACC_PUBLIC, "getSource", TypeInfos.STRING);
			return_(ldc(clazz.newConstant(usage.findSource(), TypeInfos.STRING))).emitBytecode(getSource);
			getSource.endCode();

			MethodCompileContext getDebugName = clazz.newMethod(ACC_PUBLIC, "getDebugName", TypeInfos.STRING);
			return_(ldc(usage.debug_name, TypeInfos.STRING)).emitBytecode(getDebugName);
			getDebugName.endCode();

			try {
				return (SurfaceScript)(new ScriptClassLoader(registry.loader).defineClass(clazz).getDeclaredConstructors()[0].newInstance((Object[])(null)));
			}
			catch (Throwable throwable) {
				throw new ScriptParsingException(parser.fatalError().toString(), throwable, null);
			}
		}

		public static KeywordHandler.Named createDxDz(ColumnEntryRegistry registry, boolean z) {
			return new KeywordHandler.Named(
				"d" + (z ? 'z' : 'x') + "(value)",
				(ExpressionParser parser, String name) -> {
					parser.input.expectAfterWhitespace('(');
					parser.environment.user().push();

					InsnTree result = new DerivativeMethodDefiner(parser, "derivative_" + parser.clazz.memberUniquifier++).createDerivative(registry.columnContext.columnType(), z);
					parser.environment.user().pop();

					return result;
				}
			);
		}

		@Override
		public void generateSurface(
			ScriptedColumn mainColumn,
			ScriptedColumn adjacentColumnX,
			ScriptedColumn adjacentColumnZ,
			ScriptedColumn adjacentColumnXZ,
			BlockSegmentList segments
		) {
			NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
			int used = manager.used;
			try {
				this.script.generateSurface(mainColumn, adjacentColumnX, adjacentColumnZ, adjacentColumnXZ, segments);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
			}
			finally {
				manager.used = used;
			}
		}
	}

	public static class AnyNumericTypeExpressionParser extends ExpressionParser {

		public AnyNumericTypeExpressionParser(ExpressionParser from) {
			super(from);
		}

		@Override
		public InsnTree createReturn(InsnTree value) {
			return return_(value.cast(this, TypeInfos.widenToInt(value.getTypeInfo()), CastMode.IMPLICIT_THROW));
		}
	}
}