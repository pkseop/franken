package kr.co.future.sslvpn.xtmconf.alg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kr.co.future.msgbus.Marshalable;
import kr.co.future.msgbus.Marshaler;
import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class FtpProxy extends XtmConfig {
	private int num;
	private String adverFtpIp; // Public FTP Server IP
	private String ftpIp; // Real FTP Server IP
	private String iface; // 인터페이스
	private int port; // 포트번호
	private int maxUser; // 최대 동시 접속자수
	private int action; // 동작방식, 일반게이트 방식(0), 투명게이트 방식(1)
	private Permission upload; // 파일업로드
	private Permission download; // 파일다운로드
	private Boolean sizeUse; // ?
	private Integer size; // ?
	private boolean extensionUse; // 파일확장자 제한 (체크박스)
	private String extension; // 파일확장자 제한
	private boolean argumentUse; // 명령 인자 제한 (체크박스)
	private String argument; // 명령 인자 제한
	private List<User> user = new ArrayList<FtpProxy.User>(); // 사용자 허용/차단
	private boolean commandUse; // COMMAND 제한 (체크박스)
	private String command; // COMMAND 제한

	public static class User implements Marshalable {
		public static enum Type {
			ID, IP;

			public static Type get(String str) {
				for (Type t : Type.values()) {
					if (t.toString().equals(str))
						return t;
				}
				return null;
			}

			@Override
			public String toString() {
				return name().toLowerCase();
			}
		}

		private Type type; // Type
		private Permission permission; // ACTION
		private String name; // IP/ID

		public Type getType() {
			return type;
		}

		public void setType(Type type) {
			this.type = type;
		}

		public Permission getPermission() {
			return permission;
		}

		public void setPermission(Permission permission) {
			this.permission = permission;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public Map<String, Object> marshal() {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("type", type);
			m.put("permission", permission);
			m.put("name", name);

			return m;
		}
	}

	@Override
	public String getXmlFilename() {
		return "alg_ftp_proxy.xml";
	}

	@Override
	public String getRootTagName() {
		return "alg";
	}

	public static FtpProxy parse(NodeWrapper nw) {
		if (!nw.isName("ftp"))
			return null;

		FtpProxy fp = new FtpProxy();
		fp.num = nw.intAttr("num");

		for (NodeWrapper c : nw.children()) {
			if (c.isName("advertised_ftp_ip"))
				fp.adverFtpIp = c.value();
			else if (c.isName("ftp_ip"))
				fp.ftpIp = c.value();
			else if (c.isName("interface"))
				fp.iface = c.value();
			else if (c.isName("port"))
				fp.port = c.intValue();
			else if (c.isName("max_user"))
				fp.maxUser = c.intValue();
			else if (c.isName("action"))
				fp.action = c.intValue();
			else if (c.isName("upload"))
				fp.upload = c.boolAttr("chk_deny") ? Permission.Deny : Permission.Accept;
			else if (c.isName("download"))
				fp.download = c.boolAttr("chk_deny") ? Permission.Deny : Permission.Accept;
			else if (c.isName("size")) {
				fp.sizeUse = c.boolAttr("chk_use");
				fp.size = c.intValue();
			} else if (c.isName("extension")) {
				fp.extensionUse = c.boolAttr("chk_use");
				fp.extension = c.value();
			} else if (c.isName("argument")) {
				fp.argumentUse = c.boolAttr("chk_use");
				fp.argument = c.value();
			} else if (c.isName("user_list")) {
				for (NodeWrapper u : c.children()) {
					if (u.isName("user")) {
						User us = new User();
						us.type = User.Type.get(u.attr("type"));
						us.permission = Permission.get(u.attr("action"));
						us.name = u.value();
						fp.user.add(us);
					}
				}
			} else if (c.isName("command")) {
				fp.commandUse = c.boolAttr("chk_use");
				fp.command = c.value();
			}
		}

		return fp;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public String getAdverFtpIp() {
		return adverFtpIp;
	}

	public void setAdverFtpIp(String adverFtpIp) {
		this.adverFtpIp = adverFtpIp;
	}

	public String getFtpIp() {
		return ftpIp;
	}

	public void setFtpIp(String ftpIp) {
		this.ftpIp = ftpIp;
	}

	public String getIface() {
		return iface;
	}

	public void setIface(String iface) {
		this.iface = iface;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getMaxUser() {
		return maxUser;
	}

	public void setMaxUser(int maxUser) {
		this.maxUser = maxUser;
	}

	public int getAction() {
		return action;
	}

	public void setAction(int action) {
		this.action = action;
	}

	public Permission getUpload() {
		return upload;
	}

	public void setUpload(Permission upload) {
		this.upload = upload;
	}

	public Permission getDownload() {
		return download;
	}

	public void setDownload(Permission download) {
		this.download = download;
	}

	public Boolean isSizeUse() {
		return sizeUse;
	}

	public void setSizeUse(Boolean sizeUse) {
		this.sizeUse = sizeUse;
	}

	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	public boolean isExtensionUse() {
		return extensionUse;
	}

	public void setExtensionUse(boolean extensionUse) {
		this.extensionUse = extensionUse;
	}

	public String getExtension() {
		return extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

	public boolean isArgumentUse() {
		return argumentUse;
	}

	public void setArgumentUse(boolean argumentUse) {
		this.argumentUse = argumentUse;
	}

	public String getArgument() {
		return argument;
	}

	public void setArgument(String argument) {
		this.argument = argument;
	}

	public List<User> getUser() {
		return user;
	}

	public void setUser(List<User> user) {
		this.user = user;
	}

	public boolean isCommandUse() {
		return commandUse;
	}

	public void setCommandUse(boolean commandUse) {
		this.commandUse = commandUse;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("ftp");

		e.setAttribute("num", String.valueOf(num));
		appendChild(doc, e, "advertised_ftp_ip", adverFtpIp);
		appendChild(doc, e, "ftp_ip", ftpIp);
		appendChild(doc, e, "interface", iface);
		appendChild(doc, e, "port", port);
		appendChild(doc, e, "max_user", maxUser);
		appendChild(doc, e, "action", action);
		appendChild(doc, e, "upload", null, new AttributeBuilder("chk_deny", Permission.Deny.equals(upload)));
		appendChild(doc, e, "download", null, new AttributeBuilder("chk_deny", Permission.Deny.equals(download)));
		appendChild(doc, e, "size", size, new AttributeBuilder("chk_use", sizeUse));
		appendChild(doc, e, "extension", extension, new AttributeBuilder("chk_use", extensionUse));
		appendChild(doc, e, "argument", argument, new AttributeBuilder("chk_use", argumentUse));
		Element u = appendChild(doc, e, "user_list", null, new AttributeBuilder("count", user.size()));
		for (User us : user)
			appendChild(doc, u, "user", us.name, new AttributeBuilder("type", us.type).put("action", us.permission));
		appendChild(doc, e, "command", command, new AttributeBuilder("chk_use", commandUse));

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("num", num);
		m.put("advertised_ftp_id", adverFtpIp);
		m.put("ftp_ip", ftpIp);
		m.put("interface", iface);
		m.put("port", port);
		m.put("max_user", maxUser);
		m.put("action", action);
		m.put("upload", upload);
		m.put("download", download);
		m.put("size", new MarshalValue("use", sizeUse).put("value", size).get());
		m.put("extension", new MarshalValue("use", extensionUse).put("value", extension).get());
		m.put("argument", new MarshalValue("use", argumentUse).put("value", argument).get());
		m.put("user_list", Marshaler.marshal(user));
		m.put("command", new MarshalValue("use", commandUse).put("value", command).get());

		return m;
	}

	@Override
	public String toString() {
		return "FtpProxy [num=" + num + ", adverFtpIp=" + adverFtpIp + ", ftpIp=" + ftpIp + ", iface=" + iface
				+ ", port=" + port + ", maxUser=" + maxUser + ", action=" + action + ", upload=" + upload
				+ ", download=" + download + ", sizeUse=" + sizeUse + ", size=" + size + ", extensionUse="
				+ extensionUse + ", extension=" + extension + ", argumentUse=" + argumentUse + ", argument=" + argument
				+ ", user=" + user + ", commandUse=" + commandUse + ", command=" + command + "]";
	}
}
