package builderb0y.bigglobe.settings;

import builderb0y.autocodec.annotations.VerifyIntRange;
import builderb0y.autocodec.annotations.VerifyNullable;

public record OverworldUndergroundSettings(
	@VerifyIntRange(min = 0, max = 4096) int cobble_per_section,
	@VerifyNullable OverworldCaveSettings caves,
	@VerifyNullable OverworldCavernSettings deep_caverns
) {

	public boolean hasCaves() {
		return this.caves != null;
	}

	public boolean hasCaverns() {
		return this.deep_caverns != null;
	}
}