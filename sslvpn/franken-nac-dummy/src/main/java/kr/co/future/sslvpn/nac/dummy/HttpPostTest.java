package kr.co.future.sslvpn.nac.dummy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpPostTest {

	private static OutputStream out = null;

	private static HttpURLConnection connect(String targetUrl) throws IOException {
		URL url = new URL(targetUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setUseCaches(false);
		connection.setConnectTimeout(5000);
		connection.setReadTimeout(100000);
		connection.setAllowUserInteraction(false);
		return connection;
	}

	private static void readContents(HttpURLConnection conn) {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				System.out.println(inputLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void sendParameter(HttpURLConnection conn, int index) {
		try {
			System.out.println("sending");
			out = conn.getOutputStream();
			String[] jsonFile = new String[] { "C:/TestJsonText/new  2.txt", "C:/TestJsonText/new  3.txt",
					"C:/TestJsonText/new  4.txt", "C:/TestJsonText/new  5.txt", "C:/TestJsonText/new  6.txt",
					"C:/TestJsonText/new  7.txt", "C:/TestJsonText/new  8.txt", "C:/TestJsonText/new  9.txt",
					"C:/TestJsonText/new  10.txt", "C:/TestJsonText/new  11.txt" };

			File fr = new File(jsonFile[index]);
			BufferedReader reader = new BufferedReader(new FileReader(fr));
			String json = "";
			String line = "";

			while ((line = reader.readLine()) != null) {
				json += line + "\n";

			}

			// System.out.println(json);
			byte[] bytes = json.getBytes("utf-8");
			out.write(bytes);
			out.flush();
			out.close();
			reader.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void sendParameter(HttpURLConnection conn, String filePath) {
		System.out.println("sending");
		try {
			out = conn.getOutputStream();
			File fr = new File(filePath);
			BufferedReader reader = new BufferedReader(new FileReader(fr));
			String json = "";
			String line = "";

			while ((line = reader.readLine()) != null) {
				json += line + "\n";
			}

			byte[] bytes = json.getBytes("utf-8");
			out.write(bytes);
			out.flush();
			out.close();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void main(String args[]) throws IOException {
		String targetUrl = "http://127.0.0.1/msgbus/request";

		HttpURLConnection conn = connect(targetUrl);
		sendParameter(conn, "C:/TestProfile2.txt");
		readContents(conn);
		conn.disconnect();

		// readContents(conn);

	}
}
