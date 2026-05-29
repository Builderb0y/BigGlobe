package builderb0y.bigglobe.columns.scripted2.entries;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.bigglobe.columns.scripted2.MappedRangeArray;
import builderb0y.bigglobe.columns.scripted2.MappedRangeNumberArray;
import builderb0y.bigglobe.columns.scripted2.MappedRangeObjectArray;
import builderb0y.bigglobe.classes.spec.ElementSpec;
import builderb0y.bigglobe.columns.scripted2.AccessSchema;
import builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted2.Valid;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.ScopeContext.LoopName;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclarationInsnTree;
import builderb0y.scripting.bytecode.tree.VariableDeclareAssignInsnTree;
import builderb0y.scripting.bytecode.tree.flow.loop.ForIntRangeInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.ScopedInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.binary.SubtractInsnTree;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public abstract class LoopColumnEntry extends NonConstantColumnEntry {

	public LoopColumnEntry(AccessSchema params, @VerifyNullable Valid valid, boolean cache) {
		super(params, valid, cache);
	}

	@Override
	public InsnTree makeBulkComputer(ColumnEntryRegistry registry, ColumnEntryContext context) throws ScriptParsingException {
		InsnTree loadColumn = registry.columnCompileContext.loadColumn();
		InsnTree loadMappedArray = getField(loadColumn, context.valueField.info);
		TypeInfo elementType = this.typeInfo(registry);
		InsnTree getRawArray = getField(loadMappedArray, elementType.isObject() ? MappedRangeObjectArray.ARRAY : MappedRangeNumberArray.INFO.array);
		VariableDeclareAssignInsnTree rawArray = new VariableDeclareAssignInsnTree(new LazyVarInfo("array", getRawArray.getTypeInfo()), getRawArray);
		LazyVarInfo iterY = new LazyVarInfo("iterY", TypeInfos.INT);
		LazyVarInfo minY = new LazyVarInfo("minY", TypeInfos.INT);
		LazyVarInfo maxY = new LazyVarInfo("maxY", TypeInfos.INT);
		InsnTree index = new SubtractInsnTree(load(iterY), load(minY), ISUB);
		InsnTree computer = invokeInstance(loadColumn, context.computer.info, load(iterY));
		LoadInsnTree loadRawArray = load(rawArray.variable);
		return new ScopedInsnTree(
			seq(
				rawArray,
				new ForIntRangeInsnTree(
					LoopName.of(null),
					new VariableDeclarationInsnTree(iterY),
					true,
					getField(loadMappedArray, MappedRangeArray.INFO.minCached),
					true,
					minY,
					getField(loadMappedArray, MappedRangeArray.INFO.maxCached),
					false,
					maxY,
					ldc(1),
					null,
					switch (elementType.getSort()) {
						case VOID -> throw new IllegalStateException("void array");
						case BOOLEAN -> invokeInstance(loadRawArray, NumberArray.INFO.implSetZ, index, computer);
						case BYTE -> invokeInstance(loadRawArray, NumberArray.INFO.implSetB, index, computer);
						case CHAR -> throw new UnsupportedOperationException("char array");
						case SHORT -> invokeInstance(loadRawArray, NumberArray.INFO.implSetS, index, computer);
						case INT -> invokeInstance(loadRawArray, NumberArray.INFO.implSetI, index, computer);
						case LONG -> invokeInstance(loadRawArray, NumberArray.INFO.implSetL, index, computer);
						case FLOAT -> invokeInstance(loadRawArray, NumberArray.INFO.implSetF, index, computer);
						case DOUBLE -> invokeInstance(loadRawArray, NumberArray.INFO.implSetD, index, computer);
						case OBJECT, ARRAY -> arrayStore(loadRawArray, index, computer);
					}
				)
			)
		);
	}
}