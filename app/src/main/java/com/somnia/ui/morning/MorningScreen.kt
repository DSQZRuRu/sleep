package com.somnia.ui.morning

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.somnia.data.repository.hasStages
import com.somnia.domain.game.CompanionLogic
import com.somnia.domain.game.DreamCreature
import com.somnia.domain.game.Rarity
import com.somnia.domain.game.XpCalculator
import com.somnia.domain.sleep.SleepScoreCalculator
import com.somnia.domain.sleep.TimeMath
import com.somnia.ui.components.CompanionView
import com.somnia.ui.game.GameButton
import com.somnia.ui.game.SparkleBurst
import com.somnia.ui.components.CreatureView
import com.somnia.ui.components.ProgressBar
import com.somnia.ui.components.ScoreRing
import com.somnia.ui.components.SectionLabel
import com.somnia.ui.components.SomniaCard
import com.somnia.ui.components.StatItem
import com.somnia.ui.theme.SomniaColors
import kotlinx.coroutines.delay

/** Révélation animée des récompenses de la nuit, étape par étape. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MorningScreen(vm: MorningViewModel, onDone: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val session = ui.session
    val rewards = ui.rewards
    var phase by remember { mutableIntStateOf(0) }
    val scoreAnim = remember { Animatable(0f) }
    val xpAnim = remember { Animatable(0f) }
    var burst by remember { mutableIntStateOf(0) }

    LaunchedEffect(session?.id, rewards, ui.earlierSleepFrom) {
        if (session == null || ui.earlierSleepFrom != null) return@LaunchedEffect
        delay(300)
        phase = 1
        burst++
        scoreAnim.animateTo(session.score.toFloat(), tween(1400, easing = FastOutSlowInEasing))
        phase = 2; delay(500)
        phase = 3
        if (rewards != null) {
            val before = XpCalculator.levelFor(rewards.totalXpAfter - rewards.xpTotal)
            xpAnim.snapTo(before.xpIntoLevel / before.xpForLevel.toFloat())
            delay(rewards.xpLines.size * 180L + 200)
            val after = XpCalculator.levelFor(rewards.totalXpAfter)
            if (rewards.levelAfter > rewards.levelBefore) {
                xpAnim.animateTo(1f, tween(700)); xpAnim.snapTo(0f)
            }
            xpAnim.animateTo(after.xpIntoLevel / after.xpForLevel.toFloat(), tween(900))
        }
        phase = 4; delay(400)
        phase = 5; delay(rewards?.creatures?.size?.times(350L) ?: 0L)
        phase = 6
        if (rewards != null && (rewards.levelAfter > rewards.levelBefore || rewards.stageAfter != rewards.stageBefore || rewards.badges.isNotEmpty())) burst++
        delay(400)
        phase = 7
    }

    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(SomniaColors.Dawn.copy(alpha = 0.85f), androidx.compose.ui.graphics.Color.Transparent)))
            .statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Bonjour ☀", style = MaterialTheme.typography.headlineMedium)
        if (session == null) return@Column

        // Avant la révélation : la nuit a-t-elle commencé avant le lancement du suivi ?
        ui.earlierSleepFrom?.let { from ->
            val fromText = com.somnia.domain.sleep.TimeMath.format(com.somnia.data.repository.Clock.minuteOfDay(from))
            val launchText = com.somnia.domain.sleep.TimeMath.format(com.somnia.data.repository.Clock.minuteOfDay(session.startTime))
            val extra = ((session.startTime - from) / 60_000L).toInt()
            CompanionView(com.somnia.domain.game.CompanionStage.YOUNG, com.somnia.domain.game.CompanionMood.SLEEPY, Modifier.size(120.dp))
            SomniaCard(accent = SomniaColors.Lavender) {
                SectionLabel("Petite question avant vos récompenses")
                Text("Vous dormiez déjà avant de lancer la nuit ?", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Votre téléphone n'a pas été utilisé de $fromText jusqu'à un court réveil juste avant le lancement du suivi, à $launchText. " +
                        "Si vous dormiez pendant ce temps, Somnia ajoute ces ${com.somnia.domain.sleep.TimeMath.formatDuration(extra)} à votre nuit " +
                        "(comptées en sommeil estimé, sans phases détaillées) avant de calculer score et récompenses.",
                    style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted,
                )
                GameButton(
                    "Oui, je dormais depuis $fromText", onClick = { vm.answerEarlierSleep(true) },
                    enabled = !ui.resolving, leading = "☾", modifier = Modifier.fillMaxWidth(),
                )
                androidx.compose.material3.OutlinedButton(
                    onClick = { vm.answerEarlierSleep(false) }, enabled = !ui.resolving, modifier = Modifier.fillMaxWidth(),
                ) { Text("Non, j'étais éveillé") }
            }
            return@Column
        }

        AnimatedVisibility(phase >= 1, enter = fadeIn() + scaleIn(spring(Spring.DampingRatioMediumBouncy))) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ScoreRing(scoreAnim.value.toInt(), Modifier.size(180.dp), progress = scoreAnim.value / 100f)
                Text(SleepScoreCalculator.label(session.score), style = MaterialTheme.typography.titleLarge)
            }
        }

        AnimatedVisibility(phase >= 2, enter = fadeIn() + slideInVertically { it / 3 }) {
            SomniaCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("Sommeil", TimeMath.formatDuration(session.totalSleepMin))
                    if (session.hasStages) {
                        StatItem("Profond", TimeMath.formatDuration(session.deepMin), color = SomniaColors.Lavender)
                        StatItem("Endormi en", "${session.latencyMin} min")
                    } else {
                        StatItem("Source", "Usage tél.")
                    }
                }
                if (session.snoreMinutes > 0) Text("Sons réguliers (ronflements probables) : ${session.snoreMinutes} min",
                    style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
            }
        }

        if (ui.phoneNotOnBed) {
            AnimatedVisibility(phase >= 2, enter = fadeIn()) {
                SomniaCard(accent = SomniaColors.Awake) {
                    SectionLabel("Téléphone sur le matelas ?")
                    Text("Le capteur n'a presque rien senti de la nuit : le téléphone était sans doute posé à côté du lit. " +
                        "Posez-le sur le matelas, près de l'oreiller, pour que les phases soient fiables.",
                        style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
                }
            }
        }

        if (rewards != null) {
            AnimatedVisibility(phase >= 2, enter = fadeIn() + slideInVertically { it / 3 }) {
                DreamReveal(rewards)
            }

            if (rewards.expeditionSteps > 0 || rewards.fullMoon) {
                AnimatedVisibility(phase >= 4, enter = fadeIn() + slideInVertically { it / 3 }) {
                    ExpeditionReveal(rewards)
                }
            }

            AnimatedVisibility(phase >= 3, enter = fadeIn()) {
                SomniaCard {
                    SectionLabel("Points d'expérience")
                    rewards.xpLines.forEachIndexed { i, line ->
                        var shown by remember { mutableIntStateOf(0) }
                        LaunchedEffect(Unit) { delay(i * 180L); shown = 1 }
                        AnimatedVisibility(shown == 1, enter = fadeIn() + slideInVertically { -it }) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(line.label)
                                Text("+${line.xp} XP", color = SomniaColors.Gold)
                            }
                        }
                    }
                    if (rewards.xpLines.isEmpty()) Text("Pas d'XP cette nuit — chaque nuit est un nouveau départ.", color = SomniaColors.Muted)
                    Spacer(Modifier.height(4.dp))
                    ProgressBar(xpAnim.value, color = SomniaColors.Gold)
                    Text(
                        if (rewards.levelAfter > rewards.levelBefore) "✦ Niveau ${rewards.levelAfter} atteint !" else "Niveau ${rewards.levelAfter}",
                        color = if (rewards.levelAfter > rewards.levelBefore) SomniaColors.Gold else SomniaColors.Muted,
                    )
                }
            }

            AnimatedVisibility(phase >= 4, enter = fadeIn() + slideInVertically { it / 3 }) {
                SomniaCard {
                    SectionLabel("Série")
                    Text(
                        when {
                            rewards.freezeUsed -> "🛡 Protection utilisée : votre série de ${rewards.streakAfter} nuits est sauvée."
                            rewards.streakBroken -> "La série repart à ${rewards.streakAfter}. Ce soir, on recommence ensemble."
                            rewards.streakAfter > 0 -> "🔥 ${rewards.streakAfter} nuit${if (rewards.streakAfter > 1) "s" else ""} régulière${if (rewards.streakAfter > 1) "s" else ""} d'affilée"
                            else -> "Une nuit régulière (coucher à ±45 min de l'heure idéale, 6 h+ de sommeil) lance une série."
                        }
                    )
                }
            }

            if (rewards.creatures.isNotEmpty()) {
                AnimatedVisibility(phase >= 5, enter = fadeIn()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SectionLabel("Créatures du rêve venues cette nuit")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rewards.creatures.forEachIndexed { i, cr ->
                                CreatureReveal(cr, isNew = cr.id in rewards.newCreatureIds, delayMs = i * 350L, Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                AnimatedVisibility(phase >= 5) {
                    Text("Aucune créature n'est venue : elles n'apparaissent qu'après au moins 3 h de vrai sommeil.",
                        color = SomniaColors.Muted, textAlign = TextAlign.Center)
                }
            }

            AnimatedVisibility(phase >= 6, enter = fadeIn() + scaleIn()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (rewards.stageAfter != rewards.stageBefore) {
                        SomniaCard {
                            SectionLabel("Évolution !")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CompanionView(rewards.stageAfter, ui.mood, Modifier.size(100.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("${CompanionLogic.NAME} est devenu ${rewards.stageAfter.label} ! ${rewards.stageAfter.description}")
                            }
                        }
                    }
                    rewards.challenges.forEach {
                        SomniaCard {
                            SectionLabel("Défi accompli")
                            Text("⚑ ${it.title}", style = MaterialTheme.typography.titleMedium, color = SomniaColors.Teal)
                        }
                    }
                    if (rewards.badges.isNotEmpty()) {
                        SomniaCard {
                            SectionLabel("Nouveaux badges")
                            rewards.badges.forEach { b ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(40.dp).background(SomniaColors.SurfaceHigh, CircleShape), contentAlignment = Alignment.Center) {
                                        Text(b.symbol, color = SomniaColors.Gold)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column { Text(b.name); Text(b.description, style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted) }
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(phase >= 7 || rewards == null, enter = fadeIn()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SomniaCard(accent = SomniaColors.Lavender) {
                    SectionLabel("Journal des rêves")
                    ui.fragment?.let {
                        Text("« $it »", style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = SomniaColors.Lavender)
                        Text("Fragment de rêve offert par les créatures de la nuit.", style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
                    }
                    androidx.compose.material3.OutlinedTextField(
                        value = ui.dreamNote,
                        onValueChange = vm::setDreamNote,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Votre rêve, en quelques mots (facultatif)") },
                        minLines = 2,
                    )
                }
                SomniaCard {
                    SectionLabel("Hier, il y a eu…")
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        com.somnia.domain.sleep.SleepFactor.entries.forEach { f ->
                            androidx.compose.material3.FilterChip(
                                selected = f in ui.factors, onClick = { vm.toggleFactor(f) },
                                label = { Text("${f.emoji} ${f.label}") },
                            )
                        }
                    }
                    Text("Après quelques semaines, l'onglet Conseils vous montrera ce qui aide VOTRE sommeil.",
                        style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
                }
                Text("Comment vous sentez-vous ?", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("😫", "😕", "😐", "🙂", "😄").forEachIndexed { i, emoji ->
                        val value = i + 1
                        if (ui.rating == value) Button(onClick = { vm.rate(value) }) { Text(emoji) }
                        else OutlinedButton(onClick = { vm.rate(value) }) { Text(emoji) }
                    }
                }
                Text("Votre ressenti aide Somnia à ajuster votre nombre de cycles idéal.",
                    style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted, textAlign = TextAlign.Center)
                GameButton("Commencer la journée", onClick = { vm.done(); onDone() }, modifier = Modifier.fillMaxWidth(), leading = "☀")
            }
        }
    }
    SparkleBurst(burst.takeIf { it > 0 }, Modifier.fillMaxSize())
    }
}

@Composable
private fun CreatureReveal(creature: DreamCreature, isNew: Boolean, delayMs: Long, modifier: Modifier = Modifier) {
    var visible by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { delay(delayMs); visible = 1 }
    AnimatedVisibility(visible == 1, modifier, enter = fadeIn() + scaleIn(spring(Spring.DampingRatioLowBouncy))) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CreatureView(creature, Modifier.size(86.dp))
            Text(creature.name, style = MaterialTheme.typography.titleSmall)
            Text(creature.rarity.label, style = MaterialTheme.typography.labelSmall, color = rarityColor(creature.rarity))
            if (isNew) Text("NOUVEAU", style = MaterialTheme.typography.labelSmall, color = SomniaColors.Gold)
        }
    }
}

fun rarityColor(r: Rarity) = when (r) {
    Rarity.COMMON -> SomniaColors.Muted
    Rarity.RARE -> SomniaColors.Teal
    Rarity.EPIC -> SomniaColors.Lavender
    Rarity.LEGENDARY -> SomniaColors.Gold
}


/** Éclat de rêve : score de sommeil + bonus des créatures du lit, puis poussière d'étoiles. */
@Composable
private fun DreamReveal(rewards: com.somnia.data.repository.StoredRewards) {
    val total = remember { Animatable(rewards.dreamBase.toFloat()) }
    var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(rewards) {
        for (i in 1..rewards.dreamLines.size) { delay(350); shown = i }
        delay(300)
        total.animateTo(rewards.dreamScore.toFloat(), tween(900, easing = FastOutSlowInEasing))
        shown = rewards.dreamLines.size + 1
    }
    SomniaCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel("Éclat de rêve")
                Text("Score de sommeil ${rewards.dreamBase} + bonus du lit", style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
            }
            Text("${total.value.toInt()}", style = MaterialTheme.typography.displayMedium, color = SomniaColors.Gold)
        }
        rewards.event?.let {
            Text("${it.symbol} ${it.name}", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(it.accent))
        }
        rewards.dreamLines.forEachIndexed { i, line ->
            AnimatedVisibility(shown > i, enter = fadeIn() + slideInVertically { -it }) {
                Row(Modifier.fillMaxWidth()) {
                    Text(line.label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                        color = if (line.active) SomniaColors.Moon else SomniaColors.Muted)
                    Text(if (line.active) "+${line.pct} %" else "condition non remplie", style = MaterialTheme.typography.bodySmall,
                        color = if (line.active) SomniaColors.Gold else SomniaColors.Muted)
                }
            }
        }
        if (rewards.dreamLines.isEmpty()) {
            Text("Installez des créatures dans votre lit (onglet Carnet) pour faire briller vos nuits.",
                style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
        }
        AnimatedVisibility(shown > rewards.dreamLines.size, enter = fadeIn() + scaleIn()) {
            Column {
                Row(Modifier.fillMaxWidth()) {
                    Text("Poussière d'étoiles", Modifier.weight(1f))
                    Text("+${rewards.stardustGained} ✦", color = SomniaColors.Gold, style = MaterialTheme.typography.titleMedium)
                }
                rewards.stardustLines.filter { it.active }.forEach {
                    Text("${it.label} : +${it.pct} %", style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
                }
                if (rewards.duplicateStardust > 0) Text("Créatures déjà connues dissoutes en poussière : +${rewards.duplicateStardust} ✦",
                    style = MaterialTheme.typography.bodySmall, color = SomniaColors.Muted)
            }
        }
    }
}


