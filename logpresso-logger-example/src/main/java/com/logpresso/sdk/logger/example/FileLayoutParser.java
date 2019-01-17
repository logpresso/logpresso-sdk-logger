package com.logpresso.sdk.logger.example;

import java.io.File;
import java.util.Map;

import org.araqne.log.api.V1LogParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileLayoutParser extends V1LogParser {
	private final Logger slog = LoggerFactory.getLogger(FileLayoutParser.class);
	private final String absolutePath;
	private FileLayoutParserFactory factory;

	public FileLayoutParser(Map<String, String> configs, FileLayoutParserFactory factory) {
		this.factory = factory;
		String path = configs.get("path");
		if (path == null)
			throw new IllegalArgumentException("path should be not null");

		this.absolutePath = new File(path).getAbsolutePath();
	}

	@Override
	public Map<String, Object> parse(Map<String, Object> params) {
		Object o = params.get("line");
		if (!(o instanceof String))
			return params;

		String line = o.toString();

		FileLayout layout = factory.getLayout(absolutePath);
		if (layout == null)
			return params;

		try {
			Map<String, Object> m = layout.parse(line);
			if (m == null)
				return params;

			return m;
		} catch (Throwable t) {
			if (slog.isDebugEnabled())
				slog.debug("logger example: cannot parse layout - " + line, t);
			
			return params;
		}
	}

}
