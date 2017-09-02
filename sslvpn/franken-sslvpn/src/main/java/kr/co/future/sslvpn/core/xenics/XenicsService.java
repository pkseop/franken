package kr.co.future.sslvpn.core.xenics;

import java.util.List;
import java.util.Map;

import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.core.xenics.impl.Users;

public interface XenicsService {
	public int getTotalNumOfTunnels();
	
	public int getTotalNumOfTunnels(String filterField, String filterValue);
	
	public List<Object> getTunnels(int limit, int offset, String filterField, String filterValue);
	public boolean isAlreadyLoggedin(String loginName);
	public int getClientId(int tunnelId);
	public int getClientId(String loginName);
	
	public Users getTunnelInfo(int tunnelId);
	
	public Users getConnectedTunnelInfo(String remoteAddr);
	
	//스픞릿 라우팅, 차단/내부 IP 대역, 접근 허용 서버 목록에 대한 db 저장.
	public void updateAccessProfileInfo(List<AccessProfile> profiles);
	
	public String killTunnel(int tunnelId);
	
	public void createTables();
	
	public void insertBackupInfo(int type, String fileName, String fileSize, int stateCode);
	
	public void updateBackupState(String fileName, int stateCode);
	
	public int getTotalNumOfFtpBackupRecords(int type);
	
	public List<Object> getFtpBackupRecords(int type, int limit, int offset);
	
	public void	deleteBackupRecord(String fileName);
	
	public List<String> fetchFtpBackupFileNames(int type, int stateCode);
	
	public void updatePrevPolicyRollbackState(int stateCode);
	
	public void remoteDbStart(String url, String user, String pw, String table);
	
	public void remoteDbStop();
	
	public boolean isDupLoginByRemoteDb(String loginName);
	
	public String execkillTunnel(int clientId);
	
	public int retrieveFtpBackupStateCode(String fileName);
	
	public List<Users> getConnectedTunnelInfo();
	
	public String killDuploginTunnel(String loginName);
}
