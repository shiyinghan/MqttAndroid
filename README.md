# Mqtt Android Service

[![](https://jitpack.io/v/shiyinghan/MqttAndroid.svg)](https://jitpack.io/#shiyinghan/MqttAndroid)

The Mqtt Android Service is an MQTT client library for developing applications on Android,based on Eclipse Paho Java MQTT Client.

## Using the Mqtt Android Client

**Step 1.** Add it in your root **build.gradle** at the end of repositories:

```groovy
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

**Step 2.** Add the dependency:

```groovy
dependencies {
	        implementation 'com.github.shiyinghan:MqttAndroid:1.0.0'
	}
```

