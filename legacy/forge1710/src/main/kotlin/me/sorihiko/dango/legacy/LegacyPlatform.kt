package me.sorihiko.dango.legacy

import me.sorihiko.dango.DangoMusic
import me.sorihiko.dango.platform.Platform
import me.sorihiko.dango.platform.PlayerPose
import me.sorihiko.dango.platform.PlayerKeyBinding

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.GuiMainMenu
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiPlayerInfo
import net.minecraft.util.MathHelper
import net.minecraft.client.audio.ISound
import net.minecraft.client.audio.MusicTicker
import cpw.mods.fml.relauncher.ReflectionHelper
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

import java.nio.file.Path

/** 1.7.10 的游戏接口。键码本来就是 LWJGL 2 的，不用转换 */
object LegacyPlatform : Platform {

    override val configDir: Path get() = mc.mcDataDir.toPath().resolve("config").resolve(DangoMusic.MOD_ID)

    override val playerName: String get() = mc.session.username

    override val languageCode: String? get() = mc.gameSettings?.language

    override val windowWidth: Int get() = mc.displayWidth
    override val windowHeight: Int get() = mc.displayHeight

    override val mouseX: Int get() = Mouse.getX()
    override val mouseY: Int get() = mc.displayHeight - Mouse.getY() - 1

    override fun isMouseDown(button: Int): Boolean = Mouse.isButtonDown(button)

    override fun isKeyDown(key: Int): Boolean = try {
        if (key in -100..-1) Mouse.isButtonDown(key + 100) else Keyboard.isKeyDown(key)
    } catch (_: Exception) {
        false
    }

    override fun syncKeyBindings(bindings: List<PlayerKeyBinding>) = LegacyKeyBindings.sync(bindings)

    override val ctrlDown: Boolean get() = GuiScreen.isCtrlKeyDown()

    override var clipboard: String
        get() = GuiScreen.getClipboardString()
        set(value) = GuiScreen.setClipboardString(value)

    override val inGame: Boolean get() = mc.currentScreen == null && mc.theWorld != null

    override val inMainMenu: Boolean get() = mc.currentScreen is GuiMainMenu

    override val chatOpen: Boolean get() = mc.currentScreen is GuiChat

    override val guiScale: Int get() = legacyResolution().scaleFactor

    override val fps: Int get() = debugFpsField.getInt(null)

    override val fov: Float get() = mc.gameSettings.fovSetting

    override val pingMs: Int
        get() {
            val p = mc.thePlayer ?: return 0
            if (mc.isSingleplayer) return 0
            val players = mc.netHandler?.playerInfoList ?: return 0
            return players.asSequence().filterIsInstance<GuiPlayerInfo>().firstOrNull { it.name == p.commandSenderName }?.responseTime ?: 0
        }

    override val skyDarken: Int? get() = mc.theWorld?.skylightSubtracted

    // 旧版没有方块标签，同时识别树叶类和树叶材质，避免树冠产生室内回声。
    private fun isAcousticLeaves(block: net.minecraft.block.Block): Boolean =
        block is net.minecraft.block.BlockLeaves || block.material == net.minecraft.block.material.Material.leaves

    private val absorptionCache = java.util.IdentityHashMap<net.minecraft.block.Block, Float>()

    override fun soundAbsorption(x: Int, y: Int, z: Int): Float {
        val world = mc.theWorld ?: return -1f
        val block = world.getBlock(x, y, z)
        return absorptionCache.getOrPut(block) {
            val material = block.material
            if (isAcousticLeaves(block) || material.isLiquid || !material.blocksMovement()) -1f else when (block.stepSound) {
                net.minecraft.block.Block.soundTypeStone, net.minecraft.block.Block.soundTypePiston -> 0.03f
                net.minecraft.block.Block.soundTypeMetal, net.minecraft.block.Block.soundTypeAnvil -> 0.04f
                net.minecraft.block.Block.soundTypeGlass -> 0.05f
                net.minecraft.block.Block.soundTypeWood, net.minecraft.block.Block.soundTypeLadder -> 0.12f
                net.minecraft.block.Block.soundTypeGrass -> 0.3f
                net.minecraft.block.Block.soundTypeGravel, net.minecraft.block.Block.soundTypeSand -> 0.35f
                net.minecraft.block.Block.soundTypeSnow -> 0.5f
                net.minecraft.block.Block.soundTypeCloth -> 0.7f
                else -> 0.1f
            }
        }
    }

