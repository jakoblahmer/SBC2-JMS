package sbc.producer;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public abstract class Producer extends Thread {

	protected InitialContext ctx;
	protected QueueConnectionFactory connectionFactory;
	protected Queue serverQueue;
	protected QueueConnection connection;
	protected QueueSession session;
	protected MessageProducer adminProducer;

	protected int productCount;

	protected int id;
	
	public Producer(String[] args)	{
		this.parseArgs(args);
		this.init();
	}

	public void setArgs(String[] args)	{
		this.parseArgs(args);
	}

	private void parseArgs(String[] args) {
		if(args.length < 1)	{
			throw new IllegalArgumentException("at least an ID has to be given in arguments!");
		}
		try	{
			this.id = Integer.parseInt(args[0]);
		} catch (Exception e)	{
			throw new IllegalArgumentException("ID has to be an integer!");
		}
		
		if(args.length > 1)	{
			try	{
				this.productCount = Integer.parseInt(args[1]);
			} catch (Exception e)	{
				throw new IllegalArgumentException("amount has to be an integer");
			}
		} else	{
			this.productCount = 1;
		}
	}


	protected void init() {
		try {
			ctx = new InitialContext();
			connectionFactory = (QueueConnectionFactory) ctx.lookup("SBC.Factory");
			serverQueue = (Queue) ctx.lookup("sbc.server.queue");
			connection = connectionFactory.createQueueConnection();
			connection.start();

			session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);


			//				consumer = (QueueReceiver) session.createConsumer(serverQueue);

			adminProducer = session.createProducer(serverQueue);

			
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
			this.adminProducer.close();
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
//		finally	{
//			System.exit(0);
//		}
	}
	
}
