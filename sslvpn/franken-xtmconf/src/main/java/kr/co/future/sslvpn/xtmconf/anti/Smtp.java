package kr.co.future.sslvpn.xtmconf.anti;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kr.co.future.sslvpn.xtmconf.AttributeBuilder;
import kr.co.future.sslvpn.xtmconf.MarshalValue;
import kr.co.future.sslvpn.xtmconf.NodeWrapper;
import kr.co.future.sslvpn.xtmconf.XtmConfig;

public class Smtp extends XtmConfig {
	private String sender; // 송신자
	private boolean allow; // 설정된 송신자/수신자 허용
	private String recipient; // 수신자
	private String subject; // 제목
	private String content; // 내용
	private boolean delAttachedFile; // 첨부파일 제거
	private Integer size; // 최대메일크기
	private boolean sizeLimit; // 최대메일크기 제한
	private boolean audio; // MIME 차단-Audio
	private boolean video; // MIME 차단-Video
	private boolean app; // MIME 차단-Application
	private boolean image; // MIME 차단-Image
	private boolean message; // MIME 차단-Message
	private boolean multipart; // MIME 차단-Multipart
	private String sendToTransform; // 변환필터링-송신자(좌)
	private String transformedSender; // 변환필터링-송신자 (우)
	private String recipientToTransform; // 변환필터링-수신자 (좌)
	private String transformedRecipient; // 변환필터링-수신자 (우)
	private String subjectToTransform; // 변환필터링-제목 (좌)
	private String transformedSubject; // 변환필터링-제목 (우)

	@Override
	public String getXmlFilename() {
		return "anti_smtp.xml";
	}

	@Override
	public String getRootTagName() {
		return "smtp";
	}

