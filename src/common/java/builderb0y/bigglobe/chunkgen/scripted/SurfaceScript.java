package builderb0y.bigglobe.chunkgen.scripted;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.Type;

import net.minecraft.core.Holder;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted.ScriptColumnEntryParser;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SetBasedMutableDependencyView;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.scripting.ScriptCatcher;
import builderb0y.bigglobe.scripting.environments.ColorScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.GridScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.KeywordHandler;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptClassLoader;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.UserMethodDefiner.DerivativeMethodDefiner;
import builderb0y.scripting.parsing.input.ScriptUsage;
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
	public static class Catcher extends ScriptCatcher<SurfaceScript> implements SurfaceScript, SetBasedMutableDependencyView {

		public final Set<Holder<? extends DependencyView>> dependencies = new HashSet<>();

		public Catcher(ScriptUsage usage) throws ScriptParsingException {
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
				Type.getInternalName(SurfaceScript.class) + '$' + (this.usage.debug_name != null ? this.usage.debug_name : "Generated") + '_' + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
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
				new LazyVarInfo("mainColumn", registry.columnCompileContext.columnTypeInfo()),
				new LazyVarInfo("adjacentColumnX", registry.columnCompileContext.columnTypeInfo()),
				new LazyVarInfo("adjacentColumnZ", registry.columnCompileContext.columnTypeInfo()),
				new LazyVarInfo("adjacentColumnXZ", registry.columnCompileContext.columnTypeInfo()),
				new LazyVarInfo("segments", type(BlockSegmentList.class))
			};
			MethodCompileContext actualMethod = clazz.newMethod(ACC_PUBLIC, "generateSurface", TypeInfos.VOID, actualParams);
			MethodCompileContext bridgeMethod = clazz.newMethod(ACC_PUBLIC, "generateSurface", TypeInfos.VOID, bridgeParams);

			return_(
				invokeInstance(
					load("this", clazz.info),
					actualMethod.info,
					new DirectCastInsnTree(load("mainColumn", type(ScriptedColumn.class)), registry.columnCompileContext.columnTypeInfo(), false),
					new DirectCastInsnTree(load("adjacentColumnX", type(ScriptedColumn.class)), registry.columnCompileContext.columnTypeInfo(), false),
					new DirectCastInsnTree(load("adjacentColumnZ", type(ScriptedColumn.class)), registry.columnCompileContext.columnTypeInfo(), false),
					new DirectCastInsnTree(load("adjacentColumnXZ", type(ScriptedColumn.class)), registry.columnCompileContext.columnTypeInfo(), false),
					load("segments", type(BlockSegmentList.class))
				)
			)
			.emitBytecode(bridgeMethod);
			bridgeMethod.endCode();

			LoadInsnTree loadMainColumn = load("mainColumn", registry.columnCompileContext.columnTypeInfo());
			ScriptColumnEntryParser parser = new ScriptColumnEntryParser(this.usage, clazz, actualMethod, registry.parserFlags());
			parser
			.environment
			.mutable()
			.addAll(MathScriptEnvironment.INSTANCE)
			.addAll(StatelessRandomScriptEnvironment.INSTANCE)
			.configure(GridScriptEnvironment.createWithSeed(registry.columnCompileContext.loadSeed(null)))
			.addFunctionInvokes(load("segments", type(BlockSegmentList.class)), BlockSegmentList.class, "getBlockState", "setBlockState", "setBlockStates", "getTopOfSegment", "getBottomOfSegment")
			.addVariableInvokes(load("segments", type(BlockSegmentList.class)), BlockSegmentList.class, "minY", "maxY")
			.addKeyword(createDxDz(registry, false))
			.addKeyword(createDxDz(registry, true))
			.addAll(ColorScriptEnvironment.ENVIRONMENT)
			;
			registry.setupEnvironment(
				parser,
				new ExternalEnvironmentParams()
				.withColumn(loadMainColumn)
				.trackDependencies(this)
			);
			parser.parseEntireInput().emitBytecode(actualMethod);
			actualMethod.endCode();

			MethodCompileContext getSource = clazz.newMethod(ACC_PUBLIC, "getSource", TypeInfos.STRING);
			return_(ldc(clazz.newConstant(this.usage.getSource(), TypeInfos.STRING))).emitBytecode(getSource);
			getSource.endCode();

			MethodCompileContext getDebugName = clazz.newMethod(ACC_PUBLIC, "getDebugName", TypeInfos.STRING);
			return_(ldc(this.usage.debug_name, TypeInfos.STRING)).emitBytecode(getDebugName);
			getDebugName.endCode();

			try {
				this.script = (SurfaceScript)(new ScriptClassLoader(registry.loader).defineClass(clazz, ExpressionParser.CLASS_DUMP_DIRECTORY, this.usage.getSource()).getDeclaredConstructors()[0].newInstance((Object[])(null)));
			}
			catch (Throwable throwable) {
				throw new ScriptParsingException(parser.fatalError().toString(), throwable, null);
			}
		}

		public static KeywordHandler.Named createDxDz(ColumnEntryRegistry registry, boolean z) {
			return new KeywordHandler.Named(
				z ? "dz" : "dx",
				"d" + (z ? 'z' : 'x') + "(value)",
				null,
				(ExpressionParser parser, String name) -> {
					parser.input.expectAfterWhitespace('(');
					parser.environment.user().push();

					InsnTree result = new DerivativeMethodDefiner(parser, "derivative_" + parser.clazz.memberUniquifier++).createDerivative(registry.columnCompileContext.columnTypeInfo(), z);
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
			return return_(value.cast(this, TypeInfos.widenToInt(value.getTypeInfo()), CastMode.IMPLICIT_THROW, false));
		}
	}
}