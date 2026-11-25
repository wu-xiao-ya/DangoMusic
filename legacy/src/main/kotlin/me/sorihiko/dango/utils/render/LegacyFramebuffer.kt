package me.sorihiko.dango.utils.render

import org.lwjgl.opengl.EXTFramebufferObject
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GLContext

/** ARB_framebuffer_object and OpenGL 3 expose the same unsuffixed LWJGL entry points. */
internal object LegacyFramebuffer {
    private val caps get() = GLContext.getCapabilities()
    private val core get() = caps.OpenGL30 || caps.GL_ARB_framebuffer_object
    val supported get() = core || caps.GL_EXT_framebuffer_object
    val separateReadDrawBindings get() = core || caps.GL_EXT_framebuffer_blit

    fun glGenFramebuffers(): Int =
        if (core) GL30.glGenFramebuffers() else EXTFramebufferObject.glGenFramebuffersEXT()

    fun glBindFramebuffer(target: Int, id: Int) {
        if (core) GL30.glBindFramebuffer(target, id) else EXTFramebufferObject.glBindFramebufferEXT(target, id)
    }

    fun glDeleteFramebuffers(id: Int) {
        if (core) GL30.glDeleteFramebuffers(id) else EXTFramebufferObject.glDeleteFramebuffersEXT(id)
    }

    fun glCheckFramebufferStatus(target: Int): Int =
        if (core) GL30.glCheckFramebufferStatus(target) else EXTFramebufferObject.glCheckFramebufferStatusEXT(target)

    fun glFramebufferTexture2D(target: Int, attachment: Int, textureTarget: Int, texture: Int, level: Int) {
        if (core) GL30.glFramebufferTexture2D(target, attachment, textureTarget, texture, level)
        else EXTFramebufferObject.glFramebufferTexture2DEXT(target, attachment, textureTarget, texture, level)
    }

    fun generateMipmap() {
        if (core) GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D) else EXTFramebufferObject.glGenerateMipmapEXT(GL11.GL_TEXTURE_2D)
    }
}
