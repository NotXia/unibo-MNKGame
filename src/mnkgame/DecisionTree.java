package mnkgame;
import java.util.HashMap;
import java.util.LinkedList;

import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * java -cp ".." mnkgame.MNKGame 4 4 4 mnkgame.OurPlayer
 *
 * **/

public class DecisionTree {
    private Node root;
    private int rows, columns, target, curr_target;
    private boolean first;
    private final MNKCellState MY_STATE, OPPONENT_STATE;
    private final MNKGameState WIN_STATE, LOSS_STATE;
    private int maxScore, minScore;


    public DecisionTree(int M, int N, int K, boolean first) {
        this.root = null;
        this.rows = M;
        this.columns = N;
        this.target = K;
        this.curr_target = 2;

        this.first = first;
        this.MY_STATE = first ? MNKCellState.P1 : MNKCellState.P2;
        this.OPPONENT_STATE = first ? MNKCellState.P2 : MNKCellState.P1;
        this.WIN_STATE = first ? MNKGameState.WINP1 : MNKGameState.WINP2;
        this.LOSS_STATE = first ? MNKGameState.WINP2 : MNKGameState.WINP1;

        this.maxScore = M*N;
        this.minScore = -(M*N);
    }

    public boolean isEmpty() {
        return root == null;
    }

    private boolean isValidCell(int x, int y) {
        return (x >= 0 && x < rows) && (y >= 0 && y < columns);
    }

    private int alphabeta(Node node, boolean mePlaying, int alpha, int beta) {
        if (node.isLeaf()) {
            return node.score;
        }
        else {
            int eval;

            if (mePlaying) {
                eval = maxScore+1;
                int i = 0;
                for (Node child : node.children) {
                    eval = Math.min(eval, alphabeta(child, false, alpha, beta));
                    beta = Math.min(eval, beta);
                    if (beta <= alpha) {
                        node.children.subList(i+1, node.children.size()).clear(); /** Verificare se bisogna potare anche i figli prima (e non solo quelli dopo) **/
                        break;
                    }
                    i++;
                }
            }
            else {
                eval = minScore-1;
                int i = 0;
                for (Node child : node.children) {
                    eval = Math.max(eval, alphabeta(child, true, alpha, beta));
                    alpha = Math.max(eval, alpha);
                    if (beta <= alpha) {
                        node.children.subList(i+1, node.children.size()).clear();
                        break;
                    }
                    i++;
                }
            }

            node.score = eval;
            return eval;
        }
    }


    private Node createTree(Node toEval, boolean mePlaying, int depth, MNKCellState[][] board, int boardSize) {
        MNKGameState gameState = boardStatus(board, boardSize, toEval.action.i, toEval.action.j);

        if (gameState == MNKGameState.OPEN) {
            // Tiene traccia delle celle già elaborate
            HashMap<String, Boolean> hasBeenEvaluated = new HashMap<>();

            // Valuta tutte le celle adiacenti a quelle già marcate
            for (MNKCell markedCell : toEval.getMarkedCells()) {
                for (int i=-1; i<=1; i++) {
                    for (int j=-1; j<=1; j++) {
                        if (i == 0 && j == 0) { continue; }

                        int toVisit_x = markedCell.i+i;
                        int toVisit_y = markedCell.j+j;

                        if (isValidCell(toVisit_x, toVisit_y)) {
                            if (board[toVisit_x][toVisit_y] == null && hasBeenEvaluated.get(""+toVisit_x+" "+toVisit_y) == null) {
                                MNKCellState state = mePlaying ? MY_STATE : OPPONENT_STATE;

                                MNKCell toEvalCell = new MNKCell(toVisit_x, toVisit_y, state);
                                Node child = new Node(toEval, toEvalCell);

                                board[toVisit_x][toVisit_y] = state;
                                // A parità di punteggio si darà priorità alle mosse in celle centrali
                                if (toVisit_x == 0 || toVisit_x == rows-1 || toVisit_y == 0 || toVisit_y == columns-1) {
                                    toEval.children.addLast( createTree(child, !mePlaying, depth+1, board, boardSize+1) );
                                }
                                else {
                                    toEval.children.addFirst( createTree(child, !mePlaying, depth+1, board, boardSize+1) );
                                }
                                board[toVisit_x][toVisit_y] = null;

                                /*if (alphabeta(toEval, !mePlaying, minScore, maxScore) > 0) {
                                    return toEval;
                                }*/

                                hasBeenEvaluated.put(""+toVisit_x+" "+toVisit_y, true);
                            }
                        }
                    }
                }
            }
            alphabeta(toEval, !mePlaying, minScore, maxScore);
        }
        else {
            if (gameState == WIN_STATE) {
                toEval.score = maxScore - depth;
            }
            else if (gameState == LOSS_STATE) {
                toEval.score = minScore + depth;
            }
            else {
                toEval.score = 0;
            }
        }

        return toEval;
    }

