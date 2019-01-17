package com.logpresso.sdk.logger.example;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FileLayout {
	private File file;
	private long timestamp;
	private String[] fields;
	private int[] lengths;
	private int totalLength;

	public Map<String, Object> parse(String line) {
		if (line.length() < totalLength)
			return null;

		Map<String, Object> m = new HashMap<String, Object>();
		int start = 0;
		for (int i = 0; i < lengths.length; i++) {
			String value = line.substring(start, start + lengths[i]);
			start += lengths[i];
			m.put(fields[i], value);
		}

		return m;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String[] getFields() {
		return fields;
	}

	public void setFields(String[] fields) {
		this.fields = fields;
	}

	public int[] getLengths() {
		return lengths;
	}

	public void setLengths(int[] lengths) {
		this.lengths = lengths;

		if (lengths == null) {
			this.totalLength = 0;
			return;
		}

		int total = 0;
		for (int len : lengths)
			total += len;

		this.totalLength = total;
	}
}
