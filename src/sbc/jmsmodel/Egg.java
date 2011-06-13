package sbc.jmsmodel;

public class Egg extends sbc.model.Egg {

	private static final long serialVersionUID = 1L;

	
	private String id;
	
	public Egg(String egg_id, int producer_id, int colorCount)	{
		super(producer_id, colorCount);
		this.id = egg_id;
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
		return "EGG: [id: " + this.id + ", producer_id: " + producer_id + ", colorCount: " + this.getColorCount() + ", colors: " + this.colorToString() + ", failure: " + this.error + "]";
	}
	
}
