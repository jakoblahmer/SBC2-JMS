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
import sbc.model.Product;
import sbc.worker.exceptions.NoColorGivenException;


public class ColorRabbit extends Worker implements MessageListener {

	public static void main(String[] args) throws NoColorGivenException	{
		ColorRabbit rab = new ColorRabbit(args);
	}

	private static Logger log = Logger.getLogger(ColorRabbit.class);

	private static String consumerName = "sbc.color.queue";

	private MessageConsumer consumer;

	private Queue consumerQueue;

	private String color;

	private Egg egg;


	public ColorRabbit(String[] args) throws NoColorGivenException	{
		super(args);

		if(this.secondArgument == null)	{
			throw new NoColorGivenException("A color has to be given");
		}
		this.color = this.secondArgument;
		
		this.egg = null;
		
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
			consumerQueue = (Queue) ctx.lookup(consumerName);

			consumer = session.createConsumer(consumerQueue);

			consumer.setMessageListener(this);

			log.info("#######################################");
			log.info("###### COLOR RABBIT waiting for eggs...");
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
				if(om.getObject() instanceof Egg)	{
					egg = (Egg) om.getObject();
					log.info("###### RECEIVED an egg (id: " + egg.getId() + ")");

					int sleep = new Random().nextInt(3) + 1;

					Thread.sleep(sleep * 1000);

					egg.setColor(this.color);
					egg.setColorer_id(this.id);
					log.info("###### COLORED egg (" + this.color + ")");

					ObjectMessage replyMsg = session.createObjectMessage(egg);

					producer.send(replyMsg);
					egg = null;
					log.info("###### SENT egg");
					log.info("#######################################");
					log.info("###### COLOR RABBIT waiting for eggs...");
					log.info("#######################################");

				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch(JMSException e)	{
				e.printStackTrace();
			}
		}
	}


	@Override
	protected void close() {
		try {
			if(egg != null)	{
				try {
					ObjectMessage replyMsg = session.createObjectMessage(egg);
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
