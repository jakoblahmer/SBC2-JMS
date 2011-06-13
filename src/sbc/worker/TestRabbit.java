package sbc.worker;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.naming.NamingException;

import org.apache.log4j.Logger;

import sbc.jmsmodel.Nest;


public class TestRabbit extends Worker implements MessageListener {

	public static void main(String[] args)	{
		TestRabbit rab = new TestRabbit(args);
	}

	private static Logger log = Logger.getLogger(TestRabbit.class);

	private static String consumerName = "test.queue";


	private MessageConsumer consumer;

	private Nest nest;

	private Queue consumerQueue;

	private ObjectMessage replyMsg;

	private ObjectMessage guiMsg;


	public TestRabbit(String[] args)	{
		super(args);
		
		this.initProducer("logistic.queue");
		this.initGUIProducer();
		
		nest = null;
		
		this.addShutdownHook();
		
		this.initConsumer();
		
	}

	/**
	 * shutdown hook
	 */
	private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
            	log.info("SHUTDOWN...");
            	close();
            	this.destroy();
            }
        });
	}
	
	@Override
	protected void initConsumer() {

		try {
			log.info("listen to: " + prefix + "." + consumerName);
			consumerQueue = (Queue) ctx.lookup(prefix + "." + consumerName);

			consumer = session.createConsumer(consumerQueue);

			consumer.setMessageListener(this);

			log.info("#######################################");
			log.info("###### TEST RABBIT waiting for nest...");
			log.info("###### shutdown using Ctrl + C");
			log.info("#######################################");
			
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onMessage(Message message) {
		log.debug("RECEIVED SOMETHING ");
		if(message instanceof ObjectMessage)	{
			ObjectMessage om = (ObjectMessage) message;
			try {
				if(om.getObject() instanceof Nest)	{
					nest = (Nest) om.getObject();
					
					log.info("###### RECEIVED a nest (id: " + nest.getId() + ")");
					
//					int sleep = new Random().nextInt(3) + 1;
//					Thread.sleep(sleep * 1000);
					
					// calculate error and set it
					nest.calculateError();
					// set tested to true
					nest.setTested(true);
					nest.setTester_id(this.id);
					
					replyMsg = session.createObjectMessage(nest);
					producer.send(replyMsg);
					
					
					if(!message.propertyExists("hideFromGUI"))	{
						// update gui
						guiMsg = session.createObjectMessage(nest);
						guiMsg.setBooleanProperty("tested", true);
						guiProducer.send(guiMsg);
					}
					
					nest = null;
					log.info("#######################################");
					log.info("###### TEST RABBIT - waiting for nest to ship...");
					log.info("#######################################");

				}
//			} catch (InterruptedException e) {
//				this.close();
			} catch(JMSException e)	{
				this.close();
			}
		}
	}

	
	@Override
	protected void close() {
		try {
			consumer.setMessageListener(null);
			consumer.close();
			if(nest != null)	{
				try {
					this.initCallbackProducer("test.queue");
					ObjectMessage replyMsg = session.createObjectMessage(nest);
					replyMsg.setBooleanProperty("hideFromGUI", true);
					callbackProducer.send(replyMsg);
					this.closeCallbackProducer();
				} catch (JMSException e) {
				}
			}
			
			this.closeGUIProducer();
			this.closeProducer();
		} catch (JMSException e) {
			e.printStackTrace();
		} catch (NamingException e) {
			e.printStackTrace();
		} finally	{
			System.exit(1);
		}
	}
}
