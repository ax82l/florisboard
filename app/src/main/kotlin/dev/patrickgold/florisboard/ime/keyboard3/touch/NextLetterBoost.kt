package dev.patrickgold.florisboard.ime.keyboard3.touch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.patrickgold.florisboard.ime.keyboard3.ImeEditor
import dev.patrickgold.florisboard.ime.keyboard3.ui.asAttrValue

object NextLetterBoost {
    var enabled by mutableStateOf(true)
    var growth by mutableFloatStateOf(0.4f)

    var probs by mutableStateOf<Map<String, Float>>(emptyMap())
        private set

    fun refresh(editor: ImeEditor) {
        val before = editor.getSurroundingText(40, 0).textBefore
        val prefix = before.takeLastWhile { it.isLetter() }.lowercase()
        probs = computeProbs(prefix)
    }

    fun keyText(key: TouchKey): String? = key.attrs.output?.asAttrValue()?.lowercase()

    fun isLetterKey(key: TouchKey): Boolean {
        val t = keyText(key) ?: return false
        return t.length == 1 && t[0].isLetter()
    }

    fun probOf(key: TouchKey): Float {
        val t = keyText(key) ?: return 0f
        return probs[t] ?: 0f
    }

    fun expandedHitbox(key: TouchKey): Rect {
        val p = if (enabled) probOf(key).coerceIn(0f, 1f) else 0f
        if (p <= 0f) return key.hitbox
        val g = growth.coerceIn(0f, 0.45f) * p
        val gx = key.hitbox.width * g
        val gy = key.hitbox.height * g
        return Rect(
            key.hitbox.left - gx,
            key.hitbox.top - gy,
            key.hitbox.right + gx,
            key.hitbox.bottom + gy,
        )
    }

    fun boostedKey(keys: List<TouchKey>, position: Offset): TouchKey? {
        if (!enabled || probs.isEmpty()) return null
        var best: TouchKey? = null
        for (key in keys) {
            if (key.hitbox.contains(position)) {
                best = key
                break
            }
        }
        if (best != null && !isLetterKey(best)) return null
        var bestP = best?.let { probOf(it) } ?: 0f
        var changed = false
        for (key in keys) {
            val p = probOf(key)
            if (p <= bestP) continue
            if (expandedHitbox(key).contains(position)) {
                best = key
                bestP = p
                changed = true
            }
        }
        return if (changed) best else null
    }

    private fun computeProbs(prefix: String): Map<String, Float> {
        val counts = HashMap<String, Float>()
        var total = 0f
        for ((word, freq) in WORDS) {
            if (word.length > prefix.length && word.startsWith(prefix)) {
                val c = word[prefix.length].toString()
                counts[c] = (counts[c] ?: 0f) + freq
                total += freq
            }
        }
        if (total <= 0f) return emptyMap()
        return counts.mapValues { it.value / total }
    }

    private val WORDS: List<Pair<String, Float>> = (
        "the:100 that:50 this:40 there:30 then:20 they:35 them:20 these:15 think:15 though:6 through:8 " +
        "three:8 thing:12 things:8 what:30 when:25 where:15 which:20 while:10 who:20 with:45 would:25 " +
        "will:35 was:60 were:25 we:40 you:55 your:30 yes:8 year:12 and:90 are:45 about:25 after:15 " +
        "again:12 all:35 also:15 any:15 because:12 been:18 before:10 but:50 by:30 can:35 come:15 " +
        "could:20 day:15 do:30 down:12 even:10 first:12 for:55 from:35 get:18 give:10 go:20 good:12 " +
        "have:45 he:40 her:25 here:15 him:15 his:35 how:20 if:25 in:70 into:12 is:60 it:60 its:12 " +
        "just:15 know:18 like:20 look:10 make:15 many:10 me:20 more:15 most:8 my:25 new:10 no:20 " +
        "not:45 now:15 of:80 on:45 one:25 only:10 or:30 other:10 our:12 out:20 over:10 people:12 " +
        "say:10 see:15 she:25 so:30 some:15 take:10 tell:10 than:10 time:15 to:90 two:8 up:20 us:10 " +
        "use:10 very:8 want:12 way:10 well:10 work:10 hello:6 help:6 happy:4 house:5 hand:4 " +
        "thanks:6 text:3 type:3 typing:3 keyboard:2"
        ).split(" ").map { entry ->
        val (w, f) = entry.split(":")
        w to f.toFloat()
    }
}
