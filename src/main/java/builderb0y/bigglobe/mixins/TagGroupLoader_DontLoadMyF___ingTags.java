package builderb0y.bigglobe.mixins;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Either;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.tag.TagEntry.ValueGetter;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.registry.tag.TagGroupLoader.TagDependencies;
import net.minecraft.registry.tag.TagGroupLoader.TrackedEntry;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.config.BigGlobeConfig;

/**
big thanks to Blodhgarm for making the "Load My F***ing Tags" mod.
without it, I would've had to figure out how tag loading logic worked myself.
but with it, I can just copy what they already had (it's CC0, so I'm allowed to do this).

anyway, Load My F***ing Tags is a great mod, but I am now extending it to add another option:
instead of just loading the tag (ignore errors) or not loading the tag (also ignoring errors),
I am adding the option to *not* ignore errors, making data pack validation fail if errors are encountered.
this behavior is configurable, and useful for data pack developers.
*/
@Mixin(TagGroupLoader.class)
public class TagGroupLoader_DontLoadMyF___ingTags {

	@Unique
	private static final ThreadLocal<Identifier> CURRENT_TAG_ID = new ThreadLocal<>();

	@WrapMethod(method = "method_51476")
	private void bigglobe_storeCurrentTagId(
		ValueGetter valueGetter,
		Map map,
		Identifier id,
		TagDependencies dependencies,
		Operation<Void> original
	) {
		Identifier old = CURRENT_TAG_ID.get();
		CURRENT_TAG_ID.set(id);
		try {
			original.call(valueGetter, map, id, dependencies);
		}
		finally {
			CURRENT_TAG_ID.set(old);
		}
	}

	@Inject(method = "resolveAll", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
	private <T> void bigglobe_checkForErrors(
		ValueGetter<T> valueGetter,
		List<TrackedEntry> entries,
		CallbackInfoReturnable<Either<List<TrackedEntry>, List<T>>> callback,
		@Local(ordinal = 1) List<TrackedEntry> errors
	) {
		if (!errors.isEmpty() && ColumnEntryRegistry.Loading.addInvalidTag(CURRENT_TAG_ID.get(), errors)) {
			errors.clear();
		}
	}

	#if MC_VERSION >= MC_1_21_2

		@Inject(method = "startReload(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/registry/DynamicRegistryManager;)Ljava/util/List;", at = @At("HEAD"))
		private static void bigglobe_prepareForReload(
			ResourceManager resourceManager,
			DynamicRegistryManager registryManager,
			CallbackInfoReturnable<List<net.minecraft.registry.Registry.PendingTagLoad<?>>> callback
		) {
			ColumnEntryRegistry.Loading.invalidTagHandling = BigGlobeConfig.INSTANCE.get().dataPackDebugging.invalidTagHandling;
			ColumnEntryRegistry.Loading.invalidTags = new HashMap<>();
		}

	#else

		@Inject(method = "load", at = @At("HEAD"))
		private static void bigglobe_prepareForReload(ResourceManager manager, CallbackInfoReturnable<Map<Identifier, Collection<?>>> callback) {
			ColumnEntryRegistry.Loading.invalidTagHandling = BigGlobeConfig.INSTANCE.get().dataPackDebugging.invalidTagHandling;
			ColumnEntryRegistry.Loading.invalidTags = new HashMap<>();
		}

	#endif
}