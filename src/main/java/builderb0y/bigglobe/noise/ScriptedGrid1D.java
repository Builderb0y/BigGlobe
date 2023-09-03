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

public class ScriptedGrid1D extends ScriptedGrid<Grid1D> implements Grid1D {

	public static final GridTypeInfo GRID_1D_TYPE_INFO = new GridTypeInfo(Grid1D.class, Parser.class, 1);

	public final transient Grid1D delegate;

	public ScriptedGrid1D(ScriptUsage<ScriptedGridTemplateUsage<Grid1D>> script, Map<String, Grid1D> inputs, double min, double max) throws ScriptParsingException {
		super(script, inputs, min, max);
		LinkedHashMap<String, Input> processedInputs = processInputs(inputs, GRID_1D_TYPE_INFO);
		Parser parser = new Parser(script, processedInputs);
		parser
		.addEnvironment(new Environment(processedInputs, GRID_1D_TYPE_INFO))
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
	public double getValue(long seed, int x) {
		return this.delegate.getValue(seed, x);
	}

	@Override
	public void getBulkX(long seed, int startX, double[] samples, int sampleCount) {
		this.delegate.getBulkX(seed, startX, samples, sampleCount);
	}

	public static class Parser extends ScriptedGrid.Parser<Grid1D> {

		@SuppressWarnings("MultipleVariablesInDeclaration")
		public static final MethodInfo
			GET_BULK_X = MethodInfo.getMethod(Grid1D.class, "getBulkX"),
			GET_BULK[] = { GET_BULK_X };

		public Parser(ScriptUsage<ScriptedGridTemplateUsage<Grid1D>> usage, LinkedHashMap<String, Input> inputs) {
			super(usage, inputs, GRID_1D_TYPE_INFO);
		}

		@Override
		public void addGetBulkOne(int methodDimension) {
			MethodInfo methodInfo = GET_BULK[methodDimension];
			this.clazz.newMethod(methodInfo.changeOwner(this.clazz.info)).scopes.withScope((MethodCompileContext getBulkX) -> {
				Input input = this.gridInputs.values().iterator().next();
				VarInfo thisVar     = getBulkX.addThis();
				VarInfo seed        = getBulkX.newParameter("seed", TypeInfos.LONG);
				VarInfo startX      = getBulkX.newParameter("startX", TypeInfos.INT);
				VarInfo samples     = getBulkX.newParameter("samples", DOUBLE_ARRAY);
				VarInfo sampleCount = getBulkX.newParameter("sampleCount", TypeInfos.INT);
				VarInfo column      = getBulkX.newVariable ("column", type(WorldColumn.class));

				//if (sampleCount <= 0) return;
				ifThen(
					le(
						this,
						load(sampleCount),
						ldc(0)
					),
					return_(noop)
				)
				.emitBytecode(getBulkX);
				//get column.
				store(column, GET_SECRET_COLUMN).emitBytecode(getBulkX);
				//fill samples with input.
					invokeInstance(
					getField(
						load(thisVar),
						input.fieldInfo(getBulkX)
					),
					methodInfo,
					load(seed),
					load(startX),
					load(samples),
					load(sampleCount)
				)
				.emitBytecode(getBulkX);
				getBulkX.node.visitLabel(label());
				//replace samples with evaluation results.
				getBulkX.scopes.withScope((MethodCompileContext getBulkX_) -> {
					VarInfo index = getBulkX_.newVariable("index", TypeInfos.INT);
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
									ACC_PUBLIC | INVOKESTATIC,
									getBulkX_.clazz.info,
									"evaluate",
									TypeInfos.DOUBLE,
									types(WorldColumn.class, 'I', 'D')
								),
								load(column),
								add(this, load(startX), load(index)),
								arrayLoad(load(samples), load(index))
							)
						)
					)
					.emitBytecode(getBulkX_);
				});
				//return.
				return_(noop).emitBytecode(getBulkX);
			});
		}

		@Override
		public void addGetBulkMany(int methodDimension) {
			MethodInfo methodInfo = GET_BULK[methodDimension];
			this.clazz.newMethod(methodInfo.changeOwner(this.clazz.info)).scopes.withScope((MethodCompileContext getBulkX) -> {
				VarInfo thisVar     = getBulkX.addThis();
				VarInfo seed        = getBulkX.newParameter("seed", TypeInfos.LONG);
				VarInfo startX      = getBulkX.newParameter("startX", TypeInfos.INT);
				VarInfo samples     = getBulkX.newParameter("samples", DOUBLE_ARRAY);
				VarInfo sampleCount = getBulkX.newParameter("sampleCount", TypeInfos.INT);
				VarInfo column      = getBulkX.newVariable("column", type(WorldColumn.class));

				//declare scratch arrays.
				VarInfo[] scratches = new VarInfo[this.gridInputs.size()];
				for (Input input : this.gridInputs.values()) {
					scratches[input.index] = getBulkX.newVariable(input.name, DOUBLE_ARRAY);
				}
				//if (sampleCount <= 0) return;
				ifThen(
					le(
						this,
						load(sampleCount),
						ldc(0)
					),
					return_(noop)
				)
				.emitBytecode(getBulkX);
				//get column.
				store(column, GET_SECRET_COLUMN).emitBytecode(getBulkX);
				//allocate scratch arrays.
				for (Input input : this.gridInputs.values()) {
					store(
						scratches[input.index],
						invokeStatic(
							GET_SCRATCH_ARRAY,
							load(sampleCount)
						)
					)
					.emitBytecode(getBulkX);
					getBulkX.node.visitLabel(label());
				}
				//fill scratch arrays.
				for (Input input : this.gridInputs.values()) {
					invokeInstance(
						getField(load(thisVar), input.fieldInfo(getBulkX)),
						methodInfo,
						load(seed),
						load(startX),
						load(scratches[input.index]),
						load(sampleCount)
					)
					.emitBytecode(getBulkX);
					getBulkX.node.visitLabel(label());
				}
				//fill samples.
				getBulkX.scopes.withScope((MethodCompileContext getBulkX_) -> {
					VarInfo index = getBulkX_.newVariable("index", TypeInfos.INT);
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
									getBulkX_.clazz.info,
									"evaluate",
									TypeInfos.DOUBLE,
									types(WorldColumn.class, 'I', 'D', this.gridInputs.size())
								),
								Stream.concat(
									Stream.of(
										load(column),
										add(this, load(startX), load(index))
									),
									this.gridInputs.values().stream().map(input -> (
										arrayLoad(load(scratches[input.index]), load(index))
									))
								)
								.toArray(InsnTree.ARRAY_FACTORY)
							)
						)
					)
					.emitBytecode(getBulkX_);
				});
				//reclaim scratch arrays.
				for (Input input : this.gridInputs.values()) {
					invokeStatic(
						RECLAIM_SCRATCH_ARRAY,
						load(scratches[input.index])
					)
					.emitBytecode(getBulkX);
					getBulkX.node.visitLabel(label());
				}
				//return.
				return_(noop).emitBytecode(getBulkX);
			});
		}
	}
}