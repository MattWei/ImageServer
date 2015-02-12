package com.mj.imagedownloader;


import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
	
public class SqliteManager {
	private static final String LOG_TAG = "SqliteManager";
	
	public static final String DATABASE_FILE = "/tumblr.db";
	
	private static SqliteManager mInstance = null;
	private int mTimeout = 30;
	private Connection mConnection = null;
	
    public static final String TABLE_NAME = "tumblr";
    
    private static String mDataBaseFilePath = null;
    
	private SqliteManager() {
		
	}
	
	public static boolean isCreated(String path) {
		mDataBaseFilePath = path + DATABASE_FILE;
		File file = new File(mDataBaseFilePath);
		if (file.exists())
			return true;
		
		if (!file.getParentFile().exists()) {
			Log.i(LOG_TAG, "Create parent directory:" + file.getParent());
			file.getParentFile().mkdirs();
		}
		
		return false;
	}
	
	public static SqliteManager getInstance() {
		if (mInstance == null) {
			mInstance = new SqliteManager();
			if (!mInstance.connect(mDataBaseFilePath))
				return null;
		}
		
		return mInstance;
	}
	
	public boolean connect(String dbPath) {
		try {
			Class.forName("org.sqlite.JDBC");
			mConnection = DriverManager.getConnection("jdbc:sqlite:" + mDataBaseFilePath);
		} catch (ClassNotFoundException e) {
			System.err.print("Error org.sqlite.JDBC not found:\n");
			e.printStackTrace();
			return false;
		} catch (SQLException e) {
			System.err.print("Error connect error:\n");
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public boolean createDataBase() {
        String tumblr_table_create =
                "CREATE TABLE " + TABLE_NAME + " (" +
                " _id          INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " TUMBLR_ID	   INTEGER  NOT NULL, " +
                " NAME         TEXT     NOT NULL, " +
                " DATE         DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                " PATH         TEXT     NOT NULL, " +
                " TYPE	       TEXT     NOT NULL, " +
                " SIZE		   INTEGER  NOT NULL, " +
                " THUMB_URL    TEXT, " +
                " THUMB_WIDTH  INTEGER, " +
                " THUMB_HEIGHT INTEGER )";
        
        return sqliteUpdate(tumblr_table_create);
	}
	
	private boolean sqliteUpdate(String sql) {
        Statement stmt = null;
        
		try {
			stmt = mConnection.createStatement();
            stmt.setQueryTimeout(mTimeout);
            stmt.executeUpdate(sql); 
		} catch (SQLException e) {
			System.err.print("Error insert database error:\n");
			e.printStackTrace();
			return false;
		}finally {
            try { stmt.close(); } catch (Exception ignore) {}
		}
		
		return true;
	}
	
	public boolean insert(TumblrPost post) {
		String sql = "INSERT INTO " + TABLE_NAME + " " + post.toSql() + ";";
		Log.d(LOG_TAG, "Insert sql:" + sql);
		return sqliteUpdate(sql);
	}
	
	public void delete() {
		
	}
	
	public boolean isInserted(Long tumblrId, String fileName, String type) {
		String sql = "SELECT _id from " + TABLE_NAME + 
				" WHERE TUMBLR_ID = '" + tumblrId + "'" +
				" AND NAME = '" + fileName + "'" +
				" AND TYPE = '" + type + "';";
		
		Statement stmt = null;
		ResultSet res = null;
		int count = 0;
	   	try {
	   		stmt = mConnection.createStatement();
            stmt.setQueryTimeout(mTimeout);
            res = stmt.executeQuery(sql);

			while(res.next()) {
				//count = res.getInt("COUNT");
				count++;
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} finally {
            try {
            	res.close(); 
            	stmt.close(); 
            } catch (Exception ignore) {}
		}	
		
		return count > 0 ? true : false;
	}
	
	/*
	private ResultSet doSelect(String sql) {
        Statement stmt = null;
        ResultSet rs = null;
		try {
			stmt = mConnection.createStatement();
            stmt.setQueryTimeout(mTimeout);
            rs = stmt.executeQuery(sql);
		} catch (SQLException e) {
			System.err.print("Error execute query error:\n");
			e.printStackTrace();
			return null;
		}finally {
            try { stmt.close(); } catch (Exception ignore) {}
		}		
		return rs;
	}
	*/
	
	public List<TumblrPost> select(String request, String type, String sinceId, String limit) {
		String sql = "SELECT _id, TUMBLR_ID, NAME, DATE, PATH, SIZE, THUMB_URL, THUMB_WIDTH, THUMB_HEIGHT from " + 
				TABLE_NAME + " WHERE " + "TYPE = '" + type + "'";
		
		if (sinceId != null && Long.parseLong(sinceId) > 0) {
			if (request.equals("get")) {
				sql += " AND _id < ";
			} else {
				sql += " AND _id > ";
			}
			 sql += sinceId;
		}
		
		sql += " ORDER BY _id DESC " + " LIMIT " + limit + ";";
		
		ArrayList<TumblrPost> posts = new ArrayList<TumblrPost>();
		
		Log.d(LOG_TAG, "select query:" + sql);
		Statement stmt = null;
		ResultSet res = null;
	   	try {
	   		//ResultSet res = doSelect(sql);
	   		stmt = mConnection.createStatement();
            stmt.setQueryTimeout(mTimeout);
            res = stmt.executeQuery(sql);
            
            TumblrPost tumblrPost = null;
	   		while (res != null && res.next()) {
	   			tumblrPost = new TumblrPost(res.getLong("_id"),
	   					res.getLong("TUMBLR_ID"),
	   					null,
	   					res.getString("NAME"),
	   					type,
	   					res.getString("PATH"),
	   					res.getInt("SIZE"),
	   					res.getString("DATE"));
	   			
	   			tumblrPost.setThumbnail(res.getString("THUMB_URL"), 
	   					res.getInt("THUMB_WIDTH"), 
	   					res.getInt("THUMB_HEIGHT"));
	   			
	   			posts.add(tumblrPost);
	   		}
	   	
	   		Log.i(LOG_TAG, "Select rows:" + posts.size());
	   		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} finally {
            try {
            	res.close(); 
            	stmt.close(); 
            } catch (Exception ignore) {}
		}	
	   	
	   	return posts;
	}
	
	public void modify() {
		
	}
	
	  public static void main( String args[] )
	  {
	      SqliteManager sqlite = SqliteManager.getInstance();
	      if (sqlite != null) {
	    	  //System.out.println("Opened database successfully\n");
	    	  //if (sqlite.createDataBase())
	    	//		System.out.println("create database successfully\n");
	    	  /*
	    	  List<TumblrPost> posts = sqlite.select(TumblrPost.PHOTO_TYPE, null, "20");
	    	  
	          if (posts == null || posts.isEmpty()) {
	              Log.e(LOG_TAG, "Error, nothing is download");
	          }
	          
	          for (TumblrPost post : posts) {
	              Log.d(LOG_TAG, post.toString());
	          }
	          */
	      }
	  }
}
