package me.sorihiko.dango.legacy

import cpw.mods.fml.client.registry.ClientRegistry
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.settings.KeyBinding
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL14

internal val legacyRenderManager get() = RenderManager.instance
internal fun legacyResolution() = ScaledResolution(mc, mc.displayWidth, mc.displayHeight)
internal fun registerLegacyKeyBinding(binding: KeyBinding) = ClientRegistry.registerKeyBinding(binding)

internal fun drawLegacyString(text: String, x: Float, y: Float, color: Int, shadow: Boolean) {
    // The 1.7 font renderer accepts integer positions. Preserve fractional lyric/HUD placement.
    GL11.glPushMatrix()
    try {
        GL11.glTranslatef(x, y, 0f)
        legacyFontRenderer.drawString(text, 0, 0, color, shadow)
    } finally {
        GL11.glPopMatrix()
    }
}

/** Vanilla 1.7 renders directly through LWJGL and has no GlStateManager cache. */
internal object GlStateManager {
    fun enableTexture2D() = GL11.glEnable(GL11.GL_TEXTURE_2D)
    fun disableTexture2D() = GL11.glDisable(GL11.GL_TEXTURE_2D)
    fun enableBlend() = GL11.glEnable(GL11.GL_BLEND)
    fun disableBlend() = GL11.glDisable(GL11.GL_BLEND)
    fun enableDepth() = GL11.glEnable(GL11.GL_DEPTH_TEST)
    fun disableDepth() = GL11.glDisable(GL11.GL_DEPTH_TEST)
    fun enableCull() = GL11.glEnable(GL11.GL_CULL_FACE)
    fun disableCull() = GL11.glDisable(GL11.GL_CULL_FACE)
    fun enableAlpha() = GL11.glEnable(GL11.GL_ALPHA_TEST)
    fun disableAlpha() = GL11.glDisable(GL11.GL_ALPHA_TEST)
    fun enableFog() = GL11.glEnable(GL11.GL_FOG)
    fun disableFog() = GL11.glDisable(GL11.GL_FOG)
    fun enableLighting() = GL11.glEnable(GL11.GL_LIGHTING)
    fun disableLighting() = GL11.glDisable(GL11.GL_LIGHTING)
    fun setActiveTexture(unit: Int) = GL13.glActiveTexture(unit)
    fun bindTexture(texture: Int) = GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture)
    fun color(r: Float, g: Float, b: Float, a: Float) = GL11.glColor4f(r, g, b, a)
    fun depthMask(enabled: Boolean) = GL11.glDepthMask(enabled)
    fun shadeModel(mode: Int) = GL11.glShadeModel(mode)
    fun tryBlendFuncSeparate(src: Int, dst: Int, srcAlpha: Int, dstAlpha: Int) =
        GL14.glBlendFuncSeparate(src, dst, srcAlpha, dstAlpha)
}