@Composable
private fun ExpeditionReveal(rewards: com.somnia.data.repository.StoredRewards) {
    var chapter by remember { mutableStateOf<com.somnia.domain.game.DreamRegion?>(null) }
    SomniaCard(accent = SomniaColors.Gold) {
        SectionLabel("Monde des rêves")
        if (rewards.fullMoon) Text("◯ Nuit de pleine lune", style = MaterialTheme.typography.titleSmall, color = SomniaColors.Gold)
        if (rewards.expeditionSteps > 0) Text("Expédition : Brumi a avancé de ${rewards.expeditionSteps} étape${if (rewards.expeditionSteps > 1) "s" else ""}",
            style = MaterialTheme.typography.titleSmall)
        rewards.regionsCompleted.forEach { r ->
            Text("✦ ${r.name} explorée ! +${r.rewardStardust} ✦", color = androidx.compose.ui.graphics.Color(r.accent))
            androidx.compose.material3.OutlinedButton(onClick = { chapter = r }, modifier = Modifier.fillMaxWidth()) { Text("Lire : ${r.chapterTitle}") }
        }
    }
    chapter?.let { r ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { chapter = null },
            title = { Text(r.chapterTitle) },
            text = { Text(r.chapter) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { chapter = null }) { Text("Fermer") } },
        )
    }
}
