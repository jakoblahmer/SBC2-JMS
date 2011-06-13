package sbc.loadbalancing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.naming.InitialContext;

import org.apache.log4j.Logger;

public class LoadBalancingRabbit implements ILoadBalancingCallback {

	private static boolean loadbalanceActive = false;
	
	public static final int timout = 3000;

	private static Logger log = Logger.getLogger(LoadBalancingRabbit.class);
	
	public static final int maxEggFactor = 6;
	public static final int maxEggColoredFactor = 6;
	public static final int maxChocoRabbitFactor = 5;
	
	private int id;

	private String[] prefixes;

	private InitialContext ctx;

	private QueueConnectionFactory connectionFactory;

	private QueueConnection connection;

	private QueueSession session;

	private ArrayList<LoadBalancingRabbitListener> listeners = new ArrayList<LoadBalancingRabbitListener>();

	private HashMap<String, Integer> eggCount = new HashMap<String, Integer>();
	private HashMap<String, Integer> chocoCount = new HashMap<String, Integer>();
	
	public LoadBalancingRabbit(String[] args) {
		parseArguments(args);
		
		initConsumer();
		
		startBalancing();
	}

	public static void main(String[] args)	{
		new LoadBalancingRabbit(args);
	}

	private void parseArguments(String[] args) {
		if (args.length < 3) {
			throw new IllegalArgumentException("Please provide at least a ID and two factory prefixes!");
		}
		
		try	{
			this.id = Integer.parseInt(args[0]);
		} catch (Exception e)	{
			throw new IllegalArgumentException("ID has to be an integer!");
		}
		
		prefixes = Arrays.copyOfRange(args, 1, args.length);
	}

	private void startBalancing() {
		// TODO Auto-generated method stub
		
	}

	private void initConsumer() {
		for (String prefix : prefixes) {
			eggCount.put(prefix, 0);
			chocoCount.put(prefix, 0);
			
			LoadBalancingRabbitListener listener = new LoadBalancingRabbitListener(prefix);
			listener.setProductListener(this);
			listeners.add(listener);
		}
	}

	@Override
	public synchronized void addEggs(String factory, int amount) {
		eggCount.put(factory, eggCount.get(factory)+amount);
		if (loadbalanceActive) balance();
	}

	@Override
	public synchronized void addChocolateRabbits(String factory, int amount) {
		chocoCount.put(factory, chocoCount.get(factory)+amount);
		if (loadbalanceActive) balance();
	}
	
	protected synchronized void balance() {
		
	}
}
