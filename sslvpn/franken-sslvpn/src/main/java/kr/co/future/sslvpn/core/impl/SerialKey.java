package kr.co.future.sslvpn.core.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import kr.co.future.sslvpn.core.impl.SerialKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerialKey {
	private SerialKey() {
	}

	public static int getMaxTunnel() {
		BufferedReader br = null;
		FileInputStream is = null;
		try {
			File serial = new File("/proc/utm/serial");
			is = new FileInputStream(serial);
			br = new BufferedReader(new InputStreamReader(is));
			String line = br.readLine();

			// 'H' for sslplus and 'S' for software (secuwiz legacy)
			boolean hardware = line.charAt(11) == 'H';

			if (line.startsWith("WSS100"))
				return 100;
			else if (line.startsWith("WSS101"))
				return hardware ? 1000 : 250;
			else if (line.startsWith("WSS301"))
				return hardware ? 3000 : 1500;
			else if (line.startsWith("WSS501")) {
				// 원래 5000모델은 5000명을 반환하나
				// return hardware ? 5000 : 3700;
				return hardware ? 10000 : 3700;
			} else if (line.startsWith("WSS102"))
				return 10000;
			else if (line.startsWith("WSS302"))
				return 30000;
		} catch (FileNotFoundException e) {
			Logger logger = LoggerFactory.getLogger(SerialKey.class.getName());
			logger.error("frodo core: general linux, set max tunnel 10");
		} catch (Exception e) {
			Logger logger = LoggerFactory.getLogger(SerialKey.class.getName());
			logger.error("frodo core: serial fail, cannot set max tunnel", e);
		} finally {
         if (br != null){
            try {
                br.close();
            } catch (IOException e) {
            }
        }
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
            }
        }
		}

		return 100;
	}
}
