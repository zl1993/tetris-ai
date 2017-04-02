package tetris;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

/***********************************
 * This class runs the genetic algorithm to produce the best genes for the game
 * 
 * **********************************/

public class Genepool {
	
	private final int NUM_HEURISTICS = 4;
	private final double FRAC_IN_TOURNAMENT = 0.1;
	private final int GAMES_PER_TOURNAMENT = 100;
	private final double OFFSPRING_POOL_LIMIT = 0.3;
	
	private int geneNumber;
	private ArrayList<Gene> genepool;
	private ArrayList<Gene> offspringPool;
	
	// Initializes a genepool with a number of randomly generated genes
	public Genepool(int numGenes) {
		geneNumber = numGenes;
		genepool = new ArrayList<Gene>();
		
		generateGenes();
	}
	
	// Generate random normalized genes, and get each gene's fitness score
	public void generateGenes() {
		for(int i = 0; i < geneNumber; i++){
			double[] vector = new double[NUM_HEURISTICS];
			for(int j = 0; j < NUM_HEURISTICS; j++){
				// To get a number in [-0.5, 0.5)
				vector[j] = Math.random() - 0.5;
			}
			Gene newGene = new Gene(vector, true);
			genepool.add(runGames(newGene));
		}
	}
	
	// Run the algorithm to get the fittest genes 
	public void start() {
		
	}
	
	// Selects a fraction of the genepool, then take
	// the 2 with the best fitness score and get their offspring gene
	public void runTournament() {
		PriorityQueue<Gene> tournament = new PriorityQueue<Gene>();
		int numContenders = (int)(FRAC_IN_TOURNAMENT * geneNumber);
		Set<Integer> chosenOnes = new HashSet<Integer>();
		
		while(chosenOnes.size() < numContenders) {
			int randIndex = (int) (Math.random() * geneNumber);
			chosenOnes.add(randIndex);
		}
		for(int i : chosenOnes){
			tournament.add(genepool.get(i));
		}
		// Create a new gene from the 2 fittest genes
		Gene newGene = tournament.poll().waCrossover(tournament.poll());
		offspringPool.add(newGene);
	}
	
	// Runs many tetris games with a gene, and attach to it a fitness score
	private Gene runGames(Gene gene){
		int sumFitness = 0;
		for(int i = 0; i < GAMES_PER_TOURNAMENT; i++){
			sumFitness += 0;
		}
		double fitnessScore = ((double) sumFitness)/GAMES_PER_TOURNAMENT;
		gene.setFitness(fitnessScore);
		return gene;
	}
	
}








