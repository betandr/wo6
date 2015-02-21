package io.andr.wo6;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

public class MainWearActivity extends Activity implements DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final long CONNECTION_TIME_OUT_MS = 100;

    public static final String START_ACTIVITY_PATH = "/start/MainMobActivity";

    private GoogleApiClient client;

    private TextView mTextView;

    private GoogleApiClient getGoogleApiClient(Context context) {
        return new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d("onResume", "Resuming...");

        client = getGoogleApiClient(this);

        Log.d("onResume", "Sending messages...");

        new SendMessageTask(client)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        else
            asyncTask.execute(params);

        .execute();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });
    }

    public class SendMessageTask extends AsyncTask<Void, Void, Void> {

        GoogleApiClient client;

        public SendMessageTask(GoogleApiClient client) {

            this.client = client;

            Log.d("SendMessageTask", "Creating SendMessageTask");
        }

        private Collection<String> getNodeIds() {
            HashSet<String> results = new HashSet<String>();
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(this.client).await();

            for (Node node : nodes.getNodes()) {
                results.add(node.getId());
            }

            Log.d("getNodeIds", "Found IDs: " + results);
            return results;
        }

        private void sendStartActivityMessage(String nodeId) {
            Log.d("ssam", "Sending start activity to " + nodeId);

            Wearable.MessageApi.sendMessage(client, nodeId, START_ACTIVITY_PATH, new byte[0])
                    .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            if (!sendMessageResult.getStatus().isSuccess()) {
                                Log.e("ssam", "Failed to send message with status code: " + sendMessageResult.getStatus().getStatusCode());
                            } else {
                                Log.d("ssam", "Message sent");
                            }
                        }
                    });
        }

        @Override
        protected Void doInBackground(Void... voids) {

            Log.d("doInBackground", "Getting nodes...");
            Collection<String> nodeIds = getNodeIds();
            Log.d("doInBackground", "Found: " + nodeIds);

            for (String nodeId : nodeIds) {
                Log.d("doInBackground", "Sending activity message to " + nodeId);
                sendStartActivityMessage(nodeId);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Wearable.DataApi.addListener(client, this);
        Toast.makeText(this, "Added listener...", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int num) {
        Toast.makeText(this, "Connection suspended...", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult res) {
        Toast.makeText(this, "Connection failed..", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Toast.makeText(this, "Data changed...", Toast.LENGTH_LONG).show();

        ImageView packShot = (ImageView) findViewById(R.id.packshot_image);
        TextView trackText = (TextView) findViewById(R.id.text_track_data);

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/image")) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());

                String track = dataMapItem.getDataMap().getString("track");
                trackText.setText(track);

                Asset profileAsset = dataMapItem.getDataMap().getAsset("pack_shot");
                Bitmap bitmap = loadBitmapFromAsset(profileAsset);

                packShot.setScaleType(ImageView.ScaleType.FIT_XY);
                packShot.setImageBitmap(bitmap);

                Toast.makeText(this, "Updated...", Toast.LENGTH_LONG).show();
            }
        }
    }

    public Bitmap loadBitmapFromAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }

        ConnectionResult result = client.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
        if (!result.isSuccess()) {
            return null;
        }

        // Convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(client, asset).await().getInputStream();
        client.disconnect();

        if (assetInputStream == null) {
            Log.w("loadBitmapFromAsset", "Requested an unknown asset.");
            return null;
        }

        // Decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
    }
}
