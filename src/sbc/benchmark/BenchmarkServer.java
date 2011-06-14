package sbc.benchmark;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;

public class BenchmarkServer implements MessageListener {

	public static void main(String[] args)	{
		new BenchmarkServer(args);
	}

	private static Logger log = Logger.getLogger(BenchmarkServer.class);
	
	private int id;

	private int completedNestCount = 0;
	private int errorNestCount = 0;

	private InitialContext ctx;
	private QueueConnectionFactory connectionFactory;
	private Connection connection;
	private Session session;

	private Topic benchTopic;
	private Queue resultQueue;
	private MessageProducer benchProducer;
	private MessageConsumer resultConsumer;

	private TextMessage msg;
	private int companyCount = 0;
	private int receivedResults = 0;;
	
	
	public BenchmarkServer(String[] args) {
		this.parseArguments(args);
		
		this.initTopic();
		
		log.info("######################################");
		log.info("# BENCHMARK SERVER ###################");
		log.info("######################################");
		log.info("# press ENTER to start benchmark...");
//		Scanner sc = new Scanner(System.in);
//		sc.nextLine();
		log.info("# benchmark started...");
		this.startBenchmark();
//		benchmarkTimer = new Timer();
//		benchmarkTimer.schedule(new StopBenchmark(), 60000);
		try {
			Thread.sleep(60000);
			log.info("60 seconds");
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally	{
			this.stopBenchmark();
		}
	}


	private void initTopic() {
		try {
			ctx = new InitialContext();
			connectionFactory = (QueueConnectionFactory) ctx.lookup("SBC.Factory");
			connection = connectionFactory.createConnection();
			connection.start();
			
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			
			// lookup topic (start stop...)
			benchTopic = (Topic) ctx.lookup("bench.topic");
			benchProducer = session.createProducer(benchTopic);
			
			resultQueue = (Queue) ctx.lookup("bench.result.queue");
			resultConsumer = session.createConsumer(resultQueue);
			
			resultConsumer.setMessageListener(this);
		} catch (NamingException e1) {
			e1.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}


	/**
	 * starts the benchmark
	 */
	private void startBenchmark() {
		System.out.println("START BENCHMARK");
		
		// send start signal to queue
		try {
			msg = session.createTextMessage();
			msg.setStringProperty("START", "1");
			msg.setJMSExpiration(100);
			benchProducer.send(msg);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	/**
	 * stops the benchmark
	 */
	private void stopBenchmark() {
		System.out.println("STOP BENCHMARK");
		
		// send start signal to queue
		try {
			msg = session.createTextMessage();
			msg.setStringProperty("STOP", "1");
			benchProducer.send(msg);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	/**
	 * COLLECT RESULTS
	 */
	@Override
	public void onMessage(Message message) {
		try {
			if(message.getJMSRedelivered())
				return;
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if(message instanceof TextMessage)	{
			try {
				
				this.completedNestCount += message.getIntProperty("completed");
				this.errorNestCount += message.getIntProperty("error");
				
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			receivedResults++;
		}
		
		if(receivedResults >= this.companyCount)	{
			System.out.println("###################################");
			System.out.println("### RESULT:");
			System.out.println("###################################");
			System.out.println("	completed: " + completedNestCount);
			System.out.println("	error: " + errorNestCount);
			System.out.println("	---------------------");
			System.out.println("	SUM: " + (errorNestCount + completedNestCount));
			
		}
	}

	
	/**
	 * parses the arguments, expected arguments
	 * 
	 * 	- ID of benchmarkServer
	 * 	- number of companies
	 * @param args
	 */
	private void parseArguments(String[] args) {
//		/*** DISABLED FOR DEBUG **
		if(args.length < 2)	{
			throw new IllegalArgumentException("at least an ID and the number of companies have to be given!");
		}
//		*/
		try	{
			this.id = Integer.parseInt(args[0]);
		} catch (Exception e)	{
			throw new IllegalArgumentException("ID has to be an integer!");
		}
		
		try	{
			this.companyCount = Integer.parseInt(args[1]);
		} catch (Exception e)	{
			throw new IllegalArgumentException("companyCount has to be an integer!");
		}
	}

	
	/**
	 * close the benchmark server
	 */
	private void close() {
		try {
			benchProducer.close();
			resultConsumer.close();
			session.close();
			connection.close();
			ctx.close();
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally	{
			System.exit(0);
		}
	}
}



