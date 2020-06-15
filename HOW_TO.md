# Implementation of the example app with MTI
This document describes the special features of the example app, or which components are involved and how.
Sufficient knowledge about developing apps on Android is assumed.
The example app was implemented with Java.

## AndroidManifest
The example app has status and uses local variables. Due to the interaction with MapTrip it is often sent to the foreground or background.
Therefore, the example app has been declared as __singleTask__ for simplicity.
        <activity
            android:name=".main.MainActivity"
            android:launchMode="singleTask"
            android:configChanges="orientation|screenSize|keyboardHidden">
            ...
        </activity>
 

## MTI Lib
The example app uses the MapTrip Interface (MTI), which provides access to the navigation features of MapTrip in the form of a simplified API.
The MTI lib file __mti.aar__ is stored in the app/libs folder and included as a dependency in the build.gradle (Module: app):

  dependencies {

    implementation fileTree(dir: 'libs', include: ['*.jar'])
    
    implementation "en.infoware:mti:8.4.3"
    
  }

## MtiHelper Class
The MTI functions work asynchronously. When the function is called, a return value is returned immediately, which is usually a request ID.
The actual processing then takes place in the background. 
Once processing is complete, the callback method associated with the function is called, which must be overwritten by the app.
The signature of the callback methods contains various parameters that vary depending on the method, but always the original request ID, which can then be assigned to the call.

The class MtiHelper implements the callback methods and encapsulates the calls of the MTI methods.

In addition, the class MtiHelper controls parts of the application logic, which in terms of good design would certainly be better handled in a dedicated class.

## MtiCallbackSynchronizer Class
As already mentioned, MTI works asynchronously.
This means that the callback method calls do not necessarily occur in the same order in which the MTI methods were called.
However, depending on the method and application logic, it may be necessary for the calling method to continue its own processing only when the corresponding callback occurs.

The class MtiCallbackSynchronizer is used for this purpose. It allows you to 'wait' for the callback, optionally with timeouts.
This class is an example implementation that was created in the course of the example app project. Certainly there are further possibilities to orchestrate asynchronous processes.

## Other classes
The remaining classes of the project implement the representation layer, file handling, logging, configuration, etc.
As the example app is intended to highlight the functionality and capabilities of MTI, these classes will not be discussed further.
