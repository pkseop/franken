/*
 * Copyright 2011 Future Systems
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.co.future.mail;

import java.util.Properties;

import kr.co.future.api.FieldOption;
import kr.co.future.confdb.CollectionName;

@CollectionName("mailer")
public class MailerConfig {
	@FieldOption(nullable = false)
	private String name;
	@FieldOption(nullable = false)
	private String protocol;
	@FieldOption(nullable = false)
	private String host;
	@FieldOption(nullable = false)
	private int port;
	@FieldOption(nullable = false)
	private String user;
	@FieldOption(nullable = false)
	private String password;
	@FieldOption(nullable = false)
	private boolean auth;
	@FieldOption(nullable = false)
	private boolean tls;
    @FieldOption(nullable = false)
    private String localhost;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isAuth() {
		return auth;
	}

	public void setAuth(boolean auth) {
		this.auth = auth;
	}

	public boolean isTls() {
		return tls;
	}

	public void setTls(boolean tls) {
		this.tls = tls;
	}

    public String getLocalhost() {
        return localhost;
    }

    public void setLocalhost(String localhost) {
        this.localhost = localhost;
    }

	public Properties getProperties() {
		Properties props = new Properties();
		props.put("mail.transport.protocol", protocol);
		props.put("mail.smtp.auth", Boolean.toString(auth));
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", Integer.toString(port));
		props.put("mail.smtp.user", user);
		props.put("mail.smtp.password", password);
		if(port == 587)
			props.put("mail.smtp.starttls.enable", Boolean.toString(tls));
		else if(port == 465) {
			props.put("mail.smtp.socketFactory.port", "465");
			props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		}
        props.put("mail.smtp.localhost", localhost);

		return props;
	}

	@Override
	public String toString() {
		return String.format("[%s]%s, %s:%d\tuser: %s", name, protocol, host, port, user);
	}
}
