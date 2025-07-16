package builderb0y.bigglobe.codecs.registries;

import org.jetbrains.annotations.NotNull;

import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntryOwner;

import builderb0y.autocodec.coders.AutoCoder.NamedCoder;
import builderb0y.autocodec.common.AbstractDecodeContext;
import builderb0y.autocodec.common.DynamicOpsContext;
import builderb0y.autocodec.decoders.DecodeContext;
import builderb0y.autocodec.decoders.DecodeException;
import builderb0y.autocodec.reflection.reification.ReifiedType;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterDynamicRegistry;
import builderb0y.bigglobe.dynamicRegistries.BetterRegistry.BetterHardCodedRegistry;
import builderb0y.bigglobe.dynamicRegistries.BigGlobeDynamicRegistries;

public abstract class AbstractRegistryCoder<T_Object, T_Result> extends NamedCoder<T_Result> {

	public final RegistryKey<Registry<T_Object>> key;

	public AbstractRegistryCoder(@NotNull ReifiedType<T_Result> handledType, RegistryKey<Registry<T_Object>> key) {
		super(handledType);
		this.key = key;
	}

	public <T_Encoded> BetterRegistry<T_Object> registry(@NotNull DecodeContext<T_Encoded> context) throws DecodeException {
		return registry(this.key, context);
	}

	public static <T_Encoded, T_Object> BetterRegistry<T_Object> registry(@NotNull RegistryKey<Registry<T_Object>> key, @NotNull DynamicOpsContext<T_Encoded> context) throws DecodeException {
		/* if (BigGlobeDynamicRegistries.KEYS.contains(key) && ColumnEntryRegistry.Loading.LOADING != null && ColumnEntryRegistry.Loading.OVERRIDE.getCurrent() == null) {
			return ColumnEntryRegistry.Loading.LOADING.betterRegistryLookup.getRegistry(key);
		}
		else */ if (context.ops instanceof RegistryOps<T_Encoded> registryOps) {
			RegistryEntryLookup<T_Object> lookup = registryOps.getEntryLookup(key).orElse(null);
			if (lookup == null) {
				throw new DecodeException(() -> "Registry " + key.getValue() + " not present in RegistryOps");
			}
			RegistryEntryOwner<T_Object> owner = registryOps.getOwner(key).orElse(null);
			if (!(owner instanceof RegistryWrapper.Impl<T_Object> wrapperImpl)) {
				throw new DecodeException(() -> "Owner is not a RegistryWrapper.Impl: " + owner + " in registry " + key.getValue());
			}
			return new BetterDynamicRegistry<>(wrapperImpl, lookup);
		}
		else {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			MutableRegistry<T_Object> registry = (MutableRegistry<T_Object>)(Registries.REGISTRIES.get((RegistryKey)(key)));
			if (registry != null) {
				return new BetterHardCodedRegistry<>(registry);
			}
			else {
				throw new DecodeException(() -> "Not a RegistryOps: " + context.ops);
			}
		}
	}
}