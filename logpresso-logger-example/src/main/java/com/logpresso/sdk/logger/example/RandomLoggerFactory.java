package com.logpresso.sdk.logger.example;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.araqne.log.api.AbstractLoggerFactory;
import org.araqne.log.api.IntegerConfigType;
import org.araqne.log.api.Logger;
import org.araqne.log.api.LoggerConfigOption;
import org.araqne.log.api.LoggerSpecification;

@Component(name = "random-logger-factory")
@Provides
public class RandomLoggerFactory extends AbstractLoggerFactory {
	@Override
	public String getName() {
		return "random";
	}

	@Override
	public String getDisplayName(Locale locale) {
		return "Random";
	}

	@Override
	public String getDescription(Locale locale) {
		return "Random number logger";
	}

	@Override
	public Collection<LoggerConfigOption> getConfigOptions() {
		// optional config specification
		LoggerConfigOption repeat = new IntegerConfigType("repeat", t("Repeat count"), t("Generated logs per iteration"), false);
		return Arrays.asList(repeat);
	}

	private Map<Locale, String> t(String en) {
		Map<Locale, String> m = new HashMap<Locale, String>();
		m.put(Locale.ENGLISH, en);
		return m;
	}

	@Override
	protected Logger createLogger(LoggerSpecification spec) {
		return new RandomLogger(spec, this);
	}
}
