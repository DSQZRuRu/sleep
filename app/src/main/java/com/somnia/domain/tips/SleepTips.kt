package com.somnia.domain.tips

data class SleepTip(
    val id: String,
    val title: String,
    val body: String,
    val category: String,
    val source: String,
)

/** Conseils fondés sur la littérature scientifique, avec leur référence. */
object SleepTipsCatalog {
    val all = listOf(
        SleepTip("duration", "Viser 7 à 9 heures",
            "Pour les adultes de 18 à 64 ans, 7 à 9 h de sommeil par nuit sont recommandées ; 7 à 8 h après 65 ans. Dormir régulièrement moins de 7 h est associé à des effets négatifs sur la santé.",
            "Durée", "Hirshkowitz M. et al. (2015). National Sleep Foundation's sleep time duration recommendations. Sleep Health, 1(1), 40–43. · Watson N.F. et al. (2015). Sleep, 38(6), 843–844."),
        SleepTip("regularity", "Des horaires réguliers, même le week-end",
            "La régularité des heures de coucher et de lever est liée à de meilleurs résultats scolaires, à une meilleure humeur et à un risque de mortalité plus faible, indépendamment de la durée de sommeil.",
            "Régularité", "Phillips A.J.K. et al. (2017). Scientific Reports, 7, 3216. · Windred D.P. et al. (2024). Sleep regularity is a stronger predictor of mortality risk than sleep duration. Sleep, 47(1), zsad253."),
        SleepTip("screens", "Réduire les écrans lumineux le soir",
            "Lire sur une tablette avant de dormir retarde l'endormissement, diminue la sécrétion de mélatonine et réduit la vigilance le lendemain matin par rapport à un livre papier.",
            "Lumière", "Chang A.-M. et al. (2015). Evening use of light-emitting eReaders negatively affects sleep, circadian timing, and next-morning alertness. PNAS, 112(4), 1232–1237."),
        SleepTip("morning_light", "Prendre la lumière du matin",
            "L'exposition à la lumière tôt dans la journée avance l'horloge biologique et aide à s'endormir plus tôt le soir ; la lumière intense le soir la retarde.",
            "Lumière", "Khalsa S.B.S. et al. (2003). A phase response curve to single bright light pulses in human subjects. Journal of Physiology, 549(3), 945–952."),
        SleepTip("caffeine", "Pas de caféine dans les 6 h avant le coucher",
            "400 mg de caféine pris 6 heures avant le coucher réduisent encore le sommeil total d'environ une heure, même lorsque la personne ne le perçoit pas.",
            "Alimentation", "Drake C. et al. (2013). Caffeine effects on sleep taken 0, 3, or 6 hours before going to bed. Journal of Clinical Sleep Medicine, 9(11), 1195–1200."),
        SleepTip("alcohol", "L'alcool perturbe la seconde moitié de nuit",
            "L'alcool peut accélérer l'endormissement mais fragmente le sommeil en deuxième partie de nuit et réduit le sommeil paradoxal.",
            "Alimentation", "Ebrahim I.O. et al. (2013). Alcohol and sleep I: effects on normal sleep. Alcoholism: Clinical and Experimental Research, 37(4), 539–549."),
        SleepTip("exercise", "Bouger régulièrement",
            "L'activité physique régulière améliore la qualité de sommeil, le temps total de sommeil et l'efficacité, avec des effets modestes mais robustes.",
            "Activité", "Kredlow M.A. et al. (2015). The effects of physical activity on sleep: a meta-analytic review. Journal of Behavioral Medicine, 38(3), 427–449."),
        SleepTip("warm_bath", "Un bain ou une douche tiède 1 à 2 h avant",
            "Une douche ou un bain chaud (40–42,5 °C) 1 à 2 heures avant le coucher raccourcit le temps d'endormissement, en favorisant la baisse de la température corporelle.",
            "Rituel", "Haghayegh S. et al. (2019). Before-bedtime passive body heating by warm shower or bath to improve sleep. Sleep Medicine Reviews, 46, 124–135."),
        SleepTip("cool_room", "Une chambre fraîche et calme",
            "Une température trop élevée ou trop basse dans la chambre augmente les éveils et réduit le sommeil profond et paradoxal.",
            "Environnement", "Okamoto-Mizuno K., Mizuno K. (2012). Effects of thermal environment on sleep and circadian rhythm. Journal of Physiological Anthropology, 31, 14."),
        SleepTip("bed_for_sleep", "Le lit, c'est pour dormir",
            "Si vous ne dormez pas après une vingtaine de minutes, levez-vous pour une activité calme et revenez quand la somnolence arrive. Ce contrôle du stimulus est une composante centrale de la thérapie cognitivo-comportementale de l'insomnie.",
            "Insomnie", "Edinger J.D. et al. (2021). Behavioral and psychological treatments for chronic insomnia disorder in adults: an AASM clinical practice guideline. Journal of Clinical Sleep Medicine, 17(2), 255–262."),
        SleepTip("naps", "Les siestes courtes",
            "Une sieste de 10 minutes améliore la vigilance et les performances, sans l'inertie de sommeil associée aux siestes plus longues.",
            "Sieste", "Brooks A., Lack L. (2006). A brief afternoon nap following nocturnal sleep restriction: which nap duration is most recuperative? Sleep, 29(6), 831–840."),
        SleepTip("chronotype", "Respecter son chronotype",
            "Les personnes « du soir » obligées de vivre à un horaire matinal accumulent un décalage (jet-lag social) associé à plus de fatigue. Décaler progressivement par la lumière du matin aide.",
            "Chronotype", "Wittmann M. et al. (2006). Social jetlag: misalignment of biological and social time. Chronobiology International, 23(1–2), 497–509. · Horne J.A., Östberg O. (1976). Int. J. Chronobiology, 4(2), 97–110."),
        SleepTip("worry", "Vider sa tête avant de dormir",
            "Écrire une liste de tâches à faire pendant 5 minutes avant le coucher a permis de s'endormir plus vite qu'écrire sur les tâches déjà accomplies.",
            "Rituel", "Scullin M.K. et al. (2018). The effects of bedtime writing on difficulty falling asleep. Journal of Experimental Psychology: General, 147(1), 139–146."),
    )

    fun tipOfTheDay(epochDay: Long): SleepTip = all[(epochDay % all.size).toInt()]
}
