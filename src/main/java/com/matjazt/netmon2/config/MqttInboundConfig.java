package com.matjazt.netmon2.config;

import com.matjazt.netmon2.repository.NetworkRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;

/**
 * Configuration for MQTT inbound message handling via Spring Integration.
 *
 * <p>Creates a message-driven channel adapter that subscribes to MQTT topics and routes incoming
 * messages to the {@code mqttInputChannel}. Topic subscriptions are dynamically generated from
 * networks in the database using the configured topic template.
 *
 * <p>Example: If topic template is "network/{networkName}/scan" and database contains networks
 * "HomeNetwork" and "OfficeNetwork", subscribes to:
 *
 * <ul>
 *   <li>network/HomeNetwork/scan
 *   <li>network/OfficeNetwork/scan
 * </ul>
 *
 * <p>Messages received on subscribed topics are delivered to {@code mqttInputChannel} where they
 * are handled by {@link com.matjazt.netmon2.service.MqttService} with the {@code @ServiceActivator}
 * annotation.
 */
@Configuration
public class MqttInboundConfig {

    private static final Logger logger = LoggerFactory.getLogger(MqttInboundConfig.class);

    private final NetworkRepository networkRepository;

    public MqttInboundConfig(NetworkRepository networkRepository) {
        this.networkRepository = networkRepository;
    }

    /**
     * Creates direct message channel for MQTT messages.
     *
     * <p>DirectChannel delivers messages synchronously in the sender's thread. Used for receiving
     * MQTT messages and delivering them to the service activator.
     *
     * @return message channel for MQTT messages
     */
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    /**
     * Creates MQTT inbound message adapter that subscribes to topics.
     *
     * <p>Queries all networks from database and generates topic subscriptions using the topic
     * template pattern. Subscribes to all topics and delivers messages to {@code mqttInputChannel}.
     *
     * @param props MQTT configuration properties
     * @param factory MQTT client factory
     * @return message producer for MQTT messages
     */
    @Bean
    public MessageProducer mqttInbound(MqttProperties props, MqttPahoClientFactory factory) {
        // Obtain list of networks from database and create topics based on template
        var networks = networkRepository.findAll();
        String topicTemplate = props.getTopicTemplate();
        var topicList = new String[networks.size()];
        int index = 0;
        for (var network : networks) {
            String topic = topicTemplate.replace("{networkName}", network.getName());
            topicList[index++] = topic;
            logger.info("Subscribing to topic: {}", topic);
        }

        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(props.getClientId(), factory, topicList);

        adapter.setCompletionTimeout(props.getCompletionTimeout());
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(props.getQos());
        adapter.setOutputChannel(mqttInputChannel());

        return adapter;
    }
}
