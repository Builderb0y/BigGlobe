package builderb0y.bigglobe.compat.dhChunkGen;

import java.util.ArrayList;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBiomeWrapper;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint;

import net.minecraft.block.BlockState;

public class DataPointListBuilder extends ArrayList<DhApiTerrainDataPoint> {

	public IDhApiLevelWrapper level;
	public byte detailLevel;
	public int skyLightLevel;
	public Object[] query;
	public IDhApiBiomeWrapper biome;

	public DataPointListBuilder(IDhApiLevelWrapper level, byte detailLevel) {
		this.level = level;
		this.detailLevel = detailLevel;
		this.skyLightLevel = 15;
		this.query = new Object[1];
	}

	public void add(IDhApiBlockStateWrapper state, int minY, int maxY) {
		assert maxY > minY;
		this.add(new DhApiTerrainDataPoint(this.detailLevel, ((BlockState)(state.getWrappedMcObject())).getLuminance(), this.skyLightLevel, maxY, minY, state, this.biome));
	}

	public void add(BlockState state, int minY, int maxY) {
		this.query[0] = state;
		this.add(DhApi.Delayed.wrapperFactory.getBlockStateWrapper(this.query, this.level), minY, maxY);
	}
}