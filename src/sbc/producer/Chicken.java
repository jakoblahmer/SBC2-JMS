package sbc.producer;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.mozartspaces.capi3.AnyCoordinator;
import org.mozartspaces.core.Entry;
import org.mozartspaces.core.MzsCoreException;

import sbc.jmsmodel.Egg;

public class Chicken extends Producer {

	private static Logger log = Logger.getLogger(Chicken.class);

	public static void main(String[] args) {
		Chicken chicken = new Chicken(args);
		chicken.start();
	}

	private AtomicInteger eggID = new AtomicInteger(0);
	private boolean stop;
	
	public Chicken(String[] args)	{
		super(args);
		
		this.init("color.queue");
		// DEBUG
//		this.init("build.queue");
		
		this.initGUIProducer();
	}


	@Override
	public void run() {
		
		if(productCount == -1)	{
			
			int counter = 0;
			
			Egg egg;
			ObjectMessage message;
			
			while(!stop)	{
				egg = new Egg(String.valueOf(this.adminid) + "_" + String.valueOf(this.id) + "_" + String.valueOf(eggID.incrementAndGet())
						, this.id
						, getRandomColorCount());
			
				egg.setError(this.calculateDefect());
				
				try	{
				
					message = session.createObjectMessage();
					
					message.setObject(egg);
					message.setStringProperty("NOCOLOR", "1");
					producer.send(message);
	
					// write to gui
					guiMsg = session.createTextMessage();
					guiMsg.setIntProperty("eggCount", 1);
					guiProducer.send(guiMsg);
				} catch(Exception e)	{
					this.stop = true;
				}
				counter++;
				
				if(counter > 300)	{
					log.info("SLEEP");
					try {
						sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					counter = 0;
				}
				
			}
			this.close();
			return;
		}
		
		log.info("#######################################");
		log.info("###### chicken started (lay " + productCount + " eggs)");
		log.info("#######################################");
		
		Egg egg;
		
		for(int i=0; i < productCount; i++)	{
			//int sleep = new Random().nextInt(3) + 1;
			
			try {
				//sleep(sleep * 1000); // no sleep anymore
				
				
				ObjectMessage message = session.createObjectMessage();
				
				log.info("###### EGG (" + (i + 1) + ") done");
				
				egg = new Egg(String.valueOf(this.adminid) + "_" + String.valueOf(this.id) + "_" + String.valueOf(eggID.incrementAndGet())
							, this.id
							, getRandomColorCount());
				
				egg.setError(this.calculateDefect());
				
				// DEBUG:
//				egg.setColored(true);
//				message.setStringProperty("product", "egg");
				
				message.setObject(egg);
				message.setStringProperty("NOCOLOR", "1");
				producer.send(message);

				// write to gui
				guiMsg = session.createTextMessage();
				guiMsg.setIntProperty("eggCount", 1);
				guiProducer.send(guiMsg);
				
				
				log.info("#######################################");
				
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		log.info("#######################################");
		log.info("###### chicken done");
		log.info("#######################################");
		
		this.close();
	}
	
	private int getRandomColorCount()	{
		return new Random().nextInt(3) + 2;
	}

}
