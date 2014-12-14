public class dfsFile {
	//filename is the entire path
	public String fileName;
	public long lastModTime;
	public int versionNum;
	
	public dfsFile( String fileName, long lastModTime){
		this.fileName = fileName;
		this.lastModTime = lastModTime;		
		
		//default version
		this.versionNum = 0;
	}
	
	
	//getter and setters
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public long getLastModTime() {
		return lastModTime;
	}
	public void setLastModTime(long lastModTime) {
		this.lastModTime = lastModTime;
	}

	public int getVersionNum(){
		return versionNum;
	}
	
	public void setVersionNum(int versionNum){
		this.versionNum = versionNum;
	}
}
