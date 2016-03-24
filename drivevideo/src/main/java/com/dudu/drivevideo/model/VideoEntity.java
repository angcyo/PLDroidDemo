package com.dudu.drivevideo.model;

import java.io.File;
import java.io.Serializable;

public class VideoEntity implements Serializable {

	private static final long serialVersionUID = -4640760092040929571L;

	private String name;

	private String path;

	private String createTime;

	private File file;

	private String size;

	private int status = 0;

	public VideoEntity() {
		
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getCreateTime() {
		return createTime;
	}

	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}

	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

}
