package japaneseAuction;

import java.util.ArrayList;
import java.util.List;

import japaneseAuction.Bidder.State;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.MessageTemplate;

public class Cartel extends Agent{
	
	AID auctioneer;
	public enum State {
	    WAITING, ACTIVE, EXITING 
	}
	State state = State.WAITING;
	int new_price;
	int colluded_number;
	List<AID> colluded_bidders = new ArrayList<AID>();
	private MessageTemplate mt;
	
	protected void setup(){
		System.out.println("The cartel" + getAID().getName() + "has started");
		Object[] args = getArguments();
		if (args != null && args.length == 2){
			auctioneer = new AID((String)args[0], AID.ISLOCALNAME);
			colluded_number = Integer.parseInt((String)args[1]);
			for(int i = 0; i < colluded_number; i++){
				colluded_bidders.add(new AID("colluded" + Integer.toString(i), AID.ISLOCALNAME));
			}
			addBehaviour(new CartelBehaviour); //one shot
			addBehaviour //ciclo, riceve i bid
			addBehaviour //ciclico, simile a quello dei bidder
		}
		else {
			System.out.println("No number of agents specified.");
			doDelete();
		}
		
	}
	
	protected void takeDown() {
		System.out.println("The cartel " + getAID().getName() + " is terminating.");
	}

}
