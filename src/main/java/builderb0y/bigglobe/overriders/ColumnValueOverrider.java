package builderb0y.bigglobe.overriders;

import java.lang.reflect.Method;
import java.util.List;

import com.google.common.collect.ObjectArrays;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureSet.WeightedEntry;
import net.minecraft.util.math.BlockBox;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import builderb0y.autocodec.annotations.*;
import builderb0y.bigglobe.columns.scripted.ColumnScript;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.math.Interpolator;
import builderb0y.bigglobe.noise.NumberArray;
import builderb0y.bigglobe.scripting.environments.NbtScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.StructureScriptEnvironment;
import builderb0y.bigglobe.scripting.environments.WoodPaletteScriptEnvironment;
import builderb0y.bigglobe.scripting.wrappers.StructureStartWrapper;
import builderb0y.bigglobe.structures.DelegatingStructure;
import builderb0y.bigglobe.structures.ScriptStructures;
import builderb0y.bigglobe.structures.scripted.ScriptedStructure;
import builderb0y.bigglobe.util.DelayedEntryList;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.ReflectionData;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface ColumnValueOverrider extends ColumnScript {

	public abstract void override(ScriptedColumn column, ScriptStructures structures);

	public static double distanceToSquare(ScriptedColumn column, double minX, double minZ, double maxX, double maxZ) {
		double offsetX = Interpolator.clamp(minX, maxX, column.x()) - column.x();
		double offsetZ = Interpolator.clamp(minZ, maxZ, column.z()) - column.z();
		return Math.sqrt(BigGlobeMath.squareD(offsetX, offsetZ));
	}

	public static double distanceToSquare(ScriptedColumn column, StructureStartWrapper structure) {
		return distanceToSquare(column, structure.minX(), structure.minZ(), structure.maxX(), structure.maxZ());
	}

	public static double distanceToSquare(ScriptedColumn column, StructurePiece piece) {
		BlockBox box = piece.getBoundingBox();
		return distanceToSquare(column, box.getMinX(), box.getMinZ(), box.getMaxX(), box.getMaxZ());
	}

	public static double distanceToCircle(ScriptedColumn column, double centerX, double centerZ, double radius) {
		return Math.max(Math.sqrt(BigGlobeMath.squareD(centerX - column.x(), centerZ - column.z())) - radius, 0.0D);
	}

	public static double _distanceToCircle(ScriptedColumn column, BlockBox box, double radius) {
		return distanceToCircle(
			column,
			(box.getMinX() + box.getMaxX()) * 0.5D,
			(box.getMinZ() + box.getMaxZ()) * 0.5D,
			radius
		);
	}

	public static double _distanceToCircle(ScriptedColumn column, BlockBox box) {
		return _distanceToCircle(
			column,
			box,
			Math.min(
				box.getMaxX() - box.getMinX(),
				box.getMaxZ() - box.getMinZ()
			)
			* 0.5D
		);
	}

	public static double distanceToCircle(ScriptedColumn column, StructureStartWrapper structure, double radius) {
		return _distanceToCircle(column, structure.box(), radius);
	}

	public static double distanceToCircle(ScriptedColumn column, StructurePiece piece, double radius) {
		return _distanceToCircle(column, piece.getBoundingBox(), radius);
	}

	public static double distanceToCircle(ScriptedColumn column, StructureStartWrapper structure) {
		return _distanceToCircle(column, structure.box());
	}

	public static double distanceToCircle(ScriptedColumn column, StructurePiece piece) {
		return _distanceToCircle(column, piece.getBoundingBox());
	}

	public static record StructureFilter(
		@VerifyNullable DelayedEntryList<Structure> structure,
		@VerifyNullable DelayedEntryList<StructureType<?>> structure_type,
		@VerifyIntRange(min = 0L) int radius_in_chunks
	) {

		public boolean matches(Structure structure) {
			while (structure instanceof DelegatingStructure delegating) {
				structure = delegating.delegate().value();
			}
			if (this.structure != null) {
				if (this.structure_type != null) {
					return this.structure.objectSet().contains(structure) || this.structure_type.objectSet().contains(structure.getType());
				}
				else {
					return this.structure.objectSet().contains(structure);
				}
			}
			else {
				if (this.structure_type != null) {
					return this.structure_type.objectSet().contains(structure.getType());
				}
				else {
					return true; //allow { "radius": ... } to match all structures.
				}
			}
		}

		public static int getSearchRadius(StructureFilter[] filters, StructureSet set, int baseRadius) {
			int radius = baseRadius;
			List<WeightedEntry> structures = set.structures();
			int structureCount = structures.size();
			for (StructureFilter filter : filters) {
				if (radius >= filter.radius_in_chunks) continue;
				for (int index = 0; index < structureCount; index++) {
					if (filter.matches(structures.get(index).structure().value())) {
						radius = filter.radius_in_chunks;
						break;
					}
				}
			}
			return radius;
		}
	}

	public static non-sealed class Entry implements Overrider {

		public final Holder script;
		public final @DefaultBoolean(true) boolean raw_generation;
		public final @DefaultBoolean(true) boolean feature_generation;
		public final StructureFilter @VerifyNullable @SingletonArray [] structure_filter;
		public final transient Reference2IntMap<Structure> cachedRadii;

		public Entry(
			Holder script,
			@DefaultBoolean(true) boolean raw_generation,
			@DefaultBoolean(true) boolean feature_generation,
			StructureFilter @VerifyNullable @SingletonArray [] structure_filter
		) {
			this.script = script;
			this.raw_generation = raw_generation;
			this.feature_generation = feature_generation;
			this.structure_filter = structure_filter;
			this.cachedRadii = structure_filter != null ? new Reference2IntOpenHashMap<>() : null;
		}

		@Override
		public Type getOverriderType() {
			return Type.COLUMN_VALUE;
		}

		public boolean matches(Structure structure) {
			if (this.structure_filter == null) return true;
			for (StructureFilter filter : this.structure_filter) {
				if (filter.matches(structure)) return true;
			}
			return false;
		}

		public int getSearchRadius(Structure structure) {
			if (this.structure_filter == null) return 1;
			synchronized (this.cachedRadii) {
				return this.cachedRadii.computeIfAbsent(structure, (Structure structure_) -> {
					int radius = -1;
					for (StructureFilter filter : this.structure_filter) {
						if (filter.radius_in_chunks > radius && filter.matches(structure)) {
							radius = filter.radius_in_chunks;
						}
					}
					return radius;
				});
			}
		}

		public int getSearchRadius(StructureSet set, int baseRadius) {
			if (this.structure_filter == null) return Math.max(baseRadius, 1);
			return StructureFilter.getSearchRadius(this.structure_filter, set, baseRadius);
		}
	}

	public static int getSearchRadius(RegistryEntry<Entry>[] overriders, StructureSet set) {
		int radius = -1;
		for (RegistryEntry<Entry> overrider : overriders) {
			radius = overrider.value().getSearchRadius(set, radius);
		}
		return radius;
	}

	@Wrapper
	public static class Holder extends ColumnScript.BaseHolder<ColumnValueOverrider> implements ColumnValueOverrider {

		public Holder(ScriptUsage usage) {
			super(usage);
		}

		@Override
		public boolean isColumnMutable() {
			return true;
		}

		@Override
		public Class<ColumnValueOverrider> getScriptClass() {
			return ColumnValueOverrider.class;
		}

		@Override
		public void addExtraFunctionsToEnvironment(ImplParameters parameters, MutableScriptEnvironment environment) {
			super.addExtraFunctionsToEnvironment(parameters, environment);
			InsnTree loadColumn = load(parameters.actualColumn);
			environment
			.addAll(StructureScriptEnvironment.INSTANCE)
			.configure(NbtScriptEnvironment.createImmutable())
			.addFieldGet(ScriptedStructure.Piece.class, "data")
			.addVariableLoad("structures", type(ScriptStructures.class))
			.configure(JavaUtilScriptEnvironment.withoutRandom())
			.addAll(WoodPaletteScriptEnvironment.BASE);
			for (String name : new String[] { "distanceToSquare", "distanceToCircle" }) {
				for (Method method : ReflectionData.forClass(ColumnValueOverrider.class).getDeclaredMethods(name)) {
					MethodInfo info = MethodInfo.forMethod(method);
					environment.addFunction(name, new FunctionHandler.Named(info.toString(), (ExpressionParser parser, String name1, InsnTree... arguments) -> {
						InsnTree[] prefixedArguments = ObjectArrays.concat(loadColumn, arguments);
						InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, info, CastMode.IMPLICIT_NULL, prefixedArguments);
						return castArguments == null ? null : new CastResult(invokeStatic(info, castArguments), castArguments != prefixedArguments);
					}));
				}
			}
		}

		@Override
		public void override(ScriptedColumn column, ScriptStructures structures) {
			NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
			int used = manager.used;
			try {
				this.script.override(column, structures);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
			}
			finally {
				manager.used = used;
			}
		}
	}
}