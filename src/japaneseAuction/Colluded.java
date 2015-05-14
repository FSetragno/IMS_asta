package japaneseAuction;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.core.AID;
import japaneseAuction.Bidder.State;

import java.util.*;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class Colluded extends Agent{
	
	AID cartel;
	public enum State {
	    WAITING, ACTIVE 
	}
	
	State state = State.WAITING;
	int max_price;
	
	protected void setup() {
		System.out.println("Bidder-agent " + getAID().getName() + " started.");
		Object[] args = getArguments();
		if (args != null && args.length == 2) {
			cartel = new AID((String)args[0], AID.ISLOCALNAME);
			String p = (String)args[1];
			max_price = Integer.parseInt(p);
			System.out.println("Bidder-agent " + getAID().getName() + " maximum bid: " + p);
			addBehaviour(new ColludedBehaviour());
			
						
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
	
	private class ColludedBehaviour extends CyclicBehaviour{
		private static final long serialVersionUID = 1L;
		MessageTemplate mt;
		ACLMessage msg;
		
		@Override
		public void action() {
			switch(state){
				case WAITING:
					mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
					msg = myAgent.receive(mt);
					if (msg != null) {
						// CFP Message received. Respond with the highest bid
						ACLMessage reply = msg.createReply();
						reply.setPerformative(ACLMessage.INFORM);
						reply.setContent(Integer.toString(max_price));
						state = State.ACTIVE;
					}
					else{
						block();
					}
					break;
				
				case ACTIVE:
					mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
					msg = myAgent.receive(mt);
					if (msg != null) {
						//Inform Message received. Process it
						String content = msg.getContent();
						String[] splitStr = content.split("\\s+");
						if (splitStr[0].equals("WIN")){
							System.out.println("Bidder-agent " + getAID().getName() + " won the item and pays " + splitStr[1] + " to the cartel.");
						}
						else if (splitStr[0].equals("LOST")){
							System.out.println("Bidder-agent " + getAID().getName() + " lost the item, but receives " + splitStr[1] + " from the cartel.");
						}
						else{
							System.out.println("PANICO");
						}
						doDelete();
					}
					else{
						block();
					}
					break;
			}
		}
	
	}
	
}