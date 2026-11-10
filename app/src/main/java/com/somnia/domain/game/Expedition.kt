package com.somnia.domain.game

/** Région du monde des rêves que Brumi et les créatures du lit explorent pendant votre sommeil. */
data class DreamRegion(
    val id: String,
    val name: String,
    val steps: Int,
    val description: String,
    val chapterTitle: String,
    val chapter: String,
    val rewardStardust: Int,
    val accent: Long,
)

data class ExpeditionState(val regionIndex: Int = 0, val progress: Int = 0)

data class ExpeditionResult(
    val stepsGained: Int,
    val state: ExpeditionState,
    val completed: List<DreamRegion>,
    val stardust: Int,
)

object Expedition {
    val regions = listOf(
        DreamRegion("prairie", "La Prairie des Premiers Rêves", 3,
            "Une plaine d'herbes argentées où naissent les tout premiers rêves.",
            "Chapitre 1 · Le premier pas",
            "Au bord de la prairie, Brumi a trouvé une carte effacée par la rosée. On y devine un chemin qui s'enfonce vers une forêt, et tout au bout, une lumière qui ressemble à un phare. « Si l'on dort bien, la carte se dessine », murmure une Plumeline. Brumi range précieusement la carte.",
            60, 0xFFB9E3A8),
        DreamRegion("foret", "La Forêt de Mousse", 5,
            "Des arbres si doux qu'on peut s'y adosser pour dormir.",
            "Chapitre 2 · Les gardiens du silence",
            "Sous les arbres, les Mousselins veillent à ce que personne ne fasse de bruit. Ils apprennent à Brumi un secret : le sommeil se répare la nuit, mais il se prépare le soir. Un nouveau morceau de carte apparaît, montrant un océan immobile.",
            100, 0xFF7FD08A),
        DreamRegion("abysses", "Les Abysses Calmes", 7,
            "Un océan de sommeil profond où tout bouge lentement.",
            "Chapitre 3 · Le chant des profondeurs",
            "Au fond des Abysses, un Abyssin chante une berceuse si lente qu'elle dure une nuit entière. Brumi comprend que les nuits régulières creusent les rêves les plus profonds. Au loin, un orage gronde au-dessus des nuages.",
            150, 0xFF4F7BD0),
        DreamRegion("orage", "Le Ciel d'Orage", 9,
            "Les rêves agités s'y entrechoquent en petits éclairs.",
            "Chapitre 4 · Apprivoiser l'orage",
            "Les Orageons grondent, mais ils ont surtout peur du noir. Brumi leur montre comment souffler lentement, comme le soir avant de dormir. Le ciel s'apaise, et dévoile un archipel d'îles flottantes.",
            200, 0xFF9AA6C8),
        DreamRegion("archipel", "L'Archipel des Songes", 12,
            "Des îles flottantes reliées par des ponts de brume.",
            "Chapitre 5 · Les ponts de brume",
            "Chaque île est un rêve que quelqu'un a fait un jour. Les ponts ne tiennent que si l'on revient chaque nuit à la même heure. Brumi traverse le dernier pont : le phare est là, sa lumière tournant au rythme d'une respiration endormie.",
            280, 0xFFC7A8FF),
        DreamRegion("phare", "Le Phare de la Lune", 15,
            "Le cœur du monde des rêves, gardé par Onirion.",
            "Chapitre 6 · La lumière qui veille",
            "Au sommet du phare, Onirion attendait Brumi depuis longtemps. « Ce phare n'éclaire pas la mer, dit-il, il éclaire les nuits de ceux qui prennent soin de leur sommeil. » Brumi devient l'un de ses gardiens. Depuis, il explore librement tous les rêves, et chaque voyage rapporte un peu de poussière d'étoiles.",
            400, 0xFFFFD98A),
    )

    /** Après le phare : voyages libres, une récompense toutes les [FREE_ROAM_STEPS] étapes. */
    const val FREE_ROAM_STEPS = 10
    const val FREE_ROAM_STARDUST = 80

    /** Étapes gagnées : il faut vraiment dormir, et la régularité fait avancer plus vite. */
    fun stepsForNight(n: NightSummary): Int {
        if (n.totalSleepMin < 180) return 0
        var steps = 1
        if (n.regular) steps++
        if (n.score >= 75) steps++
        return steps
    }

    fun current(state: ExpeditionState): DreamRegion? = regions.getOrNull(state.regionIndex)

    fun advance(state: ExpeditionState, steps: Int): ExpeditionResult {
        var index = state.regionIndex
        var progress = state.progress + steps
        val completed = mutableListOf<DreamRegion>()
        var dust = 0
        while (true) {
            val region = regions.getOrNull(index)
            if (region == null) {
                // Voyages libres
                while (progress >= FREE_ROAM_STEPS) { progress -= FREE_ROAM_STEPS; dust += FREE_ROAM_STARDUST }
                break
            }
            if (progress < region.steps) break
            progress -= region.steps
            completed += region
            dust += region.rewardStardust
            index++
        }
        return ExpeditionResult(steps, ExpeditionState(index, progress), completed, dust)
    }
}
