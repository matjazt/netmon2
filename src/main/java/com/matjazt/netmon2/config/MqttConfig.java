package com.matjazt.netmon2.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;

import java.io.FileInputStream;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

@Configuration
public class MqttConfig {

    @Bean
    public MqttPahoClientFactory mqttClientFactory(MqttProperties props) {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[] {props.getUrl()});
        options.setUserName(props.getUsername());
        options.setPassword(props.getPassword().toCharArray());
        options.setAutomaticReconnect(props.isAutomaticReconnect());
        options.setCleanSession(props.isCleanSession());
        options.setConnectionTimeout(props.getConnectionTimeout());
        options.setKeepAliveInterval(props.getKeepAliveInterval());

        // Configure TLS/SSL if using ssl:// protocol
        if (props.getUrl().startsWith("ssl://")) {
            try {
                configureSsl(options, props);
            } catch (Exception e) {
                throw new RuntimeException("Failed to configure MQTT SSL/TLS", e);
            }
        }

        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(options);
        return factory;
    }

    private void configureSsl(MqttConnectOptions options, MqttProperties props) throws Exception {
        // If custom truststore specified, load it
        if (props.getTruststorePath() != null && !props.getTruststorePath().isEmpty()) {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (FileInputStream fis = new FileInputStream(props.getTruststorePath())) {
                trustStore.load(
                        fis,
                        props.getTruststorePassword() != null
                                ? props.getTruststorePassword().toCharArray()
                                : new char[0]);
            }

            TrustManagerFactory tmf =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            options.setSocketFactory(sslContext.getSocketFactory());
        } else {
            // Use JVM's default truststore
            options.setSocketFactory(SSLSocketFactory.getDefault());
        }

        // Optionally disable hostname verification (not recommended for production)
        if (!props.isSslVerifyHostname()) {
            options.setSSLHostnameVerifier((hostname, session) -> true);
        }
    }
}
