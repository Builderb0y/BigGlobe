package builderb0y.bigglobe.noise.resample;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.bigglobe.noise.source.WhiteNoiseGrid1D;
import builderb0y.bigglobe.settings.Seed;

@AddPseudoField("salt")
@AddPseudoField("amplitude")
public class SmoothGrid1D extends SmoothResampleGrid1D {

	public SmoothGrid1D(Seed salt, double amplitude, int scaleX) {
		super(new WhiteNoiseGrid1D(salt, amplitude), scaleX);
	}

	public Seed salt() {
		return ((WhiteNoiseGrid1D)(this.source)).salt;
	}

	public double amplitude() {
		return ((WhiteNoiseGrid1D)(this.source)).amplitude;
	}
}