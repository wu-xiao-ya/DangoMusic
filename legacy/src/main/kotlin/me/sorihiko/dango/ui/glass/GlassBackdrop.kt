package me.sorihiko.dango.ui.glass

import me.sorihiko.dango.DangoLogger
import me.sorihiko.dango.legacy.LegacyGl
import me.sorihiko.dango.legacy.mc
import me.sorihiko.dango.shader.impls.QuadShader
import me.sorihiko.dango.shader.impls.ui.GlassBlurShader
import me.sorihiko.dango.utils.render.LegacyFramebuffer

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30

import java.nio.ByteBuffer

/**
 * 液态玻璃的"背后世界"：每帧把当前画面拷贝下来，生成两级模糊纹理。
 *  - soft ：1/4 分辨率轻度模糊（清透玻璃、折射边缘）
 *  - frost：1/8 分辨率重度模糊（磨砂玻璃）
 * 必须在绘制菜单之前调用 [capture]，这样菜单自身不会出现在背景里。
 */
class GlassBackdrop {
    private val quadShader = LegacyGl.shaderManager.get<QuadShader>()
    private val blurShader = LegacyGl.shaderManager.get<GlassBlurShader>()

    private val captured = GlassTarget()
    private val half = GlassTarget()
    private val softA = GlassTarget()
    private val softB = GlassTarget()
    private val frostA = GlassTarget()
    private val frostB = GlassTarget()
    private val debugCapture = java.lang.Boolean.getBoolean("dangomusic.debugGlass")
    private var lastCaptureReport: String? = null

    val softTexture: Int get() = softA.texture
    val frostTexture: Int get() = frostA.texture

    var ready = false
        private set

