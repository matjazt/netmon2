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

@Configuration
public class MqttInboundConfig {

    private static final Logger logger = LoggerFactory.getLogger(MqttInboundConfig.class);

    private final NetworkRepository networkRepository;

    public MqttInboundConfig(NetworkRepository networkRepository) {
        this.networkRepository = networkRepository;
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

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
