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
    val equippedCount: Int = 0,
    val maxCreatureLevel: Int = 1,
    val bestDream: Int = 0,
    val eventCreatureIds: Set<String> = emptySet(),
    val regionsCompleted: Int = 0,
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
        Badge("first_bed", "Bien accompagné", "Installer une créature dans votre lit", "♡"),
        Badge("full_bed", "Lit complet", "Remplir les 3 places du lit", "❖"),
        Badge("level5_creature", "Dresseur de rêves", "Monter une créature au niveau 5", "⬆"),
        Badge("level10_creature", "Créature légendaire", "Monter une créature au niveau 10", "⇮"),
        Badge("dream120", "Éclat éblouissant", "Atteindre un Éclat de rêve de 120", "✹"),
        Badge("event_givres", "Esprit des Givres", "Rencontrer une créature de la Nuit des Givres", "❄"),
        Badge("event_lueurs", "Porte-lueur", "Rencontrer une créature de la Nuit des Lueurs", "✺"),
        Badge("event_eveil", "Premier bourgeon", "Rencontrer une créature de l'Éveil des Songes", "❀"),
        Badge("event_courtes", "Gardien du solstice", "Rencontrer une créature des Nuits Courtes", "☀"),
        Badge("event_pleinelune", "Enfant de la lune", "Rencontrer Opaline une nuit de pleine lune", "◯"),
        Badge("explorer1", "Premier pas", "Terminer la première région de l'expédition", "⚲"),
        Badge("explorer_all", "Gardien du phare", "Atteindre le Phare de la Lune", "⛯"),
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
        if (ctx.uniqueCreatures >= ctx.totalCreatures) add("collector_all") // créatures permanentes
        if (ctx.level >= 5) add("level5")
        if (ctx.level >= 10) add("level10")
        if (ctx.challengesCompleted >= 1) add("challenge1")
        if (ctx.challengesCompleted >= 10) add("challenge10")
        if (ctx.companionStage.ordinal >= CompanionStage.SPROUT.ordinal) add("hatched")
        if (ctx.companionStage == CompanionStage.ASTRAL) add("astral")
        if (ctx.regularityScore >= 80 && ctx.nightsCount >= 5) add("regular80")
        if (ctx.equippedCount >= 1) add("first_bed")
        if (ctx.equippedCount >= 3) add("full_bed")
        if (ctx.maxCreatureLevel >= 5) add("level5_creature")
        if (ctx.maxCreatureLevel >= 10) add("level10_creature")
        if (ctx.bestDream >= 120) add("dream120")
        if (ctx.regionsCompleted >= 1) add("explorer1")
        if (ctx.regionsCompleted >= Expedition.regions.size) add("explorer_all")
        ctx.eventCreatureIds.mapNotNull { DreamCreatureCatalog.byId(it)?.eventId }.toSet().forEach { add("event_$it") }
    }
}
