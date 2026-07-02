package builderb0y.bigglobe.scripting.wrappers;

import java.util.random.RandomGenerator;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.ConfiguredColumnFactory;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.Hints;
import builderb0y.bigglobe.columns.scripted.ScriptedColumnLookup;
import builderb0y.bigglobe.util.WorldOrChunk;
import builderb0y.bigglobe.util.WorldOrChunk.ReadOnlyWorldDelegator;
import builderb0y.bigglobe.versions.HeightLimitViewVersions;
import builderb0y.scripting.bytecode.FieldInfo;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.InfoHolder;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ReadOnlyWorldWrapper implements ScriptedColumnLookup {

	public static final Info INFO = new Info();
	public static class Info extends InfoHolder {

		public FieldInfo
			random;
		public MethodInfo
			seed,
			getBlockState,
			isYLevelValid,
			minValidYLevel,
			maxValidYLevel;

		public InsnTree seed(InsnTree loadWorld) {
			return invokeInstance(loadWorld, this.seed);
		}

		public InsnTree minValidYLevel(InsnTree loadWorld) {
			return invokeInstance(loadWorld, this.minValidYLevel);
		}

		public InsnTree maxValidYLevel(InsnTree loadWorld) {
			return invokeInstance(loadWorld, this.maxValidYLevel);
		}

		public InsnTree random(InsnTree loadWorld) {
			return getField(loadWorld, this.random);
		}
	}

	public final WorldOrChunk world;
	public final MutableBlockPos pos;
	public final RandomGenerator random;
	public final ConfiguredColumnFactory columnFactory;
	public final Long2ObjectMap<ScriptedColumn> columns;

	public ReadOnlyWorldWrapper(
		WorldOrChunk world,
		RandomGenerator random,
		ConfiguredColumnFactory columnFactory,
		Long2ObjectMap<ScriptedColumn> columns
	) {
		this.world = world;
		this.pos = new MutableBlockPos();
		this.random = random;
		this.columnFactory = columnFactory;
		this.columns = columns;
	}

	public ReadOnlyWorldWrapper(
		Level world,
		RandomGenerator random,
		ConfiguredColumnFactory columnFactory,
		int maxColumns
	) {
		this.world = new ReadOnlyWorldDelegator(world, columnFactory.worldInfo().seed());
		this.pos = new MutableBlockPos();
		this.random = random;
		this.columnFactory = columnFactory;
		this.columns = new Long2ObjectLinkedOpenHashMap<>(maxColumns) {

			@Override
			public ScriptedColumn computeIfAbsent(long pos, Long2ObjectFunction<? extends ScriptedColumn> mappingFunction) {
				ScriptedColumn result = this.getAndMoveToLast(pos);
				if (result == null) {
					if (this.size() >= maxColumns) {
						result = this.removeFirst();
						result.setParamsUnchecked(result.params.at(ColumnPos.getX(pos), ColumnPos.getZ(pos)));
						this.putLast(pos, result);
					}
					else {
						result = mappingFunction.apply(pos);
					}
				}
				return result;
			}
		};
	}

	@Override
	public ConfiguredColumnFactory getSource() {
		return this.columnFactory;
	}

	@Override
	public ScriptedColumn lookupColumn(int x, int z) {
		return this.columns.computeIfAbsent(
			ColumnPos.asLong(x, z),
			(long packedPos) -> this.columnFactory.createAt(
				ColumnPos.getX(packedPos),
				ColumnPos.getZ(packedPos)
			)
		);
	}

	public long seed() {
		return this.world.getSeed();
	}

	public Hints hints() {
		return this.columnFactory.hints();
	}

	public BlockState getBlockState(int x, int y, int z) {
		return this.world.getBlockState(this.pos.set(x, y, z));
	}

	public boolean isYLevelValid(int y) {
		return !this.world.isOutsideBuildHeight(y);
	}

	public int minValidYLevel() {
		return this.world.getMinY();
	}

	public int maxValidYLevel() {
		return HeightLimitViewVersions.getMaxY(this.world);
	}
}