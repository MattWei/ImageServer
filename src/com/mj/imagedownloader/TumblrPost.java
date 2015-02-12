package com.mj.imagedownloader;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.tumblr.jumblr.types.Photo;
import com.tumblr.jumblr.types.PhotoPost;
import com.tumblr.jumblr.types.PhotoSize;
import com.tumblr.jumblr.types.Post;
import com.tumblr.jumblr.types.Video;
import com.tumblr.jumblr.types.VideoPost;

public class TumblrPost {
	public static final String LOG_TAG = "TumblrPost";
	public static final String PHOTO_TYPE = "photo";
	public static final String VIDEO_TYPE = "video";

	private Long mId;
	private Long mTumblrId;
	private String mFileName;
	private String mType;
	private String mPath;
	private int mFileSize;
	private String mDate;
	private URL mUrl;

	public class Thumbnail {
		public String mThumbnailPath;
		public URL mThumbnailUrl;
		public int mThumbnailWidth;
		public int mThumbnailHeight;

		public Thumbnail(URL url, int width, int height) {
			mThumbnailWidth = width;
			mThumbnailHeight = height;
			
			mThumbnailUrl = url;

			String[] urlFile = mThumbnailUrl.getFile().split("/");
			String fileName = urlFile[urlFile.length - 1];
			mThumbnailPath = fileName;
		}
		
		public Thumbnail(String path, int width, int height) {
			mThumbnailPath = path;
			mThumbnailWidth = width;
			mThumbnailHeight = height;
		}
		
		public void setParentPath(String path) {
			mThumbnailPath = path + "/" + mThumbnailPath;
		}
		
		public String getPath() {
			return mThumbnailPath;
		}
		
		public URL getURL() {
			return mThumbnailUrl;
		}
	};

	private Thumbnail mThumbnail = null;
	
	public void setThumbnail(URL url, int width, int height) {
		mThumbnail = new Thumbnail(url, width, height);
	}
	
	public void setThumbnail(String path, int width, int height) {
		mThumbnail = new Thumbnail(path, width, height);
	}
	
	public Thumbnail getThumbnail() {
		return mThumbnail;
	}
	
	// private ThumbNail mThumbNail = null;

	public static List<TumblrPost> parse(Post post, String type) {
		if (type.equals(PHOTO_TYPE)) {
			return parsePhotoPost(post);
		} else if (type.equals(VIDEO_TYPE)) {
			return parseVideoPost((VideoPost) post);
		} else {
			Log.e(LOG_TAG, "Wrong type:" + type);
			return null;
		}
	}

	private static boolean isContains(TumblrPost tumblrPost,
			List<TumblrPost> tumblrPosts) {
		for (TumblrPost tmp : tumblrPosts) {
			if (tmp.getUrl().equals(tumblrPost.getUrl())) {
				return true;
			}
		}

		return false;
	}

	private static TumblrPost doParsePose(String urlStr, Long tumblrId,
			String type) {
		URL url = null;
		try {
			url = new URL(urlStr);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}

		String[] urlFile = url.getFile().split("/");
		String fileName = urlFile[urlFile.length - 1];

		TumblrPost tumblrPost = new TumblrPost(tumblrId, url, fileName, type,
				null, 0);

		return tumblrPost;
	}

