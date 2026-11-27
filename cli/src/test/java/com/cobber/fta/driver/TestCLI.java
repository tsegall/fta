/*
 * Copyright 2017-2026 Tim Segall
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
package com.cobber.fta.driver;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.cobber.fta.TestGroups;

public class TestCLI {

	// Gradle runs subproject tests with the subproject dir as working dir; resolve to project root
	private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().getParent();
	private static final String CLI = PROJECT_ROOT.resolve("cli/build/install/fta/bin/cli").toString();

	private static Path emailsCsv;
	private static Path datesFrCsv;
	private static Path replayFta;

	@BeforeClass(groups = { TestGroups.ALL, TestGroups.CLI })
	public static void setup() throws Exception {
		emailsCsv = PROJECT_ROOT.resolve("cli/src/test/resources/emails.csv");
		datesFrCsv = PROJECT_ROOT.resolve("cli/src/test/resources/dates_fr.csv");
		replayFta = PROJECT_ROOT.resolve("cli/src/test/resources/emails_email.fta");

		assertTrue(emailsCsv.toFile().exists(), "emails.csv test resource missing");
		assertTrue(datesFrCsv.toFile().exists(), "dates_fr.csv test resource missing");
		assertTrue(replayFta.toFile().exists(), "emails_email.fta trace resource missing");
		assertTrue(new File(CLI).exists(), "CLI binary missing — run ./gradlew installDist first");
	}

	/** Run the CLI and return a Result holding stdout, stderr and exit code. */
	private static Result run(final String... args) throws IOException, InterruptedException {
		final ProcessBuilder pb = new ProcessBuilder();
		pb.command().add(CLI);
		for (final String arg : args)
			pb.command().add(arg);
		final Process p = pb.start();
		final String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		final String err = new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
		final int exit = p.waitFor();
		return new Result(out, err, exit);
	}

	private record Result(String out, String err, int exit) {}

	// -------------------------------------------------------------------------
	// Basic analysis
	// -------------------------------------------------------------------------

	@Test(groups = { TestGroups.ALL, TestGroups.CLI })
	public void testBasicCSVAnalysis() throws Exception {
		final Result r = run("--locale", "en-US", "--col", "0", "--noPretty", emailsCsv.toString());
		assertEquals(r.exit(), 0, "Expected exit 0, stderr: " + r.err());
		assertTrue(r.out().contains("\"semanticType\":\"EMAIL\""), "Expected EMAIL semantic type in output:\n" + r.out());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.CLI })
	public void testNoPrettyOutput() throws Exception {
		final Result r = run("--locale", "en-US", "--col", "0", "--noPretty", emailsCsv.toString());
		assertEquals(r.exit(), 0);
		// --noPretty should produce a single-line JSON object (no embedded newlines in the object)
		final List<String> lines = r.out().lines().filter(l -> l.startsWith("{")).toList();
		assertFalse(lines.isEmpty(), "Expected at least one JSON object line");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.CLI })
	public void testMultipleColumns() throws Exception {
		// No --col: all columns analysed; addresses.csv has a single 'address' column
		final Result r = run("--locale", "en-US", "--noPretty",
				PROJECT_ROOT.resolve("types/src/test/resources/addresses.csv").toString());
		assertEquals(r.exit(), 0);
		assertTrue(r.out().contains("\"fieldName\":\"address\""), "Expected 'address' field in output:\n" + r.out());
	}

	// -------------------------------------------------------------------------
	// Locale option
	// -------------------------------------------------------------------------

	@Test(groups = { TestGroups.ALL, TestGroups.CLI })
	public void testLocaleOption() throws Exception {
		// dates_fr.csv contains DD/MM/YYYY dates; with fr-FR locale DayFirst resolution kicks in
		final Result r = run("--locale", "fr-FR", "--col", "0", "--noPretty", datesFrCsv.toString());
		assertEquals(r.exit(), 0, "Expected exit 0, stderr: " + r.err());
		assertTrue(r.out().contains("\"type\":\"LocalDate\""), "Expected LocalDate type:\n" + r.out());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.CLI })
	public void testInvalidLocale() throws Exception {
		// A completely unknown tag is normalised by the JVM rather than rejected; verify we still get results
		final Result r = run("--locale", "zz-ZZ", "--col", "0", "--noPretty", emailsCsv.toString());
		assertEquals(r.exit(), 0, "Expected exit 0 for normalised locale, stderr: " + r.err());
		assertTrue(r.out().contains("\"semanticType\":\"EMAIL\""), "Expected EMAIL despite unknown locale:\n" + r.out());
	}

	// -------------------------------------------------------------------------
	// Plugin validation
	// -------------------------------------------------------------------------

	@Test(groups = { TestGroups.ALL, TestGroups.CLI })
	public void testValidatePlugin() throws Exception {
		final Result r = run("--locale", "en-US", "--col", "0",
				"--pluginMode", "true", "--pluginName", "EMAIL", "--validatePlugin",
				emailsCsv.toString());
		assertEquals(r.exit(), 0, "Expected exit 0, stderr: " + r.err());
		// --validatePlugin emits true/false per row
		assertTrue(r.out().contains("true"), "Expected 'true' validation rows in output:\n" + r.out());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.CLI })
	public void testValidatePluginMissingName() throws Exception {
		// --validatePlugin without --pluginName must fail at argument parsing
		final Result r = run("--validatePlugin", emailsCsv.toString());
		assertEquals(r.exit(), 1);
		assertTrue(r.err().contains("ERROR"), "Expected ERROR in stderr: " + r.err());
	}

	// -------------------------------------------------------------------------
	// Signature
	// -------------------------------------------------------------------------

	@Test(groups = { TestGroups.ALL, TestGroups.CLI })
	public void testSignature() throws Exception {
		final Result r = run("--locale", "en-US", "--pluginName", "EMAIL", "--signature");
		assertEquals(r.exit(), 0, "Expected exit 0, stderr: " + r.err());
		// Signature is a non-empty Base64-ish string on a single line
		final String sig = r.out().trim();
		assertFalse(sig.isEmpty(), "Expected non-empty signature");
		// Deterministic — running again must produce the same value
		final Result r2 = run("--locale", "en-US", "--pluginName", "EMAIL", "--signature");
		assertEquals(r2.out().trim(), sig, "Signature is not deterministic");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.CLI })
	public void testSignatureUnknownPlugin() throws Exception {
		final Result r = run("--pluginName", "NO_SUCH_PLUGIN", "--signature");
		assertEquals(r.exit(), 1);
		assertTrue(r.err().contains("ERROR"), "Expected ERROR in stderr: " + r.err());
	}

	// -------------------------------------------------------------------------
	// Merge
	// -------------------------------------------------------------------------

	@Test(groups = { TestGroups.ALL, TestGroups.CLI })
	public void testMerge() throws Exception {
		// --testMerge splits the file into chunks of n rows, analyses each, and merges
		final Result r = run("--locale", "en-US", "--col", "0", "--noPretty",
				"--testMerge", "3", emailsCsv.toString());
		assertEquals(r.exit(), 0, "Expected exit 0, stderr: " + r.err());
		assertTrue(r.out().contains("\"semanticType\":\"EMAIL\""), "Expected EMAIL after merge:\n" + r.out());
	}

	// -------------------------------------------------------------------------
	// Replay
	// -------------------------------------------------------------------------

	@Test(groups = { TestGroups.ALL, TestGroups.CLI })
	public void testReplay() throws Exception {
		final Result r = run("--replay", replayFta.toString());
		assertEquals(r.exit(), 0, "Expected exit 0, stderr: " + r.err());
		// Replay writes the result to stderr (see Replay.java)
		assertTrue(r.err().contains("EMAIL"), "Expected EMAIL in replayed result:\n" + r.err());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.CLI })
	public void testReplayMissingFile() throws Exception {
		final Result r = run("--replay", "nonexistent.fta");
		assertEquals(r.exit(), 1);
		assertTrue(r.err().contains("Not found"), "Expected 'Not found' in stderr: " + r.err());
	}

	// -------------------------------------------------------------------------
	// createSemanticTypesMarkdown
	// -------------------------------------------------------------------------

	@Test(groups = { TestGroups.ALL, TestGroups.CLI })
	public void testCreateSemanticTypesMarkdown() throws Exception {
		final Result r = run("--createSemanticTypesMarkdown");
		assertEquals(r.exit(), 0, "Expected exit 0, stderr: " + r.err());
		assertTrue(r.out().contains("EMAIL"), "Expected EMAIL entry in Markdown:\n" + r.out().substring(0, Math.min(500, r.out().length())));
		// Output is a Markdown table; check for the header row
		assertTrue(r.out().contains("Semantic Type"), "Expected 'Semantic Type' table header:\n" + r.out().substring(0, Math.min(200, r.out().length())));
	}

	// -------------------------------------------------------------------------
	// Version / help
	// -------------------------------------------------------------------------

	@Test(groups = { TestGroups.ALL, TestGroups.CLI })
	public void testVersion() throws Exception {
		final Result r = run("--version");
		assertEquals(r.exit(), 0);
		// Version string is written to stderr
		assertFalse(r.err().isBlank(), "Expected version string on stderr");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.CLI })
	public void testHelp() throws Exception {
		final Result r = run("--help");
		assertEquals(r.exit(), 0);
		assertTrue(r.err().contains("--locale"), "Expected --locale in help output");
		assertTrue(r.err().contains("--validatePlugin"), "Expected --validatePlugin in help output");
	}

	// -------------------------------------------------------------------------
	// Error cases
	// -------------------------------------------------------------------------

	@Test(groups = { TestGroups.ALL, TestGroups.CLI })
	public void testNoFileArgument() throws Exception {
		final Result r = run("--locale", "en-US");
		assertEquals(r.exit(), 1);
		assertTrue(r.err().contains("ERROR"), "Expected ERROR in stderr: " + r.err());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.CLI })
	public void testUnrecognizedOption() throws Exception {
		final Result r = run("--noSuchOption");
		assertEquals(r.exit(), 1);
		assertTrue(r.err().contains("ERROR"), "Expected ERROR in stderr: " + r.err());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.CLI })
	public void testMissingFile() throws Exception {
		final Result r = run("--locale", "en-US", "--col", "0", "nonexistent_file.csv");
		// FileProcessor catches the missing file — expect non-zero exit or error output
		assertTrue(r.exit() != 0 || r.err().contains("ERROR"),
				"Expected error for missing file. exit=" + r.exit() + " stderr=" + r.err());
	}

	// -------------------------------------------------------------------------
	// --output flag (exercises the PrintStream path in FileProcessor)
	// -------------------------------------------------------------------------

	@Test(groups = { TestGroups.ALL, TestGroups.CLI })
	public void testOutputFlag() throws Exception {
		// --output writes results to <inputfile>.out instead of stdout
		// Use a temp copy of the CSV so we don't litter the source tree
		final Path tmp = Files.createTempFile("fta_test_", ".csv");
		try {
			Files.copy(emailsCsv, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			final Path outFile = Path.of(tmp + ".out");
			try {
				final Result r = run("--locale", "en-US", "--col", "0", "--noPretty",
						"--output", tmp.toString());
				assertEquals(r.exit(), 0, "Expected exit 0, stderr: " + r.err());
				assertTrue(outFile.toFile().exists(), "Expected .out file to be created");
				final String contents = Files.readString(outFile);
				assertTrue(contents.contains("\"semanticType\":\"EMAIL\""), "Expected EMAIL in .out file:\n" + contents);
			} finally {
				outFile.toFile().delete();
			}
		} finally {
			tmp.toFile().delete();
		}
	}

	// -------------------------------------------------------------------------
	// noStatistics / noSemanticTypes flags
	// -------------------------------------------------------------------------

	@Test(groups = { TestGroups.ALL, TestGroups.CLI })
	public void testNoStatistics() throws Exception {
		final Result r = run("--locale", "en-US", "--col", "0", "--noPretty",
				"--noStatistics", emailsCsv.toString());
		assertEquals(r.exit(), 0, "Expected exit 0, stderr: " + r.err());
		assertFalse(r.out().contains("\"min\":"), "Expected no min stat with --noStatistics:\n" + r.out());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.CLI })
	public void testNoSemanticTypes() throws Exception {
		final Result r = run("--locale", "en-US", "--col", "0", "--noPretty",
				"--noSemanticTypes", emailsCsv.toString());
		assertEquals(r.exit(), 0, "Expected exit 0, stderr: " + r.err());
		assertFalse(r.out().contains("\"semanticType\":\"EMAIL\""),
				"Expected no semantic type with --noSemanticTypes:\n" + r.out());
	}
}
