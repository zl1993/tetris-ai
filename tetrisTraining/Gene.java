package tetrisTraining;

/***********************************
 * This class would contain the parametric weight vector for the heuristics 
 * 
 * **********************************/


public class Gene implements Comparable<Gene>{
	
	private final int NUM_HEURISTICS = 5;
	private double mutChance = 0.05;
	private double mutMax = 0.2;
	private double[] paramVector;
	private double fitness = 1;
	
	// Default Constructor - all equal weights, normalized
	public Gene() {
		paramVector = new double[NUM_HEURISTICS];
		for(int i = 0; i < NUM_HEURISTICS; i++) {
			double value = Math.sqrt(1.0/NUM_HEURISTICS);
			paramVector[i] = 0.5;
		}
	}
	
	// Specific Constructor, if need be, normalize the weights before creating the gene
	public Gene(double[] weights, boolean toNormalize) {
		if(toNormalize) {
			double sumSquared = 0;
			for(int i = 0; i < weights.length; i++){
				sumSquared += weights[i] * weights[i];
			}
			double factor = Math.sqrt(sumSquared);
			for(int i = 0; i < weights.length; i++){
				weights[i] = ((double) weights[i]) / factor; 
			}
		} 
		paramVector = weights;
	}
	
	public double[] getVector() {
		return paramVector;
	}
	
	// Set the fitness score after a test
	public void setFitness(double fit) {
		fitness = fit;
	}
	
	// 
	public void mutate() {
		if (Math.random() < mutChance){
			int randIndex = (int)(Math.floor(Math.random() * NUM_HEURISTICS));
			double randMutation = (Math.random() - 0.5) * 2 * mutMax;
			paramVector[randIndex] += randMutation;
		}
	}
	
	// Weighted average crossover to produce offspring genes from parent genes
	// using the fitness-weighted average of their param vectors
	public Gene waCrossover(Gene other) {
		double[] newVector = new double[NUM_HEURISTICS];
		for(int i=0; i<NUM_HEURISTICS; i++){
			newVector[i] = this.fitness * this.paramVector[i] + other.fitness * other.paramVector[i];
		}
		
		Gene newGene = new Gene(newVector, true);
		if(this.fitness == 0 && other.fitness == 0){
			newGene = new Gene();
		}
		mutate();
		return newGene;
	}
	
	public int compareTo(Gene other) {
		return Double.compare(other.fitness, this.fitness);
	}
	
	public String toWrite() {
		String res = "";
		for(int i = 0; i < paramVector.length; i++){
			if(i == 0){
				res = res + paramVector[i];
			} else {
				res = res + ", " + paramVector[i]; 
			}
		}
		return res + ", fitness: " + fitness;
	}
	
}
