/*
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
*/
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayerSkeleton {

    private ArrayList<Move> movesList = new ArrayList<Move>();
    
    //global variable to keep track of score across all games (if running concurrently)
    static int totalScore = 0;
    static boolean RUN1000GAMES = false;
    
    public static void main(String[] args) {
        // run 100 games concurrently
        if (RUN1000GAMES == true){
            int GAMENUM = 1000;
            ExecutorService executor = Executors.newFixedThreadPool(GAMENUM);
            for(int i = 0; i < GAMENUM; i++){
                Runnable gameRunnable = new GameRunnable(i);
                executor.execute(gameRunnable);
            }
            executor.shutdown();
            // Wait until all threads are finish
            while (!executor.isTerminated()) {
            }
            System.out.println("Total Lines Cleared over " + GAMENUM + "games: " + totalScore);
            System.out.println("Average Lines Cleared over " + GAMENUM + "games: " + totalScore/GAMENUM); 
        }
        // run 1 game
        else{
            State s = new State();
            new TFrame(s);
            PlayerSkeleton p = new PlayerSkeleton();
    
            // while it game is not lost, continue making next move
            while (!s.hasLost()) {
                s.makeMove(p.chooseMove(s, s.getNextPiece(), s.legalMoves()));
                s.draw();
                s.drawNext(0, 0);
                try {
                    Thread.sleep(0);//change back to 300
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("You have completed " + s.getRowsCleared() + " rows.");
        }
    }
    
    //for multithreading
    public static class GameRunnable implements Runnable {
        private final int gameNum;
        
        GameRunnable(int gameNum) {
            this.gameNum = gameNum;
        }
 
        @Override
        public void run() {
            State s = new State();
        //  new TFrame(s);
            PlayerSkeleton p = new PlayerSkeleton();

            // while it game is not lost, continue making next move
            while (!s.hasLost()) {
                s.makeMove(p.chooseMove(s, s.getNextPiece(), s.legalMoves()));
//              s.draw();
//              s.drawNext(0, 0);
              try {
                  Thread.sleep(0);//change back to 300
              } catch (InterruptedException e) {
                  e.printStackTrace();
              }
          }
          System.out.println("Game " + gameNum + " completed " + s.getRowsCleared() + " rows.");
          totalScore = totalScore + s.getRowsCleared();
        }
    }
    

    // AI picks the next move
    public int chooseMove(State s, int nextPiece, int[][] legalMoves) {
        // clear the current movesList, before adding new legal moves for next
        // move
        movesList.clear();

        // create a new StateView object from State object for evaluating
        // possible next states
        StateView currState = new StateView(s);

        // run through legal moves and add all possible moves to movesList
        for (int i = 0; i < legalMoves.length; i++) {
            int orient = legalMoves[i][0];
            int slot = legalMoves[i][1];
            movesList.add(new Move(currState, i, s.getNextPiece(), orient, slot));
        }

        // choose the best next move based on evaluation function
        int chosenMoveIndex = evalMoves(movesList);
        return chosenMoveIndex;
    }

    // evaluates the best next move and returns that move
    public int evalMoves(ArrayList<Move> movesList) {
        double maxScore = -Double.MAX_VALUE;
        double currScore = -Double.MAX_VALUE;
        int chosenMoveIndex = 0;

        // for every possible move in the movesList, evaluate its score
        for (int i = 0; i < movesList.size(); i++) {
            currScore = evalScore(movesList.get(i));
            // choose the highest score, and update it as our chosen move
            // for equal scores, just take the first that comes along
            if (currScore > maxScore) {
                maxScore = currScore;
                chosenMoveIndex = movesList.get(i).getIndex();
            }
        }
        return chosenMoveIndex;
    }

    // calculate move score based on overall heuristic, weighted between 4
    // heuristic components
    public double evalScore(Move move) {
        
        // WEIGHTS
        double rowsClearedWeight = 0.900283;
        double numHolesWeight = 6.302734;
        double numVerticalConnectedHolesWeight = 0.200001;
        //double totalHeightWeight = 0.510066;
        double heightVarWeight = 0.900192;
        double maxHeightWeight = 0.800240; 
        double totalWellDepthWeight = 1.301027;
        double maxWellDepthWeight = 0.100021;
        //double maxHeightDiffWeight = 0.192596;
        double avgHeightDiffFromMeanWeight = 0.871003;

        // find next state after applying move
        NextState nextState = move.getStateView().makeMoveView(move.getOrient(), move.getSlot(), move.getPiece());

        // only calculate score if next move does not result in a loss
        // if loss, just assign it lowest possible score
        if (nextState.hasLost() == true){
            return -Double.MAX_VALUE;
        }
        else{
            return  rowsClearedWeight * evalRowsCleared(nextState) 
                    - numHolesWeight * evalNumHoles(nextState)
                    - numVerticalConnectedHolesWeight * evalNumVerticalConnectedHoles(nextState)
                    //- totalHeightWeight * evaltotalHeight(nextState) 
                    - heightVarWeight * evalHeightVar(nextState)
                    - maxHeightWeight * evalMaxHeight(nextState) 
                    - totalWellDepthWeight * evalTotalWellDepth(nextState) 
                    - maxWellDepthWeight * evalMaxWellDepth(nextState) 
                    //- maxHeightDiffWeight * evalMaxHeightDifference(nextState)
                    - avgHeightDiffFromMeanWeight * evalAverageHeightDifferenceFromMean(nextState);
        }
    }

    // MAXIMIZE - because it is the game's objective
    // no. of rows cleared heuristic component
    public int evalRowsCleared(NextState nextState) {
        return nextState.getRowsCleared();
    }

    // MINIMIZE - because holes cannot be filled up
    // no. of holes heuristic component - hole is an unused square with a used
    // square above it
    public int evalNumHoles(NextState nextState) {
        // get the field and top arrays of the state after next move is applied
        int[][] field = nextState.getStateView().getField();
        int[] top = nextState.getStateView().getTop();
        int numOfHoles = 0;

        // for each column, count the number of unused spaces below the top used
        // space
        for (int i = 0; i < State.COLS; i++) {
            for (int j = top[i] - 1; j >= 0; j--) { // only check for spaces
                                                    // BELOW the top used space
                if (field[j][i] == 0) { // if empty/unused
                    numOfHoles++;
                }
            }
        }
        return numOfHoles;
    }
    
    // MINIMIZE - because holes cannot be filled up
    // no. of vertically connected holes heuristic component
    // vertically connected hole is a series of unused squares along a column, with at least one used square above it
    public int evalNumVerticalConnectedHoles(NextState nextState) {
        // get the field and top arrays of the state after next move is applied
        int[][] field = nextState.getStateView().getField();
        int[] top = nextState.getStateView().getTop();
        int numOfVerticalConnectedHoles = 0;

        // for each column, count the number of connected holes below the top used
        // space
        for (int i = 0; i < State.COLS; i++) {
            for (int j = top[i] - 1; j >= 0; j--) { // only check for spaces
                                                    // BELOW the top used space
                if (field[j][i] == 0 && field[j+1][i] != 0) { // only count if above is filled
                    numOfVerticalConnectedHoles++;
                }
            }
        }
        return numOfVerticalConnectedHoles;
    }

    // MINIMIZE - so there is more space to drop new pieces
    // total height heuristic component - total 'unavailable' space
    public int evalTotalHeight(NextState nextState) {
        // get the top array of the state after next move is applied
        int[] top = nextState.getStateView().getTop();
        int totalHeight = 0;

        // find the sum of the heights of each column of the state after next
        // move applied
        for (int i = 0; i < State.COLS; i++) {
            totalHeight = totalHeight + top[i];
        }
        return totalHeight;
    }

    // MINIMIZE - to avoid deep wells
    // height variance heuristic component - the sum of the ABSOLUTE differences
    // between every adjacent column
    public int evalHeightVar(NextState nextState) {
        // get the top array of the state after next move is applied
        int[] top = nextState.getStateView().getTop();
        int sumVar = 0;

        // find the sum of the absolute differences in heights of every adjacent
        // column of the state after next move applied
        for (int i = 1; i < State.COLS; i++) {
            sumVar = sumVar + Math.abs(top[i] - top[i - 1]);
        }
        return sumVar;
    }

    // MINIMIZE - to keep max height level low, further
    // maximum height heuristic component
    public int evalMaxHeight(NextState nextState) {
        // get the top array of the state after next move is applied
        int[] top = nextState.getStateView().getTop();
        int maxHeight = 0;

        // find the max height of all the column heights
        for (int i = 0; i < State.COLS; i++) {
            if (top[i] > maxHeight) {
                maxHeight = top[i];
            }
        }
        return maxHeight;
    }
    
    // MINIMIZE - to keep total well depth low
    // total well depth heuristic component
    // well is a column with both its adjacent columns higher than it by at least 2 blocks
    // well depth is the height from that column to the height of the shorter higher adjacent column
    public int evalTotalWellDepth(NextState nextState){
        
        int[] top = nextState.getStateView().getTop();
        int totalWellDepth = 0;

        // check if 1st column is a well (does not have left adjacent column)
        // check 2nd column - 1st column if at least 2
        if (top[1] - top[0] >= 2) {
            totalWellDepth = totalWellDepth + (top[1] - top[0]);
        }
        
        // check if 10th column is a well (does not have right adjacent column)
        // check 9th column - 10th column if at least 2
        if (top[8] - top[9] >= 2) {
            totalWellDepth = totalWellDepth + (top[8] - top[9]);
        }

        // check if 2nd to 9th columns are wells (have both left and right adjacent columns)
        for (int i = 1; i < State.COLS - 1; i++) {
            // take the minimum height diff between left and right adjacent columns
            // and check if minimum height diff is at least 2 (to be considered well)
            if (Math.min(top[i-1] - top[i], top[i+1] - top[i]) >= 2){
                totalWellDepth = totalWellDepth + Math.min(top[i-1] - top[i], top[i+1] - top[i]);
            }
        }

        return totalWellDepth;
    }
    
    // MINIMIZE - to keep maximum well depth low
    // maximum well depth heuristic component
    // well is a column with both its adjacent columns higher than it by at least 2 blocks
    // well depth is the height from that column to the height of the shorter higher adjacent column
    public int evalMaxWellDepth(NextState nextState){
        int[] top = nextState.getStateView().getTop();
        int maxWellDepth = 0;

        // check if 1st column is a well (does not have left adjacent column)
        // check 2nd column - 1st column if at least 2
        if (top[1] - top[0] >= 2 && top[1] - top[0] > maxWellDepth) {
            maxWellDepth = top[1] - top[0];
        }
        
        // check if 10th column is a well (does not have right adjacent column)
        // check 9th column - 10th column if at least 2
        if (top[8] - top[9] >= 2 && top[8] - top[9] > maxWellDepth) {
            maxWellDepth = top[8] - top[9];
        }

        // check if 2nd to 9th columns are wells (have both left and right adjacent columns)
        for (int i = 1; i < State.COLS - 1; i++) {
            // take the minimum height diff between left and right adjacent columns
            // and check if minimum height diff is at least 2 (to be considered well)
            if (Math.min(top[i-1] - top[i], top[i+1] - top[i]) >= 2 && Math.min(top[i-1] - top[i], top[i+1] - top[i]) > maxWellDepth){
                maxWellDepth = Math.min(top[i-1] - top[i], top[i+1] - top[i]);
            }
        }
        return maxWellDepth;
    }
    
    // MINIMIZE - to keep height difference between max and min height low
    // maximum height difference heuristic component
    public int evalMaxHeightDifference(NextState nextState) {
        // get the top array of the state after next move is applied
        int[] top = nextState.getStateView().getTop();
        int maxHeight = 0;
        int minHeight = State.ROWS + 1;

        // find the max and min heights among all columns
        for (int i = 0; i < State.COLS; i++) {
            if (top[i] > maxHeight) {
                maxHeight = top[i];
            }
            if (top[i] < minHeight){
                minHeight = top[i];
            }
        }
        return maxHeight - minHeight;
    }
    
    // MINIMIZE - to keep average height difference from mean low
    // average height difference from mean heuristic  component
    public double evalAverageHeightDifferenceFromMean(NextState nextState) {
        // get the top array of the state after next move is applied
        int[] top = nextState.getStateView().getTop();
        int totalHeight = 0;
        double meanHeight = 0; 
        double totalHeightDiffFromMean = 0;
        double averageHeightDiffFromMean = 0;
        
        // find the sum of the heights of each column of the state after next
        // move applied
        for (int i = 0; i < State.COLS; i++) {
            totalHeight = totalHeight + top[i];
        }
        
        meanHeight = (double) totalHeight / (double) State.COLS;
        for (int i = 0; i < State.COLS; i++) {
            totalHeightDiffFromMean =  Math.abs(top[i] - meanHeight);
        }
        averageHeightDiffFromMean = totalHeightDiffFromMean / (double) State.COLS;
        
        return averageHeightDiffFromMean;
    }

    // class for each possible tetris move
    private static class Move {

        private StateView stateView;
        private int index;
        private int piece;
        private int orient;
        private int slot;

        // constructor
        public Move(StateView stateView, int index, int piece, int orient, int slot) {
            this.stateView = stateView;
            this.index = index;
            this.piece = piece;
            this.orient = orient;
            this.slot = slot;
        }

        // getters
        public StateView getStateView() {
            return stateView;
        }

        public int getIndex() {
            return index;
        }

        public int getPiece() {
            return piece;
        }

        public int getOrient() {
            return orient;
        }

        public int getSlot() {
            return slot;
        }

    }

    // class for evaluating next state after a possible move
    // 'view' state after a move is made, without actually making that move
    private static class StateView {

        public static final int COLS = 10;
        public static final int ROWS = 21;
        public static final int N_PIECES = 7;

        // each square in the grid - int means empty - other values mean the
        // turn it was placed
        private int[][] field = new int[ROWS][COLS];
        // top row+1 of each column
        // 0 means empty
        private int[] top = new int[COLS];
        private int turn;
        // all legal moves - first index is piece type - then a list of 2-length
        // arrays
        protected static int[][][] legalMoves = new int[N_PIECES][][];

        // indices for legalMoves
        public static final int ORIENT = 0;
        public static final int SLOT = 1;

        // possible orientations for a given piece type
        protected static int[] pOrients = { 1, 2, 4, 4, 4, 2, 2 };

        // the next several arrays define the piece vocabulary in detail
        // width of the pieces [piece ID][orientation]
        protected static int[][] pWidth = { { 2 }, { 1, 4 }, { 2, 3, 2, 3 }, { 2, 3, 2, 3 }, { 2, 3, 2, 3 }, { 3, 2 },
                { 3, 2 } };
        // height of the pieces [piece ID][orientation]
        private static int[][] pHeight = { { 2 }, { 4, 1 }, { 3, 2, 3, 2 }, { 3, 2, 3, 2 }, { 3, 2, 3, 2 }, { 2, 3 },
                { 2, 3 } };
        private static int[][][] pBottom = { { { 0, 0 } }, { { 0 }, { 0, 0, 0, 0 } },
                { { 0, 0 }, { 0, 1, 1 }, { 2, 0 }, { 0, 0, 0 } }, { { 0, 0 }, { 0, 0, 0 }, { 0, 2 }, { 1, 1, 0 } },
                { { 0, 1 }, { 1, 0, 1 }, { 1, 0 }, { 0, 0, 0 } }, { { 0, 0, 1 }, { 1, 0 } },
                { { 1, 0, 0 }, { 0, 1 } } };
        private static int[][][] pTop = { { { 2, 2 } }, { { 4 }, { 1, 1, 1, 1 } },
                { { 3, 1 }, { 2, 2, 2 }, { 3, 3 }, { 1, 1, 2 } }, { { 1, 3 }, { 2, 1, 1 }, { 3, 3 }, { 2, 2, 2 } },
                { { 3, 2 }, { 2, 2, 2 }, { 2, 3 }, { 1, 2, 1 } }, { { 1, 2, 2 }, { 3, 2 } },
                { { 2, 2, 1 }, { 2, 3 } } };

        // CONSTRUCTORS
        // duplicates an existing State object into a StateView object
        public StateView(State state) {
            field = duplicate(state.getField());
            top = Arrays.copyOf(state.getTop(), state.getTop().length);
            turn = state.getTurnNumber();
        }

        // creates a StateView object from field and top
        public StateView(int[][] field, int[] top) {
            this.field = field;
            this.top = top;
        }

        // duplicates a given field
        private static int[][] duplicate(int[][] field) {
            int[][] duplicateField = new int[ROWS][COLS];
            for (int i = 0; i < ROWS; i++) {
                for (int j = 0; j < COLS; j++) {
                    duplicateField[i][j] = field[i][j];
                }
            }
            return duplicateField;
        }

        // GETTERS
        public int[][] getField() {
            return field;
        }

        public int[] getTop() {
            return top;
        }

        // initialize legalMoves
        {
            // for each piece type
            for (int i = 0; i < N_PIECES; i++) {
                // figure number of legal moves
                int n = 0;
                for (int j = 0; j < pOrients[i]; j++) {
                    // number of locations in this orientation
                    n += COLS + 1 - pWidth[i][j];
                }
                // allocate space
                legalMoves[i] = new int[n][2];
                // for each orientation
                n = 0;
                for (int j = 0; j < pOrients[i]; j++) {
                    // for each slot
                    for (int k = 0; k < COLS + 1 - pWidth[i][j]; k++) {
                        legalMoves[i][n][ORIENT] = j;
                        legalMoves[i][n][SLOT] = k;
                        n++;
                    }
                }
            }

        }

        // evaluates and returns score of a given potential move
        public NextState makeMoveView(int orient, int slot, int currPiece) {
            // MODIFICATION from State class: duplicate field to restart at
            // original field position (before move is made)
            int[][] field = duplicate(this.field);
            int[] top = Arrays.copyOf(this.top, this.top.length);
            this.turn++;

            // height if the first column makes contact
            // MODIFICATION from State class: current piece is being tested
            // instead of nextPiece
            int height = top[slot] - pBottom[currPiece][orient][0];
            // for each column beyond the first in the piece
            for (int c = 1; c < pWidth[currPiece][orient]; c++) {
                height = Math.max(height, top[slot + c] - pBottom[currPiece][orient][c]);
            }

            // LOSS CONDITION
            // check if game ended, if move results in a loss, return Score
            // object with no rows cleared
            if (height + pHeight[currPiece][orient] >= ROWS) {
                return new NextState(field, top, true, 0);
            }

            // IF NOT LOSS CONDITION, FIND STATE SCORE AFTER APPLYING CURRENT
            // MOVE
            // for each column in the piece - fill in the appropriate blocks
            for (int i = 0; i < pWidth[currPiece][orient]; i++) {
                // from bottom to top of brick
                for (int h = height + pBottom[currPiece][orient][i]; h < height + pTop[currPiece][orient][i]; h++) {
                    field[h][i + slot] = turn;
                }
            }
            // adjust top
            for (int c = 0; c < pWidth[currPiece][orient]; c++) {
                top[slot + c] = height + pTop[currPiece][orient][c];
            }
            int rowsCleared = 0;
            // check for full rows - starting at the top
            for (int r = height + pHeight[currPiece][orient] - 1; r >= height; r--) {
                // check all columns in the row
                boolean full = true;
                for (int c = 0; c < COLS; c++) {
                    if (field[r][c] == 0) {
                        full = false;
                        break;
                    }
                }
                // if the row was full - remove it and slide above stuff down
                if (full) {
                    rowsCleared++;

                    // for each column
                    for (int c = 0; c < COLS; c++) {

                        // slide down all bricks
                        for (int i = r; i < top[c]; i++) {
                            field[i][c] = field[i + 1][c];
                        }
                        // lower the top
                        top[c]--;
                        while (top[c] >= 1 && field[top[c] - 1][c] == 0)
                            top[c]--;
                    }
                }
            }
            return new NextState(field, top, false, rowsCleared);
        }
    }

    // heuristic score
    public static class NextState {

        private final StateView state;
        private final int rowsCleared;
        private final boolean lost;

        // constructor
        public NextState(int[][] field, int[] top, boolean lost, int rowsCleared) {
            this.state = new StateView(field, top);
            this.rowsCleared = rowsCleared;
            this.lost = lost;

        }

        // getters
        public int getRowsCleared() {
            return rowsCleared;
        }

        public boolean hasLost() {
            return lost;
        }

        public StateView getStateView() {
            return state;
        }
    }

}

/* GENETIC ALGORITHM FOR TRAINING
 * ******************************
 * ******************************


public class Gene implements Comparable<Gene>{
    
    private final int NUM_HEURISTICS = 8;
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
        return Double.compare(this.fitness, other.fitness);
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

public class Genepool {
    
    private final int NUM_HEURISTICS = 8;
    private final double FRAC_IN_TOURNAMENT = 0.1;
    private final double OFFSPRING_POOL_LIMIT = 0.3;
    int numGames = 10;
    
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
                return o2.compareTo(o1);
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
        //ArrayList<Gene> tournament = new ArrayList<Gene>();
        PriorityQueue<Gene> tournament = new PriorityQueue<Gene>(Collections.reverseOrder());
        int numContenders = (int)(FRAC_IN_TOURNAMENT * geneNumber);
        Set<Integer> chosenOnes = new HashSet<Integer>();
        
        while(chosenOnes.size() < numContenders) {
            int randIndex = (int) (Math.random() * geneNumber);
            chosenOnes.add(randIndex);
        }
        for(int i : chosenOnes){
            tournament.add(genepool.get(i));
        }
        //int firstIndex = getMax(tournament, -1);
        //int secondIndex = getMax(tournament, firstIndex);
        //Gene parent1 = tournament.get(firstIndex);
        //Gene parent2 = tournament.get(secondIndex);
        Gene parent1 = tournament.poll();
        Gene parent2 = tournament.poll();
        // Create a new gene from the 2 fittest genes
        Gene newGene = parent1.waCrossover(parent2);
        
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
    
    public static int getMax(List<Gene> list, int index) {
        int res = 0;
        Gene current = null;
        for(int i = 0; i < list.size(); i++) {
            if(i != index) {
                if(current != null) {
                    if(list.get(i).compareTo(current) > 0) {
                        res = i;
                    }
                } else {
                    current = list.get(i);
                    res = i;
                }
            }   
        }
        return res;
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

public static void main(String[] args) {
        int generations = 1000;
        int numGenes = 40;
        int numGames = 20;
        int GENETHREADS = 40;    //for multithreading
        
        //for running multiple threads concurrently
        ExecutorService executor = Executors.newFixedThreadPool(GENETHREADS);
        
        System.out.println("Generating " + numGenes + " genes in genepool");
        // Generate 1000 random genes
        Genepool gp = new Genepool(numGenes);
        System.out.println("Starting game simulations");
        // Run games with each gene a number of times to get the initial fitness scores
        for(int i = 0; i < gp.size(); i++) {
            Runnable geneRunnable = new GeneRunnable(numGames, gp.get(i), i);
            executor.execute(geneRunnable);
            //runGames(numGames, gp.get(i));
            //System.out.println("Finished gene number " + i);
        }
         executor.shutdown();
         // Wait until all threads are finish
         while (!executor.isTerminated()) {
         }
        
        gp.toFile("Genedata_before.txt");

        System.out.println("Starting tournaments");
        // One generation means doing a 'cleansing' of the bottom 30% of genes once
        for(int i = 0; i < generations; i++){
            gp.runGeneticAlgo();
            System.out.println("Finished " + (i + 1) + " generations");
            //FOR CHECKING
            gp.toFile("Generation" + i + "Data.txt");
        }
        gp.toFile("GeneData.txt");
        System.out.println("Finished training");
    }
    
    public static class GeneRunnable implements Runnable {
        private final int numGames;
        private final Gene gene;
        private final int geneNum;
        
        GeneRunnable(int numGames, Gene gene, int geneNum) {
            this.numGames = numGames;
            this.gene = gene;
            this.geneNum = geneNum;
        }
 
        @Override
        public void run() {
            runGames(numGames, gene);
            System.out.println("Finished gene number " + geneNum);
        }
    }
    
    // Run a number of games with the gene, and average each game's number
    // of rows cleared to get the gene's fitness score
    // Returns the fitness score of the gene
    public static double runGames(int numGames, Gene gene) {
        int sumFitness = 0;
        for(int i = 0; i < numGames; i++) {
            State s = new State();
            PlayerSkeleton p = new PlayerSkeleton();

            // while the game is not lost, continue making next move
            while (!s.hasLost()) {
                s.makeMove(p.chooseMove(s, s.getNextPiece(), s.legalMoves(), gene));
                //s.draw();
                //s.drawNext(0, 0);
            }
            sumFitness += s.getRowsCleared();
        }
        // Average the gene's fitness score over all games
        double fitnessScore = ((double) sumFitness)/numGames;
        gene.setFitness(fitnessScore);
        return fitnessScore;
    }
    
    // AI picks the next move
    public int chooseMove(State s, int nextPiece, int[][] legalMoves, Gene gene) {
        // clear the current movesList, before adding new legal moves for next
        // move
        movesList.clear();

        // create a new StateView object from State object for evaluating
        // possible next states
        StateView currState = new StateView(s);

        // run through legal moves and add all possible moves to movesList
        for (int i = 0; i < legalMoves.length; i++) {
            int orient = legalMoves[i][0];
            int slot = legalMoves[i][1];
            movesList.add(new Move(currState, i, s.getNextPiece(), orient, slot));
        }

        // choose the best next move based on evaluation function
        int chosenMoveIndex = evalMoves(movesList, gene);
        return chosenMoveIndex;
    }

    // evaluates the best next move and returns that move
    public int evalMoves(ArrayList<Move> movesList, Gene gene) {
        double maxScore = -Double.MAX_VALUE;
        double currScore = -Double.MAX_VALUE;
        int chosenMoveIndex = 0;

        // for every possible move in the movesList, evaluate its score
        for (int i = 0; i < movesList.size(); i++) {
            currScore = evalScore(movesList.get(i), gene);
            // choose the highest score, and update it as our chosen move
            // for equal scores, just take the first that comes along
            if (currScore > maxScore) {
                maxScore = currScore;
                chosenMoveIndex = movesList.get(i).getIndex();
            }
        }
        return chosenMoveIndex;
    }

    // calculate move score based on overall heuristic, weighted between 4
    // heuristic components
    public double evalScore(Move move, Gene gene) {
        
        double[] vector = gene.getVector();
        
        // WEIGHTS, base: educated guesses based on observations (rather than random)
        double rowsClearedWeight = Math.abs(0.5 + vector[0]);
        double numHolesWeight = Math.abs(5 + vector[1]);
        double numVerticalConnectedHolesWeight = Math.abs(0.1 + vector[2]);
        //double totalHeightWeight = 0.5;
        double heightVarWeight = Math.abs(0.6 + vector[3]);
        double maxHeightWeight = Math.abs(0.5 + vector[4]); 
        double totalWellDepthWeight = Math.abs(0.9 + vector[5]);
        double maxWellDepthWeight = Math.abs(0.3 + vector[6]);
        //double maxHeightDiffWeight = 0.1;
        double avgHeightDiffFromMeanWeight = Math.abs(0.9 + vector[7]);

        // find next state after applying move
        NextState nextState = move.getStateView().makeMoveView(move.getOrient(), move.getSlot(), move.getPiece());

        // only calculate score if next move does not result in a loss
        // if loss, just assign it lowest possible score
        if (nextState.hasLost() == true){
            return -Double.MAX_VALUE;
        }
        else{
            return  rowsClearedWeight * evalRowsCleared(nextState) 
                    - numHolesWeight * evalNumHoles(nextState)
                    - numVerticalConnectedHolesWeight * evalNumVerticalConnectedHoles(nextState)
                    //- totalHeightWeight * evaltotalHeight(nextState) 
                    - heightVarWeight * evalHeightVar(nextState)
                    - maxHeightWeight * evalMaxHeight(nextState) 
                    - totalWellDepthWeight * evalTotalWellDepth(nextState) 
                    - maxWellDepthWeight * evalMaxWellDepth(nextState) 
                    //- maxHeightDiffWeight * evalMaxHeightDifference(nextState)
                    - avgHeightDiffFromMeanWeight * evalAverageHeightDifferenceFromMean(nextState);
        }
    }

        // MAXIMIZE - because it is the game's objective
        // no. of rows cleared heuristic component
        public int evalRowsCleared(NextState nextState) {
            return nextState.getRowsCleared();
        }

        // MINIMIZE - because holes cannot be filled up
        // no. of holes heuristic component - hole is an unused square with a used
        // square above it
        public int evalNumHoles(NextState nextState) {
            // get the field and top arrays of the state after next move is applied
            int[][] field = nextState.getStateView().getField();
            int[] top = nextState.getStateView().getTop();
            int numOfHoles = 0;

            // for each column, count the number of unused spaces below the top used
            // space
            for (int i = 0; i < State.COLS; i++) {
                for (int j = top[i] - 1; j >= 0; j--) { // only check for spaces
                                                        // BELOW the top used space
                    if (field[j][i] == 0) { // if empty/unused
                        numOfHoles++;
                    }
                }
            }
            return numOfHoles;
        }
        
        // MINIMIZE - because holes cannot be filled up
        // no. of vertically connected holes heuristic component
        // vertically connected hole is a series of unused squares along a column, with at least one used square above it
        public int evalNumVerticalConnectedHoles(NextState nextState) {
            // get the field and top arrays of the state after next move is applied
            int[][] field = nextState.getStateView().getField();
            int[] top = nextState.getStateView().getTop();
            int numOfVerticalConnectedHoles = 0;

            // for each column, count the number of connected holes below the top used
            // space
            for (int i = 0; i < State.COLS; i++) {
                for (int j = top[i] - 1; j >= 0; j--) { // only check for spaces
                                                        // BELOW the top used space
                    if (field[j][i] == 0 && field[j+1][i] != 0) { // only count if above is filled
                        numOfVerticalConnectedHoles++;
                    }
                }
            }
            return numOfVerticalConnectedHoles;
        }

        // MINIMIZE - so there is more space to drop new pieces
        // total height heuristic component - total 'unavailable' space
        public int evalTotalHeight(NextState nextState) {
            // get the top array of the state after next move is applied
            int[] top = nextState.getStateView().getTop();
            int totalHeight = 0;

            // find the sum of the heights of each column of the state after next
            // move applied
            for (int i = 0; i < State.COLS; i++) {
                totalHeight = totalHeight + top[i];
            }
            return totalHeight;
        }

        // MINIMIZE - to avoid deep wells
        // height variance heuristic component - the sum of the ABSOLUTE differences
        // between every adjacent column
        public int evalHeightVar(NextState nextState) {
            // get the top array of the state after next move is applied
            int[] top = nextState.getStateView().getTop();
            int sumVar = 0;

            // find the sum of the absolute differences in heights of every adjacent
            // column of the state after next move applied
            for (int i = 1; i < State.COLS; i++) {
                sumVar = sumVar + Math.abs(top[i] - top[i - 1]);
            }
            return sumVar;
        }

        // MINIMIZE - to keep max height level low, further
        // maximum height heuristic component
        public int evalMaxHeight(NextState nextState) {
            // get the top array of the state after next move is applied
            int[] top = nextState.getStateView().getTop();
            int maxHeight = 0;

            // find the max height of all the column heights
            for (int i = 0; i < State.COLS; i++) {
                if (top[i] > maxHeight) {
                    maxHeight = top[i];
                }
            }
            return maxHeight;
        }
        
        // MINIMIZE - to keep total well depth low
        // total well depth heuristic component
        // well is a column with both its adjacent columns higher than it by at least 2 blocks
        // well depth is the height from that column to the height of the shorter higher adjacent column
        public int evalTotalWellDepth(NextState nextState){
            
            int[] top = nextState.getStateView().getTop();
            int totalWellDepth = 0;

            // check if 1st column is a well (does not have left adjacent column)
            // check 2nd column - 1st column if at least 2
            if (top[1] - top[0] >= 2) {
                totalWellDepth = totalWellDepth + (top[1] - top[0]);
            }
            
            // check if 10th column is a well (does not have right adjacent column)
            // check 9th column - 10th column if at least 2
            if (top[8] - top[9] >= 2) {
                totalWellDepth = totalWellDepth + (top[8] - top[9]);
            }

            // check if 2nd to 9th columns are wells (have both left and right adjacent columns)
            for (int i = 1; i < State.COLS - 1; i++) {
                // take the minimum height diff between left and right adjacent columns
                // and check if minimum height diff is at least 2 (to be considered well)
                if (Math.min(top[i-1] - top[i], top[i+1] - top[i]) >= 2){
                    totalWellDepth = totalWellDepth + Math.min(top[i-1] - top[i], top[i+1] - top[i]);
                }
            }

            return totalWellDepth;
        }
        
        // MINIMIZE - to keep maximum well depth low
        // maximum well depth heuristic component
        // well is a column with both its adjacent columns higher than it by at least 2 blocks
        // well depth is the height from that column to the height of the shorter higher adjacent column
        public int evalMaxWellDepth(NextState nextState){
            int[] top = nextState.getStateView().getTop();
            int maxWellDepth = 0;

            // check if 1st column is a well (does not have left adjacent column)
            // check 2nd column - 1st column if at least 2
            if (top[1] - top[0] >= 2 && top[1] - top[0] > maxWellDepth) {
                maxWellDepth = top[1] - top[0];
            }
            
            // check if 10th column is a well (does not have right adjacent column)
            // check 9th column - 10th column if at least 2
            if (top[8] - top[9] >= 2 && top[8] - top[9] > maxWellDepth) {
                maxWellDepth = top[8] - top[9];
            }

            // check if 2nd to 9th columns are wells (have both left and right adjacent columns)
            for (int i = 1; i < State.COLS - 1; i++) {
                // take the minimum height diff between left and right adjacent columns
                // and check if minimum height diff is at least 2 (to be considered well)
                if (Math.min(top[i-1] - top[i], top[i+1] - top[i]) >= 2 && Math.min(top[i-1] - top[i], top[i+1] - top[i]) > maxWellDepth){
                    maxWellDepth = Math.min(top[i-1] - top[i], top[i+1] - top[i]);
                }
            }
            return maxWellDepth;
        }
        
        // MINIMIZE - to keep height difference between max and min height low
        // maximum height difference heuristic component
        public int evalMaxHeightDifference(NextState nextState) {
            // get the top array of the state after next move is applied
            int[] top = nextState.getStateView().getTop();
            int maxHeight = 0;
            int minHeight = State.ROWS + 1;

            // find the max and min heights among all columns
            for (int i = 0; i < State.COLS; i++) {
                if (top[i] > maxHeight) {
                    maxHeight = top[i];
                }
                if (top[i] < minHeight){
                    minHeight = top[i];
                }
            }
            return maxHeight - minHeight;
        }
        
        // MINIMIZE - to keep average height difference from mean low
        // average height difference from mean heuristic  component
        public double evalAverageHeightDifferenceFromMean(NextState nextState) {
            // get the top array of the state after next move is applied
            int[] top = nextState.getStateView().getTop();
            int totalHeight = 0;
            double meanHeight = 0; 
            double totalHeightDiffFromMean = 0;
            double averageHeightDiffFromMean = 0;
            
            // find the sum of the heights of each column of the state after next
            // move applied
            for (int i = 0; i < State.COLS; i++) {
                totalHeight = totalHeight + top[i];
            }
            
            meanHeight = (double) totalHeight / (double) State.COLS;
            for (int i = 0; i < State.COLS; i++) {
                totalHeightDiffFromMean =  Math.abs(top[i] - meanHeight);
            }
            averageHeightDiffFromMean = totalHeightDiffFromMean / (double) State.COLS;
            
            return averageHeightDiffFromMean;
        }

    // class for each possible tetris move
    private static class Move {

        private StateView stateView;
        private int index;
        private int piece;
        private int orient;
        private int slot;

        // constructor
        public Move(StateView stateView, int index, int piece, int orient, int slot) {
            this.stateView = stateView;
            this.index = index;
            this.piece = piece;
            this.orient = orient;
            this.slot = slot;
        }

        // getters
        public StateView getStateView() {
            return stateView;
        }

        public int getIndex() {
            return index;
        }

        public int getPiece() {
            return piece;
        }

        public int getOrient() {
            return orient;
        }

        public int getSlot() {
            return slot;
        }

    }

    // class for evaluating next state after a possible move
    // 'view' state after a move is made, without actually making that move
    private static class StateView {

        public static final int COLS = 10;
        public static final int ROWS = 21;
        public static final int N_PIECES = 7;
        public boolean lost = false;

        // each square in the grid - int means empty - other values mean the
        // turn it was placed
        private int[][] field = new int[ROWS][COLS];
        // top row+1 of each column
        // 0 means empty
        private int[] top = new int[COLS];
        private int turn;
        // all legal moves - first index is piece type - then a list of 2-length
        // arrays
        protected static int[][][] legalMoves = new int[N_PIECES][][];

        // indices for legalMoves
        public static final int ORIENT = 0;
        public static final int SLOT = 1;

        // possible orientations for a given piece type
        protected static int[] pOrients = { 1, 2, 4, 4, 4, 2, 2 };

        // the next several arrays define the piece vocabulary in detail
        // width of the pieces [piece ID][orientation]
        protected static int[][] pWidth = { { 2 }, { 1, 4 }, { 2, 3, 2, 3 }, { 2, 3, 2, 3 }, { 2, 3, 2, 3 }, { 3, 2 },
                { 3, 2 } };
        // height of the pieces [piece ID][orientation]
        private static int[][] pHeight = { { 2 }, { 4, 1 }, { 3, 2, 3, 2 }, { 3, 2, 3, 2 }, { 3, 2, 3, 2 }, { 2, 3 },
                { 2, 3 } };
        private static int[][][] pBottom = { { { 0, 0 } }, { { 0 }, { 0, 0, 0, 0 } },
                { { 0, 0 }, { 0, 1, 1 }, { 2, 0 }, { 0, 0, 0 } }, { { 0, 0 }, { 0, 0, 0 }, { 0, 2 }, { 1, 1, 0 } },
                { { 0, 1 }, { 1, 0, 1 }, { 1, 0 }, { 0, 0, 0 } }, { { 0, 0, 1 }, { 1, 0 } },
                { { 1, 0, 0 }, { 0, 1 } } };
        private static int[][][] pTop = { { { 2, 2 } }, { { 4 }, { 1, 1, 1, 1 } },
                { { 3, 1 }, { 2, 2, 2 }, { 3, 3 }, { 1, 1, 2 } }, { { 1, 3 }, { 2, 1, 1 }, { 3, 3 }, { 2, 2, 2 } },
                { { 3, 2 }, { 2, 2, 2 }, { 2, 3 }, { 1, 2, 1 } }, { { 1, 2, 2 }, { 3, 2 } },
                { { 2, 2, 1 }, { 2, 3 } } };

        // CONSTRUCTORS
        // initializes an empty state
        public StateView() {
            field = new int[ROWS][COLS];
            top = new int[COLS];
            turn = 0;
        }

        // duplicates an existing State object into a StateView object
        public StateView(State state) {
            field = duplicate(state.getField());
            top = Arrays.copyOf(state.getTop(), state.getTop().length);
            turn = state.getTurnNumber();
        }

        // creates a StateView object from field, top and turn
        public StateView(int[][] field, int[] top, int turn) {
            this.field = field;
            this.top = top;
            this.turn = turn;
        }

        // creates a StateView object from field and top
        public StateView(int[][] field, int[] top) {
            this.field = field;
            this.top = top;
        }

        // duplicates a given field
        private static int[][] duplicate(int[][] field) {
            int[][] duplicateField = new int[ROWS][COLS];
            for (int i = 0; i < ROWS; i++) {
                for (int j = 0; j < COLS; j++) {
                    duplicateField[i][j] = field[i][j];
                }
            }
            return duplicateField;
        }

        // GETTERS
        public int[][] getField() {
            return field;
        }

        public int[] getTop() {
            return top;
        }

        public boolean hasLost() {
            return lost;
        }

        public int getTurn() {
            return turn;
        }

        // initialize legalMoves
        {
            // for each piece type
            for (int i = 0; i < N_PIECES; i++) {
                // figure number of legal moves
                int n = 0;
                for (int j = 0; j < pOrients[i]; j++) {
                    // number of locations in this orientation
                    n += COLS + 1 - pWidth[i][j];
                }
                // allocate space
                legalMoves[i] = new int[n][2];
                // for each orientation
                n = 0;
                for (int j = 0; j < pOrients[i]; j++) {
                    // for each slot
                    for (int k = 0; k < COLS + 1 - pWidth[i][j]; k++) {
                        legalMoves[i][n][ORIENT] = j;
                        legalMoves[i][n][SLOT] = k;
                        n++;
                    }
                }
            }

        }

        // evaluates and returns score of a given potential move
        public NextState makeMoveView(int orient, int slot, int currPiece) {
            // MODIFICATION from State class: duplicate field to restart at
            // original field position (before move is made)
            int[][] field = duplicate(this.field);
            int[] top = Arrays.copyOf(this.top, this.top.length);
            this.turn++;

            // height if the first column makes contact
            // MODIFICATION from State class: current piece is being tested
            // instead of nextPiece
            int height = top[slot] - pBottom[currPiece][orient][0];
            // for each column beyond the first in the piece
            for (int c = 1; c < pWidth[currPiece][orient]; c++) {
                height = Math.max(height, top[slot + c] - pBottom[currPiece][orient][c]);
            }

            // LOSS CONDITION
            // check if game ended, if move results in a loss, return Score
            // object with no rows cleared
            if (height + pHeight[currPiece][orient] >= ROWS) {
                lost = true;
                return new NextState(field, top, lost, 0);
            }

            // IF NOT LOSS CONDITION, FIND STATE SCORE AFTER APPLYING CURRENT
            // MOVE
            // for each column in the piece - fill in the appropriate blocks
            for (int i = 0; i < pWidth[currPiece][orient]; i++) {
                // from bottom to top of brick
                for (int h = height + pBottom[currPiece][orient][i]; h < height + pTop[currPiece][orient][i]; h++) {
                    field[h][i + slot] = turn;
                }
            }
            // adjust top
            for (int c = 0; c < pWidth[currPiece][orient]; c++) {
                top[slot + c] = height + pTop[currPiece][orient][c];
            }
            int rowsCleared = 0;
            // check for full rows - starting at the top
            for (int r = height + pHeight[currPiece][orient] - 1; r >= height; r--) {
                // check all columns in the row
                boolean full = true;
                for (int c = 0; c < COLS; c++) {
                    if (field[r][c] == 0) {
                        full = false;
                        break;
                    }
                }
                // if the row was full - remove it and slide above stuff down
                if (full) {
                    rowsCleared++;

                    // for each column
                    for (int c = 0; c < COLS; c++) {

                        // slide down all bricks
                        for (int i = r; i < top[c]; i++) {
                            field[i][c] = field[i + 1][c];
                        }
                        // lower the top
                        top[c]--;
                        while (top[c] >= 1 && field[top[c] - 1][c] == 0)
                            top[c]--;
                    }
                }
            }
            return new NextState(field, top, lost, rowsCleared);
        }
    }

    // heuristic score
    public static class NextState {

        private final StateView state;
        private final int rowsCleared;
        private final boolean lost;

        // constructor
        public NextState(int[][] field, int[] top, boolean lost, int rowsCleared) {
            this.state = new StateView(field, top);
            this.rowsCleared = rowsCleared;
            this.lost = lost;

        }

        // getters
        public int getRowsCleared() {
            return rowsCleared;
        }

        public boolean hasLost() {
            return lost;
        }

        public StateView getStateView() {
            return state;
        }
    }

}
*/