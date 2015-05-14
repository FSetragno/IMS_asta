package japaneseAuction;
//commento
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.core.AID;

import java.util.*;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class Bidder extends Agent{

	AID auctioneer;
	public enum State {
	    WAITING, ACTIVE, EXITING 
	}
	
	State state = State.WAITING;
	int max_price;
	int new_price;
	
	protected void setup() {
		System.out.println("Bidder-agent " + getAID().getName() + " started.");
		Object[] args = getArguments();
		if (args != null && args.length == 2) {
			auctioneer = new AID((String)args[0], AID.ISLOCALNAME);
			String p = (String)args[1];
			max_price = Integer.parseInt(p);
			System.out.println("Bidder-agent " + getAID().getName() + " maximum bid: " + p);
			addBehaviour(new BidderBehaviour());
			
						
		}
		else {
			System.out.println("Error in the arguments.");
			doDelete();
		}
	}
	
	// Put agent clean-up operations here
	protected void takeDown() {
		System.out.println("Bidder-agent " + getAID().getName() + " terminating.");
	}
	
	private class BidderBehaviour extends CyclicBehaviour{
		private static final long serialVersionUID = 1L;
		MessageTemplate mt;
		ACLMessage msg;
		
		private boolean receiveWin(){
			mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			msg = myAgent.receive(mt);
			if (msg != null){
				String content = msg.getContent();
				if (content.equals("WIN")){
					System.out.println("Bidder-agent " + getAID().getName() + " won the item for " + new_price + " units.");
				}
				else{
					System.out.println("PANICO");
				}
				
			}
			else{
				return true;
			}
			return false;
		}
		
		public void action(){
			switch(state){
				case WAITING:
					mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
					msg = myAgent.receive(mt);
					if (msg != null) {
						// CFP Message received. Process it
						String content = msg.getContent();
						if (content.equals("START")){
							System.out.println("Bidder-agent " + getAID().getName() + " is now in the auction.");
							state = State.ACTIVE;
						}
					}
					else{
						block();
					}
					break;
				case ACTIVE:
					mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
					msg = myAgent.receive(mt);
					boolean blockcfp = false;
					
					if (msg != null) {
						// CFP Message received. Process it
						new_price = Integer.parseInt(msg.getContent());
						ACLMessage reply = msg.createReply();

						if (new_price > max_price) {
							reply.setPerformative(ACLMessage.REFUSE);
							reply.setContent("OUT");
							state = State.EXITING;
							//doDelete();
						}
						else {
							reply.setPerformative(ACLMessage.INFORM);
							reply.setContent("OK");
						}
						
						myAgent.send(reply);
					}
					
					else {
						blockcfp = true;
					}
					
					if (blockcfp && receiveWin()){
						block();
					}
					break;
					
				case EXITING:
					receiveWin(); //If two agents are even, the first wins!
					System.out.println("Bidding-agent " + getAID().getName() + " is quitting the auction.");
					doDelete();
					break;
			}
		}
	}
}
