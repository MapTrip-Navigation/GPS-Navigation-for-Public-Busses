# Referenzrouten-Beispiel-App
Hinweise zur Installation und Benutzung der Beispiel-App

Zum direkten Testen der Anwendung ohne build-Prozess kann das APK herunter geladen und unter Android installiert werden.
Dazu stehen vorbereitete Konfigurationsdateien sowie Referenzrouten zur Verfügung.
Mit entsprechenden Simulationsprogrammen (z.B. Lockito) können GPS-Signale generiert werden, die von MapTrip zur Navigation ausgewertet werden.

## Installation
Das APK (_refroutechains.apk_) liegt im Ordner _app/release_.
* Kopieren des APK auf den Gerätespeicher und durch Anklicken die Installation starten.
* Nach der Installation die Beispiel-App starten, damit die Dateistruktur im Gerätespeicher angelegt wird.
* Die Fehlermeldung der Beispiel-App ignorieren und die Beispiel-App beenden.

Die Beispiel-Konfiguration und die vorbereiteten Referenzrouten liegen im Ordner _sampleConfig_.
* Kopieren des Inhalts (Ordner _routes_ und _properties_-Dateien) auf den Gerätespeicher in den Ordner _/Android/data/com.refroutes/files_.

## Konfiguration
### Referenzrouten
Die Referenzrouten werden als Dateien mit der Endung .nmea oder .csv im Ordner _routes_ abgelegt.
In der Datei _refroutes.csv_ werden die Referenzrouten in der von der Navigation zu berücksichtigenden Reihenfolge abgelegt.
Existiert die Datei _refroutes.csv_ nicht, wird sie auf Basis der im Ordner liegenden Referenzrouten generiert.
Die Reihenfolge kann komfortabel über die GUI der Beispiel-App festgelegt werden.

### refroutechains.properties
Beeinflusst das Verhalten der Anwendung
startMapTrip=:TRUE|FALSE:
* TRUE: die Anwendung MapTrip wird bei Bedarf durch die Beispiel-App gestartet (default)
* FALSE: das automatische Starten von MapTrip wird unterdrückt und muss bei Bedarf manuell erfolgen

### logging.properties
loggingFileName=:path of logfile:
* Relativer Pfad des logfiles. Ohne Angabe eines Slash / vor einer optionalen Pfadangabe
* Beispiel: loggingFileName=log/logging.log

logLevel=:LogLevel:
* log level wie in java.util.logging definiert
* Beispiel: logLevel=FINEST

maxFileSize=:max file size:
* Maximale Größe einer Log-Datei bevor Rotiert wird. Die Größe ist eine Circa-Angabe und nicht auf das Byte genau.
* Beispiel: maxFileSize=1000000

## Benutzung
### GUI Elemente
Die GUI der Beispiel-App besteht im Wesentlichen aus drei Elementen.
* Switch-Button zur Aktivierung des Modus Autopilot (s.a. Modi)
* Liste zur Darstellung und Bearbeitung der Referenzrouten
* Aktions-Button zum Starten, Pausieren und Fortsetzen der Navigation. Die Beschriftung des Buttons wechselt je nach Anwendungsstatus zwischen __GO__, __PAUSE__ und __FORTSETZEN__.

### Auflistung der Referenzrouten
* Die Zielführung der Referenzrouten erfolgt in der dargestellten Reihenfolge.
* Die Reihenfolge kann durch Ziehen (kurzes Gedrückt-Halten und dann Ziehen) verändert werden.
* Die Reihenfolge wird bei jeder Änderung persistiert.
* Die Zielführung berücksichtigt nur die mit einem Haken aktivierten Routen.
* Referenzrouten können durch das Mülleimer-Symbol aus der Liste gelöscht werden.
* Aktuell ist das Hinzufügen von Referenzrouten lediglich manuell durch Eintragen in die Datei _refroutes.csv_ möglich. Eventuell ist es einfacher, diese Datei zu Löschen und die Beispiel-App neu zu starten, wodurch die Datei neu generiert wird.

### Navigation
* Voraussetzung für eine funktionierende Navigation ist, dass MapTrip gestartet wurde.
* Dass MapTrip aktiv ist, wird durch die grüne Beschriftung des Aktions-Buttons signalisiert (die ansonsten grau/schwarz ist).
* Mit Klick auf den Button __GO__ erfolgt die initiale Zielführung einer Referenzroute. Die Route wird farblich __lila__ markiert und die Navigation gestartet. Dazu wird MapTrip in den Vordergrund geholt.
* Die initiale Zielführung (also Klick auf __GO__) beginnt beim Startpunkt der Referenzroute. Das bedeutet, MapTrip navigiert zum Startpunkt der Route und von dort weiter die Route entlang (s.a. Pause).
* Referenzrouten, deren Navigation abgeschlossen ist, werden __grün__ dargestellt.
* Wurden alle Referenzrouten erreicht, wird bei Klick auf den Button __GO__ der farblich dargestellte Status aller Einträge zurück gesetzt und die Zielführung beginnt erneut mit der ersten Route.

### Pause
* Nach Start der Navigation einer Referenzroute wechselt die Beschriftung des Aktions-Buttons auf __PAUSE__.
* Wird die aktive Navigation einer Referenzroute pausiert, wechselt die farbliche Darstellung der Route auf __orange__ und die Beschriftung des Aktions-Buttons auf __FORTSETZEN__.
* Klick auf __FORTSETZEN__ setzt die Zielführung fort. Wurde die Referenzroute verlassen (z.B. um eine Mittagspause abseits der Route abzuhalten), wird - im Gegensatz zur initialen Zielführung - beim fortgesetzter Zielführung zum nächstgelegenen Punkt der Referenzroute navigiert und nicht zum Startpunkt der Referenzroute.

### Modi
Die Beispiel-App kennt zwei Betriebsmodi, die sich auf das Verhalten bei Zielerreichung auswirken.
* Etappenmodus; die Referenzrouten werden wie Etappen behandelt. Das heißt, dass nach erfolgreicher Zielführung einer Referenzroute mit MapTrip die Beispiel-App in den Vordergrund geholt wird, damit der Anwender manuell die nächste Route starten kann.
Dieser Modus eignet sich zum Beispiel gut für die Simulation einer Bus-Navigation, da bei Erreichen der Bushaltestelle nicht sofort die Navigation zum nächsten Ziel erfolgen soll.
* Autopilot; die Referenzrouten werden direkt nacheinander abgearbeitet. Die Beispiel-App wird erst nach Erreichung des letzten Ziels in den Vordergrund geholt.
Dieser Modus simuliert das Verhalten einer Müll-Entsorgungs-Navigation, die den Fahrer kontinuierlich von einem Teilstück der Gesamtroute zum nächsten führt.

### Wechseln zwischen der Anwendung und MapTrip
Das Gesamtkonzept der Lösung basiert auf dem effizienten Zusammenspiel der Beispiel-App und MapTrip.
Je nach Situation bzw. Status muss entweder die Beispiel-App oder MapTrip dem Anwender für die Benutzung zur Verfügung stehen.
Wie in den Abschnitten `Navigation` und `Modi` erklärt, erfolgt der Wechsel zwischen den beiden Anwendungen im Betrieb automatisiert bzw. durch Betätigen des Aktions-Buttons (__GO__, __FORTSETZEN__).
Zusätzlich kann der Anwender aber bei Bedarf (zum Beispiel um zu Pausieren), MapTrip in den Hintergrund schicken und die Beispiel-App in den Vordergrund holen. Dazu steht in der Kartenansicht von MapTrip ein Button mit einer Sprechblase als Symbol zur Verfügung.


