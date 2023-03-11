package builderb0y.bigglobe.columns;

import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.biome.Biome;

import builderb0y.bigglobe.settings.NetherSettings;

public class NetherColumn extends WorldColumn {

	public final NetherSettings settings;

	public NetherColumn(NetherSettings settings, long seed, int x, int z) {
		super(seed, x, z);
		this.settings = settings;
	}

	@Override
	public double getFinalTopHeightD() {
		return this.settings.max_y();
	}

	@Override
	public int getFinalTopHeightI() {
		return this.settings.max_y();
	}

	@Override
	public double getFinalBottomHeightD() {
		return this.settings.min_y();
	}

	@Override
	public int getFinalBottomHeightI() {
		return this.settings.min_y();
	}

	@Override
	public RegistryEntry<Biome> getBiome(int y) {
		return null; //fixme: implement this.
	}

	@Override
	public WorldColumn blankCopy() {
		return new NetherColumn(this.settings, this.seed, this.x, this.z);
	}
}