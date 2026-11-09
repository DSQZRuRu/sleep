package com.somnia.domain.game

import kotlin.math.roundToInt

/**
 * Bonus des créatures installées dans le lit. Chacun ne s'applique que si la nuit
 * remplit une condition de sommeil SAIN : on ne gagne jamais rien à rester au lit éveillé.
 */
enum class BonusKind(val label: String, val condition: String, val symbol: String) {
    ON_TIME_BED("Coucher à l'heure", "si vous vous couchez à ±30 min de l'heure idéale", "☾"),
    HEALTHY_DURATION("Juste assez", "si vous dormez entre 7 et 9 h", "◐"),
    DEEP_SLEEP("Abysses", "si au moins 15 % de la nuit est en sommeil profond", "≋"),
    EFFICIENCY("Au lit pour dormir", "si votre efficacité de sommeil dépasse 85 %", "✓"),
    WAKE_ON_TIME("Lever constant", "si vous vous levez à ±30 min de l'heure prévue", "☀"),
    STREAK("Élan", "augmente avec votre série (jusqu'à 7 nuits)", "🔥"),
    COMEBACK("Rebond", "si votre score dépasse celui de la nuit précédente", "↗"),
    STARDUST("Poussière d'étoiles", "augmente la poussière gagnée chaque nuit", "✦"),
}

object CreatureProgression {
    const val MAX_LEVEL = 10

    fun basePct(rarity: Rarity): Int = when (rarity) {
        Rarity.COMMON -> 4
        Rarity.RARE -> 6
        Rarity.EPIC -> 8
        Rarity.LEGENDARY -> 12
    }

    /** Bonus en % au niveau donné : +¼ du bonus de base par niveau (au moins +1). */
    fun bonusPct(creature: DreamCreature, level: Int): Int {
        val base = basePct(creature.rarity)
        val perLevel = maxOf(1, base / 4)
        return base + perLevel * (level.coerceIn(1, MAX_LEVEL) - 1)
    }

    private fun rarityFactor(r: Rarity) = when (r) {
        Rarity.COMMON -> 1.0
        Rarity.RARE -> 1.3
        Rarity.EPIC -> 1.6
        Rarity.LEGENDARY -> 2.0
    }

    /** Coût en poussière d'étoiles pour passer du niveau [level] au suivant. */
    fun upgradeCost(creature: DreamCreature, level: Int): Int =
        ((50 + 40 * (level - 1)) * rarityFactor(creature.rarity)).roundToInt()

    /** Une créature déjà connue rencontrée à nouveau se dissout en poussière. */
    fun duplicateStardust(rarity: Rarity): Int = when (rarity) {
        Rarity.COMMON -> 10
        Rarity.RARE -> 20
        Rarity.EPIC -> 35
        Rarity.LEGENDARY -> 60
    }

    /** Emplacements du lit débloqués selon le niveau du joueur. */
    fun bedSlots(playerLevel: Int): Int = 1 + (if (playerLevel >= 3) 1 else 0) + (if (playerLevel >= 6) 1 else 0)
    fun levelForSlot(slot: Int): Int = when (slot) { 0 -> 1; 1 -> 3; else -> 6 }

    /** Bonus permanent de Brumi selon son stade (il dort toujours dans le lit). */
    fun companionPct(stage: CompanionStage): Int = when (stage) {
        CompanionStage.EGG -> 0
        CompanionStage.SPROUT -> 3
        CompanionStage.YOUNG -> 6
        CompanionStage.GROWN -> 10
        CompanionStage.ASTRAL -> 15
    }
}

data class EquippedCreature(val creature: DreamCreature, val level: Int)

data class DreamLine(val label: String, val pct: Int, val active: Boolean)

data class DreamScore(
    val base: Int,
    val lines: List<DreamLine>,
    val total: Int,
    val stardust: Int,
    val stardustLines: List<DreamLine>,
) {
    val bonusPct: Int get() = lines.filter { it.active }.sumOf { it.pct }
}

/**
 * Éclat de rêve = score de sommeil × (1 + bonus actifs).
 * La poussière d'étoiles gagnée découle de l'Éclat.
 */
object DreamScoreCalculator {

    fun conditionMet(kind: BonusKind, n: NightSummary, previousScore: Int?): Boolean = when (kind) {
        BonusKind.ON_TIME_BED -> kotlin.math.abs(n.bedDeviation) <= 30
        BonusKind.HEALTHY_DURATION -> n.healthyDuration
        BonusKind.DEEP_SLEEP -> n.deepRatio >= 0.15f
        BonusKind.EFFICIENCY -> n.efficiency >= 0.85f
        BonusKind.WAKE_ON_TIME -> n.wokeOnTime
        BonusKind.STREAK -> true
        BonusKind.COMEBACK -> previousScore != null && n.score > previousScore
        BonusKind.STARDUST -> false // ne touche que la poussière
    }

    fun compute(
        night: NightSummary,
        previousScore: Int?,
        streak: Int,
        equipped: List<EquippedCreature>,
        companion: CompanionStage,
        event: SeasonalEvent?,
    ): DreamScore {
        val base = night.score
        // Pas de vrai sommeil, pas de bonus.
        val slept = night.totalSleepMin >= 180
        val lines = mutableListOf<DreamLine>()
        val dustLines = mutableListOf<DreamLine>()

        CreatureProgression.companionPct(companion).takeIf { it > 0 }?.let {
            lines += DreamLine("${CompanionLogic.NAME} (${companion.label})", it, slept)
        }
        for (e in equipped) {
            val c = e.creature
            var pct = CreatureProgression.bonusPct(c, e.level)
            val eventBoost = event != null && c.eventId == event.id
            if (eventBoost) pct *= 2
            val suffix = if (eventBoost) " · événement ×2" else ""
            if (c.bonus == BonusKind.STREAK) pct = pct * streak.coerceAtMost(7) / 7
            if (c.bonus == BonusKind.STARDUST) {
                dustLines += DreamLine("${c.name} niv. ${e.level}$suffix", pct, slept)
            } else {
                lines += DreamLine("${c.name} niv. ${e.level} · ${c.bonus.label}$suffix", pct,
                    slept && pct > 0 && conditionMet(c.bonus, night, previousScore))
            }
        }
        synergyPct(equipped)?.let { (label, pct) -> lines += DreamLine(label, pct, slept) }

        val total = (base * (1 + lines.filter { it.active }.sumOf { it.pct } / 100.0)).toInt()
        event?.let { dustLines += DreamLine("Événement : ${it.name}", ((it.stardustMultiplier - 1) * 100).toInt(), slept) }
        val dust = if (!slept) 0 else (total * (1 + dustLines.filter { it.active }.sumOf { it.pct } / 100.0)).toInt()
        return DreamScore(base, lines, total, dust, dustLines)
    }

    /** Harmonie : plusieurs créatures du même type de sommeil dans le lit. */
    fun synergyPct(equipped: List<EquippedCreature>): Pair<String, Int>? {
        val best = equipped.groupingBy { it.creature.type }.eachCount().maxByOrNull { it.value } ?: return null
        return when (best.value) {
            2 -> "Harmonie ${best.key.label.lowercase()} (×2)" to 3
            in 3..Int.MAX_VALUE -> "Harmonie ${best.key.label.lowercase()} (×3)" to 8
            else -> null
        }
    }
}
