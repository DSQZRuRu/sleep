package com.somnia.data.export

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.somnia.data.db.SleepSessionEntity
import com.somnia.data.repository.Clock
import com.somnia.data.repository.hasStages
import com.somnia.data.repository.wakeTime
import com.somnia.domain.sleep.SleepFactor
import com.somnia.domain.sleep.TimeMath
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Export des nuits pour un professionnel de santé : tableur (CSV) et synthèse PDF. */
object SleepExport {
    private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH)

    private fun date(s: SleepSessionEntity) = LocalDate.ofEpochDay(Clock.nightDay(s.startTime)).format(dateFmt)
    private fun hm(ts: Long?) = ts?.let { TimeMath.format(Clock.minuteOfDay(it)) } ?: ""

    fun csv(nights: List<SleepSessionEntity>, out: OutputStream) {
        val header = listOf(
            "Nuit du", "Coucher", "Endormissement", "Réveil", "Sommeil (min)", "Léger (min)", "Profond (min)",
            "Éveil (min)", "Latence (min)", "Réveils", "Efficacité (%)", "Score", "Ronflements probables (min)",
            "Source", "Facteurs", "Ressenti (1-5)",
        )
        val rows = nights.sortedBy { it.startTime }.map { s ->
            listOf(
                date(s), hm(s.startTime), hm(s.sleepOnset), hm(s.wakeTime), s.totalSleepMin, s.lightMin, s.deepMin,
                s.awakeMin, s.latencyMin, s.awakenings, if (s.hasStages) (s.efficiency * 100).toInt() else "",
                s.score, if (s.micEnabled) s.snoreMinutes else "",
                if (s.hasStages) "Capteurs" else "Usage du téléphone",
                SleepFactor.parse(s.factors).joinToString(" / ") { it.label }, s.morningRating ?: "",
            ).joinToString(";") { it.toString().replace(";", ",") }
        }
        // BOM UTF-8 : les accents s'affichent correctement dans Excel.
        out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        out.write((listOf(header.joinToString(";")) + rows).joinToString("\r\n").toByteArray(Charsets.UTF_8))
    }

    /** Synthèse PDF (A4) : moyennes puis tableau des nuits, sur plusieurs pages si besoin. */
    fun pdf(nights: List<SleepSessionEntity>, out: OutputStream) {
        val doc = PdfDocument()
        val w = 595; val h = 842
        val title = Paint().apply { textSize = 18f; typeface = Typeface.DEFAULT_BOLD; color = Color.rgb(30, 34, 80) }
        val text = Paint().apply { textSize = 10f; color = Color.rgb(40, 40, 60) }
        val bold = Paint(text).apply { typeface = Typeface.DEFAULT_BOLD }
        val muted = Paint(text).apply { color = Color.rgb(110, 110, 130); textSize = 8.5f }
        val line = Paint().apply { color = Color.rgb(210, 212, 230); strokeWidth = 0.8f }

        val sorted = nights.sortedByDescending { it.startTime }
        val n = sorted.size.coerceAtLeast(1)
        val cols = listOf("Nuit du" to 40f, "Coucher" to 110f, "Réveil" to 160f, "Sommeil" to 210f, "Profond" to 265f,
            "Latence" to 320f, "Réveils" to 372f, "Effic." to 420f, "Score" to 465f, "Ressenti" to 510f)

        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, pageNo).create())
        var c = page.canvas
        var y = 50f
        c.drawText("Somnia · Synthèse du sommeil", 40f, y, title); y += 18
        c.drawText("Généré le ${LocalDate.now().format(dateFmt)} · ${sorted.size} nuits · estimations par actigraphie (téléphone sur le matelas), pas un examen médical.", 40f, y, muted); y += 26
        if (sorted.isNotEmpty()) {
            val avgSleep = sorted.sumOf { it.totalSleepMin } / n
            val avgLat = sorted.sumOf { it.latencyMin } / n
            val avgScore = sorted.sumOf { it.score } / n
            val avgAwak = sorted.sumOf { it.awakenings }.toFloat() / n
            val snore = sorted.filter { it.micEnabled }
            listOf(
                "Sommeil moyen : ${TimeMath.formatDuration(avgSleep)}",
                "Temps d'endormissement moyen : $avgLat min",
                "Réveils par nuit : ${"%.1f".format(Locale.FRENCH, avgAwak)}",
                "Score moyen : $avgScore / 100",
                if (snore.isNotEmpty()) "Ronflements probables : ${snore.sumOf { it.snoreMinutes } / snore.size} min par nuit (sur ${snore.size} nuits avec micro)" else "Micro non utilisé",
            ).forEach { c.drawText(it, 40f, y, text); y += 15 }
            y += 12
        }
        fun header() {
            cols.forEach { (label, x) -> c.drawText(label, x, y, bold) }
            y += 6; c.drawLine(40f, y, w - 40f, y, line); y += 14
        }
        header()
        for (s in sorted) {
            if (y > h - 50) {
                doc.finishPage(page)
                pageNo++
                page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, pageNo).create())
                c = page.canvas
                y = 50f
                header()
            }
            val values = listOf(
                date(s), hm(s.startTime), hm(s.wakeTime), TimeMath.formatDuration(s.totalSleepMin),
                if (s.hasStages) "${s.deepMin} min" else "—", "${s.latencyMin} min", "${s.awakenings}",
                if (s.hasStages) "${(s.efficiency * 100).toInt()} %" else "—", "${s.score}", s.morningRating?.let { "$it/5" } ?: "—",
            )
            values.zip(cols).forEach { (v, col) -> c.drawText(v, col.second, y, text) }
            y += 15
        }
        doc.finishPage(page)
        doc.writeTo(out)
        doc.close()
    }
}
