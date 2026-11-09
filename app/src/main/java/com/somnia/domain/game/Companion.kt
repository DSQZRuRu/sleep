package com.somnia.domain.game

/** Brumi, le compagnon de nuit : une petite créature de brume qui grandit avec vos nuits. */
enum class CompanionStage(val label: String, val careNeeded: Int, val description: String) {
    EGG("Œuf de brume", 0, "Un œuf tiède qui frémit doucement la nuit."),
    SPROUT("Brumi bébé", 60, "Il vient d'éclore et découvre le monde des rêves."),
    YOUNG("Brumi", 220, "Curieux, il explore les nuits à vos côtés."),
    GROWN("Grand Brumi", 520, "Serein, il veille sur vos nuits régulières."),
    ASTRAL("Brumi astral", 1000, "Une créature d'étoiles, née d'innombrables bonnes nuits.");

    val next: CompanionStage? get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun forCare(care: Int): CompanionStage = entries.last { care >= it.careNeeded }
    }
}

enum class CompanionMood(val label: String) {
    RADIANT("Rayonnant"), CALM("Serein"), SLEEPY("Ensommeillé"), TIRED("Fatigué")
}

object CompanionLogic {
    const val NAME = "Brumi"

    /** Points de soin gagnés pour une nuit : qualité + régularité. Jamais négatifs (pas de punition). */
    fun careForNight(score: Int, regular: Boolean, inHealthyRange: Boolean): Int {
        var care = score / 5
        if (regular) care += 12
        if (inHealthyRange) care += 8
        return care.coerceAtLeast(2)
    }

    fun mood(recentScores: List<Int>): CompanionMood {
        if (recentScores.isEmpty()) return CompanionMood.CALM
        val avg = recentScores.takeLast(3).average()
        return when {
            avg >= 80 -> CompanionMood.RADIANT
            avg >= 65 -> CompanionMood.CALM
            avg >= 50 -> CompanionMood.SLEEPY
            else -> CompanionMood.TIRED
        }
    }

    fun message(mood: CompanionMood): String = when (mood) {
        CompanionMood.RADIANT -> "$NAME scintille : vos nuits lui font un bien fou !"
        CompanionMood.CALM -> "$NAME est serein. Gardez ce rythme."
        CompanionMood.SLEEPY -> "$NAME baille… une nuit à l'heure lui ferait plaisir."
        CompanionMood.TIRED -> "$NAME est un peu fatigué. Ce soir, on se couche à l'heure ensemble ?"
    }
}
