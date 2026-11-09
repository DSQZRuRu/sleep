package com.somnia.domain.game

import java.time.LocalDate
import java.time.MonthDay

/** Événement saisonnier : créatures exclusives et poussière d'étoiles bonifiée. */
data class SeasonalEvent(
    val id: String,
    val name: String,
    val description: String,
    val start: MonthDay,
    val end: MonthDay,
    val stardustMultiplier: Double,
    val accent: Long,
    val symbol: String,
) {
    fun isActive(date: LocalDate): Boolean {
        val d = MonthDay.from(date)
        return if (!start.isAfter(end)) !d.isBefore(start) && !d.isAfter(end)
        else !d.isBefore(start) || !d.isAfter(end) // chevauche le nouvel an
    }

    /** Prochaine date de début à partir de [date] (aujourd'hui si actif). */
    fun nextStart(date: LocalDate): LocalDate {
        val thisYear = start.atYear(date.year)
        return if (thisYear.isBefore(date)) start.atYear(date.year + 1) else thisYear
    }

    fun endDate(date: LocalDate): LocalDate {
        val e = end.atYear(date.year)
        return if (e.isBefore(date)) end.atYear(date.year + 1) else e
    }
}

object EventCalendar {
    val all = listOf(
        SeasonalEvent("givres", "La Nuit des Givres",
            "Les créatures de l'hiver viennent veiller sur les longues nuits des fêtes. Poussière d'étoiles +50 %.",
            MonthDay.of(12, 15), MonthDay.of(1, 3), 1.5, 0xFF9FD8FF, "❄"),
        SeasonalEvent("lueurs", "La Nuit des Lueurs",
            "Les nuits rallongent et des lueurs espiègles se glissent dans les rêves. Poussière d'étoiles +30 %.",
            MonthDay.of(10, 24), MonthDay.of(11, 2), 1.3, 0xFFFF9A3C, "✺"),
        SeasonalEvent("eveil", "L'Éveil des Songes",
            "Le printemps ramène la lumière du matin : idéal pour se lever à heure fixe. Poussière d'étoiles +30 %.",
            MonthDay.of(3, 18), MonthDay.of(4, 2), 1.3, 0xFF9BDB8A, "❀"),
        SeasonalEvent("courtes", "Les Nuits Courtes",
            "Autour du solstice, gardez le rythme malgré les longues soirées. Poussière d'étoiles +30 %.",
            MonthDay.of(6, 18), MonthDay.of(7, 3), 1.3, 0xFFFFE07A, "☀"),
    )

    fun byId(id: String?) = all.firstOrNull { it.id == id }

    /** Événement actif à cette date ; [override] permet de forcer un événement (outil développeur). */
    fun active(date: LocalDate, override: String? = null): SeasonalEvent? =
        byId(override) ?: all.firstOrNull { it.isActive(date) }

    fun next(date: LocalDate): SeasonalEvent? = all.minByOrNull { it.nextStart(date) }
}
