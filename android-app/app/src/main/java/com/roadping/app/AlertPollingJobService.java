package com.roadping.app;

import android.app.job.JobParameters;
import android.app.job.JobService;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlertPollingJobService extends JobService {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override public boolean onStartJob(JobParameters params) {
        executor.execute(() -> {
            SessionManager session = new SessionManager(this);
            if (!session.isSignedIn()) { jobFinished(params, false); return; }
            try {
                List<Models.Alert> alerts = new SupabaseApi().alerts(session.accessToken());
                long lastCheck = session.lastAlertCheck();
                for (Models.Alert alert : alerts) {
                    try {
                        long created = Instant.parse(alert.createdAt).toEpochMilli();
                        if (created > lastCheck && "new".equals(alert.status)) NotificationHelper.show(this, alert);
                    } catch (Exception ignored) {}
                }
                session.setLastAlertCheck(System.currentTimeMillis());
            } catch (Exception ignored) {}
            jobFinished(params, false);
        });
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) { return true; }
}
