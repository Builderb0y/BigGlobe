package builderb0y.bigglobe.mixins;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;

@Mixin(PostEffectProcessor.class)
public interface PostEffectProcessor_PassesAccess {

	@Accessor("passes")
	public abstract List<PostEffectPass> bigglobe_getPasses();
}