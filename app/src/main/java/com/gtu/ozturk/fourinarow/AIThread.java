package com.gtu.ozturk.fourinarow;

/**
 * Created by Osman Öztürk
 */


/**
 * For preventing from AI calculations to lag main drawingThread
 * Since Recursive implementation of Negamax AI takes too long on high depth executing these calculations
 * on another thread and updating the main thread normally in that period until calculations to be done
 * is essential for other game mechanics to work normally such as scrolling
 */
public class AIThread extends Thread {
    private boolean thinking;
    private FourinaRow game;
    private final int DEPTH;
    private int column;
    private boolean decided;

    /**
     * Since AI does moves and undoes them rapidly for heuristic calculations providing an game object is neccessary
     * @param game Four in a row object for AI operations to be done over it
     * @param depth Maximum depth of the AI negamax tree
     */
    AIThread(FourinaRow game, int depth) {
        this.game = game;
        DEPTH = depth;
        column = -1;
        decided = false;
    }

    /**
     * Thinking value can be set to true for starting AI calculations when it is Computer's turn
     */
    @Override
    public void run() {
        if (thinking) {
            decided = false;
            column = game.aiPlay(DEPTH);
            decided = true;

        }

    }

    public void setThinking(boolean thinking) {this.thinking = thinking;}
    public boolean isThinking() {return thinking;}
    public int getColumn() {return column;} //Play operation of the game will get the column which AI decided to play
    public boolean isDecided() {return  decided;}
    public void setDecided(boolean isDecided) {this.decided = isDecided;}
 }
