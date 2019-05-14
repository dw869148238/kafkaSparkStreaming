package cn.ubwifi.probe;

import cn.ubwifi.probe.bean.ProbeStatus;
import cn.ubwifi.probe.bean.ProbeStatusResult;
import cn.ubwifi.probe.service.JedisService;
import cn.ubwifi.probe.service.KafkaProducerService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;
import scala.Tuple2;

import java.util.*;

public class SparkStreaming {
	public static final String HOST = "10.192.30.128";
	public static final int PORT = 6379;
	public static final String RESULT_TOPIC = "probe_result2druid";
	public static final int BATCH_DURATION = 60 * 2;
	public static final String KAFKA_TOPIC = "probe_status";
	public static final String KAFKA_OFFSET_KEY = "t1";
	public static final int KAFKA_PARTITIONS = 15;
	public static JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
	public static Properties kafkaProProperties = new Properties();

	private static Map<String, Object> kafkaParam = new HashMap<>();
	private static Collection<String> topics;
	private static final int IS_ONCE_ENTER = 30 * 60;
	private static JedisPool jedisPool;

	static {
		kafkaParam.put("bootstrap.servers", "kafka3.1.ubiwifi:9092,kafka3.2.ubiwifi:9092,kafka3.3.ubiwifi:9092");
		kafkaParam.put("key.deserializer", StringDeserializer.class);
		kafkaParam.put("value.deserializer", StringDeserializer.class);
		kafkaParam.put("group.id", KAFKA_OFFSET_KEY);
		kafkaParam.put("auto.offset.reset", "latest");
		kafkaParam.put("enable.auto.commit", false);
		topics = Arrays.asList(KAFKA_TOPIC);

		jedisPoolConfig.setMaxTotal(30);
		jedisPoolConfig.setMaxIdle(5);
		jedisPool = JedisService.createJedisPool(jedisPoolConfig, HOST, PORT);

		kafkaProProperties.put("bootstrap.servers", "kafka3.1.ubiwifi:9092,kafka3.2.ubiwifi:9092,kafka3.3.ubiwifi:9092");
		kafkaProProperties.put("acks", "all");
		kafkaProProperties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		kafkaProProperties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
	}

	private static Long computeDeltaTs(Long max, Long min, Long cacheValue) {
		//data in kakfa may not in order between partition, so, cacheValue sometime
		//may bigger than min or max, between different batches.
		if (cacheValue == null || cacheValue >= min) {
			return max - min;
		}
		if (min - cacheValue > IS_ONCE_ENTER) {
//			return max - min;
			//return 0 meaning that the entrance will be recomputed.
			//here may leading to some error.
			return 0L;
		}
		if (max - cacheValue <= IS_ONCE_ENTER) {
			return max - cacheValue;
		}

		return min - cacheValue;
	}

	private static Long GetCacheprobeTs(String key, Jedis jedis) {
        return Long.valueOf(0);
	}

