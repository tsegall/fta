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
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Locale;

import org.testng.annotations.Test;

import com.cobber.fta.Sample;
import com.cobber.fta.TestGroups;
import com.cobber.fta.TestSupport;
import com.cobber.fta.TestUtils;
import com.cobber.fta.TextAnalysisResult;
import com.cobber.fta.TextAnalyzer;
import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.FTAPluginException;
import com.cobber.fta.core.FTAType;

public class Japanese {

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaFirstNameDetection() throws IOException, FTAPluginException, FTAException {
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
	public void jaLastNameDetection() throws IOException, FTAPluginException, FTAException {
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
	public void jaLastFirstDetectionSpaceSeparated() throws IOException, FTAPluginException, FTAException {
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
	public void jaLastFirstDetectionConcatenated() throws IOException, FTAPluginException, FTAException {
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
	public void jaStreetAddressWithPostalCode() throws IOException, FTAPluginException, FTAException {
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
	public void jaStreetAddressWithoutPostalCode() throws IOException, FTAPluginException, FTAException {
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
	public void jaCountryDetection() throws IOException, FTAPluginException, FTAException {
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
	public void jaCountryDetectionModern() throws IOException, FTAPluginException, FTAException {
		final String[] inputs = {
			// Modern katakana names (JIS X 0304) — not in the original 71-entry CSV
			"フランス", "ドイツ", "イタリア", "スペイン", "オランダ",
			"ベルギー", "スウェーデン", "ノルウェー", "デンマーク", "フィンランド",
			"アルゼンチン", "コロンビア", "チリ", "ペルー", "エクアドル",
			"ナイジェリア", "エチオピア", "エジプト", "タンザニア", "ケニア",
			// Kanji abbreviations added in expansion
			"米国", "英国", "中国", "韓国", "北朝鮮",
			// Short katakana forms added in expansion
			"アメリカ", "ロシア", "イラン", "ベネズエラ", "台湾"
		};
		final TextAnalysisResult result = TestUtils.simpleCore(Sample.allValid(inputs), "国名", Locale.forLanguageTag("ja-JP"), "COUNTRY.TEXT_JA", FTAType.STRING, 1.0);
		assertEquals(result.getMatchCount(), inputs.length);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaCityDetection() throws IOException, FTAPluginException, FTAException {
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
	public void jaCityDetectionAltHeaders() throws IOException, FTAPluginException, FTAException {
		final String[][] cases = {
			{ "市区町村名", "1.0" },
			{ "区市町村", "1.0" },
			{ "都市", "1.0" },
			{ "居住市区町村", "1.0" }
		};
		final String[] inputs = {
			"札幌市", "函館市", "小樽市", "旭川市", "釧路市",
			"帯広市", "北見市", "岩見沢市", "網走市", "留萌市",
			"苫小牧市", "稚内市", "美唄市", "芦別市", "江別市",
			"赤平市", "紋別市", "士別市", "名寄市", "三笠市",
			"根室市", "千歳市", "滝川市", "砂川市", "歌志内市"
		};
		for (final String[] c : cases) {
			final TextAnalyzer analysis = new TextAnalyzer(c[0]);
			analysis.setLocale(Locale.forLanguageTag("ja-JP"));
			for (final String s : inputs)
				analysis.train(s);
			final TextAnalysisResult result = analysis.getResult();
			assertEquals(result.getSemanticType(), "CITY", "header: " + c[0]);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaPrefectureISODetection() throws IOException, FTAPluginException, FTAException {
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
	public void jaPrefectureISOAllCodes() throws IOException, FTAPluginException, FTAException {
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
	public void jaPrefectureCodeWithHeader() throws IOException, FTAPluginException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("都道府県コード");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));

		for (int i = 1; i <= 47; i++)
			analysis.train(String.format("%02d", i));

		final TextAnalysisResult result = analysis.getResult();
		assertEquals(result.getSemanticType(), "STATE_PROVINCE.PREFECTURE_CODE_JA");
		assertEquals(result.getConfidence(), 1.0);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaPrefectureCodeNoHeader() throws IOException, FTAPluginException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("code");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));

		for (int i = 1; i <= 47; i++)
			analysis.train(String.format("%02d", i));

		final TextAnalysisResult result = analysis.getResult();
		assertNotEquals(result.getSemanticType(), "STATE_PROVINCE.PREFECTURE_CODE_JA");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaAgeDetection() throws IOException, FTAPluginException, FTAException {
		final String[][] cases = {
			{ "年齢" }, { "年令" }
		};
		for (final String[] c : cases) {
			final TextAnalyzer analysis = new TextAnalyzer(c[0]);
			analysis.setLocale(Locale.forLanguageTag("ja-JP"));
			for (int i = 20; i < 80; i++)
				analysis.train(String.valueOf(i));
			assertEquals(analysis.getResult().getSemanticType(), "PERSON.AGE", "header: " + c[0]);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaAgeNoHeader() throws IOException, FTAPluginException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("col");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));
		for (int i = 20; i < 80; i++)
			analysis.train(String.valueOf(i));
		assertNotEquals(analysis.getResult().getSemanticType(), "PERSON.AGE");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaAgeRangeDetection() throws IOException, FTAPluginException, FTAException {
		final String[] inputs = {
			"0-4", "5-9", "10-14", "15-19", "20-24", "25-29",
			"30-34", "35-39", "40-44", "45-49", "50-54", "55-59",
			"60-64", "65-69", "70-74", "75-79", "80-84", "85+"
		};
		final String[][] cases = {
			{ "年齢" }, { "年令" }
		};
		for (final String[] c : cases) {
			final TextAnalyzer analysis = new TextAnalyzer(c[0]);
			analysis.setLocale(Locale.forLanguageTag("ja-JP"));
			for (final String s : inputs)
				analysis.train(s);
			assertEquals(analysis.getResult().getSemanticType(), "PERSON.AGE_RANGE", "header: " + c[0]);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaAgeRangeNoHeader() throws IOException, FTAPluginException, FTAException {
		final String[] inputs = {
			"0-4", "5-9", "10-14", "15-19", "20-24", "25-29",
			"30-34", "35-39", "40-44", "45-49", "50-54", "55-59",
			"60-64", "65-69", "70-74", "75-79", "80-84", "85+"
		};
		final TextAnalyzer analysis = new TextAnalyzer("col");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));
		for (final String s : inputs)
			analysis.train(s);
		assertNotEquals(analysis.getResult().getSemanticType(), "PERSON.AGE_RANGE");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaQuarterDetection() throws IOException, FTAPluginException, FTAException {
		// .*四半期.* pattern — bare header and compound header both match
		final String[][] cases = {
			{ "四半期" }, { "会計四半期" }
		};
		final String[] inputs = {
			"1", "2", "3", "4", "1", "2", "3", "4", "1", "2",
			"3", "4", "1", "2", "3", "4", "1", "2", "3", "4"
		};
		for (final String[] c : cases) {
			final TextAnalyzer analysis = new TextAnalyzer(c[0]);
			analysis.setLocale(Locale.forLanguageTag("ja-JP"));
			for (final String s : inputs)
				analysis.train(s);
			assertEquals(analysis.getResult().getSemanticType(), "PERIOD.QUARTER", "header: " + c[0]);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaMonthDetection() throws IOException, FTAPluginException, FTAException {
		final String[][] cases = {
			{ "月" }, { "月号" }, { "月数" }
		};
		for (final String[] c : cases) {
			final TextAnalyzer analysis = new TextAnalyzer(c[0]);
			analysis.setLocale(Locale.forLanguageTag("ja-JP"));
			for (int month = 1; month <= 12; month++)
				for (int rep = 0; rep < 3; rep++)
					analysis.train(String.valueOf(month));
			assertEquals(analysis.getResult().getSemanticType(), "MONTH.DIGITS", "header: " + c[0]);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaMonthNoHeader() throws IOException, FTAPluginException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("col");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));
		for (int month = 1; month <= 12; month++)
			for (int rep = 0; rep < 3; rep++)
				analysis.train(String.valueOf(month));
		assertNotEquals(analysis.getResult().getSemanticType(), "MONTH.DIGITS");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaDayDetection() throws IOException, FTAPluginException, FTAException {
		final String[][] cases = {
			{ "日" }, { "日数" }
		};
		for (final String[] c : cases) {
			final TextAnalyzer analysis = new TextAnalyzer(c[0]);
			analysis.setLocale(Locale.forLanguageTag("ja-JP"));
			for (int day = 1; day <= 31; day++)
				analysis.train(String.valueOf(day));
			assertEquals(analysis.getResult().getSemanticType(), "DAY.DIGITS", "header: " + c[0]);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaDayNoHeader() throws IOException, FTAPluginException, FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("col");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));
		for (int day = 1; day <= 31; day++)
			analysis.train(String.valueOf(day));
		assertNotEquals(analysis.getResult().getSemanticType(), "DAY.DIGITS");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaFreeTextDetection() throws IOException, FTAPluginException, FTAException {
		final String[] inputs = {
			"この商品は非常に使いやすく、毎日愛用しています。",
			"配送が予定より早く届き、梱包も丁寧でした。",
			"品質は申し分なく、価格以上の価値があると思います。",
			"カスタマーサポートの対応が迅速で、問題が解決しました。",
			"デザインがシンプルで機能的、長く使えそうです。",
			"初めて購入しましたが、期待を上回る出来栄えでした。",
			"説明書が分かりやすく、設定に時間がかかりませんでした。",
			"友人にも勧めたいと思える、満足度の高い商品です。",
			"素材の質感が良く、写真で見るよりも実物の方が魅力的です。",
			"リピート購入です。前回同様、品質が安定していて安心できます。"
		};
		// All six Japanese header keywords must fire
		final String[] headers = { "説明", "備考", "コメント", "理由", "記述", "注記" };
		for (final String header : headers) {
			final TextAnalyzer analysis = new TextAnalyzer(header);
			analysis.setLocale(Locale.forLanguageTag("ja-JP"));
			for (final String s : inputs)
				analysis.train(s);
			assertEquals(analysis.getResult().getSemanticType(), "FREE_TEXT", "header: " + header);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaFreeTextNoHeader() throws IOException, FTAPluginException, FTAException {
		final String[] inputs = {
			"この商品は非常に使いやすく、毎日愛用しています。",
			"配送が予定より早く届き、梱包も丁寧でした。",
			"品質は申し分なく、価格以上の価値があると思います。",
			"カスタマーサポートの対応が迅速で、問題が解決しました。",
			"デザインがシンプルで機能的、長く使えそうです。",
			"初めて購入しましたが、期待を上回る出来栄えでした。",
			"説明書が分かりやすく、設定に時間がかかりませんでした。",
			"友人にも勧めたいと思える、満足度の高い商品です。",
			"素材の質感が良く、写真で見るよりも実物の方が魅力的です。",
			"リピート購入です。前回同様、品質が安定していて安心できます。"
		};
		final TextAnalyzer analysis = new TextAnalyzer("col");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));
		for (final String s : inputs)
			analysis.train(s);
		assertNotEquals(analysis.getResult().getSemanticType(), "FREE_TEXT");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaColorDetection() throws IOException, FTAPluginException, FTAException {
		// Mix of katakana, kanji+色, and bare kanji forms
		final String[] inputs = {
			"レッド", "ブルー", "グリーン", "ホワイト", "ブラック", "オレンジ", "ピンク", "グレー",
			"ブラウン", "パープル", "イエロー", "ベージュ", "赤色", "青色", "緑色",
			"茶色", "灰色", "金色", "赤", "青", "黄", "レッド", "ブルー"
		};
		for (final String header : new String[] { "色", "カラー", "商品色" }) {
			final TextAnalyzer analysis = new TextAnalyzer(header);
			analysis.setLocale(Locale.forLanguageTag("ja-JP"));
			for (final String s : inputs)
				analysis.train(s);
			assertEquals(analysis.getResult().getSemanticType(), "COLOR.TEXT_JA", "header: " + header);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaColorNoHeader() throws IOException, FTAPluginException, FTAException {
		final String[] inputs = {
			"レッド", "ブルー", "グリーン", "ホワイト", "ブラック", "オレンジ", "ピンク", "グレー",
			"ブラウン", "パープル", "イエロー", "ベージュ", "赤色", "青色", "緑色"
		};
		final TextAnalyzer analysis = new TextAnalyzer("col");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));
		for (final String s : inputs)
			analysis.train(s);
		assertNotEquals(analysis.getResult().getSemanticType(), "COLOR.TEXT_JA");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaLanguageDetection() throws IOException, FTAPluginException, FTAException {
		final String[] inputs = {
			"日本語", "英語", "フランス語", "ドイツ語", "スペイン語", "中国語", "ロシア語", "韓国語",
			"イタリア語", "ポルトガル語", "アラビア語", "ヒンディー語", "タイ語", "ベトナム語",
			"インドネシア語", "オランダ語", "ポーランド語", "スウェーデン語", "ノルウェー語", "ギリシャ語"
		};
		final String[] headers = { "言語", "言語名", "使用言語" };
		for (final String header : headers) {
			final TextAnalyzer analysis = new TextAnalyzer(header);
			analysis.setLocale(Locale.forLanguageTag("ja-JP"));
			for (final String s : inputs)
				analysis.train(s);
			assertEquals(analysis.getResult().getSemanticType(), "LANGUAGE.TEXT_JA", "header: " + header);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaCompanyDetection() throws IOException, FTAPluginException, FTAException {
		final String[] inputs = {
			"トヨタ自動車株式会社", "本田技研工業株式会社", "ソニーグループ株式会社", "パナソニックホールディングス株式会社",
			"富士通株式会社", "日本電気株式会社", "三菱電機株式会社", "東芝株式会社", "シャープ株式会社",
			"日立製作所株式会社", "任天堂株式会社", "スズキ株式会社", "マツダ株式会社", "日産自動車株式会社",
			"ソフトバンクグループ株式会社", "楽天グループ株式会社", "花王株式会社", "武田薬品工業株式会社",
			"株式会社資生堂", "キリンホールディングス株式会社"
		};
		final String[] headers = { "会社名", "企業名", "取引先名" };
		for (final String header : headers) {
			final TextAnalyzer analysis = new TextAnalyzer(header);
			analysis.setLocale(Locale.forLanguageTag("ja-JP"));
			for (final String s : inputs)
				analysis.train(s);
			assertEquals(analysis.getResult().getSemanticType(), "COMPANY_NAME", "header: " + header);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaCryptoDetection() throws IOException, FTAPluginException, FTAException {
		final String[] inputs = {
			"ビットコイン", "イーサリアム", "リップル", "ライトコイン", "カルダノ", "ポルカドット", "ドージコイン",
			"ソラナ", "アバランチ", "チェーンリンク", "ステラ", "モネロ", "ダッシュ", "ネオ", "コスモス",
			"アルゴランド", "テゾス", "バイナンスコイン", "ユニスワップ", "ポリゴン"
		};
		final String[] headers = { "暗号通貨", "仮想通貨", "暗号資産" };
		for (final String header : headers) {
			final TextAnalyzer analysis = new TextAnalyzer(header);
			analysis.setLocale(Locale.forLanguageTag("ja-JP"));
			for (final String s : inputs)
				analysis.train(s);
			assertEquals(analysis.getResult().getSemanticType(), "CRYPTOCURRENCY.TEXT_JA", "header: " + header);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaJobTitleDetectionWithHeader() throws IOException, FTAPluginException, FTAException {
		final String[] inputs = {
			"部長", "課長", "係長", "主任", "担当", "社長", "取締役", "監査役",
			"エンジニア", "マネージャー", "ディレクター", "弁護士", "看護師", "薬剤師",
			"技術者", "研究者", "営業担当者", "人事部長", "総務課長", "開発リーダー"
		};
		final String[] headers = { "役職", "職種", "役職名", "職位" };
		for (final String header : headers) {
			final TextAnalyzer analysis = new TextAnalyzer(header);
			analysis.setLocale(Locale.forLanguageTag("ja-JP"));
			for (final String s : inputs)
				analysis.train(s);
			assertEquals(analysis.getResult().getSemanticType(), "JOB_TITLE_JA", "header: " + header);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaJobTitleDetectionNoHeader() throws IOException, FTAPluginException, FTAException {
		// Without a header, needs 20+ samples and 10+ distinct values
		final String[] inputs = {
			"部長", "課長", "係長", "主任", "社長", "取締役", "監査役", "専務", "常務", "副社長",
			"エンジニア", "マネージャー", "ディレクター", "弁護士", "看護師", "薬剤師",
			"技術者", "研究者", "営業部長", "人事課長", "総務担当", "開発者", "設計者", "教員"
		};
		final TextAnalyzer analysis = new TextAnalyzer("col");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));
		for (final String s : inputs)
			analysis.train(s);
		assertEquals(analysis.getResult().getSemanticType(), "JOB_TITLE_JA");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaContinentDetection() throws IOException, FTAPluginException, FTAException {
		final String[] inputs = {
			"アジア", "ヨーロッパ", "アフリカ", "北アメリカ", "南アメリカ", "オセアニア", "南極",
			"アジア", "ヨーロッパ", "アフリカ", "北アメリカ", "南アメリカ", "オセアニア", "南極",
			"アジア", "欧州", "北米", "南米", "アフリカ", "オセアニア"
		};
		final String[] headers = { "大陸", "地域", "所在大陸" };
		for (final String header : headers) {
			final TextAnalyzer analysis = new TextAnalyzer(header);
			analysis.setLocale(Locale.forLanguageTag("ja-JP"));
			for (final String s : inputs)
				analysis.train(s);
			assertEquals(analysis.getResult().getSemanticType(), "CONTINENT.TEXT_JA", "header: " + header);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaCurrencyDetection() throws IOException, FTAPluginException, FTAException {
		final String[] inputs = {
			"円", "米ドル", "ユーロ", "イギリスポンド", "スイスフラン", "カナダドル", "オーストラリアドル",
			"中国人民元", "韓国ウォン", "インドルピー", "ブラジルレアル", "ロシアルーブル", "メキシコペソ",
			"スウェーデンクローナ", "ノルウェークローネ", "タイバーツ", "シンガポールドル", "香港ドル",
			"南アフリカランド", "トルコリラ"
		};
		final String[] headers = { "通貨", "通貨名", "支払通貨" };
		for (final String header : headers) {
			final TextAnalyzer analysis = new TextAnalyzer(header);
			analysis.setLocale(Locale.forLanguageTag("ja-JP"));
			for (final String s : inputs)
				analysis.train(s);
			assertEquals(analysis.getResult().getSemanticType(), "CURRENCY.TEXT_JA", "header: " + header);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaNationalityDetection() throws IOException, FTAPluginException, FTAException {
		final String[] inputs = {
			"日本人", "アメリカ人", "中国人", "韓国人", "フランス人", "ドイツ人", "イギリス人", "イタリア人",
			"スペイン人", "ブラジル人", "カナダ人", "オーストラリア人", "インド人", "ロシア人", "メキシコ人",
			"オランダ人", "ポーランド人", "スウェーデン人", "アルゼンチン人", "ベトナム人"
		};
		final String[] headers = { "国籍", "出身国", "お客様国籍" };
		for (final String header : headers) {
			final TextAnalyzer analysis = new TextAnalyzer(header);
			analysis.setLocale(Locale.forLanguageTag("ja-JP"));
			for (final String s : inputs)
				analysis.train(s);
			assertEquals(analysis.getResult().getSemanticType(), "NATIONALITY_JA", "header: " + header);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaNationalityDoublets() throws IOException, FTAPluginException, FTAException {
		// Both 米国人/アメリカ人 and 英国人/イギリス人 doublets should be valid
		final String[] inputs = {
			"米国人", "英国人", "日本人", "中国人", "韓国人", "フランス人", "ドイツ語人", "イタリア人",
			"スペイン人", "ブラジル人", "カナダ人", "オーストラリア人", "インド人", "ロシア人", "台湾人",
			"香港人", "スコットランド人", "アイルランド人", "スイス人", "アメリカ人"
		};
		final TextAnalyzer analysis = new TextAnalyzer("国籍");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));
		for (final String s : inputs)
			analysis.train(s);
		// ドイツ語人 is not in the list and will be an outlier — confirm detection still succeeds
		assertEquals(analysis.getResult().getSemanticType(), "NATIONALITY_JA");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaHonorificDetection() throws IOException, FTAPluginException, FTAException {
		final String[] inputs = {
			"様", "さん", "先生", "氏", "様", "さん", "さん", "様", "先生", "殿",
			"様", "さん", "氏", "君", "様", "さん", "先生", "様", "さん", "氏"
		};
		final String[] headers = { "敬称", "称号", "お客様敬称" };
		for (final String header : headers) {
			final TextAnalyzer analysis = new TextAnalyzer(header);
			analysis.setLocale(Locale.forLanguageTag("ja-JP"));
			for (final String s : inputs)
				analysis.train(s);
			assertEquals(analysis.getResult().getSemanticType(), "HONORIFIC_JA", "header: " + header);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaCompanyNoHeader() throws IOException, FTAPluginException, FTAException {
		final String[] inputs = {
			"トヨタ自動車株式会社", "本田技研工業株式会社", "ソニーグループ株式会社", "富士通株式会社",
			"日本電気株式会社", "三菱電機株式会社", "東芝株式会社", "任天堂株式会社", "スズキ株式会社",
			"ソフトバンクグループ株式会社", "楽天グループ株式会社", "花王株式会社"
		};
		final TextAnalyzer analysis = new TextAnalyzer("col");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));
		for (final String s : inputs)
			analysis.train(s);
		assertNotEquals(analysis.getResult().getSemanticType(), "COMPANY_NAME");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaIndustryDetection() throws IOException, FTAPluginException, FTAException {
		final String[] inputs = {
			"製造業", "情報通信業", "金融業", "保険業", "建設業", "小売業", "卸売業", "農業",
			"医療業", "教育業", "運輸業", "不動産業", "宿泊業", "飲食業", "鉄鋼業", "化学工業",
			"電気業", "通信業", "鉱業", "漁業"
		};
		final String[] headers = { "業界", "業種", "産業", "業界名", "業種名" };
		for (final String header : headers) {
			final TextAnalyzer analysis = new TextAnalyzer(header);
			analysis.setLocale(Locale.forLanguageTag("ja-JP"));
			for (final String s : inputs)
				analysis.train(s);
			assertEquals(analysis.getResult().getSemanticType(), "INDUSTRY_JA", "header: " + header);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaMaritalStatusDetection() throws IOException, FTAPluginException, FTAException {
		final String[] inputs = {
			"既婚", "未婚", "独身", "離婚", "死別", "別居", "婚約中", "事実婚", "再婚", "未亡人",
			"既婚", "未婚", "独身", "離婚", "既婚", "未婚"
		};
		final String[] headers = { "婚姻状況", "婚姻", "配偶者状況", "結婚状況" };
		for (final String header : headers) {
			final TextAnalyzer analysis = new TextAnalyzer(header);
			analysis.setLocale(Locale.forLanguageTag("ja-JP"));
			for (final String s : inputs)
				analysis.train(s);
			assertEquals(analysis.getResult().getSemanticType(), "PERSON.MARITAL_STATUS_JA", "header: " + header);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaPrefectureRegionDetection() throws IOException, FTAPluginException, FTAException {
		final String[] inputs = {
			"北海道", "東北", "関東", "中部", "近畿", "関西", "中国", "四国",
			"九州", "九州・沖縄", "北海道地方", "東北地方", "関東地方", "中部地方",
			"近畿地方", "関西地方", "中国地方", "四国地方", "九州地方"
		};
		final String[] headers = { "地方", "地域区分", "地域", "地区" };
		for (final String header : headers) {
			final TextAnalyzer analysis = new TextAnalyzer(header);
			analysis.setLocale(Locale.forLanguageTag("ja-JP"));
			for (final String s : inputs)
				analysis.train(s);
			assertEquals(analysis.getResult().getSemanticType(), "STATE_PROVINCE.REGION_NAME_JA", "header: " + header);
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaLongitudeDetection() throws IOException, FTAPluginException, FTAException {
		// Real longitude values from Hokkaido, Japan (~141°E)
		final String[] inputs = {
			"141.319722", "141.321733", "141.319617", "141.323040", "141.322217",
			"141.318922", "141.356364", "141.341914", "141.340360", "141.338831",
			"141.337283", "141.335727", "141.334155", "141.332590", "141.331028"
		};
		final TextAnalyzer analysis = new TextAnalyzer("経度");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));
		for (final String s : inputs)
			analysis.train(s);
		assertEquals(analysis.getResult().getSemanticType(), "COORDINATE.LONGITUDE_DECIMAL");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.PLUGINS })
	public void jaLatitudeDetection() throws IOException, FTAPluginException, FTAException {
		// Real latitude values from Hokkaido, Japan (~43°N)
		final String[] inputs = {
			"43.064615", "43.061389", "43.058333", "43.055278", "43.052222",
			"43.049167", "43.046111", "43.043056", "43.040000", "43.036944",
			"43.033889", "43.030833", "43.027778", "43.024722", "43.021667"
		};
		final TextAnalyzer analysis = new TextAnalyzer("緯度");
		analysis.setLocale(Locale.forLanguageTag("ja-JP"));
		for (final String s : inputs)
			analysis.train(s);
		assertEquals(analysis.getResult().getSemanticType(), "COORDINATE.LATITUDE_DECIMAL");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void basicJapanese() throws FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("basicJapanese");
		final Locale locale = Locale.forLanguageTag("ja-JP");
		analysis.setLocale(locale);
		final String[] inputs = {
				"2018年10月6日", "2019年4月30日", "2017年3月12日", "2008年4月30日", "2087年5月20日",
				"2019年6月10日", "2019年7月30日", "2019年4月30日", "2017年8月12日", "2019年4月30日",
				"2017年3月12日", "2008年4月30日", "2087年5月20日", "2019年6月10日", "2019年7月30日",
				"2019年4月30日", "2017年8月12日", "2019年4月30日", "2017年3月12日", "2008年4月30日",
				"2087年5月20日", "2019年6月10日", "2019年7月30日", "2019年4月30日", "2017年8月12日",
				"2019年4月30日", "2017年3月12日", "2008年4月30日", "2087年5月20日", "2019年6月10日",
				"2019年7月30日", "2019年4月30日", "2017年8月12日"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "yyyy年M月d日");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}年\\d{1,2}月\\d{1,2}日");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
			assertNull(TestUtils.checkParseable(result, input, locale));
		}
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void japaneseEra() throws FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("japaneseEra");
		final Locale locale = Locale.forLanguageTag("ja-JP");
		analysis.setLocale(locale);
		final String[] inputs = {
				"平成12年", "平成13年", "平成14年",
				"平成15年", "平成16年", "明治23年",
				"昭和22年", "令和5年"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "GGGGyy年");
		assertEquals(result.getSampleCount(), inputs.length);
		// 令和5年 has a 1-digit era year; the dominant format is 2-digit so it lands as an outlier
		assertEquals(result.getOutlierCount(), 1);
		assertEquals(result.getMatchCount(), inputs.length - 1);
		assertEquals(result.getNullCount(), 0);
		assertNotNull(result.getRegExp());
		assertTrue(result.getConfidence() > 0.8);
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void japaneseEraMotonen() throws FTAException {
		// Tests Japanese era date detection including '元年' (first year of an era, not a digit)
		final TextAnalyzer analysis = new TextAnalyzer("japaneseEraMotonen");
		final Locale locale = Locale.forLanguageTag("ja-JP");
		analysis.setLocale(locale);
		final String[] inputs = {
				"昭和64年1月7日", "平成元年1月8日", "平成12年1月1日",
				"平成31年4月30日", "令和元年5月1日", "令和6年4月1日", "令和8年1月1日"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATE);
		assertEquals(result.getTypeModifier(), "GGGGy年M月d日");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertNotNull(result.getRegExp());
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts(false));
	}

	@Test(groups = { TestGroups.ALL, TestGroups.DATETIME })
	public void chineseAM() throws FTAException {
		final TextAnalyzer analysis = new TextAnalyzer("japaneseEra");
		final Locale locale = Locale.forLanguageTag("ja-JP");
		analysis.setLocale(locale);
		final String[] inputs = {
				"2015/1/5 上午 12:00:00", "2015/1/30 上午 12:00:00", "2014/12/30 上午 12:00:00",
				"2015/1/13 上午 12:00:00", "2015/1/20 上午 12:00:00", "2014/12/24 上午 12:00:00",
				"2014/12/29 上午 12:00:00", "2015/2/11 上午 12:00:00", "2015/1/6 上午 12:00:00",
				"2015/1/23 上午 12:00:00", "2015/1/28 上午 12:00:00", "2015/2/4 上午 12:00:00",
				"2015/1/28 上午 12:00:00", "2015/1/5 上午 12:00:00", "2015/1/15 上午 12:00:00",
				"2015/2/26 上午 12:00:00", "2014/12/24 上午 12:00:00", "2013/11/14 上午 12:00:00",
				"2015/2/24 上午 12:00:00"
		};

		for (final String input : inputs)
			analysis.train(input);

		final TextAnalysisResult result = analysis.getResult();
		TestUtils.checkSerialization(analysis);

		assertEquals(result.getType(), FTAType.LOCALDATETIME);
		assertEquals(result.getTypeModifier(), "yyyy/M/d 上午 HH:mm:ss");
		assertEquals(result.getSampleCount(), inputs.length);
		assertEquals(result.getOutlierCount(), 0);
		assertEquals(result.getMatchCount(), inputs.length);
		assertEquals(result.getNullCount(), 0);
		assertEquals(result.getRegExp(), "\\d{4}/\\d{1,2}/\\d{1,2} 上午 \\d{2}:\\d{2}:\\d{2}");
		assertEquals(result.getConfidence(), 1.0);
		assertNull(result.checkCounts(false));

		TestSupport.checkHistogram(result, 10, true);
		TestSupport.checkQuantiles(result);

		for (final String input : inputs) {
			assertTrue(input.matches(result.getRegExp()));
			assertNull(TestUtils.checkParseable(result, input, locale));
		}
	}
}
