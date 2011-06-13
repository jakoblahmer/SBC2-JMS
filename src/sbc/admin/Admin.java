package sbc.admin;

import java.util.concurrent.atomic.AtomicInteger;

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
import sbc.jmsmodel.Nest;
import sbc.producer.Chicken;
import sbc.producer.ChocolateRabbitRabbit;

public class Admin implements MessageListener, ProducerInterface {

	private static Logger log = Logger.getLogger(Admin.class);
	
	private int id = 1;
	
	private InitialContext ctx;
	private QueueConnectionFactory connectionFactory;
	private Queue guiQueue;
	private QueueConnection connection;
	private QueueSession session;
	private MessageConsumer consumer;

	private AdminGUI gui;

	private String prefix;

	private static AtomicInteger chickenID = new AtomicInteger(0);
	private static AtomicInteger chocoRabbitID = new AtomicInteger(0);
	
	
	public static void main(String[] args)	{
		Admin admin = new Admin(args);
	}
	
	public Admin(String[] args)	{
		parseArgs(args);
		
		gui = new AdminGUI(this, "" + this.id);
		gui.start();
		
		this.initConsumer();
	}
	
	private void parseArgs(String[] args) {
		if(args.length < 2)	{
			throw new IllegalArgumentException("at least an ID and the QUEUE PREFIX have to be given in arguments!");
		}
		try	{
			this.id = Integer.parseInt(args[0]);
		} catch (Exception e)	{
			throw new IllegalArgumentException("ID has to be an integer!");
		}
		
		this.prefix = args[1];
	}
	
	protected void initConsumer() {
		try {
			ctx = new InitialContext();
			connectionFactory = (QueueConnectionFactory) ctx.lookup("SBC.Factory");
			guiQueue = (Queue) ctx.lookup(this.prefix + ".gui.queue");
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
			chick = new Chicken(new String[]{"" + chickenID.incrementAndGet(), "" + this.id, this.prefix, "" + eggs});
			chick.start();
		}
		
		for(int i=0;i<choco; i++)	{
			rabbit = new ChocolateRabbitRabbit(new String[]{"" + chocoRabbitID.incrementAndGet(), "" + this.id,  this.prefix, "" + chocoRabbits});
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
					Nest nest = (Nest)om.getObject();
					
					if(message.propertyExists("build"))	{
						gui.addNest(nest);
					}
					
					if(message.propertyExists("tested"))	{
						gui.updateNest(nest);
					}
					
					if(message.propertyExists("error"))	{
						gui.addErrorNest(nest);
					}
					
					if(message.propertyExists("completed"))	{
						gui.addCompletedNest(nest);
					}
					
				} else	{
					log.error("WRONG OBJECTDATA SENT");
					return;
				}
			} catch(JMSException e)	{
				e.printStackTrace();
			}
		} else if(message instanceof javax.jms.TextMessage)	{
			
			try {
				// update egg count
				if(message.propertyExists("eggCount"))
					gui.updateEgg(message.getIntProperty("eggCount"));
				
				// add colored egg (removes not colored egg automatically)
				if(message.propertyExists("eggColorCount"))
					gui.addColoredEgg(message.getIntProperty("eggCount"));
				
				
				if(message.propertyExists("chocoCount"))
					gui.updateChoco(message.getIntProperty("chocoCount"));
				
				
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else	{
			log.error("RECIEVED AN INVALID MESSAGE");
		}
	}

}
