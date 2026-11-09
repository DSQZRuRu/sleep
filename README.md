# Somnia 🌙

Application Android native (Kotlin + Jetpack Compose) de suivi du sommeil **avec le téléphone seul**, sans montre connectée.
Toutes les données restent sur l'appareil : base Room locale, sauvegarde cloud désactivée. Internet ne sert qu'à vérifier et télécharger les mises à jour depuis les releases GitHub (aucune donnée envoyée).

## Télécharger l'APK

Chaque push déclenche le workflow **Android debug APK** (`.github/workflows/android-debug.yml`) :
tests unitaires puis `assembleDebug`. L'APK est publié en artifact `somnia-debug-<sha>`, à récupérer dans l'onglet **Actions** du dépôt.

### Mise à jour intégrée

Chaque push sur la branche par défaut publie aussi une release `build-<n>` contenant l'APK (les 10 dernières sont conservées).
Le numéro de build CI sert de `versionCode`. Au lancement, l'app interroge `releases/latest` et affiche un bandeau « Mise à jour disponible » : un toucher télécharge l'APK, un second ouvre l'installateur Android.
Les APK sont signés avec une clé de debug fixe (`app/debug.keystore`), donc chaque version s'installe par-dessus la précédente.
⚠ L'API des releases n'est accessible sans authentification que si le dépôt est **public**.

## Fonctionnalités

| Domaine | Ce que fait Somnia |
|---|---|
| Suivi de nuit | Foreground Service (type `health`, + `microphone` si activé) avec notification persistante, écran éteint. Accéléromètre à ~5 Hz avec regroupement matériel des échantillons (batching de 20 s). |
| Micro (option) | Niveau sonore RMS calculé sur des blocs de 0,5 s puis jeté : **aucun audio enregistré**. Détection de sons réguliers (ronflements probables). |
| Phases | Actigraphie par minute, lissage pondéré façon Cole-Kripke, puis éveil / léger / profond (immobilité sur 10 min). Endormissement, réveil final, réveils, efficacité. |
| Secours | Écran allumé/déverrouillé pendant la nuit = éveil. Sans suivi, estimation à partir du dernier usage le soir et du premier le matin (UsageStatsManager). |
| Profil | Lever souhaité, temps d'endormissement, contraintes, chronotype inspiré du MEQ. Heures de coucher = lever − n×cycle − endormissement, avec recommandation 7–9 h. |
| Apprentissage | Moyenne mobile exponentielle de la latence mesurée, durée de cycle estimée sur les nuits nettes, nombre de cycles ajusté selon le ressenti du matin. |
| Réveil intelligent | Heure limite exacte (`setAlarmClock`) ; dans la fenêtre de 10/20/30 min avant, sonne dès qu'un mouvement indique un sommeil léger. Plein écran sur l'écran verrouillé, répétition 9 min. |
| Historique | Barres hebdomadaires (zone 7–9 h), hypnogramme par nuit, score /100, régularité (écart-type circulaire des horaires). |
| Conseils | 13 conseils, chacun avec sa référence scientifique. |
| Rappels | Notification à l'heure du coucher (à l'heure, 15 ou 30 min avant), avec un bouton « Lancer le suivi ». Rappel intelligent : de 30 min avant le coucher à 2 h après, si l'écran reste éteint 2 min sans suivi en cours, une notification est envoyée (une fois par nuit). Option pour sonner sur le volume des alarmes, même en mode vibreur. |
| Développeur | Dans les Réglages : notification de test, test du rappel, test du réveil dans 10 s, vérification des mises à jour. |

### Gamification (univers 100 % original)

- **Brumi**, compagnon de brume : Œuf → Bébé → Brumi → Grand Brumi → Brumi astral. Il grandit avec la qualité et la régularité des nuits, ne régresse jamais, et donne un bonus permanent d'Éclat (de 0 à +15 %) selon son stade.
- **Éclat de rêve**, second score : score de sommeil × (1 + bonus actifs). La **poussière d'étoiles ✦** gagnée chaque nuit découle de l'Éclat.
- **Le lit** : jusqu'à 3 créatures installées (places débloquées aux niveaux 1, 3 et 6). Chaque créature a un bonus lié à un comportement sain (coucher à l'heure, 7–9 h, sommeil profond, efficacité, lever régulier, série, rebond, poussière). Le bonus ne compte **que si la nuit remplit sa condition**. L'**Harmonie** ajoute +3 % ou +8 % quand 2 ou 3 créatures du même type sont installées.
- **Niveaux des créatures** (1 à 10) achetés en poussière d'étoiles. Une créature déjà connue rencontrée à nouveau se dissout en poussière.
- **23 créatures du rêve**, dessinées en Canvas : 14 permanentes et 9 exclusives aux **événements saisonniers** (Nuit des Givres à Noël, Nuit des Lueurs, Éveil des Songes, Nuits Courtes), qui doublent leur bonus pendant leur saison et augmentent la poussière gagnée.
- **XP et niveaux**, **séries** avec une protection par semaine, **3 défis hebdomadaires**, **25 badges**.
- **Écran du matin animé** : score, Éclat de rêve bonus par bonus, XP, série, créatures, évolution, badges, ressenti.

Garde-fous : aucune récompense ne demande de garder l'écran allumé. Rester au lit sans dormir ne rapporte rien : sous 3 h de sommeil réel, il n'y a ni créature, ni bonus d'Éclat, ni poussière. L'XP de durée s'arrête à 9 h, et les bonus récompensent la régularité et l'efficacité.

### Sauvegarde

