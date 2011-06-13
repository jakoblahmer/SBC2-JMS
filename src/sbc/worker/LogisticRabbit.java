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


public class LogisticRabbit extends Worker implements MessageListener {

	public static void main(String[] args)	{
		LogisticRabbit rab = new LogisticRabbit(args);
	}

	private static Logger log = Logger.getLogger(LogisticRabbit.class);

	private static String consumerName = "logistic.queue";


	private MessageConsumer consumer;

	private Nest nest;

	private Queue consumerQueue;


	public LogisticRabbit(String[] args)	{
		super(args);
		
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
			log.info("###### LOGISTIC RABBIT waiting for nest to ship...");
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
				
				log.info(om.getObject());
				
				if(om.getObject() instanceof Nest)	{
					nest = (Nest) om.getObject();
					
					log.info("###### RECEIVED a nest (id: " + nest.getId() + ")");
					
//					int sleep = new Random().nextInt(3) + 1;
//					Thread.sleep(sleep * 1000);
					
					nest.setShipped(true);
					nest.setShipper_id(this.id);
					log.info("###### NEST (id: " + nest.getId() + ") shipped!");
					
					ObjectMessage replyMsg = session.createObjectMessage(nest);
					
					if(nest.isErrorFreeAndIsComplete())	{
						// nest error free and completed => write to competed nest container
						replyMsg.setBooleanProperty("completed", true);
					} else	{
						// nest has error (or is not completed) => write to error container
						replyMsg.setBooleanProperty("error", true);
					}
					
					if(!message.propertyExists("hideFromGUI"))	{
						guiProducer.send(replyMsg);
					}
					nest = null;
					log.info("#######################################");
					log.info("###### LOGISTIC RABBIT - waiting for nest to ship...");
					log.info("#######################################");

				}
//			} catch (InterruptedException e) {
//				this.close();
			} catch(JMSException e)	{
				e.printStackTrace();
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
					this.initCallbackProducer("logistic.queue");
					ObjectMessage replyMsg = session.createObjectMessage(nest);
					replyMsg.setBooleanProperty("hideFromGUI", true);
					callbackProducer.send(replyMsg);
					this.closeCallbackProducer();
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
			
			this.closeGUIProducer();
			this.closeProducer();
		} catch (JMSException e) {
			e.printStackTrace();
		} catch (NamingException e) {
			e.printStackTrace();
		} finally	{
			System.exit(0);
		}
	}
}
