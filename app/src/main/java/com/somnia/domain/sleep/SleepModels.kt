package com.somnia.domain.sleep

/** Phase estimée pour une fenêtre d'une minute. */
enum class SleepStage(val code: Int, val label: String) {
    AWAKE(0, "Éveil"),
    LIGHT(1, "Léger"),
    DEEP(2, "Profond");

    companion object {
        fun fromCode(code: Int): SleepStage = entries.firstOrNull { it.code == code } ?: LIGHT
    }
}

/** Mesures agrégées sur une minute pendant la nuit. */
data class EpochInput(
    val timestamp: Long,
    /** Nombre d'échantillons de l'accéléromètre au-dessus du bruit de fond. */
    val motionCount: Float,
    /** Événements sonores marquants (pics au-dessus du fond sonore). */
    val loudEvents: Int = 0,
    /** Le téléphone a été utilisé (écran allumé / déverrouillé). */
    val phoneUsed: Boolean = false,
)

/** Résultat de l'analyse complète d'une nuit. */
data class NightAnalysis(
    val stages: List<SleepStage>,
    val sleepOnsetIndex: Int?,
    val finalWakeIndex: Int?,
    val totalSleepMin: Int,
    val lightMin: Int,
    val deepMin: Int,
    val awakeMin: Int,
    val latencyMin: Int,
    val awakenings: Int,
    val efficiency: Float,
    /** Nombre de cycles léger → profond détectés (pour l'ajustement du profil). */
    val detectedCycles: Int,
) {
    val deepRatio: Float get() = if (totalSleepMin > 0) deepMin.toFloat() / totalSleepMin else 0f
    val restless: Boolean get() = awakenings >= 4 || (totalSleepMin > 0 && awakeMin.toFloat() / (totalSleepMin + awakeMin) > 0.2f)
}

enum class SleepType(val label: String) {
    LIGHT("Sommeil léger"),
    DEEP("Sommeil profond"),
    RESTLESS("Sommeil agité"),
}
