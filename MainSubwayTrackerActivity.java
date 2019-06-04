package com.projects.nikita.mysubwaytracker;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Public class represents a single Activity
 *  In this application it is one and only activity
 *  Usage: Displays the time until trains to both directions arrive on
 *  Community College orange line station.
 *  */
public class MainSubwayTrackerActivity extends AppCompatActivity {
    public boolean isBusy;
    public boolean stop;

    Button btn_start;
    Button btn_stop;
    Button btn_exit;

    private Handler handler;

    public MainSubwayTrackerActivity getContext(){
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_subway_tracker);

        isBusy = false;
        stop = false;

        handler = new Handler();
        startHandler();

        btn_start = findViewById(R.id.btn_start);
        btn_stop = findViewById(R.id.btn_stop);
        btn_exit = findViewById(R.id.btn_exit);

        btn_start.setEnabled(false);
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop = false;
                btn_start.setEnabled(false);
                btn_stop.setEnabled(true);
                startHandler();
            }
        });

        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop = true;
                isBusy = false;
                btn_stop.setEnabled(false);
                btn_start.setEnabled(true);

            }
        });

        /* Exit button to go back to the home screen */
        btn_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory( Intent.CATEGORY_HOME );
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(homeIntent);
            }
        });

        // Starting a thread to update time on the screen using custom class CurrentTime
        Runnable runnableTime = new CurrentTime();
        Thread threadTime = new Thread(runnableTime);
        threadTime.start();

        new FetchItemsTask(getContext()).execute();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    public void startHandler()
    {
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                if(!isBusy) callAsyncTask();
                if(!stop) startHandler();
            }
        }, 15000);
    }

    private void callAsyncTask()
    {
        new FetchItemsTask(getContext()).execute();
    }

    /* public method updateTime() uses Calendar object to get current time in the Eastern US */
    public void updateTime() {
        runOnUiThread(new Runnable() {
            public void run() {
                try{
                    TextView mCurrentTime = findViewById(R.id.current_time);
                    Calendar mCalendar = Calendar.getInstance();
                    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss", Locale.US);
                    String currentTime = "Current time: " + formatter.format(mCalendar.getTime());
                    mCurrentTime.setText(currentTime);
                }catch (Exception e) {
                    Log.e("THREAD", e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    /** Inner private class which implements Runnable
     *  Used to update time on the screen in the Thread different from the UIThread
     *  */
    private class CurrentTime implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    updateTime();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    Log.e("THREAD", e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /** Inner private static class extends AsyncTask to fetch data from MBTA.
     *
     *  It is important to note that class is static in order to prevent memory leaks.
     *  This leaks may occur in non-static class because Task will not be destroyed
     *  when the Activity is destroyed.
     *
     *  Context of the MainSubwayTrackerActivity is passed as the only argument in the constructor
     *  for this inner class. That is done in order to give this class access to the views of the
     *  Activity without any memory leaks.
     *  */
    private static class FetchItemsTask extends AsyncTask<Void,Void,Date[][]> {
        private WeakReference<MainSubwayTrackerActivity> weakActivityRef;

        FetchItemsTask(MainSubwayTrackerActivity context){
            weakActivityRef = new WeakReference<>(context);
        }

        @Override
        protected Date[][] doInBackground(Void... params) {
            MainSubwayTrackerActivity activity = weakActivityRef.get();
            activity.isBusy = true;
            try {
                return new MBTAFetcher().fetchForestHillsArrivals();
            }catch (ParseException ex){
                Log.e("ACTIVITY", ex.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Date[][] trainArrivalTimesData) {
            MainSubwayTrackerActivity activity = weakActivityRef.get();
            activity.isBusy = false;
            if(activity.isFinishing()) return;

            if(trainArrivalTimesData == null || trainArrivalTimesData.length != 2){
                Log.e("ERROR", trainArrivalTimesData.length + " ");
                throw new IllegalArgumentException("Invalid number of arguments");
            }

            TextView nextForestHillsArrival = activity.findViewById(R.id.forest_hills_first_time);
            TextView secondForestHillsArrival = activity.findViewById(R.id.forest_hills_second_time);
            TextView nextOakGroveArrival = activity.findViewById(R.id.oak_grove_first_time);
            TextView secondOakGroveArrival = activity.findViewById(R.id.oak_grove_second_time);

            long[] arrivalMinutes = getMinutesUntilArrival(trainArrivalTimesData[0][0], trainArrivalTimesData[1][0],
                    trainArrivalTimesData[0][1], trainArrivalTimesData[1][1]);

            String[] textMinutes = semanticAnalysis(arrivalMinutes);

            nextForestHillsArrival.setText(textMinutes[0]);
            secondForestHillsArrival.setText(textMinutes[1]);
            nextOakGroveArrival.setText(textMinutes[2]);
            secondOakGroveArrival.setText(textMinutes[3]);
        }

        private long[] getMinutesUntilArrival(Date time1, Date time2, Date time3, Date time4) {
            MainSubwayTrackerActivity activity = weakActivityRef.get();
            if (activity == null || activity.isFinishing())
                throw new NullPointerException("Null activity");

            Date current = new Date();
            Log.d("no tag", current.toString());

            return new long[]{
                    getDifferenceInTimes(current, time1),
                    getDifferenceInTimes(current, time2),
                    getDifferenceInTimes(current, time3),
                    getDifferenceInTimes(current, time4)
            };
        }

        private long getDifferenceInTimes(Date current, Date other){
            long diff = other.getTime() - current.getTime();
            long diffMinutes = diff / (60 * 1000) % 60;
            long diffSeconds = diff / (60 * 60 * 1000);
            Log.d("diff", diffMinutes + " minutes");
            Log.d("diff", diffSeconds + " seconds");

            return diffMinutes;
        }

        private String[] semanticAnalysis(long[] minutes){
            String[] meaningfulMinutes = new String[4];
            for(int i = 0; i < minutes.length; i++){
                if(minutes[i] >= 1){
                    meaningfulMinutes[i] = " " + minutes[i] + " minutes";
                }else if((minutes[i] < 1) && (minutes[i] >= 0)){
                    meaningfulMinutes[i] = " BRD";
                }else{
                    meaningfulMinutes[i] = " BRD";
                }
            }
            return meaningfulMinutes;
        }
    }
}

