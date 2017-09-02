package kr.co.future.sslvpn.core.ftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class SftpService extends FtpService{
	private Logger logger = LoggerFactory.getLogger(SftpService.class);
	
	public SftpService(String host, String id, String pw, String path) {
		super(host, id, pw, path);
	}
	
	@Override
	public FtpCode uploadFile(String filePath) {
		ChannelSftp channelSftp = connect();
		if(channelSftp == null) {
			return FtpCode.ConnectionFail;
		}
		try{
			return upload(channelSftp, filePath);
		} finally {
			channelSftp.exit();
		}
	}
	
	@Override
	public FtpCode uploadFiles(List<String> filesPaths) {
		ChannelSftp channelSftp = connect();
		if(channelSftp == null) {
			return FtpCode.ConnectionFail;
		}
		FtpCode code = null;
		try{
			for(String filePath : filesPaths) {
				code = upload(channelSftp, filePath);
				if(code != FtpCode.Success)
					return code;
			}
			return code;
		} finally {
			channelSftp.exit();
		}
	}
	
	@Override
	public FtpCode downloadFile(String remoteFileName, String localPath) {
		ChannelSftp channelSftp = connect();
		if(channelSftp == null) {
			return FtpCode.ConnectionFail;
		}
		try{
			return download(channelSftp, remoteFileName, localPath);
		} finally {
			channelSftp.exit();
		}
	}
	
	@Override
	public FtpCode downloadFiles(List<String> remoteFileNames, String localPath) {
		ChannelSftp channelSftp = connect();
		if(channelSftp == null) {
			return FtpCode.ConnectionFail;
		}
		FtpCode code = null;
		try{
			for(String remoteFileName : remoteFileNames) {
				code = download(channelSftp, remoteFileName, localPath);
				if(code != FtpCode.Success)
					return code;
			}
			return code;
		} finally {
			channelSftp.exit();
		}
	}
	
	private ChannelSftp connect() {
      logger.info("==> Connecting to " + host);
      
      Session session = null;
      Channel channel = null;

      // 1. JSch 객체를 생성한다.
      JSch jsch = new JSch();
      try {
          // 2. 세션 객체를 생성한다 (사용자 이름, 접속할 호스트, 포트를 인자로 준다.)
          session = jsch.getSession(id, host, 22);
          // 3. 패스워드를 설정한다.
          session.setPassword(pw);
          // 타임아웃 설정 30초
          session.setTimeout(30000);
          // 4. 세션과 관련된 정보를 설정한다.
          java.util.Properties config = new java.util.Properties();
          // 4-1. 호스트 정보를 검사하지 않는다.
          config.put("StrictHostKeyChecking", "no");
          session.setConfig(config);

          // 5. 접속한다.
          session.connect();

          // 6. sftp 채널을 연다.
          channel = session.openChannel("sftp");

          // 7. 채널에 연결한다.
          channel.connect();
      } catch (JSchException e) {
          logger.error("connect failed", e);
          return null;
      }

      logger.info("==> Connected to " + host);
      // 8. 채널을 FTP용 채널 객체로 캐스팅한다.
      return (ChannelSftp) channel;
	}
		
	public FtpCode upload(ChannelSftp channelSftp, String localPath) {
		logger.info("==> Uploading: [{}] to host=[{}] path=[{}]", new Object[]{localPath, host, path});
		FileInputStream in = null;
		try {
			// 입력 파일을 가져온다.
			in = new FileInputStream(localPath);
		 
			String remotePath = new File(localPath).getName();
			if(this.path != null && !path.equals("")) {
				remotePath = path + "/" + remotePath;         	 
			} 
			remotePath = remotePath.replace("//", "/");
			remotePath = remotePath.trim();
		 
			// 파일을 업로드한다.
			channelSftp.put(in, remotePath);
		} catch (SftpException e) {			 
			logger.error("sftp file upload error", e);
			return FtpCode.UploadFileFail;
		} catch (FileNotFoundException e) {
			logger.error("file not found fo uploading", e);
			return FtpCode.UploadFileNotFound;
		} finally {
			try {
				// 업로드된 파일을 닫는다.
				if(in != null)
					in.close();
			} catch (IOException e) {
				logger.error("close stream error", e);
			}
		}
      
		logger.info("==> Uploaded: [{}] to host=[{}] path=[{}]", new Object[]{localPath, host, path});
	 
		return FtpCode.Success;
	}
	 
	public FtpCode download(ChannelSftp channelSftp, String remoteFileName, String localPath) {
		logger.info("==> Downloading: [{}] from host=[{}] path=[{}]", new Object[]{remoteFileName, host, path});
	     
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		try{
			String remotePath = remoteFileName;
			if(path != null && !path.equals("")) {
				remotePath = path + "/" + remoteFileName;
			}
			remotePath = remotePath.replace("//", "/");
			remotePath = remotePath.trim();
	      	
			byte[] buffer = new byte[8192];
			bis = new BufferedInputStream(channelSftp.get(remotePath));
			
			localPath += "/" + remoteFileName;
			localPath = localPath.replace("//", "/");
         localPath = localPath.trim();
			
			File newFile = new File(localPath);
			OutputStream os = new FileOutputStream(newFile);
			bos = new BufferedOutputStream(os);
			int readCount;
			//System.out.println("Getting: " + theLine);
			while( (readCount = bis.read(buffer)) > 0) {            
				bos.write(buffer, 0, readCount);
			}          
		} catch (SftpException e) {
			logger.error("sftp donwload error", e);
			return FtpCode.DownloadFileFail;
		} catch (FileNotFoundException e) {
			logger.error("file not found fo downloading", e);
			return FtpCode.DownloadFileNotFound;
		} catch (IOException e) {
			logger.error("ftp donwload error", e);
			return FtpCode.DownloadFileFail;
		} finally {
			try {
				if(bis != null)
					bis.close();
				if(bos != null)
					bos.close();
			} catch (IOException e) {
				logger.error("close stream error", e);
			}
			
		 }
	       
		logger.info("==> Downloaded: [{}] from host=[{}] path=[{}]", new Object[]{remoteFileName, host, path});
		
		return FtpCode.Success;
	}
}
