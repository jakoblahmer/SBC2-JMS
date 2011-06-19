package sbc.loadbalancing;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class LoadBalancingRabbitListener extends Thread implements MessageListener {

	private String controlConsumerName = "gui.queue";
	private String eggQueueName = "color.queue";
	private String chocoQueueName = "build.queue";
	private InitialContext ctx;
	private QueueSession session;
	private QueueConnectionFactory connectionFactory;
	private QueueConnection connection;
	private String prefix;
	private ILoadBalancingCallback callback;
	private MessageConsumer controlConsumer;
	private MessageConsumer eggConsumer;
	private MessageConsumer chocoConsumer;
	private QueueSender eggProducer;
	private QueueSender chocoProducer;
	private final static String messageSelectorCHOCO = "product = 'chocolateRabbit'";
	private static final long CONSUMER_TIMEOUT = 100; 

	public LoadBalancingRabbitListener(String prefix) {
		this.prefix = prefix;
	}

	public void setProductListener(ILoadBalancingCallback callback) {
		this.callback = callback;
	}
	
	/**
	 * inits the jms connection
	 */
	private void initConnection()	{
		try {
			/*
			String endpoints = "hostname:port,hostname:port";
			Properties p = new Properties();
			p.put("com.sun.appserv.iiop.endpoints", endpoints);
			p.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.enterprise.naming.SerialInitContextFactory");
			p.put(Context.URL_PKG_PREFIXES, "com.sun.enterprise.naming");
			p.put(Context.STATE_FACTORIES, "com.sun.corba.ee.impl.presentation.rmi.JNDIStateFactoryImpl");
			p.put("com.sun.corba.ee.transport.ORBTCPTimeouts", "500:30000:30:999999");
			ctx = new InitialContext(p);
			*/
			
			ctx = new InitialContext();
			connectionFactory = (QueueConnectionFactory) ctx.lookup("SBC.Factory");
			
			connection = connectionFactory.createQueueConnection();
			connection.start();
			
			session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	@Override
	public void run() {
		try {
			Queue consumerQueue = (Queue) ctx.lookup(prefix + "." + controlConsumerName);
			controlConsumer = session.createConsumer(consumerQueue);
			
			Queue eggQueue = (Queue) ctx.lookup(prefix + "." + eggQueueName);
			eggConsumer = session.createConsumer(eggQueue);
			eggProducer = (QueueSender) session.createProducer(eggQueue);
			
			Queue chocoQueue = (Queue) ctx.lookup(prefix + "." + chocoQueueName);
			chocoConsumer = session.createConsumer(chocoQueue, messageSelectorCHOCO);
			chocoProducer = (QueueSender) session.createProducer(chocoQueue);
			
			controlConsumer.setMessageListener(this);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void close() {
		try {
			session.close();
			connection.stop();
			connection.close();
			connectionFactory = null;
			ctx.close();
			ctx = null;
		} catch (JMSException e) {
			e.printStackTrace();
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}
	
	public ArrayList<Message> takeEggs(int amount) {
		ArrayList<Message> list = new ArrayList<Message>();
		for (int i = 0; i < amount; i++) {
			try {
				list.add(eggConsumer.receive(CONSUMER_TIMEOUT));
			} catch (JMSException e) {
				break;
			}
		}
		return list;
	}

	public void putEggs(List<Message> messages) {
		for (Message message : messages) {
			try {
				eggProducer.send(message);
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
	}
	
	public ArrayList<Message> takeChokobunnies(int amount) {
		ArrayList<Message> list = new ArrayList<Message>();
		for (int i = 0; i < amount; i++) {
			try {
				list.add(chocoConsumer.receive(CONSUMER_TIMEOUT));
			} catch (JMSException e) {
				break;
			}
		}
		return list;
	}

	public void putChokobunnies(List<Message> messages) {
		for (Message message : messages) {
			try {
				chocoProducer.send(message);
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void onMessage(Message message) {
		try {
			if (message.propertyExists("eggCount")) {
				int amount = message.getIntProperty("eggCount");
				if (callback != null) {
					callback.addEggs(prefix, amount);
				}
			}
			if (message.propertyExists("chocoCount")) {
				Integer amount = message.getIntProperty("chocoCount");
				if (amount != null && callback != null) {
					callback.addChocolateRabbits(prefix, amount);
				}
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

}
