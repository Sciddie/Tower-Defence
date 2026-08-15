# Medieval Castle Defense

Ein vollständig spielbares 2D-Tower-Defense-Spiel für Android: Verteidige deine mittelalterliche Burg gegen immer stärkere Gegnerwellen.

## Features

- **Vollständiger Gameplay-Loop**: Gegner spawnen → laufen Wegpunkte ab → Türme greifen automatisch an → Gold für Kills → Bauen/Upgraden → nächste Welle → Boss → Sieg oder Niederlage
- **6 Turmtypen** mit je 5 Upgrade-Stufen: Bogenschütze, Kanone (Flächenschaden), Armbrust, Magier (Kettenblitz, ignoriert Rüstung), Frost (Verlangsamung), Belagerung (extreme Reichweite)
- **Upgrade-Pfade**: Bogenschütze (Schnellfeuer vs. Präzision) und Magier (Kettenblitz vs. Arkanfokus) verzweigen ab Stufe 3
- **7 Gegnertypen** mit eigenen Werten (Leben, Tempo, Rüstung, Burgschaden, Gold, Größe, Bewegungstyp): Fußsoldat, Späher, Ritter, Schwerer Ritter, Bogenschütze (legt Türme lahm!), Belagerungseinheit, Boss
- **50 Level** über 6 thematische Maps (Königreich, Bergpass, Winterreich, Wüste, Dunkler Wald, Vulkanland) mit 6 Streckenlayouts
- **6 Bosse** (alle 5 Level): Der Schwarze Ritter, Orkkönig, Eisgolem, Wikingerhäuptling, Dunkler König, Lava-Titan
- **Sterne-System** (1–3 Sterne je nach verbliebenem Burgleben), Level-Freischaltung, Turm-Freischaltung
- **8 Erfolge** (Erster Sieg, Goldrausch, Burgherr, Unbesiegbar, …)
- **Prozedurales Audio**: Alle Soundeffekte und zwei Musik-Loops (normal/Boss) werden zur Laufzeit synthetisiert – keine Binär-Assets nötig
- **Lokale Speicherung** über SharedPreferences (Sterne, Erfolge, Statistiken, Einstellungen)
- **Kein Pay-to-Win**: keine Energie, keine Werbung, keine In-App-Käufe

## Technologie

Natives Android (Kotlin) mit einer eigenen, schlanken 2D-Engine auf `SurfaceView`/Canvas:

- Keine Engine-Abhängigkeiten, kleiner APK-Footprint
- Eigener Game-Loop-Thread mit gekapptem Delta-Time (~60 FPS)
- Object Pooling für Gegner, Projektile und Partikel (keine GC-Spitzen)
- Map wird einmal pro Level in eine Bitmap gerendert (ein Blit pro Frame)
- Alle Grafiken werden prozedural als Vektorformen gezeichnet (skaliert verlustfrei auf jede Auflösung)

## Architektur

```
app/src/main/java/com/medieval/castledefense/
├── MainActivity.kt              Fullscreen-Activity, Lebenszyklus
├── GameView.kt                  SurfaceView, Game-Loop, Screen-Verwaltung (GameApp)
├── data/                        DATENGETRIEBENE KONFIGURATION
│   ├── TowerData.kt             6 Türme, Stufen, Upgrade-Pfade, Kosten
│   ├── EnemyData.kt             7 Gegnertypen, Boss-Varianten, Level-Skalierung
│   ├── MapData.kt               6 Map-Themen (Farben), 6 Streckenlayouts (Wegpunkte + Bauplätze)
│   ├── LevelData.kt             50 Level: handabgestimmt (1–5) + Budget-Generator
│   └── AchievementData.kt       Erfolgsdefinitionen
├── game/                        SIMULATION & MANAGER
│   ├── GameWorld.kt             Zentrale Simulation (Gold, Burgleben, Phasen, Treffer)
│   ├── WaveManager.kt           Datengetriebenes Wellen-Spawning
│   ├── Enemy.kt / Tower.kt / Projectile.kt   Entities (gepoolt) inkl. Rendering
│   ├── Effects.kt               Partikel- & Textpools (+10 Gold, Explosionen)
│   ├── AudioManager.kt          SoundPool + prozedurale WAV-Synthese + Musik-Loops
│   ├── SaveManager.kt           SharedPreferences-Persistenz
│   └── AchievementManager.kt    Erfolgs-Logik
├── render/
│   └── MapRenderer.kt           Thematisches Map-Rendering (Boden, Deko, Weg, Burg)
└── ui/
    ├── Screen.kt / Ui.kt        Screen-Basis, Buttons, Panels, Icons
    ├── MainMenuScreen.kt        Hauptmenü mit animierter Burg-Silhouette
    ├── LevelSelectScreen.kt     Levelkarte (Sterne, Kronen, Schlösser)
    ├── GameScreen.kt            Gameplay: HUD, Bauleiste, Upgrade-Panel, Overlays
    ├── TowersInfoScreen.kt      Turm-Enzyklopädie
    ├── AchievementsScreen.kt    Erfolge
    └── SettingsScreen.kt        Musik/Sound AN/AUS
```

Neue Inhalte (Gegner, Türme, Wellen, Level, Maps) werden ausschließlich über die `data/`-Dateien hinzugefügt – die Spiellogik muss dafür nicht angefasst werden.

## Build

Voraussetzungen: JDK 17+, Android SDK (Platform 34, Build-Tools 34).

```bash
./gradlew assembleDebug        # Debug-APK: app/build/outputs/apk/debug/
./gradlew assembleRelease      # Release-APK (minify + shrink, Signierung konfigurieren)
```

Mindest-Android-Version: 8.0 (API 26), Ziel: Android 14 (API 34), Querformat.

## Steuerung

- Turm in der Bauleiste antippen → freien Bauplatz (pulsierende Ringe) antippen = bauen
- Gebauten Turm antippen → Reichweitenkreis + Panel mit Werten, Upgrade (inkl. Pfadwahl auf Stufe 3) und Verkauf
- „JETZT STARTEN" während des Countdowns = nächste Welle früher + Goldbonus
- 1x/2x = Spielgeschwindigkeit, II = Pause

## Roadmap (vorbereitet, nicht implementiert)

Weitere Maps/Layouts, tägliche Herausforderungen, neue Spielmodi, Leaderboards sowie optionale Monetarisierung lassen sich über die bestehende datengetriebene Architektur ergänzen.
