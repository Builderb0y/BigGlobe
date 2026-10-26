package builderb0y.bigglobe;

import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import builderb0y.bigglobe.platform.fabric.api.EnvType;
import builderb0y.bigglobe.platform.fabric.api.Environment;
import builderb0y.bigglobe.platform.fabric.fabric.api.client.networking.v1.ClientPlayNetworking;
import builderb0y.bigglobe.platform.fabric.fabric.api.client.rendering.v1.ColorProviderRegistry;
import builderb0y.bigglobe.platform.fabric.fabric.api.client.rendering.v1.WorldRenderContext;
import builderb0y.bigglobe.platform.fabric.fabric.api.client.rendering.v1.WorldRenderEvents;
import builderb0y.bigglobe.platform.fabric.fabric.api.command.v2.CommandRegistrationCallback;
import builderb0y.bigglobe.platform.fabric.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import builderb0y.bigglobe.platform.fabric.fabric.api.event.player.AttackEntityCallback;
import builderb0y.bigglobe.platform.fabric.fabric.api.event.player.UseEntityCallback;
import builderb0y.bigglobe.platform.fabric.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import builderb0y.bigglobe.platform.fabric.fabric.api.itemgroup.v1.ItemGroupEvents;
import builderb0y.bigglobe.platform.fabric.fabric.api.networking.v1.PacketSender;
import builderb0y.bigglobe.platform.fabric.fabric.api.networking.v1.ServerPlayNetworking;
import builderb0y.bigglobe.platform.fabric.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import builderb0y.bigglobe.platform.fabric.fabric.api.registry.FuelRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.joml.Matrix4f;

import builderb0y.bigglobe.networking.base.BigGlobePayload;

public class BigGlobeNeoForgeEvents {

	public static void register(IEventBus modEventBus) {
		modEventBus.addListener(BigGlobeNeoForgeEvents::registerPayloads);
		modEventBus.addListener(BigGlobeNeoForgeEvents::onBuildCreativeTabs);

		if (FMLEnvironment.dist == Dist.CLIENT) {
			registerClientModBus(modEventBus);
		}

		NeoForge.EVENT_BUS.addListener(BigGlobeNeoForgeEvents::registerBrewing);
		NeoForge.EVENT_BUS.addListener(BigGlobeNeoForgeEvents::registerCommands);
		NeoForge.EVENT_BUS.addListener(BigGlobeNeoForgeEvents::serverStarting);
		NeoForge.EVENT_BUS.addListener(BigGlobeNeoForgeEvents::serverStopped);
		NeoForge.EVENT_BUS.addListener(BigGlobeNeoForgeEvents::onFuelBurnTime);
		NeoForge.EVENT_BUS.addListener(BigGlobeNeoForgeEvents::onEntityInteract);
		NeoForge.EVENT_BUS.addListener(BigGlobeNeoForgeEvents::onAttackEntity);

		if (FMLEnvironment.dist == Dist.CLIENT) {
			registerClientForgeBus();
		}
	}

	@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
	private static void registerClientModBus(IEventBus modEventBus) {
		modEventBus.addListener(BigGlobeNeoForgeEvents::registerBlockColors);
		modEventBus.addListener(BigGlobeNeoForgeEvents::registerItemColors);
		modEventBus.addListener(BigGlobeNeoForgeEvents::registerParticleProviders);
	}

	@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
	private static void registerClientForgeBus() {
		NeoForge.EVENT_BUS.addListener(BigGlobeNeoForgeEvents::onRenderLevelStage);
	}

	public static void registerPayloads(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar(BigGlobeMod.MODID);
		registrar.playBidirectional(
			BigGlobePayload.ID,
			BigGlobePayload.CODEC,
			new net.neoforged.neoforge.network.handling.DirectionalPayloadHandler<>(
				(payload, context) -> {
					ClientPlayNetworking.PlayPayloadHandler<BigGlobePayload> handler = 
						(ClientPlayNetworking.PlayPayloadHandler<BigGlobePayload>)ClientPlayNetworking.RECEIVERS.get(BigGlobePayload.ID);
					if (handler != null) {
						context.enqueueWork(() -> {
							handler.receive(payload, new ClientPlayNetworking.Context(Minecraft.getInstance(), new PacketSender() {}));
						});
					}
				},
				(payload, context) -> {
					ServerPlayNetworking.PlayPayloadHandler<BigGlobePayload> handler = 
						(ServerPlayNetworking.PlayPayloadHandler<BigGlobePayload>)ServerPlayNetworking.RECEIVERS.get(BigGlobePayload.ID);
					if (handler != null) {
						context.enqueueWork(() -> {
							handler.receive(payload, new ServerPlayNetworking.Context((ServerPlayer)context.player(), new PacketSender() {}));
						});
					}
				}
			)
		);
	}

