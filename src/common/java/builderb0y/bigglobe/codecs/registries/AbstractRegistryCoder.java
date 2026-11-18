package builderb0y.bigglobe.codecs.registries;

import org.jetbrains.annotations.NotNull;

import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;

import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.common.DynamicOpsContext;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.dynamicRegistries.RegistryAdapter;
import builderb0y.bigglobe.dynamicRegistries.RegistryAdapter.BasicImpl;

public abstract class AbstractRegistryCoder<T_Object, T_Result> extends NamedCoder<T_Result> {

	public final ResourceKey<Registry<T_Object>> key;

	public AbstractRegistryCoder(@NotNull ReifiedType<T_Result> handledType, ResourceKey<Registry<T_Object>> key) {
		super(handledType);
		this.key = key;
	}

	public <T_Encoded> RegistryAdapter<T_Object> registry(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		return registry(this.key, context);
	}

	public static <T_Encoded, T_Object> RegistryAdapter<T_Object> registry(@NotNull ResourceKey<Registry<T_Object>> key, @NotNull DynamicOpsContext<T_Encoded> context) throws DecodeException {
		if (context.ops instanceof RegistryOps<T_Encoded> registryOps) {
			HolderGetter<T_Object> getter = registryOps.getter(key).orElse(null);
			if (getter == null) {
				throw new DecodeException(() -> "Registry " + key.identifier() + " not present in RegistryOps");
			}
			return new BasicImpl<>(key, getter);
		}
		else {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Registry<T_Object> registry = (Registry<T_Object>)(BuiltInRegistries.REGISTRY.getValue((ResourceKey)(key)));
			if (registry != null) {
				return new BasicImpl<>(key, registry);
			}
			else {
				throw new DecodeException(() -> "Not a RegistryOps: " + context.ops);
			}
		}
	}
}