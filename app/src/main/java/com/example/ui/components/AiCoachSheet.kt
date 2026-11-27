package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ai.AiCoachUiState
import com.example.model.FormationType
import com.example.model.Team
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.DividerSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StadiumCard
import com.example.ui.theme.StadiumElevated
import com.example.ui.theme.StadiumObsidian
import com.example.ui.theme.TurfEmerald

@Composable
fun AiCoachOverlay(
    homeTeam: Team,
    awayTeam: Team,
    formation: FormationType,
    aiState: AiCoachUiState,
    onAskQuestion: (String) -> Unit,
    onRetryLast: () -> Unit,
    onClose: () -> Unit
) {
    var questionText by remember { mutableStateOf("") }

    val quickPrompts = remember(homeTeam.id, awayTeam.id, formation) {
        listOf(
            "أفضل خطة للفوز على ${awayTeam.nameAr}" to "اقترح لي أفضل خطة وأسلوب لعب لفريقي ${homeTeam.nameAr} بتشكيلة ${formation.displayName} للفوز على ${awayTeam.nameAr}.",
            "كيف أوقف هجومهم؟" to "كيف أدافع وأقطع الكرة أمام هجوم ${awayTeam.nameAr} (قوة هجومه ${awayTeam.attack}) باستخدام التمرير والضغط؟",
            "اختيار التبديلات" to "متى أستخدم التبديلات الخمسة وما المراكز الأهم لتنشيطها في فريقي ${homeTeam.nameAr}؟"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.80f))
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(24.dp),
            color = StadiumCard,
            border = BorderStroke(1.5.dp, ElectricCyan.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ElectricCyan.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "المدرب الذكي",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${homeTeam.nameAr} × ${awayTeam.nameAr} (${formation.displayName})",
                                style = MaterialTheme.typography.labelMedium,
                                color = TurfEmerald
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("ai_coach_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق المدرب الذكي",
                            tint = Color.White
                        )
                    }
                }

                // Quick Tactical Prompt Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickPrompts.forEachIndexed { idx, (label, prompt) ->
                        Surface(
                            onClick = { onAskQuestion(prompt) },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("ai_quick_prompt_$idx"),
                            shape = RoundedCornerShape(14.dp),
                            color = StadiumElevated,
                            border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.45f))
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ChampionshipGold
                                )
                            }
                        }
                    }
                }

                // Response Area
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = StadiumObsidian.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, DividerSubtle)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (aiState) {
                            AiCoachUiState.Idle -> {
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = ElectricCyan.copy(alpha = 0.8f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "اسأل المدرب الذكي عن خطة المباراة أو اضغط على أحد الاقتراحات بالأعلى.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            AiCoachUiState.Loading -> {
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(color = ElectricCyan)
                                    Text(
                                        text = "جاري تحليل التشكيلة وإعداد الخطة...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                            }

                            is AiCoachUiState.Streaming -> {
                                Text(
                                    text = aiState.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White,
                                    modifier = Modifier.testTag("ai_response_text")
                                )
                            }

                            is AiCoachUiState.Success -> {
                                Text(
                                    text = aiState.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White,
                                    modifier = Modifier.testTag("ai_response_text")
                                )
                            }

                            is AiCoachUiState.Error -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = CrimsonAlert,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = aiState.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = CrimsonAlert,
                                        modifier = Modifier.testTag("ai_error_text")
                                    )
                                    Button(
                                        onClick = onRetryLast,
                                        modifier = Modifier.height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = TurfEmerald,
                                            contentColor = Color(0xFF042111)
                                        )
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("إعادة المحاولة")
                                    }
                                }
                            }
                        }
                    }
                }

                // Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        placeholder = {
                            Text(
                                text = "اكتب سؤالك التكتيكي هنا...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_question_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = DividerSubtle,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    FilledIconButton(
                        onClick = {
                            val q = questionText.trim()
                            if (q.isNotEmpty()) {
                                questionText = ""
                                onAskQuestion(q)
                            }
                        },
                        enabled = questionText.isNotBlank() && aiState !is AiCoachUiState.Loading,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("ai_send_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF002026)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال السؤال"
                        )
                    }
                }
            }
        }
    }
}
