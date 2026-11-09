package com.somnia.domain.sleep

/**
 * Questionnaire court inspiré du Morningness–Eveningness Questionnaire (Horne & Östberg, 1976).
 * Les formulations sont originales et simplifiées : ce n'est pas l'outil clinique validé.
 */
data class ChronoQuestion(val id: Int, val text: String, val options: List<Pair<String, Int>>)

enum class Chronotype(val label: String, val description: String, val bedtimeShiftMin: Int) {
    DEFINITE_MORNING("Alouette affirmée", "Vous êtes à votre meilleur tôt le matin.", -30),
    MODERATE_MORNING("Plutôt du matin", "Vous préférez les matinées actives.", -15),
    NEITHER("Intermédiaire", "Votre horloge s'adapte assez bien.", 0),
    MODERATE_EVENING("Plutôt du soir", "Votre énergie monte en fin de journée.", 15),
    DEFINITE_EVENING("Hibou affirmé", "Vous êtes naturellement tourné·e vers la nuit.", 30),
}

object ChronotypeQuestionnaire {

    val questions: List<ChronoQuestion> = listOf(
        ChronoQuestion(1, "Si vous étiez totalement libre, à quelle heure vous lèveriez-vous ?", listOf(
            "Avant 6 h 30" to 5, "6 h 30 – 7 h 45" to 4, "7 h 45 – 9 h 45" to 3, "9 h 45 – 11 h" to 2, "Après 11 h" to 1,
        )),
        ChronoQuestion(2, "Et à quelle heure iriez-vous vous coucher ?", listOf(
            "Avant 21 h" to 5, "21 h – 22 h 15" to 4, "22 h 15 – 0 h 30" to 3, "0 h 30 – 1 h 45" to 2, "Après 1 h 45" to 1,
        )),
        ChronoQuestion(3, "Dans la première demi-heure après le réveil, vous vous sentez…", listOf(
            "Très en forme" to 4, "Plutôt en forme" to 3, "Plutôt fatigué·e" to 2, "Très fatigué·e" to 1,
        )),
        ChronoQuestion(4, "Pour un effort physique exigeant, vous choisiriez…", listOf(
            "8 h – 10 h" to 4, "11 h – 13 h" to 3, "15 h – 17 h" to 2, "19 h – 21 h" to 1,
        )),
        ChronoQuestion(5, "À quel moment de la journée vous sentez-vous au sommet ?", listOf(
            "Tôt le matin" to 5, "En fin de matinée" to 4, "L'après-midi" to 3, "En début de soirée" to 2, "Tard le soir" to 1,
        )),
        ChronoQuestion(6, "Vous vous décririez comme…", listOf(
            "Clairement du matin" to 6, "Plutôt du matin" to 4, "Plutôt du soir" to 2, "Clairement du soir" to 0,
        )),
    )

    val maxScore: Int = questions.sumOf { q -> q.options.maxOf { it.second } }
    val minScore: Int = questions.sumOf { q -> q.options.minOf { it.second } }

    fun classify(score: Int): Chronotype {
        val ratio = (score - minScore).toFloat() / (maxScore - minScore)
        return when {
            ratio >= 0.8f -> Chronotype.DEFINITE_MORNING
            ratio >= 0.6f -> Chronotype.MODERATE_MORNING
            ratio >= 0.4f -> Chronotype.NEITHER
            ratio >= 0.2f -> Chronotype.MODERATE_EVENING
            else -> Chronotype.DEFINITE_EVENING
        }
    }
}
