package japaneseAuction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import japaneseAuction.Bidder.State;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class Cartel extends Agent{
	
	AID auctioneer;
	public enum State {
	    WAITING, ACTIVE, EXITING 
	}
	State state = State.WAITING;
	int new_price;
	int colluded_number;
	int receivedBids;
	Hashtable<AID, Integer> colluded_bidders = new Hashtable<AID, Integer>(); //lista di bidder collusi con relative offerte
	private MessageTemplate mt;
	ACLMessage msg;
	
	protected void setup(){
		System.out.println("The cartel " + getAID().getName() + "has started");
		Object[] args = getArguments();
		if (args != null && args.length == 2){
			auctioneer = new AID((String)args[0], AID.ISLOCALNAME);
			colluded_number = Integer.parseInt((String)args[1]);
			receivedBids = 0;
			for(int i = 0; i < colluded_number; i++){
				colluded_bidders.put(new AID("colluded" + Integer.toString(i), AID.ISLOCALNAME), 0);
			}
			addBehaviour(new SendInformBehaviour()); //one shot
			addBehaviour(new CollectBidsBehaviour()); //ciclo, riceve i bid
			//addBehaviour(new BidderBehaviour()); //ciclico, simile a quello dei bidder
		}
		else {
			System.out.println("No number of agents specified.");
			doDelete();
		}
		
	}
	
	protected void takeDown() {
		System.out.println("The cartel " + getAID().getName() + " is terminating.");
	}
	
	//Inform every colluded bidder that the auction has begun
	private class SendInformBehaviour extends OneShotBehaviour{
		public void action(){
			
			ACLMessage start = new ACLMessage(ACLMessage.CFP);
			 
			Enumeration<AID> enumKey = colluded_bidders.keys();
			while(enumKey.hasMoreElements()) {
			    AID key = enumKey.nextElement();
			    start.addReceiver(key);
			}
			start.setContent("START");
			start.setConversationId("auction");
			start.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
			myAgent.send(start);
			System.out.println("The cartel is informing " + Integer.toString(colluded_number) + " colluded bidders that the auction started");
		}		
	}
	
	//Collect the maximum bid for every colluded bidder
	private class CollectBidsBehaviour extends CyclicBehaviour{
		public void action(){
			
			ACLMessage reply = myAgent.receive(mt);
			if (reply != null){
				String cont = reply.getContent();
				colluded_bidders.put(reply.getSender(), Integer.parseInt(cont));
				receivedBids ++;
				System.out.println("The cartel received a bid of " + cont + " from agent " + reply.getSender().getName() + ". Received bids: " + Integer.toString(receivedBids));
			}
			else{
				block();
			}
			if(receivedBids == colluded_number){
				addBehaviour(new BidderBehaviour());
				removeBehaviour(this);
			}
		}		
	}
	
	private class BidderBehaviour extends CyclicBehaviour{
		
		private boolean receiveWin(){
			ArrayList<String> candidates = new ArrayList<String>();
			String winner;
			mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			msg = myAgent.receive(mt);
			if (msg != null){
				String content = msg.getContent();
				if (content.equals("WIN")){
					//find the winner
					Enumeration<AID> enumKey = colluded_bidders.keys();
					while(enumKey.hasMoreElements()) {
					    AID key = enumKey.nextElement();
					    if(colluded_bidders.get(key) == Collections.max(colluded_bidders.values())){
					    	candidates.add(key.getName());
					    	//winner = key.getName();
					    }
					}
					Random generator = new Random();
					int winner_index = generator.nextInt(candidates.size());
					winner = candidates.get(winner_index);
					System.out.println("The cartel " + getAID().getName() + " won the item for " + new_price + " units.");
					inform_colluded_bidders(true, winner, compute_payment());					
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
		
		private float compute_payment(){
			int max_bid = 0;
			int second_highest_bid = 0;
			int value;
			float price_to_pay;
			//determine highest bids
			Enumeration<AID> enumKey = colluded_bidders.keys();
			while(enumKey.hasMoreElements()) {
			    AID key = enumKey.nextElement();
			    value = colluded_bidders.get(key);
			    if(value > max_bid){
			    	second_highest_bid = max_bid;
			    	max_bid = value;
			    }else if(value > second_highest_bid){
			    	second_highest_bid = value;
			    }
			}
			price_to_pay = Math.max(new_price, second_highest_bid);
			
			return price_to_pay;
		}
		
		
		//inform colluded  bidders on the auction outcome
		private void inform_colluded_bidders(boolean outcome, String winner, float price_to_pay){
			float reward = 0;
			//informo tutti che hanno perso				
			if(!outcome){	
				ACLMessage start = new ACLMessage(ACLMessage.INFORM);
				Enumeration<AID> enumKey = colluded_bidders.keys();
				while(enumKey.hasMoreElements()) {
				    AID key = enumKey.nextElement();
				    start.addReceiver(key);
				}
				start.setConversationId("auction");	
				start.setContent("LOST 0");
				myAgent.send(start);
				System.out.println("The cartel informs the colluded bidders that they have lost");
			}
			//informo il vincitore che ha vinto, e gli altri che hanno perso ma ricevono il premio
			else{
				reward = (price_to_pay-new_price)/colluded_number;
				ACLMessage lostMsg = new ACLMessage(ACLMessage.INFORM);
				ACLMessage winMsg = new ACLMessage(ACLMessage.INFORM);
				Enumeration<AID> enumKey = colluded_bidders.keys();
				while(enumKey.hasMoreElements()) {
				    AID key = enumKey.nextElement();
				    if(key.getName()!=winner){
				    	lostMsg.addReceiver(key);
				    }else{
				    	winMsg.addReceiver(key);
				    }
				}
				lostMsg.setConversationId("auction");	
				lostMsg.setContent("LOST " + Float.toString(reward));
				winMsg.setConversationId("auction");	
				winMsg.setContent("WIN " + Float.toString(price_to_pay-reward));
				myAgent.send(lostMsg);
				myAgent.send(winMsg);
			}
			
			
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
						System.out.println("The cartel " + getAID().getName() + " is now in the auction as a normal bidder.");
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
				boolean go_on = false;
				if (msg != null) {
					// CFP Message received. Process it
					new_price = Integer.parseInt(msg.getContent());
					ACLMessage reply = msg.createReply();
					//check if at least one bid is greater than new_price
					Enumeration<AID> enumKey = colluded_bidders.keys();
					while(enumKey.hasMoreElements()) {
					    AID key = enumKey.nextElement();
					    if(colluded_bidders.get(key) >= new_price)
					    	go_on = true;
					}
					if (!go_on) {
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
				
				if(receiveWin()){//perso	
					inform_colluded_bidders(false, "", 0);
				}
				
				doDelete();
				break;
			}
			
		}		
	}

}
