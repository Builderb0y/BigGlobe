package builderb0y.bigglobe.scripting;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

import builderb0y.bigglobe.scripting.wrappers.WorldWrapper;
import builderb0y.bigglobe.util.Directions;
import builderb0y.bigglobe.util.coordinators.CoordinateFunctions.CoordinateBiConsumer;
import builderb0y.bigglobe.util.coordinators.Coordinator;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.CastResult;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler.Named;
import builderb0y.scripting.environments.ScriptEnvironment;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class CoordinatorScriptEnvironment {

	public static final MethodInfo
		ALL_OF = MethodInfo.findMethod(Coordinator.class, "combine", Coordinator.class, Coordinator[].class),
		TRANSLATE = MethodInfo.findMethod(Coordinator.class, "translate", Coordinator.class, int.class, int.class, int.class),
		MULTI_TRANSLATE = MethodInfo.findMethod(Coordinator.class, "multiTranslate", Coordinator.class, int[].class);

	public static MutableScriptEnvironment create(InsnTree loadWorld) {
		return (
			new MutableScriptEnvironment()
			.addType("Coordinator", Coordinator.class)
			.addQualifiedFunction(type(Coordinator.class), "new", Handlers.builder(WorldWrapper.class, "coordinator").addImplicitArgument(loadWorld).buildFunction())
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
				new Named(
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
		);
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

	public static CoordinateBiConsumer<BlockEntity> setter(NbtCompound data) {
		return (BlockPos.Mutable pos, BlockEntity blockEntity) -> {
			blockEntity.readNbt(data);
			blockEntity.markDirty();
		};
	}

	public static CoordinateBiConsumer<BlockEntity> merger(NbtCompound data) {
		return (BlockPos.Mutable pos, BlockEntity blockEntity) -> {
			NbtCompound oldData = blockEntity.createNbtWithIdentifyingData();
			NbtCompound newData = oldData.copy().copyFrom(data);
			if (!oldData.equals(newData)) {
				blockEntity.readNbt(newData);
				blockEntity.markDirty();
			}
		};
	}
}