	public static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
		ItemGroupEvents.ModifyEntries entriesHandler = ItemGroupEvents.getEvents().get(event.getTabKey());
		if (entriesHandler != null) {
			FabricItemGroupEntries entries = new FabricItemGroupEntries();
			for (Consumer<FabricItemGroupEntries> listener : entriesHandler.getListeners()) {
				listener.accept(entries);
			}
			for (ItemStack item : entries.getEntries()) {
				event.accept(item);
			}
		}
	}

	public static void registerBrewing(RegisterBrewingRecipesEvent event) {
		for (Consumer<PotionBrewing.Builder> listener : FabricBrewingRecipeRegistryBuilder.BUILD.getListeners()) {
			listener.accept(event.getBuilder());
		}
	}

	@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
	public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
		for (ColorProviderRegistry.BlockImpl.Entry entry : ColorProviderRegistry.BlockImpl.ENTRIES) {
			event.register(entry.provider, entry.blocks);
		}
	}

	@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
	public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		for (ColorProviderRegistry.ItemImpl.Entry entry : ColorProviderRegistry.ItemImpl.ENTRIES) {
			event.register(entry.provider, entry.items);
		}
	}

	@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
	public static void registerParticleProviders(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
		for (var entry : builderb0y.bigglobe.platform.fabric.fabric.api.client.particle.v1.ParticleFactoryRegistry.Holder.FACTORIES.entrySet()) {
			registerParticleProvider(event, entry.getKey(), entry.getValue());
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends net.minecraft.core.particles.ParticleOptions> void registerParticleProvider(
		net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event,
		net.minecraft.core.particles.ParticleType<?> type,
		builderb0y.bigglobe.platform.fabric.fabric.api.client.particle.v1.ParticleFactoryRegistry.PendingParticleFactory<?> factory
	) {
		event.registerSpriteSet((net.minecraft.core.particles.ParticleType<T>) type, ((builderb0y.bigglobe.platform.fabric.fabric.api.client.particle.v1.ParticleFactoryRegistry.PendingParticleFactory<T>) factory)::create);
	}

	public static void registerCommands(RegisterCommandsEvent event) {
		CommandRegistrationCallback.EVENT.invoker(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
	}

	public static void serverStarting(ServerStartingEvent event) {
		for (ServerLifecycleEvents.ServerStarting listener : ServerLifecycleEvents.SERVER_STARTING.getListeners()) {
			listener.onServerStarting(event.getServer());
		}
	}

	public static void serverStopped(ServerStoppedEvent event) {
		for (ServerLifecycleEvents.ServerStopped listener : ServerLifecycleEvents.SERVER_STOPPED.getListeners()) {
			listener.onServerStopped(event.getServer());
		}
	}

	@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
			WorldRenderContext context = new WorldRenderContext() {
				@Override public LevelRenderer worldRenderer() { return event.getLevelRenderer(); }
				@Override public PoseStack matrixStack() { return event.getPoseStack(); }
				@Override public Matrix4f projectionMatrix() { return event.getProjectionMatrix(); }
				@Override public Matrix4f positionMatrix() { return event.getModelViewMatrix(); }
				@Override public ClientLevel world() { return Minecraft.getInstance().level; }
				@Override public Camera camera() { return event.getCamera(); }
				@Override public GameRenderer gameRenderer() { return Minecraft.getInstance().gameRenderer; }
				@Override public float tickDelta() { return event.getPartialTick().getGameTimeDeltaPartialTick(false); }
				@Override public Frustum frustum() { return event.getFrustum(); }
			};
			for (WorldRenderEvents.AfterSetup listener : WorldRenderEvents.AFTER_SETUP.getListeners()) {
				listener.afterSetup(context);
			}
		}
	}

	public static void onFuelBurnTime(FurnaceFuelBurnTimeEvent event) {
		Integer time = FuelRegistry.INSTANCE.get(event.getItemStack().getItem());
		if (time != null) {
			event.setBurnTime(time);
		}
	}

	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		InteractionResult result = UseEntityCallback.EVENT.invoker(
			event.getEntity(),
			event.getLevel(),
			event.getHand(),
			event.getTarget(),
			null
		);
		if (result != InteractionResult.PASS) {
			event.setCanceled(true);
			event.setCancellationResult(result);
		}
	}

	public static void onAttackEntity(AttackEntityEvent event) {
		InteractionResult result = AttackEntityCallback.EVENT.invoker(
			event.getEntity(),
			event.getEntity().level(),
			InteractionHand.MAIN_HAND,
			event.getTarget(),
			null
		);
		if (result == InteractionResult.FAIL) {
			event.setCanceled(true);
		}
	}
}
