package sbc.loadbalancing;

/**
 * callback interface for loadbalancing rabbit
 * @author ja
 */
public interface ILoadBalancingCallback {
	
	public void addEggs(String factory, int amount);
	public void addChocolateRabbits(String factory, int amount);
}
