/*
 * Copyright 2017-2021 Tim Segall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cobber.fta;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.cobber.fta.core.TraceException;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Class used to manage tracing for FTA.
 */
public class Trace {
	private Writer traceWriter;
	private long traceSampleCount = 1000;
	private long batchCount;
	private boolean enabled = true;
	private	final JsonStringEncoder jsonStringEncoder = JsonStringEncoder.getInstance();

	public Trace(final String trace, final AnalyzerContext context, final AnalysisConfig analysisConfig) {
		final String[] traceSettings = trace.split(",");
		String traceDirectory = null;
		String streamName = null;

		for (final String traceSetting : traceSettings) {
			final String[] traceComponents = traceSetting.split("=");
			switch (traceComponents[0]) {
			case "enabled":
				enabled = "true".equalsIgnoreCase(traceComponents[1]);
				break;
			case "stream":
				streamName = traceComponents[1];
				break;
			case "directory":
				traceDirectory = traceComponents[1];
				final Path validate = Paths.get(traceDirectory);
				if (!Files.isDirectory(validate) || !Files.isWritable(validate))
					throw new TraceException("Supplied directory either does not exist or is not writable");
				break;
			case "samples":
				traceSampleCount = Long.valueOf(traceComponents[1]);
				break;
			default:
				throw new TraceException("Unrecognized trace option: '" + traceComponents[0] +
						"' - expected enabled, stream, directory, or samples");
			}
		}

		if (!enabled || (streamName != null && !streamName.equals(context.getStreamName())))
			return;

		if (traceDirectory == null)
			traceDirectory = System.getProperty("java.io.tmpdir");

		try {
			String filename = "";
			if (context.getCompositeName() != null)
				filename += context.getCompositeName() + "_";
			filename += context.getStreamName() + ".fta";
			// There are a lot of characters that Windows does not like in filenames ...
			filename = filename.replaceAll("[<>:\"/\\\\|\\?\\*]", "_");
			traceWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(traceDirectory, filename))));
		} catch (FileNotFoundException e) {
			throw new TraceException("Cannot create file to write", e);
		}

		final ObjectMapper mapper = new ObjectMapper();
		try {
			final ObjectNode analysisNode = mapper.createObjectNode();
			analysisNode.set("analysisConfig", mapper.convertValue(analysisConfig, JsonNode.class));
			final String analysisNodeAsText = mapper.writeValueAsString(analysisNode);
			traceWriter.write(analysisNodeAsText);

			final ObjectNode contextNode = mapper.createObjectNode();
			contextNode.set("analyzerContext", mapper.convertValue(context, JsonNode.class));
			final String contextNodeAsText = mapper.writeValueAsString(contextNode);
			traceWriter.write(contextNodeAsText);
			traceWriter.flush();
		} catch (IOException e) {
			throw new TraceException("Cannot output JSON for the Analysis", e);
		}
	}

	/**
	 * Record each sample - up to the number of samples we were asked to save (traceSampleCount).
	 * @param input The sample to record
	 * @param sampleCount The number of samples we have seen so far
	 */
	public void recordSample(final String input, final long sampleCount) {
		if (enabled && sampleCount < traceSampleCount) {
			try {
				if (batchCount == 0)
					traceWriter.write("\n{\n\"samples\": [\n");
				else
					traceWriter.write(",\n");
				batchCount++;
				if (input == null)
					traceWriter.write("null");
				else {
					traceWriter.write('"');
					traceWriter.write(jsonStringEncoder.quoteAsString(input));
					traceWriter.write('"');
				}
			} catch (IOException e) {
				throw new TraceException("Cannot write sample to trace file", e);
			}
		}
	}

	/**
	 * Record the bulk samples.
	 * @param input The bulk samples
	 */
	public void recordBulk(final Map<String, Long> input) {
		if (!enabled)
			return;

		try {
			traceWriter.write("\n{\n\"samplesBulk\": [\n");
			boolean first = true;
			for (final Map.Entry<String, Long> entry : input.entrySet())  {
				if (first)
					first = false;
				else
					traceWriter.write(",\n");

				traceWriter.write("{ \"value\": \"");
				traceWriter.write(jsonStringEncoder.quoteAsString(entry.getKey()));
				traceWriter.write("\", \"count\": " + entry.getValue() + " }");
			}
			traceWriter.write("\n]\n}\n");
		} catch (IOException e) {
			throw new TraceException("Cannot write bulk samples to trace file", e);
		}
	}

	/**
	 * Record the result of the analysis.
	 * @param result The TextAnalysisResult that captures the analysis.
	 * @param internalErrors The number of internal errors detected.
	 */
	public void recordResult(final TextAnalysisResult result, final int internalErrors) {
		// If tracing is enabled - output our conclusions
		if (enabled) {
			try {
				// Close out the Samples seen to date
				if (batchCount != 0) {
					traceWriter.write(" ]\n}\n");
					batchCount = 0;
				}

				// Output the results of our analysis
				traceWriter.write(result.asJSON(true, 1));

				// Report if there were any internal errors
				if (internalErrors != 0)
					traceWriter.write("\n{ \"internalErrors\": " + internalErrors + " }");

				traceWriter.flush();
			} catch (IOException e) {
				throw new TraceException("Cannot write analysis result to trace file", e);
			}
		}
	}
}
