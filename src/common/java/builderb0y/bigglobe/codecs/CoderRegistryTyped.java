package builderb0y.bigglobe.codecs;

import builderb0y.autocodec.coders.AutoCoder;
import builderb0y.autocodec.coders.KeyDispatchCoder.Dispatchable;
import builderb0y.autocodec.reflection.reification.ReifiedType;

public interface CoderRegistryTyped<T_Base extends CoderRegistryTyped<T_Base>> extends Dispatchable<T_Base> {

	@SuppressWarnings("unchecked")
	public default ReifiedType<? extends T_Base> getType() {
		return (ReifiedType<? extends T_Base>)(ReifiedType.from(this.getClass()));
	}

	@Override
	public default AutoCoder<? extends T_Base> getCoder() {
		return BigGlobeAutoCodec.AUTO_CODEC.createCoder(this.getType());
	}
}