	public static Smtp parse(NodeWrapper nw) {
		if (!nw.isName("setting"))
			return null;

		Smtp s = new Smtp();

		for (NodeWrapper c : nw.children()) {
			if (c.isName("sender"))
				s.sender = c.value();
			else if (c.isName("recipient")) {
				s.allow = c.boolAttr("chk_allow");
				s.recipient = c.value();
			} else if (c.isName("subject"))
				s.subject = c.value();
			else if (c.isName("content"))
				s.content = c.value();
			else if (c.isName("delattachedfile"))
				s.delAttachedFile = c.value().equals("true");
			else if (c.isName("sizelimit")) {
				s.size = c.intAttr("size");
				s.sizeLimit = c.value().equals("true");
			} else if (c.isName("mime")) {
				s.audio = c.boolAttr("chk_audio");
				s.video = c.boolAttr("chk_video");
				s.app = c.boolAttr("chk_app");
				s.image = c.boolAttr("chk_image");
				s.message = c.boolAttr("chk_message");
				s.multipart = c.boolAttr("chk_multipart");
			} else if (c.isName("sendtotransform"))
				s.sendToTransform = c.value();
			else if (c.isName("transformedsender"))
				s.transformedSender = c.value();
			else if (c.isName("recipienttotransform"))
				s.recipientToTransform = c.value();
			else if (c.isName("transformedrecipient"))
				s.transformedRecipient = c.value();
			else if (c.isName("subjecttotransform"))
				s.subjectToTransform = c.value();
			else if (c.isName("transformedsubject"))
				s.transformedSubject = c.value();
		}

		return s;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public boolean isAllow() {
		return allow;
	}

	public void setAllow(boolean allow) {
		this.allow = allow;
	}

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public boolean isDelAttachedFile() {
		return delAttachedFile;
	}

	public void setDelAttachedFile(boolean delAttachedFile) {
		this.delAttachedFile = delAttachedFile;
	}

	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	public boolean isSizeLimit() {
		return sizeLimit;
	}

	public void setSizeLimit(boolean sizeLimit) {
		this.sizeLimit = sizeLimit;
	}

	public boolean isAudio() {
		return audio;
	}

	public void setAudio(boolean audio) {
		this.audio = audio;
	}

	public boolean isVideo() {
		return video;
	}

	public void setVideo(boolean video) {
		this.video = video;
	}

	public boolean isApp() {
		return app;
	}

	public void setApp(boolean app) {
		this.app = app;
	}

	public boolean isImage() {
		return image;
	}

	public void setImage(boolean image) {
		this.image = image;
	}

	public boolean isMessage() {
		return message;
	}

	public void setMessage(boolean message) {
		this.message = message;
	}

	public boolean isMultipart() {
		return multipart;
	}

	public void setMultipart(boolean multipart) {
		this.multipart = multipart;
	}

	public String getSendToTransform() {
		return sendToTransform;
	}

	public void setSendToTransform(String sendToTransform) {
		this.sendToTransform = sendToTransform;
	}

	public String getTransformedSender() {
		return transformedSender;
	}

	public void setTransformedSender(String transformedSender) {
		this.transformedSender = transformedSender;
	}

	public String getRecipientToTransform() {
		return recipientToTransform;
	}

	public void setRecipientToTransform(String recipientToTransform) {
		this.recipientToTransform = recipientToTransform;
	}

	public String getTransformedRecipient() {
		return transformedRecipient;
	}

	public void setTransformedRecipient(String transformedRecipient) {
		this.transformedRecipient = transformedRecipient;
	}

	public String getSubjectToTransform() {
		return subjectToTransform;
	}

	public void setSubjectToTransform(String subjectToTransform) {
		this.subjectToTransform = subjectToTransform;
	}

	public String getTransformedSubject() {
		return transformedSubject;
	}

	public void setTransformedSubject(String transformedSubject) {
		this.transformedSubject = transformedSubject;
	}

	@Override
	protected Element convertToElement(Document doc) {
		Element e = doc.createElement("setting");

		appendChild(doc, e, "sender", sender);
		appendChild(doc, e, "recipient", recipient, new AttributeBuilder("chk_allow", allow));
		appendChild(doc, e, "subject", subject);
		appendChild(doc, e, "content", content);
		appendChild(doc, e, "delattachedfile", delAttachedFile ? "true" : "false");
		appendChild(doc, e, "sizelimit", sizeLimit ? "true" : "false", new AttributeBuilder("size", size));
		AttributeBuilder mimeAttr = new AttributeBuilder("chk_audio", audio).put("chk_video", video)
				.put("chk_app", app).put("chk_image", image).put("chk_message", message)
				.put("chk_multipart", multipart);
		appendChild(doc, e, "mime", null, mimeAttr);
		appendChild(doc, e, "sendtotransform", sendToTransform);
		appendChild(doc, e, "transformedsender", transformedSender);
		appendChild(doc, e, "recipienttotransform", recipientToTransform);
		appendChild(doc, e, "transformedrecipient", transformedRecipient);
		appendChild(doc, e, "subjecttotransform", subjectToTransform);
		appendChild(doc, e, "transformedsubject", transformedSubject);

		return e;
	}

	@Override
	public Map<String, Object> marshal() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("sender", sender);
		m.put("allow", allow);
		m.put("recipient", recipient);
		m.put("subject", subject);
		m.put("content", content);
		m.put("del_attached_file", delAttachedFile);
		m.put("size", new MarshalValue("value", size).put("limit", sizeLimit).get());
		m.put("mime",
				new MarshalValue("audio", audio).put("video", video).put("app", app).put("image", image)
						.put("message", message).put("multipart", multipart).get());
		m.put("send_to_transform", sendToTransform);
		m.put("transformed_sender", transformedSender);
		m.put("recipient_to_transform", recipientToTransform);
		m.put("transformed_recipient", transformedRecipient);
		m.put("subject_to_transform", subjectToTransform);
		m.put("transformed_subject", transformedSubject);

		return m;
	}

	@Override
	public String toString() {
		return "Smtp [sender=" + sender + ", allow=" + allow + ", recipient=" + recipient + ", subject=" + subject
				+ ", content=" + content + ", delAttachedFile=" + delAttachedFile + ", size=" + size + ", sizeLimit="
				+ sizeLimit + ", audio=" + audio + ", video=" + video + ", app=" + app + ", image=" + image
				+ ", message=" + message + ", multipart=" + multipart + ", sendToTransform=" + sendToTransform
				+ ", transformedSender=" + transformedSender + ", recipientToTransform=" + recipientToTransform
				+ ", transformedRecipient=" + transformedRecipient + ", subjectToTransform=" + subjectToTransform
				+ ", transformedSubject=" + transformedSubject + "]";
	}
}
