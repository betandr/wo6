package io.andr.wo6;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;


public class MainMobActivity extends Activity implements MessageApi.MessageListener {

    private String url = "http://polling.bbc.co.uk/radio/nowandnextservice/bbc_6music.json";

    public static final String START_ACTIVITY_PATH = "/start/MainMobActivity";

    GoogleApiClient client;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        // todo Refactor this to a different activity
        if (messageEvent.getPath().equals(START_ACTIVITY_PATH)) {
            Intent startIntent = new Intent(this, MainMobActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
        }
    }

    private class DownloadJsonTask extends AsyncTask<String, Void, JSONObject> {
        protected JSONObject doInBackground(String... strings) {
            JSONObject jsonObject = null;

            try {
                InputStream is = new URL(strings[0].toString()).openStream();
                try {
                    BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
                    jsonObject = new JSONObject(readAll(rd));

                } finally {
                    is.close();
                }
            } catch (Exception e) {
                Log.e("DownloadJsonTask", e.getMessage());
            }

            return jsonObject;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            super.onPostExecute(result);

            TextView textTrackData = (TextView) findViewById(R.id.text_track_data);
            TextView start = (TextView) findViewById(R.id.start_text);

            String artist = "";
            try {
                artist = result.getJSONObject("message").get("artist").toString();
                Log.d("onPostExecute", "artist: " + artist);
            } catch(Exception e) {
                Log.e("onPostExecute", e.getMessage());
            }

            String title = "";
            try {
                title = result.getJSONObject("message").get("title").toString();
                Log.d("onPostExecute", "title: " + title);
            } catch(Exception e) {
                Log.e("onPostExecute", e.getMessage());
            }

            String startTime= "";
            try {
                startTime = result.getJSONObject("message").get("start").toString();
                Log.d("onPostExecute", "startTime: " + startTime);
            } catch(Exception e) {
                Log.e("onPostExecute", e.getMessage());
            }

            textTrackData.setVisibility(View.VISIBLE);
            String track = artist + "\n" + title;
            textTrackData.setText(track);
            start.setText(startTime);

            try {
                String packShotPid = result.getJSONObject("message").get("record_image_pid").toString();
                Log.d("onPostExecute", "packShotPid: " + packShotPid);

                ImageView packShot = (ImageView) findViewById(R.id.packshot_image);

                String packShotUrl = "http://ichef.bbci.co.uk/images/ic/320x320/" + packShotPid;
                Log.d("onPostExecute", "packShotUrl: " + packShotUrl);

                new DownloadImageTask(packShot, track).execute(packShotUrl);
            } catch (Exception e) {
                Log.d("onPostExecute", e.getMessage());
            }
        }

        private String readAll(Reader rd) throws IOException {
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
            return sb.toString();
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;
        String track;

        public DownloadImageTask(ImageView imageView, String track) {
            this.imageView = imageView;
            this.track = track;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap bitmap = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                bitmap = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return bitmap;
        }

        private Asset createAssetFromBitmap(Bitmap bitmap) {
            final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

            if (track.indexOf(".jpg") != -1) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteStream);
            } else if (track.indexOf(".png") != -1) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            }

            return Asset.createFromBytes(byteStream.toByteArray());
        }

        protected void onPostExecute(Bitmap result) {
            Asset asset = createAssetFromBitmap(result);
            Log.d("onPostExecute", "Created asset from bitmap");
            PutDataMapRequest request = PutDataMapRequest.create("/image");
            Log.d("onPostExecute", "Created request");

            DataMap map = request.getDataMap();
            map.putLong("time", new Date().getTime());
            map.putAsset("pack_shot", asset);
            map.putString("track", track);

            Log.d("onPostExecute", "Putting data item");
            Wearable.DataApi.putDataItem(client, request.asPutDataRequest());

            Log.d("onPostExecute", "Setting app image...");
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            imageView.setImageBitmap(result);
        }
    }

    private GoogleApiClient getGoogleApiClient(Context context) {
        return new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        client = getGoogleApiClient(this);

        new DownloadJsonTask().execute(url);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            new DownloadJsonTask().execute(url);
            Toast.makeText(this, "Reloading track data...", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
