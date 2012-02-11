package com.ventus.smartphonequadrotor.qphoneapp.util.json;

public class CameraResponse extends ResponseAbstract {
	private String FileType;	//string compression scheme
	private String Data;		//hex dump

	public CameraResponse(long Timestamp, String fileType, String data) {
		super(Timestamp);
		FileType = fileType;
		Data = data;
	}

	public String getFileType() {
		return FileType;
	}

	public String getData() {
		return Data;
	}

	@Override
	public String toString() {
		return String.format("%1$sFileType: %2$s\nData: %3$s\n", super.toString(), FileType, Data);
	}

}
