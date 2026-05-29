package builderb0y.bigglobe.columns.scripted2.entries;

import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.classes.VoronoiSampler;
import builderb0y.bigglobe.classes.compile.DetailedException;
import builderb0y.bigglobe.classes.spec.ElementSpec;
import builderb0y.bigglobe.columns.scripted2.AccessSchema;
import builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted2.ColumnValueException;
import builderb0y.bigglobe.columns.scripted2.Valid;
import builderb0y.bigglobe.settings.VoronoiDiagram2D;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.invokers.ReceiverInvokeInsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class VoronoiColumnEntry extends NonConstantColumnEntry {

	public final VoronoiDiagram2D diagram;

	public VoronoiColumnEntry(
		AccessSchema params,
		@VerifyNullable Valid valid,
		VoronoiDiagram2D diagram
	) {
		super(params, valid, true);
		this.diagram = diagram;
	}

	@Override
	public void verify(ColumnEntryRegistry registry) throws DetailedException {
		super.verify(registry);
		if (!this.typeInfo(registry).equals(VoronoiSampler.INFO.type)) {
			throw new ColumnValueException("Voronoi params type must be 'voronoi'.");
		}
		if (this.params.is_3d()) {
			throw new ColumnValueException("3D voronoi column values are not yet supported.");
		}
	}

	@Override
	public void compile(ColumnEntryRegistry registry) throws DetailedException {
		LoadInsnTree loadSelf = load("this", registry.columnCompileContext.columnTypeInfo());
		ColumnEntryContext context = registry.columnCompileContext.getCompileContext(this);
		putField(
			loadSelf,
			context.valueField.info,
			newInstance(
				VoronoiSampler.CONSTRUCTOR.methodInfo,
				ldc(this.diagram, type(VoronoiDiagram2D.class)),
				loadSelf
			)
		)
		.emitBytecode(registry.columnCompileContext.constructor);
		super.compile(registry);
	}

	@Override
	public InsnTree makeComputer(ColumnEntryRegistry registry, ColumnEntryContext context, @Nullable InsnTree loadY) throws ScriptParsingException {
		return newInstance(VoronoiSampler.CONSTRUCTOR.methodInfo, ldc(this.diagram, type(VoronoiDiagram2D.class)), registry.columnCompileContext.loadColumn());
	}

	@Override
	public InsnTree makeBulkComputer(ColumnEntryRegistry registry, ColumnEntryContext context) throws ScriptParsingException {
		throw new UnsupportedOperationException();
	}
}