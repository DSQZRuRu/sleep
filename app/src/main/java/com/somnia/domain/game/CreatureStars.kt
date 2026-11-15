package com.somnia.domain.game

/**
 * Étoiles des créatures (0 à 5) : chaque doublon rencontré la nuit fait progresser vers
 * l'étoile suivante. Les créatures fréquentes en demandent davantage. Chaque étoile ajoute
 * au bonus l'équivalent d'un niveau ; au-delà de 5 étoiles, les doublons redonnent de la poussière.
 */
object CreatureStars {
    const val MAX_STARS = 5

    /** Doublons nécessaires pour passer de l'étoile i à i+1 (index 0 → ★1). */
    fun requirements(rarity: Rarity): IntArray = when (rarity) {
        Rarity.COMMON -> intArrayOf(2, 3, 4, 6, 8)
        Rarity.RARE -> intArrayOf(1, 2, 3, 4, 6)
        Rarity.EPIC -> intArrayOf(1, 1, 2, 3, 4)
        Rarity.LEGENDARY -> intArrayOf(1, 1, 1, 2, 3)
    }

    /** Doublons encore nécessaires pour l'étoile suivante (null si 5 étoiles). */
    fun needed(rarity: Rarity, stars: Int): Int? = requirements(rarity).getOrNull(stars)

    data class Result(val stars: Int, val shards: Int, val overflow: Int, val gained: Int)

    /**
     * Ajoute [duplicates] doublons à une créature ayant [stars] étoiles et [shards] doublons en réserve.
     * @return nouvel état, doublons en trop (5 étoiles atteintes) et étoiles gagnées.
     */
    fun addDuplicates(rarity: Rarity, stars: Int, shards: Int, duplicates: Int): Result {
        var s = stars.coerceIn(0, MAX_STARS)
        var sh = shards
        var overflow = 0
        repeat(duplicates) {
            val need = needed(rarity, s)
            if (need == null) overflow++ else {
                sh++
                if (sh >= need) { s++; sh = 0 }
            }
        }
        return Result(s, if (s >= MAX_STARS) 0 else sh, overflow, s - stars.coerceIn(0, MAX_STARS))
    }

    fun display(stars: Int): String = "★".repeat(stars) + "☆".repeat(MAX_STARS - stars)
}
