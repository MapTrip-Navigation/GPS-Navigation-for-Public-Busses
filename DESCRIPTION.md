# Reference Routes Sample App
Notes on installing and using the example app

To test the application directly without the build process, the APK can be downloaded and installed on Android.
Prepared configuration files and reference routes are available for this purpose.
With appropriate simulation programs (e.g. Lockito) GPS signals can be generated, which are evaluated by MapTrip for navigation.

## Installation
The APK (_refroutechains.apk_) is located in the _app/release_ folder.
* Copy the APK to the device memory and click to start the installation.
* After installation, start the sample app to create the file structure in the device memory.
* Ignore the error message of the example app and close the example app.

The example configuration and the prepared reference routes are in the _sampleConfig_ folder.
* Copy the contents (_routes_ folder and _properties_ files) to the device memory in the _/Android/data/com.refroutes/files_ folder.

## Configuration
### Reference routes
The reference routes are stored as files with the extension .nmea or .csv in the _routes_ folder.
The reference routes are stored in the _refroutes.csv_ file in the order in which they are to be considered by the navigation.
If the _refroutes.csv_ file does not exist, it is generated based on the reference routes in the folder.
The sequence can be conveniently defined using the GUI of the example app.

### refroutechains.properties
Influences the behavior of the application
startMapTrip=:TRUE|FALSE:
* TRUE: the MapTrip application is started by the example app if required (default)
* FALSE: the automatic start of MapTrip is suppressed and must be started manually if necessary

### logging.properties
loggingFileName=:path of logfile:
* Relative path of the logfile. Without specification of a slash / before an optional path
* Example: loggingFileName=log/logging.log

logLevel=:LogLevel:
* log level as defined in java.util.logging
* Example: logLevel=FINEST

maxFileSize=:max file size:
* Maximum size of a log file before rotation. The size is an approximate value and not exact to the byte.
* Example: maxFileSize=1000000

## Use
### GUI elements
The GUI of the example app essentially consists of three elements.
* Switch button to activate the autopilot mode (see also Modes)
* List for displaying and editing the reference routes
* Action button to start, pause and continue navigation. The button label changes between __GO__, __PAUSE__, and __RESUME__ depending on the application status.

### Listing of reference routes
* The reference routes are guided to their destinations in the order shown.
* The order can be changed by dragging (press and hold briefly and then drag).
* The order is persisted with each change.
* Only routes activated with a check mark are taken into account.
* Reference routes can be deleted from the list with the trashcan symbol.
* Currently, adding reference routes is only possible manually by entering them in the _refroutes.csv_ file. It may be easier to delete this file and restart the example app, which will regenerate the file.

### Navigation
* Prerequisite for a functioning navigation is that MapTrip has been started.
* That MapTrip is active is indicated by the green label of the action button (which is otherwise grey/black).
* Clicking on the __GO__ button initiates the initial routing of a reference route. The route is highlighted in __purple__ and navigation is started. For this purpose MapTrip is brought into the foreground.
* The initial routing (i.e. click on __GO__) starts at the starting point of the reference route. This means that MapTrip navigates to the starting point of the route and from there continues along the route (see also Pause).
* Reference routes whose navigation is complete are displayed __green__.
* If all reference routes have been reached, clicking the __GO__ button resets the status of all entries shown in colour and navigation starts again with the first route.

### Pause
* After starting navigation of a reference route, the label of the action button changes to __PAUSE__.
* If active navigation of a reference route is paused, the color display of the route changes to __orange__ and the label of the action button changes to __CONTINUE__.
* Clicking on __Continue__ resumes navigation. If the reference route has been left (e.g. to take a lunch break off the route), continued navigation will navigate to the nearest point on the reference route and not to the starting point of the reference route - in contrast to initial navigation.

### Modes
The example app has two operating modes that affect the behavior when targets are reached.
* Stage mode; the reference routes are treated like stages. This means that after successfully navigating a reference route with MapTrip, the example app is brought to the foreground so that the user can manually start the next route.
This mode is well suited for simulating bus navigation, for example, because navigation to the next destination should not take place immediately after reaching the bus stop.
* Autopilot; the reference routes are processed directly one after the other. The example app is only brought to the foreground after the last destination has been reached.
This mode simulates the behavior of a garbage disposal navigation, which continuously guides the driver from one section of the overall route to the next.

### Switch between the app and MapTrip
The overall concept of the solution is based on the efficient interaction of the example app and MapTrip.
Depending on the situation or status, either the example app or MapTrip must be available for the user to use.
As explained in the sections 'Navigation' and 'Modes', switching between the two applications during operation is automated or is done by pressing the action button (__GO__, __Continue__).
In addition, however, the user can, if necessary (for example, to pause), send MapTrip to the background and bring the example app to the foreground. For this purpose, a button with a speech bubble as symbol is available in the map view of MapTrip.
