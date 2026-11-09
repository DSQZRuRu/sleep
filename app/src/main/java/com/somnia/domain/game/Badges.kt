package com.somnia.domain.game

data class Badge(val id: String, val name: String, val description: String, val symbol: String)

data class BadgeContext(
    val nightsCount: Int,
    val streak: Int,
    val level: Int,
    val uniqueCreatures: Int,
    val totalCreatures: Int,
    val challengesCompleted: Int,
    val lastNight: NightSummary?,
    val companionStage: CompanionStage,
    val regularityScore: Int,
)

object BadgeCatalog {
    val all = listOf(
        Badge("first_night", "Première nuit", "Enregistrer votre première nuit", "☾"),
        Badge("streak3", "Petite routine", "3 nuits régulières d'affilée", "✦"),
        Badge("streak7", "Semaine parfaite", "7 nuits régulières d'affilée", "✶"),
        Badge("streak30", "Métronome", "30 nuits régulières d'affilée", "✺"),
        Badge("healthy_night", "Dans la zone", "Dormir entre 7 et 9 h", "◐"),
        Badge("deep_diver", "Plongeur des rêves", "Plus de 20 % de sommeil profond estimé", "≋"),
        Badge("collector5", "Carnet entamé", "Découvrir 5 créatures différentes", "❀"),
        Badge("collector_all", "Grand naturaliste", "Découvrir toutes les créatures du rêve", "✿"),
        Badge("level5", "Rêveur confirmé", "Atteindre le niveau 5", "★"),
        Badge("level10", "Maître des songes", "Atteindre le niveau 10", "✪"),
        Badge("challenge1", "Défi relevé", "Terminer un défi hebdomadaire", "⚑"),
        Badge("challenge10", "Inarrêtable", "Terminer 10 défis hebdomadaires", "⚐"),
        Badge("hatched", "Éclosion", "Faire éclore Brumi", "◍"),
        Badge("astral", "Poussière d'étoiles", "Faire évoluer Brumi en forme astrale", "✧"),
        Badge("regular80", "Horloge réglée", "Score de régularité de 80 ou plus", "⏾"),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }

    fun earned(ctx: BadgeContext): Set<String> = buildSet {
        if (ctx.nightsCount >= 1) add("first_night")
        if (ctx.streak >= 3) add("streak3")
        if (ctx.streak >= 7) add("streak7")
        if (ctx.streak >= 30) add("streak30")
        ctx.lastNight?.let {
            if (it.healthyDuration) add("healthy_night")
            if (it.deepRatio >= 0.2f && it.totalSleepMin >= 360) add("deep_diver")
        }
        if (ctx.uniqueCreatures >= 5) add("collector5")
        if (ctx.uniqueCreatures >= ctx.totalCreatures) add("collector_all")
        if (ctx.level >= 5) add("level5")
        if (ctx.level >= 10) add("level10")
        if (ctx.challengesCompleted >= 1) add("challenge1")
        if (ctx.challengesCompleted >= 10) add("challenge10")
        if (ctx.companionStage.ordinal >= CompanionStage.SPROUT.ordinal) add("hatched")
        if (ctx.companionStage == CompanionStage.ASTRAL) add("astral")
        if (ctx.regularityScore >= 80 && ctx.nightsCount >= 5) add("regular80")
    }
}
