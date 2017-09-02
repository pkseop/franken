package kr.co.future.sslvpn.xtmconf.msgbus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.felix.ipojo.annotations.Component;

import kr.co.future.msgbus.Marshaler;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.waf.Basic;
import kr.co.future.sslvpn.xtmconf.waf.Dir;
import kr.co.future.sslvpn.xtmconf.waf.Header;
import kr.co.future.sslvpn.xtmconf.waf.Http;
import kr.co.future.sslvpn.xtmconf.waf.Injection;
import kr.co.future.sslvpn.xtmconf.waf.Outflow;
import kr.co.future.sslvpn.xtmconf.waf.Pattern;
import kr.co.future.sslvpn.xtmconf.waf.WafObject;
import kr.co.future.sslvpn.xtmconf.waf.Webserver;
import kr.co.future.sslvpn.xtmconf.waf.Pattern.Action;

@Component(name = "frodo-xtmconf-waf-plugin")
@MsgbusPlugin
public class WafPlugin {
	@MsgbusMethod
	public void getBasic(Request req, Response resp) {
		getWafObject(Basic.class, req, resp);
	}

	@MsgbusMethod
	public void modifyBasic(Request req, Response resp) {
		modifyWafObject(Basic.class, req);
	}

	@MsgbusMethod
	public void getDir(Request req, Response resp) {
		getWafObject(Dir.class, req, resp);
	}

	@MsgbusMethod
	public void modifyDir(Request req, Response resp) {
		modifyWafObject(Dir.class, req);
	}

	@MsgbusMethod
	public void getHeader(Request req, Response resp) {
		getWafObject(Header.class, req, resp);
	}

	@MsgbusMethod
	public void modifyHeader(Request req, Response resp) {
		modifyWafObject(Header.class, req);
	}

	@MsgbusMethod
	public void getHttp(Request req, Response resp) {
		getWafObject(Http.class, req, resp);
	}

	@MsgbusMethod
	public void modifyHttp(Request req, Response resp) {
		modifyWafObject(Http.class, req);
	}

	@MsgbusMethod
	public void getInjection(Request req, Response resp) {
		getWafObject(Injection.class, req, resp);
	}

	@MsgbusMethod
	public void modifyInjection(Request req, Response resp) {
		modifyWafObject(Injection.class, req);
	}

	@MsgbusMethod
	public void getOutflow(Request req, Response resp) {
		getWafObject(Outflow.class, req, resp);
	}

	@MsgbusMethod
	public void modifyOutflow(Request req, Response resp) {
		modifyWafObject(Outflow.class, req);
	}

	private <T extends WafObject> void getWafObject(Class<T> cls, Request req, Response resp) {
		String cid = req.getString("cid");

		List<T> config = XtmConfig.readConfig(cls);
		T obj = null;
		for (T wo : config) {
			if (cid.equals(wo.getCid()))
				obj = wo;
		}

		resp.put("config", (obj != null) ? obj.marshal() : null);
	}

	@SuppressWarnings("unchecked")
	private <T extends WafObject> void modifyWafObject(Class<T> cls, Request req) {
		String cid = req.getString("cid");
		List<T> config = XtmConfig.readConfig(cls);
		T obj = null;
		for (T b : config) {
			if (cid.equals(b.getCid()))
				obj = b;
		}

		try {
			if (obj == null) {
				obj = cls.newInstance();
				obj.setCid(cid);
				config.add(obj);
			}
		} catch (Exception e) {
		}

		obj.getPattern().clear();
		List<Object> pattern = (List<Object>) req.get("pattern");
		for (Object p : pattern) {
			try {
				Pattern pa = getPattern(p);
				if (((List<Integer>) cls.getMethod("getIds").invoke(null)).contains(pa.getId()))
					obj.getPattern().add(pa);
			} catch (Exception e) {
			}
		}

		XtmConfig.writeConfig(cls, config);
	}

	private void removeWafObject(List<String> cids) {
		removeWafObjectInternal(Basic.class, cids);
		removeWafObjectInternal(Dir.class, cids);
		removeWafObjectInternal(Header.class, cids);
		removeWafObjectInternal(Http.class, cids);
		removeWafObjectInternal(Injection.class, cids);
		removeWafObjectInternal(Outflow.class, cids);
	}

	private <T extends WafObject> void removeWafObjectInternal(Class<T> cls, List<String> cids) {
		List<T> config = XtmConfig.readConfig(cls);
		List<T> objs = new ArrayList<T>();
		for (T wo : config) {
			if (cids.contains(wo.getCid()))
				objs.add(wo);
		}
		for (T obj : objs)
			config.remove(obj);

		XtmConfig.writeConfig(cls, config);
	}

	@MsgbusMethod
	public void getWebserver(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Webserver.class)));
	}

	@MsgbusMethod
	public void addWebserver(Request req, Response resp) {
		Webserver w = new Webserver();
		w.setCid(UUID.randomUUID().toString());
		setWebserver(w, req);

		List<Webserver> config = XtmConfig.readConfig(Webserver.class);
		config.add(w);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(Webserver.class, config);
	}

	@MsgbusMethod
	public void modifyWebserver(Request req, Response resp) {
		String cid = req.getString("cid");

		List<Webserver> config = XtmConfig.readConfig(Webserver.class);
		for (Webserver w : config) {
			if (cid.equals(w.getCid()))
				setWebserver(w, req);
		}
		XtmConfig.writeConfig(Webserver.class, config);
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void removeWebserver(Request req, Response resp) {
		List<String> cids = (List<String>) req.get("cid");

		List<Webserver> config = XtmConfig.readConfig(Webserver.class);
		List<Webserver> objs = new ArrayList<Webserver>();
		for (Webserver w : config) {
			if (cids.contains(w.getCid()))
				objs.add(w);
		}
		for (Webserver obj : objs)
			config.remove(obj);
		Utils.setConfigNum(config);
		XtmConfig.writeConfig(Webserver.class, config);

		removeWafObject(cids);
	}

	private void setWebserver(Webserver w, Request req) {
		w.setName(req.getString("name"));
		w.setIp(req.getString("ip"));
		w.setDomain(req.getString("domain"));
		w.setPort(req.getString("port"));
		w.setWa(req.getBoolean("wa"));
	}

	private Pattern getPattern(Object map) {
		Pattern p = new Pattern();
		p.setId(Utils.getIntegerFromMap(map, "id"));
		p.setAction(Action.get(Utils.getStringFromMap(map, "action")));
		p.setPriority(Utils.getIntegerFromMap(map, "priority"));
		p.setUse(Utils.getBooleanFromMap(map, "use"));
		p.setValue(Utils.getStringFromMap(map, "value"));
		return p;
	}
}
