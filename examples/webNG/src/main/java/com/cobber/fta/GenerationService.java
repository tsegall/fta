package com.cobber.fta;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.faker.FakerLT;
import com.cobber.fta.faker.FakerParameters;
import com.cobber.fta.PluginLocaleEntry;

@Service
public class GenerationService {

	public String generate(final List<FakerParameters> params, final int count, final String locale)
			throws FTAPluginException {

		final Locale analysisLocale = (locale != null && !locale.isBlank())
				? Locale.forLanguageTag(locale)
				: Locale.getDefault();

		final TextAnalyzer analyzer = TextAnalyzer.getDefaultAnalysis(analysisLocale);
		final Collection<LogicalType> registered = analyzer.getPlugins().getRegisteredSemanticTypes();
		final Random random = new Random(31415926);

		final FakerParameters[] parameters = new FakerParameters[params.size()];
		final LogicalType[] logicals = new LogicalType[params.size()];

		for (final FakerParameters param : params) {
			parameters[param.index] = param;
			parameters[param.index].bind();
		}

		for (int i = 0; i < params.size(); i++) {
			for (final LogicalType logical : registered) {
				if (logical.getSemanticType().equals(parameters[i].type)) {
					logicals[i] = logical;
					break;
				}
			}

			if (logicals[i] == null) {
				final String clazz = parameters[i].getClazz();
				if (clazz == null)
					throw new IllegalArgumentException("Unknown type: " + parameters[i].type);
				final PluginDefinition plugin = new PluginDefinition(parameters[i].type, clazz);
				// The 2-arg constructor restricts to "en"; override to accept any locale.
				plugin.validLocales = PluginLocaleEntry.simple(new String[] { "*" });
				logicals[i] = LogicalTypeFactory.newInstance(plugin, analyzer.getConfig());
				((FakerLT) logicals[i]).setControl(parameters[i]);
			}
		}

		final StringBuilder csv = new StringBuilder();

		for (int i = 0; i < logicals.length; i++) {
			if (i != 0) csv.append(',');
			csv.append(quoteIfNeeded(parameters[i].fieldName));
		}
		csv.append('\n');

		for (int r = 0; r < count; r++) {
			for (int i = 0; i < logicals.length; i++) {
				if (i != 0) csv.append(',');
				if (parameters[i].nullPercent != 0 && random.nextDouble() <= parameters[i].nullPercent) {
					// null — leave empty
				} else if (parameters[i].blankPercent != 0 && random.nextDouble() <= parameters[i].blankPercent) {
					final int blankLen = parameters[i].blankLength == -1 ? random.nextInt(6) : parameters[i].blankLength;
					csv.append('"');
					for (int b = 0; b < blankLen; b++) csv.append(' ');
					csv.append('"');
				} else {
					csv.append(quoteIfNeeded(logicals[i].nextRandom()));
				}
			}
			csv.append('\n');
		}

		return csv.toString();
	}

	private String quoteIfNeeded(final String input) {
		if (input == null) return "";
		if (input.indexOf(',') == -1 && input.indexOf('"') == -1)
			return input;
		return "\"" + input.replace("\"", "\"\"") + "\"";
	}
}
