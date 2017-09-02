package kr.co.future.sslvpn.model.api.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.IpLease;
import kr.co.future.sslvpn.model.IpLeaseRange;
import kr.co.future.sslvpn.model.UserExtension;
import kr.co.future.sslvpn.model.api.AccessProfileApi;
import kr.co.future.sslvpn.model.api.IpLeaseApi;
import kr.co.future.sslvpn.model.api.IpLeaseEventListener;
import kr.co.future.sslvpn.model.api.UserApi;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;

import kr.co.future.confdb.BaseConfigDatabaseListener;
import kr.co.future.confdb.BaseConfigServiceListener;
import kr.co.future.confdb.Config;
import kr.co.future.confdb.ConfigDatabase;
import kr.co.future.confdb.ConfigIterator;
import kr.co.future.confdb.ConfigService;
import kr.co.future.confdb.Predicates;
import kr.co.future.dom.api.DOMException;
import kr.co.future.dom.api.DefaultEntityEventListener;
import kr.co.future.dom.api.MySQLConnectionService;
import kr.co.future.dom.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "frodo-ip-lease-api")
@Provides
public class IpLeaseApiImpl implements IpLeaseApi {
	private final Logger logger = LoggerFactory.getLogger(IpLeaseApiImpl.class.getName());

	@Requires
	private kr.co.future.dom.api.UserApi domUserApi;

	@Requires
	private UserApi userApi;

	@Requires
	private AccessProfileApi profileApi;

	@Requires
    private MySQLConnectionService connectionService;

	private ReserveIpSync ipSync;

	private ConcurrentMap<String, String> reservedAddrs;

	private CopyOnWriteArraySet<IpLeaseEventListener> listeners;

	// mapping login name to IpLease
	private ConcurrentHashMap<String, IpLease> ipLeasesByLoginName = new ConcurrentHashMap<String, IpLease>();

	// mapping ip number to IpLease
	private ConcurrentHashMap<Long, IpLease> ipLeasesByIp = new ConcurrentHashMap<Long, IpLease>();

	public IpLeaseApiImpl() {
		ipSync = new ReserveIpSync();
		reservedAddrs = new ConcurrentHashMap<String, String>();
		listeners = new CopyOnWriteArraySet<IpLeaseEventListener>();
		
		createIpLeaseTable();
	}

	@Validate
	public void start() {
		domUserApi.addEntityEventListener(ipSync);

        logger.info("IpLeaseApiImpl initializeCache() start...");
		initializeCache();
        logger.info("IpLeaseApiImpl initializeCache() end...");
	}

	@Invalidate
	public void stop() {
		if (domUserApi != null)
			domUserApi.removeEntityEventListener(ipSync);
	}

	private void initializeCache() {
		// load all reserved ip list
		try {
			for (UserExtension ext : userApi.getUserExtsWithStaticIp4()) {
				String reservedAddr = ext.getStaticIp4();
				if (reservedAddr != null) {
					String loginName = ext.getUser().getLoginName();
					logger.info("frodo model: loading reserved ip [{}] for login name [{}]", reservedAddr, loginName);
					reservedAddrs.put(reservedAddr, loginName);
				}
			}
		} catch (DOMException e) {
			if (e.getErrorCode().equals("organization-not-found"))
				return;
			logger.error("frodo model: cannot load reserved ip list", e);
		}

		ipLeasesByIp.clear();
		ipLeasesByLoginName.clear();

		List<IpLease> allLeases = getAllLeases();
		for (IpLease ip : allLeases) {
			ipLeasesByIp.put(ip.getIp(), ip);
			ipLeasesByLoginName.put(ip.getLoginName(), ip);
		}
	}

	@Override
	public List<IpLease> getAllLeases() {
		String query = "SELECT * FROM IpLease";
		List<IpLease> allLeases = new ArrayList<IpLease>();
		
		Connection con = null;
		PreparedStatement psmt = null;
		ResultSet rs = null;
		
		try {
			con = connectionService.getConnection();
			if (con == null) {
				logger.error("Can't connect to database. check database status.");
				throw new Exception("database connect fail");
			}
			 
			psmt = con.prepareStatement(query);
			rs = psmt.executeQuery();
			
			while (rs.next()) {
				allLeases.add(fetchIpLeaseFromResultSet(rs));
			}
		} catch(Exception e) {
			logger.error("error occurred during fetch all ip lease infos.", e);
		} finally {
			close(rs, psmt, con);
		}
		return allLeases;
	}

