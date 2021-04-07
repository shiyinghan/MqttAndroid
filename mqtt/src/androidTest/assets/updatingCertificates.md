# How to Add certificates to the BKS keystore

## Getting the certificates

```openssl s_client -connect mqtt.eclipseprojects.io:8883 -showcerts```

Notes: you need all certificates in chain (Copy each to a .crt file)

## Adding to keystore

```keytool -importcert -v -trustcacerts -file "mqtt.eclipseprojects.io.crt" -alias mqtt.eclipseprojects.io -keystore "test.bks" -provider org.bouncycastle.jce.provider.BouncyCastleProvider -providerpath "bcprov-jdk15to18-168.jar" -storetype BKS -storepass mqtttest```
