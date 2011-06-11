package sbc.admin;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;

import sbc.gui.AdminGUI;
import sbc.gui.ProducerInterface;
import sbc.model.Nest;
import sbc.producer.Chicken;
import sbc.producer.ChocolateRabbitRabbit;

public class Admin implements MessageListener, ProducerInterface {

	private static Logger log = Logger.getLogger(Admin.class);
	
	private static int id = 1;
	
	private InitialContext ctx;
	private QueueConnectionFactory connectionFactory;
	private Queue guiQueue;
	private QueueConnection connection;
	private QueueSession session;
	private MessageConsumer consumer;

	private AdminGUI gui;

	
	public static void main(String[] args)	{
		Admin admin = new Admin();
	}
	
	public Admin()	{
		gui = new AdminGUI(this);
		gui.start();
		
		this.initConsumer();
	}
	
	protected void initConsumer() {
		try {
			ctx = new InitialContext();
			connectionFactory = (QueueConnectionFactory) ctx.lookup("SBC.Factory");
			guiQueue = (Queue) ctx.lookup("sbc.gui.queue");
			connection = connectionFactory.createQueueConnection();
			connection.start();
			
			session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			
			consumer = session.createConsumer(guiQueue);
			
			consumer.setMessageListener(this);
			
		} catch (NamingException e1) {
			e1.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void createProducers(int chicken, int eggs, int choco, int chocoRabbits) {
		
		Chicken chick;
		
		ChocolateRabbitRabbit rabbit;
		
		for(int i=0;i<chicken; i++)	{
			chick = new Chicken(new String[]{"" + id++, "" + eggs});
			chick.start();
		}
		
		for(int i=0;i<choco; i++)	{
			rabbit = new ChocolateRabbitRabbit(new String[]{"" + id++, "" + chocoRabbits});
			rabbit.start();
		}
	}
	
	@Override
	public void onMessage(Message message) {
		if(message instanceof ObjectMessage)	{
			ObjectMessage om = (ObjectMessage) message;
			try {
				if(om.getObject() == null)	{
					// do nothing
				} else if(om.getObject() instanceof Nest)	{
					gui.updateNest((Nest)om.getObject());
				} else	{
					log.error("WRONG OBJECTDATA SENT");
					return;
				}
//				log.info("given:");
//				log.info("\teggCount: " + message.getIntProperty("eggCount"));
//				log.info("\teggColorCount: " + message.getIntProperty("eggColorCount"));
//				log.info("\tchocoCount: " + message.getIntProperty("chocoCount"));
//				log.info("\tnestCount: " + message.getIntProperty("nestCount"));
//				log.info("\tnestCompletedCount: " + message.getIntProperty("nestCompletedCount"));
				gui.updateInfoData(message.getIntProperty("eggCount"),
						message.getIntProperty("eggColorCount"),
						message.getIntProperty("chocoCount"), 
						message.getIntProperty("nestCount"), 
						message.getIntProperty("nestCompletedCount"));
					

			} catch(JMSException e)	{
				e.printStackTrace();
			}
		} else	{
			log.error("RECIEVED AN INVALID MESSAGE");
		}
	}

}
