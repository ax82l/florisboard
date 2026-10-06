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

    // Makes Arabic spelling variants match each other (hamza forms, alef maqsura, ta marbuta)
    fun norm(s: String): String = s.lowercase()
        .replace('\u0623', '\u0627')
        .replace('\u0625', '\u0627')
        .replace('\u0622', '\u0627')
        .replace('\u0649', '\u064A')
        .replace('\u0629', '\u0647')

    fun refresh(editor: ImeEditor) {
        val before = editor.getSurroundingText(40, 0).textBefore
        val prefix = norm(before.takeLastWhile { it.isLetter() })
        probs = computeProbs(prefix)
    }

    fun keyText(key: TouchKey): String? = key.attrs.output?.asAttrValue()?.let { norm(it) }

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
            if (word.length >= prefix.length && word.startsWith(prefix)) {
                total += freq
                // if the word is already complete, it only counts as "the word may end here"
                if (word.length > prefix.length) {
                    val c = word[prefix.length].toString()
                    counts[c] = (counts[c] ?: 0f) + freq
                }
            }
        }
        if (total <= 0f) return emptyMap()
        return counts.mapValues { it.value / total }
    }

    // To add words: just put them in the lists below, separated by spaces.
    // Earlier in the list = more common. Any language works.
    private fun ranked(text: String): List<Pair<String, Float>> =
        text.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .map { norm(it) }
            .distinct()
            .mapIndexed { i, w -> w to 1000f / (i + 10) }

    private val WORDS: List<Pair<String, Float>> = ranked(ENGLISH_WORDS) + ranked(ARABIC_WORDS)

    private const val ENGLISH_WORDS = """
        the of and to a in is it you that he was
        for on are with as his they be at one have this
        from or had by but what some we can out other were
        all there when up use your how said an each she which
        do their time if will way about many then them write would
        like so these her long make thing see him two has look
        more day could go come did number sound no most people my
        over know water than call first who may down side been now
        find any new work part take get place made live where after
        back little only round man year came show every good me give
        our under name very through just form sentence great think say help
        low line differ turn cause much mean before move right boy old
        too same tell does set three want air well also play small
        end put home read hand port large spell add even land here
        must big high such follow act why ask men change went light
        kind off need house picture try us again animal point mother world
        near build self earth father head stand own page should country found
        answer school grow study still learn plant cover food sun four between
        state keep eye never last let thought city tree cross farm hard
        start might story saw far sea draw left late run while press
        close night real life few north open seem together next white children
        begin got walk example ease paper group always music those both mark
        often letter until mile river car feet care second book carry took
        science eat room friend began idea fish mountain stop once base hear
        horse cut sure watch color face wood main enough plain girl usual
        young ready above ever red list though feel talk bird soon body
        dog family direct leave song measure door product black short class wind
        question happen complete ship area half rock order fire south problem piece
        told knew pass since top whole king space heard best hour better
        true during hundred five remember step early hold west ground interest reach
        fast sing listen six table travel less morning ten simple several toward
        war lay against pattern slow center love person money serve appear road
        map rain rule govern pull cold notice voice unit power town fine
        certain fly fall lead cry dark machine note wait plan figure star
        box field rest correct able pound done beauty drive stood contain front
        teach week final gave green quick develop ocean warm free minute strong
        special mind behind clear tail produce fact street nothing course stay wheel
        full force blue object decide surface deep moon island foot system busy
        test record boat common gold possible plane dry wonder laugh thousand ago
        ran check game shape hot miss brought heat snow bring yes distant
        fill east paint language among hello hi hey thanks thank please sorry
        okay maybe today tomorrow tonight yesterday message text phone send happy birthday
        really actually because probably already something anything everything someone going doing getting
        looking thinking wanting having being coming trying working playing keyboard typing type
        meet lunch dinner coffee tired sleep bad nice cool awesome perfect
"""

    private const val ARABIC_WORDS = """
        في من على الى ان ما لا هذا هذه كان عن مع
        او كل ذلك التي الذي بعد بين هو هي لم ثم قد
        حتى عند اذا اي كيف لماذا متى اين ماذا نعم لكن بل
        ايضا هناك هنا الان اليوم غدا امس دائما ابدا جدا كثير قليل
        كبير صغير جديد قديم اول اخر بعض غير مثل بدون ضد حول
        خلال منذ قبل فوق تحت داخل خارج امام وراء وش ايش ليش
        وين شلون الحين توه مرة عشان علشان لانه لان بس مو مب
        لسا بعدين بكرا زين طيب تمام حلو كويس ممتاز ابي ابغى ابا
        تبي تبغى يبي ودي اقدر تقدر اللي هذي ذا ذي كذا شي
        شيء ايوه ايه والله يعني خلاص يالله يلا حبيبي حبيبتي اخوي اخوك
        ابوي امي يمه يبه عيال ربع شباب بنات رجال دوام شغل جوال
        تلفون واتس رسالة اتصل كلمني ارسل شوف شفت سمعت عرفت فهمت قلت
        قال يقول تقول رحت جيت راح يروح اروح اجي يجي سوي سويت
        يسوي كنت تكون ابشر ابشري يهمك مشكور تسلم هلا حياك يوم ليلة
        صباح مساء سنة شهر اسبوع ساعة دقيقة وقت مكان بيت مدرسة جامعة
        عمل مال سيارة طريق مدينة بلد دولة عالم ناس رجل امراة طفل
        ولد بنت اب ام اخ اخت صديق حياة حب قلب عين يد
        راس وجه كلمة سؤال جواب مشكلة حل فكرة موضوع معنى خير شر
        حق صح خطا ممكن لازم يجب يمكن اريد احب اعرف افهم اقول
        اذهب اعمل اكتب اقرا اسمع انظر اكل اشرب انام ساعد شكرا عفوا
        اسف مرحبا اهلا سلام عليكم ورحمة الله وبركاته الخير النور يعطيك العافية
        بارك فيك جزاك شاء الحمد سبحان
"""
}
