package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.bigglobe.settings.Seed;

@AddPseudoField("salt")
@AddPseudoField("amplitude")
public class LinearGrid1D extends LinearResampleGrid1D {

	public LinearGrid1D(Seed salt, double amplitude, int scaleX) {
		super(new WhiteNoiseGrid1D(salt, amplitude), scaleX);
	}

	public Seed salt() {
		return ((WhiteNoiseGrid1D)(this.source)).salt;
	}

	public double amplitude() {
		return ((WhiteNoiseGrid1D)(this.source)).amplitude;
	}
}