	@Override
	public List<IpLease> getLeases(String loginName) {
		String query = "SELECT * FROM IpLease WHERE loginName = ?";
		List<IpLease> leases = new ArrayList<IpLease>();
		
		Connection con = null;
		PreparedStatement psmt = null;
		ResultSet rs = null;
		
		try {
			con = connectionService.getConnection();
			if (con == null) {
				logger.error("Can't connect to database. check database status.");
				throw new Exception("database connect fail");
			}
			 
			psmt = con.prepareStatement(query);
			psmt.setString(1, loginName);
			rs = psmt.executeQuery();
			
			while (rs.next()) {
				leases.add(fetchIpLeaseFromResultSet(rs));
			}
		} catch(Exception e) {
			logger.error("error occurred during fetch ip lease infos.", e);
		} finally {
			close(rs, psmt, con);
		}
		return leases;
	}

	@Override
	public List<IpLeaseRange> getFilteredRanges(List<IpLeaseRange> ranges, long ipFrom, long ipTo) {

		List<IpLeaseRange> filteredIpLeaseRanges = new ArrayList<IpLeaseRange>();
		for (IpLeaseRange i : ranges) {

			long profileFrom = IpLease.toLong(i.getIpFrom());
			long profileTo = IpLease.toLong(i.getIpTo());

			// 범위에서 벗어난 경우
			if ((profileFrom > ipTo) || (profileTo < ipFrom)) {
				continue;
			}

			long from = Math.max(ipFrom, profileFrom);
			long to = Math.min(ipTo, profileTo);

			// 필터링된 대역을 새로운 리스트에 저장
			IpLeaseRange filteredIpLeaseRange = new IpLeaseRange();
			filteredIpLeaseRange.setIpFrom(IpLease.toInetAddress(from).getHostAddress());
			filteredIpLeaseRange.setIpTo(IpLease.toInetAddress(to).getHostAddress());
			filteredIpLeaseRanges.add(filteredIpLeaseRange);

		}
		return filteredIpLeaseRanges;
	}

	@Override
	public List<Integer> getPoolSizeOffsets(List<IpLeaseRange> ranges) {
		List<Integer> offsets = new ArrayList<Integer>();
		int poolSize = 0;

		// 필터링된 대역의 오프셋을 오프셋 리스트에 저장
		for (IpLeaseRange range : ranges) {
			poolSize += range.getPoolSize();
			offsets.add(poolSize);
		}
		return offsets;
	}

	@Override
	public long calculateIpAddress(List<IpLeaseRange> ranges, List<Integer> poolOffsets, int offset) {
		long from = -1;

		// 오프셋 크기로 from, to 를 설정
		for (int j = 0; j < poolOffsets.size(); j++) {
			if (j == 0) {
				if (offset >= 0 && offset < poolOffsets.get(j)) {
					// 초기 진입시에는 0부터 시작해서 비교를 한다. 오프셋 값은 그대로.
					from = ranges.get(j).getIpFromLong();
				} else {
					continue;
				}
			} else {
				// 두번째 진입부터는 이전 대역범위 크기부터 비교를 하며 결정된 오프셋값은 기존의 오프셋값에서 이전
				// 범위값을 뺀 값.
				if (offset >= poolOffsets.get(j - 1) && offset < poolOffsets.get(j)) {
					from = ranges.get(j).getIpFromLong();
					offset = offset - poolOffsets.get(j - 1);
				} else {
					continue;
				}
			}
		}

		return from + offset;
	}

	@Override
	public synchronized InetAddress request(int tunnelId, String loginName, InetAddress tunIp, InetAddress tunNetmask) {
		User user = domUserApi.getUser("localhost", loginName);
		return request(tunnelId, user, tunIp, tunNetmask, false);
	}

	@Override
	public synchronized InetAddress request(int tunnelId, User user, InetAddress tunIp, InetAddress tunNetmask, boolean retry) {
		String loginName = user.getLoginName();

		// calculate expire date (after 1day)
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, 1);
		Date expire = cal.getTime();

		// fetch user data and check reserved ip
		UserExtension ext = userApi.getUserExtension(user);
		
		if (retry) {
         ext.setLastIp(null);
     }
		
