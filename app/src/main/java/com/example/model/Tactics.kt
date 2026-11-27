package com.example.model

enum class FormationType(
    val displayName: String,
    val styleAr: String,
    val slots: List<FormationSlot>
) {
    F_4_3_3(
        displayName = "4-3-3",
        styleAr = "هجوم ضاغط",
        slots = listOf(
            FormationSlot("GK", PlayerRole.GK, 11f, 50f),
            FormationSlot("LB", PlayerRole.DEF, 31f, 18f),
            FormationSlot("CB", PlayerRole.DEF, 27f, 39f),
            FormationSlot("CB", PlayerRole.DEF, 27f, 61f),
            FormationSlot("RB", PlayerRole.DEF, 31f, 82f),
            FormationSlot("LCM", PlayerRole.MID, 52f, 25f),
            FormationSlot("CDM", PlayerRole.MID, 45f, 50f),
            FormationSlot("RCM", PlayerRole.MID, 52f, 75f),
            FormationSlot("LW", PlayerRole.FWD, 70f, 19f),
            FormationSlot("ST", PlayerRole.FWD, 76f, 50f),
            FormationSlot("RW", PlayerRole.FWD, 70f, 81f)
        )
    ),
    F_4_4_2(
        displayName = "4-4-2",
        styleAr = "توازن كلاسيكي",
        slots = listOf(
            FormationSlot("GK", PlayerRole.GK, 11f, 50f),
            FormationSlot("LB", PlayerRole.DEF, 30f, 18f),
            FormationSlot("CB", PlayerRole.DEF, 27f, 39f),
            FormationSlot("CB", PlayerRole.DEF, 27f, 61f),
            FormationSlot("RB", PlayerRole.DEF, 30f, 82f),
            FormationSlot("LM", PlayerRole.MID, 53f, 17f),
            FormationSlot("LCM", PlayerRole.MID, 49f, 39f),
            FormationSlot("RCM", PlayerRole.MID, 49f, 61f),
            FormationSlot("RM", PlayerRole.MID, 53f, 83f),
            FormationSlot("LST", PlayerRole.FWD, 75f, 38f),
            FormationSlot("RST", PlayerRole.FWD, 75f, 62f)
        )
    ),
    F_3_5_2(
        displayName = "3-5-2",
        styleAr = "سيطرة الوسط",
        slots = listOf(
            FormationSlot("GK", PlayerRole.GK, 11f, 50f),
            FormationSlot("LCB", PlayerRole.DEF, 28f, 27f),
            FormationSlot("CB", PlayerRole.DEF, 26f, 50f),
            FormationSlot("RCB", PlayerRole.DEF, 28f, 73f),
            FormationSlot("LWB", PlayerRole.MID, 49f, 14f),
            FormationSlot("CDM", PlayerRole.MID, 44f, 38f),
            FormationSlot("CAM", PlayerRole.MID, 58f, 50f),
            FormationSlot("CDM", PlayerRole.MID, 44f, 62f),
            FormationSlot("RWB", PlayerRole.MID, 49f, 86f),
            FormationSlot("LST", PlayerRole.FWD, 75f, 38f),
            FormationSlot("RST", PlayerRole.FWD, 75f, 62f)
        )
    ),
    F_4_2_3_1(
        displayName = "4-2-3-1",
        styleAr = "تكتيك حديث",
        slots = listOf(
            FormationSlot("GK", PlayerRole.GK, 11f, 50f),
            FormationSlot("LB", PlayerRole.DEF, 31f, 18f),
            FormationSlot("CB", PlayerRole.DEF, 27f, 39f),
            FormationSlot("CB", PlayerRole.DEF, 27f, 61f),
            FormationSlot("RB", PlayerRole.DEF, 31f, 82f),
            FormationSlot("LDM", PlayerRole.MID, 45f, 37f),
            FormationSlot("RDM", PlayerRole.MID, 45f, 63f),
            FormationSlot("LAM", PlayerRole.MID, 63f, 20f),
            FormationSlot("CAM", PlayerRole.MID, 63f, 50f),
            FormationSlot("RAM", PlayerRole.MID, 63f, 80f),
            FormationSlot("ST", PlayerRole.FWD, 77f, 50f)
        )
    ),
    F_5_3_2(
        displayName = "5-3-2",
        styleAr = "دفاع ومرتدات",
        slots = listOf(
            FormationSlot("GK", PlayerRole.GK, 11f, 50f),
            FormationSlot("LWB", PlayerRole.DEF, 33f, 14f),
            FormationSlot("LCB", PlayerRole.DEF, 25f, 32f),
            FormationSlot("CB", PlayerRole.DEF, 24f, 50f),
            FormationSlot("RCB", PlayerRole.DEF, 25f, 68f),
            FormationSlot("RWB", PlayerRole.DEF, 33f, 86f),
            FormationSlot("LCM", PlayerRole.MID, 49f, 29f),
            FormationSlot("CM", PlayerRole.MID, 47f, 50f),
            FormationSlot("RCM", PlayerRole.MID, 49f, 71f),
            FormationSlot("LST", PlayerRole.FWD, 74f, 37f),
            FormationSlot("RST", PlayerRole.FWD, 74f, 63f)
        )
    )
}

data class FormationSlot(
    val code: String,
    val role: PlayerRole,
    val x: Float,
    val y: Float
)

data class SquadPlayer(
    val id: String,
    val name: String,
    val jerseyNumber: Int,
    val preferredRole: PlayerRole,
    val rating: Int,
    val pace: Int,
    val shooting: Int,
    val skinToneIndex: Int,
    val hairStyleIndex: Int
)
