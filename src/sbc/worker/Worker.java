package sbc.worker;

import javax.jms.JMSException;
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
	protected String prefix;

	
	protected Queue guiQueue;
	protected QueueSender guiProducer;

	protected Queue callbackQueue;
	protected QueueSender callbackProducer;
	
	public Worker(String[] args)	{
		this.parseArgs(args);
		this.initConnection();
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
		
		if(args.length > 2)	{
			try	{
				this.secondArgument = args[2];
			} catch (Exception e)	{
				throw new IllegalArgumentException("amount has to be an integer");
			}
		}
	}
	
	/**
	 * inits the jms connection
	 */
	private void initConnection()	{
		try {
			ctx = new InitialContext();
			connectionFactory = (QueueConnectionFactory) ctx.lookup("SBC.Factory");
			
			connection = connectionFactory.createQueueConnection();
			connection.start();
			
			session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * inits a producer
	 * @param producerQueue
	 */
	protected void initProducer(String producerQueue) {
		try {
			serverQueue = (Queue) ctx.lookup(prefix + "." + producerQueue);
			
			producer = (QueueSender) session.createProducer(serverQueue);
			
		} catch (NamingException e1) {
			e1.printStackTrace();
		} catch (JMSException e) {
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
	
	/**
	 * inits the callback producer (when worker is closed)
	 * @throws NamingException 
	 * @throws JMSException 
	 */
	protected void initCallbackProducer(String callbackQueueName) throws NamingException, JMSException	{
		callbackQueue = (Queue) ctx.lookup(prefix + "." + callbackQueueName);
		callbackProducer = (QueueSender) session.createProducer(guiQueue);
	}
	
	/**
	 * closes the callback producer (when worker is closed)
	 * @throws JMSException 
	 */
	protected void closeCallbackProducer() throws JMSException	{
		callbackProducer.close();
	}
	
	/**
	 * close GUI producer
	 * @throws JMSException
	 */
	protected void closeGUIProducer() throws JMSException	{
		guiProducer.close();
	}
	
	/**
	 * close producer
	 * @throws JMSException
	 */
	protected void closeProducer() throws JMSException	{
		producer.close();
	}
	

	protected abstract void initConsumer();
	
	protected abstract void close();
	
}
