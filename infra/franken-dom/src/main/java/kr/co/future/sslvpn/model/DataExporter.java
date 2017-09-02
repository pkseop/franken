package kr.co.future.sslvpn.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONWriter;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataExporter {
	public static void exportData(ConfigDatabase db, File cdbFile) throws IOException {
		Logger logger = LoggerFactory.getLogger(DataExporter.class.getName());
		OutputStream os = null;
		try {
			os = new FileOutputStream(cdbFile);
			db.exportData(os);
		} catch (IOException e) {
			logger.error("frodo model: cannot export data " + cdbFile.getAbsolutePath(), e);
			throw e;
		} finally {
			if (os != null)
				try {
					os.close();
				} catch (IOException e) {
				}
		}
	}

	public static void exportDevices(ConfigDatabase db, File deviceFile) throws IOException {
		Logger logger = LoggerFactory.getLogger(DataExporter.class.getName());
		OutputStream os = null;
		OutputStreamWriter writer = null;
		JSONWriter jw = null;
		ConfigIterator it = null;

		try {
			it = db.findAll(AuthorizedDevice.class);

			os = new FileOutputStream(deviceFile);
			writer = new OutputStreamWriter(os, Charset.forName("utf-8"));
			jw = new JSONWriter(writer);
			jw.array();

			while (it.hasNext()) {
				jw.object();
				Config c = it.next();
				@SuppressWarnings("unchecked")
				Map<String, Object> device = (Map<String, Object>) c.getDocument();

				for (String key : device.keySet()) {
					jw.key(key);
					jw.value(device.get(key));
				}
				jw.endObject();
			}

			jw.endArray();
			writer.flush();
		} catch (IOException e) {
			logger.error("frodo model: cannot export file " + deviceFile.getAbsolutePath(), e);
			throw e;
		} catch (JSONException e) {
			logger.error("frodo model: cannot convert data to json");
			throw new IOException(e);
		} finally {
			if (os != null)
				try {
					os.close();
				} catch (IOException e) {
				}
			if (it != null)
				it.close();
		}
	}
}
