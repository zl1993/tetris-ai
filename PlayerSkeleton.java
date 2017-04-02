import java.util.*;

public class PlayerSkeleton {

    private ArrayList<Move> movesList = new ArrayList<Move>();

    public static void main(String[] args) {
        State s = new State();
        new TFrame(s);
        PlayerSkeleton p = new PlayerSkeleton();

        // while it game is not lost, continue making next move
        while (!s.hasLost()) {
            s.makeMove(p.chooseMove(s, s.getNextPiece(), s.legalMoves()));
            s.draw();
            s.drawNext(0, 0);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("You have completed " + s.getRowsCleared() + " rows.");
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
        // weights
 //       double[] weights = new double[5];
        //0.12686429241639857, 0.6202792374104067, 0.6776074904252886, 0.2600545225318098, 0.10923504505223744
        double rowsClearedWeight = 0.12686429241639857;//0.760666;
        double numHolesWeight = 0.6202792374104067;//0.35663;
        double spaceUsedWeight = 0.6776074904252886;
        double heightVarWeight = 0.2600545225318098;
        double maxHeightWeight = 0.10923504505223744;

        // find next state after applying move
        NextState nextState = move.getStateView().makeMoveView(move.getOrient(), move.getSlot(), move.getPiece());

        // only calculate score if next move does not result in a loss
        if (nextState.hasLost() == true)
            return -1;
        else
            return rowsClearedWeight * evalRowsCleared(nextState) - numHolesWeight * evalNumHoles(nextState)
                    - spaceUsedWeight * evalSpaceUsed(nextState) - heightVarWeight * evalHeightVar(nextState)
                    - maxHeightWeight * evalMaxHeight(nextState);
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

    // MINIMIZE - so there is more space to drop new pieces
    // total space used heuristic component - total 'taken up' space
    public int evalSpaceUsed(NextState nextState) {
        // get the top array of the state after next move is applied
        int[] top = nextState.getStateView().getTop();
        int spaceUsed = 0;

        // find the sum of the heights of each column of the state after next
        // move applied
        for (int i = 0; i < State.COLS; i++) {
            spaceUsed = spaceUsed + top[i];
        }
        return spaceUsed;
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

        // find the sum of the absolute differences in heights of every adjacent
        // column of the state after next move applied
        for (int i = 0; i < State.COLS; i++) {
            if (top[i] > maxHeight) {
                maxHeight = top[i];
            }
        }
        return maxHeight;
    }

    /*
    public int fitnessFunction(double rowsClearedWeight, double numHolesWeight, double spaceUsedWeight,
            double heightVarWeight, double maxHeightWeight) {
        
                //genetic algorithm variables
        int totalLinesCleared = 0;
        fitness

        double a = rowsClearedWeight;
        double b = numHolesWeight;
        double c = spaceUsedWeight;
        double d = heightVarWeight;
        double e = maxHeightWeight;
    }
    */

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