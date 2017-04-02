package tetrisTraining;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/***********************************
 * This class runs the genetic algorithm to produce the best genes for the game
 * 
 * **********************************/

public class Genepool {
	
	private final int NUM_HEURISTICS = 5;
	private final double FRAC_IN_TOURNAMENT = 0.1;
	private final double OFFSPRING_POOL_LIMIT = 0.3;
	int numGames = 20;
	
	private int geneNumber;
	private List<Gene> genepool;
	private List<Gene> offspringPool;
	
	// Initializes a genepool with a number of randomly generated genes
	public Genepool(int numGenes) {
		geneNumber = numGenes;
		genepool = new ArrayList<Gene>();
		offspringPool = new ArrayList<Gene>();
		generateGenes();
	}
	
	public int size() {
		return geneNumber;
	}
	
	public Gene get(int index) {
		return genepool.get(index);
	}
	
	public void sortPool() {
		// Sort the genepool
		Collections.sort(genepool, new Comparator<Gene>() {
			@Override
			public int compare(Gene o1, Gene o2) {
				return o1.compareTo(o2);
			}
		});
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
			genepool.add(newGene);
		}
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
		//System.out.println("Fittest: " + tournament.peek().toWrite());
		// Create a new gene from the 2 fittest genes
		Gene newGene = tournament.poll().waCrossover(tournament.poll());
		
		// Calculate fitness for the new gene
		PlayerSkeleton.runGames(numGames, newGene);
		offspringPool.add(newGene);
	}
	
	// Runs tournament until the offspring count reaches the specified fraction 
	// of the total genepool, then eliminate the least fit genes from the pool
	// before adding the offsprings into the pool. Maintain the same total genes
	public void runGeneticAlgo() {
		int offspringLimit = (int)(OFFSPRING_POOL_LIMIT * geneNumber);
		int factor = (int) Math.pow(10, String.valueOf(offspringLimit).length() - 1);
		while(offspringPool.size() < offspringLimit) {
			runTournament();
			if(offspringPool.size() % factor == 0) {
				System.out.println("Finished " + offspringPool.size() + " tournaments");
			}
		}
		sortPool();
		ArrayList<Gene> newGenepool = new ArrayList<Gene>(genepool.subList(0, geneNumber - offspringLimit));
		newGenepool.addAll(offspringPool);
		genepool = newGenepool;
		offspringPool = new ArrayList<Gene>();
	}
	
	public void toFile(String filename) {
		//Sort first before writing to file
		sortPool();
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
			for(int i = 0; i < genepool.size(); i++) {
				bw.write(genepool.get(i).toWrite());
				bw.newLine();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void fromFile(String filename) {
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			String line;
			while((line = br.readLine()) != null) {
				String[] stringArray = line.split(", ");
				double[] vector = new double[stringArray.length];
				for(int i = 0; i < stringArray.length; i++) {
					vector[i] = Double.parseDouble(stringArray[i]);
				}
				Gene newGene = new Gene(vector, false);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}








