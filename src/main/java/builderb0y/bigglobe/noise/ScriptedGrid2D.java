package builderb0y.bigglobe.noise;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.noise.ScriptedGridTemplate.ScriptedGridTemplateUsage;
import builderb0y.bigglobe.scripting.ColumnScriptEnvironmentBuilder;
import builderb0y.bigglobe.scripting.StatelessRandomScriptEnvironment;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ScriptedGrid2D extends ScriptedGrid<Grid2D> implements Grid2D {

	public static final GridTypeInfo GRID_2D_TYPE_INFO = new GridTypeInfo(Grid2D.class, Parser.class, 2);

	public final transient Grid2D delegate;

	public ScriptedGrid2D(ScriptUsage<ScriptedGridTemplateUsage<Grid2D>> script, Map<String, Grid2D> inputs, double min, double max) throws ScriptParsingException {
		super(script, inputs, min, max);
		LinkedHashMap<String, Input> processedInputs = processInputs(inputs, GRID_2D_TYPE_INFO);
		Parser parser = new Parser(script, processedInputs);
		parser
		.addEnvironment(new Environment(processedInputs, GRID_2D_TYPE_INFO))
		.addEnvironment(MathScriptEnvironment.INSTANCE)
		.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
		.addEnvironment(
			ColumnScriptEnvironmentBuilder.createFixedXZVariableY(
				ColumnValue.REGISTRY,
				load("column", 0, type(WorldColumn.class)),
				null
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
	public double getValue(long seed, int x, int y) {
		return this.delegate.getValue(seed, x, y);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, double[] samples, int sampleCount) {
		this.delegate.getBulkX(seed, startX, y, samples, sampleCount);
	}

	@Override
	public void getBulkY(long seed, int x, int startY, double[] samples, int sampleCount) {
		this.delegate.getBulkY(seed, x, startY, samples, sampleCount);
	}

	public static class Parser extends ScriptedGrid.Parser<Grid2D> {

		@SuppressWarnings("MultipleVariablesInDeclaration")
		public static final MethodInfo
			GET_BULK_X = MethodInfo.getMethod(Grid2D.class, "getBulkX"),
			GET_BULK_Y = MethodInfo.getMethod(Grid2D.class, "getBulkY"),
			GET_BULK[] = { GET_BULK_X, GET_BULK_Y };

		public Parser(ScriptUsage<ScriptedGridTemplateUsage<Grid2D>> usage, LinkedHashMap<String, Input> inputs) {
			super(usage, inputs, GRID_2D_TYPE_INFO);
		}

		@Override
		public void addGetBulkOne(int methodDimension) {
			MethodInfo methodInfo = GET_BULK[methodDimension];
			this.clazz.newMethod(methodInfo.changeOwner(this.clazz.info)).scopes.withScope((MethodCompileContext getBulk) -> {
				Input firstInput = this.gridInputs.values().iterator().next();
				VarInfo thisVar     = getBulk.addThis();
				VarInfo seed        = getBulk.newParameter("seed", TypeInfos.LONG);
				VarInfo x           = getBulk.newParameter("x", TypeInfos.INT);
				VarInfo y           = getBulk.newParameter("y", TypeInfos.INT);
				VarInfo samples     = getBulk.newParameter("samples", type(double[].class));
				VarInfo sampleCount = getBulk.newParameter("sampleCount", TypeInfos.INT);
				VarInfo column      = getBulk.newVariable("column", type(WorldColumn.class));

				//if (sampleCount <= 0) return;
				ifThen(
					le(
						this,
						load(sampleCount),
						ldc(0)
					),
					return_(noop)
				)
				.emitBytecode(getBulk);
				//get column.
				store(column, GET_SECRET_COLUMN).emitBytecode(getBulk);
				//fill samples with firstInput.
					invokeInstance(
					getField(
						load(thisVar),
						firstInput.fieldInfo(getBulk)
					),
					methodInfo,
					load(seed),
					load(x),
					load(y),
					load(samples),
					load(sampleCount)
				)
				.emitBytecode(getBulk);
				getBulk.node.visitLabel(label());
				//replace samples with evaluation results.
				getBulk.scopes.withScope((MethodCompileContext getBulk_) -> {
					VarInfo index = getBulk_.newVariable("index", TypeInfos.INT);
					for_(
						null,
						store(index, ldc(0)),
						lt(
							this,
							load(index),
							load(sampleCount)
						),
						inc(index, 1),
						arrayStore(
							load(samples),
							load(index),
							invokeStatic(
								new MethodInfo(
									ACC_PUBLIC | ACC_STATIC,
									getBulk_.clazz.info,
									"evaluate",
									TypeInfos.DOUBLE,
									types(WorldColumn.class, 'I', 'I', 'D', this.gridInputs.size())
								),
								load(column),
								maybeAdd(this, x, index, 0, methodDimension),
								maybeAdd(this, y, index, 1, methodDimension),
								arrayLoad(load(samples), load(index))
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
				VarInfo samples     = getBulk.newParameter("samples", type(double[].class));
				VarInfo sampleCount = getBulk.newParameter("sampleCount", TypeInfos.INT);
				VarInfo column      = getBulk.newVariable("column", type(WorldColumn.class));

				//declare scratch arrays.
				VarInfo[] scratches = new VarInfo[this.gridInputs.size()];
				for (Input input : this.gridInputs.values()) {
					scratches[input.index] = getBulk.newVariable(input.name, type(double[].class));
				}
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
						invokeStatic(
							GET_SCRATCH_ARRAY,
							load(sampleCount)
						)
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
						load(scratches[input.index]),
						load(sampleCount)
					)
					.emitBytecode(getBulk);
					getBulk.node.visitLabel(label());
				}
				//fill samples.
				getBulk.scopes.withScope((MethodCompileContext getBulk_) -> {
					VarInfo index = getBulk_.newVariable("index", TypeInfos.INT);
					for_(
						null,
						store(index, ldc(0)),
						lt(this, load(index), load(sampleCount)),
						inc(index, 1),
						arrayStore(
							load(samples),
							load(index),
							invokeStatic(
								new MethodInfo(
									ACC_PUBLIC | ACC_STATIC,
									getBulk_.clazz.info,
									"evaluate",
									TypeInfos.DOUBLE,
									types(WorldColumn.class, 'I', 'I', 'D', this.gridInputs.size())
								),
								Stream.concat(
									Stream.of(
										load(column),
										maybeAdd(this, x, index, 0, methodDimension),
										maybeAdd(this, y, index, 1, methodDimension)
									),
									this.gridInputs.values().stream().map(input -> (
										arrayLoad(load(scratches[input.index]), load(index))
									))
								)
								.toArray(InsnTree[]::new)
							)
						)
					)
					.emitBytecode(getBulk_);
				});
				//reclaim scratch arrays.
				for (Input input : this.gridInputs.values()) {
					invokeStatic(
						RECLAIM_SCRATCH_ARRAY,
						load(scratches[input.index])
					)
					.emitBytecode(getBulk);
					getBulk.node.visitLabel(label());
				}
				//return.
				return_(noop).emitBytecode(getBulk);
			});
		}
	}
}