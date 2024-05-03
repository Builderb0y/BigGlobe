package builderb0y.bigglobe.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.particle.WaterSuspendParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registry;

import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.BigGlobeAutoCodec;
import builderb0y.bigglobe.versions.RegistryVersions;

#if MC_VERSION >= MC_1_20_5
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
#endif

public class SporeParticles {

	public static void init() {
		Registry.register(RegistryVersions.particleType(), BigGlobeMod.modID("spore"), Type.INSTANCE);
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		ParticleFactoryRegistry.getInstance().register(Type.INSTANCE, ClientFactory::new);
	}

	public static class Effect implements ParticleEffect {

		#if MC_VERSION >= MC_1_20_5
		public static final MapCodec<Effect> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(Effect.class);
		#else
		public static final Codec<Effect> CODEC = BigGlobeAutoCodec.AUTO_CODEC.createDFUMapCodec(Effect.class).codec();
		#endif

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

		public void write(PacketByteBuf buffer) {
			buffer.writeByte(this.red);
			buffer.writeByte(this.green);
			buffer.writeByte(this.blue);
		}

		public static Effect read(PacketByteBuf buffer) {
			return new Effect(buffer.readUnsignedByte(), buffer.readUnsignedByte(), buffer.readUnsignedByte());
		}

		@Override
		public String #if MC_VERSION >= MC_1_20_5 toString #else asString #endif() {
			return "SporeParticles$Effect: { red: " + this.red + ", green: " + this.green + ", blue: " + this.blue + " }";
		}
	}

	public static class Type extends ParticleType<Effect> {

		public static final Type INSTANCE = new Type();

		public Type() {
			super(false #if MC_VERSION < MC_1_20_5 , ServerFactory.INSTANCE #endif);
		}

		#if MC_VERSION >= MC_1_20_5

			@Override
			public MapCodec<Effect> getCodec() {
				return Effect.CODEC;
			}

			@Override
			public PacketCodec<? super RegistryByteBuf, Effect> getPacketCodec() {
				return PacketCodec.of(Effect::write, Effect::read);
			}
		#else

			@Override
			public Codec<Effect> getCodec() {
				return Effect.CODEC;
			}
		#endif
	}

	@Environment(EnvType.CLIENT)
	public static class ClientFactory implements ParticleFactory<Effect> {

		public SpriteProvider spriteProvider;

		public ClientFactory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		@Nullable
		@Override
		public Particle createParticle(Effect parameters, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
			WaterSuspendParticle particle = new WaterSuspendParticle(world, this.spriteProvider, x, y, z, velocityX, velocityY, velocityZ);
			particle.setColor(parameters.red / 255.0F, parameters.green / 255.0F, parameters.blue / 255.0F);
			return particle;
		}
	}

	#if MC_VERSION < MC_1_20_5

		@SuppressWarnings("deprecation")
		public static class ServerFactory implements ParticleEffect.Factory<Effect> {

			public static final ServerFactory INSTANCE = new ServerFactory();

			@Override
			public Effect read(ParticleType<Effect> type, StringReader reader) throws CommandSyntaxException {
				return new Effect(nextComponent(reader), nextComponent(reader), nextComponent(reader));
			}

			public static int nextComponent(StringReader reader) throws CommandSyntaxException {
				reader.expect(' ');
				int value = reader.readInt();
				if (value < 0) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooLow().createWithContext(reader, value, 0);
				if (value > 255) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooHigh().createWithContext(reader, value, 255);
				return value;
			}

			@Override
			public Effect read(ParticleType<Effect> type, PacketByteBuf buffer) {
				return new Effect(buffer.readUnsignedByte(), buffer.readUnsignedByte(), buffer.readUnsignedByte());
			}
		}
	#endif
}