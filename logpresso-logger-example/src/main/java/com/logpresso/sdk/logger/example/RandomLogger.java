package com.logpresso.sdk.logger.example;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.araqne.log.api.AbstractLogger;
import org.araqne.log.api.Log;
import org.araqne.log.api.LoggerFactory;
import org.araqne.log.api.LoggerSpecification;
import org.araqne.log.api.SimpleLog;

public class RandomLogger extends AbstractLogger {

	public RandomLogger(LoggerSpecification spec, LoggerFactory factory) {
		super(spec, factory);
	}

	// run with specified interval
	@Override
	protected void runOnce() {
		int repeat = 1;
		if (getConfigs().get("repeat") != null)
			repeat = Integer.parseInt(getConfigs().get("repeat"));

		for (int i = 0; i < repeat; i++) {
			long num = new Random().nextLong();
			String line = "random number: " + Long.toString(num);

			Map<String, Object> params = new HashMap<String, Object>();
			params.put("line", line);

			Log log = new SimpleLog(new Date(), getFullName(), params);
			write(log);
		}
	}

}
