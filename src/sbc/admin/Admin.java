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
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
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

	private boolean RUN_GUI = true;

	private int benchmarkChicks;
	private int benchmarkChoco;

	private int completedNestCount;
	private int errorNestCount;

	private Topic benchTopic;

	private TopicSubscriber benchSubscriber;

	private ChocolateRabbitRabbit[] benchRabbits;

	private Chicken[] benchChickens;

	private static AtomicInteger chickenID = new AtomicInteger(0);
	private static AtomicInteger chocoRabbitID = new AtomicInteger(0);
	
	
	private int eggCount = 0;
	private int coloredEggCount = 0;
	private int chocoCount = 0;
	
	
	public static void main(String[] args)	{
		Admin admin = new Admin(args);
	}
	
	public Admin(String[] args)	{
		parseArgs(args);
		
		if(RUN_GUI)	{
			gui = new AdminGUI(this, "" + this.id);
			gui.start();
		} else	{
			completedNestCount = 0;
			errorNestCount = 0;
		}
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
		
		
		if(args.length > 2)	{
			RUN_GUI = Boolean.parseBoolean(args[2]);
			
			if(!RUN_GUI && args.length < 4)	{
				throw new IllegalArgumentException("benchmark expects parameters: 'id' 'queue prefix' 'RUN_GUI (boolean)' 'number of chickens (int)' 'number of chocoRabbits (int)'");
			}
			
			try	{
				benchmarkChicks = Integer.parseInt(args[3]);
				benchmarkChoco = Integer.parseInt(args[4]);
			} catch (Exception e)	{
				throw new IllegalArgumentException("number of chickens / rabbits has to be an integer");
			}
		}
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
			
			if(RUN_GUI)	{
				consumer.setMessageListener(this);
			} else	{
				this.initBenchmark();
			}
			
		} catch (NamingException e1) {
			e1.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	/**
	 * sets values for benchmark mode
	 * @throws JMSException 
	 * @throws NamingException 
	 */
	private void initBenchmark() throws JMSException, NamingException {
		
		// init listener
		consumer.setMessageListener(new BenchmarkMessageListener());
		
		// lookup topic (start stop...)
		benchTopic = (Topic) ctx.lookup("bench.topic");
		benchSubscriber = session.createDurableSubscriber(benchTopic, "admin" + this.id);
		benchSubscriber.setMessageListener(new BenchmarkStartStopListener());
		
		// init the chickens
		benchRabbits = new ChocolateRabbitRabbit[benchmarkChoco];
		benchChickens = new Chicken[benchmarkChicks];
		
		for(int i=0; i<benchmarkChoco; i++)	{
			benchRabbits[i] = new ChocolateRabbitRabbit(new String[]{"" + chocoRabbitID.incrementAndGet(), "" + this.id,  this.prefix, "-1"}); 
		}
		
		for(int i=0; i<benchmarkChicks; i++)	{
			benchChickens[i] = new Chicken(new String[]{"" + chickenID.incrementAndGet(), "" + this.id, this.prefix, "-1"}); 
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

	
	/**
	 * message listener for benchmark mode
	 * @author ja
	 *
	 */
	class BenchmarkMessageListener implements MessageListener	{
		
		@Override
		public void onMessage(Message message) {
			if(message instanceof ObjectMessage)	{
				ObjectMessage om = (ObjectMessage) message;
				try {
					if(om.getObject() == null)	{
						// do nothing
					} else if(om.getObject() instanceof Nest)	{
						
						if(message.propertyExists("build"))	{
							chocoCount--;
							coloredEggCount-=2;
							// TODO PRODUCE TO load balancing rabbit
						}
						
						if(message.propertyExists("error"))	{
							errorNestCount++;
						}
						
						if(message.propertyExists("completed"))	{
							completedNestCount++;
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
					if(message.propertyExists("eggCount"))	{
						eggCount += message.getIntProperty("eggCount");
						// TODO PRODUCE TO load balancing rabbit
					}
					
					// add colored egg (removes not colored egg automatically)
					if(message.propertyExists("eggColorCount"))	{
						coloredEggCount += message.getIntProperty("eggColorCount");
						eggCount -= message.getIntProperty("eggColorCount");
						// TODO PRODUCE TO load balancing rabbit
					}
					
					
					if(message.propertyExists("chocoCount"))	{
						chocoCount += message.getIntProperty("chocoCount");
						// TODO PRODUCE TO load balancing rabbit
					}
					
				} catch (JMSException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else	{
				log.error("RECIEVED AN INVALID MESSAGE");
			}
		}
	}
	
	/**
	 * message listener for benchmark mode
	 * @author ja
	 *
	 */
	class BenchmarkStartStopListener implements MessageListener	{

		@Override
		public void onMessage(Message message) {
			try {
				if(message instanceof TextMessage)	{
					
					// START
					if(message.propertyExists("START"))	{
						// set counter to 0
						completedNestCount = 0;
						errorNestCount = 0;
						for(Chicken ck : benchChickens)	{
							ck.start();
						}
						for(ChocolateRabbitRabbit rb : benchRabbits)	{
							rb.start();
						}
					}
					// STOP
					if(message.propertyExists("STOP"))	{
						int completed = completedNestCount;
						int error = errorNestCount;
						
						for(ChocolateRabbitRabbit rb : benchRabbits)	{
							rb.stopBenchmark();
						}
						for(Chicken ck : benchChickens)	{
							ck.stopBenchmark();
						}
						
						System.out.println("######################################");
						System.out.println("# RESULT: ############################");
						System.out.println("	completed: " + completed);
						System.out.println("	error: " + error);
						System.out.println("	---------------------");
						System.out.println("	SUM: " + (error + completed));
					}
				}
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
}
