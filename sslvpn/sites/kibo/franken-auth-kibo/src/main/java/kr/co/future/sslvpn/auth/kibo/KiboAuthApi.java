package kr.co.future.sslvpn.auth.kibo;

import java.util.Map;

public interface KiboAuthApi {

	/*
	 * key example "MobileTransKey10" data example
	 * "f786cea8b466da11043f529174e3d493c4262a42015c28ee0a503f3f4269fd27420f47a525c8463987bfd761ddf484b488bcc938e0d04f829035f450d5d687b68c139127c41e74cb4c207cb687968bfc607a7113e4a1ffb38264a1c26f4dc969a403738e3d0b91a2fd8ee3c1c59fbfba6b5dee7952109fd73e0b1f5bcbe5917d"
	 */
	String decryptKey(String data);

	/*
	 * 서비스명(serviceName) 및 함수명(methodName) mimdi01 (MDM) 단말 인증 -
	 * updateUserDeviceInfo mrmrm91 단말 제한정책 변경 - insertSetRestrictionUserId
	 * mumum01 OTP - getOTPValidate
	 * 
	 * 
	 * /* MDM 유저아이디 - iphone3g 단말아이디 - a136d5b9ee7604f54f1fa3a7968cdc801c8390b9
	 */
	Map<String, Object> verifyDevice(String deviceId);

	/*
	 * OTP값 - 11631491 단말아이디 - 6Q042EM5A4S
	 */
	Map<String, Object> verifyOtp(String deviceId, String otpValue);
	
	void setConfig(KiboConfig config);

	KiboConfig getConfig();

}
