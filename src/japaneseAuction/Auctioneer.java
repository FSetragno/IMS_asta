//BABBALA
package japaneseAuction;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.core.AID;

import java.util.*;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class Auctioneer extends Agent{

	List<AID> bidders = new ArrayList<AID>();
	
	public enum State {
		START, ACTIVE
	}
	
	State state = State.START;
	
	int price = 10;
	int increment = 1;
	
	private MessageTemplate mt;
	
	protected void setup() {
		System.out.println("Auctioneer-agent " + getAID().getName() + " started.");
		Object[] args = getArguments();
		if (args != null && args.length == 3) {
			int biddersNumber = Integer.parseInt((String)args[0]);
			for(int i = 0; i < biddersNumber; i++){
				bidders.add(new AID("bidder" + Integer.toString(i), AID.ISLOCALNAME));
			}
			
			String p = (String)args[1];
			
			String inc = (String)args[2];
			
			increment = Integer.parseInt(inc);
			price = Integer.parseInt(p) - increment;
			
			System.out.println("Auctioneer-agent " + getAID().getName() + " starts from price = " + p + " units and will increment by " + inc + ".");
			//Send immediately an inform message with the start of the auction
			System.out.println("Press any key to start the auction.");
			Scanner input = new Scanner(System.in);
			String s = input.nextLine();
			startAuction();
			
			addBehaviour(new PublishPriceBehaviour(this, 5000));

			addBehaviour(new HandleCallsBehaviour());
			
		}
		else {
			System.out.println("No number of agents specified.");
			doDelete();
		}
	}
	
	// Put agent clean-up operations here
	protected void takeDown() {
		System.out.println("Auctioneer-agent " + getAID().getName() + " terminating.");
	}
	
	/**
	This is invoked at the auctioneer setup
	*/
	private void startAuction() {
		addBehaviour(new OneShotBehaviour() {
			private static final long serialVersionUID = 1L;

			public void action() {
				ACLMessage start = new ACLMessage(ACLMessage.INFORM);
				for (int i = 0; i < bidders.size(); ++i) {
					start.addReceiver(bidders.get(i));
				} 
				start.setContent("START");
				start.setConversationId("auction");
				myAgent.send(start);
			}
		} );
	}
	
	private class PublishPriceBehaviour extends TickerBehaviour {
		
		private static final long serialVersionUID = 1L;

		public PublishPriceBehaviour(Agent a, long period) {
			super(a, period);
		}

		@Override
		protected void onTick() {
			price += increment;
			ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
			for (int i = 0; i < bidders.size(); ++i) {
				cfp.addReceiver(bidders.get(i));
			} 
			cfp.setContent(Integer.toString(price));
			cfp.setConversationId("bid");
			cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
			System.out.println("Auctioneer-agent " + getAID().getName() + " is proposing " + Integer.toString(price) + ".");
			myAgent.send(cfp);
			// Prepare the template to get proposals
			mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bid"),
				 MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
			
		}
		
	}
	
	private class HandleCallsBehaviour extends CyclicBehaviour{

		@Override
		public void action() {
			ACLMessage reply = myAgent.receive(mt);
			if (reply != null){
				String cont = reply.getContent();
			
				if (cont.equals("OUT")){
					bidders.remove(reply.getSender());
					System.out.println("Auctioneer-agent " + getAID().getName() + " received withdrawal of bidder-agent " + reply.getSender().getName() + ".");
				}
			
				if (bidders.size() == 1){
					System.out.println("End of the auction.");
					System.out.println("The winner is " + bidders.get(0).getName() + ".");
					System.out.println("The price to be paid for the item is " + Integer.toString(price) + ".");
					
					//Send a final INFORM message to the winner
					ACLMessage win = new ACLMessage(ACLMessage.INFORM);
					win.addReceiver(bidders.get(0)); 
					win.setContent("WIN");
					win.setConversationId("win");
					myAgent.send(win);
					
					doDelete();
				}
			
			}
			else{
				block();
			}
		}
		
	}
}
