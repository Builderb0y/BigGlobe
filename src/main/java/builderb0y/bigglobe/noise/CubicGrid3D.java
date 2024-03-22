package builderb0y.bigglobe.noise;

import builderb0y.autocodec.annotations.AddPseudoField;
import builderb0y.bigglobe.settings.Seed;

@AddPseudoField("salt")
@AddPseudoField("amplitude")
public class CubicGrid3D extends CubicResampleGrid3D {

	public CubicGrid3D(Seed salt, double amplitude, int scaleX, int scaleY, int scaleZ) {
		super(new WhiteNoiseGrid3D(salt, amplitude), scaleX, scaleY, scaleZ);
	}

	public Seed salt() {
		return ((WhiteNoiseGrid3D)(this.source)).salt;
	}

	public double amplitude() {
		return ((WhiteNoiseGrid3D)(this.source)).amplitude;
	}
}