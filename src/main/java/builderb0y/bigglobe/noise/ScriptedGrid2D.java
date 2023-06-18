package builderb0y.bigglobe.noise;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.scripting.ColumnScriptEnvironment;
import builderb0y.bigglobe.scripting.StatelessRandomScriptEnvironment;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.VarInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ScriptedGrid2D extends ScriptedGrid<Grid2D> implements Grid2D {

	public static final GridTypeInfo GRID_2D_TYPE_INFO = new GridTypeInfo(Grid2D.class, Parser.class, 2);

	public final transient Grid2D delegate;

	public ScriptedGrid2D(String script, Map<String, Grid2D> inputs, double min, double max) throws ScriptParsingException {
		super(inputs, min, max);
		LinkedHashMap<String, Input> processedInputs = processInputs(inputs, GRID_2D_TYPE_INFO);
		Parser parser = new Parser(script, processedInputs);
		parser
		.addEnvironment(new Environment(processedInputs, GRID_2D_TYPE_INFO))
		.addEnvironment(MathScriptEnvironment.INSTANCE)
		.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
		.addEnvironment(
			ColumnScriptEnvironment.createFixedXZVariableY(
				ColumnValue.REGISTRY,
				load("column", 0, type(WorldColumn.class)),
				null
			)
			.mutable
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

	public static class Parser extends ScriptedGrid.Parser {

		public Parser(String input, LinkedHashMap<String, Input> inputs) {
			super(input, inputs, GRID_2D_TYPE_INFO);
		}

		@Override
		public Grid2D parse() throws ScriptParsingException {
			return (Grid2D)(super.parse());
		}

		@Override
		public void addGetBulkOne(int methodDimension) {
			String methodName = methodDimension == 0 ? "getBulkX" : "getBulkY";
			this.clazz.newMethod(
				ACC_PUBLIC,
				methodName,
				TypeInfos.VOID, //returnType
				TypeInfos.LONG, //seed
				TypeInfos.INT, //x
				TypeInfos.INT, //y
				type(double[].class), //samples
				TypeInfos.INT //sampleCount
			)
			.scopes.withScope((MethodCompileContext getBulk) -> {
				Input firstInput = this.inputs.values().iterator().next();
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
					method(
						ACC_PUBLIC | ACC_INTERFACE,
						GRID_2D_TYPE_INFO.type,
						methodName,
						TypeInfos.VOID, //returnType
						TypeInfos.LONG, //seed
						TypeInfos.INT, //x
						TypeInfos.INT, //y
						type(double[].class), //samples
						TypeInfos.INT //sampleCount
					),
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
								method(
									ACC_PUBLIC | ACC_STATIC,
									getBulk_.clazz.info,
									"evaluate",
									TypeInfos.DOUBLE,
									types(WorldColumn.class, 'I', 'I', 'D', this.inputs.size())
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
			String methodName = methodDimension == 0 ? "getBulkX" : "getBulkY";
			this.clazz.newMethod(
				ACC_PUBLIC,
				methodName,
				TypeInfos.VOID, //returnType
				TypeInfos.LONG, //seed
				TypeInfos.INT, //x
				TypeInfos.INT, //z
				type(double[].class), //samples
				TypeInfos.INT //sampleCount
			)
			.scopes.withScope((MethodCompileContext getBulk) -> {
				VarInfo thisVar     = getBulk.addThis();
				VarInfo seed        = getBulk.newParameter("seed", TypeInfos.LONG);
				VarInfo x           = getBulk.newParameter("x", TypeInfos.INT);
				VarInfo y           = getBulk.newParameter("y", TypeInfos.INT);
				VarInfo samples     = getBulk.newParameter("samples", type(double[].class));
				VarInfo sampleCount = getBulk.newParameter("sampleCount", TypeInfos.INT);
				VarInfo column      = getBulk.newVariable("column", type(WorldColumn.class));

				//declare scratch arrays.
				VarInfo[] scratches = new VarInfo[this.inputs.size()];
				for (Input input : this.inputs.values()) {
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
				for (Input input : this.inputs.values()) {
					store(
						scratches[input.index],
						invokeStatic(
							method(
								ACC_PUBLIC | ACC_STATIC | ACC_INTERFACE,
								type(Grid.class),
								"getScratchArray",
								type(double[].class),
								TypeInfos.INT
							),
							load(sampleCount)
						)
					)
					.emitBytecode(getBulk);
					getBulk.node.visitLabel(label());
				}
				//fill scratch arrays.
				for (Input input : this.inputs.values()) {
					invokeInstance(
						getField(
							load(thisVar),
							input.fieldInfo(getBulk)
						),
						method(
							ACC_PUBLIC | ACC_INTERFACE,
							GRID_2D_TYPE_INFO.type,
							methodName,
							TypeInfos.VOID, //returnType
							TypeInfos.LONG, //seed
							TypeInfos.INT, //x
							TypeInfos.INT, //z
							type(double[].class), //samples
							TypeInfos.INT //sampleCount
						),
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
						store(index, ldc(0)),
						lt(this, load(index), load(sampleCount)),
						inc(index, 1),
						arrayStore(
							load(samples),
							load(index),
							invokeStatic(
								method(
									ACC_PUBLIC | ACC_STATIC,
									getBulk_.clazz.info,
									"evaluate",
									TypeInfos.DOUBLE,
									types(WorldColumn.class, 'I', 'I', 'D', this.inputs.size())
								),
								Stream.concat(
									Stream.of(
										load(column),
										maybeAdd(this, x, index, 0, methodDimension),
										maybeAdd(this, y, index, 1, methodDimension)
									),
									this.inputs.values().stream().map(input -> (
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
				for (Input input : this.inputs.values()) {
					invokeStatic(
						method(
							ACC_PUBLIC | ACC_STATIC | ACC_INTERFACE,
							type(Grid.class),
							"reclaimScratchArray",
							TypeInfos.VOID,
							type(double[].class)
						),
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