		if (ext != null && ext.getStaticIp4() != null) {
			// if reserved ip exists
			InetAddress ip = null;
			try {
				ip = InetAddress.getByName(ext.getStaticIp4());
//pks. 2015-06-03. 고정IP를 사용할 경우 InetAddress.getLocalHost() 에서 지연 현상이 발생되어 문제가 됨. 				
//				if (ip.equals(InetAddress.getLocalHost()))
//					ip = null;
			} catch (UnknownHostException e) {
				logger.error("error occurred during retrieving static ip", e);
			} 

			if (ip != null) {
				logger.trace("frodo model: reserved ip [{}]", ip.getHostAddress());
				// extend or new lease
				long ipnumber = IpLease.toLong(ip.getHostAddress());
				IpLease lease = ipLeasesByIp.get(ipnumber);
				if (lease == null) {
					logger.trace("frodo model: insert reserved ip [{}]", ip.getHostAddress());
					insertLease(tunnelId, loginName, ipnumber, expire);
				} else {
					// check ownership
					if (!lease.getLoginName().equals(user.getLoginName()))
						return null;

					logger.trace("frodo model: extend reserved ip [{}]", ip.getHostAddress());
					extendIpLease(lease, tunnelId, expire);
				}

				logger.trace("frodo model: return reserved ip [{}]", ip.getHostAddress());
				return ip;
			}
		}

		// get lease ip range from profile
		AccessProfile profile = profileApi.determineProfile(user);

		// get tun0 ip range
		long longTunIp = IpLease.toLong(tunIp.getHostAddress());
		long longTunNetmask = IpLease.toLong(tunNetmask.getHostAddress());

		long tunFrom = (longTunIp & longTunNetmask) + 1;
		long tunTo = (longTunIp | (~longTunNetmask & 0xFFFFFFFL)) - 1;

		long last = -1;

		boolean canLease = false;
		IpLease lease = null;

		logger.debug("frodo model: tun range [{}] ~ [{}]", IpLease.toInetAddress(tunFrom).getHostAddress(), IpLease
				.toInetAddress(tunTo).getHostAddress());

		// 프로필에 설정되어 있는 대역의 총 범위를 poolSize로
		List<IpLeaseRange> filteredIpLeaseRanges = getFilteredRanges(profile.getIpLeaseRanges(), tunFrom, tunTo);
		List<Integer> offsets = getPoolSizeOffsets(filteredIpLeaseRanges);

		lease = ipLeasesByLoginName.get(loginName);
		if (lease != null) {
			// check if ip is valid (tun0 network can be changed)
			for (IpLeaseRange i : filteredIpLeaseRanges) {
				if (i.getIpFromLong() <= lease.getIp() && lease.getIp() <= i.getIpToLong()) {
					// extend lease
					extendIpLease(lease, tunnelId, expire);
					return IpLease.toInetAddress(lease.getIp());
				}

			}
		}

		// try to assign last ip
		if (ext != null && ext.getLastIp() != null) {
			for (IpLeaseRange i : filteredIpLeaseRanges) {
				long t = IpLease.toLong(ext.getLastIp());
				if (i.getIpFromLong() <= t && t <= i.getIpToLong()) {
					last = t;
					if (logger.isTraceEnabled())
						logger.trace("frodo model: last ip lease range [{}]~[{}], last lease ip [{}]",
								new Object[] { i.getIpFrom(), i.getIpTo(), ext.getLastIp() });
					break;
				}
				last = -1;
			}
		}

		// 아무것도 임대대역에 걸치치 못하면?
		if (offsets.isEmpty()) {
			logger.error("frodo model: pool size is zero. cannot lease ip");
			return null;
		}

		long candidate = 0;
		int offset = 0;
		int poolSize = offsets.get(offsets.size() - 1);

		// ramdom assign
		Random r = new Random();
		for (int i = 0; i < 10; i++) {
			// try last ip first
			if (last >= 0) {
				candidate = last;
				last = -1;
			} else {
				offset = r.nextInt(poolSize);
				logger.debug("frodo model: trying random ip assign, poolSize=[{}], random offset=[{}]", poolSize, offset);
				candidate = calculateIpAddress(filteredIpLeaseRanges, offsets, offset);
			}

			if (logger.isDebugEnabled())
				logger.debug("frodo model: isReservedIp? [{}]", isReservedIp(IpLease.toInetAddress(candidate)));

			if (isReservedIp(IpLease.toInetAddress(candidate)))
				continue;

			// tunip와 겹치면 다시 배당
			if (tunIp.equals(IpLease.toInetAddress(candidate)))
				continue;

			// check old lease
			lease = null;
			lease = ipLeasesByIp.get(candidate);

			if (logger.isDebugEnabled())
				logger.debug("frodo model: ipLeasesByIp? [{}]", lease);

			// hooray! it's empty
			if (lease == null) {
				canLease = true;
				break;
			}

			if (lease.getLoginName().equals(loginName)) {
				// if ip owner, extend it
				extendIpLease(lease, tunnelId, expire);
				canLease = true;
				break;
			} else if (lease.getExpireDate().before(new Date())) {
				IpLease fetchedLease = getIpLeaseByIp(candidate);
				if(fetchedLease != null) 
					deleteIpLeaseByIp(lease.getIp());
				ipLeasesByIp.remove(lease.getIp());
				ipLeasesByLoginName.remove(lease.getLoginName());
				canLease = true;
				break;
			}
		}

