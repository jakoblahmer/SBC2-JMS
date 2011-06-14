package sbc.worker;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.naming.NamingException;

import org.apache.log4j.Logger;

import sbc.jmsmodel.Egg;
import sbc.worker.colorRabbit.Colors;
import sbc.worker.exceptions.NoColorGivenException;


public class ColorRabbit extends Worker implements MessageListener {

	public static void main(String[] args) throws NoColorGivenException	{
		ColorRabbit rab = new ColorRabbit(args);
	}

	private static Logger log = Logger.getLogger(ColorRabbit.class);
	
	private static String messageSelector = "[COLOR] = '0' OR NOCOLOR = '1'";
	
	private static String consumerName = "color.queue";

	private MessageConsumer consumer;

	private Queue consumerQueue;
	private MessageProducer notCompletelyColoredProducer;

	private String color;

	private Egg egg;

	private ObjectMessage replyMsg;

	private TextMessage guiMsg;



	public ColorRabbit(String[] args) throws NoColorGivenException	{
		super(args);
		
		if(this.secondArgument == null)	{
			throw new NoColorGivenException("A color has to be given");
		}
		this.color = this.secondArgument;
		
		boolean error = true;
		for(Colors s : Colors.values())	{
			if(this.color.equals(s.toString()))
				error = false;
		}
		if(error)	{
			throw new NoColorGivenException("COLOR " + this.color + " is not a valid color");
		}
		
		this.initProducer("build.queue");
		this.initGUIProducer();
		
		// adopt message selector
		messageSelector = messageSelector.replace("[COLOR]", this.color);
		log.info(messageSelector);
		
		this.egg = null;
		
//		this.addShutdownHook();
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

			consumer = session.createConsumer(consumerQueue, messageSelector);
			notCompletelyColoredProducer = session.createProducer(consumerQueue);

			consumer.setMessageListener(this);

			log.info("#######################################");
			log.info("###### COLOR RABBIT (" + this.color + ") waiting for eggs...");
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
					log.info(this.color + " GOT: " + egg + ")");
					
//					int sleep = new Random().nextInt(3) + 1;
//					Thread.sleep(sleep * 1000);
					if(!egg.isColored())	{
						egg.addColor(this.color, this.id);
					}

					replyMsg = session.createObjectMessage(egg);
					
					// egg is colored, send to server
					if(egg.isColored())	{
						replyMsg.setStringProperty("product", "egg");
						producer.send(replyMsg);
						
						if(!message.propertyExists("hideFromGUI"))	{
							guiMsg = session.createTextMessage();
							guiMsg.setIntProperty("eggCount", -1);
							guiMsg.setIntProperty("eggColorCount", 1);
							guiProducer.send(guiMsg);
						}
						
						log.debug(this.color + " SENT TO SERVER: " + egg + ")");
					}
					
					// egg is not completely colored => send to same queue
					else	{
						for(Colors col : Colors.values())	{
							replyMsg.setStringProperty(col.toString(), (egg.getColor().contains(col.toString()) ? "1" : "0"));
						}
						replyMsg.setJMSPriority(9);
						notCompletelyColoredProducer.send(replyMsg);
					}
					
					
					egg = null;
//					log.info("###### SENT egg");
//					log.info("#######################################");
//					log.info("###### COLOR RABBIT waiting for eggs...");
//					log.info("#######################################");

				}
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
			} catch(JMSException e)	{
				e.printStackTrace();
			}
		}
	}


	@Override
	protected void close() {
		try {
			// first close consumer
			consumer.setMessageListener(null);
			consumer.close();
			if(egg != null)	{
				// if an egg is set, create callbackproducer
				try {
					ObjectMessage replyMsg = session.createObjectMessage(egg);
					for(Colors col : Colors.values())	{
						replyMsg.setStringProperty(col.toString(), (egg.getColor().contains(col.toString()) ? "1" : "0"));
					}
					replyMsg.setBooleanProperty("hideFromGUI", true);
					notCompletelyColoredProducer.send(replyMsg);
					
				} catch (Exception e) {
				}
			}
			
			notCompletelyColoredProducer.close();
			
			this.closeGUIProducer();
			this.closeProducer();
			
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
