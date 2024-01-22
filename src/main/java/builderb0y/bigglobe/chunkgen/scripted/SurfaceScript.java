package builderb0y.bigglobe.chunkgen.scripted;

import java.util.Collections;
import java.util.stream.Stream;

import com.google.common.collect.ObjectArrays;
import org.objectweb.asm.Type;

import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ScriptColumnEntryParser;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.entries.ColumnEntry.ColumnEntryMemory;
import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.MinecraftScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.MethodDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.ConditionalNegateInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.KeywordHandler;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface SurfaceScript extends Script {

	public abstract void generateSurface(
		ScriptedColumn mainColumn,
		ScriptedColumn adjacentColumnX,
		ScriptedColumn adjacentColumnZ,
		ScriptedColumn adjacentColumnXZ,
		int originY,
		BlockSegmentList segments
	);

	public static class Holder extends ScriptHolder<SurfaceScript> implements SurfaceScript {

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage, ColumnEntryRegistry registry) throws ScriptParsingException {
			super(usage, createScript(usage, registry));
		}

		public static SurfaceScript createScript(ScriptUsage<GenericScriptTemplateUsage> usage, ColumnEntryRegistry registry) throws ScriptParsingException {
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
				new LazyVarInfo("originY", TypeInfos.INT),
				new LazyVarInfo("segments", type(BlockSegmentList.class))
			};
			LazyVarInfo[] actualParams = {
				new LazyVarInfo("mainColumn",       registry.columnContext.columnType()),
				new LazyVarInfo("adjacentColumnX",  registry.columnContext.columnType()),
				new LazyVarInfo("adjacentColumnZ",  registry.columnContext.columnType()),
				new LazyVarInfo("adjacentColumnXZ", registry.columnContext.columnType()),
				new LazyVarInfo("originY",          TypeInfos.INT),
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
					load("originY", TypeInfos.INT),
					load("segments", type(BlockSegmentList.class))
				)
			)
			.emitBytecode(bridgeMethod);
			bridgeMethod.endCode();

			MutableScriptEnvironment environment = (
				new MutableScriptEnvironment()
				.addAll(MathScriptEnvironment.INSTANCE)
				.addAll(StatelessRandomScriptEnvironment.INSTANCE)
				.addAll(MinecraftScriptEnvironment.create())
				.addVariableLoad("originY", TypeInfos.INT)
				.addVariableRenamedGetField(load("mainColumn", registry.columnContext.columnType()), "worldSeed", FieldInfo.getField(ScriptedColumn.class, "seed"))
				.addVariableRenamedInvoke(load("mainColumn", registry.columnContext.columnType()), "columnSeed", MethodInfo.findMethod(ScriptedColumn.class, "columnSeed", long.class))
				.addFunctionInvoke("columnSeed", load("mainColumn", registry.columnContext.columnType()), MethodInfo.findMethod(ScriptedColumn.class, "columnSeed", long.class, long.class))
				.addVariableGetField(load("mainColumn", registry.columnContext.columnType()), FieldInfo.getField(ScriptedColumn.class, "x"))
				.addVariableGetField(load("mainColumn", registry.columnContext.columnType()), FieldInfo.getField(ScriptedColumn.class, "z"))
				.addFunctionInvokes(load("segments", type(BlockSegmentList.class)), BlockSegmentList.class, "getBlockState", "setBlockState", "setBlockStates")
				.addKeyword("dx", createDxDz(registry, false))
				.addKeyword("dz", createDxDz(registry, true))
			);
			for (ColumnEntryMemory memory : registry.filteredMemories) {
				memory.getTyped(ColumnEntryMemory.ENTRY).setupExternalEnvironment(memory, registry.columnContext, environment, load("mainColumn", registry.columnContext.columnType()));
			}
			ScriptColumnEntryParser parser = new ScriptColumnEntryParser(usage, clazz, actualMethod).addEnvironment(environment);
			parser.parseEntireInput().emitBytecode(actualMethod);
			actualMethod.endCode();

			MethodCompileContext getSource = clazz.newMethod(ACC_PUBLIC, "getSource", TypeInfos.STRING);
			return_(ldc(usage.findSource())).emitBytecode(getSource);
			getSource.endCode();

			MethodCompileContext getDebugName = clazz.newMethod(ACC_PUBLIC, "getDebugName", TypeInfos.STRING);
			return_(ldc(usage.debug_name, TypeInfos.STRING)).emitBytecode(getDebugName);
			getDebugName.endCode();

			try {
				return (SurfaceScript)(registry.loader.defineClass(clazz).getDeclaredConstructors()[0].newInstance((Object[])(null)));
			}
			catch (Throwable throwable) {
				throw new ScriptParsingException(parser.fatalError().toString(), throwable, null);
			}
		}

		public static KeywordHandler createDxDz(ColumnEntryRegistry registry, boolean z) {
			return (ExpressionParser parser, String name) -> {
				parser.input.expectAfterWhitespace('(');
				parser.environment.user().push();
				ExpressionParser newParser = new AnyTypeExpressionParser(parser);
				newParser.environment.mutable().functions.put("return", Collections.singletonList((ExpressionParser parser1, String name1, InsnTree... arguments) -> {
					throw new ScriptParsingException("For technical reasons, you cannot return from inside a dx block", parser1.input);
				}));
				VariableCapturer capturer = new VariableCapturer(newParser);
				capturer.addCapturedParameters();

				InsnTree body = newParser.nextScript();
				body = body.cast(newParser, TypeInfos.widenToInt(body.getTypeInfo()), CastMode.IMPLICIT_THROW);

				parser.input.expectAfterWhitespace(')');
				parser.environment.user().pop();

				LazyVarInfo mainColumn       = new LazyVarInfo("mainColumn",       registry.columnContext.columnType());
				LazyVarInfo adjacentColumnX  = new LazyVarInfo("adjacentColumnX",  registry.columnContext.columnType());
				LazyVarInfo adjacentColumnZ  = new LazyVarInfo("adjacentColumnZ",  registry.columnContext.columnType());
				LazyVarInfo adjacentColumnXZ = new LazyVarInfo("adjacentColumnXZ", registry.columnContext.columnType());
				LazyVarInfo originY          = new LazyVarInfo("originY",          TypeInfos.INT);
				LazyVarInfo segments         = new LazyVarInfo("segments",         type(BlockSegmentList.class));

				MethodDeclarationInsnTree declaration = new MethodDeclarationInsnTree(
					parser.method.info.access(),
					"derivative_" + parser.clazz.memberUniquifier++,
					body.getTypeInfo(),
					Stream.concat(
						Stream.of(
							mainColumn,
							adjacentColumnX,
							adjacentColumnZ,
							adjacentColumnXZ,
							originY,
							segments
						),
						capturer.streamImplicitParameters()
					)
					.toArray(LazyVarInfo.ARRAY_FACTORY),
					return_(body)
				);
				MethodInfo derivativeMethod = declaration.createMethodInfo(parser.clazz.info);

				InsnTree[] normalArgs = ObjectArrays.concat(
					new InsnTree[] {
						load(mainColumn),
						load(adjacentColumnX),
						load(adjacentColumnZ),
						load(adjacentColumnXZ),
						load(originY),
						load(segments)
					},
					capturer.implicitParameters.toArray(new LoadInsnTree[capturer.implicitParameters.size()]),
					InsnTree.class
				);
				InsnTree[] adjacentArgs = ObjectArrays.concat(
					new InsnTree[] {
						load(z ? adjacentColumnZ  : adjacentColumnX ),
						load(z ? adjacentColumnXZ : mainColumn      ),
						load(z ? mainColumn       : adjacentColumnXZ),
						load(z ? adjacentColumnX  : adjacentColumnZ ),
						load(originY),
						load(segments)
					},
					capturer.implicitParameters.toArray(new LoadInsnTree[capturer.implicitParameters.size()]),
					InsnTree.class
				);

				InsnTree normalInvoker, adjacentInvoker;
				if (derivativeMethod.isStatic()) {
					normalInvoker = invokeStatic(derivativeMethod, normalArgs);
					adjacentInvoker = invokeStatic(derivativeMethod, adjacentArgs);
				}
				else {
					normalInvoker = invokeInstance(load("this", parser.clazz.info), derivativeMethod, normalArgs);
					adjacentInvoker = invokeInstance(load("this", parser.clazz.info), derivativeMethod, adjacentArgs);
				}

				return seq(
					declaration,
					ConditionalNegateInsnTree.create(
						newParser,
						sub(newParser, adjacentInvoker, normalInvoker),
						lt(
							parser,
							getField(load(z ? adjacentColumnZ : adjacentColumnX), FieldInfo.getField(ScriptedColumn.class, z ? "z" : "x")),
							getField(load(mainColumn), FieldInfo.getField(ScriptedColumn.class, z ? "z" : "x"))
						)
					)
				);
			};
		}

		@Override
		public void generateSurface(
			ScriptedColumn mainColumn,
			ScriptedColumn adjacentColumnX,
			ScriptedColumn adjacentColumnZ,
			ScriptedColumn adjacentColumnXZ,
			int y,
			BlockSegmentList segments
		) {
			try {
				this.script.generateSurface(mainColumn, adjacentColumnX, adjacentColumnZ, adjacentColumnXZ, y, segments);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
			}
		}
	}

	public static class AnyTypeExpressionParser extends ExpressionParser {

		public AnyTypeExpressionParser(ExpressionParser from) {
			super(from);
		}

		@Override
		public InsnTree createReturn(InsnTree value) {
			return return_(value.cast(this, TypeInfos.widenToInt(value.getTypeInfo()), CastMode.IMPLICIT_THROW));
		}
	}
}