		if (!canLease)
			return null;

		InetAddress addr = IpLease.toInetAddress(candidate);
		logger.info("frodo model: assigned ip [{}]", addr.getHostAddress());

		insertLease(tunnelId, loginName, candidate, expire);

		return addr;
	}

	private boolean isReservedIp(InetAddress ip) {
		return reservedAddrs.containsKey(ip.getHostAddress());
	}

	private void insertLease(int tunnelId, String loginName, long ip, Date expire) {
		IpLease lease = new IpLease();
		lease.setLoginName(loginName);
		lease.setTunnelId(tunnelId);
		lease.setIp(ip);
		lease.setLeaseDate(new Date());
		lease.setExpireDate(expire);
		insertIpLeaseToDB(lease);
		ipLeasesByIp.put(lease.getIp(), lease);
		ipLeasesByLoginName.put(loginName, lease);

		for (IpLeaseEventListener listener : listeners) {
			try {
				listener.onLease(lease);
			} catch (Throwable t) {
				logger.error("frodo model: ip lease callback should not throw any exception", t);
			}
		}
	}

	private void extendIpLease(IpLease lease, int tunnelId, Date expire) {
		IpLease fetchedLease = getIpLeaseByIp(lease.getIp());
		if(fetchedLease == null) {
			insertLease(tunnelId, lease.getLoginName(), lease.getIp(), expire);
			return;
		}
		lease.setTunnelId(tunnelId);
		lease.setLeaseDate(new Date());
		lease.setExpireDate(expire);
		updateIpLeaseToDB(lease);
		ipLeasesByIp.put(lease.getIp(), lease);
		ipLeasesByLoginName.put(lease.getLoginName(), lease);

		for (IpLeaseEventListener listener : listeners) {
			try {
				listener.onExtend(lease);
			} catch (Throwable t) {
				logger.error("frodo model: ip lease callback should not throw any exception", t);
			}
		}
	}

	@Override
	public synchronized void release(int tunnelId) {
		IpLease lease = getIpLeaseByTunnelId(tunnelId);
		if(lease != null) {
			String query = "DELETE FROM IpLease WHERE tunnelId = ?";
			
			Connection con = null;
			PreparedStatement psmt = null;
			ResultSet rs = null;
			
			try {
				con = connectionService.getConnection();
				if (con == null) {
					logger.error("Can't connect to database. check database status.");
					throw new Exception("database connect fail");
				}
				 
				psmt = con.prepareStatement(query);
				psmt.setInt(1, tunnelId);
				psmt.executeUpdate();
			} catch(Exception e) {
				logger.error("error occurred during fetch all ip lease infos.", e);
			} finally {
				close(rs, psmt, con);
			}
			
			ipLeasesByIp.remove(lease.getIp());
			ipLeasesByLoginName.remove(lease.getLoginName());
	
			notifyRelease(lease);
		}
	}

	private void notifyRelease(IpLease lease) {
		for (IpLeaseEventListener listener : listeners) {
			try {
				listener.onRelease(lease);
			} catch (Throwable t) {
				logger.error("frodo model: ip lease callback should not throw any exception", t);
			}
		}
	}

	@Override
	public void addListener(IpLeaseEventListener listener) {
		if (listener == null)
			throw new IllegalStateException("ip lease event listener should be not null");
		listeners.add(listener);
	}

	@Override
	public void removeListener(IpLeaseEventListener listener) {
		if (listener == null)
			throw new IllegalStateException("ip lease event listener should be not null");
		listeners.remove(listener);
	}

	private class ReserveIpSync extends DefaultEntityEventListener<User> {
		@Override
		public void entityUpdated(String domain, User obj, Object state) {
			UserExtension ext = userApi.getUserExtension(obj);
			if (ext == null)
				return;

			// remove old reserved ip
			removeOldReservedIp(obj);

			if (ext.getStaticIp4() != null) {
				logger.trace("frodo model: adding reserved ip [{}] for login name [{}]", ext.getStaticIp4(), obj.getLoginName());
				reservedAddrs.put(ext.getStaticIp4(), obj.getLoginName());
			}
		}

		@Override
		public void entityRemoved(String domain, User obj, Object state) {
			removeOldReservedIp(obj);
		}

		private void removeOldReservedIp(User obj) {
			for (String ip : reservedAddrs.keySet()) {
				String loginName = reservedAddrs.get(ip);
				if (loginName != null && loginName.equals(obj.getLoginName())) {
					logger.trace("frodo model: removing old reserved ip [{}] for login name [{}]", ip, loginName);
					reservedAddrs.remove(ip);
				}
			}
		}
	}

	private void createIpLeaseTable() {
		 String query = "CREATE TABLE IF NOT EXISTS IpLease\n" +
	                "(\n" +
	                "    ip BIGINT UNSIGNED PRIMARY KEY NOT NULL,\n" +
	                "    loginName VARCHAR(60) NOT NULL,\n" +
	                "    tunnelId INT UNSIGNED,\n" +
	                "    leaseDate TIMESTAMP NOT NULL,\n" +
	                "    expireDate TIMESTAMP NOT NULL,\n" +
	                "    profileName VARCHAR(60),\n" +
	                "    node INT UNSIGNED NOT NULL,\n" +
	                "    ipOffset BIGINT UNSIGNED NOT NULL\n" +
	                ");\n";

	        runQuery(query);

	        logger.info("Check ProgramPack Table: OK");
	}
	
	private void runQuery(String query) {
       Connection con = null;
       PreparedStatement psmt = null;

       try {
           con = connectionService.getConnection();

           if (con == null) {
               logger.error("Can't connect to database. check database status.");
               throw new Exception("database connect fail");
           }

           psmt = con.prepareStatement(query);
           psmt.executeUpdate();

           logger.debug("Create Table success!");
       } catch (SQLException e) {
       	logger.error("error occurred during excute sql", e);
       } catch (Exception e) {
           logger.info(e.getMessage());
           logger.error("Exception Create Table error", e);
       } finally {
           close(psmt, con);
       }
   }
	
	private void insertIpLeaseToDB(IpLease lease) {
		String query = "INSERT INTO IpLease (ip, loginName, tunnelId, leaseDate, expireDate, profileName, node, ipOffset) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
		
		Connection con = null;
       PreparedStatement psmt = null;

       try {
           con = connectionService.getConnection();

           if (con == null) {
               logger.error("Can't connect to database. check database status.");
               throw new Exception("database connect fail");
           }

           psmt = con.prepareStatement(query);
           psmt.setLong(1, lease.getIp());
           psmt.setString(2, lease.getLoginName());
           psmt.setInt(3, lease.getTunnelId());
           psmt.setTimestamp(4, new Timestamp(lease.getLeaseDate().getTime()));
           psmt.setTimestamp(5, new Timestamp(lease.getExpireDate().getTime()));
           psmt.setString(6, lease.getProfileName());
           psmt.setInt(7, lease.getNode());
           psmt.setLong(8, lease.getIpOffset());
           
           psmt.executeUpdate();
       } catch (SQLException e) {
       	logger.error("error occurred during excute sql", e);
       } catch (Exception e) {
           logger.info(e.getMessage());
           logger.error("Exception Create Table error", e);
       } finally {
           close(psmt, con);
       }
	}
	
	private void updateIpLeaseToDB(IpLease lease) {
		String query = "UPDATE TABLE IpLease SET ip=?, loginName=?, tunnelId=?, leaseDate=?, expireDate=?, profileName=?, node=?, ipOffset=?";
		
		Connection con = null;
       PreparedStatement psmt = null;

       try {
           con = connectionService.getConnection();

           if (con == null) {
               logger.error("Can't connect to database. check database status.");
               throw new Exception("database connect fail");
           }

           psmt = con.prepareStatement(query);
           psmt.setLong(1, lease.getIp());
           psmt.setString(2, lease.getLoginName());
           psmt.setInt(3, lease.getTunnelId());
           psmt.setTimestamp(4, new Timestamp(lease.getLeaseDate().getTime()));
           psmt.setTimestamp(5, new Timestamp(lease.getExpireDate().getTime()));
           psmt.setString(6, lease.getProfileName());
           psmt.setInt(7, lease.getNode());
           psmt.setLong(8, lease.getIpOffset());
           
           psmt.executeUpdate();
       } catch (SQLException e) {
       	logger.error("error occurred during excute sql", e);
       } catch (Exception e) {
           logger.info(e.getMessage());
           logger.error("Exception Create Table error", e);
       } finally {
           close(psmt, con);
       }
	}
	
	public IpLease getIpLeaseByIp(long ip) {
		String query = "SELECT * FROM IpLease WHERE ip = ?";
		
		Connection con = null;
		PreparedStatement psmt = null;
		ResultSet rs = null;
		
		try {
			con = connectionService.getConnection();
			if (con == null) {
				logger.error("Can't connect to database. check database status.");
				throw new Exception("database connect fail");
			}
			 
			psmt = con.prepareStatement(query);
			psmt.setLong(1, ip);
			rs = psmt.executeQuery();
			
			if (rs.next()) {
				return fetchIpLeaseFromResultSet(rs);
			}
		} catch(Exception e) {
			logger.error("error occurred getIpLeaseByIp.", e);
		} finally {
			close(rs, psmt, con);
		}
		return null;
	}
	
	public IpLease getIpLeaseByTunnelId(int tunnelId) {
		String query = "SELECT * FROM IpLease WHERE tunnelId = ?";
		
		Connection con = null;
		PreparedStatement psmt = null;
		ResultSet rs = null;
		
		try {
			con = connectionService.getConnection();
			if (con == null) {
				logger.error("Can't connect to database. check database status.");
				throw new Exception("database connect fail");
			}
			 
			psmt = con.prepareStatement(query);
			psmt.setInt(1, tunnelId);
			rs = psmt.executeQuery();
			
			if (rs.next()) {
				return fetchIpLeaseFromResultSet(rs);
			}
		} catch(Exception e) {
			logger.error("error occurred in getIpLeaseByTunnelId", e);
		} finally {
			close(rs, psmt, con);
		}
		return null;
	}
	
	private void deleteIpLeaseByIp(long ip) {
		String query = "DELETE FROM IpLease WHERE ip = ?";
		
		Connection con = null;
		PreparedStatement psmt = null;
		ResultSet rs = null;
		
		try {
			con = connectionService.getConnection();
			if (con == null) {
				logger.error("Can't connect to database. check database status.");
				throw new Exception("database connect fail");
			}
			 
			psmt = con.prepareStatement(query);
			psmt.setLong(1, ip);
			psmt.executeUpdate();
		} catch(Exception e) {
			logger.error("error occurred in deleteIpLeaseByIp.", e);
		} finally {
			close(rs, psmt, con);
		}
	}
	
	private IpLease fetchIpLeaseFromResultSet(ResultSet rs) throws SQLException {
		IpLease lease = new IpLease();
		lease.setIp(rs.getLong("ip"));
		lease.setLoginName(rs.getString("loginName"));
		lease.setTunnelId(rs.getInt("tunnelId"));
		lease.setLeaseDate(toDate(rs.getTimestamp("leaseDate")));
		lease.setExpireDate(toDate(rs.getTimestamp("expireDate")));
		lease.setProfileName(rs.getString("profileName"));
		lease.setNode(rs.getInt("node"));
		lease.setIpOffset(rs.getLong("ipOffset"));
		
		return lease;
	}
	
	private void close(ResultSet rs, PreparedStatement psmt, Connection con) {
       logger.trace("close sql jdbc");
       if(rs != null) {
       	try {
       		rs.close();
       	} catch (SQLException e) {
           	logger.error("error occurred during closing ResultSet", e);
           }
       }
       close(psmt, con);
   }

   private void close(PreparedStatement psmt, Connection con) {
       logger.trace("close sql jdbc");
       if (psmt != null)
           try {
               psmt.close();
           } catch (SQLException e) {
           	logger.error("error occurred during closing PreparedStatement", e);
           }
       if (con != null)
           try {
               con.close();
           } catch (SQLException e) {
           	logger.error("error occurred during closing Connection", e);
           }
   }
   
   public static java.util.Date toDate(java.sql.Timestamp timestamp) {
       long milliseconds = timestamp.getTime() + (timestamp.getNanos() / 1000000);
       return new java.util.Date(milliseconds);
   }
}