Android efface les données d'une app quand on la désinstalle. Somnia écrit donc une archive (base Room et réglages) dans **Téléchargements/Somnia**, une fois par jour et après chaque nuit, en gardant les 3 plus récentes. Ce dossier survit à la désinstallation : au premier lancement, l'écran d'accueil propose « Restaurer une sauvegarde ». Les Réglages permettent aussi de sauvegarder, d'exporter et de restaurer à la main.
La sauvegarde automatique d'Android (chiffrée, sur le compte Google) est également activée.
Les mises à jour s'installent par-dessus l'app sans rien effacer : il n'est pas nécessaire de désinstaller.

### Illustrations

Brumi (5 stades, avec une variante yeux fermés) et une partie des créatures sont des illustrations vectorielles originales. La source est en SVG dans `art/svg/`, écrite par `tools/art/build_art.py`, puis convertie en VectorDrawable Android (`res/drawable/art_*.xml`) par `tools/art/svg2vd.py`. `art/preview.html` affiche la planche complète.

```bash
python3 tools/art/build_art.py   # régénère SVG, VectorDrawable et aperçu
```

Les créatures qui n'ont pas encore d'illustration gardent leur rendu procédural en Canvas.

## Architecture

MVVM + Repository, injection de dépendances manuelle (`AppContainer`), coroutines/Flow, Room.

```
app/src/main/java/com/somnia/
├── SomniaApp.kt, AppContainer.kt, MainActivity.kt
├── domain/                    ← Kotlin pur, sans Android, testé unitairement
│   ├── sleep/                 SleepStageClassifier, SleepScoreCalculator, BedtimeCalculator,
│   │                          ChronotypeQuestionnaire, ProfileAdjuster, RegularityCalculator, TimeMath
│   ├── game/                  Creatures, Companion, Progression (XP, séries), Challenges, Badges, RewardEngine
│   └── tips/                  SleepTipsCatalog (conseils + sources)
├── data/
│   ├── db/                    Entités, DAO, SomniaDatabase (Room)
│   └── repository/            Sleep/Profile/GameRepository, NightProcessor (clôture d'une nuit en une transaction)
├── tracking/                  SleepTrackingService, MotionSensorMonitor, SoundLevelMonitor, PhoneUsageDetector, TrackingState
├── reminders/                 ReminderPrefs, ReminderScheduler, ReminderReceiver (rappel du coucher + surveillance de l'écran)
├── update/                    UpdateChecker, UpdateInstaller, UpdateViewModel, UpdateBanner
├── alarm/                     SmartAlarmScheduler, AlarmReceiver, AlarmController, AlarmActivity, AlarmSound, Notifications
└── ui/
    ├── theme/                 Thème sombre apaisant
    ├── components/            Créatures en Canvas, graphiques (hypnogramme, barres, anneau), cartes
    ├── permissions/           Statut et demande de chaque permission, avec explication
    ├── navigation/            SomniaRoot (onboarding → nuit → matin → onglets), fabrique de ViewModel
    └── onboarding/ home/ tracking/ morning/ history/ journal/ tips/ settings/   (écran + ViewModel)
```

Déroulé d'une nuit :

```
Accueil « Commencer ma nuit »
  → SleepTrackingService (premier plan, wakelock partiel)
      toutes les minutes : EpochEntity (mouvement, son, usage) → Room
      dans la fenêtre : sommeil léger détecté ? → AlarmController.ring()
  AlarmManager (heure limite) → AlarmReceiver → ring()
  « Je me lève » → arrêt du service → NightProcessor :
      classification → score → ajustement du profil → RewardEngine → Room
  → écran du matin (révélation) → ressenti → ajustement du nombre de cycles
```

## Permissions

| Permission | Pourquoi |
|---|---|
| `FOREGROUND_SERVICE_HEALTH` + `HIGH_SAMPLING_RATE_SENSORS` | Service de suivi (les deux sont accordées à l'installation) |
| `RECORD_AUDIO` + `FOREGROUND_SERVICE_MICROPHONE` | Option niveau sonore, demandée seulement si activée |
| `POST_NOTIFICATIONS` | Notification de suivi et sonnerie |
| `USE_EXACT_ALARM` (API 33+) / `SCHEDULE_EXACT_ALARM` (≤ 32) | Réveil à l'heure exacte |
| `USE_FULL_SCREEN_INTENT` | Réveil sur l'écran verrouillé |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Empêcher le système de couper le suivi la nuit |
| `PACKAGE_USAGE_STATS` (accès spécial) | Détection de secours, optionnelle |
| `WAKE_LOCK`, `VIBRATE` | Suivi continu, vibration du réveil |
| `RECEIVE_BOOT_COMPLETED` | Reprogrammer les rappels après un redémarrage |
| `INTERNET`, `REQUEST_INSTALL_PACKAGES` | Télécharger et installer les mises à jour depuis GitHub |

Chaque permission est expliquée et demandée depuis l'onboarding et les Réglages ; son état est relu à chaque retour des paramètres système.
Si les notifications sont refusées, le réveil se rabat sur une sonnerie jouée directement par le processus du service.

## Compiler en local

JDK 17 et Android SDK 35 :

```bash
./gradlew testDebugUnitTest assembleDebug
# APK : app/build/outputs/apk/debug/app-debug.apk
```

## Limites

Somnia n'est pas un dispositif médical. Les phases viennent de l'actigraphie (mouvements du matelas) : c'est utile pour suivre des tendances, pas pour remplacer une polysomnographie. Le questionnaire de chronotype s'inspire du MEQ de Horne & Östberg (1976) mais en est une version courte et non validée.
