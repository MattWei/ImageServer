package com.mj.imageserver;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.mj.imagedownloader.DownloadThread;
import com.mj.imagedownloader.Log;
import com.mj.imagedownloader.SqliteManager;
import com.mj.imagedownloader.TumblrPost;


/**
 * Servlet implementation class ImageServer
 */
@WebServlet("/ImageServer")
public class ImageServer extends HttpServlet {
	private static final String LOG_TAG = "ImageServer";

	private static final long serialVersionUID = 1L;

	private static boolean mIsDownloadThreadStarted = false;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ImageServer() {
		super();
		// TODO Auto-generated constructor stub
	}

	public void init() throws ServletException {

		if (!mIsDownloadThreadStarted) {
			Log.e(LOG_TAG, "Start download thread\n");

			mIsDownloadThreadStarted = true;

			String saveDir = getServletContext().getRealPath("/") + "tumblr/";
			if (!SqliteManager.isCreated(saveDir)) {
				Log.d(LOG_TAG, "Create Database\n");
				if (!SqliteManager.getInstance().createDataBase()) {
					Log.d(LOG_TAG, "Create Database false\n");
					return;
				}
			} else {
				Log.i(LOG_TAG, "Database have created\n");
			}

			
			DownloadThread imageThread = new DownloadThread(
					SqliteManager.getInstance(), 
					DownloadThread.DOWNLOAD_BASH_BOARD,
					new String[] {saveDir, TumblrPost.PHOTO_TYPE, "1000"}
					);
			
			DownloadThread videoThread = new DownloadThread(
					SqliteManager.getInstance(), 
					DownloadThread.DOWNLOAD_BASH_BOARD,
					new String[] {saveDir, TumblrPost.VIDEO_TYPE, "200"}
					);
			
			imageThread.start();
			videoThread.start();
			
			
		}

	}

	public class JsonRespond {
		private Long miniId = 0L;
		private Long maxId = 0L;
		private String html = null;
		
		public Long getMiniId() {
			return miniId;
		}

		public void setMiniId(Long miniId) {
			this.miniId = miniId;
		}

		public Long getMaxId() {
			return maxId;
		}

		public void setMaxId(Long maxId) {
			this.maxId = maxId;
		}

		public String getHtml() {
			return html;
		}

		public void setHtml(String html) {
			this.html = html;
		}

	};

	private String parseToImageHtml(TumblrPost post) {
		String imagePath = post.getPath().replaceFirst(getServletContext().getRealPath("/"), "");
		
		String tableImage = "<a class=\"tableItem\" id=\"tablePhoto" + post.getId()  + "\" href=\"" + imagePath  + 
				"\" rel=\"prettyPhoto\"" +
				" data-rel=\"popup\" data-position-to=\"window\" class=\"ui-btn ui-corner-all ui-shadow ui-btn-inline\">" +
				"<img src=\""+ imagePath + 
				"\" alt=\"Mountain View\" style=\"width:100px;height:100px\">"
				+ "</a>";
		
		return tableImage;
	}
	
	private String parseToVideoHtml(TumblrPost post) {
		String videoHref = post.getPath().replaceFirst(getServletContext().getRealPath("/"), "");
		String thumbnailPath = post.getThumbnail().getPath().replaceFirst(getServletContext().getRealPath("/"), "");
		
		String tableImage = "<a class=\"tableItem\" id=\"tableItem" + post.getId()  + "\" href=\"" + videoHref  + 
				"\" rel=\"prettyPhoto\"" +
				" data-rel=\"popup\" data-position-to=\"window\" class=\"ui-btn ui-corner-all ui-shadow ui-btn-inline\">" +
				"<img src=\""+ thumbnailPath + 
				"\" alt=\"Mountain View\" style=\"width:100px;height:100px\">"
				+ "</a>";
		
		return tableImage;
	}
	
	private JsonRespond getImageHtml(String request, String type, String sinceId, String limit) {
		String imageTable = "";
		JsonRespond json = new JsonRespond();
		
		SqliteManager sqlManager = SqliteManager.getInstance();
		List<TumblrPost> posts = sqlManager.select(request, type, sinceId, limit);

		if (posts == null || posts.isEmpty()) {
			Log.e(LOG_TAG, "Error, nothing is download");
			imageTable = "<tr><td>Nothing to show</td></tr>";
			json.setMiniId(0L);
			json.setMaxId(0L);
			json.setHtml(imageTable);
		} else {
			TumblrPost post = null;
			int j = 0;
			for (int i = 0; i < posts.size(); i++) {
				post = posts.get(i);
				j = (i % 3);
				if (j == 0) {
					imageTable += "<tr> \n";
				}

				imageTable += "<td class=\"image-td\">"
						+ (post.getType().equals(TumblrPost.PHOTO_TYPE) ? 
								parseToImageHtml(post) : parseToVideoHtml(post))
						+ "</td>\n";

				if (j == 2) {
					imageTable += "</tr> \n";
				}
			}
			
			json.setMiniId(posts.get(posts.size() - 1).getId());
			json.setMaxId(posts.get(0).getId());
			json.setHtml(imageTable);
		}

		return json;
	}

	
	private JsonRespond getErrorTypeHtml() {
		JsonRespond json = new JsonRespond();
		json.setMiniId(0L);
		json.setMaxId(0L);
		json.setHtml("<b>Error Type</b>");
		
		return json;
	}
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String requestType = (request.getParameter("request_type") == null) ? "get"
				: request.getParameter("request_type");
		String type = (request.getParameter("data_type") == null) ? "photo"
				: request.getParameter("data_type");
		String sinceId = (request.getParameter("since_id") == null) ? "0"
				: request.getParameter("since_id");
		String limit = (request.getParameter("limit") == null) ? "21" : request
				.getParameter("limit");

		Log.d(LOG_TAG, "requeType=" + requestType + " type=" + type + " sinceId=" + sinceId + " limit=" + limit);
		// System.out.println(getServletContext().getRealPath("/"));
		JsonRespond jsonRespond = null;
		if (type.equals(TumblrPost.PHOTO_TYPE) || type.equals(TumblrPost.VIDEO_TYPE)) {
			jsonRespond = getImageHtml(requestType, type, sinceId,limit);
		} else {
			jsonRespond = getErrorTypeHtml();
		}

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(new Gson().toJson(jsonRespond));

		// response.setContentType("text/html");
		// PrintWriter out = response.getWriter();
		// out.println(imageTable);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}


}
