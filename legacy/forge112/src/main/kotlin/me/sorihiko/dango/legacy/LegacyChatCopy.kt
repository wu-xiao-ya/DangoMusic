package me.sorihiko.dango.legacy

import net.minecraft.util.text.event.ClickEvent
import net.minecraft.util.text.event.HoverEvent
import net.minecraft.util.text.TextComponentString
import net.minecraft.util.text.Style
import net.minecraft.util.text.TextFormatting
import net.minecraft.util.text.ITextComponent

/** 1.12.2 没有 COPY_TO_CLIPBOARD；只由本地鼠标事件处理自己创建的链接。 */
internal object LegacyChatCopy {
    private class CopyLink(value: String) : ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, value)

    fun message(text: String, copyText: String, copyHint: String): ITextComponent {
        val at = text.indexOf(copyText)
        if (copyText.isEmpty() || at < 0) return TextComponentString(text)
        val click = CopyLink(copyText)
        val style = Style().setColor(TextFormatting.AQUA).setUnderlined(true).setClickEvent(click)
        if (copyHint.isNotEmpty()) style.setHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponentString(copyHint)))
        return TextComponentString(text.substring(0, at))
            .appendSibling(TextComponentString(copyText).setStyle(style))
            .appendSibling(TextComponentString(text.substring(at + copyText.length)))
    }

    // 按事件对象身份匹配，服务器发来的同名 SUGGEST_COMMAND 不会被接管。
    fun copyValue(event: ClickEvent?): String? = (event as? CopyLink)?.value
}
