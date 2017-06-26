package com.example.zhaojie.asyncloading;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String DATA_URL = "http://www.imooc.com/api/teacher?type=4&num=40";
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        new NewsAsyncTask().execute(DATA_URL);
    }

    private void initView(){
        mListView = (ListView) findViewById(R.id.lv_main);
    }

    private List<NewsBean> getJsonData(String url){
        List<NewsBean> newsBeanList = new ArrayList<NewsBean>();
        try {
            String jsonString = readStream(new URL(url).openStream());
            Log.i("zhaojie","jsonString = "+jsonString);
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray jsonArray = jsonObject.getJSONArray("data");
            for(int i = 0;i<jsonArray.length();i++){
                NewsBean newsBean = new NewsBean();
                jsonObject = jsonArray.getJSONObject(i);
                if(jsonObject.has("name")){
                    newsBean.setTitle(jsonObject.getString("name"));
                }
                if(jsonObject.has("description")){
                    newsBean.setContent(jsonObject.getString("description"));
                }
                if(jsonObject.has("picSmall")){
                    newsBean.setIconUrl(jsonObject.getString("picSmall"));
                }
                newsBeanList.add(newsBean);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }catch (JSONException e) {
            e.printStackTrace();
        }
        return newsBeanList;
    }

    /**
     * 读取url地址的数据，将其转化为Json字符串
     */
    private String readStream(InputStream is){
        InputStreamReader isr = null;
        String result = "";
        try {
             isr = new InputStreamReader(is,"utf-8");
            BufferedReader bfr = new BufferedReader(isr);
            String line = "";
            StringBuilder sb = new StringBuilder();
            while((line = bfr.readLine()) != null){
                sb.append(line);
            }
            result = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if(isr != null){
                    isr.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    class NewsAsyncTask extends AsyncTask<String,Void,List<NewsBean>>{
        @Override
        protected List<NewsBean> doInBackground(String... params) {
            return getJsonData(params[0]);
        }

        @Override
        protected void onPostExecute(List<NewsBean> newsBeen) {
            super.onPostExecute(newsBeen);
            NewsAdapter adapter = new NewsAdapter(MainActivity.this,newsBeen,mListView);
            mListView.setAdapter(adapter);
        }
    }
}
