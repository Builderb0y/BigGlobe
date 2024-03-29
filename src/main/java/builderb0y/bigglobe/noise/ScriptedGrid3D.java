package builderb0y.bigglobe.noise;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.objectweb.asm.Opcodes;

import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.noise.ScriptedGridTemplate.ScriptedGridTemplateUsage;
import builderb0y.bigglobe.scripting.environments.ColumnScriptEnvironmentBuilder;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.ScopeContext;
import builderb0y.scripting.bytecode.ScopeContext.LoopName;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.OpcodeCastInsnTree;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ScriptedGrid3D extends ScriptedGrid<Grid3D> implements Grid3D {

	public static final GridTypeInfo GRID_3D_TYPE_INFO = new GridTypeInfo(Grid3D.class, Parser.class, 3);

	public final transient Grid3D delegate;

	public ScriptedGrid3D(ScriptUsage<ScriptedGridTemplateUsage<Grid3D>> script, Map<String, Grid3D> inputs, double min, double max) throws ScriptParsingException {
		super(script, inputs, min, max);
		LinkedHashMap<String, Input> processedInputs = processInputs(inputs, GRID_3D_TYPE_INFO);
		Parser parser = new Parser(script, processedInputs);
		parser
		.addEnvironment(new Environment(processedInputs, GRID_3D_TYPE_INFO))
		.addEnvironment(MathScriptEnvironment.INSTANCE)
		.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
		.addEnvironment(
			ColumnScriptEnvironmentBuilder.createFixedXZVariableY(
				ColumnValue.REGISTRY,
				load("column", 0, type(WorldColumn.class)),
				new OpcodeCastInsnTree(
					load("y", 2, TypeInfos.INT),
					Opcodes.I2D,
					TypeInfos.DOUBLE
				)
			)
			.build()
		);
		this.delegate = parser.parse();
	}

	@Override
	public Grid getDelegate() {
		return this.delegate;
	}

	@Override
	public double getValue(long seed, int x, int y, int z) {
		return this.delegate.getValue(seed, x, y, z);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, int z, NumberArray samples) {
		//workaround for the fact that I *really* don't want to deal
		//with generating bytecode for try-with-resources at runtime.
		NumberArray.Direct.Manager manager = NumberArray.Direct.Manager.INSTANCES.get();
		int used = manager.used;
		try {
			this.delegate.getBulkX(seed, startX, y, z, samples);
		}
		catch (Throwable throwable) {
			this.onError(throwable);
		}
		finally {
			manager.used = used;
		}
	}

	@Override
	public void getBulkY(long seed, int x, int startY, int z, NumberArray samples) {
		//workaround for the fact that I *really* don't want to deal
		//with generating bytecode for try-with-resources at runtime.
		NumberArray.Direct.Manager manager = NumberArray.Direct.Manager.INSTANCES.get();
		int used = manager.used;
		try {
			this.delegate.getBulkY(seed, x, startY, z, samples);
		}
		catch (Throwable throwable) {
			this.onError(throwable);
		}
		finally {
			manager.used = used;
		}
	}

	@Override
	public void getBulkZ(long seed, int x, int y, int startZ, NumberArray samples) {
		//workaround for the fact that I *really* don't want to deal
		//with generating bytecode for try-with-resources at runtime.
		NumberArray.Direct.Manager manager = NumberArray.Direct.Manager.INSTANCES.get();
		int used = manager.used;
		try {
			this.delegate.getBulkZ(seed, x, y, startZ, samples);
		}
		catch (Throwable throwable) {
			this.onError(throwable);
		}
		finally {
			manager.used = used;
		}
	}

	public static class Parser extends ScriptedGrid.Parser<Grid3D> {

		@SuppressWarnings("MultipleVariablesInDeclaration")
		public static final MethodInfo
			GET_BULK_X = MethodInfo.getMethod(Grid3D.class, "getBulkX"),
			GET_BULK_Y = MethodInfo.getMethod(Grid3D.class, "getBulkY"),
			GET_BULK_Z = MethodInfo.getMethod(Grid3D.class, "getBulkZ"),
			GET_BULK[] = { GET_BULK_X, GET_BULK_Y, GET_BULK_Z };

		public Parser(ScriptUsage<ScriptedGridTemplateUsage<Grid3D>> usage, LinkedHashMap<String, Input> inputs) {
			super(usage, inputs, GRID_3D_TYPE_INFO);
		}

		@Override
		public void addGetBulkOne(int methodDimension) {
			MethodInfo methodInfo = GET_BULK[methodDimension];
			this.clazz.newMethod(methodInfo.changeOwner(this.clazz.info)).scopes.withScope((MethodCompileContext getBulk) -> {
				Input input = this.gridInputs.values().iterator().next();
				VarInfo thisVar     = getBulk.addThis();
				VarInfo seed        = getBulk.newParameter("seed", TypeInfos.LONG);
				VarInfo x           = getBulk.newParameter("x", TypeInfos.INT);
				VarInfo y           = getBulk.newParameter("y", TypeInfos.INT);
				VarInfo z           = getBulk.newParameter("z", TypeInfos.INT);
				VarInfo samples     = getBulk.newParameter("samples", NUMBER_ARRAY);
				VarInfo sampleCount = getBulk.newVariable ("sampleCount", TypeInfos.INT);
				VarInfo column      = getBulk.newVariable ("column", type(WorldColumn.class));

				//sampleCount = samples.length();
				store(sampleCount, numberArrayLength(load(samples))).emitBytecode(getBulk);
				//if (sampleCount <= 0) return;
				ifThen(
					le(this, load(sampleCount), ldc(0)),
					return_(noop)
				)
				.emitBytecode(getBulk);
				//get column.
				store(column, GET_SECRET_COLUMN).emitBytecode(getBulk);
				//fill samples with input.
				invokeInstance(
					getField(
						load(thisVar),
						input.fieldInfo(getBulk)
					),
					methodInfo,
					load(seed),
					load(x),
					load(y),
					load(z),
					load(samples)
				)
				.emitBytecode(getBulk);
				getBulk.node.visitLabel(label());
				//replace samples with evaluation results.
				getBulk.scopes.withScope((MethodCompileContext getBulk_) -> {
					VarInfo index = getBulk_.newVariable("index", TypeInfos.INT);
					for_(
						new LoopName(null),
						store(index, ldc(0)),
						lt(this, load(index), load(sampleCount)),
						inc(index, 1),
						numberArrayStore(
							load(samples),
							load(index),
							invokeStatic(
								new MethodInfo(
									ACC_PUBLIC | ACC_STATIC,
									getBulk_.clazz.info,
									"evaluate",
									TypeInfos.DOUBLE,
									types(WorldColumn.class, 'I', 'I', 'I', 'D', this.gridInputs.size())
								),
								load(column),
								maybeAdd(this, x, index, 0, methodDimension),
								maybeAdd(this, y, index, 1, methodDimension),
								maybeAdd(this, z, index, 2, methodDimension),
								numberArrayLoad(load(samples), load(index))
							)
						)
					)
					.emitBytecode(getBulk_);
				});
				//return.
				return_(noop).emitBytecode(getBulk);
			});
		}

		@Override
		public void addGetBulkMany(int methodDimension) {
			MethodInfo methodInfo = GET_BULK[methodDimension];
			this.clazz.newMethod(methodInfo.changeOwner(this.clazz.info)).scopes.withScope((MethodCompileContext getBulk) -> {
				VarInfo thisVar     = getBulk.addThis();
				VarInfo seed        = getBulk.newParameter("seed", TypeInfos.LONG);
				VarInfo x           = getBulk.newParameter("x", TypeInfos.INT);
				VarInfo y           = getBulk.newParameter("y", TypeInfos.INT);
				VarInfo z           = getBulk.newParameter("z", TypeInfos.INT);
				VarInfo samples     = getBulk.newParameter("samples", NUMBER_ARRAY);
				VarInfo sampleCount = getBulk.newVariable ("sampleCount", TypeInfos.INT);
				VarInfo column      = getBulk.newVariable ("column", type(WorldColumn.class));

				//declare scratch arrays.
				VarInfo[] scratches = new VarInfo[this.gridInputs.size()];
				for (Input input : this.gridInputs.values()) {
					scratches[input.index] = getBulk.newVariable(input.name, NUMBER_ARRAY);
				}
				//sampleCount = samples.length();
				store(sampleCount, numberArrayLength(load(samples))).emitBytecode(getBulk);
				//if (sampleCount <= 0) return;
				ifThen(
					le(this, load(sampleCount), ldc(0)),
					return_(noop)
				)
				.emitBytecode(getBulk);
				//get column.
				store(column, GET_SECRET_COLUMN).emitBytecode(getBulk);
				//allocate scratch arrays.
				for (Input input : this.gridInputs.values()) {
					store(
						scratches[input.index],
						newNumberArray(load(sampleCount))
					)
					.emitBytecode(getBulk);
					getBulk.node.visitLabel(label());
				}
				//fill scratch arrays.
				for (Input input : this.gridInputs.values()) {
					invokeInstance(
						getField(
							load(thisVar),
							input.fieldInfo(getBulk)
						),
						methodInfo,
						load(seed),
						load(x),
						load(y),
						load(z),
						load(scratches[input.index])
					)
					.emitBytecode(getBulk);
					getBulk.node.visitLabel(label());
				}
				//fill samples.
				getBulk.scopes.withScope((MethodCompileContext getBulk_) -> {
					VarInfo index = getBulk_.newVariable("index", TypeInfos.INT);
					for_(
						new LoopName(null),
						store(index, ldc(0)),
						lt(this, load(index), load(sampleCount)),
						inc(index, 1),
						numberArrayStore(
							load(samples),
							load(index),
							invokeStatic(
								new MethodInfo(
									ACC_PUBLIC | ACC_STATIC,
									getBulk_.clazz.info,
									"evaluate",
									TypeInfos.DOUBLE,
									types(WorldColumn.class, 'I', 'I', 'I', 'D', this.gridInputs.size())
								),
								Stream.concat(
									Stream.of(
										load(column),
										maybeAdd(this, x, index, 0, methodDimension),
										maybeAdd(this, y, index, 1, methodDimension),
										maybeAdd(this, z, index, 2, methodDimension)
									),
									this.gridInputs.values().stream().map(input -> (
										numberArrayLoad(
											load(scratches[input.index]),
											load(index)
										)
									))
								)
								.toArray(InsnTree.ARRAY_FACTORY)
							)
						)
					)
					.emitBytecode(getBulk_);
				});
				//return.
				return_(noop).emitBytecode(getBulk);
			});
		}
	}
}