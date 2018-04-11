package com.gtu.ozturk.fourinarow;

import android.graphics.drawable.Animatable;

/**
 * Created by Osman Öztürk
 */

public class FrameCounter implements Animatable {
    private boolean isRunning;
    private int animationFrames;
    private float currentFrame;
    private float speed;
    private boolean reverseLoop; //Animation will loop in reverse direction when it is completed
    private int startFrame;

    /**
     * Provides a class for executing loop animations within periodically updating games with constant FPS
     * For controlling animations within it's update thread
     * @param frames Frame amount of the animation
     * @param frameSpeed Speed of frame change (Can be considered as reverse of frame delay)
     * @param reverseLoop Animation will loop with reverse frame order if it is true, loop normally otherwise
     * @param startFrame Starting frame of the animation useful with sprite sheets
     */
    FrameCounter(int frames, float frameSpeed, boolean reverseLoop, int startFrame) {
        animationFrames = frames;
        speed = frameSpeed;
        this.startFrame = startFrame;
        currentFrame = startFrame;
        isRunning = false;
        this.reverseLoop = reverseLoop;
    }

    public int getCurrentFrame() {return (int)currentFrame;}

    /**
     * @return Length of the animation with frame units
     */
    public int getAnimationFrames() {return animationFrames;}

    /**
     * Plays animation and increases it's frame according to given speed
     * Should be called inside game update loop
     */
    public void step() {
        if (isRunning) {
            currentFrame += speed;
            if (currentFrame >= animationFrames || (currentFrame <= startFrame && speed < startFrame)) {
                if (reverseLoop)
                    speed *= -1;
                else
                    currentFrame = startFrame;
            }


        }
    }

    /**
     * Starts the drawable's animation.
     */
    @Override
    public void start() {
        isRunning = true;
        currentFrame = 0.0f;

    }

    /**
     * Stops the drawable's animation.
     */
    @Override
    public void stop() {
        isRunning = false;
    }

    /**
     * Indicates whether the animation is running.
     *
     * @return True if the animation is running, false otherwise.
     */
    @Override
    public boolean isRunning() {
        return isRunning;
    }


}
