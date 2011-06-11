package sbc.producer;

import java.util.Random;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.log4j.Logger;

import sbc.model.ChocolateRabbit;

public class ChocolateRabbitRabbit extends Producer {

	private static Logger log = Logger.getLogger(ChocolateRabbitRabbit.class);

	public static void main(String[] args) {
		ChocolateRabbitRabbit rabbit = new ChocolateRabbitRabbit(args);
		rabbit.start();
	}


	public ChocolateRabbitRabbit(String[] args)	{
		super(args);
	}


	@Override
	public void run() {
		
		log.info("#######################################");
		log.info("###### CholateRabbit started (make " + productCount + " ChocoRabbits)");
		log.info("#######################################");
		
		for(int i=0; i < productCount; i++)	{
			int sleep = new Random().nextInt(3) + 1;
			
			try {
				sleep(sleep * 1000);
				
				ObjectMessage message = session.createObjectMessage();
				
				message.setObject(new ChocolateRabbit(this.id));
				
				log.info("###### ChocoRabbits (" + (i + 1) + ") done");
				
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
		log.info("###### CholateRabbit done");
		log.info("#######################################");
		this.close();
	}
}
