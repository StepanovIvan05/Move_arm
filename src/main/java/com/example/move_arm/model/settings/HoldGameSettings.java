package com.example.move_arm.model.settings;

public class HoldGameSettings extends BaseSettings{

    private int holdTimeMs = 500;
    private int seed = 0;

    public int getHoldTimeMs() { return holdTimeMs; }
    public void setHoldTimeMs(int holdTimeMs) { this.holdTimeMs = holdTimeMs; }
    
    public int getSeed(){return this.seed;}
    public void setSeed(int seed) {this.seed = seed;}
}
