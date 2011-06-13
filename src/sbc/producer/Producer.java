package sbc.producer;

import java.util.Random;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public abstract class Producer extends Thread {

	protected InitialContext ctx;
	protected QueueConnectionFactory connectionFactory;
	protected Queue serverQueue;
	protected QueueConnection connection;
	protected QueueSession session;
	protected MessageProducer producer;

	protected int productCount;

	protected int id;
	protected int adminid;
	
	protected double failureRate;
	protected String prefix;
	protected Queue guiQueue;
	protected QueueSender guiProducer;
	
	protected TextMessage guiMsg;
	
	public Producer(String[] args)	{
		this.parseArgs(args);
	}

	public void setArgs(String[] args)	{
		this.parseArgs(args);
	}

	private void parseArgs(String[] args) {
		if(args.length < 3)	{
			throw new IllegalArgumentException("at least an ID, parent ID and the QUEUE PREFIX have to be given in arguments!");
		}
		try	{
			this.id = Integer.parseInt(args[0]);
		} catch (Exception e)	{
			throw new IllegalArgumentException("ID has to be an integer!");
		}
		
		try	{
			this.adminid = Integer.parseInt(args[1]);
		} catch (Exception e)	{
			throw new IllegalArgumentException("ID has to be an integer!");
		}
		
		this.prefix = args[2];
		
		
		if(args.length > 3)	{
			try	{
				this.productCount = Integer.parseInt(args[3]);
			} catch (Exception e)	{
				throw new IllegalArgumentException("amount has to be an integer");
			}
		} else	{
			this.productCount = 1;
		}
		
		this.failureRate = 0.2;
		
		if(args.length > 4)	{
			try	{
				this.failureRate = Double.parseDouble(args[4]);
			} catch (Exception e)	{
				throw new IllegalArgumentException("failure rate has to be a float and must be 0 <= failure rate <= 1");
			}
		}
		
	}

	protected boolean calculateDefect()	{
		return (this.failureRate >= new Random().nextDouble());
	}

	protected void init(String queue) {
		try {
			ctx = new InitialContext();
			connectionFactory = (QueueConnectionFactory) ctx.lookup("SBC.Factory");
			serverQueue = (Queue) ctx.lookup(prefix + "." + queue);
			connection = connectionFactory.createQueueConnection();
			connection.start();

			session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);


			//				consumer = (QueueReceiver) session.createConsumer(serverQueue);

			producer = session.createProducer(serverQueue);

			
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	/**
	 * inits the gui producer
	 */
	protected void initGUIProducer()	{
		try {
			guiQueue = (Queue) ctx.lookup(prefix + "." + "gui.queue");
			
			guiProducer = (QueueSender) session.createProducer(guiQueue);
			
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	protected void close() {
		try {
			this.producer.close();
			this.guiProducer.close();
			session.close();
			session = null;
			connection.stop();
			connection.close();
			connection = null;
			connectionFactory = null;
			ctx.close();
			ctx = null;
		} catch (JMSException e) {
			e.printStackTrace();
		} catch (NamingException e) {
			e.printStackTrace();
		} 
//		finally	{
//			System.exit(0);
//		}
	}
	
}
