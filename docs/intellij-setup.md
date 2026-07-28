# Configuration IntelliJ pour La Table

Ce projet est un mod Minecraft NeoForge pour Minecraft 1.21.1.

## Prerequis

- IntelliJ IDEA, edition Community ou Ultimate.
- JDK 21. Le projet a ete teste avec Eclipse Temurin 21.
- Une connexion internet au premier import Gradle, pour telecharger Gradle, NeoForge, Minecraft, JEI et EMI.

## Ouverture du projet

1. Dans IntelliJ, choisis `File > Open`.
2. Ouvre le dossier racine du projet: `La-Table-1.21.X`.
3. Si IntelliJ demande quoi importer, choisis `Gradle`.
4. Dans les reglages Gradle du projet:
   - `Gradle JVM`: JDK 21.
   - `Use Gradle from`: Gradle wrapper.
5. Lance la synchronisation Gradle.

## Lancements utiles

Les taches NeoGradle disponibles sont:

- `runClient`: lance Minecraft en client de dev.
- `runServer`: lance un serveur de dev.
- `runData`: regenere les donnees dans `src/generated/resources`.
- `runGameTestServer`: lance le serveur de tests Minecraft.

Dans IntelliJ, tu peux les lancer depuis la fenetre Gradle, ou utiliser les configurations creees dans `.run`.

## Commandes de verification

Depuis un terminal ouvert a la racine du projet:

```powershell
.\gradlew.bat idePostSync
.\gradlew.bat compileJava
.\gradlew.bat runClient
```

## Si IntelliJ ne trouve pas le JDK

Va dans `File > Project Structure > Project SDK`, ajoute le JDK 21 installe sur la machine, puis relance la synchronisation Gradle.

## Si Gradle bloque sur un fichier dans `.gradle`

Le dossier `.gradle` du projet est un cache local regenerable. Apres une copie depuis un autre PC, il peut contenir des fichiers avec des permissions ou verrous Windows invalides.

Solution:

1. Ferme IntelliJ.
2. Supprime le dossier `.gradle` a la racine du projet.
3. Rouvre IntelliJ et relance la synchronisation Gradle.

