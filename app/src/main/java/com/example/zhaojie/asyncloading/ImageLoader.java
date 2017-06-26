package com.example.zhaojie.asyncloading;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;
import android.widget.ListView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by zhaojie on 2017/6/25.
 */

public class ImageLoader {
    private static final String TAG = "ImageLoader";
    private ImageView mImageView;
    private String mUrl;
    private LruCache<String,Bitmap> mCaches;
    private ListView mListView;
    private Set<ImageAsyncTask> mTasks;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Bitmap bitmap = (Bitmap) msg.obj;
            if(mImageView.getTag().equals(mUrl)){
            mImageView.setImageBitmap(bitmap);
            }
        }
    };

    public ImageLoader(ListView listView){
        mListView = listView;
        mTasks = new HashSet<>();
        //获得最大可用内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        Log.d(TAG,"maxMemory = "+maxMemory);
        int cacheSize = maxMemory/4;
        Log.d(TAG,"cacheSize = "+cacheSize);
        mCaches = new LruCache<String,Bitmap>(cacheSize){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                //在每次存入缓存时调用
                Log.d(TAG,"value.getByteCount() = "+value.getByteCount());
                return value.getByteCount();
            }
        };

    }

    /**
     * 将Bitmap添加到缓存
     */
    public void addBitmapToCache(String url,Bitmap bitmap){
        if(mCaches.get(url) == null){
            mCaches.put(url,bitmap);
        }
    }

    /**
     * 从缓存中获取Bitmap
     */
    public Bitmap getBitmapFromCache(String url){
        return mCaches.get(url);
    }

    public void showImageByThread(final ImageView imageView, final String url){
        new Thread(){
            @Override
            public void run() {
                mImageView = imageView;
                mUrl = url;
                Bitmap bitmap = getBitmapFromUrl(url);
                Message msg = Message.obtain();
                msg.obj = bitmap;
                mHandler.sendMessage(msg);
            }
        }.start();
    }

    private Bitmap getBitmapFromUrl(String urlString){
        Bitmap bitmap = null;
        InputStream is = null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            is = connection.getInputStream();
            bitmap = BitmapFactory.decodeStream(is);
            connection.disconnect();
            return bitmap;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(is != null){
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    /**
     * 用来加载从start到end的所有图片
     */
    public void loadImages(int start,int end){
        for(int i = start;i<end;i++){
            String url = NewsAdapter.URLS[i];
            //从缓存中获取图片
            Bitmap bitmap = getBitmapFromCache(url);
            if(bitmap == null){
                ImageAsyncTask imageAsyncTask = new ImageAsyncTask(url);
                imageAsyncTask.execute(url);
                mTasks.add(imageAsyncTask);
            }else{
                ImageView imageView = (ImageView) mListView.findViewWithTag(url);
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    public void cancelAllTask(){
        if(mTasks != null && mTasks.size() > 0){
            for(ImageAsyncTask task : mTasks){
                task.cancel(false);
            }
        }
    }

    public void showImageByAsyncTask(ImageView imageView,String url){
        //从缓存中获取图片
        Bitmap bitmap = getBitmapFromCache(url);
        if(bitmap == null){
            //如果缓存中没有，则需要从网络去下载
            new ImageAsyncTask(url).execute(url);
            Log.d(TAG,"get bitmap from url");
        }else{
            imageView.setImageBitmap(bitmap);
            Log.d(TAG,"get bitmap from cache");
        }
    }

    class ImageAsyncTask extends AsyncTask<String,Void,Bitmap>{
        //private ImageView mImageView;
        private String mUrl;
        public ImageAsyncTask(String url){
            //mImageView = imageView;
            mUrl = url;
        }
        @Override
        protected Bitmap doInBackground(String... params) {
            String url = params[0];
            //从网络获取图片
            Bitmap bitmap = getBitmapFromUrl(url);
            if(bitmap != null){
                //将图片添加到缓存中
                addBitmapToCache(url,bitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            ImageView imageView = (ImageView) mListView.findViewWithTag(mUrl);
            if(imageView != null && bitmap != null){
                imageView.setImageBitmap(bitmap);
            }
            mTasks.remove(this);
        }
    }

}
