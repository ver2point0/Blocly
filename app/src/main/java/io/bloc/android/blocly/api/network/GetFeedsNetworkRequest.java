package io.bloc.android.blocly.api.network;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class GetFeedsNetworkRequest extends NetworkRequest {

    String[] mFeedUrls;

    public GetFeedsNetworkRequest(String... feedUrls) {
        mFeedUrls = feedUrls;
    }

    @Override
    public Object performRequest() {
        for (String feedUrlString : mFeedUrls) {
            InputStream inputStream = openStream(feedUrlString);
            if (inputStream == null) {
                return null;
            }
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line = bufferedReader.readLine();
                while (line != null) {
                    Log.v(getClass().getSimpleName(), "Line: " + line);
                    line = bufferedReader.readLine();
                }
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
                setErrorCode(ERROR_IO);
                return null;
            }
        }
        return null;
    }

}
