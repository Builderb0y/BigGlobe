package builderb0y.bigglobe.columns.scripted;

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
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.UsageCallback;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static org.objectweb.asm.Opcodes.*;

public class ExternalEnvironmentParams {

	/**
	invariants:
	must specify both loadX and loadZ, or neither.
	*/
	public InsnTree loadColumn, loadLookup, loadCustomClass, loadX, loadY, loadZ, offsetY;
	public String loadLookupName;
	public boolean mutable;
	public @Nullable DependencyView.MutableDependencyView dependencies;
	public @Nullable Identifier caller;

	public UsageCallback dependencyCallback(Holder<? extends DependencyView> reference) {
		MutableDependencyView dependencies = this.dependencies;
		return (ExpressionParser parser, String name) -> {
			if (dependencies != null) dependencies.addDependency(reference);
		};
	}

	public ExternalEnvironmentParams withColumn(InsnTree loadColumn) {
		this.loadColumn = loadColumn;
		return this;
	}

	public ExternalEnvironmentParams withLookup(String name, InsnTree loadLookup) {
		this.loadLookupName = name;
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

	public static record Access(AccessMode mode, InsnTree loader) {

		public static Access from(ExpressionParser parser, @Nullable InsnTree receiver) {
			AccessMode mode = AccessMode.from(receiver);
			if (mode != null) {
				return new Access(mode, receiver);
			}
			InsnTree column = parser.environment.getImportedObject(parser, ScriptedColumn.INFO.type);
			InsnTree lookup = parser.environment.getImportedObject(parser, ScriptedColumnLookup.TYPE);
			return from(column, lookup);
		}

		public static Access from(InsnTree column, InsnTree lookup) {
			if (column != null) {
				if (lookup != null) {
					throw new IllegalStateException("Environment has both column and lookup imported");
				}
				else {
					return new Access(AccessMode.COLUMN, column);
				}
			}
			else {
				if (lookup != null) {
					return new Access(AccessMode.LOOKUP, lookup);
				}
				else {
					return null;
				}
			}
		}
	}

	public static enum AccessMode {
		COLUMN,
		LOOKUP;

		public static AccessMode from(@Nullable InsnTree receiver) {
			if (receiver == null) return null;
			if (receiver.getTypeInfo().extendsOrImplements(ScriptedColumn.INFO.type)) return COLUMN;
			if (receiver.getTypeInfo().extendsOrImplements(ScriptedColumnLookup.TYPE)) return LOOKUP;
			throw new IllegalArgumentException("Expected receiver to be a column or column lookup, but it was " + receiver.getTypeInfo().getClassName());
		}
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
		@Nullable InsnTree receiver,
		InsnTree... arguments
	)
	throws ScriptParsingException {
		Access access = Access.from(parser, receiver);
		if (!is3D && (arguments.length & 1) != 0) {
			throw new ScriptParsingException("Invalid number of arguments for 2D column value " + name, parser.input);
		}
		if (access.mode == AccessMode.COLUMN && arguments.length >= 2) {
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
		if (access.mode == AccessMode.LOOKUP) error = appendIfMissing(error, name, x, "x");
		if (is3D                            ) error = appendIfMissing(error, name, y, "y");
		if (access.mode == AccessMode.LOOKUP) error = appendIfMissing(error, name, z, "z");
		if (error != null) throw new ScriptParsingException(error.toString(), parser.input);

		if (!this.mutable) valueSetter = null;
		if (this.offsetY != null) y = new AddInsnTree(y, this.offsetY, IADD);

		InsnTree result = switch (access.mode) {
			case COLUMN -> {
				if (hasTraits) {
					if (is3D) {
						yield new StandAloneTraits3DGetterInsnTree(access.loader, y, valueGetter, valueSetter);
					}
					else {
						yield new StandAloneTraits2DGetterInsnTree(access.loader, valueGetter, valueSetter);
					}
				}
				else {
					if (is3D) {
						yield new StandAloneDirect3DGetterInsnTree(access.loader, y, valueGetter, valueSetter);
					}
					else {
						yield new StandAloneDirect2DGetterInsnTree(access.loader, valueGetter, valueSetter);
					}
				}
			}
			case LOOKUP -> {
				if (hasTraits) {
					if (is3D) {
						yield new LookupTraits3DGetterInsnTree(access.loader, x, y, z, valueGetter, valueSetter);
					}
					else {
						yield new LookupTraits2DGetterInsnTree(access.loader, x, z, valueGetter, valueSetter);
					}
				}
				else {
					if (is3D) {
						yield new LookupDirect3DGetterInsnTree(access.loader, x, y, z, valueGetter, valueSetter);
					}
					else {
						yield new LookupDirect2DGetterInsnTree(access.loader, x, z, valueGetter, valueSetter);
					}
				}
			}
		};
		return new CastResult(result, requiredCasting);
	}
}