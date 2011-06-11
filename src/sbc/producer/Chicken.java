package sbc.producer;

import java.util.Random;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.log4j.Logger;

import sbc.model.Egg;

public class Chicken extends Producer {

	private static Logger log = Logger.getLogger(Chicken.class);

	public static void main(String[] args) {
		Chicken chicken = new Chicken(args);
		chicken.start();
	}


	public Chicken(String[] args)	{
		super(args);
	}


	@Override
	public void run() {
		
		log.info("#######################################");
		log.info("###### chicken started (lay " + productCount + " eggs)");
		log.info("#######################################");
		
		for(int i=0; i < productCount; i++)	{
			int sleep = new Random().nextInt(3) + 1;
			
			try {
				sleep(sleep * 1000);
				
				
				ObjectMessage message = session.createObjectMessage();
				
				log.info("###### EGG (" + (i + 1) + ") done");
				
				message.setObject(new Egg(this.id));
				
				adminProducer.send(message);
				log.info("#######################################");
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
}
