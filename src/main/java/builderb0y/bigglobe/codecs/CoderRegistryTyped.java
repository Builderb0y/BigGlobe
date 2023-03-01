package builderb0y.bigglobe.codecs;

import builderb0y.autocodec.reflection.reification.ReifiedType;

public interface CoderRegistryTyped {

	public default ReifiedType<? extends CoderRegistryTyped> getType() {
		return ReifiedType.from(this.getClass());
	}
}