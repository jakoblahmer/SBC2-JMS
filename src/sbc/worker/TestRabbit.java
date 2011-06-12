package sbc.worker;

import java.util.Random;
import java.util.Scanner;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.naming.NamingException;

import org.apache.log4j.Logger;

import sbc.model.Egg;
import sbc.model.Nest;


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
            }
        });
	}
	
	@Override
	protected void initConsumer() {

		try {
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
					
					
					// update gui
					guiMsg = session.createObjectMessage(nest);
					guiMsg.setBooleanProperty("tested", true);
					guiProducer.send(guiMsg);
					
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
			if(nest != null)	{
				try {
					ObjectMessage replyMsg = session.createObjectMessage(nest);
					replyMsg.setBooleanProperty("hideFromGUI", true);
					producer.send(replyMsg);
				} catch (JMSException e) {
				}
			}
			producer.close();
			consumer.setMessageListener(null);
			consumer.close();
			session.close();
			connection.stop();
			connection.close();
			ctx.close();
			ctx = null;
		} catch (JMSException e) {
			e.printStackTrace();
		} catch (NamingException e) {
			e.printStackTrace();
		} finally	{
			System.exit(0);
		}
	}
}
