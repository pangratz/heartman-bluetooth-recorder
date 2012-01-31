The aim of this project is to create a Java library which can be used to record data, which is sent via Bluetooth from a HeartMan ECG device.

### Restrictions

- under Mac OS X, the JVM must be started with "-d32" argument, because the bluecove library only works for 32bit systems (see [this issue](http://code.google.com/p/bluecove/issues/detail?id=35))