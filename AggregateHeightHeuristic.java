public class AggregateHeightHeuristic {
  
  int totalHeight;
  int currHeight;
  
  public AggregateHeightHeuristic() {
    totalHeight=0;
  }
  public int getAggregateHeight() {
    return totalHeight;
  }
  public void calculateAggregateHeight(int[][] field, int row, int col) {
    for (int i=0; i< col; i++) {
      currHeight=0;
      for (int j=row-1;j>0;j--) {
        if(field[i][j]!=0) { //this square is not empty
          currHeight++;
        }
      }
      totalHeight+=currHeight;
    }
  }
}