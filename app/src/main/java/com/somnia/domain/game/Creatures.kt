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
)

/** Créatures du rêve — univers entièrement original de Somnia. */
object DreamCreatureCatalog {
    val all: List<DreamCreature> = listOf(
        // — Sommeil léger : créatures aériennes et vaporeuses —
        DreamCreature("plumeline", "Plumeline", SleepType.LIGHT, Rarity.COMMON,
            "Une boule de duvet qui flotte au-dessus des oreillers et compte les respirations.",
            CreatureLook(0xFFB9C7FF, 0xFFE6EBFF, 0xFF8FA4FF, setOf(Feature.WINGS))),
        DreamCreature("frissonnette", "Frissonnette", SleepType.LIGHT, Rarity.COMMON,
            "Elle naît du froissement des draps et disparaît au premier rayon du matin.",
            CreatureLook(0xFFA6E3E9, 0xFFDDF7F9, 0xFF6CC5CF, setOf(Feature.ANTENNA), 0.9f)),
        DreamCreature("veilleuse", "Veilleuse", SleepType.LIGHT, Rarity.COMMON,
            "Une lucciole timide qui garde une petite lumière pour les rêveurs.",
            CreatureLook(0xFFFFE29A, 0xFFFFF3CF, 0xFFFFC94D, setOf(Feature.ANTENNA, Feature.WINGS), 1.1f, 1)),
        DreamCreature("brumelot", "Brumelot", SleepType.LIGHT, Rarity.RARE,
            "Fait de brume d'aube, il glisse entre deux rêves sans jamais se poser.",
            CreatureLook(0xFFC8D6E5, 0xFFF1F5FA, 0xFF9DB4CC, setOf(Feature.TAIL, Feature.EARS), 0.85f)),
        DreamCreature("songelune", "Songelune", SleepType.LIGHT, Rarity.EPIC,
            "Porte un croissant de lune sur la tête et chuchote des rêves légers.",
            CreatureLook(0xFFD7C4FF, 0xFFF3ECFF, 0xFFB38CFF, setOf(Feature.CROWN, Feature.WINGS), 1f, 1)),

        // — Sommeil profond : créatures des abysses tranquilles —
        DreamCreature("dormouflon", "Dormouflon", SleepType.DEEP, Rarity.COMMON,
            "Un petit bélier de nuage, si lourd de sommeil qu'il ne bouge plus d'un poil.",
            CreatureLook(0xFF6C7BD9, 0xFFC5CCFF, 0xFF4656C2, setOf(Feature.HORNS), 1.15f, 1)),
        DreamCreature("abyssin", "Abyssin", SleepType.DEEP, Rarity.COMMON,
            "Il nage dans l'océan calme du sommeil profond, ses nageoires à peine visibles.",
            CreatureLook(0xFF3D5A98, 0xFF8FB0F0, 0xFF2A4580, setOf(Feature.FINS, Feature.TAIL), 1f)),
        DreamCreature("mousselin", "Mousselin", SleepType.DEEP, Rarity.RARE,
            "Doux comme la mousse des forêts, il pousse là où l'on dort sans bruit.",
            CreatureLook(0xFF4E8F7A, 0xFFA8DCC9, 0xFF2F6E5B, setOf(Feature.SPOTS, Feature.EARS), 1.1f, 1)),
        DreamCreature("lentorne", "Lentorne", SleepType.DEEP, Rarity.EPIC,
            "Une créature cornue qui ne se montre qu'aux dormeurs réguliers.",
            CreatureLook(0xFF5B3F9E, 0xFFC0A8F5, 0xFF3E2780, setOf(Feature.HORNS, Feature.SPOTS, Feature.TAIL), 1.05f)),
        DreamCreature("onirion", "Onirion", SleepType.DEEP, Rarity.LEGENDARY,
            "Le gardien des grandes profondeurs. Le croiser porte chance pour toute la semaine.",
            CreatureLook(0xFF1F2E6B, 0xFFFFD98A, 0xFF7B8CFF, setOf(Feature.CROWN, Feature.WINGS, Feature.TAIL), 1.2f, 1)),

        // — Sommeil agité : créatures espiègles —
        DreamCreature("tourbillot", "Tourbillot", SleepType.RESTLESS, Rarity.COMMON,
            "Il tourne sur lui-même toute la nuit et emmêle les couvertures.",
            CreatureLook(0xFFF2A07B, 0xFFFFD6C2, 0xFFE07A50, setOf(Feature.ANTENNA, Feature.TAIL), 0.9f, 2)),
        DreamCreature("grignoton", "Grignoton", SleepType.RESTLESS, Rarity.COMMON,
            "Un rongeur de pensées qui adore les soirées trop chargées.",
            CreatureLook(0xFFD99A6C, 0xFFF5D3B8, 0xFFB5733F, setOf(Feature.EARS, Feature.SPOTS), 1f, 2)),
        DreamCreature("cliquetis", "Cliquetis", SleepType.RESTLESS, Rarity.RARE,
            "Il tapote doucement l'oreiller pour rappeler qu'un rituel apaisant aide à dormir.",
            CreatureLook(0xFFE8C468, 0xFFFFEDB8, 0xFFC99A2E, setOf(Feature.HORNS, Feature.ANTENNA), 0.95f, 2)),
        DreamCreature("orageon", "Orageon", SleepType.RESTLESS, Rarity.EPIC,
            "Un petit nuage d'orage, plus grondeur que méchant.",
            CreatureLook(0xFF7D8AA6, 0xFFD5DCEB, 0xFF5A6785, setOf(Feature.WINGS, Feature.SPOTS), 1.1f, 2)),
    )

    fun byId(id: String): DreamCreature? = all.firstOrNull { it.id == id }
    fun ofType(type: SleepType): List<DreamCreature> = all.filter { it.type == type }
}