	private static List<TumblrPost> parseVideoPost(VideoPost post) {
		List<Video> videos = (post).getVideos();
		List<TumblrPost> tumblrPosts = new ArrayList<TumblrPost>();

		TumblrPost tumblrPost = null;

		for (Video video : videos) {
			String urlStr = video.getEmbedCode();
			if (!urlStr.contains("<source src=\"")) {
				Log.e(LOG_TAG, urlStr + "didn't contains url");
				continue;
			}

			urlStr = (urlStr.split("<source src=\"", 2))[1];
			urlStr = urlStr.split("\" type=\"video", 2)[0];
			// Log.d(LOG_TAG, "Video EmbedCode=" + video.getEmbedCode() +
			// " urlStr=" + urlStr);

			tumblrPost = doParsePose(urlStr, post.getId(), VIDEO_TYPE);
			if (tumblrPost == null || isContains(tumblrPost, tumblrPosts)) {
				continue;
			}

			Log.d(LOG_TAG, tumblrPost.toString());

			try {
				tumblrPost.setThumbnail(new URL(post.getThumbnailUrl()), 
						post.getThumbnailWidth(), 
						post.getThumbnailHeight());
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			tumblrPosts.add(tumblrPost);
		}

		return tumblrPosts;
	}

	private static List<TumblrPost> parsePhotoPost(Post post) {
		PhotoSize photoSize = null;
		List<Photo> photos = ((PhotoPost) post).getPhotos();

		List<TumblrPost> tumblrPosts = new ArrayList<TumblrPost>(photos.size());
		TumblrPost tumblrPost = null;

		for (Photo photo : photos) {
			photoSize = photo.getOriginalSize();
			// Log.d(LOG_TAG, i + " Caption:" + photo.getCaption() +
			// " Width:" + photoSize.getWidth() +
			// " Height:" + photoSize.getHeight() +
			// " Url:" + photoSize.getUrl());

			tumblrPost = doParsePose(photoSize.getUrl(), post.getId(),
					PHOTO_TYPE);
			if (tumblrPost == null || isContains(tumblrPost, tumblrPosts)) {
				continue;
			}

			tumblrPosts.add(tumblrPost);
		}

		return tumblrPosts;
	}

	public TumblrPost(long id, Long tumblrId, URL url, String fileName,
			String type, String path, int fileSize, String date) {
		mUrl = url;
		mId = id;
		mTumblrId = tumblrId;
		mFileName = fileName;
		mType = type;
		mPath = path;
		mFileSize = fileSize;
		mDate = date;
	}

	public TumblrPost(Long tumblrId, URL url, String fileName, String type,
			String path, int fileSize) {
		mUrl = url;
		mId = null;
		mTumblrId = tumblrId;
		mFileName = fileName;
		mType = type;
		mPath = path;
		mFileSize = fileSize;
		mDate = null;
	}

	public TumblrPost() {
		this(null, null, null, null, null, 0);
	}

	public Long getId() {
		return mId;
	}

	public void setId(Long mId) {
		this.mId = mId;
	}

	public String getFileName() {
		return mFileName;
	}

	public void setFileName(String mFileName) {
		this.mFileName = mFileName;
	}

	public String getType() {
		return mType;
	}

	public void setType(String mType) {
		this.mType = mType;
	}

	public String getPath() {
		return mPath;
	}

	public void setPath(String mPath) {
		this.mPath = mPath;
	}

	public int getFileSize() {
		return mFileSize;
	}

	public void setFileSize(int mFileSize) {
		this.mFileSize = mFileSize;
	}

	public String getDate() {
		return mDate;
	}

	public void setDate(String mDate) {
		this.mDate = mDate;
	}

	public Long getTumblrId() {
		return mTumblrId;
	}

	public void setTumblrId(Long mTumblrId) {
		this.mTumblrId = mTumblrId;
	}

	public URL getUrl() {
		return mUrl;
	}

	public void setUrl(URL mUrl) {
		this.mUrl = mUrl;
	}
	
	public String toSql() {
		if (mTumblrId == null || mFileName == null || mPath == null
				|| mType == null || mFileSize == 0)
			return null;

		/*
		 * (_id, TUMBLR_ID, NAME, DATE, PATH, TYPE, SIZE, THUMB_URL, THUMB_WIDTH, THUMB_HEIGHT)
		 */
		String sql = "(TUMBLR_ID, NAME, ";
		if (mDate != null)
			sql += "DATE, ";
		
		sql += "PATH, TYPE, SIZE";
		
		if (mType.equals(VIDEO_TYPE)) {
			sql += ", THUMB_URL, THUMB_WIDTH, THUMB_HEIGHT";
		}
		
		sql += ") VALUES ('" + mTumblrId + "', '" + mFileName + "'";

		if (mDate != null)
			sql += ",'" + mDate + "'";

		sql += ", '" + mPath + "', '" + mType + "', '" + mFileSize + "'";
		
		if (mType.equals(VIDEO_TYPE)) {
			sql +=  ", '" + mThumbnail.mThumbnailPath + "'" +
					", '" + mThumbnail.mThumbnailWidth +  "'" +
					", '" + mThumbnail.mThumbnailHeight + "'";
		}
		
		sql += ")";

		return sql;
	}
	
	public String toString() {
		return " _id=" + mId + " tumblrId=" + mTumblrId + " fileName="
				+ mFileName + " path=" + mPath + " type=" + mType + " url="
				+ mUrl + " fileSize=" + mFileSize + " Date=" + mDate;
	}
}
