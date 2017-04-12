package tetrisTraining;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;

public class ResultsReader {
	static String filename = "Results.txt";
	
	public static void main (String[] args){
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			ArrayList<Double> allRes = new ArrayList<Double>();
			String line;
			while((line = br.readLine()) != null) {
				String[] arr = line.split(" ");
				String res = arr[3]; 
				allRes.add(Double.parseDouble(res));
			}
			Collections.sort(allRes);
			int medIndex = allRes.size()/2 + 1;
			System.out.println("Median: " + allRes.get(medIndex));
			System.out.println("Max: " + allRes.get(allRes.size() - 1));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
