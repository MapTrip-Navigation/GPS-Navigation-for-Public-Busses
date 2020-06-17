# Realisierung der Beispiel-App mit MTI
Dieses Dokument beschreibt die Besonderheiten der Beispiel-App bzw. welche Komponenten daran beteiligt sind und wie.
Ausreichende Kenntnisse über die Entwicklung von Apps unter Android werden vorausgesetzt.
Die Beispiel-App wurde mit Java implementiert.

## AndroidManifest
Die Beispiel-App ist statusbehaftet und verwendet lokale Variablen. Durch das Zusammenspiel mit MapTrip wird sie häufig in den Vordergrund bzw. Hintergrund geschickt.
Daher wurde die Beispiel-App zur Vereinfachung als __singleTask__ deklariert.
        <activity
            android:name=".main.MainActivity"
            android:launchMode="singleTask"
            android:configChanges="orientation|screenSize|keyboardHidden">
            ...
        </activity>
 

## MTI Lib
Die Beispiel-App bedient sich des MapTrip Interface (MTI), das in Form einer vereinfachten API den Zugriff auf die Navigations-Features von MapTrip ermöglicht.
Dazu wird die MTI Lib-Datei __mti.aar__ im Ordner app/libs abgelegt und als Dependency in das build.gradle (Module: app) aufgenommen:

  dependencies {

    implementation fileTree(dir: 'libs', include: ['*.jar'])
    
    implementation "de.infoware:mti:8.4.3"
    
  }

## MtiCalls Class
Die MTI-Funktionen arbeiten asynchron. Bei Funktionsaufruf wird sofort ein Rückgabewert geliefert, bei dem es sich in der Regel um eine sogenannte Request-ID handelt.
Die eigentlich Bearbeitung erfolgt anschließend im Hintergrund. 
Nach Abschluss der Bearbeitung wird die mit der Funktion assoziierte Callback-Method aufgerufen, die von der App überschrieben werden muss.
Die Signatur der Callback-Methoden beinhaltet diverse und je nach Method abweichende Parameter, aber immer die ursprüngliche Request-ID, die dann dem Aufruf zugeordnet werden kann.

Die Klasse MtiCalls implementiert die Callback-Methoden und kapselt die Aufrufe der MTI-Methoden.

## RefRouteManager Class
Der RefRouteManager regelt das Verhalten der Anwendung in Bezug auf die nächste zu fahrende Route, die aktuelle Route und welche Routen noch zu fahren sind. Darüber hinaus entkoppelt er die GUI vollständig vom MTI.

## MtiCallbackSynchronizer Class
Wie bereits erwähnt, arbeitet MTI asynchron.
Das heißt, dass die Callback-Methoden-Aufrufe nicht zwangsläufig in der selben Reihenfolge erfolgen, wie die MTI-Methoden aufgerufen wurden.
Je nach Methode und Anwendungslogik kann es aber notwendig sein, dass die aufrufende Methode die eigene Bearbeitung erst dann fortsetzt, wenn der entsprechende Callback eintritt.

Diesem Zweck dient die Klasse MtiCallbackSynchronizer, die das 'Warten' auf den Callback ermöglicht, wahlweise mit Timeouts.
Diese Klasse ist eine Beispiel-Implementierung, die im Zuge des Beispiel-App-Projekts entstanden ist. Sicherlich gibt es weitere Möglichkeiten, asynchrone Prozesse zu orchestrieren.

## Sonstige Klassen
Die übrigen Klassen des Projekts implementieren die Repräsentationsschicht, Dateihandling, Logging, Konfiguration etc.
Da die Beispiel-App die Funktionsweise und Möglichkeiten von MTI hervorheben soll, wird auf diese Klassen nicht weiter eingegangen.
