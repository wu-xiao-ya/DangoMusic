package me.sorihiko.dango.forge

import me.sorihiko.dango.DangoLogger
import me.sorihiko.dango.DangoMusic
import me.sorihiko.dango.events.HudRenderEvent
import me.sorihiko.dango.events.TickEvent
import me.sorihiko.dango.events.WorldRenderEndEvent
import me.sorihiko.dango.legacy.LegacyGl
import me.sorihiko.dango.legacy.chatCopyAt
import me.sorihiko.dango.legacy.mc
import me.sorihiko.dango.legacy.suppressBackgroundMusic

import me.sorihiko.dango.music.Dance
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent as ForgeTickEvent
import java.lang.reflect.Modifier

/**
 * 把 Forge 的事件转成 mod 自己的事件：
 *   ClientTickEvent(START) → TickEvent（对应原来注入在 Minecraft.runTick 开头）
 *   RenderWorldLastEvent   → WorldRenderEndEvent（对应原来注入在 renderWorldPass 末尾、画手之前）
 *   画南瓜头遮罩之前（暗角刚画完）→ HudRenderEvent（Song card、灵动岛、Now playing，暗角压不到它们）
 */
class ForgeBridge {

    private val stellarHudCache by lazy {
        if (!Loader.isModLoaded("stellar_core")) return@lazy null
        try {
            Class.forName("github.kasuminova.stellarcore.client.hudcaching.HUDCaching")
                .getField("renderingCacheOverride")
                .takeIf { it.type == Boolean::class.javaPrimitiveType && Modifier.isStatic(it.modifiers) }
        } catch (_: ReflectiveOperationException) {
            null
        } catch (_: LinkageError) {
            null
        }
    }
    private var hudDeferred = false
    private var hudRendered = false

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onChatMouse(e: net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent.Pre) {
        if (e.gui !is net.minecraft.client.gui.GuiChat || org.lwjgl.input.Mouse.getEventButton() != 0 || !org.lwjgl.input.Mouse.getEventButtonState()) return
        val value = chatCopyAt(org.lwjgl.input.Mouse.getX(), org.lwjgl.input.Mouse.getY()) ?: return
        net.minecraft.client.gui.GuiScreen.setClipboardString(value)
        e.isCanceled = true
    }

    @SubscribeEvent
    fun onMenuDraw(e: net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent.Post) {
        if (me.sorihiko.dango.legacy.LegacyPlatform.inMainMenu) {
            me.sorihiko.dango.DangoClient.renderMainMenuNowPlaying()
        }
    }

    @SubscribeEvent
    fun onClientTick(e: ForgeTickEvent.ClientTickEvent) {
        if (e.phase != ForgeTickEvent.Phase.START) return
        try {
            LegacyGl.init()
        } catch (t: Throwable) {
            DangoLogger.error("Dango Music GL init failed: {}", t)
        }
        DangoMusic.callEvent(TickEvent)
    }

    /** 原版音乐让路：放歌时拦掉原版要开始放的背景音乐（音乐盒是 RECORDS，不受影响） */
    @SubscribeEvent
    fun onPlaySound(e: net.minecraftforge.client.event.sound.PlaySoundEvent) {
        if (me.sorihiko.dango.platform.VanillaMusicHold.active) suppressBackgroundMusic(e)
    }

    @SubscribeEvent
    fun onRenderWorldLast(e: RenderWorldLastEvent) {
        DangoMusic.callEvent(WorldRenderEndEvent(e.partialTicks))
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onOverlay(e: RenderGameOverlayEvent.Pre) {
        if (e.type == RenderGameOverlayEvent.ElementType.ALL) {
            if (hudDeferred && stellarHudCache?.getBoolean(null) != true) hudDeferred = false
            return
        }
        if (e.type != RenderGameOverlayEvent.ElementType.HELMET) return
        hudDeferred = !e.isCanceled && stellarHudCache?.getBoolean(null) == true
        if (!e.isCanceled && !hudDeferred) renderHud()
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onScreenDrawBefore(e: GuiScreenEvent.DrawScreenEvent.Pre) {
        if (e.gui === mc.currentScreen) renderDeferredHud()
    }

    @SubscribeEvent
    fun onRenderTick(e: ForgeTickEvent.RenderTickEvent) {
        if (e.phase == ForgeTickEvent.Phase.START) {
            hudRendered = false
            if (hudDeferred && (mc.world == null || !OpenGlHelper.isFramebufferEnabled())) hudDeferred = false
        } else {
            renderDeferredHud()
        }
    }

    private fun renderDeferredHud() {
        // StellarCore calls Forge's overlay events only when refreshing its transparent HUD texture.
        // Draw the live HUD after that cache has been composited onto the game framebuffer.
        if (hudDeferred && mc.world != null && (!mc.gameSettings.hideGUI || mc.currentScreen != null)) {
            renderHud()
        }
    }

    private fun renderHud() {
        if (hudRendered) return
        hudRendered = true
        DangoMusic.callEvent(HudRenderEvent)
    }

    // ---------------------------------------------------------------- 生物跟着节奏跳

    /** 这只生物画之前套上了跳舞的姿势（Post 里撤掉），以及给它的身体 / 头多转了多少度 */
    private var dancing: EntityLivingBase? = null
    private var danceSpin = 0f

    /**
     * 画生物之前：压扁 / 拉长套在矩阵上（以脚下为原点）；转圈改的是身体和头的朝向，
     * 不直接转矩阵，免得头顶的名字跟着转。被别的 mod 取消的就不管（最低优先级，最后一个收到）。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onRenderLivingPre(e: RenderLivingEvent.Pre<*>) {
        val pose = Dance.pose() ?: return
        val entity = e.entity
        danceSpin = pose[0]
        entity.renderYawOffset += danceSpin
        entity.prevRenderYawOffset += danceSpin
        entity.rotationYawHead += danceSpin
        entity.prevRotationYawHead += danceSpin
        val squash = pose[1]
        val wide = 1f / Math.sqrt(squash.toDouble()).toFloat()
        GlStateManager.pushMatrix()
        GlStateManager.translate(e.x, e.y, e.z)
        GlStateManager.scale(wide, squash, wide)
        GlStateManager.translate(-e.x, -e.y, -e.z)
        dancing = entity
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onRenderLivingPost(e: RenderLivingEvent.Post<*>) {
        val entity = dancing ?: return
        if (entity !== e.entity) return
        dancing = null
        entity.renderYawOffset -= danceSpin
        entity.prevRenderYawOffset -= danceSpin
        entity.rotationYawHead -= danceSpin
        entity.prevRotationYawHead -= danceSpin
        GlStateManager.popMatrix()
    }
}
