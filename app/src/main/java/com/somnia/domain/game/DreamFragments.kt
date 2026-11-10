package com.somnia.domain.game

import com.somnia.domain.sleep.SleepType

/** Fragments de rêve poétiques (textes originaux), tirés selon le type de nuit. */
object DreamFragments {
    private val light = listOf(
        "Vous marchiez sur un pont de brume, et chaque pas sonnait comme une note de piano.",
        "Une Plumeline vous a montré un jardin où les fleurs s'ouvraient au rythme de votre souffle.",
        "Le ciel avait la couleur d'un matin d'été, et quelqu'un riait dans la pièce d'à côté.",
        "Vous lisiez un livre dont les pages se tournaient toutes seules, très doucement.",
        "Une fenêtre donnait sur une mer de nuages ; une Veilleuse y traçait des chemins de lumière.",
        "Vous retrouviez une maison d'enfance où toutes les portes ouvraient sur la même prairie.",
        "Un train de nuit traversait des collines argentées, sans jamais s'arrêter.",
        "Des lettres flottaient dans l'air et formaient le prénom de quelqu'un que vous aimez.",
    )
    private val deep = listOf(
        "Vous nagiez sans effort dans un océan calme, et la lumière venait d'en dessous.",
        "Un Dormouflon vous a prêté sa laine : le monde entier est devenu silencieux.",
        "Vous étiez une montagne endormie, et des forêts poussaient lentement sur vos épaules.",
        "Au fond d'une grotte tiède, des cristaux chantaient une berceuse très ancienne.",
        "La nuit était si profonde qu'on pouvait y entendre pousser les étoiles.",
        "Vous descendiez un escalier de mousse qui menait à une bibliothèque sans fin.",
        "Un Abyssin vous a guidé jusqu'à une cité engloutie où tout le monde dormait paisiblement.",
        "Le temps s'était arrêté, et vous flottiez au milieu d'un ciel sans bruit.",
    )
    private val restless = listOf(
        "Vous couriez après un train qui partait toujours un peu plus tôt — puis il s'est arrêté pour vous.",
        "Un Tourbillot a emmêlé les fils de vos rêves ; vous les avez dénoués un par un.",
        "Les escaliers changeaient de sens, mais chaque étage menait à une pièce plus calme.",
        "Un petit orage grondait dans votre poche ; vous l'avez laissé s'envoler.",
        "Vous cherchiez vos clés dans une maison immense, et vous avez trouvé une porte vers la mer.",
        "Des horloges sonnaient toutes en même temps, puis se sont tues d'un coup, apaisées.",
        "Vous traversiez une foule pressée, et soudain tout le monde s'est assis pour regarder la lune.",
        "Un Grignoton a mangé tous vos soucis de la journée, sauf un tout petit, qu'il vous a rendu.",
    )

    fun forNight(sessionId: Long, type: SleepType): String {
        val list = when (type) {
            SleepType.LIGHT -> light
            SleepType.DEEP -> deep
            SleepType.RESTLESS -> restless
        }
        return list[((sessionId * 2654435761L) ushr 8).mod(list.size.toLong()).toInt()]
    }
}
