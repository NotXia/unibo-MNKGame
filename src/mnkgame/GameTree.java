package mnkgame;
import java.util.HashMap;

import java.util.PriorityQueue;

/**
 *
 * java -cp ".." mnkgame.MNKGame 4 4 4 mnkgame.OurPlayer
 *
 * **/

public class GameTree {
    private Node root;
    private int rows, columns, target;
    private boolean first;
    private final MNKCellState MY_STATE, OPPONENT_STATE;
    private final MNKGameState WIN_STATE, LOSS_STATE;
    private int maxScore, minScore;
    private int MAX_DEPTH;
    private final int MIN_EVAL, SCORE_THRESHOLD;
    private final int WIN_SCORE, LOSS_SCORE, DRAW_SCORE;

    public GameTree(int M, int N, int K, boolean first) {
        this.root = null;
        this.rows = M;
        this.columns = N;
        this.target = K;

        this.first = first;
        this.MY_STATE = first ? MNKCellState.P1 : MNKCellState.P2;
        this.OPPONENT_STATE = first ? MNKCellState.P2 : MNKCellState.P1;
        this.WIN_STATE = first ? MNKGameState.WINP1 : MNKGameState.WINP2;
        this.LOSS_STATE = first ? MNKGameState.WINP2 : MNKGameState.WINP1;

        this.maxScore = M*N;
        this.minScore = -(M*N);

        this.MAX_DEPTH = 6;
        this.MIN_EVAL = 5;
        this.SCORE_THRESHOLD = 2;

        this.WIN_SCORE = target + 1;
        this.LOSS_SCORE = -target - 1;
        this.DRAW_SCORE = 0;
    }

    public boolean isEmpty() {
        return root == null;
    }

    private boolean isValidCell(int x, int y) {
        return (x >= 0 && x < columns) && (y >= 0 && y < rows);
    }

    private int alphabeta(Node node, boolean myNode, int alpha, int beta) {
        if (node.isLeaf()) {
            return node.score;
        }
        else {
            int eval;
            for (Node child : node.children) {
                child.alphabeta = false;
            }

            if (myNode) {
                eval = maxScore+1;

                for (Node child : node.children) {
                    eval = Math.min(eval, alphabeta(child, false, alpha, beta));
                    beta = Math.min(eval, beta);
                    child.alphabeta = true;
                    if (beta <= alpha) { break; }
                }
            }
            else {
                eval = minScore-1;
                for (Node child : node.children) {
                    eval = Math.max(eval, alphabeta(child, true, alpha, beta));
                    alpha = Math.max(eval, alpha);
                    child.alphabeta = true;
                    if (beta <= alpha) { break; }
                }
            }

            node.score = eval;
            return eval;
        }
    }

    /**
     * Restituisce il punteggio di un nodo contenente uno stato di gioco terminale
     * @implNote Costo: O(1)
     * */
    private void setScoreOf(Node node, MNKGameState gameState) {
        if (gameState == WIN_STATE) {
            node.score = WIN_SCORE;
        }
        else if (gameState == LOSS_STATE) {
            node.score = LOSS_SCORE;
        }
        else {
            node.score = DRAW_SCORE;
        }
    }

    private void setHeuristicScoreOf(Node node, BoardStatus board) {
        int playerScore, opponentScore;

        /*board.generateScore();
        playerScore = board.getGlobalBestMovesToWin(MY_STATE);
        opponentScore = board.getGlobalBestMovesToWin(OPPONENT_STATE);*/

        board.removeAt(node.action.j, node.action.i);
        board.generateScoreAt(node.action.j, node.action.i);
        playerScore = board.getMovesToWinAt(node.action.j, node.action.i, MY_STATE);
        opponentScore = board.getMovesToWinAt(node.action.j, node.action.i, OPPONENT_STATE);
        board.setAt(node.action.j, node.action.i, node.action.state);

        playerScore = target - playerScore;
        opponentScore = target - opponentScore;

        if (playerScore > opponentScore) {
            node.score = playerScore;
        }
        else {
            node.score = -opponentScore;
        }
    }

