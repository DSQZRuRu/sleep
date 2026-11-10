package com.somnia.domain.game

/** Emplacements de la chambre, dans l'ordre d'affichage (du fond vers l'avant). */
enum class DecorSlot(val label: String) {
    WALL("Mur"), CURTAINS("Rideaux"), RUG("Tapis"), LAMP("Veilleuse"), PLANT("Plante"), QUILT("Couette"), MOBILE("Mobile"),
}

/**
 * Objet de décoration. Le bonus, quand il existe, reste petit et conditionné comme
 * celui des créatures : il récompense une habitude saine, jamais le temps passé au lit.
 */
data class DecorItem(
    val id: String,
    val name: String,
    val slot: DecorSlot,
    val price: Int,
    val description: String,
    val bonus: BonusKind? = null,
    val bonusPct: Int = 0,
) {
    val isDefault: Boolean get() = price == 0
}

object DecorCatalog {
    val all = listOf(
        // Couettes
        DecorItem("quilt_moons", "Couette des lunes", DecorSlot.QUILT, 0, "La couette d'origine, semée de petits croissants."),
        DecorItem("quilt_dawn", "Couette aurore", DecorSlot.QUILT, 180, "Rose et lilas, comme le ciel juste avant le réveil.", BonusKind.WAKE_ON_TIME, 2),
        DecorItem("quilt_forest", "Couette forêt", DecorSlot.QUILT, 180, "Un sous-bois douillet tissé de feuilles.", BonusKind.EFFICIENCY, 2),
        DecorItem("quilt_stars", "Couette étoilée", DecorSlot.QUILT, 400, "Bleu nuit brodé d'or.", BonusKind.DEEP_SLEEP, 3),
        // Rideaux
        DecorItem("curtains_lilac", "Rideaux lilas", DecorSlot.CURTAINS, 0, "Les rideaux d'origine."),
        DecorItem("curtains_velvet", "Rideaux de velours", DecorSlot.CURTAINS, 150, "Épais et sombres : la chambre reste noire jusqu'au matin.", BonusKind.DEEP_SLEEP, 2),
        DecorItem("curtains_peach", "Rideaux d'aube", DecorSlot.CURTAINS, 150, "Ils laissent entrer la lumière du matin, alliée du lever.", BonusKind.WAKE_ON_TIME, 2),
        // Veilleuses
        DecorItem("lamp_classic", "Petite lampe", DecorSlot.LAMP, 0, "La veilleuse d'origine."),
        DecorItem("lamp_paper", "Lanterne de papier", DecorSlot.LAMP, 120, "Une lumière ambrée et douce, sans lumière bleue.", BonusKind.ON_TIME_BED, 2),
        DecorItem("lamp_fireflies", "Bocal à lucioles", DecorSlot.LAMP, 260, "Des lucioles qui s'endorment avec vous.", BonusKind.STARDUST, 3),
        // Plantes
        DecorItem("plant_sprout", "Jeune pousse", DecorSlot.PLANT, 0, "La plante d'origine."),
        DecorItem("plant_fern", "Fougère", DecorSlot.PLANT, 90, "Un peu de verdure apaisante."),
        DecorItem("plant_mushrooms", "Champignons lumineux", DecorSlot.PLANT, 220, "Ils brillent faiblement dans le noir.", BonusKind.STREAK, 2),
        // Tapis
        DecorItem("rug_none", "Plancher nu", DecorSlot.RUG, 0, "Rien au sol."),
        DecorItem("rug_moon", "Tapis lune", DecorSlot.RUG, 110, "Un tapis rond et moelleux."),
        DecorItem("rug_cloud", "Tapis nuage", DecorSlot.RUG, 200, "On y pose les pieds en douceur au réveil.", BonusKind.WAKE_ON_TIME, 2),
        // Mobiles
        DecorItem("mobile_none", "Aucun mobile", DecorSlot.MOBILE, 0, "Rien au-dessus du lit."),
        DecorItem("mobile_stars", "Mobile d'étoiles", DecorSlot.MOBILE, 160, "Des étoiles qui tournent lentement au-dessus du lit."),
        DecorItem("mobile_dreamcatcher", "Attrape-rêves", DecorSlot.MOBILE, 300, "Il retient les rêves agités.", BonusKind.COMEBACK, 3),
        // Mur
        DecorItem("wall_none", "Mur nu", DecorSlot.WALL, 0, "Le papier peint étoilé d'origine."),
        DecorItem("wall_garland", "Guirlande lumineuse", DecorSlot.WALL, 140, "De petites lumières chaudes le long du mur.", BonusKind.STARDUST, 2),
        DecorItem("wall_constellations", "Constellations", DecorSlot.WALL, 240, "Vos propres constellations peintes au mur.", BonusKind.HEALTHY_DURATION, 2),
    )

    fun byId(id: String?) = all.firstOrNull { it.id == id }
    fun ofSlot(slot: DecorSlot) = all.filter { it.slot == slot }
    fun default(slot: DecorSlot) = all.first { it.slot == slot && it.isDefault }

    /** Disposition complète : l'objet choisi pour chaque emplacement, ou celui d'origine. */
    fun layout(chosen: Map<DecorSlot, String>): Map<DecorSlot, DecorItem> =
        DecorSlot.entries.associateWith { slot -> byId(chosen[slot])?.takeIf { it.slot == slot } ?: default(slot) }
}
