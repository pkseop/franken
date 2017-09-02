package kr.co.future.sslvpn.core;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import kr.co.future.api.ScriptContext;

public class Config {
	public static String internalInterfaceName = "tun0";
	public static int externalServicePort = 41000;
	public static int internalServicePort = 41001;

	public static class Cert {
		public static String privateCAName = "local";
		public static String privateCAPassword = "kraken";
		public static String caPrivKeyPass = "kraken";
		public static String caKeystorePass = "kraken";
		public static String caDistinguishedName = "CN=local, OU=RND, O=FutureSystems, L=Guro, ST=Seoul, C=KR";
		public static String caCommonName = "local";
		public static int certDurationDefault = 365;
	}

	public static class IceSSL {
		public static String DefaultDir = new File(System.getProperty("kraken.dir")).getAbsolutePath();
		public static String KeyPassword = "1234";
		public static String Keystore = "clientadapter.jks";
		public static String KeystorePassword = "123456";
		public static String TrustStore = "CA.jks";
		public static String TrustStorePassword = "123456";
	}

	public static void save() {
	}

	public static void load() {
	}

	public static void print(Class<?> targetClass, ScriptContext context) {
		Class<?> r = targetClass;
		Field[] fields = r.getDeclaredFields();
		for (Field field : fields) {
			if (Modifier.isStatic(field.getModifiers())) {
				try {
					Object object = field.get(null);
					context.printf("%s: %s\n", field.getName(), object.toString());
				} catch (IllegalArgumentException e) {
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					context.println(sw.toString());
				} catch (IllegalAccessException e) {
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					context.println(sw.toString());
				}
			}
		}
	}

	public static void set(Class<?> targetClass, String varName, String val) throws SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException {
		Field declaredField = targetClass.getDeclaredField(varName);
		if (declaredField != null)
			declaredField.set(null, val);
	}
}