    /**
     * Restituisce una Priority Queue di posizioni da visitare ordinate in base alla funzione euristica.
     * @implNote Costo:
     * */
    /*public PriorityQueue<EvaluationPosition> getInterestingPositions(Node node, BoardStatus board) {
        HashMap<String, Boolean> hasBeenEvaluated = new HashMap<>();
        PriorityQueue<EvaluationPosition> moves = new PriorityQueue<>();

        Node iter = node;
        while (iter != null) {
            MNKCell markedCell = iter.action;
            int x = markedCell.j, y = markedCell.i;

            board.generateScoreAt(x, y);
            for (EvaluationPosition position : board.getIntrestingAdjacencyAt(x, y)) {
                if (hasBeenEvaluated.get(""+position.x+" "+position.y) == null) {
                    moves.add( position );
                    hasBeenEvaluated.put(""+position.x+" "+position.y, true);
                }
            }

            iter = iter.parent;
        }

        return moves;
    }*/
    public PriorityQueue<EvaluationPosition> getInterestingPositions(Node node, BoardStatus board) {
        HashMap<String, Boolean> hasBeenEvaluated = new HashMap<>();
        PriorityQueue<EvaluationPosition> moves = new PriorityQueue<>();

        Node iter = node;
        while (iter != null) {
            MNKCell markedCell = iter.action;

            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i == 0 && j == 0) { continue; }
                    int toVisit_x = markedCell.j + j;
                    int toVisit_y = markedCell.i + i;

                    if (isValidCell(toVisit_x, toVisit_y)) {
                        if (board.isFreeAt(toVisit_x, toVisit_y) && hasBeenEvaluated.get(""+toVisit_x+" "+toVisit_y) == null) {
                            board.generateScoreAt(toVisit_x, toVisit_y);
                            int best = Math.min(board.getMovesToWinAt(toVisit_x, toVisit_y, MY_STATE), board.getMovesToWinAt(toVisit_x, toVisit_y, OPPONENT_STATE));
                            moves.add( new EvaluationPosition(toVisit_x, toVisit_y, best) );
                            hasBeenEvaluated.put(""+toVisit_x+" "+toVisit_y, true);
                        }
                    }
                }
            }
            iter = iter.parent;
        }

        return moves;
    }

    /**
     * Genera l'albero di gioco fino a una determinata profondità
     * @param toEval Nodo radice
     * @param depth Profondità di generazione
     * @param board Mantiene memorizzata la situazione attuale della griglia
     * @implNote Costo:
     * */
    private Node createTree(Node toEval, boolean mePlaying, int depth, BoardStatus board) {
        MNKGameState gameState = board.statusAt(toEval.action.j, toEval.action.i);

        if (gameState != MNKGameState.OPEN) {
            setScoreOf(toEval, gameState);
            toEval.endState = true;
        }
        else if (depth == 0) {
            setHeuristicScoreOf(toEval, board);
        }
        else {
            PriorityQueue<EvaluationPosition> moves = getInterestingPositions(toEval, board);

            int i=0;
            while (moves.size() > 0) {
                if (i >= MIN_EVAL && moves.peek().score > SCORE_THRESHOLD) { break; }

                EvaluationPosition toVisit = moves.poll();
                int toVisit_x = toVisit.x;
                int toVisit_y = toVisit.y;

                MNKCellState state = mePlaying ? MY_STATE : OPPONENT_STATE;

                MNKCell toEvalCell = new MNKCell(toVisit_y, toVisit_x, state);
                Node child = new Node(toEval, toEvalCell);

                board.setAt(toVisit_x, toVisit_y, state);
                toEval.children.add( createTree(child, !mePlaying, depth-1, board) );
                board.removeAt(toVisit_x, toVisit_y);
                i++;
            }
        }

        return toEval;
    }

    /**
     * Genera l'albero di gioco iniziale
     * @implNote Costo:
     * */
    public void generate(MNKCell firstMove) {
        root = new Node(null, firstMove);

        BoardStatus board = new BoardStatus(columns, rows, target, MY_STATE);
        board.setAt(firstMove.j, firstMove.i, firstMove.state);

        createTree(root, !first, MAX_DEPTH, board);
        alphabeta(root, first, LOSS_SCORE, WIN_SCORE);
    }

    /**
     * Estende di una determinata profodità l'albero radicato nel nodo indicato
     * @param node Nodo da estendere
     * @implNote Costo:
     * */
    public void extendNode(Node node, int depth) {
        BoardStatus board = new BoardStatus(columns, rows, target, MY_STATE);
        for (MNKCell cell : node.getMarkedCells()) {
            board.setAt(cell.j, cell.i, cell.state);
        }

        boolean mePlaying = node.action.state == MY_STATE;
        createTree(node, !mePlaying, depth, board);
    }

    /**
     * Estende di un livello di profondità tutte le foglie dell'albero radicato nel nodo indicato
     * @param node Nodo di partenza
     * @implNote Costo:
     * */
    private void extendLeaves(Node node) {
        if (node.isLeaf() && !node.endState) {
            extendNode(node, 1);
        }
        else {
            for (Node child : node.children) {
                extendLeaves(child);
            }
        }
    }

    /**
     * Sposta la radice dell'albero al nodo contenente la mossa corrispondente
     * @param move Mossa dell'avversario
     * @implNote Costo:
     * */
    public void setOpponentMove(MNKCell move) {
        Node bestChild = null;

        for (Node child : root.children) {
            if (child.action.equals(move)) {
                bestChild = child;
                break;
            }
        }

        if (bestChild == null) {
            Node new_root = new Node(this.root, move);
            root.setSelectedChild(new_root);
            this.root = new_root;
            extendNode(this.root, MAX_DEPTH);
            alphabeta(this.root, this.root.action.state==MY_STATE, LOSS_SCORE, WIN_SCORE);
        }
        else {
            root.setSelectedChild(bestChild);
            root = bestChild;
            extendLeaves(root);
            alphabeta(this.root, this.root.action.state==MY_STATE, LOSS_SCORE, WIN_SCORE);
        }

    }

    /**
     * Sposta la radice dell'albero al nodo contenente la mossa migliore
     * @return Mossa da eseguire
     * @implNote Costo:
     * */
    public MNKCell nextMove() {
        Node nextChild = root.children.peek(); /*** TODO RIMETTERE POLL **/

        for (Node child : root.children) {
            //System.out.println(child.action + " " + child.score + " " + child.alphabeta);
            if (child.score > nextChild.score && child.alphabeta) {
                nextChild = child;
            }
        }
        //System.out.println();

        root.setSelectedChild(nextChild);
        root = nextChild;

        if (false) {
            if (root.score == 0) {
                System.out.println(root.action + " | Gita in SVIZZERA " + root.score);
            }
            else if (root.score > 0) {
                System.out.println(root.action + " | Verso la VITTORIA " + root.score);
            }
            else {
                System.out.println(root.action + " | Sulla strada verso la DISFATTA " + root.score);
            }
        }

        extendLeaves(root);
        alphabeta(this.root, this.root.action.state==MY_STATE, LOSS_SCORE, WIN_SCORE);
        return root.action;
    }
}
