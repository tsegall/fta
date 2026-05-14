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
package com.cobber.fta;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;

public class TestStandalonePlugins {
	private final Logger logger = LoggerFactory.getLogger("com.cobber.fta");

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void randomIPV4Address() throws IOException, FTAPluginException {
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(PluginDefinition.findByName("IPADDRESS.IPV4"), new AnalysisConfig());

		assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void randomIPV6Address() throws IOException, FTAPluginException {
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(PluginDefinition.findByName("IPADDRESS.IPV6"), new AnalysisConfig());

		assertTrue(logical.isValid("::"), "::");
		assertTrue(logical.isValid("::1"), "::1");
		assertTrue("::".matches(logical.getRegExp()), "::");
		assertTrue("::1".matches(logical.getRegExp()), "::1");
		for (int i = 0; i < 100; i++) {
			final String sample = logical.nextRandom();
			assertTrue(sample.matches(logical.getRegExp()), sample);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void randomPhoneNumber() throws IOException, FTAPluginException {
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(PluginDefinition.findByName("TELEPHONE"), new AnalysisConfig(Locale.forLanguageTag("en-US")));
		final String initialCheck = logical.nextRandom();

		assertNotNull(initialCheck);
		assertTrue(initialCheck.matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++) {
			final String sample = logical.nextRandom();
			assertTrue(sample.matches(logical.getRegExp()));
			assertTrue(logical.isValid(sample), sample);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void randomEmail() throws IOException, FTAPluginException {
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(PluginDefinition.findByName("EMAIL"), new AnalysisConfig());

		assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void randomFirst() throws IOException, FTAPluginException {
		final LogicalTypeCode logical = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByName("NAME.FIRST"), new AnalysisConfig());

		assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void randomLast() throws IOException, FTAPluginException {
		final LogicalTypeCode logical = (LogicalTypeFiniteSimple) LogicalTypeFactory.newInstance(PluginDefinition.findByName("NAME.LAST"), new AnalysisConfig());

		assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++) {
			final String example = logical.nextRandom();
			assertTrue(example.matches(logical.getRegExp()));
			assertTrue(logical.isValid(example));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void randomURL() throws IOException, FTAPluginException {
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(PluginDefinition.findByName("URI.URL"), new AnalysisConfig());

		assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void randomZip() throws IOException, FTAPluginException {
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(PluginDefinition.findByName("POSTAL_CODE.ZIP5_US"), new AnalysisConfig(Locale.forLanguageTag("en-US")));

		assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void randomGUID() throws IOException, FTAPluginException {
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(PluginDefinition.findByName("GUID"), new AnalysisConfig());

		assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void randomGender() throws IOException, FTAPluginException {
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(PluginDefinition.findByName("GENDER.TEXT_<LANGUAGE>"), new AnalysisConfig(Locale.forLanguageTag("nl-NL")));

		assertTrue(logical.nextRandom().matches(logical.getRegExp()));

		for (int i = 0; i < 100; i++)
			assertTrue(logical.nextRandom().matches(logical.getRegExp()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void randomCountry() throws IOException, FTAPluginException {
		final LogicalTypeCode logical = (LogicalTypeCode) LogicalTypeFactory.newInstance(PluginDefinition.findByName("COUNTRY.TEXT_EN"), new AnalysisConfig());

		for (int i = 0; i < 100; i++) {
			final String example = logical.nextRandom();
			assertTrue(example.matches(logical.getRegExp()), example + logical.getRegExp());
			assertTrue(logical.isValid(example.toLowerCase(Locale.ENGLISH)), example);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void randomSupport() throws IOException, FTAPluginException {
		final int SAMPLE_SIZE = 100;
		final Locale[] locales = {
				Locale.forLanguageTag("bg-BG"),
				Locale.forLanguageTag("da-DK"),
				Locale.forLanguageTag("de-CH"), Locale.forLanguageTag("de-DE"),
//				Locale.forLanguageTag("el-GR"),
				Locale.forLanguageTag("en-AU"), Locale.forLanguageTag("en-CA"), Locale.forLanguageTag("en-GB"), Locale.forLanguageTag("en-IE"), Locale.forLanguageTag("en-US"),
				Locale.forLanguageTag("es-CO"), Locale.forLanguageTag("es-ES"), Locale.forLanguageTag("es-MX"), Locale.forLanguageTag("es-PE"), Locale.forLanguageTag("es-UY"),
// Finnish has a strange minus sign - Unicode Character 'MINUS SIGN' (U+2212)

//				Locale.forLanguageTag("fi-FI"),
				Locale.forLanguageTag("fr-CA"), Locale.forLanguageTag("fr-CH"), Locale.forLanguageTag("fr-FR"),
				Locale.forLanguageTag("ga-IE"),
				Locale.forLanguageTag("hr-HR"),
				Locale.forLanguageTag("hu-HU"),
				Locale.forLanguageTag("is-IS"),
				Locale.forLanguageTag("it-CH"), Locale.forLanguageTag("it-IT"),
				Locale.forLanguageTag("jp-JP"),
				Locale.forLanguageTag("lv-LV"),
				Locale.forLanguageTag("nl-NL"),
				Locale.forLanguageTag("pt-BR"), Locale.forLanguageTag("pt-PT"),
				Locale.forLanguageTag("ro-RO"),
				Locale.forLanguageTag("ru-RU"),
				Locale.forLanguageTag("sk-SK"),
				Locale.forLanguageTag("sv-SE"),
				Locale.forLanguageTag("tr-TR")
		};

		for (final Locale locale : locales) {
			// Create an Analyzer to retrieve the Semantic Types (magically will be all - since passed in '*')
			final TextAnalyzer analyzer = new TextAnalyzer("*");
			analyzer.setLocale(locale);
			// Load the default set of plugins for Semantic Type detection (normally done by a call to train())
			try {
				analyzer.registerDefaultPlugins(analyzer.getConfig());
			}
			catch (IllegalArgumentException e) {
				logger.error("ERROR: Failed to register plugins for locale: {}, error: {}", locale.toLanguageTag(), e.getMessage());
				fail();
			}
			final Collection<LogicalType> registered = analyzer.getPlugins().getRegisteredSemanticTypes();

			for (int iters = 0; iters < 10; iters++) {
				for (final LogicalType logical : registered) {

					final PluginDefinition definition = logical.getPluginDefinition();
					// TODO
					if ("STATE_PROVINCE.COMMUNE_IT".equals(definition.semanticType))
						continue;

					final String pluginSignature = definition.signature;
					if (!"[NONE]".equals(pluginSignature) && !logical.getSignature().equals(logical.getPluginDefinition().signature))
						logger.warn("WARNING: Signature incorrect for '{}'.  LogicalType = '{}', Plugin = '{}'.",
								logical.getSemanticType(), logical.getSignature(), logical.getPluginDefinition().signature);
					assertTrue("[NONE]".equals(pluginSignature) || logical.getSignature().equals(logical.getPluginDefinition().signature));

					if (logical instanceof LogicalTypeRegExp && !((LogicalTypeRegExp)logical).isRegExpComplete())
						continue;

					if (logical.nextRandom() == null) {
						System.err.println("No nextRandom() support for Semantic Type: " + logical.getSemanticType());
						continue;
					}

					final String[] testCases = new String[SAMPLE_SIZE];
					for (int i = 0; i < SAMPLE_SIZE; i++) {
						testCases[i] = logical.nextRandom();
						if (testCases[i] != null && !testCases[i].isEmpty()) {
							boolean ret = false;
							try {
								ret = logical.isValid(testCases[i]);
							}
							catch (NumberFormatException e) {
								// Do nothing
							}
							assertTrue(ret, logical.getSemanticType() + "(" + locale.toLanguageTag() + "):'" +  testCases[i] + "'");
						}
					}
					for (int i = 0; i < SAMPLE_SIZE; i++)
						if (testCases[i] != null && !testCases[i].isEmpty())
							assertTrue(testCases[i].matches(logical.getRegExp()), logical.getSemanticType() + ": '" + testCases[i] + "', RE: " + logical.getRegExp());
				}
			}
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void randomCOORDINATE_LATITUDE_DECIMAL() throws IOException, FTAPluginException {
		final PluginDefinition pluginDefinition = PluginDefinition.findByName("COORDINATE.LATITUDE_DECIMAL");
		final LogicalType logical = LogicalTypeFactory.newInstance(pluginDefinition, new AnalysisConfig());

		final String[] validSamples = { "12.43", "13.49", "90.0", "-69.4", "-90.0" };

		for (final String sample : validSamples)
			assertTrue(logical.isValid(sample), sample);

		final String[] invalidSamples = { "91.0", "-90.2" };

		for (final String sample : invalidSamples)
			assertFalse(logical.isValid(sample), sample);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void randomCOORDINATE_LONGITUDE_DECIMAL() throws IOException, FTAPluginException {
		final PluginDefinition pluginDefinition = PluginDefinition.findByName("COORDINATE.LONGITUDE_DECIMAL");
		final LogicalType logical = LogicalTypeFactory.newInstance(pluginDefinition, new AnalysisConfig());

		final String[] validSamples = { "12.43", "13.49", "180.0", "90.0", "-69.4", "-90.0", "-170.0",  };

		for (final String sample : validSamples)
			assertTrue(logical.isValid(sample), sample);

		final String[] invalidSamples = { "181.0", "-190.2" };

		for (final String sample : invalidSamples)
			assertFalse(logical.isValid(sample), sample);
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
	public void randomCITY() throws IOException, FTAPluginException {
		final PluginDefinition pluginDefinition = PluginDefinition.findByName("CITY");
		final LogicalType logical = LogicalTypeFactory.newInstance(pluginDefinition, new AnalysisConfig());

		final String[] validSamples = {
				"Milton Keynes", "Tokyo", "Delhi", "Shanghai", "Sao Paulo", "Mexico City", "Cairo", "Dhaka",
				"Mumbai", "Beijing","Osaka", "Karachi", "Chongqing", "Buenos Aires", "Istanbul", "Kolkata",
				"Lagos", "Manila", "Tianjin","Rio De Janeiro", "Malmö", "St. Louis", "Saint-Georges",
				"MARTHA'S VINEYARD", "CLARK'S MOUNTAIN", "Fort McMurray", "Montréal"
		};

		for (final String sample : validSamples)
			assertTrue(logical.isValid(sample), sample);

		final String[] invalidSamples = { "2001Olypics" };

		for (final String sample : invalidSamples)
			assertFalse(logical.isValid(sample), sample);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaFirstNameDetection() throws IOException, FTAPluginException, com.cobber.fta.core.FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("名前");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));

		final String[] inputs = {
			"太郎", "花子", "健一", "裕子", "直樹",
			"美咲", "拓也", "さくら", "翔", "陽子",
			"誠", "智子", "浩二", "恵子", "大輔",
			"由美子", "達也", "香織", "慎一", "友美"
		};
		for (final String s : inputs)
			analysis.train(s);

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getSemanticType(), "NAME.FIRST");
		assertTrue(result.getConfidence() >= 0.85);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaLastNameDetection() throws IOException, FTAPluginException, com.cobber.fta.core.FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("姓");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));

		final String[] inputs = {
			"田中", "佐藤", "鈴木", "高橋", "渡辺",
			"伊藤", "山本", "中村", "小林", "加藤",
			"吉田", "山田", "佐々木", "山口", "松本",
			"井上", "木村", "林", "斎藤", "清水"
		};
		for (final String s : inputs)
			analysis.train(s);

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getSemanticType(), "NAME.LAST");
		assertTrue(result.getConfidence() >= 0.85);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaLastFirstDetectionSpaceSeparated() throws IOException, FTAPluginException, com.cobber.fta.core.FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("氏名");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));

		// Common Japanese full names in Last First order, space-separated
		final String[] inputs = {
			"田中 太郎", "佐藤 花子", "鈴木 健一", "高橋 裕子", "渡辺 直樹",
			"伊藤 美咲", "山本 拓也", "中村 さくら", "小林 翔", "加藤 陽子",
			"吉田 誠", "山田 智子", "佐々木 浩二", "山口 恵子", "松本 大輔",
			"井上 由美子", "木村 達也", "林 香織", "斎藤 慎一", "清水 友美"
		};
		for (final String s : inputs)
			analysis.train(s);

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getSemanticType(), "NAME.LAST_FIRST");
		assertTrue(result.getConfidence() >= 0.85);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaLastFirstDetectionConcatenated() throws IOException, FTAPluginException, com.cobber.fta.core.FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("氏名");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));

		// Common Japanese full names in Last First order, no separator
		final String[] inputs = {
			"田中太郎", "佐藤花子", "鈴木健一", "高橋裕子", "渡辺直樹",
			"伊藤美咲", "山本拓也", "中村翔", "小林陽子", "加藤誠",
			"吉田智子", "山田浩二", "山口恵子", "松本大輔", "井上由美子",
			"木村達也", "林香織", "斎藤慎一", "清水友美", "小川一郎"
		};
		for (final String s : inputs)
			analysis.train(s);

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getSemanticType(), "NAME.LAST_FIRST");
		assertTrue(result.getConfidence() >= 0.85);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void colombianDepartmentDetection() throws IOException, FTAPluginException, com.cobber.fta.core.FTAException {
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
	public void colombianDepartmentDetectionTruncatedHeader() throws IOException, FTAPluginException, com.cobber.fta.core.FTAException {
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
	public void colombianMunicipalityDetection() throws IOException, FTAPluginException, com.cobber.fta.core.FTAException {
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
	public void jaStreetAddressWithPostalCode() throws IOException, FTAPluginException, com.cobber.fta.core.FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("住所");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));

		// Full addresses with 〒 postal-code prefix, from japanese/sa.csv
		final String[] inputs = {
			"〒100-0001 東京都千代田区千代田1-1",
			"〒160-0023 東京都新宿区西新宿2-8-1",
			"〒530-0001 大阪府大阪市北区梅田1-2-2-100",
			"〒460-0001 愛知県名古屋市中区三の丸3-1-2",
			"〒060-0001 北海道札幌市中央区北1条西5-2",
			"〒220-0012 神奈川県横浜市西区みなとみらい2-3-3",
			"〒812-0011 福岡県福岡市博多区博多駅前1-1-1",
			"〒600-8411 京都府京都市下京区烏丸通四条下ル水銀屋町620",
			"〒980-0021 宮城県仙台市青葉区中央1-1-1",
			"〒730-0011 広島県広島市中区基町10-52"
		};
		for (final String s : inputs)
			analysis.train(s);

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getSemanticType(), "STREET_ADDRESS_JA");
		assertTrue(result.getConfidence() >= 0.95);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaStreetAddressWithoutPostalCode() throws IOException, FTAPluginException, com.cobber.fta.core.FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("所在地");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));

		// Addresses without postal-code prefix
		final String[] inputs = {
			"東京都千代田区千代田1-1",
			"東京都新宿区西新宿2-8-1",
			"大阪府大阪市北区梅田1-2-2-100",
			"愛知県名古屋市中区三の丸3-1-2",
			"北海道札幌市中央区北1条西5-2",
			"神奈川県横浜市西区みなとみらい2-3-3",
			"福岡県福岡市博多区博多駅前1-1-1",
			"京都府京都市下京区烏丸通四条下ル水銀屋町620",
			"宮城県仙台市青葉区中央1-1-1",
			"広島県広島市中区基町10-52"
		};
		for (final String s : inputs)
			analysis.train(s);

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getSemanticType(), "STREET_ADDRESS_JA");
		assertTrue(result.getConfidence() >= 0.95);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void streetNameBareNL() throws IOException, FTAPluginException, com.cobber.fta.core.FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("straat");
		analysis.setLocale(Locale.forLanguageTag("nl"));

		final String[] inputs = {
			"Kalverstraat", "Damrak", "Leidsestraat", "Herengracht", "Keizersgracht",
			"Prinsengracht", "Nieuwezijds Voorburgwal", "Rokin", "Spui", "Reguliersbreestraat",
			"Amstelstraat", "Utrechtsestraat", "Vijzelstraat", "Overtoom", "Jan Luijkenstraat"
		};
		for (final String s : inputs)
			analysis.train(s);

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getSemanticType(), "STREET_NAME_BARE_NL");
		assertTrue(result.getConfidence() >= 0.95);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void streetNameBareNL_numericWordRejected() throws IOException, FTAPluginException {
		final LogicalType logical = LogicalTypeFactory.newInstance(PluginDefinition.findByName("STREET_NAME_BARE_<LANGUAGE>"), new AnalysisConfig(Locale.forLanguageTag("nl")));

		assertTrue(logical.isValid("Kalverstraat"));
		// A value that is purely numeric should be rejected
		assertFalse(logical.isValid("42"));
		// Empty input should be rejected
		assertFalse(logical.isValid(""));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void logicalTypeRegExp_matchEntryAlreadySet() throws IOException, FTAPluginException {
		final PluginDefinition defn = PluginDefinition.findByName("MACADDRESS");
		final LogicalTypeRegExp logical = (LogicalTypeRegExp) LogicalTypeFactory.newInstance(defn, new AnalysisConfig());

		// Force matchEntry to be set by calling isMatch() first
		assertTrue(logical.isMatch(logical.getRegExp()));

		// Now matchEntry is set — isRegExpComplete() and getRegExp() take the matchEntry branch
		assertTrue(logical.isRegExpComplete());
		assertNotNull(logical.getRegExp());

		// isMatch() with matchEntry already set checks only that entry
		assertTrue(logical.isMatch(logical.getRegExp()));
		assertFalse(logical.isMatch("nomatch"));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void postalCodeFR_validInvalid() throws IOException, FTAPluginException {
		final LogicalType logical = LogicalTypeFactory.newInstance(PluginDefinition.findByName("POSTAL_CODE.POSTAL_CODE_FR"), new AnalysisConfig(Locale.forLanguageTag("fr-FR")));

		final String[] valid = { "01000", "01001", "01002", "75001", "75008", "13001", "69001", "33000", "06000", "59000" };
		for (final String v : valid)
			assertTrue(logical.isValid(v), v);

		final String[] invalid = { "1000", "123456", "ABCDE", "7500A", "" };
		for (final String iv : invalid)
			assertFalse(logical.isValid(iv), iv);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void postalCodeFR_endToEnd() throws IOException, FTAPluginException, com.cobber.fta.core.FTAException {
		final String[] inputs = {
			"75001", "75002", "75003", "75004", "75005",
			"13001", "13002", "13003", "69001", "69002",
			"33000", "06000", "59000", "31000", "67000",
			"76000", "44000", "35000", "57000", "29200"
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "code_postal", Locale.forLanguageTag("fr-FR"), "POSTAL_CODE.POSTAL_CODE_FR", FTAType.LONG, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void postalCodeFR_lowCardinalityNoHeader() throws IOException, FTAPluginException, com.cobber.fta.core.FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("field1");
		analysis.setLocale(Locale.forLanguageTag("fr-FR"));

		// Only 3 distinct values — below the cardinality threshold — and no postal header, so should back out
		final String[] inputs = { "75001", "75002", "75003" };
		for (final String s : inputs)
			for (int i = 0; i < 5; i++)
				analysis.train(s);

		final TextAnalysisResult result = analysis.getResult();
		assertFalse("POSTAL_CODE.POSTAL_CODE_FR".equals(result.getSemanticType()));
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
	public void postalCodeCO_endToEnd() throws IOException, FTAPluginException, com.cobber.fta.core.FTAException {
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
	public void postalCodeCO_lowCardinalityNoHeader() throws IOException, FTAPluginException, com.cobber.fta.core.FTAException {
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
	public void postalCodeUK_validInvalid() throws IOException, FTAPluginException {
		final LogicalType logical = LogicalTypeFactory.newInstance(PluginDefinition.findByName("POSTAL_CODE.POSTAL_CODE_UK"), new AnalysisConfig(Locale.forLanguageTag("en-GB")));

		final String[] valid = { "EC1A 1BB", "W1A 0AX", "M1 1AE", "B1 1BB", "CR2 6XH", "DN55 1PT", "GIR 0AA" };
		for (final String v : valid)
			assertTrue(logical.isValid(v), v);

		// Inputs failing isCandidate non-alpha prefix check
		final String[] invalid = { "123 4AB", "1W1 0AX", "ABCDEF", "" };
		for (final String iv : invalid)
			assertFalse(logical.isValid(iv), iv);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void postalCodeUK_endToEnd() throws IOException, FTAPluginException, com.cobber.fta.core.FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("PostCode");
		analysis.setLocale(Locale.forLanguageTag("en-GB"));

		final String[] inputs = {
			"EC1A 1BB", "W1A 0AX", "M1 1AE", "B1 1BB", "CR2 6XH",
			"DN55 1PT", "SW1A 2AA", "E1 6AN", "N1 9GU", "WC2N 5DU",
			"SE1 7PB", "W2 3QL", "NW3 5PR", "E14 5AB", "SW3 4RY",
			"W8 4PT", "N7 8RP", "SE22 0RZ", "SW6 1JF", "WC1E 7HT"
		};

		for (final String s : inputs)
			analysis.train(s);

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getSemanticType(), "POSTAL_CODE.POSTAL_CODE_UK");
		assertTrue(result.getConfidence() >= 0.95);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void postalCodeUK_lowCardinalityNoPostHeader() throws IOException, FTAPluginException, com.cobber.fta.core.FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("field1");
		analysis.setLocale(Locale.forLanguageTag("en-GB"));

		// Only 3 distinct values and no postal header — should back out
		final String[] inputs = { "EC1A 1BB", "W1A 0AX", "M1 1AE" };
		for (final String s : inputs)
			for (int i = 0; i < 5; i++)
				analysis.train(s);

		final TextAnalysisResult result = analysis.getResult();
		assertFalse("POSTAL_CODE.POSTAL_CODE_UK".equals(result.getSemanticType()));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaCountryDetection() throws IOException, FTAPluginException, com.cobber.fta.core.FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("国名");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));

		// Entries taken directly from the ja_countries.csv reference file
		final String[] inputs = {
			"日本", "中華人民共和国", "中華民国", "大韓民国", "朝鮮民主主義人民共和国",
			"印度", "印度尼西亜", "越南", "泰", "比利賓",
			"馬来西亜", "緬甸", "蒙古", "星嘉波", "錫蘭",
			"土耳其", "沙地亜剌比亜", "叙利亜", "伊朗", "伊拉克",
			"老檛", "香佐富斯坦", "塔吉克斯坦", "孟加拉", "柬埔寨",
			"尼泊爾", "巴基斯坦", "豪斯多剌里亜", "新西蘭土", "亜米利加"
		};
		for (final String s : inputs)
			analysis.train(s);

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getSemanticType(), "COUNTRY.TEXT_JA");
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaCityDetection() throws IOException, FTAPluginException, com.cobber.fta.core.FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("市区町村");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));

		final String[] inputs = {
			"札幌市", "函館市", "小樽市", "旭川市", "釧路市",
			"帯広市", "北見市", "岩見沢市", "網走市", "留萌市",
			"苫小牧市", "稚内市", "美唄市", "芦別市", "江別市",
			"赤平市", "紋別市", "士別市", "名寄市", "三笠市",
			"根室市", "千歳市", "滝川市", "砂川市", "歌志内市"
		};
		for (final String s : inputs)
			analysis.train(s);

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getSemanticType(), "CITY");
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaPrefectureISODetection() throws IOException, FTAPluginException, com.cobber.fta.core.FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("都道府県コード");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));

		final String[] inputs = {
			"JP-01", "JP-02", "JP-03", "JP-04", "JP-05", "JP-06", "JP-07", "JP-08", "JP-09", "JP-10",
			"JP-11", "JP-12", "JP-13", "JP-14", "JP-15", "JP-16", "JP-17", "JP-18", "JP-19", "JP-20",
			"JP-21", "JP-22", "JP-23", "JP-24", "JP-25", "JP-26", "JP-27", "JP-28", "JP-29", "JP-30"
		};
		for (final String s : inputs)
			analysis.train(s);

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getSemanticType(), "STATE_PROVINCE.PREFECTURE_ISO_JA");
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaPrefectureISOAllCodes() throws IOException, FTAPluginException, com.cobber.fta.core.FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("pref_iso_code");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));

		// All 47 ISO codes
		for (int i = 1; i <= 47; i++)
			analysis.train(String.format("JP-%02d", i));

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getSemanticType(), "STATE_PROVINCE.PREFECTURE_ISO_JA");
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaPrefectureCodeWithHeader() throws IOException, FTAPluginException, com.cobber.fta.core.FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("都道府県コード");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));

		for (int i = 1; i <= 47; i++)
			analysis.train(String.format("%02d", i));

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getSemanticType(), "STATE_PROVINCE.PREFECTURE_CODE_JA");
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaPrefectureCodeNoHeader() throws IOException, FTAPluginException, com.cobber.fta.core.FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("code");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));

		for (int i = 1; i <= 47; i++)
			analysis.train(String.format("%02d", i));

		final TextAnalysisResult result = analysis.getResult();
		assertNotEquals(result.getSemanticType(), "STATE_PROVINCE.PREFECTURE_CODE_JA");
	}
}
