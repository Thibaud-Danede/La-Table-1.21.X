@echo off
setlocal

cd /d "%~dp0"

echo.
echo === La Table - reparation du cache Gradle local ===
echo.
echo Ce script supprime uniquement des dossiers generes localement.
echo Ils seront reconstruits automatiquement par Gradle/NeoForge.
echo.

call gradlew.bat --stop

echo.
echo Suppression des caches du projet...
if exist ".gradle" rmdir /s /q ".gradle"
if exist "build" rmdir /s /q "build"

echo.
echo Preparation NeoForge / IntelliJ...
call gradlew.bat idePostSync
if errorlevel 1 goto failed

echo.
echo Test de compilation...
call gradlew.bat compileJava
if errorlevel 1 goto failed

echo.
echo OK. Tu peux maintenant lancer Minecraft Client depuis IntelliJ,
echo ou lancer: gradlew.bat runClient
echo.
pause
exit /b 0

:failed
echo.
echo La reparation n'a pas suffi. Copie l'erreur affichee ci-dessus.
echo.
pause
exit /b 1