    fun capture() {
        val w = mc.displayWidth
        val h = mc.displayHeight
        ready = false
        if (w <= 0 || h <= 0 || !LegacyFramebuffer.supported) return

        // OptiFine Fast Render can draw directly to the window while our blur FBOs still work.
        val drawFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING)
        val separateBindings = LegacyFramebuffer.separateReadDrawBindings
        val readFbo = if (separateBindings) GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING) else drawFbo
        val samples = GL11.glGetInteger(GL13.GL_SAMPLES)
        val multisampled = samples > 0
        if (multisampled && !separateBindings) return
        val sourceBuffer = when (val drawBuffer = GL11.glGetInteger(GL11.GL_DRAW_BUFFER)) {
            GL11.GL_NONE -> return
            GL11.GL_FRONT_AND_BACK -> if (GL11.glGetBoolean(GL11.GL_DOUBLEBUFFER)) GL11.GL_BACK else GL11.GL_FRONT
            else -> drawBuffer
        }
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT or GL11.GL_VIEWPORT_BIT or GL11.GL_COLOR_BUFFER_BIT or
                    GL11.GL_SCISSOR_BIT or GL11.GL_TEXTURE_BIT
        )
        try {
            val previousError = if (debugCapture) GL11.glGetError() else GL11.GL_NO_ERROR
            GL13.glActiveTexture(GL13.GL_TEXTURE0)
            if (w != captured.width || h != captured.height || captured.texture == -1) resize(w, h)

            LegacyFramebuffer.glBindFramebuffer(GL30.GL_FRAMEBUFFER, drawFbo)
            val oldReadBuffer = GL11.glGetInteger(GL11.GL_READ_BUFFER)
            GL11.glDisable(GL11.GL_SCISSOR_TEST)
            try {
                if (sourceBuffer != oldReadBuffer) GL11.glReadBuffer(sourceBuffer)
                if (multisampled) {
                    // Resolve at full size before downsampling. CopyTexSubImage cannot read a multisampled FBO.
                    LegacyFramebuffer.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, captured.fbo)
                    LegacyFramebuffer.resolveColor(w, h)
                } else {
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, captured.texture)
                    GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h)
                }
            } finally {
                if (sourceBuffer != oldReadBuffer) GL11.glReadBuffer(oldReadBuffer)
            }
            val copyError = if (debugCapture) GL11.glGetError() else GL11.GL_NO_ERROR

            GL11.glDisable(GL11.GL_BLEND)
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GL11.glDisable(GL11.GL_ALPHA_TEST)
            GL11.glDisable(GL11.GL_CULL_FACE)
            GL11.glColorMask(true, true, true, true)

            // 逐级 2x 降采样（双线性正好取 2x2 平均，画面移动时不闪）
            blit(captured.texture, half)
            blit(half.texture, softA)
            blur(softA, softB, 1f, 2)

            blit(softA.texture, frostA)
            blur(frostA, frostB, 1.6f, 3)

            val blurError = if (debugCapture) GL11.glGetError() else GL11.GL_NO_ERROR
            ready = copyError == GL11.GL_NO_ERROR && blurError == GL11.GL_NO_ERROR
            if (debugCapture) {
                val report = "size=${w}x$h drawFbo=$drawFbo readFbo=$readFbo sourceBuffer=$sourceBuffer " +
                    "samples=$samples previousError=$previousError copyError=$copyError blurError=$blurError ready=$ready"
                if (report != lastCaptureReport) {
                    DangoLogger.info("Liquid glass backdrop: {}", report)
                    lastCaptureReport = report
                }
            }
        } finally {
            GL20.glUseProgram(0)
            LegacyFramebuffer.glBindFramebuffer(GL30.GL_FRAMEBUFFER, drawFbo)
            if (readFbo != drawFbo) LegacyFramebuffer.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo)
            GL11.glPopAttrib()
        }
    }

    private fun resize(w: Int, h: Int) {
        half.ensure(w / 2, h / 2)
        softA.ensure(w / 4, h / 4)
        softB.ensure(w / 4, h / 4)
        frostA.ensure(w / 8, h / 8)
        frostB.ensure(w / 8, h / 8)
        captured.ensure(w, h)
    }

    private fun blit(source: Int, target: GlassTarget) {
        target.bind()
        quadShader.bind()
        quadShader.setup()
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, source)
        fullscreenQuad()
    }

    /** 可分离高斯：A -> B（横向）-> A（纵向），结果留在 A */
    private fun blur(a: GlassTarget, b: GlassTarget, spread: Float, iterations: Int) {
        blurShader.bind()
        repeat(iterations) {
            b.bind()
            blurShader.setup(spread / a.width, 0f)
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, a.texture)
            fullscreenQuad()

            a.bind()
            blurShader.setup(0f, spread / b.height)
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, b.texture)
            fullscreenQuad()
        }
    }

    private fun fullscreenQuad() {
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glTexCoord2f(0f, 0f); GL11.glVertex2f(-1f, -1f)
        GL11.glTexCoord2f(1f, 0f); GL11.glVertex2f(1f, -1f)
        GL11.glTexCoord2f(1f, 1f); GL11.glVertex2f(1f, 1f)
        GL11.glTexCoord2f(0f, 1f); GL11.glVertex2f(-1f, 1f)
        GL11.glEnd()
    }

    fun delete() {
        listOf(captured, half, softA, softB, frostA, frostB).forEach { it.delete() }
        ready = false
    }
}

/** 线性过滤 + 边缘钳制的颜色 FBO（RenderTarget 是 NEAREST，不适合降采样） */
class GlassTarget {
    var fbo = -1
        private set
    var texture = -1
        private set
    var width = 0
        private set
    var height = 0
        private set

    fun ensure(w: Int, h: Int) {
        val nw = w.coerceAtLeast(1)
        val nh = h.coerceAtLeast(1)
        if (nw == width && nh == height && fbo != -1) return
        delete()
        width = nw
        height = nh

        texture = createTexture(nw, nh)
        fbo = LegacyFramebuffer.glGenFramebuffers()
        LegacyFramebuffer.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo)
        LegacyFramebuffer.glFramebufferTexture2D(
            GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture, 0
        )
        val status = LegacyFramebuffer.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Glass framebuffer create failed: $status")
        }
    }

    fun bind() {
        LegacyFramebuffer.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo)
        GL11.glViewport(0, 0, width, height)
    }

    fun delete() {
        if (fbo != -1) LegacyFramebuffer.glDeleteFramebuffers(fbo)
        if (texture != -1) GL11.glDeleteTextures(texture)
        fbo = -1
        texture = -1
        width = 0
        height = 0
    }

    companion object {
        fun createTexture(w: Int, h: Int): Int {
            val id = GL11.glGenTextures()
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id)
            GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, null as ByteBuffer?
            )
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
            return id
        }
    }
}
