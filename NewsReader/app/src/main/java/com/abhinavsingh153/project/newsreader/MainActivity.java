package com.abhinavsingh153.project.newsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> content = new ArrayList<>();

    ArrayList<String> titles = new ArrayList<>();

    ArrayAdapter arrayAdapter;

    SQLiteDatabase articlesDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        articlesDb = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);

        articlesDb.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleID INTEGER , title VARCHAR, content VARCHAR)");

        DownloadTask task = new DownloadTask();
        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty").get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ListView listView = findViewById(R.id.titleslist);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Intent intent = new Intent(getApplicationContext(), ArticleActivity.class);
                intent.putExtra("content", content.get(i));

            }
        });

        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);

        listView.setAdapter(arrayAdapter);

        updateListView();
    }

    public void updateListView() {

        Cursor c = articlesDb.rawQuery("SELECT * FROM articles", null);

        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");

        if (c.moveToFirst()) {

            titles.clear();
            content.clear();

            do {
                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));


            } while (c.moveToNext());

            arrayAdapter.notifyDataSetChanged();
        }

    }

    public class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {

            String result = "";

            URL url;
            HttpURLConnection httpURLConnection = null;
            try {
                url = new URL(urls[0]);

                httpURLConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = httpURLConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(inputStream);
                int data = reader.read();

                while (data != -1) {

                    char current = (char) data;

                    result += current;

                    data = reader.read();

                }


                JSONArray jsonArray = new JSONArray(result);
                int numberOfItems = 20;

                if (jsonArray.length() < 20) {
                    numberOfItems = jsonArray.length();
                }

                articlesDb.execSQL("DELETE FROM articles");

                for (int i = 0; i < numberOfItems; i++) {

                    String articleID = jsonArray.getString(i);

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleID + ".json?print=pretty");

                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    inputStream = httpURLConnection.getInputStream();
                    reader = new InputStreamReader(inputStream);
                    data = reader.read();
                    String articleInfo = "";

                    while (data != -1) {

                        char current = (char) data;
                        articleInfo += current;
                        data = reader.read();
                    }


                    JSONObject jsonObject = new JSONObject(articleInfo);


                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                        String articleTitle = jsonObject.getString("title");
                        String articleURL = jsonObject.getString("url");

                         Log.i("info", articleTitle + articleURL);

                        url = new URL(articleURL);
                        httpURLConnection = (HttpURLConnection) url.openConnection();
                        inputStream = httpURLConnection.getInputStream();
                        reader = new InputStreamReader(inputStream);
                        data = reader.read();

                        String articleContent = "";

                        while (data != -1) {

                            char current = (char) data;
                            articleContent += current;
                            data = reader.read();
                        }

                        Log.i("articleContent", articleContent);

                        String sql = "INSERT INTO articles (articleID , title, content) VALUES (? ,? , ?)";
                        SQLiteStatement statement = articlesDb.compileStatement(sql);

                        statement.bindString(1, articleID);
                        statement.bindString(2, articleTitle);
                       // statement.bindString(3, articleContent);

                        statement.execute();


                    }


                    //Log.i("JSONItem" , jsonArray.getString(i));
                }

            } catch (MalformedURLException e) {

                e.printStackTrace();
            } catch (IOException e) {

                e.printStackTrace();
            } catch (JSONException e) {

                e.printStackTrace();
            }

            return null;

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
        }
    }

}
