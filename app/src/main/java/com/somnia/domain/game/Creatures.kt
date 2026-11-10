package com.somnia.domain.game

import com.somnia.domain.sleep.SleepType

enum class Rarity(val label: String, val weight: Int) {
    COMMON("Commune", 60), RARE("Rare", 28), EPIC("Épique", 10), LEGENDARY("Légendaire", 2)
}

enum class Feature { EARS, HORNS, WINGS, ANTENNA, TAIL, SPOTS, CROWN, FINS }

/** Apparence procédurale : les créatures sont dessinées en Canvas, aucune image externe. */
data class CreatureLook(
    val body: Long,
    val accent: Long,
    val glow: Long,
    val features: Set<Feature>,
    val roundness: Float = 1f,
    val eyeStyle: Int = 0,
)

data class DreamCreature(
    val id: String,
    val name: String,
    val type: SleepType,
    val rarity: Rarity,
    val lore: String,
    val look: CreatureLook,
    /** Bonus apporté quand la créature est installée dans le lit. */
    val bonus: BonusKind = BonusKind.HEALTHY_DURATION,
    /** Créature exclusive à un événement saisonnier (null = toute l'année). */
    val eventId: String? = null,
)

/** Créatures du rêve — univers entièrement original de Somnia. */
object DreamCreatureCatalog {
    val all: List<DreamCreature> = listOf(
        // — Sommeil léger : créatures aériennes et vaporeuses —
        DreamCreature("plumeline", "Plumeline", SleepType.LIGHT, Rarity.COMMON,
            "Une boule de duvet qui flotte au-dessus des oreillers et compte les respirations.",
            CreatureLook(0xFFB9C7FF, 0xFFE6EBFF, 0xFF8FA4FF, setOf(Feature.WINGS)), bonus = BonusKind.ON_TIME_BED),
        DreamCreature("frissonnette", "Frissonnette", SleepType.LIGHT, Rarity.COMMON,
            "Elle naît du froissement des draps et disparaît au premier rayon du matin.",
            CreatureLook(0xFFA6E3E9, 0xFFDDF7F9, 0xFF6CC5CF, setOf(Feature.ANTENNA), 0.9f), bonus = BonusKind.WAKE_ON_TIME),
        DreamCreature("veilleuse", "Veilleuse", SleepType.LIGHT, Rarity.COMMON,
            "Une lucciole timide qui garde une petite lumière pour les rêveurs.",
            CreatureLook(0xFFFFE29A, 0xFFFFF3CF, 0xFFFFC94D, setOf(Feature.ANTENNA, Feature.WINGS), 1.1f, 1), bonus = BonusKind.STARDUST),
        DreamCreature("brumelot", "Brumelot", SleepType.LIGHT, Rarity.RARE,
            "Fait de brume d'aube, il glisse entre deux rêves sans jamais se poser.",
            CreatureLook(0xFFC8D6E5, 0xFFF1F5FA, 0xFF9DB4CC, setOf(Feature.TAIL, Feature.EARS), 0.85f), bonus = BonusKind.EFFICIENCY),
        DreamCreature("songelune", "Songelune", SleepType.LIGHT, Rarity.EPIC,
            "Porte un croissant de lune sur la tête et chuchote des rêves légers.",
            CreatureLook(0xFFD7C4FF, 0xFFF3ECFF, 0xFFB38CFF, setOf(Feature.CROWN, Feature.WINGS), 1f, 1), bonus = BonusKind.STREAK),

        // — Sommeil profond : créatures des abysses tranquilles —
        DreamCreature("dormouflon", "Dormouflon", SleepType.DEEP, Rarity.COMMON,
            "Un petit bélier de nuage, si lourd de sommeil qu'il ne bouge plus d'un poil.",
            CreatureLook(0xFF6C7BD9, 0xFFC5CCFF, 0xFF4656C2, setOf(Feature.HORNS), 1.15f, 1), bonus = BonusKind.DEEP_SLEEP),
        DreamCreature("abyssin", "Abyssin", SleepType.DEEP, Rarity.COMMON,
            "Il nage dans l'océan calme du sommeil profond, ses nageoires à peine visibles.",
            CreatureLook(0xFF3D5A98, 0xFF8FB0F0, 0xFF2A4580, setOf(Feature.FINS, Feature.TAIL), 1f), bonus = BonusKind.HEALTHY_DURATION),
        DreamCreature("mousselin", "Mousselin", SleepType.DEEP, Rarity.RARE,
            "Doux comme la mousse des forêts, il pousse là où l'on dort sans bruit.",
            CreatureLook(0xFF4E8F7A, 0xFFA8DCC9, 0xFF2F6E5B, setOf(Feature.SPOTS, Feature.EARS), 1.1f, 1), bonus = BonusKind.EFFICIENCY),
        DreamCreature("lentorne", "Lentorne", SleepType.DEEP, Rarity.EPIC,
            "Une créature cornue qui ne se montre qu'aux dormeurs réguliers.",
            CreatureLook(0xFF5B3F9E, 0xFFC0A8F5, 0xFF3E2780, setOf(Feature.HORNS, Feature.SPOTS, Feature.TAIL), 1.05f), bonus = BonusKind.ON_TIME_BED),
        DreamCreature("onirion", "Onirion", SleepType.DEEP, Rarity.LEGENDARY,
            "Le gardien des grandes profondeurs. Le croiser porte chance pour toute la semaine.",
            CreatureLook(0xFF1F2E6B, 0xFFFFD98A, 0xFF7B8CFF, setOf(Feature.CROWN, Feature.WINGS, Feature.TAIL), 1.2f, 1), bonus = BonusKind.DEEP_SLEEP),

        // — Sommeil agité : créatures espiègles —
        DreamCreature("tourbillot", "Tourbillot", SleepType.RESTLESS, Rarity.COMMON,
            "Il tourne sur lui-même toute la nuit et emmêle les couvertures.",
            CreatureLook(0xFFF2A07B, 0xFFFFD6C2, 0xFFE07A50, setOf(Feature.ANTENNA, Feature.TAIL), 0.9f, 2), bonus = BonusKind.COMEBACK),
        DreamCreature("grignoton", "Grignoton", SleepType.RESTLESS, Rarity.COMMON,
            "Un rongeur de pensées qui adore les soirées trop chargées.",
            CreatureLook(0xFFD99A6C, 0xFFF5D3B8, 0xFFB5733F, setOf(Feature.EARS, Feature.SPOTS), 1f, 2), bonus = BonusKind.HEALTHY_DURATION),
        DreamCreature("cliquetis", "Cliquetis", SleepType.RESTLESS, Rarity.RARE,
            "Il tapote doucement l'oreiller pour rappeler qu'un rituel apaisant aide à dormir.",
            CreatureLook(0xFFE8C468, 0xFFFFEDB8, 0xFFC99A2E, setOf(Feature.HORNS, Feature.ANTENNA), 0.95f, 2), bonus = BonusKind.WAKE_ON_TIME),
        DreamCreature("orageon", "Orageon", SleepType.RESTLESS, Rarity.EPIC,
            "Un petit nuage d'orage, plus grondeur que méchant.",
            CreatureLook(0xFF7D8AA6, 0xFFD5DCEB, 0xFF5A6785, setOf(Feature.WINGS, Feature.SPOTS), 1.1f, 2), bonus = BonusKind.COMEBACK),
    )


