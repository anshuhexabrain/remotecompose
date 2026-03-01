package com.example.remotecompose.server

import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.modifiers.ShapeType
import androidx.compose.remote.creation.JvmRcPlatformServices
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import com.example.remotecompose.shared.ElementConfig
import com.example.remotecompose.shared.LayoutConfig
import com.example.remotecompose.shared.parseColorLong

private const val ACTION_BUTTON_CLICKED = 1001

private fun parseArgb(hex: String): Int = parseColorLong(hex).toInt()

private fun density(): Float = 2.625f

private fun dp(value: Int): Float = value * density()
private fun dp(value: Float): Float = value * density()
private fun sp(value: Int): Float = value * density()

private val platform = JvmRcPlatformServices()

fun buildDocument(config: LayoutConfig): ByteArray {
    val width = (400 * density()).toInt()
    val height = (800 * density()).toInt()

    val writer = RemoteComposeWriter(
        platform,
        RemoteComposeWriter.HTag(Header.DOC_WIDTH, width),
        RemoteComposeWriter.HTag(Header.DOC_HEIGHT, height),
    )

    val bgColor = parseArgb(config.backgroundColor)
    val padding = config.padding ?: 24

    val rootMod = RecordingModifier()
        .fillMaxSize()
        .background(bgColor)

    if (config.scrollable) {
        rootMod.verticalScroll()
    }

    rootMod.padding(dp(padding))

    val arrangement = if (config.scrollable) ColumnLayout.TOP else ColumnLayout.CENTER

    writer.root {
        writer.column(rootMod, ColumnLayout.CENTER, arrangement) {
            for (element in config.elements) {
                renderElement(writer, element, insideRow = false)
            }
        }
    }

    return writer.encodeToByteArray()
}

private fun renderElement(writer: RemoteComposeWriter, el: ElementConfig, insideRow: Boolean) {
    when (el.type) {
        "text" -> renderText(writer, el)
        "button" -> renderButton(writer, el, fillWidth = !insideRow)
        "spacer" -> renderSpacer(writer, el)
        "hspacer" -> renderHSpacer(writer, el)
        "divider" -> renderDivider(writer, el)
        "card" -> renderCard(writer, el)
        "row" -> renderRow(writer, el)
    }
}

private fun renderText(writer: RemoteComposeWriter, el: ElementConfig) {
    val color = parseArgb(el.color ?: "#000000")
    val fontSize = sp(el.fontSize ?: 16)
    val textId = writer.addText(el.text ?: "")

    val padH = el.paddingH ?: 0
    val padV = el.paddingV ?: 0
    val mod = RecordingModifier()
    if (padH > 0 || padV > 0) {
        mod.padding(dp(padH), dp(padV), dp(padH), dp(padV))
    }

    writer.textComponent(mod, textId, color, fontSize, 0, 400f, null, CoreText.TEXT_ALIGN_START, 0, Int.MAX_VALUE) {}
}

private fun renderButton(writer: RemoteComposeWriter, el: ElementConfig, fillWidth: Boolean) {
    val radius = el.cornerRadius ?: 24
    val bgColor = parseArgb(el.color ?: "#6200EA")
    val textColor = parseArgb(el.textColor ?: "#FFFFFF")
    val borderW = el.borderWidth ?: 0
    val borderColor = if (el.borderColor != null && borderW > 0) parseArgb(el.borderColor!!) else bgColor
    val shape = RoundedRectShape(dp(radius), dp(radius), dp(radius), dp(radius))

    val actionName = el.actionName ?: el.id.ifEmpty { el.text ?: "button" }

    val mod = RecordingModifier()
    if (fillWidth) mod.fillMaxWidth()
    mod.clip(shape)
        .background(bgColor)
        .border(dp(if (borderW > 0) borderW else 1), dp(radius), borderColor, ShapeType.ROUNDED_RECTANGLE)
        .onClick(HostAction(ACTION_BUTTON_CLICKED, writer.addText(actionName)))
        .padding(dp(el.paddingH ?: 32), dp(el.paddingV ?: 14), dp(el.paddingH ?: 32), dp(el.paddingV ?: 14))

    val textId = writer.addText(el.text ?: "Button")

    writer.startBox(mod, BoxLayout.CENTER, BoxLayout.CENTER)
    writer.textComponent(RecordingModifier(), textId, textColor, sp(el.fontSize ?: 16), 0, 600f, null, CoreText.TEXT_ALIGN_CENTER, 0, Int.MAX_VALUE) {}
    writer.endBox()
}

private fun renderSpacer(writer: RemoteComposeWriter, el: ElementConfig) {
    val mod = RecordingModifier().height(dp(el.height ?: 16))
    writer.startBox(mod)
    writer.endBox()
}

private fun renderHSpacer(writer: RemoteComposeWriter, el: ElementConfig) {
    val mod = RecordingModifier().width(dp(el.width ?: 16))
    writer.startBox(mod)
    writer.endBox()
}

private fun renderDivider(writer: RemoteComposeWriter, el: ElementConfig) {
    val color = parseArgb(el.color ?: "#CCCCCC")
    val mod = RecordingModifier()
        .fillMaxWidth()
        .height(dp(el.height ?: 1))
        .background(color)
        .padding(0f, dp(8), 0f, dp(8))
    writer.startBox(mod)
    writer.endBox()
}

private fun renderCard(writer: RemoteComposeWriter, el: ElementConfig) {
    val radius = el.cornerRadius ?: 16
    val cardBg = parseArgb(el.color ?: "#FFFFFF")
    val borderW = el.borderWidth ?: 0
    val cardBorderColor = el.borderColor

    val alignment = when (el.align) {
        "start" -> ColumnLayout.START
        "end" -> ColumnLayout.END
        else -> ColumnLayout.CENTER
    }

    val padH = el.paddingH ?: 16
    val padV = el.paddingV ?: 16
    val shape = RoundedRectShape(dp(radius), dp(radius), dp(radius), dp(radius))

    val mod = RecordingModifier().fillMaxWidth()

    if (cardBorderColor != null && borderW > 0) {
        mod.border(dp(borderW), dp(radius), parseArgb(cardBorderColor), ShapeType.ROUNDED_RECTANGLE)
    }

    if (radius > 0) {
        mod.clip(shape)
    }

    mod.background(cardBg)

    val cardAction = el.actionName
    if (cardAction != null) {
        mod.onClick(HostAction(ACTION_BUTTON_CLICKED, writer.addText(cardAction)))
    }

    if (padH > 0 || padV > 0) {
        mod.padding(dp(padH), dp(padV), dp(padH), dp(padV))
    }

    writer.startBox(mod)
    val innerMod = RecordingModifier().fillMaxWidth()
    writer.column(innerMod, alignment, ColumnLayout.TOP) {
        el.children?.forEach { child -> renderElement(writer, child, insideRow = false) }
    }
    writer.endBox()
}

private fun renderRow(writer: RemoteComposeWriter, el: ElementConfig) {
    val mod = RecordingModifier()
        .fillMaxWidth()
        .padding(0f, dp(4), 0f, dp(4))

    writer.row(mod, RowLayout.CENTER, RowLayout.SPACE_EVENLY) {
        el.children?.forEach { child -> renderElement(writer, child, insideRow = true) }
    }
}
