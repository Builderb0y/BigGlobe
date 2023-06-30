package builderb0y.bigglobe.features;

import java.lang.reflect.Array;

import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.chunkgen.BigGlobeChunkGenerator.SortedFeatures;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.overriders.Overrider;

public class OverrideFeature<T_Holder extends Overrider.Holder<?>> extends DummyFeature<OverrideFeature.Config<T_Holder>> {

	public final Class<T_Holder> holderClass;

	public OverrideFeature(Class<T_Holder> holderClass) {
		super(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(ReifiedType.parameterize(Config.class, ReifiedType.from(holderClass))));
		this.holderClass = holderClass;
	}

	@SuppressWarnings("unchecked")
	public static <T_Holder extends Overrider.Holder<?>> T_Holder[] collect(SortedFeatures features, OverrideFeature<T_Holder> feature) {
		return features.streamConfigs(feature).map(Config::script).toArray((int length) -> (T_Holder[])(Array.newInstance(feature.holderClass, length)));
	}

	public static class Config<T_Holder extends Overrider.Holder<?>> extends DummyConfig {

		public final T_Holder script;

		public Config(T_Holder script) {
			this.script = script;
		}

		public T_Holder script() {
			return this.script;
		}
	}
}