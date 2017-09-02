package kr.co.future.sslvpn.xtmconf.msgbus;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.model.api.UserApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;

import kr.co.future.dom.model.User;
import kr.co.future.msgbus.Marshaler;
import kr.co.future.msgbus.MsgbusException;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.filtering.FirewallIpv6;
import kr.co.future.sslvpn.xtmconf.filtering.FirewallIpv6Nat;
import kr.co.future.sslvpn.xtmconf.filtering.FirewallNat;
import kr.co.future.sslvpn.xtmconf.filtering.FirewallPolicy;
import kr.co.future.sslvpn.xtmconf.network.AnomalySpoofing;
import kr.co.future.sslvpn.xtmconf.object.IpAddress;
import kr.co.future.sslvpn.xtmconf.object.IpGroup;
import kr.co.future.sslvpn.xtmconf.object.Ipv6Address;
import kr.co.future.sslvpn.xtmconf.object.Ipv6Group;
import kr.co.future.sslvpn.xtmconf.object.Ipv6Header;
import kr.co.future.sslvpn.xtmconf.object.Qos;
import kr.co.future.sslvpn.xtmconf.object.Schedule;
import kr.co.future.sslvpn.xtmconf.object.ServiceGroup;
import kr.co.future.sslvpn.xtmconf.object.ServiceHttp;
import kr.co.future.sslvpn.xtmconf.object.ServicePort;
import kr.co.future.sslvpn.xtmconf.object.ServiceRpc;
import kr.co.future.sslvpn.xtmconf.object.Session;
import kr.co.future.sslvpn.xtmconf.object.UserGroup;
import kr.co.future.sslvpn.xtmconf.object.UserList;
import kr.co.future.sslvpn.xtmconf.object.Schedule.Period;
import kr.co.future.sslvpn.xtmconf.object.Schedule.Time;
import kr.co.future.sslvpn.xtmconf.object.ServiceHttp.Protocol;
import kr.co.future.sslvpn.xtmconf.object.ServiceHttp.Url;
import kr.co.future.sslvpn.xtmconf.object.ServicePort.IcmpType;
import kr.co.future.sslvpn.xtmconf.object.UserList.AuthType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-xtmconf-object-plugin")
@MsgbusPlugin
public class ObjectPlugin {
	@Requires
	private kr.co.future.dom.api.UserApi domUserApi;

	@Requires
	private UserApi userApi;

	private Logger logger = LoggerFactory.getLogger(ObjectPlugin.class);

