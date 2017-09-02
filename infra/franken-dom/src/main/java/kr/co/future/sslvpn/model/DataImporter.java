package kr.co.future.sslvpn.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import kr.co.future.sslvpn.model.api.AuthorizedDeviceApi;

import org.json.JSONConverter;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import kr.co.future.api.PrimitiveConverter;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataImporter {
	public static void importData(ConfigService conf, String dbName, File cdbFile) throws IOException {
		Logger logger = LoggerFactory.getLogger(DataImporter.class.getName());
		InputStream is = null;
		try {
			ConfigDatabase db = conf.ensureDatabase(dbName);
			is = new FileInputStream(cdbFile);
			db.importData(is);
		} catch (IOException e) {
			logger.error("frodo model: cannot import data " + cdbFile.getAbsolutePath(), e);
			throw e;
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}
			cdbFile.delete();
		}
	}

	public static void importDevices(ConfigService conf, AuthorizedDeviceApi deviceApi, File deviceFile) throws IOException {
		Logger logger = LoggerFactory.getLogger(DataImporter.class.getName());
		InputStream is = null;
		try {
			is = new FileInputStream(deviceFile);
			JSONTokener t = new JSONTokener(new InputStreamReader(is, Charset.forName("utf-8")));
			List<AuthorizedDevice> devices = new ArrayList<AuthorizedDevice>();

			t.next(); // '['

			char c;
			while ((c = t.next()) != ']') {
				if (c != ',')
					t.back();

				AuthorizedDevice device = PrimitiveConverter.parse(AuthorizedDevice.class,
						JSONConverter.parse((JSONObject) t.nextValue()));
				logger.trace("frodo model: register device [{}]", device.toString());
				devices.add(device);
			}
			ConfigDatabase db = conf.ensureDatabase("frodo");
			db.dropCollection(AuthorizedDevice.class);
			db.ensureCollection(AuthorizedDevice.class);

			deviceApi.registerDevices(devices);
		} catch (IOException e) {
			logger.error("frodo model: cannot import file " + deviceFile.getAbsolutePath(), e);
			throw e;
		} catch (JSONException e) {
			logger.error("frodo model: cannot convert json to data");
			throw new IOException(e.getMessage());
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}

			deviceFile.delete();
		}
	}
}
