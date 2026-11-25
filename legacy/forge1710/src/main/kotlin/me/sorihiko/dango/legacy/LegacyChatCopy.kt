package me.sorihiko.dango.legacy

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.IChatComponent
import org.lwjgl.input.Mouse

/** 1.7 GuiChat sends clicks on components without a click action to its ordinary buttons. */
internal object LegacyChatCopy {
    private class CopyHint(text: String, val value: String) : ChatComponentText(text)

    fun message(text: String, copyText: String, copyHint: String): IChatComponent {
        val at = text.indexOf(copyText)
        if (copyText.isEmpty() || at < 0) return ChatComponentText(text)
        val style = ChatStyle().setColor(EnumChatFormatting.AQUA).setUnderlined(true)
            .setChatHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, CopyHint(copyHint, copyText)))
        return ChatComponentText(text.substring(0, at))
            .appendSibling(ChatComponentText(copyText).setChatStyle(style))
            .appendSibling(ChatComponentText(text.substring(at + copyText.length)))
    }

    // The marker lives only in local chat history, so remote messages cannot claim a clipboard action.
    class ClickListener : GuiButton(-1, 0, 0, 0, 0, "") {
        override fun drawButton(mc: Minecraft, x: Int, y: Int) = Unit

        override fun mousePressed(mc: Minecraft, x: Int, y: Int): Boolean {
            val component = mc.ingameGUI.chatGUI.getChatComponent(Mouse.getEventX(), Mouse.getEventY())
            val hint = component?.chatStyle?.chatHoverEvent?.value as? CopyHint
            if (hint != null) GuiScreen.setClipboardString(hint.value)
            return false
        }
    }
}
