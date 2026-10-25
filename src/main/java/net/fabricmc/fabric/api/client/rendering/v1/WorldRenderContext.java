package net.fabricmc.fabric.api.client.rendering.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public interface WorldRenderContext {

	LevelRenderer worldRenderer();

	PoseStack matrixStack();

	Matrix4f projectionMatrix();

	Matrix4f positionMatrix();

	default Matrix4fc viewMatrix() {
		return positionMatrix();
	}

	ClientLevel world();

	Camera camera();

	GameRenderer gameRenderer();

	float tickDelta();

	default DeltaTracker tickCounter() {
		return new DeltaTracker() {
			@Override
			public float getGameTimeDeltaTicks() {
				return 1.0F;
			}

			@Override
			public float getGameTimeDeltaPartialTick(boolean runsNormally) {
				return tickDelta();
			}

			@Override
			public float getRealtimeDeltaTicks() {
				return 1.0F;
			}
		};
	}

	Frustum frustum();
}