    // — Créatures d'événements saisonniers —
    val eventCreatures: List<DreamCreature> = listOf(
        DreamCreature("givrelin", "Givrelin", SleepType.LIGHT, Rarity.RARE,
            "Un petit esprit de givre qui dessine des étoiles sur les vitres pendant les nuits d'hiver.",
            CreatureLook(0xFFD6F0FF, 0xFFFFFFFF, 0xFF9FD8FF, setOf(Feature.CROWN, Feature.ANTENNA), 1f, 1),
            BonusKind.ON_TIME_BED, "givres"),
        DreamCreature("flocondor", "Flocondor", SleepType.DEEP, Rarity.EPIC,
            "Il dort roulé en boule sous la neige et ne se réveille qu'au printemps.",
            CreatureLook(0xFF8FB8E8, 0xFFF2F8FF, 0xFF5E8FD0, setOf(Feature.EARS, Feature.TAIL, Feature.SPOTS), 1.2f, 1),
            BonusKind.DEEP_SLEEP, "givres"),
        DreamCreature("lanternelle", "Lanternelle", SleepType.LIGHT, Rarity.LEGENDARY,
            "Elle porte la lumière des fêtes d'une maison endormie à l'autre.",
            CreatureLook(0xFFFFC46B, 0xFFFFF0C8, 0xFFFF9F3D, setOf(Feature.WINGS, Feature.CROWN), 1f, 1),
            BonusKind.STARDUST, "givres"),
        DreamCreature("citrouillon", "Citrouillon", SleepType.RESTLESS, Rarity.RARE,
            "Il fait semblant d'être effrayant, mais s'endort dès qu'on éteint la lumière.",
            CreatureLook(0xFFFF9A3C, 0xFFFFD29E, 0xFFE0701A, setOf(Feature.ANTENNA, Feature.SPOTS), 1.15f, 2),
            BonusKind.COMEBACK, "lueurs"),
        DreamCreature("brumoire", "Brumoire", SleepType.DEEP, Rarity.EPIC,
            "Une ombre douce qui garde les dormeurs pendant les nuits les plus longues de l'automne.",
            CreatureLook(0xFF4A3B6B, 0xFFB7A4E0, 0xFF7E5FC4, setOf(Feature.HORNS, Feature.WINGS), 1f, 0),
            BonusKind.EFFICIENCY, "lueurs"),
        DreamCreature("bourgeonnet", "Bourgeonnet", SleepType.LIGHT, Rarity.RARE,
            "Il éclot avec les premiers matins clairs du printemps.",
            CreatureLook(0xFF9BDB8A, 0xFFE4F8DC, 0xFF6DBF57, setOf(Feature.ANTENNA, Feature.EARS), 1f, 1),
            BonusKind.WAKE_ON_TIME, "eveil"),
        DreamCreature("petalune", "Pétalune", SleepType.DEEP, Rarity.EPIC,
            "Elle se replie comme une fleur la nuit et rêve en couleurs.",
            CreatureLook(0xFFF2A6C8, 0xFFFFE3F0, 0xFFDB78A8, setOf(Feature.CROWN, Feature.TAIL), 1.05f, 1),
            BonusKind.HEALTHY_DURATION, "eveil"),
        DreamCreature("solsticelle", "Solsticelle", SleepType.LIGHT, Rarity.EPIC,
            "Née de la nuit la plus courte, elle aide à garder le rythme malgré l'été.",
            CreatureLook(0xFFFFE07A, 0xFFFFF7D6, 0xFFFFC02E, setOf(Feature.WINGS, Feature.ANTENNA), 0.95f, 1),
            BonusKind.STREAK, "courtes"),
        DreamCreature("meduzephyr", "Méduzéphyr", SleepType.DEEP, Rarity.RARE,
            "Elle flotte dans la fraîcheur des nuits d'été, au fond des rêves.",
            CreatureLook(0xFF7FD6E0, 0xFFD8F7FA, 0xFF45B5C4, setOf(Feature.FINS, Feature.TAIL), 0.9f, 0),
            BonusKind.DEEP_SLEEP, "courtes"),
    )

    /** Créatures rarissimes qui n'apparaissent que les nuits de pleine lune. */
    val moonCreatures: List<DreamCreature> = listOf(
        DreamCreature("opaline", "Opaline", SleepType.DEEP, Rarity.LEGENDARY,
            "Une perle de lune vivante. Elle ne descend dans les rêves que les nuits de pleine lune.",
            CreatureLook(0xFFEDE7FF, 0xFFFFFFFF, 0xFFB8F0E8, setOf(Feature.CROWN), 1f, 3),
            BonusKind.HEALTHY_DURATION, "pleinelune"),
    )

    /** Toutes les créatures, événements compris (pour le carnet et la résolution des ids). */
    val everything: List<DreamCreature> get() = all + eventCreatures + moonCreatures

    fun byId(id: String): DreamCreature? = everything.firstOrNull { it.id == id }
    fun ofType(type: SleepType): List<DreamCreature> = all.filter { it.type == type }
    fun ofEvent(eventId: String): List<DreamCreature> = eventCreatures.filter { it.eventId == eventId }
}
