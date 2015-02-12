package com.mj.imagedownloader;

import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.exceptions.JumblrException;
import com.tumblr.jumblr.types.Post;
import com.tumblr.jumblr.types.User;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by weimj on 15/1/27.
 */
public class DownloadThread extends Thread {
    private static final String LOG_TAG = "DownloadThread";
    
    public final static String DOWNLOAD_BASH_BOARD = "downloadDashBoard";
    public final static String GET_USER_INFO = "getUserInfo";

    private final static String FINISH_TUMBLR_POST_TYPE = "finish";
    
    private JumblrClient mClient = null;

    private boolean mIsStop = false;

    private SqliteManager mSqlManager = null;
    private String mJob = null;

    private static final int QUEUE_SIZE = 100;
    private ArrayBlockingQueue<TumblrPost> mFairQueue = null;

    private int mThreadSize = 10;
    private ArrayList<FileDownloadThread> mThreadList = null;
    
    private int mLimitPerRequest = 20;
    private int mOffset = 0;
    private int mSinceId = 0;
    
    private String mDownloadType = TumblrPost.PHOTO_TYPE;
    private int mMaxDownload = 100;
    private String mSavePath = null;

    public void setLimitPerRequest(int limit) {
    	if (limit > 0)
    		mLimitPerRequest = limit;
    }
    
    public void setOffset(int offset) {
    	if (offset > 0)
    		mOffset = offset;
    }
    
    public void setSinceId(int sinceId) {
    	if (sinceId > 0)
    		mSinceId = sinceId;
    }
    
    
    public DownloadThread(SqliteManager sqlManager, String job, String... params) {
        // Authenticate via OAuth
    	
    	mClient = new JumblrClient(
    	  "i6ZbpJnTsiz4bYCVvFjNI3qBsXigY10HiTvBjihRRKKFCS0s4Y",
    	  "zFx6ibpCqASZuzJSHqCXumKycvo0l4eG5oaCcjrk6lydRsWEva"
    	);
    	
    	mClient.setToken(
    			  "jilqu0xy9dzstlEqMJqds9Qu3YecYogC7Q2jI6dqlHWcOA27Ko",
    			  "o3JkdERxfwU67rz4MdTJwljE90OWG9tv8BqHkTp9YnFJDsMhfe"
    	);

        mSqlManager = sqlManager;

        mJob = job;
        
        if (params.length > 0) {
        	mSavePath = params[0];
        }
        
        if (params.length > 1) {
        	mDownloadType = params[1];
        }
        
        if (params.length > 2) {
        	mMaxDownload = Integer.parseInt(params[2]);
        }
        
        mFairQueue = new  ArrayBlockingQueue<TumblrPost>(QUEUE_SIZE);
    }

    public void setDownloadThread(int size) {
    	if (size >= 0)
    		mThreadSize = size;
    }
    
    public class FinishTumblrPost extends TumblrPost {
    	public FinishTumblrPost() {
    		setType(FINISH_TUMBLR_POST_TYPE);
    	}
    }
    
