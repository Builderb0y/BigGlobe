package builderb0y.bigglobe.scripting.environments;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.util.Symmetry;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.CoordinateConsumer;
import builderb0y.bigglobe.util.coordinators.Coordinator;
import builderb0y.bigglobe.versions.BlockEntityVersions;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.environments.ScriptEnvironment.GetMethodMode;
import builderb0y.scripting.parsing.ExpressionParser;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class CoordinatorScriptEnvironment {

	public static final MethodInfo
		ALL_OF = MethodInfo.findMethod(Coordinator.class, "combine", Coordinator.class, Coordinator[].class),
		TRANSLATE = MethodInfo.findMethod(Coordinator.class, "translate", Coordinator.class, int.class, int.class, int.class),
		MULTI_TRANSLATE = MethodInfo.findMethod(Coordinator.class, "multiTranslate", Coordinator.class, int[].class),
		SYMMETRIFY_1 = MethodInfo.findMethod(Coordinator.class, "symmetric", Coordinator.class, Symmetry.class),
		SYMMETRIFY_2 = MethodInfo.findMethod(Coordinator.class, "symmetric", Coordinator.class, Symmetry.class, Symmetry.class),
		SYMMETRIFY_4 = MethodInfo.findMethod(Coordinator.class, "symmetric", Coordinator.class, Symmetry.class, Symmetry.class, Symmetry.class, Symmetry.class),
		SYMMETRIFY_VARARGS = MethodInfo.findMethod(Coordinator.class, "symmetric", Coordinator.class, Symmetry[].class);

	public static final MutableScriptEnvironment BASE = (
		new MutableScriptEnvironment()
		.addType("Coordinator", Coordinator.class)
		.addQualifiedFunction(
			type(Coordinator.class),
			new FunctionHandler.Named(
				"allOf",
				"Coordinator.allOf(Coordinator... coordinators)",
				null,
				(ExpressionParser parser, String name, InsnTree... arguments) -> {
					InsnTree[] castArguments = ScriptEnvironment.castArguments(parser, "allOf", types(Coordinator.class, arguments.length), CastMode.IMPLICIT_NULL, arguments);
					if (castArguments == null) return null;
					InsnTree array = newArrayWithContents(parser, type(Coordinator[].class), castArguments);
					return new CastResult(invokeStatic(ALL_OF, array), castArguments != arguments);
				}
			)
		)
		.addMethodInvokeSpecific(Coordinator.class, "setBlockState", void.class, int.class, int.class, int.class, BlockState.class)
		.addMethodInvokeSpecific(Coordinator.class, "setBlockStateCuboid", void.class, int.class, int.class, int.class, int.class, int.class, int.class, BlockState.class)
		.addMethodInvokeSpecific(Coordinator.class, "setBlockStateLine", void.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, BlockState.class)
		.addMethodInvokeStatics(CoordinatorScriptEnvironment.class, "setBlockData", "setBlockDataLine", "setBlockDataCuboid", "mergeBlockData", "mergeBlockDataLine", "mergeBlockDataCuboid")
		.addMethod(
			new MethodHandler.Named(
				type(Coordinator.class),
				"translate",
				"translate(int... offsets ;(number of offsets must be divisible by 3))",
				null,
				(ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) -> {
					if (arguments.length % 3 != 0) return null;
					InsnTree[] offsets = ScriptEnvironment.castArguments(parser, "translate", types("I".repeat(arguments.length)), CastMode.IMPLICIT_NULL, arguments);
					if (offsets == null) return null;
					if (offsets.length == 3) {
						return new CastResult(invokeInstance(receiver, TRANSLATE, offsets), offsets != arguments);
					}
					else {
						InsnTree array = newArrayWithContents(parser, type(int[].class), offsets);
						return new CastResult(invokeInstance(receiver, MULTI_TRANSLATE, array), offsets != arguments);
					}
				}
			)
		)
		.addMethod(
			new MethodHandler.Named(
				type(Coordinator.class),
				"symmetrify",
				"symmetrify(Symmetry...)",
				null,
				(ExpressionParser parser, InsnTree receiver, String name, GetMethodMode mode, InsnTree... arguments) -> {
					InsnTree[] symmetries = ScriptEnvironment.castArguments(parser, "symmetrify", types(Symmetry.class, arguments.length), CastMode.IMPLICIT_NULL, arguments);
					if (symmetries == null) return null;
					return new CastResult(
						switch (symmetries.length) {
							case 1 -> invokeInstance(receiver, SYMMETRIFY_1, symmetries);
							case 2 -> invokeInstance(receiver, SYMMETRIFY_2, symmetries);
							case 4 -> invokeInstance(receiver, SYMMETRIFY_4, symmetries);
							default -> invokeInstance(receiver, SYMMETRIFY_VARARGS, newArrayWithContents(parser, type(Symmetry[].class), symmetries));
						},
						symmetries != arguments
					);
				}
			)
		)
		.addMethod(Handlers.methodBuilder(Coordinator.class, "rotate1x").addReceiverArgument(Coordinator.class).addNestedArgument(Handlers.methodBuilder(Directions.class, "scriptRotation").addRequiredArgument(int.class)).buildMethod())
		.addMethodInvokeSpecific(Coordinator.class, "rotate2x180", Coordinator.class)
		.addMethodInvokeSpecific(Coordinator.class, "rotate4x90", Coordinator.class)
		.addMethodInvokeSpecific(Coordinator.class, "flip1X", Coordinator.class)
		.addMethodInvokeSpecific(Coordinator.class, "flip1Z", Coordinator.class)
		.addMethodInvokeSpecific(Coordinator.class, "flip2X", Coordinator.class)
		.addMethodInvokeSpecific(Coordinator.class, "flip2Z", Coordinator.class)
		.addMethodInvokeSpecific(Coordinator.class, "flip4XZ", Coordinator.class)
		.addMethodInvokeSpecific(Coordinator.class, "stack", Coordinator.class, int.class, int.class, int.class, int.class)
		.addMethodInvokeSpecific(Coordinator.class, "inBox", Coordinator.class, int.class, int.class, int.class, int.class, int.class, int.class)
		.addMethodMultiInvokeStatic(CoordinatorScriptEnvironment.class, "summon")
	);

	public static Consumer<MutableScriptEnvironment> create(InsnTree loadWorld) {
		return (MutableScriptEnvironment environment) -> {
			environment.addAll(BASE).addQualifiedFunction(type(Coordinator.class), Handlers.methodBuilder(WorldWrapper.class, "coordinator").exposedName("new").addImplicitArgument(loadWorld).buildFunction());
		};
	}

	public static void setBlockData(Coordinator coordinator, int x, int y, int z, CompoundTag data) {
		coordinator.getBlockEntity(x, y, z, setter(data));
	}

	public static void setBlockDataLine(Coordinator coordinator, int x, int y, int z, int dx, int dy, int dz, int length, CompoundTag data) {
		coordinator.getBlockEntityLine(x, y, z, dx, dy, dz, length, setter(data));
	}

	public static void setBlockDataCuboid(Coordinator coordinator, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CompoundTag data) {
		coordinator.getBlockEntityCuboid(minX, minY, minZ, maxX, maxY, maxZ, setter(data));
	}

	public static void mergeBlockData(Coordinator coordinator, int x, int y, int z, CompoundTag data) {
		coordinator.getBlockEntity(x, y, z, merger(data));
	}

	public static void mergeBlockDataLine(Coordinator coordinator, int x, int y, int z, int dx, int dy, int dz, int length, CompoundTag data) {
		coordinator.getBlockEntityLine(x, y, z, dx, dy, dz, length, merger(data));
	}

	public static void mergeBlockDataCuboid(Coordinator coordinator, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, CompoundTag data) {
		coordinator.getBlockEntityCuboid(minX, minY, minZ, maxX, maxY, maxZ, merger(data));
	}

	public static CoordinateConsumer<BlockEntity> setter(CompoundTag data) {
		return (BlockPos.MutableBlockPos pos, BlockEntity blockEntity) -> {
			BlockEntityVersions.readFromNbt(blockEntity, data);
			blockEntity.setChanged();
		};
	}

	public static CoordinateConsumer<BlockEntity> merger(CompoundTag data) {
		return (BlockPos.MutableBlockPos pos, BlockEntity blockEntity) -> {
			CompoundTag oldData = BlockEntityVersions.writeToNbt(blockEntity);
			CompoundTag newData = oldData.copy().merge(data);
			if (!oldData.equals(newData)) {
				BlockEntityVersions.readFromNbt(blockEntity, newData);
				blockEntity.setChanged();
			}
		};
	}

	public static void summon(Coordinator coordinator, double x, double y, double z, String entityTypeName) {
		Identifier identifier = IdentifierVersions.create(entityTypeName);
		if (BuiltInRegistries.ENTITY_TYPE.containsKey(identifier)) {
			EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(identifier);
			double offsetX = BigGlobeMath.modulus_BP(x, 1.0D);
			double offsetY = BigGlobeMath.modulus_BP(y, 1.0D);
			double offsetZ = BigGlobeMath.modulus_BP(z, 1.0D);
			coordinator.addEntity(
				BigGlobeMath.floorI(x), BigGlobeMath.floorI(y), BigGlobeMath.floorI(z), (BlockPos.MutableBlockPos pos, ServerLevel world) -> {
					double newX = pos.getX() + offsetX;
					double newY = pos.getY() + offsetY;
					double newZ = pos.getZ() + offsetZ;
					Entity entity = entityType.create(world, EntitySpawnReason.CHUNK_GENERATION);
					if (entity != null) {
						entity.snapTo(newX, newY, newZ, entity.getYRot(), entity.getXRot());
						return entity;
					}
					else {
						throw new IllegalArgumentException("Entity type " + entityTypeName + " is not enabled in this world's feature flags.");
					}
				}
			);
		}
		else {
			throw new IllegalArgumentException("Unknown entity type: " + entityTypeName);
		}
	}

	public static void summon(Coordinator coordinator, double x, double y, double z, String entityTypeName, CompoundTag data) {
		Identifier identifier = IdentifierVersions.create(entityTypeName);
		if (BuiltInRegistries.ENTITY_TYPE.containsKey(identifier)) {
			double offsetX = BigGlobeMath.modulus_BP(x, 1.0D);
			double offsetY = BigGlobeMath.modulus_BP(y, 1.0D);
			double offsetZ = BigGlobeMath.modulus_BP(z, 1.0D);
			CompoundTag copy = data.copy();
			copy.putString("id", entityTypeName);
			coordinator.addEntity(
				BigGlobeMath.floorI(x), BigGlobeMath.floorI(y), BigGlobeMath.floorI(z), (BlockPos.MutableBlockPos pos, ServerLevel world) -> {
					double newX = pos.getX() + offsetX;
					double newY = pos.getY() + offsetY;
					double newZ = pos.getZ() + offsetZ;
					return EntityType.loadEntityRecursive(
						copy, world, EntitySpawnReason.CHUNK_GENERATION, (Entity entity) -> {
							entity.snapTo(newX, newY, newZ, entity.getYRot(), entity.getXRot());
							return entity;
						}
					);
				}
			);
		}
		else {
			throw new IllegalArgumentException("Unknown entity type: " + entityTypeName);
		}
	}
}