package builderb0y.bigglobe.features;

import java.lang.reflect.Array;

import builderb0y.autocodec.annotations.DefaultInt;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.chunkgen.BigGlobeChunkGenerator.SortedFeatures;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.overriders.LegacyOverrider;

public class OverrideFeature<T_Holder extends LegacyOverrider.Holder<?>> extends DummyFeature<OverrideFeature.Config<T_Holder>> {

	public final Class<T_Holder> holderClass;

	public OverrideFeature(Class<T_Holder> holderClass) {
		super(BigGlobeAutoCodec.AUTO_CODEC.createDFUCodec(ReifiedType.parameterize(Config.class, ReifiedType.from(holderClass))));
		this.holderClass = holderClass;
	}

	@SuppressWarnings("unchecked")
	public static <T_Holder extends LegacyOverrider.Holder<?>> T_Holder[] collect(SortedFeatures features, OverrideFeature<T_Holder> feature) {
		return features.streamConfigs(feature).sorted().map(Config::script).toArray((int length) -> (T_Holder[])(Array.newInstance(feature.holderClass, length)));
	}

	public static class Config<T_Holder extends LegacyOverrider.Holder<?>> extends DummyConfig implements Comparable<Config<T_Holder>> {

		public final T_Holder script;
		public final @DefaultInt(0) int priority;

		public Config(T_Holder script, int priority) {
			this.script = script;
			this.priority = priority;
		}

		public T_Holder script() {
			return this.script;
		}

		@Override
		public int compareTo(OverrideFeature.Config<T_Holder> that) {
			return Integer.compare(this.priority, that.priority);
		}
	}
}