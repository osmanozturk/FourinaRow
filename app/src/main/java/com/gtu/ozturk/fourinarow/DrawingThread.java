package com.gtu.ozturk.fourinarow;

import android.graphics.Canvas;
import android.os.Build;
import android.view.SurfaceHolder;

/**
 * Created by Osman Öztürk
 */

public class DrawingThread extends Thread {
    //try not to recreate canvas
    private boolean running;
    private Canvas canvas;
    private FourinaRow surface;
    private SurfaceHolder surfaceHolder;
    private final int FPS = 60;

    /**
     * Controls game update and drawing operations and executes them periodically according to FPS
     * @param surface Main game class which is a SurfaceView and Operations will be done by calling
     *                its update and draw functions.
     * @param surfaceHolder For controlling surfaceView Canvas and adding listeners
     */
    public DrawingThread(FourinaRow surface, SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
        this.surface = surface;
    }

    /**
     * Updates canvas and redraws it when trying to get consistent FPS
     */
    @Override
    public void run() {
        long start = System.nanoTime();
        final long redrawInterval = 1000/FPS; //Converting to ms

        while(running)  {
            canvas = null;
            try {
                // Locking the canvas for drawing periodically according to FPS
                if (Build.VERSION.SDK_INT >= 26)
                    canvas = surfaceHolder.lockHardwareCanvas();

                else
                    canvas = surfaceHolder.lockCanvas();
                //Canvas can be used hardware accelerated with lockHardwareCanvas but requires SDK 26
                // Preventing from other threads to change canvas
                synchronized (canvas)  {
                    surface.update(canvas); //Seperated render as two methods for clearity
                    surface.draw(canvas);   //calls main game class's methods for game operations to be done
                }
            }

            catch(Exception e)  {
                // If thread is busy and canvas cannot be locked do nothing
            }

            finally {
                if(canvas != null)  {
                    // Updating canvas after performed necessary operations over it
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }

            long current = System.nanoTime(); //more accurate than currentTimeMilis
            // Interval to redraw game
            // (Changing nanoseconds to milliseconds)
            long passedTime = (current - start)/1000000; //converting passed time to miliseconds
            if(passedTime < redrawInterval)  {
                passedTime = redrawInterval; // Millisecond
            }

            try {
                //if operations completed too fast wait for consistent FPS
                sleep(passedTime);
            }
            catch(InterruptedException e)  {
                //When thread is busy do nothing until next cycle
            }
            start = System.nanoTime();
        }
    }

    /**
     * For controlling thread loop
     * @param running Thread state, giving false stops thread operations
     */
    public void setRunning(boolean running)  {
        this.running = running;
    }

    /**
     * @return Returns target FPS of the Game
     */
    public int getFPS() {return FPS;}



}

