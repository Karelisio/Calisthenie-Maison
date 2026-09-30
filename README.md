# Calisthénie Maison

Application web (PWA) de séances de calisthénie sans matériel, avec minuteur
intégré. Le site tient en quelques fichiers statiques (`index.html`, `sw.js`,
`manifest.json`, icônes) ; les données (séances, exercices personnalisés,
équipement, suivi) restent dans le navigateur de l'appareil.

## Installer

- **Android (Chrome)** : ouvrir le site, menu ⋮ → « Installer l'application »
  (ou « Ajouter à l'écran d'accueil »).
- **iPhone (Safari)** : bouton Partager → « Sur l'écran d'accueil ».

Après une première ouverture, l'app marche aussi hors ligne.

## Sauvegarder ou changer d'appareil

Onglet **Séances**, carte « Réglages » en bas :

- **📤 Export** : copie le texte affiché et garde-le (note, e-mail…).
- **📥 Import** : colle ce texte sur l'autre appareil. Il remplace les données
  actuelles ; un texte invalide est refusé sans rien changer, et
  « Annuler l'import » (en haut de l'onglet Séances) remet les données
  d'avant.

## Modifier le site

En cas d'envoi de `index.html` par l'interface web de GitHub, garder dans le
fichier le lien vers `manifest.json`, la balise `theme-color` et
l'enregistrement du service worker (`sw.js`), et laisser `manifest.json`,
`sw.js` et les icônes à la racine. La vérification « Garde PWA » passe au
rouge sur le commit s'il en manque un.
