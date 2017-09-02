package kr.co.future.sslvpn.xtmconf.msgbus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.ipojo.annotations.Component;

import kr.co.future.msgbus.Marshaler;
import kr.co.future.msgbus.Request;
import kr.co.future.msgbus.Response;
import kr.co.future.msgbus.handler.MsgbusMethod;
import kr.co.future.msgbus.handler.MsgbusPlugin;
import kr.co.future.sslvpn.xtmconf.Utils;
import kr.co.future.sslvpn.xtmconf.XtmConfig;
import kr.co.future.sslvpn.xtmconf.anti.Smtp;
import kr.co.future.sslvpn.xtmconf.anti.Spam;
import kr.co.future.sslvpn.xtmconf.anti.Virus;
import kr.co.future.sslvpn.xtmconf.anti.Spam.BlockingRule;
import kr.co.future.sslvpn.xtmconf.anti.Spam.SpamRule;
import kr.co.future.sslvpn.xtmconf.anti.Spam.Type;
import kr.co.future.sslvpn.xtmconf.anti.Virus.Option;

@Component(name = "frodo-xtmconf-anti-plugin")
@MsgbusPlugin
public class AntiPlugin {
	@MsgbusMethod
	public void getSmtp(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Smtp.class)));
	}

	@MsgbusMethod
	public void setSmtp(Request req, Response resp) {
		Smtp smtp = new Smtp();
		smtp.setSender(req.getString("sender"));
		smtp.setAllow(req.getBoolean("allow"));
		smtp.setRecipient(req.getString("recipient"));
		smtp.setSubject(req.getString("subject"));
		smtp.setContent(req.getString("content"));
		smtp.setDelAttachedFile(req.getBoolean("del_attached_file"));
		smtp.setSize(Utils.getIntegerFromMap(req.get("size"), "value"));
		smtp.setSizeLimit(Utils.getBooleanFromMap(req.get("size"), "limit"));
		smtp.setAudio(Utils.getBooleanFromMap(req.get("mime"), "audio"));
		smtp.setVideo(Utils.getBooleanFromMap(req.get("mime"), "video"));
		smtp.setApp(Utils.getBooleanFromMap(req.get("mime"), "app"));
		smtp.setImage(Utils.getBooleanFromMap(req.get("mime"), "image"));
		smtp.setMessage(Utils.getBooleanFromMap(req.get("mime"), "message"));
		smtp.setMultipart(Utils.getBooleanFromMap(req.get("mime"), "multipart"));
		smtp.setSendToTransform(req.getString("send_to_transform"));
		smtp.setTransformedSender(req.getString("transformed_sender"));
		smtp.setRecipientToTransform(req.getString("recipient_to_transform"));
		smtp.setTransformedRecipient(req.getString("transformed_recipient"));
		smtp.setSubjectToTransform(req.getString("subject_to_transform"));
		smtp.setTransformedSubject(req.getString("transformed_subject"));

		XtmConfig.writeConfig(Smtp.class, Arrays.asList(smtp));
	}

	@MsgbusMethod
	public void getSpam(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Spam.class)));
	}

	@MsgbusMethod
	public void setSpamDb(Request req, Response resp) {
		List<Spam> config = XtmConfig.readConfig(Spam.class);
		Spam obj = null;
		for (Spam spam : config) {
			if (spam.getType() == Type.DB)
				obj = spam;
		}

		if (obj == null) {
			obj = new Spam();
			obj.setType(Type.DB);
			config.add(obj);
		}

		setSpamContent(obj, req);
		XtmConfig.writeConfig(Spam.class, config);
	}

	@MsgbusMethod
	public void addSpamContent(Request req, Response resp) {
		Spam spam = new Spam();
		spam.setType(Type.Content);
		setSpamContent(spam, req);

		List<Spam> config = XtmConfig.readConfig(Spam.class);
		config.add(spam);
		Utils.setConfigNum(config, Type.Content);
		XtmConfig.writeConfig(Spam.class, config);
	}

	@MsgbusMethod
	public void modifySpamContent(Request req, Response resp) {
		int num = req.getInteger("num");

		List<Spam> config = XtmConfig.readConfig(Spam.class);
		for (Spam spam : config) {
			if (spam.getType() == Type.Content && spam.getNum() == num)
				setSpamContent(spam, req);
		}
		XtmConfig.writeConfig(Spam.class, config);
	}

	@SuppressWarnings("unchecked")
	@MsgbusMethod
	public void removeSpamContent(Request req, Response resp) {
		List<Integer> nums = (List<Integer>) req.get("num");

		List<Spam> config = XtmConfig.readConfig(Spam.class);
		List<Spam> objs = new ArrayList<Spam>();
		for (Spam spam : config) {
			if (spam.getType() == Type.Content && nums.contains(spam.getNum()))
				objs.add(spam);
		}
		for (Spam obj : objs)
			config.remove(obj);
		Utils.setConfigNum(config, Type.Content);
		XtmConfig.writeConfig(Spam.class, config);
	}

	private void setSpamContent(Spam spam, Request req) {
		if (spam.getType() == Type.DB) {
			spam.setSpamdb(req.getBoolean("spamdb"));
			spam.setAutolearn(req.getBoolean("autolearn"));
			spam.setServer(req.getString("server"));
			spam.setSpam(SpamRule.valueOf(req.getString("spam")));
			spam.setSmtp(req.getBoolean("smtp"));
			spam.setPop3(req.getBoolean("pop3"));
		} else if (spam.getType() == Type.Content) {
			spam.setBlockingRule(BlockingRule.get(req.getString("blocking_rule")));
			spam.setBlockingName(req.getString("blocking_name"));
		}
	}

	@MsgbusMethod
	public void getVirus(Request req, Response resp) {
		resp.put("config", Marshaler.marshal(XtmConfig.readConfig(Virus.class)));
	}

	@MsgbusMethod
	public void setVirus(Request req, Response resp) {
		Virus virus = new Virus();
		virus.setProtocolSmtp(Utils.getBooleanFromMap(req.get("protocol"), "smtp"));
		virus.setProtocolFtp(Utils.getBooleanFromMap(req.get("protocol"), "ftp"));
		virus.setProtocolHttp(Utils.getBooleanFromMap(req.get("protocol"), "http"));
		virus.setProtocolPop3(Utils.getBooleanFromMap(req.get("protocol"), "pop3"));
		virus.setUseAlarm(req.getBoolean("use_alarm"));
		virus.setAlarm(req.getString("alarm"));
		virus.setSmtp(getVirusOption(req.get("smtp")));
		virus.setFtp(getVirusOption(req.get("ftp")));
		virus.setHttp(getVirusOption(req.get("http")));

		XtmConfig.writeConfig(Virus.class, Arrays.asList(virus));
	}

	private Option getVirusOption(Object map) {
		Option option = new Option();
		option.setRun(Utils.getBooleanFromMap(map, "run"));
		option.setZip(Utils.getBooleanFromMap(map, "zip"));
		option.setOffice(Utils.getBooleanFromMap(map, "office"));
		option.setImage(Utils.getBooleanFromMap(map, "image"));
		option.setPdf(Utils.getBooleanFromMap(map, "pdf"));
		option.setHtml(Utils.getBooleanFromMap(map, "html"));
		return option;
	}
}
