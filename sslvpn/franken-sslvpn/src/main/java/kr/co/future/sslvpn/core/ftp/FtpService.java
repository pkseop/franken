package kr.co.future.sslvpn.core.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.List;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FtpService {
	
	protected Logger logger = LoggerFactory.getLogger(FtpService.class);
	
	protected String host;
	
	protected String id;
	
	protected String pw;
	
	protected String path;
	
	protected boolean isBinary;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPw() {
		return pw;
	}

	public void setPw(String pw) {
		this.pw = pw;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	public boolean isBinary() {
		return isBinary;
	}

	public void setBinary(boolean isBinary) {
		this.isBinary = isBinary;
	}
	
	public FtpService(String host, String id, String pw, String path) {
		this.host = host;
		this.id = id;
		this.pw = pw;
		this.path = path;
	}
	
	public FtpService(String host, String id, String pw, String path, boolean isBinary) {
		this.host = host;
		this.id = id;
		this.pw = pw;
		this.path = path;
		this.isBinary = isBinary;
	}
	
	public FtpCode uploadFile(String filePath) {
		FTPClient ftpClient = null;
		try {
			ftpClient = connect();
			if(ftpClient == null) {
      		return FtpCode.ConnectionFail;
      	}	
			return upload(ftpClient, filePath);
		} finally {
			connClose(ftpClient);
		}
	}
	
	public FtpCode uploadFiles(List<String> filesPaths) {
		FTPClient ftpClient = null;

		try {
			ftpClient = connect();
			if(ftpClient == null) {
				return FtpCode.ConnectionFail;
			}	
			FtpCode code = null;
			for(String path : filesPaths) {
				code = upload(ftpClient, path);
				if(code != FtpCode.Success)
					return code;
			}
			return code;
		} finally {
			connClose(ftpClient);
		}
	}
	
	public FtpCode downloadFile(String remoteFileName, String localPath) {
		FTPClient ftpClient = null;

		try {
			ftpClient = connect();
			if(ftpClient == null) {
				return FtpCode.ConnectionFail;
			}	
			return download(ftpClient, remoteFileName, localPath);
		} finally {
			connClose(ftpClient);
		}
	}
	
	public FtpCode downloadFiles(List<String> remoteFileNames, String localPath) {
		FTPClient ftpClient = null;

		try {
			ftpClient = connect();
			if(ftpClient == null) {
				return FtpCode.ConnectionFail;
			}
      	
			FtpCode code = null;
			for(String remoteFileName : remoteFileNames) {
				code = download(ftpClient, remoteFileName, localPath);
				if(code != FtpCode.Success)
					return code;
			}
			return code;
		} finally {
			connClose(ftpClient);
		}
	}
	
//pks. 2015-01-15 사용하지 않아 주석처리함.	
//	public FtpCode deleteFile(String remoteFileName) {
//		FTPClient ftpClient = null;
//		
//		try {
//			ftpClient = connect();
//			ftpClient = connect();
//			if(ftpClient == null) {
//				return FtpCode.ConnectionFail;
//			}
//			return delete(ftpClient, remoteFileName);
//		} finally {
//			connClose(ftpClient);
//		}	
//	}
//	
//	public FtpCode deleteFiles(List<String> remoteFileNames) {
//		FTPClient ftpClient = null;
//		
//		try {
//			ftpClient = connect();
//			ftpClient = connect();
//			if(ftpClient == null) {
//				return FtpCode.ConnectionFail;
//			}
//			
//			FtpCode code = null;
//			for(String remoteFileName : remoteFileNames) {
//				code = delete(ftpClient, remoteFileName);
//				if(code != FtpCode.Success)
//					return code;
//			}
//			return code;
//		} finally {
//			connClose(ftpClient);
//		}	
//	}
	
	private void connClose(FTPClient ftpClient) {
		if (ftpClient != null && ftpClient.isConnected()) {
   		try {
   			ftpClient.logout();
   			ftpClient.disconnect();
   		} catch (IOException e) {
   			logger.error("ftp connection close error", e);
   		}
   	}
	}
	
	private boolean setWorkingDir(FTPClient ftpClient) throws IOException {
		if(path != null && !path.equals("")) {
			ftpClient.changeWorkingDirectory(path);
			int reply = ftpClient.getReplyCode(); 
	   		if(!FTPReply.isPositiveCompletion(reply)) {
	   			logger.error("[{}] path does not exist.", path);
	      		return false;
	   		}
		}
		return true;
	}
	
	//binayr 파일일 경우 file type을 binary로 설정하지 않으면 업로드 또는 다운로드된 파일이 깨진 형태로 되게 된다.
	private void setFileType(FTPClient ftpClient) throws IOException {
		if(isBinary) {
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		} else {
			ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
		}
	}
	
	private FTPClient connect() {
		logger.info("==> Connecting to " + host);
		
		FTPClient ftpClient = new FTPClient();        
      try {
      	ftpClient.connect(host); 
      	int reply = ftpClient.getReplyCode(); // 응답코드가 비정상이면 종료합니다
      	if (!FTPReply.isPositiveCompletion(reply)) {
      		logger.error("server refused connect.");
      		return null;
      	} else {
      		ftpClient.setSoTimeout(30000);  // 현재 커넥션 timeout을 millisecond 값으로 입력합니다      		
      		ftpClient.login(id, pw); // 로그인 유저명과 비밀번호를 입력 합니다
      		reply = ftpClient.getReplyCode(); 
      		if(!FTPReply.isPositiveCompletion(reply)) {
      			logger.error("ftp login failed");
         		return null;
      		}
      		if(setWorkingDir(ftpClient) == false) {      			
   			  return null;
      		}
      	}
      } catch (SocketException e) {
      	logger.error("connect failed", e);
         return null;
      } catch (IOException e) {
      	logger.error("connect failed", e);
         return null;
      }
      
      logger.info("==> Connected to " + host);
      
      return ftpClient;
	}
	
	private FtpCode upload(FTPClient ftpClient, String localPath) {
		logger.info("==> Uploading: [{}] to host=[{}] path=[{}]", new Object[]{localPath, host, path});
      
		FileInputStream inputStream = null;
      boolean result = false;
      try {
      	File localFile = new File(localPath);
      	inputStream = new FileInputStream(localFile);
      	String remotePath = localFile.getName();
          
      	setFileType(ftpClient);
      	result = ftpClient.storeFile(remotePath, inputStream);
      } catch (FileNotFoundException e) {
      	logger.error("file not found for uploading", e);
			return FtpCode.UploadFileNotFound;
      } catch (IOException e) {
      	logger.error("ftp file upload error", e);
			return FtpCode.UploadFileFail;
      } finally {
      	try {
      		if(inputStream != null)
      			inputStream.close();
      	} catch (IOException e) {
      		logger.error("close stream error", e);
      	}
      }
      
      if(result) {
	      logger.info("==> Uploaded: [{}] to host=[{}] path=[{}]", new Object[]{localPath, host, path});
	      return FtpCode.Success;
      } else {
      	logger.error("==> Upload failed: [{}] to host=[{}] path=[{}]. reply msg [{}]", new Object[]{localPath, host, path, ftpClient.getReplyString()});
	      return FtpCode.UploadFileFail;
      }
  }
  
  private FtpCode download(FTPClient ftpClient, String remoteFileName, String localPath) {
	  logger.info("==> Downloading: [{}] from host=[{}] path=[{}]", new Object[]{remoteFileName, host, path});
	  
	  OutputStream outputStream = null;
	  boolean result = false;
	  try {
		  localPath += "/" + remoteFileName;
		  localPath = localPath.replace("//", "/");
		  localPath = localPath.trim();
		  
		  File file = new File(localPath);  
		  outputStream = new FileOutputStream(file);
		  setFileType(ftpClient);
		  result = ftpClient.retrieveFile(remoteFileName, outputStream);
	  } catch (FileNotFoundException e) {
		  logger.error("file not found fo downloading", e);
		  return FtpCode.DownloadFileNotFound;          
	  } catch (IOException e) {
		  logger.error("ftp donwload error", e);
		  return FtpCode.DownloadFileFail;
	  } finally {
		  try {
			  if(outputStream != null)
				  outputStream.close();
		  } catch (IOException e) {
			  logger.error("close stream error", e);
		  }
	  }
	  
	  if(result) {
		  logger.info("==> Downloaded: [{}] from host=[{}] path=[{}]", new Object[]{remoteFileName, host, path});
		  return FtpCode.Success;
	  } else {
		  logger.error("==> Download failed: [{}] from host=[{}] path=[{}]. reply msg [{}]", new Object[]{remoteFileName, host, path, ftpClient.getReplyString()});
		  return FtpCode.DownloadFileFail;
	  }
  }
  
//pks. 2015-01-15 사용하지 않아 주석처리함.
//  private FtpCode delete(FTPClient ftpClient, String remoteFileName) {
//	  logger.info("==> Deleting: [{}] from host=[{}] path=[{}]", new Object[]{remoteFileName, host, path});
//      
//	  boolean result = false;
//	  try {	  
//		  ftpClient.deleteFile(remoteFileName);
//	  } catch (FileNotFoundException e) {
//		  logger.error("file not found for deleting", e);
//		  return FtpCode.DeleteFail;
//	  } catch (IOException e) {
//		  logger.error("ftp file delete error", e);
//		  return FtpCode.DeleteFail;
//	  } 
//    
//	  if(result) {
//		  logger.info("==> Deleted: [{}] from host=[{}] path=[{}]", new Object[]{remoteFileName, host, path});
//		  return FtpCode.Success;
//	  } else {
//		  logger.error("==> Delete failed: [{}] from host=[{}] path=[{}]. reply msg [{}]", new Object[]{remoteFileName, host, path, ftpClient.getReplyString()});
//		  return FtpCode.DeleteFail;
//	  }
//  }
}
