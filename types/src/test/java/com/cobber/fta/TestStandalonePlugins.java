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
}
