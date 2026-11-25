package me.sorihiko.dango.legacy

import net.minecraft.client.audio.SoundCategory
import net.minecraftforge.client.event.sound.PlaySoundEvent17

internal val legacyGameDir get() = mc.mcDataDir
internal val legacyFontRenderer get() = mc.fontRendererObj
internal val hasClientWorld get() = mc.theWorld != null

internal fun suppressBackgroundMusic(event: PlaySoundEvent17) {
    if (event.category == SoundCategory.MUSIC) event.result = null
}
