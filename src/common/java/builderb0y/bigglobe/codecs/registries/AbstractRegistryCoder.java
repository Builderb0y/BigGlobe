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
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterDynamicRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterHardCodedRegistry;

public abstract class AbstractRegistryCoder<T_Object, T_Result> extends NamedCoder<T_Result> {

	public final ResourceKey<Registry<T_Object>> key;

	public AbstractRegistryCoder(@NotNull ReifiedType<T_Result> handledType, ResourceKey<Registry<T_Object>> key) {
		super(handledType);
		this.key = key;
	}

	public <T_Encoded> BetterRegistry<T_Object> registry(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		return registry(this.key, context);
	}

	public static <T_Encoded, T_Object> BetterRegistry<T_Object> registry(@NotNull ResourceKey<Registry<T_Object>> key, @NotNull DynamicOpsContext<T_Encoded> context) throws DecodeException {
		/* if (BigGlobeDynamicRegistries.KEYS.contains(key) && ColumnEntryRegistry.Loading.LOADING != null && ColumnEntryRegistry.Loading.OVERRIDE.getCurrent() == null) {
			return ColumnEntryRegistry.Loading.LOADING.betterRegistryLookup.getRegistry(key);
		}
		else */
		if (context.ops instanceof RegistryOps<T_Encoded> registryOps) {
			HolderGetter<T_Object> lookup = registryOps.getter(key).orElse(null);
			if (lookup == null) {
				throw new DecodeException(() -> "Registry " + key.identifier() + " not present in RegistryOps");
			}
			HolderOwner<T_Object> owner = registryOps.owner(key).orElse(null);
			if (!(owner instanceof HolderLookup.RegistryLookup<T_Object> wrapperImpl)) {
				throw new DecodeException(() -> "Owner is not a RegistryWrapper.Impl: " + owner + " in registry " + key.identifier());
			}
			return new BetterDynamicRegistry<>(wrapperImpl, lookup);
		}
		else {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			WritableRegistry<T_Object> registry = (WritableRegistry<T_Object>)(BuiltInRegistries.REGISTRY.getValue((ResourceKey)(key)));
			if (registry != null) {
				return new BetterHardCodedRegistry<>(registry);
			}
			else {
				throw new DecodeException(() -> "Not a RegistryOps: " + context.ops);
			}
		}
	}
}