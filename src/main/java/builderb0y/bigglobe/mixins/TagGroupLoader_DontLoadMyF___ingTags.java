package builderb0y.bigglobe.mixins;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
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
@Mixin(TagLoader.class)
public class TagGroupLoader_DontLoadMyF___ingTags {

	@Unique
	private static final ThreadLocal<ResourceLocation> CURRENT_TAG_ID = new ThreadLocal<>();

	@WrapMethod(method = {"method_51476", "lambda$build$6"})
	private void bigglobe_storeCurrentTagId(
		Lookup valueGetter,
		Map map,
		ResourceLocation id,
		SortingEntry dependencies,
		Operation<Void> original
	) {
		ResourceLocation old = CURRENT_TAG_ID.get();
		CURRENT_TAG_ID.set(id);
		try {
			original.call(valueGetter, map, id, dependencies);
		}
		finally {
			CURRENT_TAG_ID.set(old);
		}
	}

	@Inject(method = "build(Lnet/minecraft/tags/TagEntry$Lookup;Ljava/util/List;)Lcom/mojang/datafixers/util/Either;", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
	private <T> void bigglobe_checkForErrors(
		Lookup<T> valueGetter,
		List<EntryWithSource> entries,
		CallbackInfoReturnable<Either<Collection<EntryWithSource>, Collection<T>>> callback,
		@Local(ordinal = 1) List<EntryWithSource> errors
	) {
		if (!errors.isEmpty() && ColumnEntryRegistry.Loading.addInvalidTag(CURRENT_TAG_ID.get(), errors)) {
			errors.clear();
		}
	}

	@Inject(method = "load", at = @At("HEAD"))
	private void bigglobe_prepareForReload(ResourceManager manager, CallbackInfoReturnable<Map<ResourceLocation, List<EntryWithSource>>> callback) {
		ColumnEntryRegistry.Loading.invalidTagHandling = BigGlobeConfig.INSTANCE.get().dataPackDebugging.invalidTagHandling;
		ColumnEntryRegistry.Loading.invalidTags = new HashMap<>();
	}
}
