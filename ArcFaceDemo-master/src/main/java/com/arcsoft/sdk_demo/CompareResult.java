package com.arcsoft.sdk_demo;

public class CompareResult {
    private String name;
    private float score;

    public CompareResult(String name, float score) {
        this.name = name;
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public float getScore() {
        return score;
    }

    @Override
    public String toString() {
        return "CompareResult{" +
                "name='" + name + '\'' +
                ", score=" + score +
                '}';
    }
}
