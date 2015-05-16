//BABBALA
package japaneseAuction;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.core.AID;

import java.util.*;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class Auctioneer extends Agent{

	private class Withdrawal 
	{ 
		AID agent;
		Long proposal;
		
		public Withdrawal(AID a, Long p){
			agent = a;
			proposal = p;
		}
		
		public AID getAgent(){
			return agent;
		}
		public Long getProposal(){
			return proposal;
		}
	}
	
	List<Withdrawal> withdrawed = new ArrayList<Withdrawal>();
	
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
			withdrawed = new ArrayList<Withdrawal>();
			price += increment;
			ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
			for (int i = 0; i < bidders.size(); ++i) {
				cfp.addReceiver(bidders.get(i));
			} 
			cfp.setContent(Integer.toString(price));
			cfp.setConversationId("bid");
			cfp.setReplyWith(Long.toString(System.currentTimeMillis())); // Unique value
			System.out.println("Auctioneer-agent " + getAID().getName() + " is proposing " + Integer.toString(price) + ".");
			myAgent.send(cfp);
			// Prepare the template to get proposals
			mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bid"),
				 MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
			
		}
		
	}
	
	private class HandleCallsBehaviour extends CyclicBehaviour{

		boolean waitLast = false;

		private void informWinner(AID winner, int payPrice){
			//sposta in funzione que
			
			System.out.println("The winner is " + winner.getName() + ".");
			System.out.println("The price to be paid for the item is " + Integer.toString(payPrice) + ".");
	
			//Send a final INFORM message to the winner
			ACLMessage win = new ACLMessage(ACLMessage.INFORM);
			win.addReceiver(winner); 
			win.setContent("WIN");
			win.setConversationId("win");
			myAgent.send(win);
	
			doDelete();
		}
		
		public void action() {
			ACLMessage reply = myAgent.receive(mt);
			if(!waitLast){
				if (reply != null){
					String cont = reply.getContent();
			
					if (cont.equals("OUT")){
						bidders.remove(reply.getSender());
					
					/*for(Iterator<Withdrawal> i = withdrawed.iterator(); i.hasNext();) {
					       Withdrawal w = i.next();
					       if(w.getProposal() < Long.parseLong(reply.getReplyWith())){
					    	   //Do Something
					    	   i.remove();
					       }
					 }
					
					System.out.println(reply.getReplyWith());*/
						withdrawed.add(new Withdrawal(reply.getSender(), System.currentTimeMillis())); //da migliorare se abbiam tempo
						System.out.println("Auctioneer-agent " + getAID().getName() + " received withdrawal of bidder-agent " + reply.getSender().getName() + ".");
					}
			
					if (bidders.size() == 1){
						System.out.println("End of the auction.");
						System.out.println("Waiting for a possible last out message.");
						//Facciamo che può anche avere vinto uno che si è ritirato prima dell'ultimo di cui si è ricevuto 
						//il messagggio, a parità di cfp in cui si è usciti.
						waitLast = true;
					}
			}	
			
			else{
				block();
			}
	
		}
		else{
			//ero in waitlast
			if(reply != null){
				String cont = reply.getContent();
				
				if (cont.equals("OUT")){
					ArrayList<AID> candidates = new ArrayList<AID>();
					candidates.add(bidders.get(0));
			
					for(int i = 0; i < withdrawed.size(); i++){
						candidates.add(withdrawed.get(i).getAgent());
					}
			
					//Scelgo in modo random chi ha vinto tra gli ultimi a uscire.
					System.out.println("Received an OUT even from the last bidder. Choosing randomly the winner...");
			
					Random generator = new Random();
					int winner = generator.nextInt(candidates.size());
			
					informWinner(candidates.get(winner), price - increment);
					
			}
			else{
				//se mi è arrivato l'ultimo inform
				informWinner(bidders.get(0), price);
				
			}
		}
		else{
			//vuol dire che nessuno mi ha più risposto (uno mi aveva già mandato prima un INFORM, quello è il vincitore
			informWinner(bidders.get(0), price);
			}
		}
	}
		
	}
}
