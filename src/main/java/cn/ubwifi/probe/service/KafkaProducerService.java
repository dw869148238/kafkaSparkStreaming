package cn.ubwifi.probe.service;

import org.apache.kafka.clients.producer.KafkaProducer;

import java.io.Serializable;
import java.util.Properties;

public class KafkaProducerService implements Serializable {

	public static KafkaProducer<String, String> createInstance(Properties properties) {
		return new KafkaProducer<String, String>(properties);
	}

	public static void shutdownProducer(KafkaProducer<String, String> producer) {
		producer.close();
	}
}
