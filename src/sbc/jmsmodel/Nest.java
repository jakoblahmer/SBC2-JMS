package sbc.jmsmodel;

public class Nest extends sbc.model.Nest {

	private static final long serialVersionUID = 1L;

	
	private String id;
	
	public Nest(String id, int producer_id) {
		super(producer_id);
		this.id = id;
	}

	public String getStringId()	{
		return this.id;
	}
	
	public void setStringId(String id)	{
		this.id = id;
	}
	
	@Override
	public String getIdAsString()	{
		return this.id;
	}
	
	@Override
	public String toString()	{
		return "NEST: [id: " + this.id + ", builder_id: " + this.getBuilder_id() + ", logistic_id: " + this.getShipper_id() + 
				",\nEGG1: \n" + this.getEgg1() + ", \nEGG2: \n" + this.getEgg2() + ", \nChocoRabbit: \n" + this.getRabbit() + "]";
	}
	
	@Override
	public String toSimpleString()	{
		return "NEST: [id: " + this.id + "]"; 
	}
}
