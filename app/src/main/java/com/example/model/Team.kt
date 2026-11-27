package com.example.model

import androidx.compose.ui.graphics.Color

enum class CrestPattern {
    STRIPES,
    SASH,
    CHEVRON,
    HALF,
    STAR,
    HOOPS
}

data class Team(
    val id: String,
    val nameAr: String,
    val shortCode: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val attack: Int,
    val defense: Int,
    val speed: Int,
    val crestPattern: CrestPattern,
    val squad: List<SquadPlayer> // 16 players: 11 starters + 5 bench substitutes
) {
    val overall: Int get() = (attack + defense + speed) / 3
    val playerNames: List<String> get() = squad.map { it.name }
}

private fun buildSquad(
    teamId: String,
    names: List<String>,
    baseRating: Int,
    basePace: Int
): List<SquadPlayer> {
    val roles = listOf(
        PlayerRole.GK,
        PlayerRole.DEF, PlayerRole.DEF, PlayerRole.DEF, PlayerRole.DEF,
        PlayerRole.MID, PlayerRole.MID, PlayerRole.MID,
        PlayerRole.FWD, PlayerRole.FWD, PlayerRole.FWD,
        // 5 Bench Substitutes
        PlayerRole.GK, PlayerRole.DEF, PlayerRole.MID, PlayerRole.MID, PlayerRole.FWD
    )
    val numbers = listOf(1, 3, 4, 5, 2, 6, 8, 10, 11, 9, 7, 13, 15, 14, 18, 19)
    return names.mapIndexed { idx, name ->
        val role = roles.getOrElse(idx) { PlayerRole.MID }
        val isBench = idx >= 11
        val ratingOffset = if (isBench) -4 else ((idx % 3) - 1)
        val paceBoost = if (role == PlayerRole.FWD) 4 else if (role == PlayerRole.GK) -14 else 0
        SquadPlayer(
            id = "${teamId}_p$idx",
            name = name,
            jerseyNumber = numbers.getOrElse(idx) { idx + 1 },
            preferredRole = role,
            rating = (baseRating + ratingOffset).coerceIn(74, 96),
            pace = (basePace + paceBoost + (idx % 4) - 1).coerceIn(62, 97),
            shooting = (baseRating + (if (role == PlayerRole.FWD) 5 else -4)).coerceIn(60, 96),
            skinToneIndex = (idx + teamId.hashCode().let { if (it < 0) -it else it }) % 3,
            hairStyleIndex = (idx * 3 + teamId.length) % 4
        )
    }
}

