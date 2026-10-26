package builderb0y.bigglobe.particles;

import com.mojang.serialization.MapCodec;
import builderb0y.bigglobe.platform.fabric.api.EnvType;
import builderb0y.bigglobe.platform.fabric.api.Environment;
import builderb0y.bigglobe.platform.fabric.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SuspendedParticle;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;

public class SporeParticles {

	public static void init() {
		Registry.register(BuiltInRegistries.PARTICLE_TYPE, BigGlobeMod.modID("spore"), Type.INSTANCE);
	}

	@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
	public static void initClient() {
		ParticleFactoryRegistry.getInstance().register(Type.INSTANCE, ClientFactory::new);
	}

	public static class Effect implements ParticleOptions {

		public static final MapCodec<Effect> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(Effect.class);

		public final int red, green, blue;

		public Effect(int red, int green, int blue) {
			this.red = red;
			this.green = green;
			this.blue = blue;
		}

		@Override
		public ParticleType<?> getType() {
			return Type.INSTANCE;
		}

		public void write(FriendlyByteBuf buffer) {
			buffer.writeByte(this.red);
			buffer.writeByte(this.green);
			buffer.writeByte(this.blue);
		}

		public static Effect read(FriendlyByteBuf buffer) {
			return new Effect(buffer.readUnsignedByte(), buffer.readUnsignedByte(), buffer.readUnsignedByte());
		}

		@Override
		public String toString() {
			return "SporeParticles$Effect: { red: " + this.red + ", green: " + this.green + ", blue: " + this.blue + " }";
		}
	}

	public static class Type extends ParticleType<Effect> {

		public static final Type INSTANCE = new Type();

		public Type() {
			super(false);
		}

		@Override
		public MapCodec<Effect> codec() {
			return Effect.CODEC;
		}

		@Override
		public StreamCodec<? super RegistryFriendlyByteBuf, Effect> streamCodec() {
			return StreamCodec.ofMember(Effect::write, Effect::read);
		}
	}

	@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
	public static class SporeParticle extends SuspendedParticle {
		public SporeParticle(ClientLevel level, SpriteSet sprites, double x, double y, double z, double vx, double vy, double vz) {
			super(level, sprites, x, y, z);
			this.xd = vx;
			this.yd = vy;
			this.zd = vz;
		}
	}

	@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
	public static class ClientFactory implements ParticleProvider<Effect> {

		public SpriteSet spriteProvider;

		public ClientFactory(SpriteSet spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		@Override
		public @Nullable Particle createParticle(Effect parameters, ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
			SporeParticle particle = new SporeParticle(world, this.spriteProvider, x, y, z, velocityX, velocityY, velocityZ);
			particle.setColor(parameters.red / 255.0F, parameters.green / 255.0F, parameters.blue / 255.0F);
			return particle;
		}
	}
}
