/*
 *  Copyright (c) 2017 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package io.breezelab.siddhi.extenstion.rediskeynotfication.source;

import io.siddhi.annotation.Example;
import io.siddhi.annotation.Extension;
import io.siddhi.annotation.Parameter;
import io.siddhi.annotation.util.DataType;
import io.siddhi.core.config.SiddhiAppContext;
import io.siddhi.core.event.Event;
import io.siddhi.core.exception.ConnectionUnavailableException;
import io.siddhi.core.stream.ServiceDeploymentInfo;
import io.siddhi.core.stream.input.source.Source;
import io.siddhi.core.stream.input.source.SourceEventListener;
import io.siddhi.core.util.config.ConfigReader;
import io.siddhi.core.util.snapshot.state.State;
import io.siddhi.core.util.snapshot.state.StateFactory;
import io.siddhi.core.util.transport.OptionHolder;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import org.apache.log4j.Logger;
import java.net.ConnectException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;;
import java.util.Properties;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.lang.Object;

/**
 * The class implementing Email Source message listener to listen incoming email Messages.
 */
public class RKNSourceStreamListener implements Runnable {
    private static final Logger LOG = Logger.getLogger(RKNSourceStreamListener.class);
    private SourceEventListener sourceEventListener;
	private final Lock consumerLock = new ReentrantLock();
    private String[] aMessage;
    private String rJson;
    private String conn_ip;
    private Integer conn_port;
    private String key_pattern;
    
    private ReentrantLock lock;
    private Condition condition;
    private volatile boolean paused;
    private volatile boolean inactive;
	private JedisPool jedisPool;

    RKNSourceStreamListener(SourceEventListener sourceEventListener, String rJson, String[] aMessage, String conn_ip, Integer conn_port, String key_pattern  ) {
        this.sourceEventListener = sourceEventListener;
        this.conn_ip = conn_ip;
        this.conn_port = conn_port;
        this.key_pattern = key_pattern;
        lock = new ReentrantLock();
        condition = lock.newCondition();

		LOG.info("in RKNSourceStreamListener");
    }

    @Override
    public void run() {
		LOG.info("Redis connect is trying");

		jedisPool = new JedisPool(new JedisPoolConfig(), conn_ip, conn_port, 1000);
		
		LOG.info("Redis connect is working");
		
		Jedis tjedis = jedisPool.getResource();
		tjedis.psubscribe(new JedisPubSub() {
			@Override
			public void onPSubscribe(String pattern, int subscribedChannels) {}
			
			@Override
			public void onPMessage(String pattern, String keyspace, String keyop) {
				rJson = "{\t\"pattern\": \"" + pattern + "\",\t\"keyspace\": \"" + keyspace.substring(15, keyspace.length()) + "\",\t\"operation\": \"" + keyop + "\" }";
				LOG.info("onPMessage : rJson : " + rJson);
				String[] t_rJson = new String[] { rJson };
				Object rObj = t_rJson;
				sourceEventListener.onEvent(rObj, null);
				
			}
		}, key_pattern);
        
	}
	
    void pause() {
        paused = true;
    }

    void resume() {
        restore();
        paused = false;
        try {
            lock.lock();
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

	void restore() {
        paused = false;
	}
	
    void shutdownConsumer() {
        try {
            consumerLock.lock();
        } finally {
            consumerLock.unlock();
        }
        inactive = true;
    }

}
