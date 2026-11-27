package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.example.engine.EngineEvent
import com.example.engine.FootballEngine
import com.example.model.MatchPhase
import com.example.model.MatchState
import com.example.model.TeamSide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

class MatchCommentaryEngine(context: Context) : TextToSpeech.OnInitListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tts: TextToSpeech? = null

    @Volatile
    private var isReady: Boolean = false

    @Volatile
    private var effectiveVolume: Float = 0.75f

    @Volatile
    private var isEnabled: Boolean = true

    private val _activeCommentaryText = MutableStateFlow<String?>(null)
    val activeCommentaryText: StateFlow<String?> = _activeCommentaryText.asStateFlow()

    private var captionClearJob: Job? = null
    private val rng = Random(77)

    private var lastSpokenMs: Long = 0L
    private var lastShotMs: Long = 0L
    private var lastTackleMs: Long = 0L
    private var lastPassMs: Long = 0L
    private var lastPlayByPlayMs: Long = 0L

    private data class QueuedLine(val text: String, val pitch: Float, val rate: Float)
    private var pendingLine: QueuedLine? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (_: Throwable) {
            isReady = false
        }
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            isReady = false
            return
        }
        try {
            val engine = tts ?: return
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            val preferredLocales = listOf(
                Locale("ar", "EG"),
                Locale("ar", "SA"),
                Locale("ar"),
                Locale.getDefault()
            )
            for (loc in preferredLocales) {
                val res = engine.setLanguage(loc)
                if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                    break
                }
            }

            // Pick the highest-quality natural Arabic voice available on the device
            val bestArabicVoice = selectBestArabicVoice(engine)
            if (bestArabicVoice != null) {
                engine.voice = bestArabicVoice
            }

            engine.setSpeechRate(0.96f)
            engine.setPitch(0.92f)
            isReady = true
            pendingLine?.let { q ->
                pendingLine = null
                speakInternal(q.text, urgent = true, pitch = q.pitch, rate = q.rate)
            }
        } catch (_: Throwable) {
            isReady = false
        }
    }

    private fun selectBestArabicVoice(engine: TextToSpeech): Voice? {
        val available = try {
            engine.voices
        } catch (_: Throwable) {
            null
        } ?: return null

        return available
            .filter { it.locale?.language?.equals("ar", ignoreCase = true) == true }
            .maxByOrNull { voice ->
                var score = voice.quality * 10
                if (!voice.isNetworkConnectionRequired) score += 250
                val country = voice.locale?.country.orEmpty().uppercase()
                if (country == "EG") score += 80 else if (country == "SA" || country == "AE") score += 60
                val nameLower = voice.name.lowercase()
                if (nameLower.contains("male") || nameLower.contains("wavenet") || nameLower.contains("neural") || nameLower.contains("hq")) {
                    score += 120
                }
                score
            }
    }

    fun updateAudioConfig(volume: Float, enabled: Boolean) {
        effectiveVolume = volume.coerceIn(0f, 1f)
        isEnabled = enabled
        if (!isEnabled || effectiveVolume <= 0.01f) {
            stop()
        }
    }

    fun announceKickoff(homeTeamName: String, awayTeamName: String) {
        val now = System.currentTimeMillis()
        lastSpokenMs = now
        lastPlayByPlayMs = now
        val line = "بسم الله... تنطلق الآن صافرة البداية، قمة كروية تجمع $homeTeamName، أمام $awayTeamName."
        speakInternal(line, urgent = true, pitch = 0.94f, rate = 0.96f)
    }

    fun playSampleCommentary() {
        val line = "أهلاً بكم أعزائي المشاهدين في ملعب ركلة الأبطال... كل شيء جاهز لقمة كروية ممتعة."
        speakInternal(line, urgent = true, pitch = 0.93f, rate = 0.96f)
    }

    fun onMatchEvents(events: List<EngineEvent>, state: MatchState) {
        if (events.isEmpty()) return
        val now = System.currentTimeMillis()

        for (event in events) {
            when (event) {
                is EngineEvent.Goal -> {
                    val teamName = if (event.scoringSide == TeamSide.HOME) state.homeTeam.nameAr else state.awayTeam.nameAr
                    val scorer = state.ball.lastTouchPlayerName.takeIf { it.isNotBlank() } ?: "المهاجم"
                    val lines = listOf(
                        "الله... هدف أول ولا أروع! $scorer يضعها في الشباك لصالح $teamName!",
                        "يا سلام على اللمسة الأخيرة! هدف جميل من $scorer يشعل المدرجات لـ $teamName!",
                        "في المقص... مستحيل تصدها! $scorer يسجل لصالح $teamName!"
                    )
                    speakInternal(lines[rng.nextInt(lines.size)], urgent = true, pitch = 1.00f, rate = 1.01f)
                    lastSpokenMs = now
                    return
                }

                is EngineEvent.PostClang -> {
                    if (now - lastSpokenMs > 1800L) {
                        speakInternal("يا الله... الكرة ترتطم في القائم! كانت قريبة جداً من الشباك!", urgent = true, pitch = 0.98f, rate = 1.00f)
                        lastSpokenMs = now
                        return
                    }
                }

                is EngineEvent.HalfTime -> {
                    speakInternal(
                        "صافرة الحكم تعلن نهاية الشوط الأول، والنتيجة ${state.homeScore} مقابل ${state.awayScore}.",
                        urgent = true,
                        pitch = 0.91f,
                        rate = 0.95f
                    )
                    lastSpokenMs = now
                    return
                }

                is EngineEvent.MatchEnded -> {
                    val summary = when {
                        event.homeScore > event.awayScore -> "نهاية المباراة! فوز مستحق لصالح ${state.homeTeam.nameAr}، بنتيجة ${event.homeScore} مقابل ${event.awayScore}."
                        event.homeScore < event.awayScore -> "صافرة النهاية... ${state.awayTeam.nameAr} يحسم اللقاء بنتيجة ${event.awayScore} مقابل ${event.homeScore}."
                        else -> "نهاية مثيرة للمباراة، بالتعادل ${event.homeScore} لكل فريق."
                    }
                    speakInternal(summary, urgent = true, pitch = 0.94f, rate = 0.96f)
                    lastSpokenMs = now
                    return
                }

                is EngineEvent.Kick -> {
                    if (event.power >= 0.72f && now - lastShotMs > 3800L && now - lastSpokenMs > 2600L) {
                        val shooter = state.ball.lastTouchPlayerName.takeIf { it.isNotBlank() } ?: "اللاعب"
                        val shotLines = listOf(
                            "تسديدة قوية من $shooter... يا ترى فين؟",
                            "$shooter يسدد بقوة نحو المرمى!",
                            "فرصة خطيرة الآن من قدم $shooter!"
                        )
                        speakInternal(shotLines[rng.nextInt(shotLines.size)], urgent = false, pitch = 0.97f, rate = 0.99f)
                        lastShotMs = now
                        lastSpokenMs = now
                        return
                    }
                }

                is EngineEvent.Tackle -> {
                    if (now - lastTackleMs > 6200L && now - lastSpokenMs > 3600L) {
                        val tackleLines = listOf(
                            "قطع ممتاز للكرة في توقيت سليم.",
                            "تدخل ناجح من الدفاع، واستعادة سريعة للاستحواذ."
                        )
                        speakInternal(tackleLines[rng.nextInt(tackleLines.size)], urgent = false, pitch = 0.93f, rate = 0.97f)
                        lastTackleMs = now
                        lastSpokenMs = now
                        return
                    }
                }

                is EngineEvent.Pass -> {
                    if (now - lastPassMs > 9000L && now - lastSpokenMs > 4600L) {
                        val inAttackThird = if (state.homeAttacksRight) state.ball.x > FootballEngine.CENTER_X + 28f else state.ball.x < FootballEngine.CENTER_X - 28f
                        if (inAttackThird) {
                            speakInternal("تمريرة ذكية في المساحة، محاولة لفتح الدفاع.", urgent = false, pitch = 0.93f, rate = 0.97f)
                            lastPassMs = now
                            lastSpokenMs = now
                            return
                        }
                    }
                }

                is EngineEvent.Whistle -> Unit
            }
        }
    }

    fun maybeSpeakPlayByPlay(state: MatchState) {
        if (state.phase != MatchPhase.PLAYING) return
        if (!isEnabled || effectiveVolume <= 0.01f) return
        val now = System.currentTimeMillis()
        if (now - lastSpokenMs < 7200L || now - lastPlayByPlayMs < 10500L) return
        if (isTtsBusy()) return

        lastPlayByPlayMs = now
        lastSpokenMs = now

        val carrier = state.ball.ownerId?.let { id -> state.players.firstOrNull { it.id == id } }
        val inHomeAttackZone = if (state.homeAttacksRight) {
            state.ball.x > FootballEngine.FIELD_RIGHT - 56f
        } else {
            state.ball.x < FootballEngine.FIELD_LEFT + 56f
        }
        val inAwayAttackZone = if (state.homeAttacksRight) {
            state.ball.x < FootballEngine.FIELD_LEFT + 56f
        } else {
            state.ball.x > FootballEngine.FIELD_RIGHT - 56f
        }

        val line = when {
            carrier != null && carrier.teamSide == TeamSide.HOME && inHomeAttackZone -> {
                "هجمة خطيرة الآن... ${carrier.name} على مشارف منطقة جزاء ${state.awayTeam.nameAr}!"
            }
            carrier != null && carrier.teamSide == TeamSide.AWAY && inAwayAttackZone -> {
                "انتباه في الدفاع... ${carrier.name} يقترب من مرمى ${state.homeTeam.nameAr}."
            }
            carrier != null -> {
                val teamName = if (carrier.teamSide == TeamSide.HOME) state.homeTeam.nameAr else state.awayTeam.nameAr
                listOf(
                    "الكرة بحوزة ${carrier.name}، تحضير هادئ ومنظم من $teamName.",
                    "${carrier.name} يتقدم بالكرة، ويبحث عن زميل غير مراقب.",
                    "نقلات سريعة للاعبي $teamName في وسط الملعب."
                )[rng.nextInt(3)]
            }
            state.remainingSeconds <= 18f -> {
                "ندخل الدقائق الأخيرة من عمر اللقاء، والإثارة ما زالت حاضرة."
            }
            else -> {
                listOf(
                    "صراع تكتيكي قوي على الكرة في منتصف الملعب.",
                    "إيقاع سريع ومحاولة لفرض السيطرة من الفريقين."
                )[rng.nextInt(2)]
            }
        }
        speakInternal(line, urgent = false, pitch = 0.92f, rate = 0.96f)
    }

    private fun isTtsBusy(): Boolean {
        return try {
            tts?.isSpeaking == true
        } catch (_: Throwable) {
            false
        }
    }

    private fun speakInternal(text: String, urgent: Boolean, pitch: Float, rate: Float) {
        if (!isEnabled || effectiveVolume <= 0.01f) return

        // Always update visual ticker caption when commentary is active
        _activeCommentaryText.value = text
        captionClearJob?.cancel()
        captionClearJob = scope.launch {
            delay(3800L)
            if (_activeCommentaryText.value == text) {
                _activeCommentaryText.value = null
            }
        }

        if (!isReady) {
            if (urgent) pendingLine = QueuedLine(text, pitch, rate)
            return
        }

        try {
            val engine = tts ?: return
            if (!urgent && engine.isSpeaking) return
            engine.setPitch(pitch.coerceIn(0.82f, 1.08f))
            engine.setSpeechRate(rate.coerceIn(0.88f, 1.06f))
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, effectiveVolume.coerceIn(0f, 1f))
            }
            val mode = if (urgent) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            engine.speak(text, mode, params, "commentary_${System.currentTimeMillis()}")
        } catch (_: Throwable) {
            // Ignore TTS failure on headless devices
        }
    }

    fun stop() {
        captionClearJob?.cancel()
        _activeCommentaryText.value = null
        try {
            tts?.stop()
        } catch (_: Throwable) {
        }
    }

    fun release() {
        stop()
        scope.cancel()
        try {
            tts?.shutdown()
        } catch (_: Throwable) {
        }
        tts = null
    }
}
