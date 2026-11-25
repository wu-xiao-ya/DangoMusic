package me.sorihiko.dango.legacy

import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.client.registry.ClientRegistry

internal typealias GlStateManager = net.minecraft.client.renderer.GlStateManager
internal val legacyRenderManager get() = mc.renderManager
internal fun legacyResolution() = ScaledResolution(mc)
internal fun drawLegacyString(text: String, x: Float, y: Float, color: Int, shadow: Boolean) {
    legacyFontRenderer.drawString(text, x, y, color, shadow)
}
internal fun registerLegacyKeyBinding(binding: KeyBinding) = ClientRegistry.registerKeyBinding(binding)