    public void generate(MNKCell markedCell) {
        root = new Node(null, markedCell);

        MNKCellState[][] board = new MNKCellState[rows][columns];
        board[markedCell.i][markedCell.j] = markedCell.state;

        createTree(root, !first, 0, board, 1);
    }

    public void generate(Node newRoot) {
        this.root = newRoot;
        MNKCellState[][] board = new MNKCellState[rows][columns];
        int boardSize = 0;
        boolean mePlaying = newRoot.action.state == MY_STATE ? true : false;

        for (MNKCell cell : newRoot.getMarkedCells()) {
            board[cell.i][cell.j] = cell.state;
            boardSize++;
        }

        createTree(root, !mePlaying, 0, board, boardSize);
    }

    public void setOpponentMove(MNKCell move) {
        if (root.isLeaf() && curr_target < target) {
            curr_target++;
            generate(root);
        }

        Node bestChild = root.children.getFirst();

        for (Node child : root.children) {
            if (child.action.equals(move)) {
                bestChild = child;
                break;
            }
            if (child.score > bestChild.score) {
                bestChild = child;
            }
        }

        root = bestChild;
        //root.parent = null;
    }
    /**
     * TODO: Eliminare i figli >:D
     * **/
    public MNKCell nextMove() {
        if (root.isLeaf() && curr_target < target) {
            curr_target++;
            generate(root);
        }

        Node bestChild = root.children.getFirst();

        for (Node child : root.children) {
            if (child.score > bestChild.score) {
                bestChild = child;
            }
        }

        root = bestChild;
        //root.parent = null;

        return bestChild.action;
    }


    private MNKGameState boardStatus(MNKCellState[][] board, int boardSize, int x, int y) {
        MNKCellState toCheckState = board[x][y];

        int aligned;
        MNKGameState result;

        if (toCheckState == MY_STATE) {
            result = WIN_STATE;
        }
        else {
            result = LOSS_STATE;
        }

        // VERTICALE
        aligned = 1;
        for (int i=1; i<=curr_target && x-i>=0; i++) { // Sx
            if (board[x-i][y] == toCheckState) { aligned++; }
            else { break; }
        }
        for (int i=1; i<=curr_target && x+i<rows; i++) { // Dx
            if (board[x+i][y] == toCheckState) { aligned++; }
            else { break; }
        }
        if (aligned >= curr_target) { return result; }

        // ORIZZONTALE
        aligned = 1;
        for (int i=1; i<=curr_target && y-i>=0; i++) { // Alto
            if (board[x][y-i] == toCheckState) { aligned++; }
            else { break; }
        }
        for (int i=1; i<=curr_target && y+i<columns; i++) { // Basso
            if (board[x][y+i] == toCheckState) { aligned++; }
            else { break; }
        }
        if (aligned >= curr_target) { return result; }

        // OBLIQUO SX a DX
        aligned = 1;
        for (int i=1; i<=curr_target && x-i>=0 && y-i>=0; i++) { // Alto sx
            if (board[x-i][y-i] == toCheckState) { aligned++; }
            else { break; }
        }
        for (int i=1; i<=curr_target && x+i<rows && y+i<columns; i++) { // Basso dx
            if (board[x+i][y+i] == toCheckState) { aligned++; }
            else { break; }
        }
        if (aligned >= curr_target) { return result; }

        // OBLIQUO DX a SX
        aligned = 1;
        for (int i=1; i<=curr_target && x+i<rows && y-i>=0; i++) { // Alto dx
            if (board[x+i][y-i] == toCheckState) { aligned++; }
            else { break; }
        }
        for (int i=1; i<=curr_target && x-i>=0 && y+i<columns; i++) { // Basso sx
            if (board[x-i][y+i] == toCheckState) { aligned++; }
            else { break; }
        }
        if (aligned >= curr_target) { return result; }

        if (boardSize == rows*columns) {
            return MNKGameState.DRAW;
        }
        else {
            return MNKGameState.OPEN;
        }
    }

    public void print() {
        FileWriter myWriter;

        try {
            myWriter = new FileWriter("filename.txt");
        }
        catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
            return;
        }
        LinkedList<Node> queue = new LinkedList<>();
        queue.addFirst(root);
        int next_len = 0;
        int curr_len = 1;
        int tree_nodes = 0;

        while (queue.size() != 0) {
            Node x = queue.removeLast();
            tree_nodes++;

            for (Node child : x.children) {
                queue.addFirst(child);
                next_len++;
            }

            try {
                myWriter.write(x.action + " " + x.score);
                myWriter.write("\n");
            } catch (IOException e) {
                e.printStackTrace();
            }

            curr_len--;

            if (curr_len <= 0) {
                curr_len = next_len;
                next_len = 0;
                try {
                    myWriter.write("\n\n\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println(tree_nodes);
        try {
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
