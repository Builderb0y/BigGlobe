package builderb0y.bigglobe.compat.dhChunkGen;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiLevelLoadEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.chunkgen.BigGlobeOverworldChunkGenerator;

public class FakeChunkGenSetterUpper extends DhApiLevelLoadEvent {

	@Override
	public void onLevelLoad(DhApiEventParam<EventParam> input) {
		if (input.value.levelWrapper instanceof ILevelWrapper wrapper) {
			World world = (World)(wrapper.getWrappedMcObject());
			if (world instanceof ServerWorld serverWorld && serverWorld.getChunkManager().getChunkGenerator() instanceof BigGlobeOverworldChunkGenerator generator) {
				BigGlobeMod.LOGGER.info("Setting up fast fake DH chunk generator.");
				DhApi.worldGenOverrides.registerWorldGeneratorOverride(wrapper, new DHOverworldChunkGenerator(wrapper, serverWorld, generator));
			}
		}
	}
}