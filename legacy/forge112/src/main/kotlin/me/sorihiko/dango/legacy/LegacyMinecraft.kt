package me.sorihiko.dango.legacy

import net.minecraft.util.SoundCategory
import net.minecraftforge.client.event.sound.PlaySoundEvent

internal val legacyGameDir get() = mc.gameDir
internal val legacyFontRenderer get() = mc.fontRenderer
internal val hasClientWorld get() = mc.world != null

internal fun chatCopyAt(x: Int, y: Int): String? {
    val component = mc.ingameGUI.chatGUI.getChatComponent(x, y) ?: return null
    return LegacyChatCopy.copyValue(component.style.clickEvent)
}

internal fun suppressBackgroundMusic(event: PlaySoundEvent) {
    if (event.sound.category == SoundCategory.MUSIC) event.resultSound = null
}