	public static void main(String [] args) throws Exception{
		ObjectMapper objectMapper = new ObjectMapper().configure(
				DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		ObjectMapper objectMapper1 = new ObjectMapper();
		SparkConf sparkConf = new SparkConf().
				setSparkHome("/usr/hdp/2.6.5.0-292/spark2")
				.setMaster("yarn")
				.setAppName("probe2druidSparkStream")
				.set("spark.streaming.backpressure.enabled", "true")
				.set("spark.streaming.backpressure.initialRate", "2500")
				.set("spark.streaming.kafka.maxRatePerPartition", "2500");
		JavaStreamingContext javaStreamingContext = new JavaStreamingContext(sparkConf,
				Durations.seconds(BATCH_DURATION));

		Logger.getRootLogger().setLevel(Level.ERROR);

		//get the offset from redis
		boolean offsetHasZero = false;
		Map<TopicPartition, Long> fromOffsets = new HashMap<>();
		Jedis jed = jedisPool.getResource();
		for (int i= 0; i < KAFKA_PARTITIONS; i++) {
			String offset = jed.hget(KAFKA_OFFSET_KEY, String.valueOf(i));
			if (offset == null) {
				offsetHasZero = true;
				break;
			}
			TopicPartition topicPartition = new TopicPartition(KAFKA_TOPIC, Integer.valueOf(i));
			fromOffsets.put(topicPartition, Long.valueOf(offset));
		}
		jedisPool.returnResourceObject(jed);

		//initialize spark streaming
		JavaInputDStream<ConsumerRecord<String, String>> stream;
		if (offsetHasZero) {
			 stream = KafkaUtils.createDirectStream(
					javaStreamingContext,
					LocationStrategies.PreferConsistent(),
					ConsumerStrategies.<String, String>Subscribe(topics, kafkaParam)
			);
		} else {
			stream = KafkaUtils.createDirectStream(
					javaStreamingContext,
					LocationStrategies.PreferConsistent(),
					ConsumerStrategies.<String, String>Assign(fromOffsets.keySet(),
							kafkaParam, fromOffsets));
		}

        //NOTICE: if not toLowerCase, objectMapper will readValue failed.
		//NOTICE: follow use cache(), because of concurrentModificationException throwed by
		//kafkaconsumer, when using spark-submit --executor-cores bigger than 1.
        JavaDStream<ProbeStatus> javaDStream = stream.map(
        		record -> objectMapper.readValue(record.value().toLowerCase(), ProbeStatus.class)).cache();
        JavaPairDStream<String, Long> probePairDStream = javaDStream.mapToPair(
        		probeStatus -> new Tuple2<>(probeStatus.getAsset_group_id() + ":" + probeStatus.getClient_mac(),
						probeStatus.getTimestamp()));
        JavaPairDStream<String, Long> probePairMaxDStream = probePairDStream.reduceByKey((x, y) -> x >=y? x: y);
        JavaPairDStream<String, Long> probePairMinDStream = probePairDStream.reduceByKey((x, y) -> x <=y? x: y);
        JavaPairDStream<String, Tuple2<Long,Long >> probePairMaxMinDStream = probePairMaxDStream.join(probePairMinDStream);
        JavaPairDStream<String, Long> probeDeltaTsDStream = probePairMaxMinDStream.mapToPair(
        		x -> {
        			Jedis jedis = jedisPool.getResource();
        			String val = jedis.get(x._1);
        			Long cacheValue = val != null? Long.valueOf(val): 0L;
        			Long deltaTs = computeDeltaTs(x._2._1, x._2._2, cacheValue);
        			jedisPool.returnResourceObject(jedis);
        		return new Tuple2<>(x._1, deltaTs);
        		});

        //producer result to kafka (id, (deltaTs, maxTs))
		JavaPairDStream<String, Tuple2<Long, Long>> probeDeltaTsWithMaxDStream = probeDeltaTsDStream.join(probePairMaxDStream);
//		probeDeltaTsWithMaxDStream.map(r -> r.toString()).print(50);
		probeDeltaTsWithMaxDStream.foreachRDD(rdd -> rdd.foreachPartition( partition -> {
			KafkaProducer<String, String> producer = KafkaProducerService.createInstance(kafkaProProperties);
			    while (partition.hasNext()) {
			    	Tuple2<String, Tuple2<Long, Long>> record = partition.next();
			    	ProbeStatusResult result = new ProbeStatusResult();
			    	String[] s = record._1.split(":");
			    	result.setAsset_group_id(s[0]);
			    	result.setClient_mac(s[1]);
			    	result.setTimestamp(record._2._2);
			    	result.setOrig_row(record._2._1);
			    	result.setAp_mac(s[0]);
			    	producer.send(new ProducerRecord<String, String>(RESULT_TOPIC,
							objectMapper1.writeValueAsString(result)));
				}
				producer.flush();
			    KafkaProducerService.shutdownProducer(producer);
				}));

		//cache max ts to redis
		probePairMaxDStream.foreachRDD(rdd -> rdd.foreachPartition(partition -> {
			Jedis jedis = jedisPool.getResource();
			Pipeline pipeline = jedis.pipelined();
			Tuple2<String, Long> tuple2;
			while (partition.hasNext()) {
				tuple2 = partition.next();
				pipeline.set(tuple2._1, String.valueOf(tuple2._2));
				pipeline.expire(tuple2._1, IS_ONCE_ENTER);
			};
			pipeline.sync();
			jedisPool.returnResourceObject(jedis);
		}));

		//save kafka offset to redis
		stream.foreachRDD(rdd -> {
			OffsetRange[] offsetRanges = ((HasOffsetRanges) rdd.rdd()).offsetRanges();
			if (offsetRanges == null) {
				return;
			}
			Jedis jedis = jedisPool.getResource();
			Pipeline pipeline = jedis.pipelined();
			for (OffsetRange offsetRange: offsetRanges) {
				if (offsetRange == null) {
					continue;
				}
                pipeline.hset(KAFKA_OFFSET_KEY, String.valueOf(offsetRange.partition()),
						String.valueOf(offsetRange.untilOffset()));
			}
			pipeline.sync();
			jedisPool.returnResourceObject(jedis);

			//also commit to kafka, if redis clashed, we can read the latest offset
			//from kafka's self, in order to reducing the error.
			((CanCommitOffsets) stream.inputDStream()).commitAsync(offsetRanges);
		});

		// Execute the Spark workflow defined above
		javaStreamingContext.start();
		javaStreamingContext.awaitTermination();
	}
}
