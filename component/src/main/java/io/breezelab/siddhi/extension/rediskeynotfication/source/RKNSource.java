package io.breezelab.siddhi.extenstion.rediskeynotfication.source;

//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
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
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Random;

@Extension(name = "rediskeynotification", namespace = "source", description = " ", 
		/*
		 * @Parameter(name = " ", description = " " , dynamic = false/true, optional =
		 * true/false, defaultValue = " ", type = {DataType.INT, DataType.BOOL,
		 * DataType.STRING, DataType.DOUBLE, }), type = {DataType.INT, DataType.BOOL,
		 * DataType.STRING, DataType.DOUBLE, }),
		 */
parameters = {
        @Parameter(name = "redis.conn.ip",
                description = "The connection ip address to the redis.",
                type = DataType.STRING
        ),
        @Parameter(name = "redis.conn.port",
        		description = "The connection port to the redis.",
        		type = DataType.STRING
        ),
        @Parameter(name = "key.pattern",
				description = "The redis keyspace pattern.",
				type = DataType.STRING
        )
}, examples = { @Example(syntax = " ", description = " ") } )
public class RKNSource extends Source {

	public static final String REDIS_CONN_IP = "redis.conn.ip";
	public static final String REDIS_CONN_PORT = "redis.conn.port";
	public static final String REDIS_KEY_PATTERN = "key.pattern";
	private static final Logger LOG = Logger.getLogger(RKNSource.class);
	private SourceEventListener sourceEventListener;
	private Map<String, String> properties = new HashMap<>();
	private String[] aMessage;
	private String rJson;
	private String r_conn_ip;
	private String r_key_pattern;
	private Integer r_conn_port;
	private List<RKNSourceStreamListener> RKNSourceStreamListenerList = new ArrayList<>();
	private OptionHolder optionHolder;
	
	@Override
	public void connect(ConnectionCallback connectionCallback, State state)
			throws ConnectionUnavailableException {
		RKNSourceStreamListener rknSourceStreamListener = new RKNSourceStreamListener(sourceEventListener, rJson, aMessage, r_conn_ip, r_conn_port, r_key_pattern);
		LOG.info("Create Thread to running job");
		Thread th2 = new Thread(rknSourceStreamListener);
		LOG.info("Naming Thread");
		th2.setName("Jedis #1 Thread");
		th2.start();
		RKNSourceStreamListenerList.add(rknSourceStreamListener);
		while(th2.isAlive()) LOG.info(th2.getName() + "is running");
	}

	@Override
	protected ServiceDeploymentInfo exposeServiceDeploymentInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StateFactory init(SourceEventListener sourceEventListener, OptionHolder optionHolder,
			String[] requestedTransportPropertyNames, ConfigReader configReader, SiddhiAppContext siddhiAppContext) {

		this.sourceEventListener = sourceEventListener;
		this.optionHolder = optionHolder;
		r_conn_ip = optionHolder.validateAndGetStaticValue(REDIS_CONN_IP);
		r_conn_port = Integer.parseInt(optionHolder.validateAndGetStaticValue(REDIS_CONN_PORT));
		r_key_pattern = optionHolder.validateAndGetStaticValue(REDIS_KEY_PATTERN);
		return null;
	}

	@Override
	public Class[] getOutputEventClasses() {
		LOG.info("getOutputEventClasses");
		return new Class[] { Event.class, Event[].class };
	}

	@Override
	public void disconnect() {
		LOG.info("disconnect");
	}

	@Override
	public void destroy() {
		LOG.info("destroy");

	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		LOG.info("pause");

	}

	@Override
	public void resume() {
		LOG.info("resume");
	}

}
