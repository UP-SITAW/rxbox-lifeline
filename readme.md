# Setup
Open the root directory in Android Studio. Build with the included gradlew or migrate to latest version. Note that there may be some required changes for newer versions of gradle.

# Dependencies
Item | Description
--- | ---
`com.github.mik3y:usb-serial-for-android:v2.2.2` | USB CDC Driver to allow communication with the arduino module over USB OTG
Retrofit2 | Librbary for RESTful API to communicate with the lifeline server

The minimum SDK version is 21 Lollipop. If you need to compile to a lower SDK version (such as for the RxBox units produced by Alexan), then you need to select a lower version of the USB serial driver that is compatible.

# Activities
1. StartActivity
2. SelectPatientActivity
3. MainActivity

# Important classes/packages
Item | Description
--- | ---
`ph.chits.rxbox.lifeline.hardware.Parser` | Decodes the arduino serial stream
`package ph.chits.rxbox.lifeline.rest.FhirService` | Defines the API according to the style of retrofit

# Future work
* Use official client library for HL7 FHIR implementation
* Improve state management
* Reactive UI design for other screen sizes