object TeamCatalog {
    val teams: List<Team> = listOf(
        Team(
            id = "cairo_eagles",
            nameAr = "نسور القاهرة",
            shortCode = "CAI",
            primaryColor = Color(0xFFD51E2D),
            secondaryColor = Color(0xFFFFFFFF),
            accentColor = Color(0xFFFFC107),
            attack = 89,
            defense = 86,
            speed = 90,
            crestPattern = CrestPattern.STRIPES,
            squad = buildSquad(
                "cairo_eagles",
                listOf(
                    "الشناوي", "معلول", "عبد المنعم", "حجازي", "هاني",
                    "مروان", "عاشور", "قفشة", "تريزيجيه", "مصطفى", "صلاح",
                    "شوبير", "فتوح", "السولية", "زيزو", "مرموش"
                ),
                baseRating = 87, basePace = 88
            )
        ),
        Team(
            id = "madrid_royals",
            nameAr = "ملوك مدريد",
            shortCode = "MAD",
            primaryColor = Color(0xFFF5F7FA),
            secondaryColor = Color(0xFF1A2A5E),
            accentColor = Color(0xFFFFC107),
            attack = 93,
            defense = 88,
            speed = 91,
            crestPattern = CrestPattern.SASH,
            squad = buildSquad(
                "madrid_royals",
                listOf(
                    "كورتوا", "ميندي", "روديجر", "ميليتاو", "كارفاخال",
                    "تشواميني", "فالفيردي", "بيلينجهام", "فينيسيوس", "مبابي", "رودريجو",
                    "لونين", "ألابا", "كامافينجا", "مودريتش", "إندريك"
                ),
                baseRating = 90, basePace = 90
            )
        ),
        Team(
            id = "barca_stars",
            nameAr = "نجوم برشلونة",
            shortCode = "BAR",
            primaryColor = Color(0xFF0B4DA2),
            secondaryColor = Color(0xFFA50044),
            accentColor = Color(0xFFFFD100),
            attack = 92,
            defense = 85,
            speed = 90,
            crestPattern = CrestPattern.HALF,
            squad = buildSquad(
                "barca_stars",
                listOf(
                    "شتيجن", "بالدي", "أراوخو", "كوبارسي", "كوندي",
                    "دي يونج", "بيدري", "أولمو", "رافينيا", "ليفاندوفسكي", "يامال",
                    "بينا", "كريستنسن", "جافي", "فيرمين", "توريس"
                ),
                baseRating = 89, basePace = 89
            )
        ),
        Team(
            id = "riyadh_wolves",
            nameAr = "صقور الرياض",
            shortCode = "RIY",
            primaryColor = Color(0xFF007A3D),
            secondaryColor = Color(0xFFFFFFFF),
            accentColor = Color(0xFFF4C430),
            attack = 87,
            defense = 85,
            speed = 89,
            crestPattern = CrestPattern.CHEVRON,
            squad = buildSquad(
                "riyadh_wolves",
                listOf(
                    "بونو", "لودي", "كوليبالي", "البليهي", "كانسيلو",
                    "نيفيز", "سافيتش", "مالكوم", "الدوسري", "ميتروفيتش", "ليوناردو",
                    "العويس", "تمبكتي", "كنو", "الحمدان", "البريكان"
                ),
                baseRating = 86, basePace = 87
            )
        ),
        Team(
            id = "london_tigers",
            nameAr = "أسود لندن",
            shortCode = "LON",
            primaryColor = Color(0xFF034694),
            secondaryColor = Color(0xFF6CABDD),
            accentColor = Color(0xFFFFFFFF),
            attack = 88,
            defense = 90,
            speed = 87,
            crestPattern = CrestPattern.STAR,
            squad = buildSquad(
                "london_tigers",
                listOf(
                    "رايا", "كالافيوري", "جابرييل", "ساليبا", "وايت",
                    "رايس", "أوديجارد", "بالمر", "مارتينيلي", "هالاند", "ساكا",
                    "رامسديل", "تيمبر", "كوفاسيتش", "فودين", "هافيرتز"
                ),
                baseRating = 88, basePace = 86
            )
        ),
        Team(
            id = "casablanca_lions",
            nameAr = "أسود الأطلس",
            shortCode = "CAS",
            primaryColor = Color(0xFFC1272D),
            secondaryColor = Color(0xFF006233),
            accentColor = Color(0xFFFFD700),
            attack = 86,
            defense = 89,
            speed = 88,
            crestPattern = CrestPattern.STAR,
            squad = buildSquad(
                "casablanca_lions",
                listOf(
                    "المحمدي", "مزراوي", "أكرد", "سايس", "حكيمي",
                    "أمرابط", "أوناحي", "دياز", "بوفال", "النصيري", "زياش",
                    "تاجناوتي", "داري", "أملاح", "الخنوس", "رحيمي"
                ),
                baseRating = 86, basePace = 87
            )
        ),
        Team(
            id = "munich_falcons",
            nameAr = "عاصفة ميونخ",
            shortCode = "MUN",
            primaryColor = Color(0xFFDC052D),
            secondaryColor = Color(0xFF141821),
            accentColor = Color(0xFFFFFFFF),
            attack = 91,
            defense = 87,
            speed = 88,
            crestPattern = CrestPattern.HOOPS,
            squad = buildSquad(
                "munich_falcons",
                listOf(
                    "نوير", "ديفيز", "أوباميكانو", "كيم", "كيميتش",
                    "بافلوفيتش", "جوريتسكا", "موسيالا", "ساني", "كين", "أوليسي",
                    "أولرايش", "لايمر", "بالينيا", "جنابري", "كومان"
                ),
                baseRating = 89, basePace = 87
            )
        ),
        Team(
            id = "milano_blacks",
            nameAr = "أفاعي ميلانو",
            shortCode = "MIL",
            primaryColor = Color(0xFF0E131F),
            secondaryColor = Color(0xFF0066B2),
            accentColor = Color(0xFF00E5FF),
            attack = 88,
            defense = 91,
            speed = 85,
            crestPattern = CrestPattern.STRIPES,
            squad = buildSquad(
                "milano_blacks",
                listOf(
                    "سومر", "ديماركو", "باستوني", "أتشيربي", "دومفريس",
                    "باريلا", "تشالهان", "مخيتاريان", "تورام", "لاوتارو", "طارمي",
                    "مارتينيز", "دي فري", "فراتيسي", "زيلينسكي", "أرناتوفيتش"
                ),
                baseRating = 88, basePace = 85
            )
        )
    )

    fun findById(id: String): Team = teams.firstOrNull { it.id == id } ?: teams.first()
}
