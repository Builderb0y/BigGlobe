package builderb0y.bigglobe.compat.distanthorizons;

import java.util.ArrayList;
import net.minecraft.world.level.block.state.BlockState;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBiomeWrapper;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint;
import builderb0y.bigglobe.compat.distanthorizons.DistantHorizonsCompat.DHCode;

public class DataPointListBuilder extends ArrayList<DhApiTerrainDataPoint> {

	public IDhApiLevelWrapper level;
	public byte detailLevel;
	public int yOffset;
	public int skyLightLevel;
	public Object[] query;
	public IDhApiBiomeWrapper biome;

	public DataPointListBuilder(IDhApiLevelWrapper level, byte detailLevel, IDhApiBiomeWrapper biome, int yOffset) {
		this.level = level;
		this.detailLevel = detailLevel;
		this.yOffset = yOffset;
		this.skyLightLevel = 15;
		this.query = new Object[1];
		this.biome = biome;
	}

	public void add(BlockState mcState, IDhApiBlockStateWrapper dhState, int minY, int maxY) {
		assert maxY > minY;
		minY -= this.yOffset;
		maxY -= this.yOffset;
		this.add(DHCode.newDataPoint(this.detailLevel, mcState.getLightEmission(), this.skyLightLevel, minY, maxY, dhState, this.biome));
	}

	public void add(BlockState state, int minY, int maxY) {
		this.query[0] = state;
		this.add(state, DhApi.Delayed.wrapperFactory.getBlockStateWrapper(this.query, this.level), minY, maxY);
	}
}