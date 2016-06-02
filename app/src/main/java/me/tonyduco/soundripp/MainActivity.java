package me.tonyduco.soundripp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gmail.jerickson314.sdscanner.ScanFragment;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagOptionSingleton;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import de.voidplus.soundcloud.SoundCloud;
import de.voidplus.soundcloud.Track;
import de.voidplus.soundcloud.User;


public class MainActivity extends Activity {

    private Button updateButton;
    private ProgressBar progressBar;
    private ProgressBar progressSpinner;
    private TextView countText;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        updateButton = (Button) findViewById(R.id.button);
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                update();
                updateButton.setEnabled(false);
                progressSpinner.setVisibility(View.VISIBLE);
            }
        });
        progressBar = (ProgressBar) findViewById(R.id.progressBar2);
        countText = (TextView) findViewById(R.id.textView2);
        statusText = (TextView) findViewById(R.id.textView3);
        progressSpinner = (ProgressBar) findViewById(R.id.progressBar);
        progressSpinner.setVisibility(View.INVISIBLE);


    }

    public void setStatus(String status){
        statusText.setText(status);
    }

    public void setCounter(String count){
        countText.setText(count);
    }

    public void progressMax(int max){
        progressBar.setMax(max);
    }

    public void progressStatus(int status){
        progressBar.setProgress(status);
    }

    public void update() {
        new soundCloudDownload().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    class soundCloudDownload extends AsyncTask<String, String, String> {

        @Override
        protected void onProgressUpdate(String... values){
            switch(values[0]){
                case "status":
                    setStatus(values[1]);
                    break;
                case "counter":
                    setCounter(values[1]);
                    break;
                case "progressbar":
                    progressStatus(Integer.parseInt(values[1]));
                    break;
                case "progressbarmax":
                    progressMax(Integer.parseInt(values[1]));
                    break;
                case "finished":
                    progressSpinner.setVisibility(View.INVISIBLE);
                    updateButton.setEnabled(true);
                    break;
                default:
                    break;
            }
        }

        public Drawable LoadImageFromWeb(String url){
            try {
                InputStream is = (InputStream) new URL(url).getContent();
                Drawable d = Drawable.createFromStream(is, "soundcloud");
                return d;
            }catch(Exception ex){
                return null;
            }
        }
        protected String doInBackground(String... params){
            try {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());


                publishProgress("status", "Initializing SoundCloud...");
                SoundCloud soundCloud = new SoundCloud("a6d53c1d23d52a573379f80114806a3c", "60a94e71ac51b840e8ed55b9690e6ab0");
                soundCloud.login(prefs.getString("soundcloud-username", ""), prefs.getString("soundcloud-password", ""));
                publishProgress("status", "Logging in...");
                User me = soundCloud.getMe();
                Integer count = me.getPublicFavoritesCount();
                Integer limit = 200; // = max
                Integer pages = ((int) count / limit);
                ArrayList<Track> all_tracks = new ArrayList<Track>();
                ArrayList<Track> all_tags = new ArrayList<Track>();
                publishProgress("status", "Retrieving favorites list...");
                Log.d("SS", "count: " + count + " pages: " + pages);
                for (int i = 0; i < pages; i++) {
                   ArrayList<Track> temp_tracks = soundCloud.getMeFavorites((i * limit), limit);
                    Log.d("SCDL", "Trying " + i + "...");
//                    Log.d("SCDL", soundCloud.get("/users/" + me.getId() + "/favorites", new String[] { "order", "created_at", "limit", Integer.toString(limit), "offset", Integer.toString(i*limit)}).toString());
                    Log.d("SCDL", soundCloud.get("/me/favorites", new String[] { "order", "created_at", "limit", Integer.toString(limit), "offset", Integer.toString(i*limit)}).toString());
//                    ArrayList<Track> temp_tracks = soundCloud.get("/users/" + me.getId() + "/favorites", new String[] { "order", "created_at", "limit", Integer.toString(limit), "offset", Integer.toString(i*limit)});
                    all_tracks.addAll(temp_tracks);
                    all_tags.addAll(temp_tracks);
                }

                ArrayList<String> downloads = new ArrayList<String>();
                ArrayList<Track> tracks = new ArrayList<Track>();
                ArrayList<String> album = new ArrayList<String>();

                publishProgress("status", "Grabbing URLs for MP3s and Artwork...");

                int ii=0;
                while(ii<all_tracks.size()){
                    File musicFile = new File(prefs.getString("download-path", "") + "SoundCloud/" + all_tracks.get(ii).getTitle() + ".mp3");
                    if(musicFile.exists()){
                        Log.d("SoundRipp", "Skipping " + all_tracks.get(ii).getTitle());
                        all_tracks.remove(ii);
                    }else{
                        ii++;
                    }
                }
                for (Track track : all_tracks) {
                    Log.d("SoundRipp", "Running " + track.getTitle());
                    String dl = track.getWaveformUrl();
                    String albumArt = track.getArtworkUrl();
                    if(!(albumArt == null)){
                        albumArt = albumArt.replace("large.jpg", "original.jpg");
                            if(LoadImageFromWeb(albumArt) == null){

                                albumArt = albumArt.replace("original.jpg", "t500x500.jpg");
                                if(LoadImageFromWeb(albumArt) == null){
                                    albumArt = track.getArtworkUrl();
                                }else{
                                }
                            }else{
                            }
                    }else{
                        Log.d("SoundRipp", "No album art found for " + track.getTitle());
                        albumArt = "None";
                    }

                    //Need to redo way of downloading songs...
                    String json = getJSON("http://api.soundcloud.com/i1/tracks/" + track.getId() + "/streams?client_id=376f225bf427445fc4bfb6b99b72e0bf");
                    try{
                        JSONObject jsonObject = new JSONObject(json);
                        downloads.add(jsonObject.getString("http_mp3_128_url"));
                        Log.d("SCDL", jsonObject.getString("http_mp3_128_url"));
                    } catch(Exception e){e.printStackTrace();}
                    finally{System.out.println("Success");}



//                    dl = dl.replaceAll("https://w1.sndcdn.com/", "").replaceAll("_m.png", "");
//                    String dlURL = "http://media.soundcloud.com/stream/" + dl + ".mp3";
//                    downloads.add(dlURL);
                    tracks.add(track);
                    album.add(albumArt);

                }
                publishProgress("status", "Initializing download...");
                publishProgress("counter", "0/" + downloads.size());
                publishProgress("progressbarmax", String.valueOf(downloads.size()));
                for (int i = downloads.size()-1; i >=0; i--) {
                    publishProgress("counter", tracks.get(i).getTitle() + " " + (i + 1) + "/" + downloads.size());
                    publishProgress("progressbar", String.valueOf((i + 1)));
                    String[] URL = new String[4];
                    URL[0] = downloads.get(i);
                    URL[1] = tracks.get(i).getTitle();
                    URL[2] = album.get(i);
                    URL[3] = tracks.get(i).getUser().getUsername();

                    int countt;
                    try {
                        publishProgress("status", "Initializing URL connection...");
                        java.net.URL url = new URL(URL[0]);
                        URLConnection conexion = url.openConnection();
                        conexion.connect();

                        InputStream input = new BufferedInputStream(url.openStream());
                        File soundCloudFolder = new File(prefs.getString("download-path", "") + "SoundCloud/");
                        if(!soundCloudFolder.isDirectory()){
                            soundCloudFolder.mkdir();
                        }
                        publishProgress("status", "Directing folders...");
                        String musicOutputString = soundCloudFolder + "/" + URL[1] + ".mp3";
                        File musicOutfileFile = new File(musicOutputString);
                        if(!musicOutfileFile.exists()) {

                            publishProgress("status", "Downloading MP3 file...");
                            OutputStream output = new FileOutputStream(musicOutputString);

                            byte data[] = new byte[1024];

                            long total = 0;

                            while ((countt = input.read(data)) != -1) {
                                total += countt;
                                output.write(data, 0, countt);
                            }

                            output.flush();
                            output.close();
                            input.close();
                            java.net.URL artworkURL;
                            if(URL[2].contains("None")){
                                artworkURL = new URL("http://tonyduco.me/soundripp/album.png");
                            }else {
                                artworkURL = new URL(URL[2]);
                            }
                            URLConnection artworkConnection = artworkURL.openConnection();
                            artworkConnection.connect();

                            // Create album folder
                            File artworkFolder = new File(prefs.getString("download-path", "") + "SoundCloud/Artwork/");
                            if (!artworkFolder.isDirectory()) {
                                artworkFolder.mkdir();
                            }

                            //Download artwork
                            publishProgress("status", "Downloading album artwork...");
                            InputStream artworkInput = new BufferedInputStream(artworkURL.openStream());
                            String artworkFolderString = artworkFolder + "/" + URL[1] + ".jpg";
                            OutputStream artworkOutput = new FileOutputStream(artworkFolderString);

                            byte artworkData[] = new byte[1024];

                            long artworkTotal = 0;
                            countt = 0;
                            while ((countt = artworkInput.read(artworkData)) != -1) {
                                artworkTotal += countt;
                                artworkOutput.write(artworkData, 0, countt);
                            }
                            artworkOutput.flush();
                            artworkOutput.close();
                            artworkInput.close();

                            publishProgress("status", "Downloading MP3 tags...");
                            File musicFile = new File(soundCloudFolder + "/" + URL[1] + ".mp3");
                            File imgPath = new File(artworkFolderString);
                            TagOptionSingleton.getInstance().setAndroid(true);
                            MP3File music = new MP3File(musicOutputString);
                            music.setTag(new ID3v24Tag());
                            Tag tag = music.getTag();
                            tag.addField(FieldKey.ARTIST, URL[3]);
                            tag.addField(FieldKey.TITLE, URL[1]);
                            tag.addField(FieldKey.ALBUM, (all_tags.indexOf(tracks.get(i)) + 1) + " " +  URL[1]);

                            Bitmap bitmap = BitmapFactory.decodeFile(imgPath.getAbsolutePath());

                            final int value;
                            if(bitmap.getHeight() <= bitmap.getWidth()){
                                value = bitmap.getHeight();
                            }else{
                                value = bitmap.getWidth();
                            }

                            final Bitmap finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, value, value);
                            FileOutputStream out = new FileOutputStream(imgPath);
                            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                            out.flush();
                            out.close();

                            Artwork artwork = ArtworkFactory.createArtworkFromFile(imgPath);
                            tag.deleteArtworkField();
                            tag.setField(artwork);
                            music.setTag(tag);
                            AudioFileIO.write(music);
                            AudioFileIO.write(music);
                            publishProgress("status", "Cleaning up...");
                            imgPath.delete();
                            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                            intent.setData(Uri.fromFile(musicFile));
                            sendBroadcast(intent);
                            Log.d("SoundRipp", "Finished " + URL[1]);
                        }else{
                            Log.d("SoundRipp", "Section 2 Skipping " + all_tracks.get(ii).getTitle());
                            publishProgress("status", "Song already downloaded! Skipping...");
                        }




                        publishProgress("status", "Finished...");
                    } catch (Exception e) {
                    }
                }


                if(prefs.getBoolean("update-library", true)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                statusText.setText("Updating Media Libraries...");
                                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                ScanFragment scanFragment = new ScanFragment();
                                File path = new File(prefs.getString("download-path", ""));
                                scanFragment.startScan(path.getCanonicalFile(), false, getApplicationContext());
                                statusText.setText("Finished! Might take a minute for Music to show.");
                                progressSpinner.setVisibility(View.INVISIBLE);
                            }catch(Exception ex){
                            }
                        }
                    });
                }else{
                    publishProgress("status", "Finished!");
                    publishProgress("finished", "Finished!");
                }
            }catch(Exception ex){
            }
            return null;
        }

    }

    public String getJSON(String address){
        StringBuilder builder = new StringBuilder();
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(address);
        try{
            HttpResponse response = client.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if(statusCode == 200){
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                String line;
                while((line = reader.readLine()) != null){
                    builder.append(line);
                }
            } else {
                Log.e(MainActivity.class.toString(),"Failed to get JSON object");
            }
        }catch(ClientProtocolException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
        return builder.toString();
    }

}

