package com.gtu.ozturk.fourinarow;

/**
 * Created by Osman Öztürk
 */

/**
 * Represents every cell ın the boad, keeps track of the matching and ai status of itself
 * If it is played by AI for calculations it wont be displayed according to that value.
 * And match value can be used for four in a row animation
 */
public class Cell {
    /**
     * Tile types of a cell
     */
    public enum Tile {EMPTY, BLUE, GREEN}
    private Tile tile;
    private boolean inMatch;
    private boolean forThinking;


    /**
     * Initializes an empty cell
     */
    public Cell() {
        tile = Tile.EMPTY;
        inMatch = false;
        forThinking = false;

    }

    /**
     * @return Type of the cell
     */
    public Tile getTile() {return tile;}
    public void setTile(Tile tile) {
        this.tile = tile;
    }
    public void setMatch(boolean isMatching) {inMatch = isMatching;}
    //package private
    public boolean getMatch() {return inMatch;}

    /**
     * Sets thinking parameter. If it is played during thinking period of AI it will be set to true
     * for not displaying that temporary moves
     * @param forThinking new value of the thinking status.
     */
    public void setForThinking(boolean forThinking) {
        this.forThinking = forThinking;
    }

    public boolean isForThinking() {
        return forThinking;
    }

}
