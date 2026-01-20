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

/**
 * Configuration for MQTT client factory with TLS/SSL support.
 *
 * <p>Creates a {@link MqttPahoClientFactory} bean configured with connection options from {@link
 * MqttProperties}. Automatically enables TLS when broker URL uses "ssl://" protocol.
 *
 * <p>TLS configuration supports:
 *
 * <ul>
 *   <li>Custom truststore (JKS format) for self-signed CA certificates
 *   <li>Default JVM truststore for certificates signed by well-known CAs
 *   <li>Optional hostname verification bypass (not recommended for production)
 * </ul>
 *
 * <p>Connection options include:
 *
 * <ul>
 *   <li>Automatic reconnection on connection loss
 *   <li>Clean/persistent session management
 *   <li>Configurable timeouts and keep-alive intervals
 *   <li>Username/password authentication
 * </ul>
 */
@Configuration
public class MqttConfig {

    /**
     * Creates MQTT client factory with configured connection options.
     *
     * @param props MQTT configuration properties
     * @return configured MQTT client factory
     * @throws RuntimeException if TLS configuration fails
     */
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
            /**
             * Configures SSL/TLS socket factory for MQTT connection.
             *
             * <p>If custom truststore path is specified in properties, loads certificates from JKS
             * truststore. Otherwise uses JVM's default truststore (cacerts). Optionally disables
             * hostname verification if configured (not recommended for production).
             *
             * @param options MQTT connection options to configure
             * @param props MQTT configuration properties
             * @throws Exception if truststore loading or SSL context initialization fails
             */
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
