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

public class ScriptedGrid1D extends ScriptedGrid<Grid1D> implements Grid1D {

	public static final GridTypeInfo GRID_1D_TYPE_INFO = new GridTypeInfo(Grid1D.class, Parser.class, 1);

	public final transient Grid1D delegate;

	public ScriptedGrid1D(String script, Map<String, Grid1D> inputs, double min, double max) throws ScriptParsingException {
		super(inputs, min, max);
		LinkedHashMap<String, Input> processedInputs = processInputs(inputs, GRID_1D_TYPE_INFO);
		Parser parser = new Parser(script, processedInputs);
		parser
		.addEnvironment(new Environment(processedInputs, GRID_1D_TYPE_INFO))
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
	public double getValue(long seed, int x) {
		return this.delegate.getValue(seed, x);
	}

	@Override
	public void getBulkX(long seed, int startX, double[] samples, int sampleCount) {
		this.delegate.getBulkX(seed, startX, samples, sampleCount);
	}

	public static class Parser extends ScriptedGrid.Parser {


		public Parser(String source, LinkedHashMap<String, Input> inputs) {
			super(source, inputs, GRID_1D_TYPE_INFO);
		}

		@Override
		public Grid1D parse() throws ScriptParsingException {
			return (Grid1D)(super.parse());
		}

		@Override
		public void addGetBulkOne(int methodDimension) {
			this.clazz.newMethod(
				ACC_PUBLIC,
				"getBulkX",
				TypeInfos.VOID,
				types("JI[DI")
			)
			.scopes.withScope((MethodCompileContext getBulkX) -> {
				Input input = this.inputs.values().iterator().next();
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
				invokeInterface(
					getField(
						load(thisVar),
						input.fieldInfo(getBulkX)
					),
					method(
						ACC_PUBLIC | ACC_INTERFACE,
						GRID_1D_TYPE_INFO.type,
						"getBulkX",
						TypeInfos.VOID,
						types("LI[DI")
					),
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
						store(index, ldc(0)),
						lt(this, load(index), load(sampleCount)),
						inc(index, 1),
						arrayStore(
							load(samples),
							load(index),
							invokeStatic(
								method(
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
			this.clazz.newMethod(ACC_PUBLIC, "getBulkX", TypeInfos.VOID, types("JI[DI")).scopes.withScope((MethodCompileContext getBulkX) -> {
				VarInfo thisVar     = getBulkX.addThis();
				VarInfo seed        = getBulkX.newParameter("seed", TypeInfos.LONG);
				VarInfo startX      = getBulkX.newParameter("startX", TypeInfos.INT);
				VarInfo samples     = getBulkX.newParameter("samples", DOUBLE_ARRAY);
				VarInfo sampleCount = getBulkX.newParameter("sampleCount", TypeInfos.INT);
				VarInfo column      = getBulkX.newVariable("column", type(WorldColumn.class));

				//declare scratch arrays.
				VarInfo[] scratches = new VarInfo[this.inputs.size()];
				for (Input input : this.inputs.values()) {
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
				for (Input input : this.inputs.values()) {
					store(
						scratches[input.index],
						invokeStatic(
							method(
								ACC_PUBLIC | ACC_STATIC | ACC_INTERFACE,
								type(Grid.class),
								"getScratchArray",
								DOUBLE_ARRAY,
								TypeInfos.INT
							),
							load(sampleCount)
						)
					)
					.emitBytecode(getBulkX);
					getBulkX.node.visitLabel(label());
				}
				//fill scratch arrays.
				for (Input input : this.inputs.values()) {
					invokeInterface(
						getField(load(thisVar), input.fieldInfo(getBulkX)),
						method(
							ACC_PUBLIC | ACC_INTERFACE,
							GRID_1D_TYPE_INFO.type,
							"getBulkX",
							TypeInfos.VOID,
							types("JI[DI")
						),
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
						store(index, ldc(0)),
						lt(this, load(index), load(sampleCount)),
						inc(index, 1),
						arrayStore(
							load(samples),
							load(index),
							invokeStatic(
								method(
									ACC_PUBLIC | ACC_STATIC,
									getBulkX_.clazz.info,
									"evaluate",
									TypeInfos.DOUBLE,
									types(WorldColumn.class, 'I', 'D', this.inputs.size())
								),
								Stream.concat(
									Stream.of(
										load(column),
										add(this, load(startX), load(index))
									),
									this.inputs.values().stream().map(input -> (
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
				for (Input input : this.inputs.values()) {
					invokeStatic(
						method(
							ACC_PUBLIC | ACC_STATIC | ACC_INTERFACE,
							type(Grid.class),
							"reclaimScratchArray",
							TypeInfos.VOID,
							DOUBLE_ARRAY
						),
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