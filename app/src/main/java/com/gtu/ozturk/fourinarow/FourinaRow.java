package com.gtu.ozturk.fourinarow;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;


/**
 * Created by Osman Öztürk.
 */

/**
 * Main game class which will control game actions and drawing via drawingThread and
 * AI calculations via aiThread
 */
public class FourinaRow extends SurfaceView implements SurfaceHolder.Callback {
    private Activity gameActivity; //For getting intent extras etc

    //Storing key Tiles inside of class for not recalculating them inside methods
    //They should be defined in surfaceCreatedMethod so they cannot be final
    private int xOffset;
    private int yOffset;
    private int size;
    private int screenWidth;
    private int screenHeight;
    private ArrayList<ArrayList<Cell>> board;
    private DrawingThread drawingThread;
    private Bitmap blueChip;
    private Bitmap greenChip;
    private Bitmap bg;
    private Bitmap portionBg;
    private Bitmap panel;
    private GestureDetector gestureDetector; //for detecting scrolls and play moves

    private int gameOffset;
    private float canvasPosX;
    private float canvasPosY;
    private float scrollX;
    private float scrollY;
    private int cellWidth;
    private int cellHeight;
    private int chipWidth;
    private int chipHeight;
    private int chipToPanelOffsetX;
    private int chipToPanelOffsetY;
    private float boardOriginX;
    private float boardOriginY;
    private float boardWidth;
    private float boardHeight;


    private boolean matched;
    private boolean ended;
    private boolean timeup;
    private boolean destroyed;
    private int AIDepth; //Depth of the Ai negamax tree (intelligence sort of)
    private int playedColumn;
    private int timeLimit;
    private Player currentPlayer;
    private GameMode gameMode;
    private ArrayList<Integer> moveList;
    private ImageButton undoButton;
    private FrameCounter fadeAnimation;
    private CountDownTimer timer;
    private TextView countDown;
    private FourinaRow backup;
    private AIThread aiThread;

