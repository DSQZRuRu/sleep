package com.somnia.domain.sleep

/**
 * Suivi automatique : trouve le début réel de la nuit. Les périodes de sommeil séparées par moins
 * de [MERGE_GAP] minutes d'éveil forment un bloc ; on garde le bloc le plus long (la vraie nuit),
 * et la nuit commence au dernier usage du téléphone qui le précède. Ainsi, un téléphone posé
 * immobile sur la table de nuit en début de soirée ne compte pas comme du sommeil.
 */
object AutoNightTrimmer {
    const val MERGE_GAP = 20

    /** Index de la première minute à garder (0 si rien à couper, null s'il n'y a pas de sommeil). */
    fun startIndex(stages: List<SleepStage>, phoneUsed: List<Boolean>): Int? {
        data class Block(var start: Int, var end: Int, var sleep: Int)
        val blocks = mutableListOf<Block>()
        var i = 0
        while (i < stages.size) {
            if (stages[i] == SleepStage.AWAKE) { i++; continue }
            var j = i
            while (j + 1 < stages.size && stages[j + 1] != SleepStage.AWAKE) j++
            val last = blocks.lastOrNull()
            if (last != null && i - last.end - 1 < MERGE_GAP) {
                last.end = j; last.sleep += j - i + 1
            } else blocks += Block(i, j, j - i + 1)
            i = j + 1
        }
        val main = blocks.maxByOrNull { it.sleep } ?: return null
        val lastUse = (main.start downTo 0).firstOrNull { phoneUsed.getOrElse(it) { false } }
        return lastUse ?: if (main.start > 0) main.start - 1 else 0
    }
}
