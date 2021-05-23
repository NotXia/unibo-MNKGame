package mnkgame;

import java.util.LinkedList;

public class Matrix {
    private MNKCellState[][] matrix;
    private int size;
    private int columns, rows, target;

    public Matrix(int columns, int rows, int target) {
        this.matrix = new MNKCellState[columns][rows];
        for (int y=0; y<rows; y++) {
            for (int x=0; x<columns; x++) {
                this.matrix[x][y] = MNKCellState.FREE;
            }
        }

        this.size = 0;
        this.columns = columns;
        this.rows = rows;
        this.target = target;
    }

    public void setAt(int x, int y, MNKCellState state) {
        if (state != null && state != MNKCellState.FREE) {
            matrix[x][y] = state;
            size++;
        }
        else {
            matrix[x][y] = MNKCellState.FREE;
            size--;
        }
    }

    public MNKCellState getAt(int x, int y) {
        if (x < 0 || x >= columns || y < 0 || y >= rows) { return null; }
        return matrix[x][y];
    }

    public void removeAt(int x, int y) {
        if (matrix[x][y] != MNKCellState.FREE) {
            matrix[x][y] = MNKCellState.FREE;
            size--;
        }
    }

    public int size() {
        return size;
    }

    /**
     * Restituisce un vettore contenente la configurazione dell'intera riga a cui appartiene la posizione in input
     * @implNote Costo: O(N)
     * */
    public MNKCellState[] getRowAt(int start_x, int start_y) {
        MNKCellState[] row = new MNKCellState[columns];
        for (int x=0; x<columns; x++) {
            row[x] = matrix[x][start_y];
        }
        return row;
    }

    /**
     * Restituisce un vettore contenente la configurazione dell'intera colonna a cui appartiene la posizione in input
     * @implNote Costo: O(M)
     * */
    public MNKCellState[] getColumnAt(int start_x, int start_y) {
        MNKCellState[] column = new MNKCellState[rows];
        for (int y=0; y<rows; y++) {
            column[y] = matrix[start_x][y];
        }
        return column;
    }

    /**
     * Restituisce un vettore contenente la configurazione dell'intera diagonale principale a cui appartiene la posizione in input
     * @implNote Costo: O(min{M, N})
     * */
    public MNKCellState[] getMainDiagonalAt(int start_x, int start_y) {
        LinkedList<MNKCellState> buffer = new LinkedList<>();

        int i = start_x, j = start_y;
        while (i < columns && j < rows) {
            buffer.addLast(matrix[i][j]);
            i++; j++;
        }

        i = start_x-1;
        j = start_y-1;
        while (i >= 0 && j >= 0) {
            buffer.addFirst(matrix[i][j]);
            i--; j--;
        }

        return buffer.toArray(new MNKCellState[buffer.size()]);
    }

    /**
     * Restituisce un vettore contenente la configurazione dell'intera diagonale secondaria a cui appartiene la posizione in input
     * @implNote Costo: O(min{M, N})
     * */
    public MNKCellState[] getSecondaryDiagonalAt(int start_x, int start_y) {
        LinkedList<MNKCellState> buffer = new LinkedList<>();

        int i = start_x, j = start_y;
        while (i < columns && j >= 0) {
            buffer.addFirst(matrix[i][j]);
            i++; j--;
        }

        i = start_x-1;
        j = start_y+1;
        while (i >= 0 && j < rows) {
            buffer.addLast(matrix[i][j]);
            i--; j++;
        }

        return buffer.toArray(new MNKCellState[buffer.size()]);
    }

    public String toString(MNKCellState playerState) {
        String out = "";
        for (int y=0; y<rows-1; y++) {
            for (int x=0; x<columns; x++) {
                out += (matrix[x][y] == MNKCellState.FREE ?
                            "-"
                        :
                            matrix[x][y] == playerState ?
                                "Me"
                            :
                                "Op")
                        + "\t";
            }
            out += "\n";
        }
        for (int x=0; x<columns; x++) {
            out += (matrix[x][rows-1] == MNKCellState.FREE ?
                        "-"
                    :
                        matrix[x][rows-1] == playerState ?
                            "Me"
                        :
                            "Op")
                    + "\t";
        }
        return out;
    }
}
