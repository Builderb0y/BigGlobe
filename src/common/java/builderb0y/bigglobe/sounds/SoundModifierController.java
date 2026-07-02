package builderb0y.bigglobe.sounds;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;

import builderb0y.autocodec.annotations.Wrapper;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.columns.scripted.ExternalEnvironmentParams;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView;
import builderb0y.bigglobe.columns.scripted.dependencies.DependencyView.SetBasedMutableDependencyView;
import builderb0y.bigglobe.scripting.ScriptCatcher;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.bigglobe.scripting.wrappers.ReadOnlyWorldWrapper;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.scripting.wrappers.entries.EntityTypeEntry;
import builderb0y.bigglobe.scripting.wrappers.entries.SoundModifierEntry;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.parsing.input.ScriptUsage;
import builderb0y.scripting.util.InfoHolder;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface SoundModifierController extends Script {

	public abstract SoundModifierEntry modifySound(ReadOnlyWorldWrapper world, SoundInstance sound, Entity listener);

	public static class EntityMethods {

		public static final Info INFO = new Info();
		public static class Info extends InfoHolder {

			public MethodInfo
				feetX,
				feetY,
				feetZ,
				eyeX,
				eyeY,
				eyeZ,
				velocityX,
				velocityY,
				velocityZ,
				boundsMinX,
				boundsMinY,
				boundsMinZ,
				boundsMaxX,
				boundsMaxY,
				boundsMaxZ,
				sizeX,
				sizeY,
				sizeZ,
				pitch,
				yaw,
				underwater,
				inWater,
				inRain,
				isWet,
				inLiquid,
				inLava,
				underlava,
				isOnFire,
				onGround;

			public void configure(MutableScriptEnvironment environment) {
				environment
				.addType("Entity", Entity.class)
				.addFieldInvokeStatic(this.feetX)
				.addFieldInvokeStatic(this.feetY)
				.addFieldInvokeStatic(this.feetZ)
				.addFieldInvokeStatic(this.eyeX)
				.addFieldInvokeStatic(this.eyeY)
				.addFieldInvokeStatic(this.eyeZ)
				.addFieldInvokeStatic(this.velocityX)
				.addFieldInvokeStatic(this.velocityY)
				.addFieldInvokeStatic(this.velocityZ)
				.addFieldInvokeStatic(this.boundsMinX)
				.addFieldInvokeStatic(this.boundsMinY)
				.addFieldInvokeStatic(this.boundsMinZ)
				.addFieldInvokeStatic(this.boundsMaxX)
				.addFieldInvokeStatic(this.boundsMaxY)
				.addFieldInvokeStatic(this.boundsMaxZ)
				.addFieldInvokeStatic(this.sizeX)
				.addFieldInvokeStatic(this.sizeY)
				.addFieldInvokeStatic(this.sizeZ)
				.addFieldInvokeStatic(this.pitch)
				.addFieldInvokeStatic(this.yaw)
				.addFieldInvokeStatic(this.underwater)
				.addFieldInvokeStatic(this.inWater)
				.addFieldInvokeStatic(this.inRain)
				.addFieldInvokeStatic(this.isWet)
				.addFieldInvokeStatic(this.inLiquid)
				.addFieldInvokeStatic(this.inLava)
				.addFieldInvokeStatic(this.underlava)
				.addFieldInvokeStatic(this.isOnFire)
				.addFieldInvokeStatic(this.onGround)
				;
			}
		}

		public static double  feetX     (Entity entity) { return entity.getX(); }
		public static double  feetY     (Entity entity) { return entity.getY(); }
		public static double  feetZ     (Entity entity) { return entity.getZ(); }
		public static double  eyeX      (Entity entity) { return entity.getX(); }
		public static double  eyeY      (Entity entity) { return entity.getEyeY(); }
		public static double  eyeZ      (Entity entity) { return entity.getZ(); }
		public static double  velocityX (Entity entity) { return entity.getDeltaMovement().x; }
		public static double  velocityY (Entity entity) { return entity.getDeltaMovement().y; }
		public static double  velocityZ (Entity entity) { return entity.getDeltaMovement().z; }
		public static double  boundsMinX(Entity entity) { return entity.getBoundingBox().minX; }
		public static double  boundsMinY(Entity entity) { return entity.getBoundingBox().minY; }
		public static double  boundsMinZ(Entity entity) { return entity.getBoundingBox().minZ; }
		public static double  boundsMaxX(Entity entity) { return entity.getBoundingBox().maxX; }
		public static double  boundsMaxY(Entity entity) { return entity.getBoundingBox().maxY; }
		public static double  boundsMaxZ(Entity entity) { return entity.getBoundingBox().maxZ; }
		public static double  sizeX     (Entity entity) { return entity.getBoundingBox().getXsize(); }
		public static double  sizeY     (Entity entity) { return entity.getBoundingBox().getYsize(); }
		public static double  sizeZ     (Entity entity) { return entity.getBoundingBox().getZsize(); }
		public static float   pitch     (Entity entity) { return entity.getYRot(); }
		public static float   yaw       (Entity entity) { return entity.getXRot(); }
		public static boolean underwater(Entity entity) { return entity.isUnderWater(); }
		public static boolean inWater   (Entity entity) { return entity.isInWater(); }
		public static boolean inRain    (Entity entity) { return entity.isInRain(); }
		public static boolean isWet     (Entity entity) { return entity.isInWaterOrRain(); }
		public static boolean inLiquid  (Entity entity) { return entity.isInLiquid(); }
		public static boolean inLava    (Entity entity) { return entity.isInLava(); }
		public static boolean underlava (Entity entity) { return entity.isEyeInFluid(FluidTags.LAVA); }
		public static boolean isOnFire  (Entity entity) { return entity.isOnFire(); }
		public static boolean onGround  (Entity entity) { return entity.onGround(); }

		public static EntityTypeEntry type(Entity entity) {
			return new EntityTypeEntry(entity.typeHolder());
		}
	}

	public static class SoundMethods {

		public static final Info INFO = new Info();
		public static class Info extends InfoHolder {

			public MethodInfo
				id,
				category,
				loops,
				relative,
				delay,
				volume,
				pitch,
				x,
				y,
				z,
				decays;

			public void configure(MutableScriptEnvironment environment) {
				environment
				.addType("Sound", SoundInstance.class)
				.addFieldInvokeStatic(this.id)
				.addFieldInvokeStatic(this.category)
				.addFieldInvokeStatic(this.loops)
				.addFieldInvokeStatic(this.relative)
				.addFieldInvokeStatic(this.delay)
				.addFieldInvokeStatic(this.volume)
				.addFieldInvokeStatic(this.pitch)
				.addFieldInvokeStatic(this.x)
				.addFieldInvokeStatic(this.y)
				.addFieldInvokeStatic(this.z)
				.addFieldInvokeStatic(this.decays)
				;
			}
		}

		public static String id(SoundInstance sound) {
			return sound.getIdentifier().toString();
		}

		public static String category(SoundInstance sound) {
			return sound.getSource().getName();
		}

		public static boolean loops(SoundInstance sound) {
			return sound.isLooping();
		}

		public static boolean relative(SoundInstance sound) {
			return sound.isRelative();
		}

		public static int delay(SoundInstance sound) {
			return sound.getDelay();
		}

		public static float volume(SoundInstance sound) {
			return sound.getVolume();
		}

		public static float pitch(SoundInstance sound) {
			return sound.getPitch();
		}

		public static double x(SoundInstance sound) { return sound.getX(); }
		public static double y(SoundInstance sound) { return sound.getY(); }
		public static double z(SoundInstance sound) { return sound.getZ(); }

		public static boolean decays(SoundInstance sound) {
			return sound.getAttenuation() != Attenuation.NONE;
		}
	}

	@Wrapper
	public static class Catcher extends ScriptCatcher<SoundModifierController> implements SoundModifierController, SetBasedMutableDependencyView {

		public static final WorldWrapper.BoundInfo WORLD = WorldWrapper.BOUND_PARAM;

		public final Set<Holder<? extends DependencyView>> dependencies = new HashSet<>();

		public Catcher(ScriptUsage usage) {
			super(usage);
			this.addAllDependencies(usage);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			InsnTree loadWorld = load("world", type(ReadOnlyWorldWrapper.class));
			this.script = (
				new TemplateScriptParser<>(SoundModifierController.class, this.usage, registry.parserFlags())
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.configure((ExpressionParser parser) -> {
					MutableScriptEnvironment environment = parser.environment.mutable();
					EntityMethods.INFO.configure(environment);
					SoundMethods.INFO.configure(environment);
					environment
					.addVariableLoad("sound", type(SoundInstance.class))
					.addVariableLoad("listener", type(Entity.class))
					.addCastConstant(SoundModifierEntry.CONSTANT_FACTORY, true)
					;
					registry.setupEnvironment(
						parser,
						new ExternalEnvironmentParams()
						.withLookup("world", loadWorld)
						.trackDependencies(this)
					);
				})
				.addImportedValue("random", ReadOnlyWorldWrapper.INFO.random(loadWorld))
				.parse(new ScriptClassLoader(registry.loader))
			);
		}

		@Override
		public SoundModifierEntry modifySound(ReadOnlyWorldWrapper world, SoundInstance sound, Entity listener) {
			try {
				return this.script.modifySound(world, sound, listener);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
				return null;
			}
		}

		@Override
		public Set<Holder<? extends DependencyView>> getDependencies() {
			return this.dependencies;
		}
	}
}