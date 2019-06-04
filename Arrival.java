package com.projects.nikita.mysubwaytracker;

import java.util.Date;

public class Arrival {
    private String arrival_to;
    private long minutes;
    private long seconds;

    public Arrival(String to, long min, long sec){
        this.arrival_to = to;
        this.minutes = min;
        this.seconds = sec;

        if(sec > 30){
            this.minutes++;
        }
    }

    public Arrival(String to, String time){
        long minutes = new Date().getTime();

    }

    public long getMinutes() {
        return minutes;
    }
}
