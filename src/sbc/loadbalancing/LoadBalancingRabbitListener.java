package sbc.loadbalancing;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class LoadBalancingRabbitListener extends Thread implements MessageListener {

	private String controlConsumerName = "gui.queue";
	private String eggConsumerName = "gui.queue";
	private String chocoConsumerName = "gui.queue";
	private InitialContext ctx;
	private QueueSession session;
	private QueueConnectionFactory connectionFactory;
	private QueueConnection connection;
	private String prefix;
	private ILoadBalancingCallback callback;
	private MessageConsumer controlConsumer;

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
			
			consumerQueue = (Queue) ctx.lookup(prefix + "." + controlConsumerName);
			controlConsumer = session.createConsumer(consumerQueue);
			
			
			
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
		} catch (JMSException e) {}
	}
}
