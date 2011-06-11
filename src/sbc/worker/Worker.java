package sbc.worker;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public abstract class Worker {

	protected int id;
	
	protected InitialContext ctx;
	protected QueueConnectionFactory connectionFactory;
	protected QueueConnection connection;
	protected QueueSession session;
	protected Queue serverQueue;
	protected QueueSender producer;

	protected String secondArgument;
	
	protected static String producerName = "sbc.server.queue";
	
	public Worker(String[] args)	{
		this.parseArgs(args);
		this.initProducer();
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
				this.secondArgument = args[1];
			} catch (Exception e)	{
				throw new IllegalArgumentException("amount has to be an integer");
			}
		}
	}
	
	protected void initProducer() {
		try {
			ctx = new InitialContext();
			connectionFactory = (QueueConnectionFactory) ctx.lookup("SBC.Factory");
			serverQueue = (Queue) ctx.lookup(producerName);
			connection = connectionFactory.createQueueConnection();
			connection.start();
			
			session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			
			producer = (QueueSender) session.createProducer(serverQueue);
			
		} catch (NamingException e1) {
			e1.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	protected abstract void initConsumer();
	
	protected abstract void close();
	
}
