package sbc.producer;

import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.log4j.Logger;

import sbc.jmsmodel.ChocolateRabbit;

public class ChocolateRabbitRabbit extends Producer {

	private static Logger log = Logger.getLogger(ChocolateRabbitRabbit.class);

	public static void main(String[] args) {
		ChocolateRabbitRabbit rabbit = new ChocolateRabbitRabbit(args);
		rabbit.start();
	}


	private AtomicInteger chocoID = new AtomicInteger(0);
	
	public ChocolateRabbitRabbit(String[] args)	{
		super(args);
		this.init("build.queue");
		this.initGUIProducer();
	}


	@Override
	public void run() {
		
		if(productCount == -1)	{
			
//			this.startLoopTimeout();
			
			int counter = 0;
			
			ChocolateRabbit cr;
			ObjectMessage message;
			
			while(!stop)	{
				
				cr = new ChocolateRabbit(String.valueOf(this.adminid) + "_" + String.valueOf(this.id) + "_" + String.valueOf(chocoID.incrementAndGet()), this.id);
				cr.setError(this.calculateDefect());

				try {
					message = session.createObjectMessage();
				
					message.setObject(cr);
					message.setStringProperty("product", "chocolateRabbit");
					
					// write to gui
					guiMsg = session.createTextMessage();
					guiMsg.setIntProperty("chocoCount", 1);
					guiProducer.send(guiMsg);
				
				} catch (JMSException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					this.stop = true;
				}
				
				counter++;
				
				if(counter > 300)	{
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
		log.info("###### CholateRabbit started (make " + productCount + " ChocoRabbits)");
		log.info("#######################################");
		
		ChocolateRabbit cr;
		
		for(int i=0; i < productCount; i++)	{
//			int sleep = new Random().nextInt(3) + 1;
//			sleep(sleep * 1000);
			
			try {
				
				ObjectMessage message = session.createObjectMessage();
				
				cr = new ChocolateRabbit(String.valueOf(this.adminid) + "_" + String.valueOf(this.id) + "_" + String.valueOf(chocoID.incrementAndGet()), this.id);
				cr.setError(this.calculateDefect());

				message.setObject(cr);
				message.setStringProperty("product", "chocolateRabbit");
				
				log.info("###### ChocoRabbits (" + (i + 1) + ") done");
				
				// write to gui
				guiMsg = session.createTextMessage();
				guiMsg.setIntProperty("chocoCount", 1);
				guiProducer.send(guiMsg);
				
				producer.send(message);
				log.info("#######################################");
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
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
