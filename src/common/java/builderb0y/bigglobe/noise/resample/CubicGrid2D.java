package builderb0y.bigglobe.noise.resample;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.bigglobe.noise.source.WhiteNoiseGrid2D;
import builderb0y.bigglobe.settings.Seed;

@AddPseudoField("salt")
@AddPseudoField("amplitude")
public class CubicGrid2D extends CubicResampleGrid2D {

	public CubicGrid2D(Seed salt, double amplitude, int scaleX, int scaleY) {
		super(new WhiteNoiseGrid2D(salt, amplitude), scaleX, scaleY);
	}

	public Seed salt() {
		return ((WhiteNoiseGrid2D)(this.source)).salt;
	}

	public double amplitude() {
		return ((WhiteNoiseGrid2D)(this.source)).amplitude;
	}
}