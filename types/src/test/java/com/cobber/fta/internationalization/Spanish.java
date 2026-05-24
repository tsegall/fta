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
package com.cobber.fta.internationalization;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map.Entry;

import org.testng.annotations.Test;

import com.cobber.fta.AnalysisConfig;
import com.cobber.fta.AnalyzerContext;
import com.cobber.fta.LogicalType;
import com.cobber.fta.LogicalTypeFactory;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.RecordAnalyzer;
import com.cobber.fta.Sample;
import com.cobber.fta.TestGroups;
import com.cobber.fta.TestSupport;
import com.cobber.fta.TestUtils;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;
import com.cobber.fta.dates.DateTimeParser.DateResolutionMode;
import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

public class Spanish {

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void nameLast_DetectTrue() throws IOException, FTAException {
		final LogicalType validator = LogicalTypeFactory.newInstance(PluginDefinition.findByName("NAME.LAST"), new AnalysisConfig(Locale.forLanguageTag("es")));

		assertTrue(validator.isValid("TATE Jr.", true, 0));
		assertTrue(validator.isValid("TATE", true, 0));
		assertTrue(validator.isValid("TATE, Jr", true, 0));
		assertFalse(validator.isValid("TATE, BRIAN E", true, 0));
		assertTrue(validator.isValid("Davis Brown", true, 0));
		assertTrue(validator.isValid("Davis-Brown", true, 0));
		assertFalse(validator.isValid("Monday, Tuesday", true, 0));
		assertTrue(validator.isValid("REYES MOYANO", true, 0));
		assertTrue(validator.isValid("Thompson, Jr.", true, 0));
		assertFalse(validator.isValid("SAN ANDRES DE TUMACO", true, 0));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void genderES() throws IOException, FTAException {
		final String[] inputs = {
				"FEMENINO", "MASCULINO", "FEMENINO", "MASCULINO", "FEMENINO", "MASCULINO", "FEMENINO", "MASCULINO", "FEMENINO", "MASCULINO",
				"MASCULINO", "FEMENINO", "MASCULINO", "FEMENINO", "MASCULINO", "FEMENINO", "MASCULINO", "FEMENINO", "MASCULINO", "FEMENINO",
				"FEMENINO", "MASCULINO", "FEMENINO", "MASCULINO", "FEMENINO", "MASCULINO", "FEMENINO", "MASCULINO", "FEMENINO", "MASCULINO",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "Sexo", Locale.forLanguageTag("es-ES"), "GENDER.TEXT_<LANGUAGE>", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void issue159() throws IOException, FTAException {
		RecordAnalyzer analyzer = null;
		int rows = 0;

		try (BufferedReader in = new BufferedReader(new InputStreamReader(Spanish.class.getResourceAsStream("/lat.csv"), StandardCharsets.UTF_8))) {
			final CsvReader<NamedCsvRecord> csv = CsvReader.builder().ofNamedCsvRecord(in);

			for (final CloseableIterator<NamedCsvRecord> iter = csv.iterator(); iter.hasNext();) {
				final NamedCsvRecord rowRaw = iter.next();
				final String[] row = rowRaw.getFields().toArray(new String[0]);
				if (rows == 0) {
					final String[] header = rowRaw.getHeader().toArray(new String[0]);
					final AnalyzerContext context = new AnalyzerContext(null, DateResolutionMode.Auto, "profile", header);
					final TextAnalyzer template = new TextAnalyzer(context);
					template.setLocale(Locale.forLanguageTag("es-CO"));
					template.setDebug(2);
					analyzer = new RecordAnalyzer(template);
				}
				analyzer.train(row);
				rows++;
			}
		}

		final TextAnalysisResult coordinate = analyzer.getResult().getStreamResults()[0];
		assertEquals(coordinate.getSampleCount(), rows);
		assertEquals(coordinate.getType(), FTAType.STRING);
		assertEquals(coordinate.getSemanticType(), "COORDINATE.LONGITUDE_DECIMAL");
		assertNull(coordinate.checkCounts(false));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DOUBLES })
	public void localeDoubleES_CO() throws IOException, FTAException {
		final String[] ugly = {
				"77.506.942,294", "55.466.183,606", "78.184.714,556", "52.225.004,254",
				"49.728.440,901", "46.654.635,41", "44.855.131,454", "74.523.230,406",
				"49.266.524,337", "21.683.364,918", "50.170.727,311", "45.015.038,753",
				"77.374.136,348", "14.954.505,431", "67.357.001,775", "81.430.862,119",
				"56.012.358,58", "49.427.706,653", "18.983.565,706", "76.804.973,122",
				"82.300.559,154", "40.007.535,851", "48.120.618,984", "25.215.331,00",
				"68.970.889,115", "98.530.458,063", "52.423.892,78", "51.938.286,39"
		};
		final Locale locale = Locale.forLanguageTag("es-CO");

		final TextAnalyzer analysis = new TextAnalyzer("Separator");
		analysis.setLocale(locale);

		for (final String sample : ugly)
			analysis.train(sample);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.DOUBLE, locale.toLanguageTag());
		assertEquals(result.getTypeModifier(), "GROUPING");
		assertNull(result.getSemanticType());
		assertEquals(result.getSampleCount(), ugly.length);
		assertEquals(result.getMatchCount(), ugly.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getLeadingZeroCount(), 0);
		final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);

		final String grp = formatSymbols.getGroupingSeparator() == '.' ? "\\." : "" + formatSymbols.getGroupingSeparator();
		final String dec = formatSymbols.getDecimalSeparator() == '.' ? "\\." : "" + formatSymbols.getDecimalSeparator();

		final String regExp = "[\\d" + grp + "]*" + dec + "?" + "[\\d" + grp +"]+";

		assertEquals(result.getRegExp(), regExp);
		assertEquals(result.getConfidence(), 1.0);

		for (final String sample : ugly) {
			assertTrue(sample.matches(regExp), sample + " " + regExp);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATES })
	public void twoUnboundThenOne() throws IOException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("fecha_de_vinculaci_n");
		analysis.setLocale(Locale.forLanguageTag("es-CO"));
		analysis.setDebug(2);

