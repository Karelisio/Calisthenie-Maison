# Calisthénie Maison — mémo projet

App Android de calisthénie (musculation au poids du corps) pour Karelisio,
générée à partir d'une PWA via Capacitor. Utilisateur francophone — répondre
en français dans ce projet.

## Règles de développement

- **Une seule version maintenue : la branche `material-design`.** L'ancienne
  version "originale" (branche `claude/apk-pwa-mdg92t`) est abandonnée,
  ne plus y toucher ni la comparer.
- **`index.html` est le seul fichier source actif** — c'est une app
  monofichier (HTML+CSS+JS en IIFE), pas de build JS/bundler. Les autres
  fichiers du repo (android/, .github/workflows/) sont l'emballage Capacitor
  + CI, à ne modifier que si le besoin le justifie explicitement.
- Commits : toujours suivre les lignes d'attribution données par le système
  au moment du commit (elles changent selon le modèle utilisé dans la
  session — ne pas réutiliser une valeur codée en dur d'une session
  précédente).
- Ne jamais committer le keystore de signature ni son mot de passe : ils
  vivent uniquement dans les secrets GitHub Actions
  (`CALISTHENIE_KEYSTORE_BASE64`, `CALISTHENIE_KEYSTORE_PASSWORD`).
- Le build APK se fait via GitHub Actions (`.github/workflows/build-apk.yml`)
  à chaque push sur `material-design` — pas de SDK Android local disponible
  dans le sandbox. L'APK est publié en release GitHub, tag
  `apk-material-latest`.
- Pont natif Capacitor : ne pas supposer `window.Capacitor.Plugins.X`
  (nécessite `@capacitor/core` bundlé, absent ici). Utiliser
  `window.Capacitor.nativePromise(pluginName, method, options)`.

## Décisions produit déjà tranchées (ne pas revenir dessus sans le redemander)

- Design Material 3, couleurs dynamiques Material You (fond d'écran,
  Android 12+), palette de secours orange `#ff7a45` / teal `#22c3a6`
  (pas vert/teal générique — testé et rejeté).
- Boutons "brillants"/arrondis façon version originale, mais en Material.
  Un seul bouton de thème qui cycle système → clair → sombre (pas 3 boutons
  séparés), largeur fixe (`min-width:112px`) pour ne pas sauter en layout.
- Mode plein écran immersif natif (pas de barre d'état).
- Mise à jour in-app (bouton dans Réglages) avec keystore stable pour
  installer par-dessus sans désinstaller/perdre les données.
- Échauffement/étirement auto avant/après séance, adaptés aux muscles
  travaillés, mode Court (~3 min) / Long (~7 min) au long-press du bouton
  toggle. Bibliothèque complète accessible hors séance : onglet Exercices →
  sous-onglets Échauffement / Exercices / Étirements.
- Pause automatique "changez de côté" (5s) sur les mouvements unilatéraux
  (tag `sides` dans WARMUP_LIB/STRETCH_LIB).
- Option "Éviter les fentes" (tag `lunge`) dans Exercices → Gérer mon
  équipement, pour désactiver les exercices en fente (gêne au genou).
- Suivi de séance avec note de ressenti (facile/parfait/trop dur) + texte
  libre.

## Repères techniques utiles

- `EXO_LIB`, `WARMUP_LIB`, `STRETCH_LIB` : les trois bibliothèques
  d'exercices, indexées ensemble via `getExoById` (Map mémoïsée).
- `FLEX_PLAN` / `FLEX_REST` / `SIDE_SWITCH_SEC` : réglages de sélection et
  timing de l'échauffement/étirement auto.
- `settings.avoidLunges`, `settings.warmupMode`, `settings.theme` :
  préférences persistées en localStorage.
- Pour tester la logique sans navigateur : extraire le corps de l'IIFE de
  `index.html` et l'exécuter dans un `vm.createContext` Node avec des mocks
  minimalistes de `document`/`window`/`localStorage`.
- Pour tester l'UI : Playwright + Chromium pré-installé
  (`/opt/pw-browsers/.../chrome`), utiliser `page.clock` (API Clock de
  Playwright 1.63+) plutôt que de vrais `waitForTimeout` longs — le
  navigateur headless du sandbox devient instable sur des exécutions de
  plusieurs dizaines de secondes.

## Piège vécu à éviter

Le statut d'un run GitHub Actions peut rester affiché "in_progress" par
l'API alors que le run est déjà terminé (cache/latence API) — avant de
ré-interroger en boucle, vérifier `get_workflow_run`/`list_workflow_jobs`
une seule fois de plus après un délai raisonnable plutôt que de multiplier
les checks rapprochés.
