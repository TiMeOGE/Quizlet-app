# QuizApp 🃏

Application Android d'apprentissage de vocabulaire **anglais ↔ français**, dans
l'esprit de Quizlet, **100 % hors-ligne**. Aucune connexion réseau ni clé API
n'est nécessaire : toutes les données sont stockées localement sur l'appareil.

## Aperçu

QuizApp est une application Android **native** (Kotlin + Jetpack Compose) qui
héberge une application web mono-fichier (HTML/CSS/JS) dans une `WebView`.
L'ensemble de l'interface et de la logique métier se trouve dans
[`app/src/main/assets/quizlet_clone.html`](app/src/main/assets/quizlet_clone.html) ;
la couche native sert d'enveloppe (bouton retour Android, import/export de
fichiers, partage).

## Fonctionnalités

- **Gestion des sets de cartes** : créer, modifier, supprimer des jeux de cartes
  (terme anglais ↔ définition française).
- **4 modes d'étude** :
  - 🃏 **Flashcards** — mélange, mode « à revoir seulement », reprise de la
    dernière position, choix du sens (EN→FR ou FR→EN).
  - ✏️ **Quiz QCM** — questions à choix multiples avec correction immédiate.
  - ⌨️ **Écrire** — saisie libre avec tolérance orthographique (distance de
    Levenshtein, seuil de similarité à 85 %) et option « J'avais juste ».
  - 🧩 **Mots croisés** — génération locale d'une grille à partir des cartes du set.
- **Suivi de progression** par carte (connue / à revoir / neutre), avec barres
  de progression et filtres.
- **Import / Export** de sets au format JSON (partage natif Android via
  `FileProvider`).
- **Interface en français** et prise en charge du bouton retour matériel.

Deux sets de démonstration sont préchargés au premier lancement.

## Architecture

| Élément | Rôle |
| --- | --- |
| `app/src/main/java/com/example/MainActivity.kt` | Hôte `WebView`, pont JavaScript (`QuizAppBridge.downloadJSON`) pour l'export, gestion du bouton retour et du sélecteur de fichiers pour l'import. |
| `app/src/main/assets/quizlet_clone.html` | Toute l'application : interface, état, persistance (`localStorage`) et moteurs des 4 modes. |
| `app/src/main/java/com/example/ui/theme/` | Thème Compose (utilisé uniquement pour l'enveloppe système). |

La persistance se fait via le `localStorage` de la `WebView`
(clés `quizapp_sets`, `quizapp_progress`, `quizapp_fc_state`).

## Prérequis

- [Android Studio](https://developer.android.com/studio) récent
- JDK 11+
- Android SDK 36 (`minSdk` 24, `targetSdk` 36)

## Lancer en local

1. Cloner ce dépôt.
2. Ouvrir le projet dans Android Studio (laisser Gradle se synchroniser).
3. Lancer l'application sur un émulateur ou un appareil physique.

> Les builds **debug** sont signés automatiquement avec le keystore de debug
> fourni — aucune configuration supplémentaire n'est requise.
>
> ⚠️ Contrairement à ce que pouvait indiquer l'ancien README, **aucun fichier
> `.env` ni aucune clé `GEMINI_API_KEY` n'est nécessaire** : l'application ne
> fait aucun appel réseau.

## Build release

Le build release nécessite un keystore de signature, fourni via des variables
d'environnement :

| Variable | Description |
| --- | --- |
| `KEYSTORE_PATH` | Chemin vers le fichier `.jks` (défaut : `my-upload-key.jks` à la racine). |
| `STORE_PASSWORD` | Mot de passe du keystore. |
| `KEY_PASSWORD` | Mot de passe de la clé `upload`. |

```bash
./gradlew assembleRelease
```

## Format d'un set exporté (JSON)

```json
{
  "quizapp_set": true,
  "title": "Anglais Express — Les Indispensables",
  "desc": "Les expressions les plus utiles au quotidien.",
  "cards": [
    { "term": "Hello", "def": "Bonjour" },
    { "term": "Thank you", "def": "Merci" }
  ]
}
```

Le champ `"quizapp_set": true` est obligatoire pour qu'un fichier soit reconnu
à l'import.
