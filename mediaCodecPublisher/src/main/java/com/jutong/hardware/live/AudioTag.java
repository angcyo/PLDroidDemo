package com.jutong.hardware.live;

public class AudioTag {

	private byte[] data;

	private int currentBit = 0;

	public AudioTag() {
		data = new byte[2];
	}

	public int getCurrentBit() {
		return currentBit;
	}

	public byte[] getData() {
		return data;
	}
	
	public void addCurrentBit(int currentBit) {
		this.currentBit += currentBit;
	}
}
