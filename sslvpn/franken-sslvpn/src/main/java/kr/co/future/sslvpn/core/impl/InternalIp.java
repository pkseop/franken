package kr.co.future.sslvpn.core.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import kr.co.future.sslvpn.model.AccessProfile;
import kr.co.future.sslvpn.model.IpLease;
import kr.co.future.sslvpn.xtmconf.BridgeTunCachingService;

import org.slf4j.Logger;

public class InternalIp {

	private InetAddress ip;
	private InetAddress netmask;
	private boolean isEnabledBridge;

	public InternalIp(Logger logger, BridgeTunCachingService bridgeCache, AccessProfile profile) throws UnknownHostException {
		Inet4Address internalIp = null;
		InetAddress internalNetmask = null;
		short subnetMask = 0;

		String tunName = "tun0";
		if (profile.getUseIOS() != null ? profile.getUseIOS() : false)
			tunName = "tun1";
		
		if (bridgeCache.getBridgedTunIp() != null) {
			this.ip = InetAddress.getByName(bridgeCache.getBridgedTunIp());
			this.netmask = InetAddress.getByName(bridgeCache.getBridgredTunNetmask());
			this.isEnabledBridge = true;
		} else {
//			try {
//				NetworkInterface iface = NetworkInterface.getByName("tap0");
//				if (iface == null)
//					throw new IllegalStateException("tap ip not set");
//
//				Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();
//
//				while (inetAddresses.hasMoreElements()) {
//					InetAddress inetAddr = inetAddresses.nextElement();
//					if (inetAddr instanceof Inet4Address) {
//						internalIp = (Inet4Address) inetAddr;
//						for (InterfaceAddress address : iface.getInterfaceAddresses())
//							subnetMask = address.getNetworkPrefixLength();
//
//						internalNetmask = convertToSubnetMask(subnetMask);
//						break;
//					}
//				}
//
//				if (internalIp == null)
//					throw new IllegalStateException("cannot obtain ipv4 address of tap0");
//
//			} catch (SocketException e) {
//				throw new IllegalStateException("cannot obtain information of tap0");
//			}
			String[] res = getInternalIp(tunName);
			internalIp = (Inet4Address) Inet4Address.getByName(res[0]);
			internalNetmask = InetAddress.getByName(res[1]);
			
			this.ip = internalIp;
			this.netmask = internalNetmask;
			this.isEnabledBridge = false;
		}
	}
	
	private String[] getInternalIp(String tun) {
		String command = "ifconfig " + tun;  // <---- 실행할 쉘명령어
      String line="";
      Runtime rt = Runtime.getRuntime();
      Process ps = null;
      BufferedReader br = null;
      try{
        ps = rt.exec(command);
        br = new BufferedReader(new InputStreamReader(new SequenceInputStream(ps.getInputStream(), ps.getErrorStream())));
        String addr = "", mask = "";
        while((line = br.readLine()) != null){
            if(line.contains("inet addr:")) {
            	addr = extractInfo(line, "inet addr:");
            }
            if(line.contains("Mask:")) {
            	mask = extractInfo(line, "Mask:");
            }
        } 
        return new String[]{addr, mask};
      }catch(IOException ie){
          throw new IllegalStateException("cannot obtain ipv4 address of " + tun, ie);
      }catch(Exception e){
      	throw new IllegalStateException("cannot obtain ipv4 address of " + tun, e);
      } finally {
      	if (ps != null) {
      		closeInputStream(ps.getErrorStream());
      		closeInputStream(ps.getInputStream());
      		closeOutputStream(ps.getOutputStream());
      	}
      	
      	if(br != null) {
      		try {
      			br.close();
      		} catch (IOException e) {
      			throw new IllegalStateException("error occurred during buffered reader close", e);
      		}
         }
      }
	}

	private void closeInputStream(InputStream is) {
	   try {
	       is.close();
	   } catch (IOException e) {
	       e.printStackTrace();
	   }
	}
	
	private void closeOutputStream(OutputStream os) {
	   try {
	       os.close();
	   } catch (IOException e) {
	       e.printStackTrace();
	   }
	}
	
	private String extractInfo(String line, String find) {
		int start = line.indexOf(find);
      start += find.length();
      int end = line.indexOf(' ', start);
      if(end == -1)
           end = line.length();
      String result = line.substring(start, end);
      return result.trim();
	}

	private InetAddress convertToSubnetMask(short subnetMask) throws UnknownHostException {
		long bitmask = 1 << (32 - subnetMask);
		bitmask--;
		return IpLease.toInetAddress(~bitmask);

	}

	public InetAddress getIp() {
		return ip;
	}

	public InetAddress getNetmask() {
		return netmask;
	}

	public boolean getMode() {
		return isEnabledBridge;
	}
}