	@MsgbusMethod
	public void getIpAddress(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(IpAddress.class)));
	}

	@MsgbusMethod
	public void addIpAddress(Request req, Response resp) {
		addObject(IpAddress.class, req);
	}

	@MsgbusMethod
	public void modifyIpAddress(Request req, Response resp) {
		modifyObject(IpAddress.class, req);
	}

	@MsgbusMethod
	public void removeIpAddress(Request req, Response resp) {
		removeObject(IpAddress.class, req.get("cid"));
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private void setIpAddress(IpAddress ia, Request req) {
		ia.setName(req.getString("name"));
		ia.setDesc(req.getString("desc"));
		ia.setIp((List<String>) req.get("ip"));
	}

	@MsgbusMethod
	public void getIpGroup(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(IpGroup.class)));
	}

	@MsgbusMethod
	public void addIpGroup(Request req, Response resp) {
		addObject(IpGroup.class, req);
	}

	@MsgbusMethod
	public void modifyIpGroup(Request req, Response resp) {
		modifyObject(IpGroup.class, req);
	}

	@MsgbusMethod
	public void removeIpGroup(Request req, Response resp) {
		removeObject(IpGroup.class, req.get("cid"));
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private void setIpGroup(IpGroup ig, Request req) {
		ig.setName(req.getString("name"));
		ig.setDesc(req.getString("desc"));
		ig.setMember((List<String>) req.get("member"));
	}

	@MsgbusMethod
	public void getIpv6Address(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Ipv6Address.class)));
	}

	@MsgbusMethod
	public void addIpv6Address(Request req, Response resp) {
		addObject(Ipv6Address.class, req);
	}

	@MsgbusMethod
	public void modifyIpv6Address(Request req, Response resp) {
		modifyObject(Ipv6Address.class, req);
	}

	@MsgbusMethod
	public void removeIpv6Address(Request req, Response resp) {
		removeObject(Ipv6Address.class, req.get("cid"));
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private void setIpv6Address(Ipv6Address ia, Request req) {
		ia.setName(req.getString("name"));
		ia.setDesc(req.getString("desc"));
		ia.setIp((List<String>) req.get("ip"));
	}

	@MsgbusMethod
	public void getIpv6Group(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Ipv6Group.class)));
	}

	@MsgbusMethod
	public void addIpv6Group(Request req, Response resp) {
		addObject(Ipv6Group.class, req);
	}

	@MsgbusMethod
	public void modifyIpv6Group(Request req, Response resp) {
		modifyObject(Ipv6Group.class, req);
	}

	@MsgbusMethod
	public void removeIpv6Group(Request req, Response resp) {
		removeObject(Ipv6Group.class, req.get("cid"));
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private void setIpv6Group(Ipv6Group ig, Request req) {
		ig.setName(req.getString("name"));
		ig.setDesc(req.getString("desc"));
		ig.setMember((List<String>) req.get("member"));
	}

	@MsgbusMethod
	public void getIpv6Header(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Ipv6Header.class)));
	}

	@MsgbusMethod
	public void addIpv6Header(Request req, Response resp) {
		addObject(Ipv6Header.class, req);
	}

	@MsgbusMethod
	public void modifyIpv6Header(Request req, Response resp) {
		modifyObject(Ipv6Header.class, req);
	}

	@MsgbusMethod
	public void removeIpv6Header(Request req, Response resp) {
		removeObject(Ipv6Header.class, req.get("cid"));
	}

	@SuppressWarnings("unused")
	private void setIpv6Header(Ipv6Header ih, Request req) {
		ih.setName(req.getString("name"));
		ih.setDesc(req.getString("desc"));
		ih.setHop(req.getBoolean("hop"));
		ih.setDest(req.getBoolean("dest"));
		ih.setAh(req.getBoolean("ah"));
		ih.setEsp(req.getBoolean("esp"));
		ih.setFragment(Utils.getBooleanFromMap(req.get("fragment"), "use"));
		ih.setFragmentSize(Utils.getIntegerFromMap(req.get("fragment"), "size"));
		ih.setRoute(Utils.getBooleanFromMap(req.get("route"), "use"));
		ih.setRouteAddress(ih.isRoute() ? Utils.getStringFromMap(req.get("route"), "type").equals("single") : null);
		ih.setRouteCid(Utils.getStringFromMap(req.get("route"), "cid"));
		ih.setRouteName(Utils.getStringFromMap(req.get("route"), "name"));
	}

	@MsgbusMethod
	public void getQos(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Qos.class)));
	}

	@MsgbusMethod
	public void addQos(Request req, Response resp) {
		addObject(Qos.class, req);
		resetQdiscid();
	}

	@MsgbusMethod
	public void modifyQos(Request req, Response resp) {
		modifyObject(Qos.class, req);
		resetQdiscid();
	}

	@MsgbusMethod
	public void removeQos(Request req, Response resp) {
		removeObject(Qos.class, req.get("cid"));
		resetQdiscid();
	}

	private void resetQdiscid() {
		List<Qos> l = XtmConfig.readConfig(Qos.class);
		for (Qos q : l)
			q.setQdiscid(q.getNum() + 9);

		XtmConfig.writeConfig(Qos.class, l);
	}

	@SuppressWarnings("unused")
	private void setQos(Qos q, Request req) {
		q.setName(req.getString("name"));
		q.setDesc(req.getString("desc"));
		q.setIface(req.getString("interface"));
		q.setMinBandwidth(Utils.getDoubleFromMap(req.get("bandwidth"), "min"));
		q.setMaxBandwidth(Utils.getDoubleFromMap(req.get("bandwidth"), "max"));
	}

	@MsgbusMethod
	public void getSchedule(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Schedule.class)));
	}

	@MsgbusMethod
	public void addSchedule(Request req, Response resp) {
		addObject(Schedule.class, req);
	}

	@MsgbusMethod
	public void modifySchedule(Request req, Response resp) {
		modifyObject(Schedule.class, req);
	}

	@MsgbusMethod
	public void removeSchedule(Request req, Response resp) {
		removeObject(Schedule.class, req.get("cid"));
	}

	@SuppressWarnings({ "unused", "unchecked" })
	private void setSchedule(Schedule s, Request req) {
		s.setName(req.getString("name"));
		s.setDesc(req.getString("desc"));
		s.setChkMon(Utils.getBooleanFromMap(req.get("week"), "mon"));
		s.setChkTue(Utils.getBooleanFromMap(req.get("week"), "tue"));
		s.setChkWed(Utils.getBooleanFromMap(req.get("week"), "wed"));
		s.setChkThu(Utils.getBooleanFromMap(req.get("week"), "thu"));
		s.setChkFri(Utils.getBooleanFromMap(req.get("week"), "fri"));
		s.setChkSat(Utils.getBooleanFromMap(req.get("week"), "sat"));
		s.setChkSun(Utils.getBooleanFromMap(req.get("week"), "sun"));
		List<Object> period = (List<Object>) req.get("period");
		s.getPeriod().clear();
		for (Object m : period) {
			Period p = new Period();
			p.setStart(Utils.getDateFromMap(m, "start"));
			p.setEnd(Utils.getDateFromMap(m, "end"));
			s.getPeriod().add(p);
		}
		List<Object> time = (List<Object>) req.get("time");
		s.getTime().clear();
		for (Object m : time) {
			Time t = new Time();
			t.setStart(Utils.getIntegerFromMap(m, "start"));
			t.setEnd(Utils.getIntegerFromMap(m, "end"));
			s.getTime().add(t);
		}
	}

	@MsgbusMethod
	public void getServiceGroup(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(ServiceGroup.class)));
	}

	@MsgbusMethod
	public void addServiceGroup(Request req, Response resp) {
		addObject(ServiceGroup.class, req);
	}

	@MsgbusMethod
	public void modifyServiceGroup(Request req, Response resp) {
		modifyObject(ServiceGroup.class, req);
	}

	@MsgbusMethod
	public void removeServiceGroup(Request req, Response resp) {
		removeObject(ServiceGroup.class, req.get("cid"));
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private void setServiceGroup(ServiceGroup sg, Request req) {
		sg.setName(req.getString("name"));
		sg.setDesc(req.getString("desc"));
		sg.setMember((List<String>) req.get("member"));
	}

	@MsgbusMethod
	public void getServiceHttp(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(ServiceHttp.class)));
	}

	@MsgbusMethod
	public void addServiceHttp(Request req, Response resp) {
		addObject(ServiceHttp.class, req);
	}

	@MsgbusMethod
	public void modifyServiceHttp(Request req, Response resp) {
		modifyObject(ServiceHttp.class, req);
	}

	@MsgbusMethod
	public void removeServiceHttp(Request req, Response resp) {
		removeObject(ServiceHttp.class, req.get("cid"));
	}

	@SuppressWarnings("unused")
	private void setServiceHttp(ServiceHttp sh, Request req) {
		sh.setName(req.getString("name"));
		sh.setDesc(req.getString("desc"));
		sh.setProtocol(Protocol.valueOf(req.getString("protocol")));
		sh.setSourceStart(Utils.getIntegerFromMap(req.get("source"), "start"));
		sh.setSourceEnd(Utils.getIntegerFromMap(req.get("source"), "end"));
		sh.setDestStart(Utils.getIntegerFromMap(req.get("dest"), "start"));
		sh.setDestEnd(Utils.getIntegerFromMap(req.get("dest"), "end"));
		sh.setContent(Utils.getStringFromMap(req.get("content"), "value"));
		sh.setContentRedirection(Utils.getStringFromMap(req.get("content"), "redirection"));
		sh.setGame(getUrl(Utils.getFromMap(req.get("url"), "game")));
		sh.setStock(getUrl(Utils.getFromMap(req.get("url"), "stock")));
		sh.setNews(getUrl(Utils.getFromMap(req.get("url"), "news")));
		sh.setItv(getUrl(Utils.getFromMap(req.get("url"), "itv")));
		sh.setEmail(getUrl(Utils.getFromMap(req.get("url"), "email")));
		sh.setWebhard(getUrl(Utils.getFromMap(req.get("url"), "webhard")));
		sh.setP2p(getUrl(Utils.getFromMap(req.get("url"), "p2p")));
		sh.setUser(getUrl(Utils.getFromMap(req.get("url"), "user")));
		sh.setSiteKiscom(Utils.getBooleanFromMap(req.get("site"), "kiscom"));
		sh.setSiteSafenet(Utils.getBooleanFromMap(req.get("site"), "safenet"));
		sh.setSiteYouth(Utils.getBooleanFromMap(req.get("site"), "youth"));
		sh.setSiteRedirection(Utils.getStringFromMap(req.get("site"), "redirection"));
		sh.setOptionLanguage(Utils.getIntegerFromMap(req.get("option"), "language"));
		sh.setOptionNude(Utils.getIntegerFromMap(req.get("option"), "nude"));
		sh.setOptionSex(Utils.getIntegerFromMap(req.get("option"), "sex"));
		sh.setOptionViolence(Utils.getIntegerFromMap(req.get("option"), "violence"));
		sh.setOptionEtc1(Utils.getBooleanFromMap(req.get("option"), "etc1"));
		sh.setOptionEtc2(Utils.getBooleanFromMap(req.get("option"), "etc2"));
	}

	private Url getUrl(Object map) {
		boolean use = Utils.getBooleanFromMap(map, "use");
		String redirect = Utils.getStringFromMap(map, "redirect");
		return new Url(use, redirect);
	}

	@MsgbusMethod
	public void getServicePort(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(ServicePort.class)));
	}

	@MsgbusMethod
	public void addServicePort(Request req, Response resp) {
		addObject(ServicePort.class, req);
	}

	@MsgbusMethod
	public void modifyServicePort(Request req, Response resp) {
		modifyObject(ServicePort.class, req);
	}

	@MsgbusMethod
	public void removeServicePort(Request req, Response resp) {
		removeObject(ServicePort.class, req.get("cid"));
	}

	@SuppressWarnings("unused")
	private void setServicePort(ServicePort sp, Request req) {
		sp.setName(req.getString("name"));
		sp.setDesc(req.getString("desc"));
		sp.setProtocol(ServicePort.Protocol.valueOf(Utils.getStringFromMap(req.get("protocol"), "type")));
		sp.setIcmp(IcmpType.get(Utils.getStringFromMap(req.get("protocol"), "icmp")));
		sp.setFtp(Utils.getBooleanFromMap(req.get("protocol"), "ftp"));
		sp.setSourceStart(Utils.getIntegerFromMap(req.get("source"), "start"));
		sp.setSourceEnd(Utils.getIntegerFromMap(req.get("source"), "end"));
		sp.setDestStart(Utils.getIntegerFromMap(req.get("dest"), "start"));
		sp.setDestEnd(Utils.getIntegerFromMap(req.get("dest"), "end"));
	}

	@MsgbusMethod
	public void getServiceRpc(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(ServiceRpc.class)));
	}

	@MsgbusMethod
	public void addServiceRpc(Request req, Response resp) {
		addObject(ServiceRpc.class, req);
	}

	@MsgbusMethod
	public void modifyServiceRpc(Request req, Response resp) {
		modifyObject(ServiceRpc.class, req);
	}

	@MsgbusMethod
	public void removeServiceRpc(Request req, Response resp) {
		removeObject(ServiceRpc.class, req.get("cid"));
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private void setServiceRpc(ServiceRpc sr, Request req) {
		sr.setName(req.getString("name"));
		sr.setDesc(req.getString("desc"));
		sr.setProtocol(ServiceRpc.Protocol.valueOf(req.getString("protocol")));
		sr.setSourceStart(Utils.getIntegerFromMap(req.get("source"), "start"));
		sr.setSourceEnd(Utils.getIntegerFromMap(req.get("source"), "end"));
		sr.setDestStart(Utils.getIntegerFromMap(req.get("dest"), "start"));
		sr.setDestEnd(Utils.getIntegerFromMap(req.get("dest"), "end"));
		sr.setRpc((List<Integer>) req.get("rpc"));
	}

	@MsgbusMethod
	public void getSession(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Session.class)));
	}

	@MsgbusMethod
	public void addSession(Request req, Response resp) {
		addObject(Session.class, req);
	}

	@MsgbusMethod
	public void modifySession(Request req, Response resp) {
		modifyObject(Session.class, req);
	}

	@MsgbusMethod
	public void removeSession(Request req, Response resp) {
		removeObject(Session.class, req.get("cid"));
	}

	@SuppressWarnings("unused")
	private void setSession(Session s, Request req) {
		s.setName(req.getString("name"));
		s.setDesc(req.getString("desc"));
		s.setAction(req.getInteger("action"));
		s.setLimit(req.getInteger("limit"));
	}

	@MsgbusMethod
	public void getUserGroup(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(UserGroup.class)));
	}

	@MsgbusMethod
	public void addUserGroup(Request req, Response resp) {
		addObject(UserGroup.class, req);
	}

	@MsgbusMethod
	public void modifyUserGroup(Request req, Response resp) {
		modifyObject(UserGroup.class, req);
	}

	@MsgbusMethod
	public void removeUserGroup(Request req, Response resp) {
		removeObject(UserGroup.class, req.get("cid"));
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private void setUserGroup(UserGroup ug, Request req) {
		ug.setName(req.getString("name"));
		ug.setDesc(req.getString("desc"));
		ug.setMember((List<String>) req.get("member"));
	}

	@MsgbusMethod
	public void getUserList(Request req, Response resp) {
		List<UserList> config = new ArrayList<UserList>();
		for (User user : domUserApi.getUsers("localhost")) {
			UserExtension ext = userApi.getUserExtension(user);
			boolean radius = user.getExt().containsKey("radius");
			if (ext == null) {
				ext = new UserExtension();
				ext.setUser(user);
				userApi.setUserExtension(ext);
			}

			UserList userList = new UserList();
			userList.setCid(ext.getCid());
			userList.setAuthType(radius ? AuthType.Radius : AuthType.Local);
			userList.setModeAllow(ext.isLocked());
			userList.setName(user.getName());
			userList.setDesc(user.getDescription());
			userList.setId(user.getLoginName());
			userList.setPassword(user.getPassword());
			userList.setSalt(user.getSalt());
			userList.setGroupCid("");
			config.add(userList);
		}
		Utils.setConfigNum(config);
		resp.put("config", Marshaler.marshal(config));
	}

	private <T extends XtmConfig> void addObject(Class<T> cls, Request req) {
		T t = null;
		try {
			t = cls.newInstance();
			cls.getMethod("setCid", String.class).invoke(t, UUID.randomUUID().toString());
			Method method = this.getClass().getDeclaredMethod("set" + cls.getSimpleName(), cls, Request.class);
			method.setAccessible(true);
			method.invoke(this, t, req);

			List<T> config = XtmConfig.readConfig(cls);

			String newName = req.getString("name");

			for (T c : config) {
				Field field = c.getClass().getDeclaredField("name");
				field.setAccessible(true);
				String name = (String) field.get(c);
				if (name.equals(newName))
					throw new MsgbusException("frodo", "duplicated-name");
			}

			config.add(t);
			Utils.setConfigNum(config);
			XtmConfig.writeConfig(cls, config);

		} catch (MsgbusException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private <T extends XtmConfig> void modifyObject(Class<T> cls, Request req) {
		String cid = req.getString("cid");
		String name = req.getString("name");

		FirewallIpv6.updateCid(cid, name);
		FirewallIpv6Nat.updateCid(cid, name);
		FirewallNat.updateCid(cid, name);
		FirewallPolicy.updateCid(cid, name);
		Ipv6Header.updateCid(cid, name);

		List<T> config = XtmConfig.readConfig(cls);
		try {
			for (T t : config) {
				if (cid.equals(t.getClass().getMethod("getCid").invoke(t))) {
					Method method = this.getClass().getDeclaredMethod("set" + cls.getSimpleName(), cls, Request.class);
					method.setAccessible(true);
					method.invoke(this, t, req);
				}
			}

			for (T c : config) {
				Field field = c.getClass().getDeclaredField("name");
				field.setAccessible(true);
				String fieldName = (String) field.get(c);

				field = c.getClass().getDeclaredField("cid");
				field.setAccessible(true);
				String fieldCid = (String) field.get(c);

				if (!fieldCid.equals(cid) && fieldName.equals(name))
					throw new MsgbusException("frodo", "duplicated-name");
			}

			XtmConfig.writeConfig(cls, config);
		} catch (MsgbusException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private boolean causedBy(boolean use, Set<String> causes, String symbol) {
		if (use)
			causes.add(symbol);

		return use;
	}

	private String join(Set<String> causes, String sep) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (String cause : causes) {
			if (i++ != 0)
				sb.append(sep);
			sb.append(cause);
		}

		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private <T extends XtmConfig> void removeObject(Class<T> cls, Object ids) {
		Set<String> causes = new HashSet<String>();
		List<String> cids = (List<String>) ids;
		for (String cid : cids) {
			boolean using = false;
			using |= causedBy(AnomalySpoofing.hasCid(cid), causes, "anomaly-spoofing");
			using |= causedBy(FirewallIpv6.hasCid(cid), causes, "firewall-ipv6");
			using |= causedBy(FirewallIpv6Nat.hasCid(cid), causes, "firewall-ipv6-nat");
			using |= causedBy(FirewallNat.hasCid(cid), causes, "firewall-nat");
			using |= causedBy(FirewallPolicy.hasCid(cid), causes, "firewall-policy");
			using |= causedBy(IpGroup.hasCid(cid), causes, "ip-group");
			using |= causedBy(Ipv6Group.hasCid(cid), causes, "ipv6-group");
			using |= causedBy(Ipv6Header.hasCid(cid), causes, "ipv6-header");
			using |= causedBy(ServiceGroup.hasCid(cid), causes, "service-group");
			using |= causedBy(UserGroup.hasCid(cid), causes, "user-group");
			if (using)
				throw new MsgbusException("frodo", "occupied cid: " + join(causes, ","));
		}

		List<T> config = XtmConfig.readConfig(cls);
		List<T> objs = new ArrayList<T>();
		try {
			for (T t : config) {
				if (cids.contains(t.getClass().getMethod("getCid").invoke(t)))
					objs.add(t);
			}
			for (T obj : objs)
				config.remove(obj);

			Utils.setConfigNum(config);
			XtmConfig.writeConfig(cls, config);
		} catch (Exception e) {
			logger.error("frood-xtmconf: error in remove object.", e);
			if (e instanceof NullPointerException)
				throw (NullPointerException) e;
		}
	}
}
