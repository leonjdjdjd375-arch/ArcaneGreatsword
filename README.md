# Arcane Greatsword – Paper 26.2

Ein fertiges Paper-Plugin-Projekt für Minecraft/Paper 26.2.

## Funktionen

- **Arcane Greatsword** als Netherite-Sword mit eigener Item-Model-Texture.
- Linksklick / normaler Schwerttreffer: **24 Schaden = 12 Herzen** vor Rüstung.
- Das sind **7 Herzen mehr** als ein Sharpness-III-Netherite-Schwert mit 5 Herzen Grundschaden.
- Beim normalen Treffer: viele **hellblaue Partikel**, Crits, End-Rods und Sweep-Sound.
- Rechtsklick: Spezialangriff, wenn der Spieler einen LivingEntity-Gegner innerhalb von 30 Blöcken anschaut.
- Rechtsklick funktioniert **nicht**, wenn in der Nebenhand ein Schild gehalten wird.
- Spezialangriff: ein großes fliegendes Schwert wird zum Ziel geschickt.
- Spezialangriff macht **20 Schaden = 10 Herzen** vor Rüstung.
- 8 Sekunden Cooldown.
- Spezialangriff erzeugt zusätzlich hellblaue Trail-/Impact-Effekte und Sounds.

## IntelliJ IDEA

Paper empfiehlt IntelliJ IDEA + Gradle für die Plugin-Entwicklung. Für die aktuelle Paper-26.x-Entwicklung wird Java 25 benötigt.

1. Installiere **JDK 25**.
2. Öffne diesen Ordner in IntelliJ IDEA.
3. IntelliJ erkennt `build.gradle.kts` als Gradle-Projekt.
4. Warte, bis Gradle alle Dependencies geladen hat.
5. Rechts bei Gradle: `Tasks -> build -> build`.
6. Die Plugin-JAR liegt danach in `build/libs/ArcaneGreatsword-1.0.0.jar`.
7. JAR in den `plugins`-Ordner deines Paper-Servers kopieren.
8. Server neu starten.
9. Im Spiel `/arcane give` eingeben.

## Resource Pack

Der Server-Code setzt die neue `minecraft:item_model`-Komponente auf
`arcane_greatsword:arcane_greatsword`.

Deshalb muss der Client das Resource Pack aktiv haben, sonst sieht das Item wie ein normales Netherite-Schwert aus.

Im Ordner `src/main/resources/resourcepack` liegt das Pack. Den Inhalt dieses Ordners als ZIP packen und in Minecraft unter `resourcepacks` aktivieren.

## Wichtiger Punkt zur Texture

Die Texture ist bewusst als **quadratisches Item-Icon** angelegt. Das lange Originalbild sollte nicht direkt als Item-Texture verwendet werden, weil es im Inventar sonst winzig/dünn wirkt.
