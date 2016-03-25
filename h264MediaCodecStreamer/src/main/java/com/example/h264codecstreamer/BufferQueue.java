package com.example.h264codecstreamer;

import java.util.concurrent.Semaphore;

// Thread safe byte buffer queue
public class BufferQueue {
	private byte[] buff;
	private volatile int head, tail, count, size;
	private Semaphore appendsem, readsem, countsem;

	// Constructor
	public BufferQueue(int size) {
		appendsem = new Semaphore(1, true);
		readsem = new Semaphore(1, true);
		countsem = new Semaphore(1, true);
		buff = new byte[size];
		this.size = size;
		head = 0;
		tail = 0;
		count = 0;
	}

	// Get the number of bytes in the buffer
	public int getCount() {
		return count;
	}

	// Append bytes to the buffer
	public void append(byte[] data) {
		if (data != null)
			append(data, 0, data.length);
	}

	public void append(byte[] data, int offset, int length) {
		if (data == null)
			return;
		if (data.length < offset + length) {
			throw new RuntimeException(
					"array index out of bounds. offset + length extends beyond the length of the array.");
		}

		try {
			appendsem.acquire();
		} catch (InterruptedException e) {
			return;
		}
		// We need to acquire the semaphore so that this.tail doesn't change.
		for (int i = 0; i < length; i++)
			buff[(i + this.tail) % this.size] = data[i + offset];
		this.tail = (length + this.tail) % this.size;
		try {
			countsem.acquire();
		} catch (InterruptedException e) {
			return;
		}
		// We need to acquire the semaphore so that this.count doesn't change.
		this.count = this.count + length;
		if (this.count > this.size)
			throw new RuntimeException("Buffer overflow error.");
		countsem.release();
		appendsem.release();
	}

	// Read bytes from the buffer
	public int read(byte[] data) {
		if (data != null)
			return read(data, 0, data.length);
		else
			return 0;
	}

	public int read(byte[] data, int offset, int length) {
		if (data == null)
			return 0;
		if (data.length < offset + length)
			throw new RuntimeException(
					"array index out of bounds. offset + length extends beyond the length of the array.");

		int readlength = 0;

		try {
			readsem.acquire();
		} catch (InterruptedException e) {
			return 0;
		}
		// We need to acquire the semaphore so that this.head doesn't change.
		for (int i = 0; i < length; i++) {
			if (i == count)
				break;
			data[i + offset] = buff[(i + head) % this.size];
			readlength++;
		}
		this.head = (readlength + this.head) % this.size;
		try {
			countsem.acquire();
		} catch (InterruptedException e) {
			readsem.release();
			return 0;
		}
		// We need to acquire the semaphore so that this.count doesn't change.
		this.count = this.count - readlength;
		countsem.release();
		readsem.release();

		return readlength;
	}

	public int peek(byte[] data) {
		if (data != null)
			return peek(data, 0, data.length);
		else
			return 0;
	}

	public int peek(byte[] data, int offset, int length) {
		if (data == null)
			return 0;
		if (data.length < offset + length)
			throw new RuntimeException(
					"array index out of bounds. offset + length extends beyond the length of the array.");

		int readlength = 0;

		try {
			readsem.acquire();
		} catch (InterruptedException e) {
			return 0;
		}
		// We need to acquire the semaphore so that this.head doesn't change.
		for (int i = 0; i < length; i++) {
			if (i == count)
				break;
			data[i + offset] = buff[(i + head) % this.size];
			readlength++;
		}

		readsem.release();

		return readlength;
	}

	public byte[] readBytes() {
		byte[] data = new byte[count];
		try {
			read(data);
		} catch (Exception ex) {
		}
		return data;
	}
}
