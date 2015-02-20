package io.andr.wo6;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;

public class MainActivity extends Activity {

    private String url = "http://polling.bbc.co.uk/radio/nowandnextservice/bbc_6music.json";

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

            try {
                String artist = result.getJSONObject("message").get("artist").toString();
                String title = result.getJSONObject("message").get("title").toString();
                String packShotPid = result.getJSONObject("message").get("record_image_pid").toString();

                textTrackData.setVisibility(View.VISIBLE);
                textTrackData.setText(artist + "\n" + title);

                ImageView packShot = (ImageView) findViewById(R.id.packshot_image);

                String packShotUrl = "http://ichef.bbci.co.uk/images/ic/320x320/" + packShotPid;

                new DownloadImageTask(packShot).execute(packShotUrl);

            } catch(JSONException je) {
                textTrackData.setVisibility(View.VISIBLE);
                textTrackData.setText("Could not get track...");
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

        public DownloadImageTask(ImageView imageView) {
            this.imageView = imageView;
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

        protected void onPostExecute(Bitmap result) {
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            imageView.setImageBitmap(result);
        }
    }

    private TextView mTextView;

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

        new DownloadJsonTask().execute(url);
    }
}
