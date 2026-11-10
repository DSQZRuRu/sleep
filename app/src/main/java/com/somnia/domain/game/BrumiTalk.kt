package com.somnia.domain.game

import com.somnia.domain.sleep.DayPhase
import kotlin.random.Random

/** Petites phrases de Brumi quand on le touche, selon le moment et votre dernière nuit. */
object BrumiTalk {
    fun line(phase: DayPhase, mood: CompanionMood, debtMin: Int, minutesToBed: Int?, random: Random = Random.Default): String {
        val pool = mutableListOf<String>()
        when (phase) {
            DayPhase.DAWN -> pool += listOf("Bonjour ! Un peu de lumière du matin et on démarre bien.", "*s'étire* … Déjà le matin ?", "Tu as rêvé de quoi ?")
            DayPhase.DAY -> pool += listOf("Hihi, ça chatouille !", "Je garde la chambre en attendant ce soir.", "Une petite marche dehors, ça aide à mieux dormir ce soir.")
            DayPhase.DUSK -> pool += listOf("Le ciel devient tout orange… bientôt l'heure du rituel ?", "*bâille* On baisse les lumières ?", "Je prépare l'oreiller !")
            DayPhase.NIGHT -> pool += listOf("Zzz… *ouvre un œil* … Il est tard, tu sais.", "Chut, les créatures du rêve arrivent…", "*marmonne* … encore cinq minutes…")
        }
        when (mood) {
            CompanionMood.RADIANT -> pool += "Je me sens tout pétillant grâce à tes nuits !"
            CompanionMood.TIRED -> pool += "Je suis un peu fatigué… on se couche à l'heure ce soir ?"
            else -> Unit
        }
        if (debtMin >= 120 && phase != DayPhase.NIGHT) pool += "On a un peu de sommeil en retard. Pas grave : on avance juste le coucher un tout petit peu."
        if (minutesToBed != null && minutesToBed in 1..45) pool += "Plus que $minutesToBed minutes avant l'heure idéale !"
        return pool[random.nextInt(pool.size)]
    }
}

/** Saison pour la météo de la fenêtre (hémisphère nord). */
enum class Season { WINTER, SPRING, SUMMER, AUTUMN;
    companion object {
        fun of(month: Int, day: Int): Season = when {
            month == 12 && day >= 1 || month in 1..2 -> WINTER
            month in 3..5 -> SPRING
            month in 6..8 -> SUMMER
            else -> AUTUMN
        }
    }
}
