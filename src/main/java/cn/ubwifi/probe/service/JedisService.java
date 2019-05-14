package cn.ubwifi.probe.service;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.Serializable;

public class JedisService implements Serializable {
	private static final long serialVersionUID = 1L;
	private static JedisPool jedisPool;

	private JedisService() {

	}

	public static JedisPool createJedisPool(JedisPoolConfig config, String host, int port) {
		if (jedisPool == null) {
			synchronized (JedisService.class) {
				if (jedisPool == null) {
					jedisPool = new JedisPool(config, host, port);
				}
			}
		}
		return jedisPool;
	}
}
