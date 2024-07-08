package builderb0y.bigglobe.scripting.environments;

import java.util.function.Consumer;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import builderb0y.bigglobe.math.BigGlobeMath;
import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.util.Symmetry;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.CoordinateConsumer;
import builderb0y.bigglobe.util.coordinators.Coordinator;
import builderb0y.bigglobe.versions.BlockEntityVersions;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.bigglobe.versions.RegistryVersions;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler;
import builderb0y.scripting.environments.ScriptEnvironment;

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
			"allOf",
			new FunctionHandler.Named(
				"Coordinator.allOf(Coordinator... coordinators)",
				(parser, name, arguments) -> {
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
			type(Coordinator.class),
			"translate",
			new MethodHandler.Named(
				"translate(int... offsets ;(number of offsets must be divisible by 3))",
				(parser, receiver, name, mode, arguments) -> {
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
			type(Coordinator.class),
			"symmetrify",
			new MethodHandler.Named(
				"symmetrify(Symmetry...)",
				(parser, receiver, name, mode, arguments) -> {
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
		.addMethod(type(Coordinator.class), "rotate1x", Handlers.builder(Coordinator.class, "rotate1x").addReceiverArgument(Coordinator.class).addNestedArgument(Handlers.builder(Directions.class, "scriptRotation").addRequiredArgument(int.class)).buildMethod())
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
			environment.addAll(BASE).addQualifiedFunction(type(Coordinator.class), "new", Handlers.builder(WorldWrapper.class, "coordinator").addImplicitArgument(loadWorld).buildFunction());
		};
	}

	public static void setBlockData(Coordinator coordinator, int x, int y, int z, NbtCompound data) {
		coordinator.getBlockEntity(x, y, z, setter(data));
	}

	public static void setBlockDataLine(Coordinator coordinator, int x, int y, int z, int dx, int dy, int dz, int length, NbtCompound data) {
		coordinator.getBlockEntityLine(x, y, z, dx, dy, dz, length, setter(data));
	}

	public static void setBlockDataCuboid(Coordinator coordinator, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, NbtCompound data) {
		coordinator.getBlockEntityCuboid(minX, minY, minZ, maxX, maxY, maxZ,setter(data));
	}

	public static void mergeBlockData(Coordinator coordinator, int x, int y, int z, NbtCompound data) {
		coordinator.getBlockEntity(x, y, z, merger(data));
	}

	public static void mergeBlockDataLine(Coordinator coordinator, int x, int y, int z, int dx, int dy, int dz, int length, NbtCompound data) {
		coordinator.getBlockEntityLine(x, y, z, dx, dy, dz, length, merger(data));
	}

	public static void mergeBlockDataCuboid(Coordinator coordinator, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, NbtCompound data) {
		coordinator.getBlockEntityCuboid(minX, minY, minZ, maxX, maxY, maxZ, merger(data));
	}

	public static CoordinateConsumer<BlockEntity> setter(NbtCompound data) {
		return (BlockPos.Mutable pos, BlockEntity blockEntity) -> {
			BlockEntityVersions.readFromNbt(blockEntity, data);
			blockEntity.markDirty();
		};
	}

	public static CoordinateConsumer<BlockEntity> merger(NbtCompound data) {
		return (BlockPos.Mutable pos, BlockEntity blockEntity) -> {
			NbtCompound oldData = BlockEntityVersions.writeToNbt(blockEntity);
			NbtCompound newData = oldData.copy().copyFrom(data);
			if (!oldData.equals(newData)) {
				BlockEntityVersions.readFromNbt(blockEntity, newData);
				blockEntity.markDirty();
			}
		};
	}

	public static void summon(Coordinator coordinator, double x, double y, double z, String entityTypeName) {
		Identifier identifier = IdentifierVersions.create(entityTypeName);
		if (RegistryVersions.entityType().containsId(identifier)) {
			EntityType<?> entityType = RegistryVersions.entityType().get(identifier);
			double offsetX = BigGlobeMath.modulus_BP(x, 1.0D);
			double offsetY = BigGlobeMath.modulus_BP(y, 1.0D);
			double offsetZ = BigGlobeMath.modulus_BP(z, 1.0D);
			coordinator.addEntity(BigGlobeMath.floorI(x), BigGlobeMath.floorI(y), BigGlobeMath.floorI(z), (pos, world) -> {
				double newX = pos.getX() + offsetX;
				double newY = pos.getY() + offsetY;
				double newZ = pos.getZ() + offsetZ;
				Entity entity = entityType.create(world);
				if (entity != null) {
					entity.refreshPositionAndAngles(newX, newY, newZ, entity.getYaw(), entity.getPitch());
					return entity;
				}
				else {
					throw new IllegalArgumentException("Entity type " + entityTypeName + " is not enabled in this world's feature flags.");
				}
			});
		}
		else {
			throw new IllegalArgumentException("Unknown entity type: " + entityTypeName);
		}
	}

	public static void summon(Coordinator coordinator, double x, double y, double z, String entityTypeName, NbtCompound data) {
		Identifier identifier = IdentifierVersions.create(entityTypeName);
		if (RegistryVersions.entityType().containsId(identifier)) {
			double offsetX = BigGlobeMath.modulus_BP(x, 1.0D);
			double offsetY = BigGlobeMath.modulus_BP(y, 1.0D);
			double offsetZ = BigGlobeMath.modulus_BP(z, 1.0D);
			NbtCompound copy = data.copy();
			copy.putString("id", entityTypeName);
			coordinator.addEntity(BigGlobeMath.floorI(x), BigGlobeMath.floorI(y), BigGlobeMath.floorI(z), (pos, world) -> {
				double newX = pos.getX() + offsetX;
				double newY = pos.getY() + offsetY;
				double newZ = pos.getZ() + offsetZ;
				return EntityType.loadEntityWithPassengers(copy, world, entity -> {
					entity.refreshPositionAndAngles(newX, newY, newZ, entity.getYaw(), entity.getPitch());
					return entity;
				});
			});
		}
		else {
			throw new IllegalArgumentException("Unknown entity type: " + entityTypeName);
		}
	}
}