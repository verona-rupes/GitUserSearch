import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSONObjectException;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * Search through git hub for users matching skill set and location
 * https://developer.github.com/v3/
 *
 * @author RAFA
 *
 */
public class GitUserSearch {
	private static List<String> userPageLinks = new ArrayList<String>();
	private static CloseableHttpClient httpclient = null;
	private static ListMultimap<String, String> multimap = ArrayListMultimap.create();
	// CHANGE- path to write a CSV file
	private static File csvFile = new File("/Users/developers.csv");
	private static int pageSize = 100;
	// CHANGE- userId and pwd for github login
	private static String auth = "USER_NAME" + ":" + "PASSWORD";

	public static void main(String[] args) throws Exception {
		try {
			httpclient = HttpClients.createDefault();
			getUserPageLink();
			getEachUserDetail();
		} finally {
			httpclient.close();
		}
		System.out.println("Done pulling ");
		for (String key : multimap.keySet()) {
			List<String> user = multimap.get(key);
			FileUtils.writeStringToFile(csvFile, StringUtils.join(user, ','), true);
			FileUtils.writeStringToFile(csvFile, StringUtils.LF, true);
		}
		System.out.println("Done writing!! ");
	}

	private static void getUserPageLink() {
		int currentPage = 0;
		int totalCount = 0;
		do {
			currentPage++;
			String gitQuery = "https://api.github.com/search/users?q=language%3AC%23+location%3AOhio&type=Users&sort=created&order=desc&per_page=100&page="
					+ currentPage;
			CloseableHttpResponse response1 = null;
			try {
				HttpGet httpGet = new HttpGet(gitQuery);
				response1 = httpclient.execute(httpGet);
				System.out.println(response1.getStatusLine());
				HttpEntity entity = response1.getEntity();
				String jsonStr = EntityUtils.toString(entity);
				Map<Object, Object> json = JSON.std.mapFrom(jsonStr);
				totalCount = (Integer) json.get("total_count");
				boolean incomplete = (Boolean) json.get("incomplete_results");
				System.out.println("total_count:" + totalCount);
				System.out.println("incomplete:" + incomplete);
				getUserList(jsonStr);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					response1.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} while (getNextPage(totalCount, currentPage));
	}

	private static void getUserList(final String jsonStr) {
		try {
			Map<Object, Object> json = JSON.std.mapFrom(jsonStr);
			List<Map<String, String>> items = (List<Map<String, String>>) json.get("items");
			int i = 0;
			for (Map<String, String> one : items) {
				String url = one.get("url");
				System.out.println(i + ":" + url);
				userPageLinks.add(url);
				i++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void getEachUserDetail() {
		String jsonStr = null;
		CloseableHttpResponse response = null;
		try {
			byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
			String authHeader = "Basic " + new String(encodedAuth);
			for (String url : userPageLinks) {
				HttpGet httpGet = new HttpGet(url);
				httpGet.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
				response = httpclient.execute(httpGet);
				System.out.println("getEachUserDetail:" + response.getStatusLine());
				HttpEntity entity1 = response.getEntity();
				jsonStr = EntityUtils.toString(entity1);
				extractUserDetails(jsonStr);
				response.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void extractUserDetails(String jsonStr) throws IOException, JSONObjectException {
		Map<Object, Object> json = JSON.std.mapFrom(jsonStr);
		String login = (String) json.get("login");
		String html_url = (String) json.get("html_url");
		String name = (String) json.get("name");
		String blog = (String) json.get("blog");
		String location = (String) json.get("location");
		String email = (String) json.get("email");
		String hire = String.valueOf(json.get("hireable"));
		String bio = (String) json.get("bio");
		String created_at = (String) json.get("created_at");
		multimap.put(login, "\"" + StringUtils.defaultString(login, "") + "\"");
		multimap.put(login, "\"" + StringUtils.defaultString(name, "") + "\"");
		multimap.put(login, "\"" + StringUtils.defaultString(html_url, "") + "\"");
		multimap.put(login, "\"" + StringUtils.defaultString(blog, "") + "\"");
		multimap.put(login, "\"" + StringUtils.defaultString(email, "") + "\"");
		multimap.put(login, "\"" + StringUtils.defaultString(location, "") + "\"");
		multimap.put(login, "\"" + StringUtils.defaultString(hire, "") + "\"");
		multimap.put(login, "\"" + StringUtils.defaultString(bio, "") + "\"");
		multimap.put(login, "\"" + StringUtils.defaultString(created_at, "") + "\"");
	}

	private static boolean getNextPage(int totalCount, int currentPage) {
		boolean nextPage = false;
		int totalPages = (totalCount / pageSize);
		int lastPage = (totalCount % pageSize);
		if (lastPage > 0) {
			totalPages++;
		}
		if (currentPage < totalPages) {
			nextPage = true;
		}
		return nextPage;
	}
}
