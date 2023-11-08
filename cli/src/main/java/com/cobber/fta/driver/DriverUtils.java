package com.cobber.fta.driver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.text.StringEscapeUtils;

import com.cobber.fta.Content;
import com.cobber.fta.LogicalType;
import com.cobber.fta.SemanticType;
import com.cobber.fta.SingletonSet;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.Utils;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

public class DriverUtils {
	public static LogicalType getLogicalType(final TextAnalyzer analyzer, final String pluginName) {
		final Collection<LogicalType> registered = analyzer.getPlugins().getRegisteredSemanticTypes();

		for (final LogicalType logical : registered)
			if (logical.getSemanticType().equals(pluginName))
				return logical;

		return null;
	}

	public static void createNormalizedOutput(final String inputName) throws UnsupportedEncodingException, FileNotFoundException, IOException {
		final File source = new File(inputName);
		final File baseDirectory = source.getParentFile();
		final String baseName = Utils.getBaseName(source.getName());
		final SingletonSet memberSet;

		memberSet = new SingletonSet(new Content("file", inputName));
		final Set<String> newSet = new TreeSet<>(memberSet.getMembers());

		for (final String member : memberSet.getMembers()) {
			if (!Normalizer.isNormalized(member, Normalizer.Form.NFKD)) {
				final String cleaned = Normalizer.normalize(member, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
				newSet.add(cleaned);
			}
		}

		if (newSet.size() != memberSet.getMembers().size()) {
			final File newFile = new File(baseDirectory, baseName + "_new.csv");

			try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newFile), "UTF-8"))) {
				for (final String member : newSet)
					out.write(member + "\n");
			}
		}
		else
			System.err.println("Error: no new entries generated!");
	}

	public static void createBloomOutput(final String inputName, final String funnelType) throws UnsupportedEncodingException, FileNotFoundException, IOException {
		// Desired sample size
		final int SAMPLE_SIZE = 200;

		final File source = new File(inputName);
		final File baseDirectory = source.getParentFile();
		final String baseName = Utils.getBaseName(source.getName());
		final File bloomFile = new File(baseDirectory, baseName + ".bf");
		final File sampleFile = new File(baseDirectory, baseName + "_s.csv");
		int lineCount = 0;

		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(source), "UTF-8"))) {
			String input;
			while ((input = in.readLine()) != null) {
				final String trimmed = input.trim();
				if (trimmed.length() == 0 || trimmed.charAt(0) == '#')
					continue;
				lineCount++;
			}
		}

		final int samplingFrequency = (lineCount + SAMPLE_SIZE - 1) / SAMPLE_SIZE;

		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(source), "UTF-8"))) {
			final List<String> samples = new ArrayList<>(SAMPLE_SIZE);
			String input;
			int recordCount = 0;

			if ("integer".equalsIgnoreCase(funnelType)) {
				final BloomFilter<Integer> filter = BloomFilter.create(Funnels.integerFunnel(), lineCount, 0.005);

				while ((input = in.readLine()) != null) {
					final String trimmed = input.trim();
					if (trimmed.length() == 0 || trimmed.charAt(0) == '#')
						continue;
					filter.put(Integer.valueOf(trimmed));
					if (++recordCount%samplingFrequency == 0)
						samples.add(trimmed);
				}

				try (OutputStream filterStream = new FileOutputStream(bloomFile)) {
					filter.writeTo(filterStream);
				}
			}
			else {
				final BloomFilter<CharSequence> filter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), lineCount, 0.005);

				while ((input = in.readLine()) != null) {
					final String trimmed = input.trim();
					if (trimmed.length() == 0 || trimmed.charAt(0) == '#')
						continue;
					filter.put(trimmed);
					if (++recordCount%samplingFrequency == 0)
						samples.add(trimmed);
				}

				try (OutputStream filterStream = new FileOutputStream(bloomFile)) {
					filter.writeTo(filterStream);
				}
			}

			try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sampleFile), "UTF-8"))) {
				for (final String sample : samples)
					out.write(sample + "\n");
			}
		}
	}

	public static void createSemanticTypesMarkdown() throws UnsupportedEncodingException, FileNotFoundException, IOException {
		System.err.println("|Semantic Type|Description|Documentation|Locale|");
		System.err.println("|-------------|-----------|-------------|------|");

		for (final SemanticType semanticType : SemanticType.getAllSemanticTypes()) {
			System.err.printf("|%s|%s|", StringEscapeUtils.escapeHtml4(semanticType.getId()), StringEscapeUtils.escapeHtml4(semanticType.getDescription()));
			if (semanticType.getDocumentation() != null)
				System.err.printf("%s", String.join(", ", semanticType.getDocumentation()));
			System.err.print("|");
			if (semanticType.getLanguages() != null)
				System.err.printf("%s", String.join(", ", semanticType.getLanguages()));
			System.err.println("|");
		}

		System.err.println("\nNote:\n");
		System.err.println("Any of the above Semantic Types suffixed with one of the following are locale-sensitive:\n");
		System.err.println(" * &lt;LOCALE&gt; - replaced by the locale, for example, MONTH.FULL_fr-FR (Month Abbreviation in french)");
		System.err.println(" * &lt;LANGUAGE&gt; - replaced by the language from the locale, for example, GENDER.TEXT_PT (Gender in Portuguese)");
		System.err.println(" * &lt;COUNTRY&gt; - replaced by the country from the locale, for example, POSTAL_CODE.POSTAL_CODE_AU (Australian Postal Code)");
	}
}
