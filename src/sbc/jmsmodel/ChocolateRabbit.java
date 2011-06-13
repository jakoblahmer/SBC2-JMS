package sbc.jmsmodel;

public class ChocolateRabbit extends sbc.model.ChocolateRabbit {


	private static final long serialVersionUID = 1L;

	private String id;
	
	public ChocolateRabbit(String choco_id, int producer_id) {
		super(producer_id);
		this.id = choco_id;
		this.setId(0);
	}
	
	public String getStringId()	{
		return this.id;
	}
	
	public void setStringId(String id)	{
		this.setId(0);
		this.id = id;
	}
	
	@Override
	public String getIdAsString()	{
		return this.id;
	}
	
	@Override
	public String toString()	{
		return "ChocolateRabbit: [id: " + this.id + ", producer_id: " + producer_id + ", failure: " + this.error + "]";
	}
}
