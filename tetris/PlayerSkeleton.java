package tetris;



public class PlayerSkeleton {
	
	//implement this function to have a working system
	public int pickMove(State s, int[][] legalMoves) {
		
		// Simulate all possible next states possible from s and legalMoves
		
		// Calculate the score for all the states using the heuristics and genes
		
		// return move with the lowest score
		return 0;
	}
	
	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
			s.draw();
			s.drawNext(0,0);
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
	
}