		final String inputs[] = {
				"25/11/16", "27/10/14", "13/12/16", "04/07/19", "02/01/17", "21/07/14",
				"02/11/13", "07/06/17", "01/01/94", "20/06/18", "01/01/20", "20/01/16",
				"10/05/13", "24/08/16", "25/01/18", "22/07/16", "28/02/12", "01/01/20",
				"27/01/16", "01/01/20", "24/06/15", "01/03/16", "01/10/20", "04/01/13",
				"11/12/95", "20/10/20", "22/12/15", "02/01/18", "01/01/85", "19/06/19",
		};
		int locked = -1;

		for (int i = 0; i < inputs.length; i++) {
			if (analysis.train(inputs[i]) && locked == -1)
				locked = i;
		}

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "dd/MM/yy");
		assertEquals(result.getRegExp(), "\\d{2}/\\d{2}/\\d{2}");
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			if (!input.isEmpty())
				assertTrue(input.matches(result.getRegExp()), input);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void VAT_ES() throws IOException, FTAException {
		TextAnalyzer[] analysis = null;
		int rows = 0;

		try (BufferedReader in = new BufferedReader(new InputStreamReader(Spanish.class.getResourceAsStream("/VAT_ES.csv"), StandardCharsets.UTF_8))) {
			final CsvReader<NamedCsvRecord> csv = CsvReader.builder().ofNamedCsvRecord(in);

			for (final CloseableIterator<NamedCsvRecord> iter = csv.iterator(); iter.hasNext();) {
				final NamedCsvRecord rowRaw = csv.iterator().next();
				final String[] row = rowRaw.getFields().toArray(new String[0]);
				if (rows == 0) {
					final String[] header = rowRaw.getHeader().toArray(new String[0]);
					analysis = new TextAnalyzer[header.length];
					for (int i = 0; i < header.length; i++) {
						analysis[i] = new TextAnalyzer(new AnalyzerContext(header[i], DateResolutionMode.Auto, "VAT_ES.csv", header));
						analysis[i].setLocale(Locale.forLanguageTag("es-ES"));
					}
				}
				rows++;
				for (int i = 0; i < row.length; i++) {
					analysis[i].train(row[i]);
				}
			}
		}

		final TextAnalysisResult result = analysis[0].getResult();
		assertEquals(result.getSemanticType(), "IDENTITY.VAT_ES");
		assertEquals(result.getBlankCount(), 0);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getSampleCount(), 1194);
		assertEquals(result.getMatchCount(), 1193);
		assertEquals(result.getType(), FTAType.STRING);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getInvalidCount(), 1);
		final Entry<String, Long> only = result.getInvalidDetails().entrySet().iterator().next();
		assertEquals(only.getKey(), "X02469358");
		assertEquals(only.getValue(), 1);

		assertNull(result.checkCounts(false));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void testNamesSecond() throws IOException, FTAException {
		TextAnalyzer[] analysis = null;
		int rows = 0;

		try (BufferedReader in = new BufferedReader(new InputStreamReader(Spanish.class.getResourceAsStream("/NamesSecond.txt"), StandardCharsets.UTF_8))) {

			final CsvReader<NamedCsvRecord> csv = CsvReader.builder().ofNamedCsvRecord(in);

			for (final CloseableIterator<NamedCsvRecord> iter = csv.iterator(); iter.hasNext();) {
				final NamedCsvRecord rowRaw = csv.iterator().next();
				final String[] row = rowRaw.getFields().toArray(new String[0]);
				if (rows == 0) {
					final String[] header = rowRaw.getHeader().toArray(new String[0]);
					analysis = new TextAnalyzer[header.length];
					for (int i = 0; i < header.length; i++) {
						analysis[i] = new TextAnalyzer(new AnalyzerContext(header[i], DateResolutionMode.Auto, "NamesSecond.txt", header));
						analysis[i].setLocale(Locale.forLanguageTag("es-CO"));
					}
				}
				rows++;
				for (int i = 0; i < row.length; i++) {
					analysis[i].train(row[i]);
				}
			}
		}

		// File Header is '"cons","nombre1","nombre2","apellido1","apellido2"'

		final TextAnalysisResult first = analysis[1].getResult();
		assertEquals(first.getSemanticType(), "NAME.FIRST");
		assertEquals(first.getStructureSignature(), PluginDefinition.findByName("NAME.FIRST").signature);

//		final String BUG = "NAME.MIDDLE";
		final String BUG = "NAME.FIRST";
		final TextAnalysisResult first_2 = analysis[2].getResult();
		assertEquals(first_2.getSemanticType(), BUG);
		assertEquals(first_2.getStructureSignature(), PluginDefinition.findByName(BUG).signature);

		final TextAnalysisResult last = analysis[3].getResult();
		assertEquals(last.getSemanticType(), "NAME.LAST");
		assertEquals(last.getStructureSignature(), PluginDefinition.findByName("NAME.LAST").signature);

		final TextAnalysisResult last_2 = analysis[3].getResult();
		assertEquals(last_2.getSemanticType(), "NAME.LAST");
		assertEquals(last_2.getStructureSignature(), PluginDefinition.findByName("NAME.LAST").signature);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void weirdLongitude_esCO() throws IOException, FTAPluginException {
		final PluginDefinition pluginDefinition = PluginDefinition.findByName("COORDINATE.LONGITUDE_DECIMAL");
		final LogicalType logical = LogicalTypeFactory.newInstance(pluginDefinition, new AnalysisConfig(Locale.forLanguageTag("es-CO")));

		final String[] validSamples = { "12.43", "13.49", "180.0", "90.0", "-69.4", "-90.0", "-170.0",  };

		for (final String sample : validSamples)
			assertTrue(logical.isValid(sample), sample);

		final String[] invalidSamples = { "181.0", "-190.2" };

		for (final String sample : invalidSamples)
			assertFalse(logical.isValid(sample), sample);

		assertFalse(logical.isValid("-73.7744434,13"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void colombianDepartmentDetection() throws IOException, FTAPluginException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("nombredpto");
		analysis.setLocale(Locale.forLanguageTag("es-CO"));

		// Entries are valid Colombian departments — header "nombredpto" contains "nombre" which is a weak
		// NAME.LAST header hint, so this test guards against NAME.LAST incorrectly winning.
		final String[] inputs = {
			"Antioquia", "Valle", "Santander", "Bogotá", "Huila",
			"Nariño", "Cundinamarca", "Boyacá", "Tolima", "Caldas",
			"Risaralda", "Córdoba", "Bolívar", "Atlántico", "Meta",
			"Cesar", "Cauca", "Chocó", "Magdalena", "Sucre"
		};
		for (final String s : inputs)
			analysis.train(s);

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getSemanticType(), "STATE_PROVINCE.DEPARTMENT_CO");
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void colombianDepartmentDetectionTruncatedHeader() throws IOException, FTAPluginException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("departame_nombre");
		analysis.setLocale(Locale.forLanguageTag("es-CO"));

		// Header "departame_nombre" uses a truncated "departamento" — guards against NAME.FIRST winning via the
		// weak "nombre" hint in the es locale.
		final String[] inputs = {
			"ANTIOQUIA", "ARAUCA", "ATLANTICO", "BOGOTA", "BOLIVAR",
			"BOYACA", "CALDAS", "CAQUETA", "CASANARE", "CAUCA",
			"CESAR", "CHOCO", "CORDOBA", "CUNDINAMARCA", "GUAVIARE",
			"HUILA", "MAGDALENA", "META", "PUTUMAYO", "SANTANDER"
		};
		for (final String s : inputs)
			analysis.train(s);

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getSemanticType(), "STATE_PROVINCE.DEPARTMENT_CO");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void colombianMunicipalityDetection() throws IOException, FTAPluginException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("nombre_centro_poblado");
		analysis.setLocale(Locale.forLanguageTag("es-CO"));

		final String[] inputs = {
			"ABRIAQUÍ", "ALEJANDRÍA", "AMAGÁ", "AMALFI", "ANDES",
			"ANGELÓPOLIS", "ANGOSTURA", "ANORÍ", "ANZA", "APARTADÓ",
			"ARBOLETES", "ARGELIA", "ARMENIA", "BARBOSA", "BELLO",
			"BETANIA", "BETULIA", "BRICEÑO", "BURITICÁ", "CÁCERES"
		};
		for (final String s : inputs)
			analysis.train(s);

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getSemanticType(), "STATE_PROVINCE.MUNICIPALITY_CO");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void postalCodeCO_validInvalid() throws IOException, FTAPluginException {
		final LogicalType logical = LogicalTypeFactory.newInstance(PluginDefinition.findByName("POSTAL_CODE.POSTAL_CODE_CO"), new AnalysisConfig(Locale.forLanguageTag("es-CO")));

		// 6-digit codes stored directly in reference data
		final String[] valid6 = { "050001", "050002", "050003", "110111", "110121" };
		for (final String v : valid6)
			assertTrue(logical.isValid(v), v);

		// 5-digit inputs that resolve via "0" + input lookup
		final String[] valid5 = { "50001", "50002", "50003" };
		for (final String v : valid5)
			assertTrue(logical.isValid(v), v);

		final String[] invalid = { "1234", "1234567", "ABCDEF", "" };
		for (final String iv : invalid)
			assertFalse(logical.isValid(iv), iv);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void postalCodeCO_endToEnd() throws IOException, FTAPluginException, FTAException {
		final String[] inputs = {
			"050001", "050002", "050003", "050004", "050005",
			"050006", "050007", "050010", "050011", "050012",
			"050013", "050014", "050015", "110111", "110121",
			"680001", "760001", "760002", "080001", "080002"
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "codigo_postal", Locale.forLanguageTag("es-CO"), "POSTAL_CODE.POSTAL_CODE_CO", FTAType.LONG, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void postalCodeCO_lowCardinalityNoHeader() throws IOException, FTAPluginException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("field1");
		analysis.setLocale(Locale.forLanguageTag("es-CO"));

		// Only 3 distinct values and no postal header — should back out
		final String[] inputs = { "050001", "050002", "050003" };
		for (final String s : inputs)
			for (int i = 0; i < 5; i++)
				analysis.train(s);

		final TextAnalysisResult result = analysis.getResult();
		assertFalse("POSTAL_CODE.POSTAL_CODE_CO".equals(result.getSemanticType()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void esColorDetection() throws IOException, FTAException {
		final String[] inputs = {
			"ROJO", "AZUL", "VERDE", "BLANCO", "NEGRO", "NARANJA", "ROSA", "GRIS",
			"MARRÓN", "VIOLETA", "AMARILLO", "BEIGE", "CREMA", "AZUL MARINO", "BORGOÑA",
			"TURQUESA", "ÍNDIGO", "PLATA", "ORO", "BRONCE", "ROJO", "AZUL", "VERDE"
		};
		for (final String header : new String[] { "color", "Color", "color_producto" }) {
			final TextAnalyzer analysis = new TextAnalyzer(header);
			analysis.setLocale(Locale.forLanguageTag("es-ES"));
			for (final String s : inputs)
				analysis.train(s);
			assertEquals(analysis.getResult().getSemanticType(), "COLOR.TEXT_ES", "header: " + header);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void esColorNoHeader() throws IOException, FTAException {
		final String[] inputs = {
			"ROJO", "AZUL", "VERDE", "BLANCO", "NEGRO", "NARANJA", "ROSA", "GRIS",
			"MARRÓN", "VIOLETA", "AMARILLO", "BEIGE", "CREMA", "AZUL MARINO", "BORGOÑA"
		};
		final TextAnalyzer analysis = new TextAnalyzer("col");
		analysis.setLocale(Locale.forLanguageTag("es-ES"));
		for (final String s : inputs)
			analysis.train(s);
		assertNotEquals(analysis.getResult().getSemanticType(), "COLOR.TEXT_ES");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void continentES() throws IOException, FTAException {
		final String[] inputs = {
				"ÁFRICA", "ASIA", "EUROPA", "AMERICA DEL NORTE", "AMERICA DEL SUR", "OCEANÍA", "ANTÁRTIDA",
				"EUROPA", "ASIA", "ÁFRICA", "AMERICA DEL NORTE", "ASIA", "EUROPA", "ÁFRICA", "OCEANÍA",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "continente", Locale.forLanguageTag("es-ES"), "CONTINENT.TEXT_ES", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void languageES() throws IOException, FTAException {
		final String[] inputs = {
				"ESPAÑOL", "INGLÉS", "FRANCÉS", "ALEMÁN", "ITALIANO", "PORTUGUÉS", "RUSO", "CHINO",
				"JAPONÉS", "ÁRABE", "HINDI", "COREANO", "TURCO", "POLACO", "SUECO",
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "idioma", Locale.forLanguageTag("es-ES"), "LANGUAGE.TEXT_ES", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}
}
