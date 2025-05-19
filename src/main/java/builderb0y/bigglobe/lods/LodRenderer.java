package builderb0y.bigglobe.lods;

import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import org.joml.Vector4f;
import org.lwjgl.opengl.*;

import net.minecraft.client.render.VertexConsumerProvider;

@Environment(EnvType.CLIENT)
public interface LodRenderer extends SafeCloseable {

	public static class FogParams {

		public float red, green, blue, farPlaneDistance;
	}

	/**
	called before the draw calls are issued.
	override this method to capture the current
	GL state and use the shader program.

	if this renderer uses a VAO or element buffer,
	this would be the place to bind those too.

	the returned SafeCloseable is expected to un-bind resources
	and restore the captured GL state when closed.
	it will be closed on the same thread which calls bind().
	*/
	public abstract SafeCloseable bind(
		WorldRenderContext context,
		FogParams fog,
		boolean translucent
	);

	/**
	called to draw a mesh. the provided token will be one which was
	previously returned by {@link #finishMeshing()}.
	the position in player space is vertexPosition * scale + modelOffset.

	the implementation may choose to draw these vertices however it wants,
	whether that implies calling {@link GL11C#glDrawArrays(int, int, int)},
	{@link GL11C#glDrawElements}, or appending the request to an internal
	buffer to be multi-drawn when the return value of
	{@link #bind(WorldRenderContext, FogParams, boolean)} is closed.

	{@link #bind(WorldRenderContext, FogParams, boolean)} will always be called before this method.
	if this renderer uses a VAO, it should be bound in that method, not this one.
	*/
	public abstract void draw(
		SafeCloseable token,
		float modelOffsetX,
		float modelOffsetY,
		float modelOffsetZ,
		float scale
	);

	/**
	returns a NEW VertexConsumerProvider that
	will be used to render a new mesh with.
	this method should NOT return a shared instance.
	this method may be called more than once before
	{@link #finishMeshing()} is called.

	the provider may be used on a different
	thread than the one that calls this method,
	but the provider will NOT be used by more
	than one thread at a time (unless you ignore the
	above warning and return a shared instance anyway).
	*/
	public abstract VertexConsumerProvider beginMeshing();

	/**
	called when new meshes have been finished.
	the provided MeshUploader will itself accept
	VertexConsumerProvider's which were previously
	returned by {@link #beginMeshing()}.

	the returned uploader will be closed once all finished
	meshes for the current frame have been uploaded.
	*/
	public abstract MeshUploader finishMeshing();

	public static interface MeshUploader extends SafeCloseable {

		/**
		@param provider a VertexConsumerProvider which was
		previously returned by {@link #beginMeshing()}.

		@return a token that can be used to draw the mesh.
		this token will be passed into
		{@link #draw(SafeCloseable, float, float, float, float)}
		when it comes time to actually draw the mesh.
		the token will be closed when it is no longer needed.
		typically, this will happen when the player gets too far away from the mesh.
		*/
		public SafeCloseable upload(VertexConsumerProvider provider) throws OutOfVramException;
	}

	/**
	when the MeshUploader throws an OutOfVramException,
	the LOD quality is first reduced,
	then uploaded meshes are closed when they are too high quality,
	and then this method is called.
	implementors can use this to do any other cleanup
	action after many meshes are closed in bulk.
	*/
	public default void oom() {}

	/**
	called when mesh has been finished
	and, if applicable, uploaded.
	if the provider stores any extra memory,
	then this method should free that memory.
	the provided provider will be one which was
	previously returned by {@link #beginMeshing()}.
	the provider may or may not have been used
	or appended to when this method is called.

	this method will be called on the same
	thread which called {@link #beginMeshing()}.
	*/
	public abstract void endMeshing(VertexConsumerProvider provider);

	/** self-explanatory. default impl does nothing. */
	public default void appendTextToF3Menu(List<String> lines) {}

	/**
	called when the LOD system is shutting down.
	this can happen for several reasons, including:
	* the player teleported to a new dimension.
	* the player quit to the main menu.
	* the player pressed F3 + A.
	* the player changed a config option, which reloads LODs.

	override this method to delete any GL
	resources associated with the shader.
	if the shader is managed externally and the
	implementation of this interface is just a proxy,
	it is acceptable to do nothing in this method.
	*/
	@Override
	public default void close() {}
}