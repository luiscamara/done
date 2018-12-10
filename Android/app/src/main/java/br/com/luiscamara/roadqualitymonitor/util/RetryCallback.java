package br.com.luiscamara.roadqualitymonitor.util;

import android.os.Handler;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public abstract class RetryCallback<T> implements Callback<T> {
    private static final int RETRY_COUNT = 3;
    private static final int RETRY_DELAY = 5 * 1000;
    private int retryCount = 0;

    @Override
    public void onResponse(final Call<T> call, Response<T> response) {
        onFinalResponse(call, response);
    }

    @Override
    public void onFailure(final Call<T> call, Throwable t) {
        retryCount++;
        if (retryCount <= RETRY_COUNT) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    retry(call);
                }
            }, RETRY_DELAY * retryCount);
        } else {
            onFailedAfterRetry(t);
        }
    }

    private void retry(Call<T> call) {
        call.clone().enqueue(this);
    }

    public abstract void onFinalResponse(final Call<T> call, Response<T> response);
    public abstract void onFailedAfterRetry(Throwable t);
}