    //Most of the variables must be initialized after surface created inside that method instead of constructor
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        gestureDetector = new GestureDetector(getContext(), new ScrollDetector());
        SurfaceView.OnTouchListener touchListener = new SurfaceView.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gestureDetector.onTouchEvent(motionEvent);
            }
        };
        setOnTouchListener(touchListener);

        gameActivity = (Activity) getContext();
        setupGame(gameActivity.getIntent().getIntExtra("size", 5));

        //create bitmap objects
        bg = BitmapFactory.decodeResource(getResources(), R.drawable.bg);
        blueChip = BitmapFactory.decodeResource(getResources(), R.drawable.chip_blue);
        greenChip = BitmapFactory.decodeResource(getResources(), R.drawable.chip_green);
        panel = BitmapFactory.decodeResource(getResources(), R.drawable.panel_beige);
        screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        undoButton = gameActivity.findViewById(R.id.undoButton);
        drawingThread = new DrawingThread(this, surfaceHolder);
        initializeVariables();
        portionBg = Bitmap.createBitmap(bg, 0, 0, screenWidth, gameOffset + yOffset, null, false);
        //Starting game drawing thread
        drawingThread.setRunning(true);
        drawingThread.start();




    }

    /**
     * Executed when game minimized and maximized again and restores the game to previous state
     */
    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.i("TAG", "surfaceChanged: ");

        if (destroyed && backup != null)
            restoreGame(backup);

        destroyed = false;
        backup = null;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        destroyed = false;
        //Trying to destroy repeatedly because the join method can need that when it throws exceptions
        while (!destroyed) {
            try {
                aiThread.setThinking(false);
                drawingThread.setRunning(false);
                drawingThread.join();
                aiThread.join();
            }

            catch (InterruptedException exception) {
                exception.printStackTrace();
            }
            destroyed = true;
            backup = backupGame(this, this.getContext());
        }

    }

    private ArrayList<ArrayList<Cell>> newBoard(int boardSize) {
        ArrayList<ArrayList<Cell>> newBoard;

        newBoard = new ArrayList<>(boardSize);
        for (int i = 0; i < boardSize; i++) {
            newBoard.add(new ArrayList<Cell>(boardSize));
            for (int j = 0; j < boardSize; j++) {
                newBoard.get(i).add(new Cell());
                newBoard.get(i).get(j).setTile(Cell.Tile.EMPTY);
            }
        }
        
        return newBoard;
    }

    /**
     * Backups the game when minimized for restoring again when maximized
     * @param game Game to be backed up
     * @param context context of the current game
     * @return New game object which is contains the values from given game as backup
     */
    public FourinaRow backupGame(FourinaRow game, Context context) {
        FourinaRow backup = new FourinaRow(context);
        backup.size = game.size;
        backup.currentPlayer = game.currentPlayer;
        backup.ended = game.ended;
        backup.timeup = game.timeup;
        
        backup.moveList = new ArrayList<Integer>(game.moveList.size());
        for (int i = 0; i < game.moveList.size(); i++)
            backup.moveList.add(i, game.moveList.get(i));
            
        backup.board = new ArrayList<>(game.size);
        for (int i = 0; i < game.size; i++) {
            backup.board.add(new ArrayList<Cell>(game.size));
            for (int j = 0; j < game.size; j++)
                backup.board.get(i).add(j, game.board.get(i).get(j));
        }
        if (timeLimit > 0)
            timer.cancel(); //timer initializes again after resume

        return backup;
    }

    /**
     * Copies values of the given game's variables for restoring game state before minimizing
     * @param backup The backup game object which should be created by backupGame method when minimized
     */
    public void restoreGame(FourinaRow backup) {
        size = backup.size;
        currentPlayer = backup.currentPlayer;
        board = new ArrayList<>(size);
        ended = backup.ended;
        timeup = backup.timeup;
        for (int i = 0; i < size; i++) {
            board.add(new ArrayList<Cell>(size));
            for (int j = 0; j < size; j++)
                board.get(i).add(backup.board.get(i).get(j));
        }

        moveList = new ArrayList<>(backup.moveList.size());
        for (int i = 0; i < backup.moveList.size(); i++)
            moveList.add(i, backup.moveList.get(i));

    }

    /**
     * initializes some essential variables and makes the game focusable for being able to use listeners
     */
    public FourinaRow(Context context) {
        super(context);
        this.setFocusable(true);
        this.getHolder().addCallback(this);
        board = null;
        backup = null;
    }

    /**
     * initializes some essential variables and makes the game focusable for being able to use listeners
     * Created for being able to add this class to design xml of the activity
     */
    public FourinaRow(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.setFocusable(true);
        this.getHolder().addCallback(this);
        board = null;
        backup = null;
    }

    /**
     * initializes some essential variables and makes the game focusable for being able to use listeners
     * Created for being able to add this class to design xml of the activity
     */
    public FourinaRow(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setFocusable(true);
        this.getHolder().addCallback(this);
        board = null;
        backup = null;
    }

    //Fix this by interfaces
    private ArrayList<ArrayList<Cell>> copyBoard(ArrayList<ArrayList<Cell>> copy) {
        //Copy Game Board
        ArrayList<ArrayList<Cell>> newBoard = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            newBoard.add(new ArrayList<Cell>(size));
            for (int j = 0; j < size; j++) {
                newBoard.get(i).add(j, copy.get(i).get(j));
            }
        }
        return newBoard;
    }

    private void setupGame(int size) {
        //This function is needed because it will be used after surface created for getting size intent extra from previous activity
        //Doing that inside constructor breaks the game because surface wasn't created
        this.size = size;
        //Initializing board with panel image
        board = newBoard(size);

    }

    //initializes the essential values for the game for keeping surfacecreated method clean
    //can also come in handy when restarting the game
    private void initializeVariables() {
        scrollX = 0.0f;
        scrollY = 0.0f;
        canvasPosX = 0.0f;
        canvasPosY = 0.0f;
        gameOffset = screenHeight * 2 / 7;
        cellWidth = panel.getWidth();
        cellHeight = panel.getHeight();
        chipWidth = blueChip.getWidth();
        chipHeight = blueChip.getHeight();
        xOffset = (screenWidth - size * cellWidth) / 2;
        yOffset = (gameOffset - size * cellHeight) / 2;
        chipToPanelOffsetX = (cellWidth - chipWidth) / 2;
        chipToPanelOffsetY = (cellHeight - chipHeight) / 2;
        boardWidth = size * cellWidth;
        boardHeight = size * cellHeight;
        playedColumn = -1;
        currentPlayer = Player.PLAYERONE;
        matched = false;
        AIDepth = gameActivity.getIntent().getIntExtra("aiDepth", 2);
        aiThread = new AIThread(this, AIDepth);
        timeup = false;

        if (gameActivity.getIntent().getBooleanExtra("gameMode", true))
            gameMode = GameMode.PVP;
        else {
            gameMode = GameMode.CVP;
            undoButton.setEnabled(false);
            undoButton.setVisibility(INVISIBLE);
        }
        ended = false;
        moveList = new ArrayList<>(size*size);
        fadeAnimation = new FrameCounter(drawingThread.getFPS(), 4.4f, true, 1);
        timeLimit = gameActivity.getIntent().getIntExtra("timeLimit", -1);
        countDown = gameActivity.findViewById(R.id.countDown);
        if (timeLimit > 0) {
            initializeCountDown(gameActivity, timeLimit*1000);
            timer.start();
        }

        updateGameMessage(gameActivity); //Also initializes

        if (yOffset < 0) {
            yOffset = 0; //for drawing from half
        }

        undoButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                undo();
            }
        });

    }

    /**
     * Main function of the game and rendering operations, called by drawing thread just before draw method for updating the game state
     * @param canvas for updating canvas position
     */
    public void update(Canvas canvas) {


        updateGameMessage(gameActivity);
        scrollUpdate(canvas);


        if (!ended) {
            //PlayGame chechks if a move should be done and plays if so
            playGame();
        }

        else { //executes fade animation of the four in a row when game ended
            if (!fadeAnimation.isRunning())
                fadeAnimation.start();

            fadeAnimation.step();
            //game Ended stopping timer
            if (timeLimit > 0) {
                gameActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run()
                    {
                        //Deleting the countdown text when game ended
                        countDown.setText("");
                    }
                });
                timer.cancel();
            }

        }





    }

    /**
     * Called right after update method and redraws game screen
     * @param canvas For drawing bitmaps over it
     */
    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        //canvas drawing operations

        if (xOffset > 0) {
            canvas.drawBitmap(bg, canvasPosX, canvasPosY, null);
            drawBoard(canvas, fadeAnimation);
        }
        else {
            drawBoard(canvas, fadeAnimation);
            canvas.drawBitmap(portionBg, -canvasPosX, -canvasPosY, null);
        }

    }


    private void drawBoard(Canvas canvas, FrameCounter endAnim) {
        //TODO handle that without conditionals
        Paint fadePaint = null;

        if (endAnim.isRunning()) { //calculates the alpha value of the chips if game ended and animation is running
            //according to animation frames, When the alpha value of the paint is changed chips will be seen as animating
            fadePaint = new Paint();
            int animChange =  endAnim.getCurrentFrame() * 255 / endAnim.getAnimationFrames();
            int anim  = Math.abs(255 - animChange);
            fadePaint.setAlpha(anim);

        }


        Paint tilePaint;

        //canvas.drawBitmap(bg, 0, 0, null);
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {

                if (board.get(i).get(j).getMatch())
                    tilePaint = fadePaint;
                else
                    tilePaint = null;

                canvas.drawBitmap(panel,
                        xOffset + cellWidth*j, gameOffset + yOffset + cellHeight*i, null);

                if (!board.get(i).get(j).isForThinking()) {

                    switch (board.get(i).get(j).getTile()) {
                        case BLUE:
                            canvas.drawBitmap(blueChip, xOffset + cellWidth*j + chipToPanelOffsetX,
                                    gameOffset + yOffset + cellHeight*i + chipToPanelOffsetY, tilePaint);
                            break;
                        case GREEN:
                            canvas.drawBitmap(greenChip, xOffset + cellWidth*j + chipToPanelOffsetX,
                                    gameOffset + yOffset + cellHeight*i + chipToPanelOffsetY, tilePaint);
                            break;
                        case EMPTY:
                            break;

                    }
                }
            }
        }
    }


    /**
     * Will be called insie gestureDetector OnSingleTapConfirmed for determining played column
     * @return Returns zero based coordinate of clicked column
     * returns -1 otherwise
     */
    private int detectPlayedColumn(float touchX) {
        //fix needed for index 0
        int column = (int) (-canvasPosX - xOffset + touchX) / cellWidth;
        if (column >= 0 && column < size) {
            //Tapped within board
            return column;
            //touchX - xOffset - canvasPosX gives board based touch coordinates
        }

        else
            return -1;
    }

    /**
     * Translates the canvas to reverse direction of the scroll for simulating board scrolling
     * And tries to Keep board edges on the screen edge without updating them when they are on the edge
     * and user tries to push the board away from the edge
     * @param canvas canvas to be translated
     */
    private void scrollUpdate(Canvas canvas) {
        if (xOffset < 0) { //makes the boards smaller than the width of the screen unscrollable
            //X bound Check
            if ( ((scrollX > 0 && boardOriginX >= 0) || boardOriginX <= 0) &&
                    (scrollX < 0 || (boardOriginX+boardWidth >= screenWidth))) {
                canvasPosX -= scrollX;
            }

            //y bound check
            if (((boardOriginY <= gameOffset) ||  (scrollY > 0 && boardOriginY >= gameOffset))  &&
                    ((scrollY < 0 && boardOriginY+boardHeight <= screenHeight) ||
                            boardOriginY+boardHeight >= screenHeight)) {
                canvasPosY -= scrollY;
            }

        }
        //Log.i("TAG", "scrollUpdate boardOriginX:" + boardOriginX);
        boardOriginX = canvasPosX+xOffset;
        boardOriginY = gameOffset + canvasPosY + yOffset;
        scrollX = 0.0f;
        scrollY = 0.0f;
        canvas.translate(canvasPosX, canvasPosY);


    }


    private class ScrollDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            //must be implemented for other gestures to work
            return true;
        }


        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float scrolX, float scrolY) {
            //canvas.translate(scrolX, scrolY);
            Log.i("TAG", "onScroll x:" + scrolX + "y:"+ scrolY);
            scrollX = scrolX;
            scrollY = scrolY;
            return true;
        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float vX, float vY) {
            Log.i("TAG", "onFling x:" + vX + "y:" + vY);
            //scrollX = -vX/cellWidth*2;
            //scrollY = -vY/panel.getHeight()*2;
            //can be used for fast scroll
            return true;
        }

        /**
         * Updates the column to be played when game continues goes back to main menu otherwise
         * @return returns true if the event is handled false otherwise
         */
        @Override
        public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
            if (!ended)
                playedColumn = detectPlayedColumn(motionEvent.getX());

            else
                gameActivity.onBackPressed(); //Get back to menu for restarting the game
            Log.i("TAG", "onSingleTapConfirmed: " + playedColumn);
            return true;
        }



    }
    private enum Player {PLAYERONE, PLAYERTWO, COMPUTER}
    private enum GameMode {PVP, CVP}

    private void changePlayer() {
        if (currentPlayer == Player.PLAYERONE)
            if (gameMode == GameMode.PVP)
                currentPlayer = Player.PLAYERTWO;
            else
                currentPlayer = Player.COMPUTER;

        else
            currentPlayer = Player.PLAYERONE;


    }

    //returns 0 if the column is full can be used for that
    private int getEmptyCount(int column) {
        for (int i = 0; i < size; i++) {
            if (board.get(i).get(column).getTile() != Cell.Tile.EMPTY)
                return i;
        }

        return size;
    }

    private boolean isGameDraw() {
        for (int i = 0; i < size; i++)
            if (getEmptyCount(i) != 0)
                return false;

        return !matched;

    }

    private boolean isGameEnded(int column) {
        return ((fourMatchCheck(column) == 4) || isGameDraw());
    }

    private void play(int column) {
        if (currentPlayer == Player.PLAYERONE) {
            if (aiThread.isThinking())
                board.get(getEmptyCount(column)-1).get(column).setForThinking(true);
            else
                board.get(getEmptyCount(column)-1).get(column).setForThinking(false);
            board.get(getEmptyCount(column)-1).get(column).setTile(Cell.Tile.BLUE);

        }

        else {
            if (aiThread.isThinking())
                board.get(getEmptyCount(column)-1).get(column).setForThinking(true);
            else
                board.get(getEmptyCount(column)-1).get(column).setForThinking(false);

            board.get(getEmptyCount(column)-1).get(column).setTile(Cell.Tile.GREEN);

        }

        moveList.add(column);
        if (timeLimit > 0 && !aiThread.isThinking())
            timer.start();
    }

    /**
     * Main game mechanic goes on inside this function
     */
    private void playGame() {
        //TODO gamemodeCheck
        if (currentPlayer != Player.COMPUTER) {//Playing portion of the real players
            if (!aiThread.isThinking()) {

                if (timeup)  { //will be true when timer.onFinish called
                    playedColumn = generateRandomMove();
                    timeup = false;
                }

                if  (playedColumn >= 0 && getEmptyCount(playedColumn) != 0) {
                    play(playedColumn); //won't work on first loop because column will be negative
                    ended = isGameEnded(playedColumn); //Is game ended checks for four math
                    if(!ended)
                        changePlayer();

                    playedColumn = -1;
                }
            }
        }


        else { //Playing portion of the computer
            //Main thread will run with consistent FPS and wait for AI thread to finish calculations
            //For prevent lagging with that construction
            if (!aiThread.isThinking()) {
                try {
                    aiThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                aiThread = new AIThread(this, AIDepth);
                aiThread.setThinking(true);
                aiThread.start();
                Log.i("TAG", "playGame: Started AI thread");
            }

            if (aiThread.isDecided()) {
                aiThread.setThinking(false);
                Log.i("TAG", "Play game called Ai " + aiThread.getColumn());
                play(aiThread.getColumn());
                aiThread.setDecided(false);
                ended = isGameEnded(aiThread.getColumn()); //Is game ended checks for four math
                if(!ended) {
                    changePlayer();
                    Log.i("TAG", "playGame: Changed player after AI decided");

                }
            }


        }

    }

    //Game message wchich indicates current player and ai state
    private void updateGameMessage(Activity activity) {
        final TextView gameMessage = activity.findViewById(R.id.gameMessage);
        final String st;

        if (aiThread.isThinking())
            st = String.format(Locale.getDefault(), "%s", getResources().getString(R.string.thinking));

        else if (!ended) {
            switch (currentPlayer) {
                case PLAYERONE:
                    st = String.format(Locale.getDefault(),"%s", getResources().getString(R.string.current_player_one));
                    break;
                case PLAYERTWO:
                    st = String.format(Locale.getDefault(),"%s", getResources().getString(R.string.current_player_two));
                    break;
                case COMPUTER:
                    st = String.format(Locale.getDefault(),"%s", getResources().getString(R.string.current_player_computer));
                    break;
                default:
                    st = String.format(Locale.getDefault(),"%s %s", getResources().getString(R.string.current_player_text), currentPlayer);
                    break;

            }
        }

        else if (!isGameDraw()) {
            switch (currentPlayer) {
                case PLAYERONE:
                    st = String.format(Locale.getDefault(),"%s", getResources().getString(R.string.player_one_game));
                    break;
                case PLAYERTWO:
                    st = String.format(Locale.getDefault(),"%s", getResources().getString(R.string.player_two_game));
                    break;
                case COMPUTER:
                    st = String.format(Locale.getDefault(),"%s", getResources().getString(R.string.computer_game));
                    break;
                default:
                    st = String.format(Locale.getDefault(),"%s %s", getResources().getString(R.string.current_player_text), currentPlayer);
                    break;
            }
        }
        else

            st = String.format(Locale.getDefault(),"%s", getResources().getString(R.string.draw_game));




        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                gameMessage.setText(st);
            }
        });

    }

    //Four in a rowCheck functions
    private int horizontalCheck(int position, int yCoordinate) {
        Cell.Tile matchingPlayer = Cell.Tile.EMPTY;
        int matchCount = 0;
        int maxMatchCount = 0;
        for (int i = position - 3; i < position + 4; i++) {
            if (i >= 0 && i < size) { //Horizontal check;
                if (matchingPlayer != board.get(yCoordinate).get(i).getTile()) {
                    //since matchingPlayer is empty at start if
                    //finds a tile which is not empty it will count it as one tile match
                    matchCount = 1;
                    matchingPlayer = board.get(yCoordinate).get(i).getTile();
                }
                else if (board.get(yCoordinate).get(i).getTile() != Cell.Tile.EMPTY) {
                    matchCount++;
                    if (matchCount >= maxMatchCount)
                        maxMatchCount = matchCount;
                    //if current match is bigger than the others making it maximum match
                    //it can find various matches inside of the same line

                    if (matchCount == 4) {//if full match detected marking the tiles inside of
                        //that match for printing them lowercase
                        for (int j = 0; j < matchCount; j++)
                            board.get(yCoordinate).get(i - j).setMatch(true);
                        break;

                    }
                }

            }
        }
        return maxMatchCount;
    }

    private int verticalCheck(int position, int yCoordinate) {
        Cell.Tile matchingPlayer = Cell.Tile.EMPTY;
        int matchCount = 0;
        int maxMatchCount = 0;

        for (int i = yCoordinate - 3; i < yCoordinate + 4; i++) { //checking tiles around given tile
            //because given coordinate can be corresponding to last or first tile of the match
            if (i >= 0 && i < size) { //vertical check;
                if (matchingPlayer != board.get(i).get(position).getTile() ||
                        board.get(i).get(position).getTile() == Cell.Tile.EMPTY) {
                    matchCount = 1;
                    matchingPlayer = board.get(i).get(position).getTile();
                }
                else if (board.get(i).get(position).getTile() != Cell.Tile.EMPTY) {
                    matchCount++;
                    if (matchCount >= maxMatchCount)
                        maxMatchCount = matchCount;
                    if (matchCount == 4) {
                        for (int j = 0; j < matchCount; j++)
                            board.get(i - j).get(position).setMatch(true);
                        break;

                    }
                }

            }
        }
        return maxMatchCount; //for AI
    }

    private int leftToRightDiagonalCheck(int position, int yCoordinate) {
        Cell.Tile matchingPlayer = Cell.Tile.EMPTY;
        int matchCount = 0;
        int maxMatchCount = 0;

        for (int i = yCoordinate - 3, j = position - 3; i < yCoordinate + 4; i++, j++) { // left to right
            if (i >= 0 && i < size && j >= 0 && j < size) {
                if (matchingPlayer != board.get(i).get(j).getTile()) {
                    matchCount = 1;
                    matchingPlayer = board.get(i).get(j).getTile();
                }
                else if (board.get(i).get(j).getTile() != Cell.Tile.EMPTY) {
                    matchCount++;
                    if (matchCount >= maxMatchCount)
                        maxMatchCount = matchCount;
                    if (matchCount == 4) {
                        for (int k = 0; k < matchCount; k++)
                            board.get(i - k).get(j - k).setMatch(true);
                        break;

                    }
                }

            }
        }
        return maxMatchCount; //Not full 4 match occured will use it on AI actions
    }


    private int rightToLeftDiagonalCheck(int position, int yCoordinate) {
        Cell.Tile matchingPlayer = Cell.Tile.EMPTY;
        int matchCount = 0;
        int maxMatchCount = 0;

        for (int i = yCoordinate - 3, j = position + 3; i < yCoordinate + 4; j--, i++) { //rigth to left
            if (i >= 0 && i < size && j >= 0 && j < size) {
                if (matchingPlayer != board.get(i).get(j).getTile()) {
                    matchCount = 1;
                    matchingPlayer = board.get(i).get(j).getTile();
                }
                else if (board.get(i).get(j).getTile() != Cell.Tile.EMPTY) {
                    matchCount++;
                    if (matchCount >= maxMatchCount)
                        maxMatchCount = matchCount;

                    if (matchCount == 4) {
                        for (int k = 0; k < matchCount; k++)
                            board.get(i - k).get(j + k).setMatch(true); //used plus because it is checking right to left
                        break;

                    }
                }

            }
        }
        return maxMatchCount; //Not full 4 match occured will use it on AI actions
    }

    //Checks around last played position for four in a rows
    private int fourMatchCheck(int playedColumn) {
        //Returns -1 on unneccessary checks (not played since last check))

        if (playedColumn < 0)
            return -1;

        else {
            int yCoordinate = getEmptyCount(playedColumn);
            if (yCoordinate == size) //empty column
                return  -1;

            int[] match = new int[4];
            int max = 0;

            match[0] = horizontalCheck(playedColumn, yCoordinate);
            match[1] = verticalCheck(playedColumn, yCoordinate);
            match[2] = leftToRightDiagonalCheck(playedColumn, yCoordinate);
            match[3] = rightToLeftDiagonalCheck(playedColumn, yCoordinate);

            for (int i = 0; i < 4; i++)
                if (match[i] > max)
                    max = match[i];

            if (max == 4) //for draw and win condition on last move fix
                matched = true;

            return max;
        }

    }

    /**
     * Undoes last move, will be called from a button
     * it also reset cell states in case of undo from ended game
     * Also resets essential game variables
     */
    public void undo() {
        if (moveList.size() > 0) {
            int column = moveList.get(moveList.size()-1);
            int yCoordinate = getEmptyCount(column);
            if (yCoordinate == size)
                yCoordinate--;
            //calculates exact cell position from given x Coordinate

            board.get(yCoordinate).get(column).setTile(Cell.Tile.EMPTY);
            moveList.remove(moveList.size()-1);
            if (!ended)
                changePlayer();

            //resets essential game variables
            ended = false;
            matched = false;
            resetCellProperties();
            fadeAnimation.stop();
            if (timeLimit > 0)
                timer.start();
            //checking for matches and game status every time can affect performance
            //resetting them every time is a better practice
        }
    }

    private void resetCellProperties() {
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++) {

                board.get(i).get(j).setMatch(false);
                //board.get(i).get(j).setForThinking(false);
                //Can be used for showing temporary moves of AI
            }
    }


    /**
     * Creates the countDown timer for timed game mode
     * Also sets text update properties of countdown
     * @param miliSeconds Starting value of the countdown with miliseconds unit
     */
    private void initializeCountDown(Activity activity, long miliSeconds) {
        if (miliSeconds > 0) {
            timer = new CountDownTimer(miliSeconds, 250) {
                @Override
                public void onTick(final long millisUntilFinished) {

                    gameActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!aiThread.isThinking())
                                countDown.setText(String.format(Locale.getDefault(),"%s%s%d",
                                        getResources().getString(R.string.timer), " ", millisUntilFinished/1000));

                            else {
                                countDown.setText("");
                                timer.cancel();
                            }

                        }
                    });

                }


                @Override
                public void onFinish() {
                    if (!ended && !aiThread.isThinking())
                        timeup = true;

                }
            };

        }

        else {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    countDown.setText("");
                }
            });
        }
    }

    //will be used when timer runs out for determining a random move to be played
    private int generateRandomMove() {
        Random rand = new Random();
        int column;
        do {
            column = rand.nextInt(size);

        } while (getEmptyCount(column) == 0);

        return column;
    }

    //Simpler variant of minimax AI algorithm with recursive structure
    //based on the fact that negative of the maximum board value of enemy board is minimum for me
    //Simplifies branch returning process of minimax
    private int negamax(int depth, int position) {
        if (!aiThread.isThinking()) //cannot be ran when thread is stopped
            return 0;

        play(position);
        changePlayer(); //undo changes player but play doesn't it is done by playGame
        if (isGameDraw()) {
            undo();
            return 0;
        }

        else if (isGameEnded(position)) {
            undo();
            return Integer.MAX_VALUE;
        }

        if (depth == 0) {
            int score = heuristic();
            undo();
            //changePlayer();
            return score;
        }


        int bestValue = Integer.MIN_VALUE; //initializing with minimum value


        //Branching the minimax on  all columns
        for (int i = 0; i < size; i++) {
            if (getEmptyCount(i) != 0) { //playable
                //value, position
                int value =  -negamax(depth-1, i);
                if (value > bestValue)
                    bestValue = value;
            }
        }
        undo();
        return  bestValue;
    }

    public int aiPlay(int depth) {
        if (!aiThread.isThinking()) //For preventing from continuing to calculate move after surfaceDestroyed
            return 0;

        int best = Integer.MIN_VALUE;
        int result;
        int position = 0;

        //Starts negamax tree as a helper function and returns best column value to be played on
        for (int i = 0; i < size; i++) {
            if (getEmptyCount(i) != 0) {
                result = negamax(depth, i);
                if (result > best) {
                    best = result;
                    position = i;
                }

            }

        }
        return position;
    }


    private int heuristic() {
        Cell.Tile enemyTile;
        Cell.Tile currentTile;

        if (currentPlayer == Player.PLAYERONE) {
            enemyTile = Cell.Tile.GREEN;
            currentTile = Cell.Tile.BLUE;

        }
        else {
            enemyTile = Cell.Tile.BLUE;
            currentTile = Cell.Tile.GREEN;

        }

        int score = 0;

        final int[] weights = new int[]{1, 15, 200, 600000}; //4 streak values won't be used since that board will return immediately
        final int[] enemyWeights = new int[]{1, 15, 350, 600000};

        //calculates board heuristic with counting  1 2 3 and 4 in a rows for itself and enemy
        //uses them with corresponding weights to calculate board heuristic afterwards to be used inside negamax
        int[] streakHorizontal = streakHorizontal(enemyTile);
        int[] streakVertical = streakVertical(enemyTile);
        int[] streakLeftToRightDiagonal = streakLeftToRightDiagonal(enemyTile);
        int[] streakRightToLeftDiagonal = streakRightToLeftDiagonal(enemyTile);

        for (int i = 0; i < 4; i++) {
            score += weights[i]*streakHorizontal[i];
            score += weights[i]*streakVertical[i];
            score += weights[i]*streakLeftToRightDiagonal[i];
            score += weights[i]*streakRightToLeftDiagonal[i];
        }

        streakHorizontal = streakHorizontal(currentTile);
        streakVertical = streakVertical(currentTile);
        streakLeftToRightDiagonal = streakLeftToRightDiagonal(currentTile);
        streakRightToLeftDiagonal = streakRightToLeftDiagonal(currentTile);

        for (int i = 0; i < 4; i++) {
            score -= enemyWeights[i]*streakHorizontal[i];
            score -= enemyWeights[i]*streakVertical[i];
            score -= enemyWeights[i]*streakLeftToRightDiagonal[i];
            score -= enemyWeights[i]*streakRightToLeftDiagonal[i];
        }

        //int result = new Random().nextInt(size);
        Log.i("TAG", "heuristic: " + score);
        return score;

    }

    //Streak functions are calculating streaks within one direction which are not seperated by enemy tiles
    //Empty cells are not affecting the streak
    //they will be used for heuristic calculation
    private int[] streakHorizontal(Cell.Tile enemyTile) {
        int[] streaks = new int[4]; // one two three (check)
        int streakCounter = 0;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                    if (board.get(i).get(j).getTile() != enemyTile) {//Empties not breaking the streak
                        if (board.get(i).get(j).getTile() != Cell.Tile.EMPTY)
                            streakCounter++;
                    }

                    else
                        streakCounter=0;


            }
            if (streakCounter >= 1 && streakCounter <=4)
                streaks[streakCounter-1]++;

            streakCounter=0;

        }
        return streaks;
    }
    private int[] streakVertical(Cell.Tile enemyTile) {
        int[] streaks = new int[4]; // one two three (check)
        int streakCounter = 0;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board.get(j).get(i).getTile() != enemyTile) {//Empties not breaking the streak
                    if (board.get(j).get(i).getTile() != Cell.Tile.EMPTY)
                        streakCounter++;
                }

                else
                    streakCounter=0;


            }
            if (streakCounter >= 1 && streakCounter <=4)
                streaks[streakCounter-1]++;

            streakCounter=0;

        }
        return streaks;
    }
    private int[] streakLeftToRightDiagonal(Cell.Tile enemyTile) {
        int[] streaks = new int[4]; // one two three (check)
        int streakCounter = 0;

        for (int i = 0; i < size; i++) {
            for (int j = i, k = 0; j < size && k < size; j++, k++) {
                if (board.get(j).get(k).getTile() != enemyTile) {//Empties not breaking the streak
                    if (board.get(j).get(k).getTile() != Cell.Tile.EMPTY)
                        streakCounter++;
                }
                else
                    streakCounter=0;

            }
            if (streakCounter >= 1 && streakCounter <=4)
                streaks[streakCounter-1]++;

            streakCounter=0;

            for (int j = 0, k = i; j < size && k < size; j++, k++) {
                if (board.get(j).get(k).getTile() != enemyTile) {//Empties not breaking the streak
                    if (board.get(j).get(k).getTile() != Cell.Tile.EMPTY)
                        streakCounter++;
                }
                else
                    streakCounter=0;



            }
            if (streakCounter >= 1 && streakCounter <=4)
                streaks[streakCounter-1]++;

            streakCounter=0;
        }
        return streaks;


    }
    private int[] streakRightToLeftDiagonal(Cell.Tile enemyTile) {
        int[] streaks = new int[4]; // one two three (check)
        int streakCounter = 0;

        for (int i = 0; i < size; i++) {
            for (int j = i, k = size-1; j < size && k >= 0; j++, k--) {
                if (board.get(j).get(k).getTile() != enemyTile) {//Empties not breaking the streak
                    if (board.get(j).get(k).getTile() != Cell.Tile.EMPTY)
                        streakCounter++;
                }
                else
                    streakCounter=0;



            }
            if (streakCounter >= 1 && streakCounter <=4)
                streaks[streakCounter-1]++;

            streakCounter=0;

            for (int j = 0, k = i; j < size && k >= 0; j++, k--) {
                if (board.get(j).get(k).getTile() != enemyTile) {//Empties not breaking the streak
                    if (board.get(j).get(k).getTile() != Cell.Tile.EMPTY)
                        streakCounter++;
                }
                else
                    streakCounter=0;


            }
            if (streakCounter >= 1 && streakCounter <=4)
                streaks[streakCounter-1]++;

            streakCounter=0;

        }
        return streaks;
    }


}
