# Kalisto

Application Android de musculation au poids du corps (calisthénie), sans matériel obligatoire, avec suivi de séances, minuteur intégré et design Material 3 adaptatif (couleurs Material You selon le fond d'écran).

## Fonctionnalités

- **Séances prédéfinies et personnalisables** : Push, Pull, Jambes, Full Body, HIIT, Abdos — ou crée les tiennes.
- **Bibliothèque d'exercices** organisée en 3 sous-onglets : 🔥 Échauffement / 💪 Exercices / 🧘 Étirements, avec conseils d'exécution et sécurité pour chaque mouvement.
- **Échauffement & étirements automatiques**, adaptés aux muscles de la séance choisie (mode Court ~3 min ou Long ~7 min), avec pause "changez de côté" pour les mouvements unilatéraux.
- **Gestion du matériel** (barre de traction, élastiques, banc...) qui débloque les exercices correspondants, plus une option pour désactiver les exercices en fente (fentes) en cas de gêne au genou.
- **Suivi des séances** avec note de ressenti (facile / parfait / trop dur) et commentaire libre.
- **Thème clair / sombre / système**, couleurs Material You basées sur le fond d'écran (Android 12+).
- **Mode plein écran** natif, sans barre d'état.
- **Mise à jour intégrée** : un bouton dans Réglages va chercher la dernière release GitHub et installe l'APK en place (signature stable, pas de perte de données).

## Développement

- `index.html` : l'application web complète (PWA), unique fichier source.
- `android/` : projet Capacitor qui empaquette la PWA en APK Android natif.
- Build automatique via GitHub Actions (`.github/workflows/build-apk.yml`) à chaque push : compile l'APK signé et le publie en release GitHub.

Branche active de développement : **`material-design`** (seule version maintenue).

## Télécharger l'APK

Dernière version : [Releases](../../releases/tag/apk-material-latest)
