package com.example.vmac.WatBot;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

public class MenuJobService extends JobService {
    private boolean jobCanceled = false;


    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        doBackgroundTask();
        return false;
    }

    private void doBackgroundTask() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i("VOLLEY", "time to eat");

            }
        }).start();
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
