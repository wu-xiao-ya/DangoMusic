package me.sorihiko.dango.legacy

import net.minecraft.client.audio.SoundCategory
import net.minecraftforge.client.event.sound.PlaySoundEvent

internal val legacyGameDir get() = mc.mcDataDir
internal val legacyFontRenderer get() = mc.fontRendererObj
internal val hasClientWorld get() = mc.theWorld != null

internal fun chatCopyAt(x: Int, y: Int): String? {
    val component = mc.ingameGUI.chatGUI.getChatComponent(x, y) ?: return null
    return LegacyChatCopy.copyValue(component.chatStyle.chatClickEvent)
}

internal fun suppressBackgroundMusic(event: PlaySoundEvent) {
    if (event.category == SoundCategory.MUSIC) event.result = null
}
