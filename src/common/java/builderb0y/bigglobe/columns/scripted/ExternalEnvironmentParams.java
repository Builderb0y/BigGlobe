package builderb0y.bigglobe.columns.scripted;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;

import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.MutableDependencyView;
import builderb0y.bigglobe.columns.scripted.tree.*;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.instructions.binary.AddInsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static org.objectweb.asm.Opcodes.*;

public class ExternalEnvironmentParams {

	/**
	invariants:
	must specify loadColumn or loadLookup, but not both.
	if loadColumn is specified, then loadX and loadZ are ignored.
	must specify both loadX and loadZ, or neither.
	*/
	public InsnTree loadColumn, loadLookup, loadCustomClass, loadX, loadY, loadZ, offsetY;
	public boolean mutable;
	public @Nullable DependencyView.MutableDependencyView dependencies;
	public @Nullable Identifier caller;

	public Handlers.Callback dependencyCallback(Holder<? extends DependencyView> reference) {
		return (ExpressionParser parser, CastResult result) -> {
			if (result != null && this.dependencies != null) this.dependencies.addDependency(reference);
		};
	}

	public ExternalEnvironmentParams withColumn(InsnTree loadColumn) {
		this.loadColumn = loadColumn;
		return this;
	}

	public ExternalEnvironmentParams withLookup(InsnTree loadLookup) {
		this.loadLookup = loadLookup;
		return this;
	}

	public ExternalEnvironmentParams withCustomClass(InsnTree loadCustomClass) {
		this.loadCustomClass = loadCustomClass;
		return this;
	}

	public ExternalEnvironmentParams withXZ(InsnTree loadX, InsnTree loadZ) {
		this.loadX = loadX;
		this.loadZ = loadZ;
		return this;
	}

	public ExternalEnvironmentParams withY(InsnTree loadY) {
		this.loadY = loadY;
		return this;
	}

	public ExternalEnvironmentParams offsetY(InsnTree offsetY) {
		this.offsetY = offsetY;
		return this;
	}

	public ExternalEnvironmentParams mutable(boolean mutable) {
		this.mutable = mutable;
		return this;
	}

	public ExternalEnvironmentParams mutable() {
		this.mutable = true;
		return this;
	}

	public ExternalEnvironmentParams trackDependencies(MutableDependencyView dependencies) {
		this.dependencies = dependencies;
		return this;
	}

	public ExternalEnvironmentParams withCaller(Identifier caller) {
		this.caller = caller;
		return this;
	}

	public boolean requiresNoArguments(boolean is3D, boolean assumeColumnProvided) {
		return (!is3D || this.loadY != null) && (assumeColumnProvided || this.loadColumn != null || (this.loadX != null && this.loadZ != null));
	}

	public String getPossibleArguments(boolean is3D, boolean assumeColumnProvided) {
		return (
			'(' +
			(assumeColumnProvided || this.loadColumn != null ? "forbidden" : this.loadX != null ? "optional" : "required") + " int x, " +
			(!is3D ? "forbidden" : this.loadY != null ? "optional" : "required") + " int y, " +
			(assumeColumnProvided || this.loadColumn != null ? "forbidden" : this.loadX != null ? "optional" : "required") + " int z" +
			')'
		);
	}

	public static StringBuilder appendIfMissing(StringBuilder builder, String columnValueName, InsnTree tree, String componentName) {
		return tree == null ? (builder == null ? new StringBuilder(columnValueName).append(" requires ") : builder.append(", ")).append(componentName) : builder;
	}

	public CastResult resolveColumn(
		ExpressionParser parser,
		String name,
		boolean is3D,
		boolean hasTraits,
		MethodInfo valueGetter,
		@Nullable MethodInfo valueSetter,
		@Nullable InsnTree loadColumn,
		InsnTree... arguments
	)
		throws ScriptParsingException {
		if (loadColumn == null) {
			loadColumn = this.loadColumn;
		}
		if (arguments.length != 0 && arguments[0].getTypeInfo().extendsOrImplements(ScriptedColumn.INFO.type)) {
			loadColumn = arguments[0];
			arguments = Arrays.copyOfRange(arguments, 1, arguments.length);
		}
		if (!is3D && (arguments.length & 1) != 0) {
			throw new ScriptParsingException("Invalid number of arguments for 2D column value " + name, parser.input);
		}
		if (loadColumn != null && arguments.length >= 2) {
			throw new ScriptParsingException("x and z are hard-coded in this context and cannot be manually specified.", parser.input);
		}
		InsnTree x, y, z;
		switch (arguments.length) {
			case 0 -> {
				x = this.loadX;
				y = this.loadY;
				z = this.loadZ;
			}
			case 1 -> {
				x = this.loadX;
				y = arguments[0];
				z = this.loadZ;
			}
			case 2 -> {
				x = arguments[0];
				y = this.loadY;
				z = arguments[1];
			}
			case 3 -> {
				x = arguments[0];
				y = arguments[1];
				z = arguments[2];
			}
			default -> {
				throw new ScriptParsingException("Too many arguments for column value " + name, parser.input);
			}
		}
		boolean requiredCasting = false;
		if (x != null && x != (x = x.cast(parser, TypeInfos.INT, CastMode.IMPLICIT_THROW, false))) requiredCasting = true;
		if (y != null && y != (y = y.cast(parser, TypeInfos.INT, CastMode.IMPLICIT_THROW, false))) requiredCasting = true;
		if (z != null && z != (z = z.cast(parser, TypeInfos.INT, CastMode.IMPLICIT_THROW, false))) requiredCasting = true;

		StringBuilder error = null;
		if (loadColumn == null) error = appendIfMissing(error, name, x, "x");
		if (is3D) error = appendIfMissing(error, name, y, "y");
		if (loadColumn == null) error = appendIfMissing(error, name, z, "z");
		if (error != null) throw new ScriptParsingException(error.toString(), parser.input);

		if (!this.mutable) valueSetter = null;
		if (this.offsetY != null) y = new AddInsnTree(y, this.offsetY, IADD);

		InsnTree result;
		if (loadColumn != null) {
			if (hasTraits) {
				if (is3D) {
					result = new StandAloneTraits3DGetterInsnTree(loadColumn, y, valueGetter, valueSetter);
				}
				else {
					result = new StandAloneTraits2DGetterInsnTree(loadColumn, valueGetter, valueSetter);
				}
			}
			else {
				if (is3D) {
					result = new StandAloneDirect3DGetterInsnTree(loadColumn, y, valueGetter, valueSetter);
				}
				else {
					result = new StandAloneDirect2DGetterInsnTree(loadColumn, valueGetter, valueSetter);
				}
			}
		}
		else if (this.loadLookup != null) {
			if (hasTraits) {
				if (is3D) {
					result = new LookupTraits3DGetterInsnTree(this.loadLookup, x, y, z, valueGetter, valueSetter);
				}
				else {
					result = new LookupTraits2DGetterInsnTree(this.loadLookup, x, z, valueGetter, valueSetter);
				}
			}
			else {
				if (is3D) {
					result = new LookupDirect3DGetterInsnTree(this.loadLookup, x, y, z, valueGetter, valueSetter);
				}
				else {
					result = new LookupDirect2DGetterInsnTree(this.loadLookup, x, z, valueGetter, valueSetter);
				}
			}
		}
		else {
			throw new ScriptParsingException("No column available.", parser.input);
		}
		return new CastResult(result, requiredCasting);
	}
}