package sbc.loadbalancing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.jms.Message;

import org.apache.log4j.Logger;

public class LoadBalancingRabbit implements ILoadBalancingCallback {

	private static boolean loadbalanceActive = false;
	
	public static final int timout = 3000;

	private static Logger log = Logger.getLogger(LoadBalancingRabbit.class);
	
	public static final int maxEggFactor = 6;
	public static final int maxEggColoredFactor = 6;
	public static final int maxChocoRabbitFactor = 5;
	
	protected int id;

	private String[] prefixes;

	private HashMap<String, LoadBalancingRabbitListener> listeners = new HashMap<String, LoadBalancingRabbitListener>();
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
			listeners.put(prefix, listener);
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

	/**
	 * uses eggCount and chocoCount to check which factories have to give eggs and which have to consume
	 * and use .takeXX() and .putXX()
	 */
	protected synchronized void balance() {
		// ChocoBunnies
		Integer average = 0;
		for (Integer value : chocoCount.values()) {
			average += value;
		}
		average /= chocoCount.size();
		
		ArrayList<Message> chocoBunnies = new ArrayList<Message>();
		for ( Entry<String, Integer> entry : chocoCount.entrySet()) {
			if (entry.getValue() > average) {
				chocoBunnies.addAll(listeners.get(entry.getKey()).takeChokobunnies(entry.getValue() - average));
			}
		}
		int fromIndex = 0;
		for ( Entry<String, Integer> entry : chocoCount.entrySet()) {
			if (entry.getValue() < average) {
				int toIndex = Math.min(chocoBunnies.size(), fromIndex + (entry.getValue() - average));
				listeners.get(entry.getKey()).putChokobunnies(chocoBunnies.subList(fromIndex, toIndex));
				fromIndex = toIndex;
			}
		}
		
		// Eggs
		average = 0;
		for (Integer value : eggCount.values()) {
			average += value;
		}
		average /= eggCount.size();
		
		ArrayList<Message> eggs = new ArrayList<Message>();
		for ( Entry<String, Integer> entry : eggCount.entrySet()) {
			if (entry.getValue() > average) {
				eggs.addAll(listeners.get(entry.getKey()).takeChokobunnies(entry.getValue() - average));
			}
		}
		fromIndex = 0;
		for ( Entry<String, Integer> entry : chocoCount.entrySet()) {
			if (entry.getValue() < average) {
				int toIndex = Math.min(eggs.size(), fromIndex + (entry.getValue() - average));
				listeners.get(entry.getKey()).putEggs(eggs.subList(fromIndex, toIndex));
				fromIndex = toIndex;
			}
		}
	}
}
