The aim of this project is to create a Java library which can be used to record data, which is sent via Bluetooth from a HeartMan ECG device.

### Restrictions

- under Mac OS X, the JVM must be started with "-d32" argument, because the bluecove library only works for 32bit systems (see [this issue](http://code.google.com/p/bluecove/issues/detail?id=35))


## Bugs

### dyld: lazy symbol binding failed: Symbol not found: _IOBluetoothLocalDeviceReadSupportedFeatures

This is a known issue with the BlueCove library under Mac OS X 10.8 Mountain Lion. The issue is that this version of Mac OS X removed deprecated Bluetooth methods, which are needed by BlueCove. There are already some reports around the internet, for example https://code.google.com/p/bluecove/issues/detail?id=134#c5.

Fortunately, **Uwe** found a workaround by specifiying the `IOBluetooth.framework` of a previous Mac OS X version, as described in this [comment](http://www.uweschmidt.org/wiimote-whiteboard/comment-page-45#comment-246167). The comment explains how to patch Uwe's Wiimote project so it uses the correct `IOBluetooth.framework` - **chrispie** states in his [comment](http://www.uweschmidt.org/wiimote-whiteboard/comment-page-45#comment-266367) how to start Eclipse with the custom `IOBluetooth.framework`:

```
#!/bin/bash
export DYLD_LIBRARY_PATH=/path/to/folder/which/contains/IOBluetooth
cd "/Applications/eclipse/Eclipse.app/Contents/MacOS"
./eclipse
```

## Run tests

On Mac OS X you need to run the tests like this

	export DYLD_LIBRARY_PATH=/path/to/folder/which/contains/IOBluetooth
	mvn test -Dd32