    private const val d = 0.57735
    private val acousticDirections = arrayOf(
        doubleArrayOf(1.0, 0.0, 0.0), doubleArrayOf(-1.0, 0.0, 0.0), doubleArrayOf(0.0, 1.0, 0.0),
        doubleArrayOf(0.0, -1.0, 0.0), doubleArrayOf(0.0, 0.0, 1.0), doubleArrayOf(0.0, 0.0, -1.0),
        doubleArrayOf(d, d, d), doubleArrayOf(d, d, -d), doubleArrayOf(-d, d, d), doubleArrayOf(-d, d, -d),
        doubleArrayOf(d, -d, d), doubleArrayOf(d, -d, -d), doubleArrayOf(-d, -d, d), doubleArrayOf(-d, -d, -d),
    )

    override val environment: me.sorihiko.dango.platform.ListenerEnvironment?
        get() {
            val p = mc.thePlayer ?: return null
            val world = mc.theWorld ?: return null
            val eyeY = p.posY + p.getEyeHeight()
            val eyeX = MathHelper.floor_double(p.posX)
            val blockY = MathHelper.floor_double(eyeY)
            val eyeZ = MathHelper.floor_double(p.posZ)
            // 闭塞程度：往 14 个方向各看 6 格，碰到实心方块（不算空气和液体）的比例
            var hits = 0
            for (dir in acousticDirections) {
                for (step in 1..6) {
                    val x = MathHelper.floor_double(p.posX + dir[0] * step)
                    val y = MathHelper.floor_double(eyeY + dir[1] * step)
                    val z = MathHelper.floor_double(p.posZ + dir[2] * step)
                    val block = world.getBlock(x, y, z)
                    if (!world.isAirBlock(x, y, z) && !block.material.isLiquid && !isAcousticLeaves(block)) {
                        hits++
                        break
                    }
                }
            }
            val rain = when {
                !world.isRainingAt(eyeX, blockY, eyeZ) -> 0f
                world.isThundering -> 1f
                else -> 0.5f
            }
            return me.sorihiko.dango.platform.ListenerEnvironment(
                p.isInsideOfMaterial(net.minecraft.block.material.Material.water), world.canBlockSeeTheSky(eyeX, blockY, eyeZ), hits / acousticDirections.size.toFloat(), rain)
        }

    override val player: PlayerPose?
        get() {
            val p = mc.thePlayer ?: return null
            return PlayerPose(p.posX, p.posY + p.getEyeHeight(), p.posZ, p.rotationYaw, p.rotationPitch)
        }

    override fun showChatMessage(message: String, copyText: String, copyHint: String): Boolean {
        val player = mc.thePlayer ?: return false
        player.addChatMessage(LegacyChatCopy.message(message, copyText, copyHint))
        return true
    }

    override fun openMusicScreen() {
        mc.displayGuiScreen(LegacyMusicScreen(mc.currentScreen))
    }

    private val debugFpsField by lazy { ReflectionHelper.findField(Minecraft::class.java, "debugFPS", "field_71470_ab") }
    private val musicTickerField by lazy { ReflectionHelper.findField(Minecraft::class.java, "mcMusicTicker", "field_147126_aw") }
    private val currentMusicField by lazy { ReflectionHelper.findField(MusicTicker::class.java, "currentMusic", "field_147678_c") }
    private var holdingMusic = false

    /** 刚开始让路时停掉正在放的原版音乐；之后由 ForgeBridge.onPlaySound 拦住新的 */
    override fun holdVanillaMusic(hold: Boolean) {
        me.sorihiko.dango.platform.VanillaMusicHold.active = hold
        if (hold && !holdingMusic) {
            val ticker = musicTickerField.get(mc)
            val sound = ticker?.let { currentMusicField.get(it) as? ISound }
            if (sound != null) mc.soundHandler.stopSound(sound)
        }
        holdingMusic = hold
    }
}