    class FileDownloadThread extends Thread {
        @Override
        public void run() {
            if (mFairQueue == null) {
                Log.e(LOG_TAG, "mFairQueue is null");
                return;
            }

            try {
                while(!mIsStop || !mFairQueue.isEmpty()) {
                    TumblrPost post = (TumblrPost) mFairQueue.take();
                    if (post != null) {
                    	if (post.getType() == DownloadThread.FINISH_TUMBLR_POST_TYPE) {
                    		Log.d(LOG_TAG, "Download finish, thread exit");
                    		break;
                    	}
                    
                        DownloadFromUrl(post);
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    private boolean createDownloadThread() {
        if (mThreadList == null) {
        	Log.d(LOG_TAG, "Create download thread");
            mThreadList = new ArrayList<FileDownloadThread>(mThreadSize);
            for (int i = 0; i < mThreadSize; i++) {
                FileDownloadThread thread = new FileDownloadThread();
                mThreadList.add(thread);
                thread.start();
            }
        }
        
        return true;
    }

    public void stopDownload() {
        mIsStop = true;
    }
    
    @Override
    public void run() {
        mIsStop = false;

        Log.d(LOG_TAG, mJob);

        if (mJob.equals(DOWNLOAD_BASH_BOARD)) {
        	Log.d(LOG_TAG, "Start download photo\n");
            if (!createDownloadThread())
                return;

            try {
            	downloadDashboard(mDownloadType, mLimitPerRequest, mOffset, mSinceId, mMaxDownload);
            } catch (JumblrException e) {
            	Log.d(LOG_TAG, "JumblrException:"); 
            	e.printStackTrace();
            	return;
            }
        } else if (mJob.equals(GET_USER_INFO)) {
            getTumblrUser();
            Log.d(LOG_TAG, DOWNLOAD_BASH_BOARD);
        }
    }



    public void getTumblrUser() {
        User user = mClient.user();
        // Write the user's name
        Log.d(LOG_TAG, "User name:" + user.getName());

    }

    public boolean downloadDashboard(String type, int limit, int offset, int sinceId, int maxDownload) {
        Map<String, Object> params = new HashMap<String, Object>();
        if (limit <= 0)
            limit = 20;

        params.put("limit", limit);

        if (offset > 0)
            params.put("offset", offset);
        else
            offset = 0;

        if (type != null)
            params.put("type", type);

        if (sinceId > 0)
            params.put("since_id", sinceId);

        List<Post> posts = mClient.userDashboard(params);
        if (posts.size() <= 0)
            return false;

        boolean downloadRes = downloadPosts(posts, type);

        //TODO just download once in test
        //mIsStop = true;
        while (downloadRes && !mIsStop) {
            offset += posts.size();
            if (offset >= maxDownload) {
                mIsStop = true;
                break;
            }
            
            downloadRes = downloadDashboard(type, limit, offset, sinceId, maxDownload);
        }

    	try {
    		for (int i = 0; i < mThreadSize; i++) {
				mFairQueue.put(new FinishTumblrPost());
    		} 
    	} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
        waitingForDownloadFinish();

        return downloadRes;
    }

    private void waitingForDownloadFinish() {
        Log.d(LOG_TAG, "Waiting all download finish");

        for (FileDownloadThread thread : mThreadList) {
            try {                	
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        mThreadList.clear();
        
        Log.d(LOG_TAG, "All downloads are finish");
    }

    public boolean downloadPosts(List<Post> posts, String type) {
        Log.d(LOG_TAG, "Download from id=" + posts.get(0).getId() +
                " to id=" + posts.get(posts.size() - 1).getId());

        Date dNow = new Date( );
        SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd");
        String saveDir = mSavePath + type + "/" + ft.format(dNow) + "/";
        File file = new File(saveDir);

        if (file.exists() && !file.isDirectory()) {
            Log.d(LOG_TAG, "Remove and re-mkdir: " + saveDir + "\n");
            if (!file.delete() || !file.mkdir())
                return false;
        } else if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e(LOG_TAG, "Mkdir: " + saveDir + " error \n");
                return false;
            }
        }

        return putToQueue(posts, saveDir, type);
    }

    private boolean putToQueue(List<Post> posts, String saveDir, String type) {
        for (int i = 0; i < posts.size(); i++) {
            List<TumblrPost> tumblrPosts = TumblrPost.parse(posts.get(i), type);
            if (tumblrPosts == null || tumblrPosts.isEmpty()) {
                Log.e(LOG_TAG, "Error no tumblrPosts to download");
                continue;
            }

            for (TumblrPost tumblrPost : tumblrPosts) {
                if (tumblrPost != null && !mSqlManager.isInserted(
                        tumblrPost.getTumblrId(),
                        tumblrPost.getFileName(),
                        tumblrPost.getType())) {
                    try {
                        tumblrPost.setPath(saveDir + tumblrPost.getFileName());
                        mFairQueue.put(tumblrPost);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return false;
                    }
                } else {
                    Log.d(LOG_TAG, tumblrPost.getFileName() + " have been insert");
                }
            }
        }

        return true;
    }

    public void DownloadFromUrl(TumblrPost post) {  //this is the downloader method
        try {

            URL url = post.getUrl();
            File file = new File(post.getPath());

            boolean inserted = mSqlManager.isInserted(
                    post.getTumblrId(),
                    post.getFileName(),
                    post.getType());
            
            if (file.exists() || inserted) {
                Log.d(LOG_TAG, "File " + post.getFileName() + " had exits, return\n");
                return;
            } else if (file.exists() && !inserted) {
            	file.delete();
            }

            int fileSize = saveFile(url, file);
            
            post.setPath(file.getAbsolutePath());
            post.setFileSize(fileSize);
            
            if (post.getType().equals(TumblrPost.VIDEO_TYPE)) {
            	TumblrPost.Thumbnail thumbnail = post.getThumbnail();
            	
            	thumbnail.setParentPath(file.getParent());
            	
            	File thumbnailFile = new File(thumbnail.getPath());
            	
            	saveFile(thumbnail.getURL(), thumbnailFile);
            	//file.getParent()
            }
            
            mSqlManager.insert(post);

            //Log.d(LOG_TAG, "download ready in"
            //        + ((System.currentTimeMillis() - startTime) / 1000)
            //        + " sec \n");

        } catch (IOException e) {
            Log.d(LOG_TAG, "Error: " + e);
        }

    }
    
    private int saveFile(URL url, File file) throws IOException {
    	Log.d(LOG_TAG, "Download " + file.getName() + " from " + url.toString() + " to " + file.getAbsolutePath());
        /*
         * Define InputStreams to read from the URLConnection.
        */
        InputStream is = url.openConnection().getInputStream();
        BufferedInputStream bis = new BufferedInputStream(is);
        
    	FileOutputStream fos = new FileOutputStream(file);

    	//long startTime = System.currentTimeMillis();

    	/*
    	 * Read bytes to the Buffer until there is nothing more to read(-1).
    	 */
    	byte[] baf = new byte[1024];
    	int read;
    	int fileSize = 0;
    	while ((read = bis.read(baf)) != -1) {
    		fos.write(baf, 0, read);
    		fileSize += read;
    	}

    	bis.close();
    	fos.close();
    	
    	return fileSize;
    }
    
	  public static void main( String args[] )
	  {
		  DownloadThread dThread = new DownloadThread(SqliteManager.getInstance(), DownloadThread.GET_USER_INFO);
		  dThread.start();
	  }
}