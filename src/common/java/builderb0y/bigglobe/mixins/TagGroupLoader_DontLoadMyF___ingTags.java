package builderb0y.bigglobe.mixins;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry.PendingTags;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagEntry.Lookup;
import net.minecraft.tags.TagLoader;
import net.minecraft.tags.TagLoader.EntryWithSource;
import net.minecraft.tags.TagLoader.SortingEntry;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Either;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry;
import builderb0y.bigglobe.config.BigGlobeConfig;
import builderb0y.bigglobe.util.BetterScopedValue;

/**
big thanks to Blodhgarm for making the "Load My F***ing Tags" mod.
without it, I would've had to figure out how tag loading logic worked myself.
but with it, I can just copy what they already had (it's CC0, so I'm allowed to do this).

anyway, Load My F***ing Tags is a great mod, but I am now extending it to add another option:
instead of just loading the tag (ignore errors) or not loading the tag (also ignoring errors),
I am adding the option to *not* ignore errors, making data pack validation fail if errors are encountered.
this behavior is configurable, and useful for data pack developers.
*/
@Mixin(TagLoader.class)
public class TagGroupLoader_DontLoadMyF___ingTags {

	@Unique
	private static final BetterScopedValue<Identifier> CURRENT_TAG_ID = new BetterScopedValue<>();

	@WrapMethod(method = "lambda$build$1")
	private void bigglobe_storeCurrentTagId(
		Lookup valueGetter,
		Map map,
		Identifier id,
		SortingEntry dependencies,
		Operation<Void> original
	) {
		CURRENT_TAG_ID.run(id, () -> original.call(valueGetter, map, id, dependencies));
	}

	@Inject(method = "tryBuildTag", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
	private <T> void bigglobe_checkForErrors(
		Lookup<T> valueGetter,
		List<EntryWithSource> entries,
		CallbackInfoReturnable<Either<List<EntryWithSource>, List<T>>> callback,
		@Local(ordinal = 1) List<EntryWithSource> errors
	) {
		if (!errors.isEmpty() && builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry.Loading.addInvalidTag(CURRENT_TAG_ID.currentValue(), errors)) {
			errors.clear();
		}
	}

	@Inject(method = "loadTagsForExistingRegistries(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/core/RegistryAccess;)Ljava/util/List;", at = @At("HEAD"))
	private static void bigglobe_prepareForReload(
		ResourceManager resourceManager,
		RegistryAccess registryManager,
		CallbackInfoReturnable<List<PendingTags<?>>> callback
	) {
		builderb0y.bigglobe.columns.scripted2.ColumnEntryRegistry.Loading.invalidTagHandling = BigGlobeConfig.INSTANCE.get().dataPackDebugging.invalidTagHandling;
		ColumnEntryRegistry.Loading.invalidTags = new HashMap<>();
	}
}