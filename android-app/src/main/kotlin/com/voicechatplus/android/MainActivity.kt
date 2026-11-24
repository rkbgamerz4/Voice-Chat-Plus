package com.voicechatplus.android

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.voicechatplus.common.VoiceChatPlusInfo

/**
 * Foundation diagnostics screen.
 *
 * This is a build-verification scaffold, not the final UI. It reports the real
 * state of the project rather than presenting unimplemented features as if
 * they worked. The seven designed screens are listed in docs/ARCHITECTURE.md
 * and will be introduced in Phase 6.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = VoiceChatPlusInfo.NAME

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(BACKGROUND))
            val pagePadding = dp(24)
            setPadding(pagePadding, pagePadding, pagePadding, pagePadding)
        }

        content.addView(
            label(
                text = "VOICE CHAT+",
                color = PRIMARY_BLUE,
                sizeSp = 28f,
                bold = true,
            ),
        )
        content.addView(label(text = VoiceChatPlusInfo.TAGLINE, color = MUTED, sizeSp = 14f))
        content.addView(spacer(24))

        content.addView(
            card(
                title = "Build status",
                body = "Version ${VoiceChatPlusInfo.VERSION} (foundation build)\n" +
                    "Voice transport: not implemented\n" +
                    "Audio capture / playback: not implemented\n" +
                    "Minecraft integration: not implemented\n" +
                    "Third-party compatibility: not verified",
            ),
        )

        content.addView(
            card(
                title = "Modules",
                body = "common, audio-engine, voice-protocol, network-engine: unit tested\n" +
                    "minecraft-integration: planned (Phase 5)\n" +
                    "android-app: build scaffold",
            ),
        )

        content.addView(
            card(
                title = "Next milestone",
                body = "Android audio foundation: AudioRecord / AudioTrack adapters, " +
                    "runtime microphone permission, audio focus, Opus integration " +
                    "and microphone test screen.",
            ),
        )

        setContentView(ScrollView(this).apply { addView(content) })
    }

    private fun label(text: String, color: String, sizeSp: Float, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor(color))
            textSize = sizeSp
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun card(title: String, body: String): LinearLayout {
        val padding = dp(16)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(SECONDARY_PANEL))
            setPadding(padding, padding, padding, padding)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = dp(12) }

            addView(
                TextView(context).apply {
                    this.text = title
                    setTextColor(Color.parseColor(PRIMARY_BLUE))
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                },
            )
            addView(
                TextView(context).apply {
                    this.text = body
                    setTextColor(Color.parseColor(TEXT))
                    textSize = 13f
                    setPadding(0, padding / 2, 0, 0)
                },
            )
        }
    }

    private fun spacer(dpValue: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(dpValue),
        )
    }

    private fun dp(value: Int): Int = (resources.displayMetrics.density * value).toInt()

    private companion object {
        const val BACKGROUND = "#05070C"
        const val SECONDARY_PANEL = "#09121E"
        const val PRIMARY_BLUE = "#00A8FF"
        const val TEXT = "#FFFFFF"
        const val MUTED = "#96A8BE"
    }
}
