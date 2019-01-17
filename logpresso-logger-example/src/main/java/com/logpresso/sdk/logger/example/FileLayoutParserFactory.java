package com.logpresso.sdk.logger.example;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.araqne.cron.MinutelyJob;
import org.araqne.log.api.AbstractLogParserFactory;
import org.araqne.log.api.LogParser;
import org.araqne.log.api.LoggerConfigOption;
import org.araqne.log.api.MutableStringConfigType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MinutelyJob
@Component(name = "file-layout-parser-factory")
@Provides
public class FileLayoutParserFactory extends AbstractLogParserFactory implements Runnable {
	private final Logger slog = LoggerFactory.getLogger(FileLayoutParserFactory.class);
	private ConcurrentHashMap<String, FileLayout> layouts = new ConcurrentHashMap<String, FileLayout>();
	private CopyOnWriteArraySet<String> paths = new CopyOnWriteArraySet<String>();

	@Override
	public String getName() {
		return "file-layout";
	}

	@Override
	public String getDisplayName(Locale locale) {
		return "파일 레이아웃";
	}

	@Override
	public String getDescription(Locale locale) {
		return "레이아웃 파일을 읽어들여 고정 길이 전문을 파싱합니다.";
	}

	@Override
	public Collection<LoggerConfigOption> getConfigOptions() {
		LoggerConfigOption path = new MutableStringConfigType("path", locales("File Path", "파일 경로"), locales(
		        "Path of layout file configured in CSV format of field=length pair.", "필드=길이 쌍의 CSV 형식으로 구성된 레이아웃 파일의 경로"), true);
		return Arrays.asList(path);
	}

	private Map<Locale, String> locales(String en, String ko) {
		Map<Locale, String> m = new HashMap<Locale, String>();
		m.put(Locale.ENGLISH, en);
		m.put(Locale.KOREAN, ko);
		return m;
	}

	public FileLayout getLayout(String path) {
		if (!paths.contains(path))
			paths.add(path);

		return layouts.get(path);
	}

	private FileLayout loadLayout(File file) throws IOException {
		slog.debug("logger example: check file layout from [{}]", file.getAbsolutePath());

		if (!file.exists()) {
			slog.debug("logger example: file [{}] not found", file.getAbsolutePath());
			return null;
		}

		if (!file.canRead()) {
			slog.debug("logger example: check file [{}] read permission", file.getAbsolutePath());
			return null;
		}

		long lastModified = file.lastModified();
		FileLayout oldLayout = layouts.get(file.getAbsolutePath());
		if (oldLayout != null && lastModified <= oldLayout.getTimestamp())
			return oldLayout;

		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file, "r");
			int len = (int) raf.length();
			byte[] buf = new byte[len];
			raf.readFully(buf);

			String line = new String(buf, "utf-8");
			String[] tokens = line.split(",");

			String[] fields = new String[tokens.length];
			int[] lengths = new int[tokens.length];

			int i = 0;
			for (String token : tokens) {
				int p = token.indexOf('=');
				String key = token.substring(0, p);
				String value = token.substring(p + 1);

				fields[i] = key;
				lengths[i] = Integer.parseInt(value);
				i++;
			}

			FileLayout layout = new FileLayout();
			layout.setFile(file);
			layout.setFields(fields);
			layout.setLengths(lengths);
			layout.setTimestamp(lastModified);

			layouts.put(file.getAbsolutePath(), layout);

			slog.info("logger example: loaded new file layout from [{}]", file.getAbsolutePath());

			return layout;
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (Throwable t) {
				}
			}
		}
	}

	@Override
	public LogParser createParser(Map<String, String> configs) {
		try {
			String path = configs.get("path");
			FileLayout layout = loadLayout(new File(path));
			if (layout == null)
				throw new IllegalStateException("layout not found: " + path);

			paths.add(path);
			return new FileLayoutParser(configs, this);
		} catch (IOException e) {
			throw new IllegalStateException("cannot create layout parser", e);
		}
	}

	@Override
	public void run() {
		slog.debug("logger example: reload file layouts");
		// check updated file and reload layout
		for (String path : paths) {
			try {
				loadLayout(new File(path));
			} catch (Throwable t) {
				slog.error("logger example: cannot load layout file " + path, t);
			}
		}
	}
}
