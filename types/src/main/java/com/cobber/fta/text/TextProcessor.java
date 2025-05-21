/*
 * Copyright 2017-2025 Tim Segall
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
package com.cobber.fta.text;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Core class for processing free form text.
 */
public class TextProcessor {
	private TextConfig config;
	private Locale locale;
	private static Map<String, TextConfig> allConfigData = new HashMap<>();
	private int lastOffset;
	private int totalAlphaWordLength;

	/** Number of words in this sentence. */
	private int wordsInSentence;
	/** Number of words consisting of only alphas. */
	private int alphaWords;
	/** Number of alphaWords of length at least 3. */
	private int longWords;
	/** Number of longWords with a plausible stem (defined by TextConfig.getStarts(). */
	private int realWords;

	public enum Determination {
		/** Input looks like free text. */
		OK,
		/** Average length of words does not look reasonable. */
		BAD_AVERAGE_LENGTH,
		/** We need some real looking words. */
		NOT_ENOUGH_REAL_WORDS,
		/** The percentage of alphas, wordBreaks, punctuation(,.) and any digits in digit only words is too low. */
		PERCENT_TOO_LOW,
		/** Sentence is too short. */
		SENTENCE_TOO_SHORT,
		/** Input just has a single word. */
		SINGLE_WORD,
		/** Detected word that is unreasonably long. */
		TOO_LONG
	}

	public class TextResult {
		private Determination determination;
		private int alphas;
		private int alphaLower;
		private int alphaUpper;
		private int digits;
		private int words;
		private int sentenceBreaks;
		private int wordBreaks;
		private int punctuation;
		private int spaces;

		public TextResult() {
			determination = Determination.OK;
		}

		/**
		 * Return the number of alpha (isAlphabetic()) characters detected.
		 * Note: This may not be complete if the length of the input is greater than the analysis maximum.
		 * @return The number of alpha characters detected.
		 */
		public int getAlphas() {
			return alphas;
		}

		/**
		 * Return the number of digit (isDigit()) characters detected in digit-only words.
		 * Note: This may not be complete if the length of the input is greater than the analysis maximum.
		 * @return The number of digit characters detected.
		 */
		public int getDigits() {
			return digits;
		}

		/**
		 * Return the number of words detected.
		 * Note: This may not be complete if the length of the input is greater than the analysis maximum.
		 * @return The number of words detected.
		 */
		public int getWords() {
			return words;
		}

		/**
		 * Return the number of sentence break characters detected.
		 * Note: This may not be complete if the length of the input is greater than the analysis maximum.
		 * @return The number of sentence break characters detected.
		 */
		public int getSentenceBreaks() {
			return sentenceBreaks;
		}

		/**
		 * Return the number of word break characters detected.
		 * Note: This may not be complete if the length of the input is greater than the analysis maximum.
		 * @return The number of word break characters detected.
		 */
		public int getWordBreaks() {
			return wordBreaks;
		}

		/**
		 * Return the number of punctuation characters detected.
		 * Note: This may not be complete if the length of the input is greater than the analysis maximum.
		 * @return The number of punctuation characters detected.
		 */
		public int getPunctuation() {
			return punctuation;
		}

		/**
		 * Return the number of space characters (isWhiteSpace()) detected.
		 * Note: This may not be complete if the length of the input is greater than the analysis maximum.
		 * @return The number of space characters detected.
		 */
		public int getSpaces() {
			return spaces;
		}

		public Determination getDetermination() {
			return determination;
		}
	}

	private class WordState {
		int charsInWord;
		int digitsInWord;
		int lastWordLength;
	}

	static {
		// Bulgarian configuration
		allConfigData.put("BG", new TextConfig(
				26,				// Maximum word length - choose something that is reasonable
				3.0, 10.0,		// Average word length is ~6.5, so choose a reasonable lower and upper bound
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				160,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				", /();:.!?",	// Word Break characters
				",\"'-();:.!?",	// Punctuation character
				new String[] {
						"аб", "ав", "аг", "ад", "аз", "ай", "ак", "ал", "ам", "ан", "ап", "ар", "ас", "ат", "ау", "аф", "ах", "аш",
						"ба", "бе", "би", "бл", "бо", "бр", "бу", "бъ", "бю", "бя",
						"ва", "вд", "ве", "вз", "ви", "вк", "вл", "вн", "во", "вп", "вр", "вс", "вт", "вх", "въ", "вя",
						"га", "ге", "ги", "гл", "гн", "го", "гр", "гу", "гъ",
						"да", "дв", "де", "дж", "ди", "дл", "дн", "до", "др", "ду", "дъ", "дя",
						"ев", "ег", "ед", "ез", "ей", "ек", "ел", "ем", "ен", "еп", "ер", "ес", "ет", "еф", "ех",
						"жа", "же", "жи", "жу",
						"за", "зв", "зд", "зе", "зи", "зл", "зм", "зн", "зо", "зъ",
						"ив", "иг", "ид", "из", "ик", "ил", "им", "ин", "ир", "ис", "ит",
						"йо",
						"ка", "кв", "ке", "ки", "кл", "км", "кн", "ко", "кр", "ку", "къ",
						"ла", "ле", "ли", "ло", "лу", "лъ", "лю", "ля",
						"ма", "ме", "ми", "мл", "мм", "мн", "мо", "мр", "му", "мъ", "мя",
						"на", "не", "ни", "но", "ну", "ня",
						"об", "ог", "од", "ож", "оз", "ок", "ол", "ом", "он", "оо", "оп", "ор", "ос", "от", "оу", "оф", "ох", "оц", "оч",
						"па", "пе", "пи", "пл", "по", "пр", "пс", "пт", "пу", "пъ", "пя",
						"ра", "ре", "ри", "ро", "ру", "ръ",
						"са", "сб", "св", "сг", "сд", "се", "си", "ск", "сл", "см", "сн", "со", "сп", "ср", "ст", "су", "сх", "сц", "сч", "съ", "сю", "ся",
						"та", "тв", "те", "ти", "то", "тр", "ту", "тъ", "тя",
						"уа", "уб", "ув", "уг", "уд", "уе", "уж", "уи", "ул", "ум", "ун", "уо", "уп", "ур", "ус", "ут", "ух", "уч", "уш",
						"фа", "фе", "фи", "фл", "фо", "фр", "фу",
						"ха", "хв", "хе", "хи", "хл", "хм", "хо", "хр", "ху", "хъ", "хю",
						"ца", "цв", "це", "ци", "цъ", "ця",
						"ча", "че", "чи", "чл", "чо", "чу",
						"ша", "ше", "ши", "шк", "шо", "шп", "шу",
						"ща", "ще", "щи", "що", "щя",
						"ъг", "ър", "ъъ",
						"юн",
						"яб", "яв", "яд", "яж", "яй", "як", "ял", "ян", "яп", "яс"
				}
				));
		// Catalan configuration
		allConfigData.put("CA", new TextConfig(
				26,				// Maximum word length - choose something that is reasonable
				3.0, 10.0,		// Average word length is ~6.5, so choose a reasonable lower and upper bound
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				160,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				", /();:.!?",	// Word Break characters
				",\"'-();:.!?",	// Punctuation character
				new String[] {
						"aa", "ab", "ac", "ad", "ae", "aè", "af", "ag", "ah", "ai", "aï", "aj", "al", "àl", "am", "an", "àn", "ap", "aq", "ar", "àr", "as", "at", "au", "av",
						"ba", "bà", "be", "bé", "bè", "bi", "bj", "bl", "bo", "br", "bu", "ca",
						"cà", "cd", "ce", "cè", "ch", "ci", "cl", "co", "có", "cò", "cr", "cu", "cy",
						"da", "dà", "de", "dé", "dè", "di", "do", "dó", "dò", "dr", "du", "dy",
						"ea", "ec", "ed", "ee", "ef", "eg", "eh", "ei", "el", "em", "en", "ep", "eq", "er", "ér", "es", "és", "et", "eu", "ev", "ex", "èx",
						"fa", "fà", "fe", "fi", "fí", "fl", "fo", "fó", "fr", "fu",
						"ga", "ge", "gi", "gl", "go", "gr", "gu",
						"ha", "hà", "he", "hi", "ho", "hò", "hu",
						"ia", "id", "ig", "ii", "il", "im", "in", "ín", "io", "ir", "is", "it", "iv",
						"ja", "je", "ji", "jo", "ju", "jú",
						"ka", "ke", "kg", "kh", "ki", "kl", "kn", "ko", "kr", "ku",
						"la", "le", "lé", "li", "lí", "ll", "lo", "lò", "ls", "lu", "ly",
						"ma", "mà", "mc", "me", "mè", "mi", "mí", "mm", "mo", "mò", "mu", "mú", "my",
						"na", "ne", "né", "ni", "no", "nò", "nu", "nú", "ny",
						"ob", "oc", "od", "of", "oh", "ok", "ol", "om", "on", "op", "or", "òr", "os", "ós", "ou", "ov", "ox",
						"pa", "pà", "pe", "pè", "ph", "pi", "pí", "pl", "po", "pr", "ps", "pu", "pú",
						"qa", "qu", "qü",
						"ra", "rà", "re", "ri", "ro", "ru",
						"sa", "sà", "sc", "se", "sè", "sh", "si", "sí", "sk", "sm", "so", "só", "sò", "sp", "sq", "sr", "st", "su", "sw", "sy",
						"ta", "te", "té", "tè", "th", "ti", "tí", "to", "tò", "tr", "tu", "tú", "ty",
						"ub", "uh", "ul", "úl", "um", "un", "ún", "ur", "us", "ut", "út",
						"va", "và", "ve", "vé", "vi", "ví", "vo", "vu",
						"wa", "we", "wh", "wi", "wo", "wy",
						"xa", "xe", "xi", "xo",
						"ya", "yo", "yu",
						"za", "ze", "zo",
				}
				));
		// Danish configuration
		allConfigData.put("DA", new TextConfig(
				26,				// Maximum word length - choose something that is reasonable
				3.0, 10.0,		// Average word length is ~6.5, so choose a reasonable lower and upper bound
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				160,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				", /();:.!?",	// Word Break characters
				",\"'-();:.!?",	// Punctuation character
				new String[] {
						"ab", "åb", "ac", "ad", "af", "ag", "æg", "åh", "ak", "al", "æl", "am", "an", "ån", "æn", "ap", "ar", "år", "ær", "as", "at", "au",
						"ba", "bå", "bæ", "be", "bi", "bj", "bl", "bo", "bø", "br", "bu", "by",
						"ca", "ce", "ch", "ci", "co", "cy",
						"da", "då", "dæ", "de", "di", "do", "dø", "dr", "du", "dy",
						"ef", "eg", "ej", "ek", "el", "em", "en", "er", "et", "eu", "ev",
						"fa", "få", "fæ", "fe", "fi", "fj", "fl", "fo", "fø", "fr", "fu", "fy",
						"ga", "gå", "gæ", "ge", "gi", "gl", "gn", "go", "gø", "gr", "gu", "gy",
						"ha", "hå", "hæ", "he", "hi", "hj", "ho", "hø", "hu", "hv", "hy",
						"id", "ig", "ik", "il", "im", "in", "ip", "ir", "is",
						"ja", "jæ", "je", "jo", "ju",
						"ka", "kæ", "ke", "ki", "kj", "kl", "kn", "ko", "kø", "kr", "ku", "kv", "ky",
						"la", "lå", "læ", "le", "li", "lo", "lø", "lu", "ly",
						"ma", "må", "mæ", "me", "mi", "mm", "mo", "mø", "mu", "my",
						"na", "nå", "næ", "ne", "ni", "no", "nø", "nu", "ny",
						"ob", "ød", "of", "og", "øg", "øh", "øj", "ok", "øk", "ol", "om", "on", "øn", "op", "or", "ør", "os", "øs", "ot", "ou", "ov", "øv",
						"pa", "på", "pæ", "pe", "pi", "pl", "po", "pr", "ps", "pu",
						"ra", "rå", "ræ", "re", "ri", "ro", "rø", "ru", "ry",
						"sa", "så", "sæ", "sc", "se", "sh", "si", "sj", "sk", "sl", "sm", "sn", "so", "sø", "sp", "st", "su", "sv", "sy",
						"ta", "tå", "tæ", "te", "ti", "tj", "to", "tø", "tr", "tu", "tv", "ty",
						"ua", "ub", "ud", "ug", "uh", "uk", "ul", "um", "un", "ur", "us", "ut", "uv",
						"va", "vå", "væ", "ve", "vi", "vo", "vr", "vu",
						"we",
						"yd",
				}
				));
		// German configuration
		allConfigData.put("DE", new TextConfig(
				26,				// Maximum word length - choose something that is reasonable
				3.0, 12.0,		// German is an agglutinating language (like German)
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				160,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				", /();:.!?",	// Word Break characters
				",\"'-();:.!?",	// Punctuation character
				new String[] {
						"ab", "ac", "ad", "af", "ag", "äh", "ak", "al", "am", "an", "än", "ap", "ar", "as", "at", "au", "äu",
						"ba", "bä", "be", "bi", "bl", "bo", "bö", "br", "bu", "bü",
						"ca", "ce", "ch", "ci", "cl", "co",
						"da", "dä", "de", "di", "do", "dr", "du", "dy",
						"eb", "ec", "ef", "eh", "ei", "el", "em", "en", "er", "es", "et", "eu", "ev", "ex",
						"fa", "fä", "fe", "fi", "fl", "fo", "fö", "fr", "fu", "fü",
						"ga", "ge", "gi", "gl", "gn", "go", "gr", "gu", "gü",
						"ha", "hä", "he", "hi", "ho", "hö", "hu", "hü", "hy",
						"ic", "id", "ig", "ih", "il", "im", "in", "ip", "ir", "is",
						"ja", "jä", "je", "jo", "ju", "jü",
						"ka", "kä", "ke", "ki", "kl", "kn", "ko", "kö", "kr", "ku", "kü",
						"la", "lä", "le", "li", "lo", "lö", "lu", "lü",
						"ma", "mä", "me", "mi", "mm", "mo", "mö", "mu", "mü", "mw", "my",
						"na", "nä", "ne", "ni", "no", "nu",
						"ob", "of", "öf", "oh", "ok", "ök", "ol", "om", "on", "op", "or", "os",
						"pa", "pe", "pf", "ph", "pi", "pl", "po", "pr", "ps", "pu", "qu",
						"ra", "re", "rh", "ri", "ro", "ru", "rü",
						"sa", "sä", "sc", "se", "sh", "si", "sk", "so", "sp", "st", "su", "sü", "sy", "sz",
						"ta", "tä", "te", "th", "ti", "to", "tö", "tr", "tu", "tü", "tw", "ty",
						"üb", "uh", "um", "un", "ur",
						"va", "ve", "vi", "vo",
						"wa", "wä", "we", "wi", "wo", "wu", "wü",
						"za", "zä", "ze", "zi", "zo", "zu", "zw",
				}
				));
		// English configuration
		allConfigData.put("EN", new TextConfig(
				20,				// antidisestablishmentarianism is 28 (there are longer), so we choose something that is reasonable
				3.0, 9.0,		// Average word length is ~5, so choose a reasonable lower and upper bound
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				120,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				", /();:.!?",	// Word Break characters
				",\"'-();:.!?",	// Punctuation character
				new String[] {
					"ab", "ac", "ad", "af", "ag", "ah", "ai", "al", "am", "an", "ap", "ar", "as", "at", "au", "av", "aw",
					"ba", "be", "bi", "bl", "bo", "br", "bu", "ca",
					"ce", "ch", "ci", "cl", "co", "cr", "cu", "cy",
					"da", "de", "di", "do", "dr", "du",
					"ea", "ec", "ed", "ef", "ei", "el", "em", "en", "ep", "eq", "er", "es", "et", "ev", "ex",
					"fa", "fe", "fi", "fl", "fo", "fr", "fu",
					"ga", "ge", "gh", "gi", "gl", "go", "gr", "gu",
					"ha", "he", "hi", "ho", "hu", "hy",
					"id", "ig", "il", "im", "in", "ir", "is", "it",
					"ja", "jo", "ju",
					"ke", "ki", "kn",
					"la", "le", "li", "lo", "lu",
					"ma", "me", "mi", "mo", "mu", "my",
					"na", "ne", "ni", "no", "nu",
					"ob", "oc", "od", "of", "ok", "on", "op", "or", "ot", "ou", "ov", "ow",
					"pa", "pe", "ph", "pi", "pl", "po", "pr", "ps", "pu",
					"qu", "ra",
					"re", "rh", "ri", "ro", "ru",
					"sa", "sc", "se", "sh", "si", "sk", "sl", "sm", "sn", "so", "sp", "sq", "st", "su", "sw", "sy",
					"ta", "te", "th", "ti", "to", "tr", "tu", "tw", "ty",
					"ug", "ul", "un", "up", "ur", "us", "ut",
					"va", "ve", "vi", "vo", "vu",
					"wa", "we", "wh", "wi", "wo", "wr",
					"ya", "ye", "yi", "yo",
					"xx",						// xx is really a no-no but it seems to be commonly used to redact
					"zo"
				}
				));
		// Spanish configuration
		allConfigData.put("ES", new TextConfig(
				26,				// Maximum word length - choose something that is reasonable
				3.0, 9.0,		// Average word length is ~5, so choose a reasonable lower and upper bound
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				140,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				", /();:.!?",	// Word Break characters
				",\"'-();:.!?",	// Punctuation character
				new String[] {
						"aa", "ab", "áb", "ac", "ác", "ad", "ae", "aé", "af", "áf", "ag", "ág", "ah", "ai", "aj", "al", "ál", "am", "an", "añ", "án", "ap", "aq", "ar", "ár", "as", "at", "át", "au", "aú", "av", "ax", "ay", "az",
						"ba", "bá", "be", "bé", "bi", "bl", "bo", "bó", "br", "bu", "bú", "by",
						"ca", "cá", "cd", "ce", "cé", "ch", "ci", "cí", "cj", "cl", "cm", "co", "có", "cr", "cu", "cú", "cy",
						"da", "dá", "dc", "de", "dé", "di", "dí", "dj", "do", "dó", "dr", "du", "dw", "dy",
						"ea", "eb", "ec", "ed", "ee", "ef", "eg", "eh", "ei", "éi", "ej", "el", "él", "em", "en", "ep", "ép", "eq", "er", "ér", "es", "és", "et", "ét", "eu", "ev", "ex", "éx", "ey",
						"fa", "fá", "fb", "fe", "fé", "fi", "fí", "fl", "fo", "fó", "fr", "fu", "fú", "fü",
						"ga", "ge", "gé", "gi", "gl", "go", "gr", "gu", "gw", "gy",
						"ha", "há", "he", "hé", "hi", "hí", "hm", "ho", "hu",
						"ia", "ib", "íb", "id", "íd", "ie", "ig", "ii", "il", "im", "in", "io", "ir", "is", "it", "iu", "iv", "iz",
						"ja", "je", "ji", "jo", "jó", "jr", "ju",
						"ka", "ke", "kg", "ki", "kl", "km", "kn", "ko", "kr", "ku", "ky",
						"la", "lá", "le", "li", "lí", "ll", "lo", "ló", "lr", "lu", "ly",
						"ma", "má", "mc", "me", "mé", "mg", "mi", "mí", "mj", "mm", "mo", "mó", "mr", "mu", "mú", "my",
						"na", "ná", "ne", "ni", "no", "ns", "nu", "nú",
						"ob", "oc", "od", "oe", "of", "og", "oh", "oi", "oí", "oj", "ok", "ol", "om", "on", "oo", "op", "óp", "or", "ór", "os", "ot", "ov", "ow", "ox", "oy", "oz",
						"pa", "pá", "pe", "pé", "ph", "pi", "pí", "pl", "pm", "po", "pó", "pr", "ps", "pu", "pú",
						"qa", "qi", "qu",
						"ra", "rá", "re", "ré", "ri", "rí", "rj", "ro", "ru", "ry",
						"sa", "sá", "sc", "sd", "se", "sé", "sg", "sh", "si", "sí", "sk", "sl", "sm", "sn", "so", "só", "sp", "sr", "ss", "st", "su", "sú", "sw", "sy",
						"ta", "tá", "te", "té", "th", "ti", "tí", "tj", "to", "tó", "tr", "tt", "tu", "tú", "tv", "ty",
						"ub", "ud", "ue", "uh", "úi", "ul", "úl", "um", "un", "uñ", "ún", "ur", "us", "ut", "út", "uu",
						"va", "vá", "vd", "ve", "vé", "vi", "ví", "vo", "vu",
						"wa", "we", "wh", "wi", "wo", "wy",
						"xe", "xi",
						"ya", "ye", "yo", "yu", "yy",
						"za", "ze", "zé", "zh", "zo", "zu"
				}
				));
		// Finnish configuration
		allConfigData.put("FI", new TextConfig(
				26,				// Maximum word length - choose something that is reasonable
				3.0, 12.0,		// Finnish is an agglutinating language (like German)
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				140,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				", /();:.!?",	// Word Break characters
				",\"'-();:.!?",	// Punctuation character
				new String[] {
						"aa", "ää", "ab", "af", "ag", "ah", "ai", "äi", "aj", "ak", "äk", "al", "äl", "am", "an", "ap", "ar", "as", "at", "au", "av",
						"ba", "be", "bi", "bo", "br", "bu",
						"co", "cr",
						"de", "di", "do", "dr", "dy",
						"ed", "eh", "ei", "ek", "el", "en", "ep", "er", "es", "et", "eu", "ev",
						"fa", "fi", "fo", "fy",
						"ga", "ge", "go", "gr",
						"ha", "hä", "he", "hi", "ho", "hö", "hu", "hy",
						"id", "ih", "ik", "il", "im", "in", "ip", "ir", "is", "it",
						"ja", "jä", "jo", "ju",
						"ka", "kä", "ke", "ki", "kl", "ko", "kö", "kr", "ku", "ky",
						"la", "lä", "le", "li", "lo", "lö", "lu", "ly",
						"ma", "mä", "me", "mi", "mm", "mo", "mu", "my",
						"na", "nä", "ne", "ni", "no", "nu", "ny",
						"od", "oh", "oi", "ok", "ol", "om", "on", "op", "or", "os", "ot", "ou", "ov",
						"pa", "pä", "pe", "pi", "pl", "po", "pö", "pr", "ps", "pu", "py",
						"ra", "rä", "re", "ri", "ro", "ru", "ry",
						"sa", "sä", "se", "sh", "si", "sk", "so", "sp", "st", "su", "sy",
						"ta", "tä", "te", "ti", "to", "tr", "tu", "ty",
						"uh", "ui", "ul", "un", "up", "ur", "us", "ut", "uu",
						"va", "vä", "ve", "vi", "vo", "vu", "vy",
						"yd", "yh", "yk", "yl", "ym", "yr", "ys",
				}
				));
		// French configuration
		allConfigData.put("FR", new TextConfig(
				26,				// Maximum word length - choose something that is reasonable
				3.0, 9.0,		// Average word length is ~5, so choose a reasonable lower and upper bound
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				140,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				",' /();:.!?",	// Word Break characters
				",\"-();:.!?",	// Punctuation character
				new String[] {
						"ab", "ac", "ad", "af", "ag", "âg", "ai", "aj", "al", "am", "an", "ap", "ar", "as", "at", "au", "av",
						"ba", "bâ", "be", "bé", "bi", "bl", "bo", "br", "bu",
						"ca", "ce", "cé", "ch", "ci", "cl", "co", "cô", "cr", "cu",
						"da", "de", "dé", "di", "do", "dr", "du", "dû", "dy",
						"éb", "éc", "éd", "ef", "ég", "el", "él", "em", "ém", "en", "én", "ép", "éq", "er", "es", "et", "ét", "êt", "eu", "év",
						"ex", "fa", "fe", "fé", "fê", "fi", "fl", "fo", "fr", "fu",
						"ga", "gâ", "ge", "gé", "gi", "gl", "go", "gr", "gu",
						"ha", "he", "hé", "hi", "ho", "hô", "hu", "hy",
						"ic", "id", "ig", "il", "im", "in", "ip", "ir", "is",
						"ja", "je", "jo", "ju",
						"la", "là", "le", "lé", "li", "lo", "lu",
						"ma", "mâ", "me", "mé", "mè", "mê", "mi", "mm", "mo", "mu", "my",
						"na", "ne", "né", "ni", "no", "nu",
						"ob", "oc", "of", "oi", "ol", "om", "on", "op", "or", "os", "ou", "où",
						"pa", "pâ", "pe", "pé", "ph", "pi", "pl", "po", "pr", "ps", "pu",
						"qu",
						"ra", "râ", "re", "ré", "rê", "ri", "ro", "ru", "ry",
						"sa", "sc", "se", "sé", "si", "so", "sp", "st", "su", "sy",
						"ta", "te", "té", "th", "ti", "to", "tô", "tr", "tu", "ty",
						"un", "ur", "us", "ut",
						"va", "ve", "vé", "vê", "vi", "vo", "vr", "vu",
				}
				));
		// Irish (Gaelic) configuration
		allConfigData.put("GA", new TextConfig(
				26,				// Maximum word length - choose something that is reasonable
				3.0, 9.0,		// Average word length is ~5, so choose a reasonable lower and upper bound
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				140,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				",' /();:.!?",	// Word Break characters
				",\"-();:.!?",	// Punctuation character
				new String[] {
						"ab", "áb", "ac", "ad", "ae", "af", "ag", "ai", "ái", "al", "am", "an", "ao", "ar", "ár", "as", "at",
						"ba", "bá", "be", "bé", "bh", "bi", "bí", "bl", "bo", "bó", "br", "bu",
						"ca", "cá", "ce", "cé", "ch", "ci", "cí", "cl", "cn", "co", "có", "cr", "cu", "cú",
						"da", "dá", "de", "dé", "dh", "di", "dí", "dl", "do", "dó", "dr", "dt", "du", "dú",
						"ea", "éa", "ei", "éi", "eo", "et",
						"fa", "fá", "fe", "fé", "fh", "fi", "fí", "fl", "fo", "fó", "fr", "fu",
						"ga", "gá", "gc", "ge", "gé", "gh", "gi", "gl", "gn", "go", "gr", "gu", "gú",
						"ha", "hi", "ho", "hu",
						"ia", "id", "il", "im", "in", "io", "ío", "ip", "ir", "is", "it",
						"ja", "je",
						"ki",
						"la", "lá", "le", "lé", "li", "lí", "lo", "lu", "lú",
						"ma", "má", "mb", "me", "mé", "mh", "mi", "mí", "mm", "mo", "mó", "mu", "mú", "my",
						"na", "ná", "nd", "ne", "né", "ni", "ní", "no", "nó", "nu",
						"ob", "oc", "óc", "óg", "oi", "oí", "ói", "ol", "ón", "or", "os", "ós", "ot",
						"pa", "pá", "pe", "pé", "ph", "pi", "pí", "pl", "po", "pó", "pr", "pu", "pú",
						"ra", "rá", "re", "ré", "ri", "rí", "ro", "ró", "ru", "rú",
						"sa", "sá", "sc", "se", "sé", "sh", "si", "sí", "sl", "sm", "sn", "so", "só", "sp", "sr", "st", "su", "sú", "sw", "sy",
						"ta", "tá", "te", "té", "th", "ti", "tí", "to", "tó", "tr", "tu", "tú",
						"ua", "úd", "uh", "ui", "uí", "úi", "ul", "um", "un", "ur", "úr", "ús",
						"va", "ve", "vi", "vó",
						"wh", "wi", "wr",
						"ye",
				}
				));
		// Croatian configuration
		allConfigData.put("HR", new TextConfig(
				26,				// Maximum word length - choose something that is reasonable
				3.0, 9.0,		// Average word length is ~5, so choose a reasonable lower and upper bound
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				140,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				",' /();:.!?",	// Word Break characters
				",\"-();:.!?",	// Punctuation character
				new String[] {
						"ad", "af", "ag", "ak", "al", "am", "an", "ap", "ar", "as", "at", "au", "av", "až",
						"ba", "be", "bi", "bl", "bo", "br", "bu",
						"ca", "ce", "ci", "cj", "co", "cr", "cv",
						"da", "de", "di", "dj", "dl", "dn", "do", "dr", "du", "dv", "dž",
						"ek", "el", "em", "en", "es", "et", "eu", "ev",
						"fa", "fe", "fi", "fl", "fo", "fr", "fu",
						"ga", "gd", "ge", "gl", "gn", "go", "gr", "gu",
						"ha", "he", "hi", "hl", "hm", "ho", "hr", "ht", "hv",
						"ia", "id", "ig", "ik", "il", "im", "in", "ip", "ir", "is", "it", "iz",
						"ja", "je", "jo", "ju",
						"ka", "ke", "ki", "kl", "kn", "ko", "kr", "ku", "kv",
						"la", "le", "li", "lj", "lo", "lu",
						"ma", "me", "mi", "mj", "ml", "mm", "mn", "mo", "mr", "mu",
						"na", "ne", "ni", "nj", "no", "nu",
						"ob", "oc", "od", "og", "oh", "ok", "ol", "om", "on", "op", "or", "os", "ot", "ov", "oz", "oč", "oš", "ož",
						"pa", "pe", "pi", "pj", "pl", "po", "pr", "ps", "pu",
						"ra", "re", "ri", "ro", "ru",
						"sa", "sc", "se", "sh", "si", "sj", "sk", "sl", "sm", "sn", "so", "sp", "sr", "st", "su", "sv",
						"ta", "te", "ti", "tj", "tk", "tl", "to", "tr", "tu", "tv",
						"ub", "ud", "ug", "uh", "uj", "uk", "ul", "um", "un", "uo", "up", "ur", "us", "ut", "uv", "uz", "uč", "už",
						"va", "ve", "vi", "vj", "vl", "vo", "vr", "vu",
						"za", "zb", "zd", "ze", "zg", "zi", "zl", "zm", "zn", "zo", "zr", "zu", "zv",
						"ča", "ša", "ža", "če", "še", "že", "či", "ši", "ži", "šk", "čl", "čo", "šo", "šp", "žr", "št", "ču", "šu", "žu", "čv",
				}
				));
		// Hungarian configuration
		allConfigData.put("HU", new TextConfig(
				26,				// Maximum word length - choose something that is reasonable
				3.0, 12.0,		// Hungarian is an agglutinating language (like German)
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				140,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				",' /();:.!?",	// Word Break characters
				",\"-();:.!?",	// Punctuation character
				new String[] {
						"ab", "áb", "ad", "ag", "ág", "ah", "aj", "ak", "al", "ál", "am", "an", "ap", "ar", "ár", "as", "át", "au", "az",
						"ba", "bá", "be", "bé", "bi", "bí", "bl", "bo", "bó", "bö", "bu", "bü", "bő", "bű",
						"cé", "ci", "cí", "cs", "cu",
						"da", "dá", "de", "dé", "di", "dí", "do", "dö", "dr", "du", "dz",
						"éb", "ec", "ed", "éd", "eg", "ég", "el", "él", "em", "en", "én", "ép", "er", "ér", "es", "és", "et", "ét", "ev", "év", "ez",
						"fa", "fá", "fe", "fé", "fi", "fo", "fó", "fö", "fr", "fu", "fü", "fő",
						"ga", "ge", "gé", "go", "gö", "gr", "gy",
						"ha", "há", "he", "hé", "hi", "hí", "ho", "hu", "hú", "hü", "hő",
						"id", "ig", "íg", "il", "im", "in", "ip", "ir", "ír", "is", "it", "iz",
						"ja", "já", "je", "jo", "jó", "jö", "ju",
						"ka", "ká", "ke", "ké", "ki", "kí", "kl", "ko", "kö", "kr", "ku", "kü",
						"la", "lá", "le", "lé", "li", "lo", "lö",
						"ma", "má", "me", "mé", "mi", "mí", "mm", "mo", "mó", "mö", "mu", "mú", "mű",
						"na", "ne", "né", "no", "nö", "ny", "nő",
						"ok", "ol", "öl", "ön", "op", "or", "ór", "ör", "os", "ös", "öt", "öv",
						"pa", "pá", "pe", "pé", "pi", "pl", "po", "pó", "pr", "ps", "pu",
						"ra", "rá", "re", "ré", "ri", "ro", "ró", "rö", "ru", "rú",
						"sa", "sá", "se", "sé", "si", "sí", "sk", "so", "sö", "sp", "st", "su", "sz",
						"ta", "tá", "te", "té", "ti", "to", "tö", "tr", "tu", "tú", "tü", "tű",
						"üd", "ug", "úg", "üg", "új", "ül", "un", "ün", "ur", "ut", "út", "üt", "üv", "üz",
						"va", "vá", "ve", "vé", "vi", "vo",
						"we",
						"za", "zá", "ze", "zs",
						"őr", "ős",
				}
				));
		// Italian configuration
		allConfigData.put("IT", new TextConfig(
				26,				// Maximum word length - choose something that is reasonable
				3.0, 9.0,		// Average word length is ~5, so choose a reasonable lower and upper bound
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				140,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				", /();:.!?",	// Word Break characters
				",\"'-();:.!?",	// Punctuation character
				new String[] {
						"aa", "ab", "ac", "ad", "ae", "af", "ag", "ah", "ai", "al", "am", "an", "ap", "aq", "ar", "as", "at", "au", "av", "ax", "az",
						"ba", "be", "bè", "bi", "bl", "bo", "br", "bu", "by",
						"ca", "cd", "ce", "ch", "ci", "cl", "cm", "co", "cr", "cu", "cy",
						"da", "dà", "de", "di", "dí", "dì", "dj", "dl", "dn", "do", "dr", "du", "dv", "dw", "dy",
						"ea", "eb", "ec", "ed", "ef", "eg", "eh", "ei", "el", "em", "en", "ep", "eq", "er", "es", "et", "eu", "ev", "ex", "ez",
						"fa", "fb", "fc", "fe", "fi", "fl", "fo", "fr", "fu",
						"ga", "ge", "gh", "gi", "gl", "go", "gp", "gr", "gu", "gw",
						"ha", "he", "hi", "hm", "ho", "hu",
						"ia", "ic", "id", "ie", "ig", "ii", "il", "íl", "im", "in", "în", "io", "ip", "ir", "is", "it", "iv", "iz",
						"ja", "je", "ji", "jj", "jo", "ju",
						"ka", "ke", "kg", "kh", "ki", "kl", "km", "kn", "ko", "ku", "ky",
						"la", "là", "le", "li", "lì", "ll", "ln", "lo", "lu", "ly",
						"ma", "mc", "me", "mh", "mi", "mm", "mo", "mr", "mu", "my",
						"na", "nc", "nd", "ne", "né", "nè", "ni", "no", "ns", "nu",
						"ob", "oc", "od", "of", "og", "oh", "ok", "ol", "om", "on", "oo", "op", "or", "os", "ot", "ou", "ov", "ow", "ox", "oz",
						"pa", "pe", "ph", "pi", "pl", "po", "pò", "pr", "ps", "pu",
						"qu",
						"ra", "re", "ri", "ro", "ru", "ry",
						"sa", "sb", "sc", "se", "sé", "sè", "sf", "sg", "sh", "si", "sí", "sì", "sk", "sl", "sm", "so", "sp", "sq", "sr", "st", "su", "sv", "sy",
						"ta", "te", "tè", "th", "ti", "to", "tr", "tu", "tv", "tw", "ty",
						"ub", "uc", "ud", "uf", "ug", "uh", "ul", "um", "un", "uo", "up", "ur", "us", "ut", "uv",
						"va", "ve", "vi", "vo", "vu",
						"wa", "we", "wh", "wi", "wo", "wu", "wy",
						"xe",
						"ya", "ye", "yi", "yo", "yu",
						"za", "ze", "zi", "zo", "zu" }
				));
		// Latvian configuration
		allConfigData.put("LV", new TextConfig(
				26,				// Maximum word length - choose something that is reasonable
				3.0, 11.0,		// Average word length is ~8, so choose a reasonable lower and upper bound
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				140,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				", /();:.!?",	// Word Break characters
				",\"'-();:.!?",	// Punctuation character
				new String[] {
						"ab", "ac", "ad", "af", "ag", "ai", "ak", "al", "am", "an", "ap", "ar", "as", "at", "au", "av", "aģ",
						"ba", "be", "bi", "bl", "bo", "br", "bu", "bā", "bē", "bī", "bū",
						"ca", "ce", "ci", "cu", "cē", "cī", "cū", "da",
						"de", "di", "do", "dr", "du", "dv", "dz", "dā", "dē", "dī", "dū", "dž",
						"ef", "ei", "ek", "el", "em", "en", "es", "et", "ev",
						"fa", "fe", "fi", "fl", "fo", "fr", "fu",
						"ga", "gl", "go", "gr", "gu",
						"ha", "hi", "hm", "ho", "hr", "hu",
						"id", "ie", "ik", "il", "im", "in", "ip", "ir", "is", "it", "iz",
						"ja", "je", "jo", "ju", "jā", "jē", "jū",
						"ka", "kl", "ko", "kr", "ku", "kv", "kā", "kļ", "kū",
						"la", "le", "li", "lo", "lā", "lē", "lī", "lū",
						"ma", "me", "mi", "mm", "mo", "mu", "mā", "mē", "mī", "mū",
						"na", "ne", "no", "nu", "nā", "nē",
						"ob", "og", "ok", "ol", "op", "or", "os", "ot",
						"pa", "pe", "pi", "pl", "po", "pr", "ps", "pu", "pā", "pē", "pī", "pū",
						"ra", "re", "ri", "ro", "ru", "rā", "rī", "rū",
						"sa", "se", "si", "sk", "sl", "sm", "sn", "so", "sp", "st", "su", "sv", "sā", "sē", "sī", "sū",
						"ta", "te", "ti", "to", "tr", "tu", "tv", "tā", "tē", "tī", "tū",
						"ug", "uh", "un", "up", "uz",
						"va", "ve", "vi", "vā", "vē", "vī",
						"za", "ze", "zi", "zo", "zv", "zā", "zī",
						"ča", "ļa", "ša", "ād", "ēd", "ēs", "če", "ģe", "ķe", "ņe", "še", "ģi", "ķi", "ši", "ēn", "ļo", "šo",
						"žo", "īp", "ār", "ēr", "īr", "īs", "āt", "ēt", "žu", "āķ", "ķē", "ķī", "šķ", "šū", "žē",
				}
				));
		// Dutch configuration
		allConfigData.put("NL", new TextConfig(
				26,				// Maximum word length - choose something that is reasonable
				3.0, 9.0,		// Average word length is ~5, so choose a reasonable lower and upper bound
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				140,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				", /();:.!?",	// Word Break characters
				",\"'-();:.!?",	// Punctuation character
				new String[] {
						"aa", "ab", "ac", "ad", "ae", "af", "ag", "ah", "ai", "ak", "al", "am", "an", "ap", "ar", "as", "at", "au", "av", "ay", "az",
						"ba", "be", "bh", "bi", "bl", "bo", "br", "bu", "by",
						"ca", "cd", "ce", "ch", "ci", "cl", "cm", "co", "cr", "cs", "ct", "cu", "cv", "cy",
						"da", "dc", "de", "dé", "di", "dj", "dn", "do", "dr", "du", "dv", "dw", "dy",
						"ea", "ec", "éc", "ed", "ee", "eé", "éé", "èè", "ef", "eg", "eh", "ei", "el", "em", "en", "én", "er", "es", "et", "eu", "ev", "ex", "ez",
						"fa", "fb", "fe", "fi", "fl", "fo", "fr", "fu", "fü", "fy",
						"ga", "ge", "gê", "gh", "gi", "gl", "go", "gp", "gr", "gs", "gu", "gw", "gy",
						"ha", "he", "hé", "hè", "hi", "hm", "ho", "hu", "hy",
						"ia", "ic", "id", "ie", "if", "ii", "ij", "ik", "il", "im", "in", "io", "ir", "is", "it", "iv",
						"ja", "je", "ji", "jj", "jo", "jr", "ju",
						"ka", "ke", "kh", "ki", "kl", "km", "kn", "ko", "kr", "ku", "kw", "ky",
						"la", "le", "li", "lk", "ll", "ln", "lo", "ls", "lu", "ly",
						"ma", "mc", "me", "mi", "mm", "mo", "mr", "ms", "mu", "my",
						"na", "nc", "ne", "ni", "no", "ns", "nu", "ny",
						"ob", "oc", "oe", "of", "og", "oh", "ok", "ol", "om", "on", "oo", "op", "or", "os", "ot", "ou", "ov", "ow", "ox", "oz",
						"pa", "pc", "pe", "ph", "pi", "pl", "po", "pr", "ps", "pu", "py",
						"qo", "qu", "ra",
						"re", "ri", "ro", "rö", "ru", "ry",
						"sa", "sc", "se", "sf", "sh", "si", "sj", "sk", "sl", "sm", "sn", "so", "sp", "sq", "ss", "st", "su", "sw", "sy",
						"ta", "te", "th", "ti", "tj", "to", "tr", "tu", "tv", "tw", "ty",
						"üb", "uf", "uh", "ui", "ul", "um", "un", "up", "ur", "us", "uu", "uw", "uz",
						"va", "ve", "vi", "vl", "vn", "vo", "vó", "vr", "vs", "vu",
						"wa", "wc", "we", "wh", "wi", "wo", "wr", "wu", "wy",
						"xe",
						"ya", "ye", "yo",
						"za", "ze", "zi", "zo", "zó", "zu", "zw"
				}
				));
		// Portuguese configuration
		allConfigData.put("PT", new TextConfig(
				26,				// Maximum word length - choose something that is reasonable
				3.0, 9.0,		// Average word length is ~5, so choose a reasonable lower and upper bound
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				140,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				", /();:.!?",	// Word Break characters
				",\"'-();:.!?",	// Punctuation character
				new String[] {
						"aa", "ab", "ac", "aç", "ác", "ad", "ae", "aé", "af", "áf", "ag", "ág", "ah", "ai", "aí", "aj", "al", "ál", "am", "an", "ân", "ao", "ap", "aq", "àq", "ar", "ár", "as", "ás", "às", "at", "au", "áu", "av", "aw", "az",
						"ba", "bá", "be", "bê", "bi", "bí", "bl", "bo", "bô", "br", "bu", "by",
						"ca", "cá", "câ", "cã", "cd", "ce", "cé", "ch", "ci", "cí", "cl", "cm", "co", "có", "cr", "cs", "cu", "cú", "cy",
						"da", "dá", "dã", "dc", "de", "dé", "dê", "di", "dí", "dj", "dn", "do", "dó", "dr", "du", "dú", "dv", "dw", "dy",
						"ea", "ec", "ed", "ef", "eg", "eh", "ei", "el", "em", "en", "eo", "ep", "ép", "eq", "er", "ér", "es", "és", "et", "ét", "eu", "ev", "ex", "êx",
						"fa", "fá", "fã", "fb", "fe", "fé", "fê", "fi", "fí", "fl", "fo", "fó", "fô", "fr", "fu", "fú", "fü",
						"ga", "gá", "ge", "gê", "gi", "gl", "go", "gp", "gr", "gu", "gw",
						"ha", "há", "hã", "hd", "he", "hé", "hi", "hm", "ho", "hó", "hu",
						"ia", "iá", "ía", "id", "íd", "if", "ig", "ih", "ii", "ik", "il", "im", "in", "ín", "io", "ir", "is", "it", "iv",
						"ja", "já", "je", "ji", "jj", "jo", "jó", "jr", "ju", "jú",
						"ka", "ke", "kg", "kh", "ki", "kl", "km", "kn", "ko", "kr", "ku", "ky",
						"la", "lá", "lâ", "lã", "le", "lé", "lê", "lh", "li", "lí", "ll", "lo", "ló", "ls", "lu", "ly",
						"ma", "má", "mã", "mc", "me", "mé", "mê", "mi", "mí", "mm", "mo", "mó", "mr", "mu", "mú", "my",
						"na", "nâ", "nä", "nã", "nc", "ne", "né", "ni", "ní", "no", "nó", "nu", "nú", "ny",
						"ob", "ób", "oc", "óc", "od", "ód", "oe", "of", "oh", "oi", "ok", "ol", "ól", "om", "on", "ôn", "oo", "op", "óp", "oq", "or", "ór", "os", "ot", "ót", "ou", "ov", "ow", "ox", "oz",
						"pa", "pá", "pâ", "pã", "pe", "pé", "pê", "ph", "pi", "pí", "pl", "pn", "po", "pó", "pô", "põ", "pr", "ps", "pu", "pú",
						"qu",
						"ra", "rá", "re", "ré", "ri", "rí", "ro", "ru", "rú", "ry",
						"sa", "sá", "sã", "sc", "se", "sé", "sh", "si", "sí", "sk", "sl", "sm", "so", "só", "sp", "sq", "sr", "ss", "st", "su", "sy",
						"ta", "tá", "tã", "tc", "te", "té", "tê", "th", "ti", "tí", "to", "tó", "tô", "tr", "tu", "tú", "tv", "tw", "ty",
						"ua", "ug", "uh", "uí", "ul", "úl", "um", "un", "ún", "up", "ur", "us", "ut", "út",
						"va", "vá", "vã", "vc", "ve", "vé", "vê", "vi", "ví", "vo", "vó", "vô", "vu",
						"wa", "we", "wh", "wi", "wo", "wu", "wy",
						"xa", "xe", "xi", "xí",
						"ya", "ye", "yo",
						"za", "ze", "zo", "zu"
				}
				));
		// Romanian configuration
		allConfigData.put("RO", new TextConfig(
				26,				// Maximum word length - choose something that is reasonable
				3.0, 9.0,		// Average word length is ~5, so choose a reasonable lower and upper bound
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				140,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				", /();:.!?",	// Word Break characters
				",\"'-();:.!?",	// Punctuation character
				new String[] {
						"ab", "ac", "ad", "ae", "af", "ag", "aj", "al", "am", "an", "ap", "ar", "as", "at", "au", "av", "aş", "aș",
						"ba", "be", "bi", "bl", "bo", "br", "bu", "bă",
						"ca", "câ", "ce", "ch", "ci", "cl", "co", "cr", "cu", "că",
						"da", "de", "di", "do", "dr", "du", "dă",
						"ec", "ed", "ef", "eg", "el", "em", "en", "ep", "er", "es", "et", "eu", "ev", "ex", "eș",
						"fa", "fe", "fi", "fl", "fo", "fr", "fu", "fă",
						"ga", "gâ", "ge", "gh", "gi", "gl", "go", "gr", "gu", "gă",
						"ha", "ho",
						"ia", "id", "ie", "ig", "il", "im", "îm", "in", "în", "ip", "ir", "is", "iu", "iz",
						"ja", "je", "jo", "ju", "la",
						"lâ", "le", "li", "lo", "lu", "lă",
						"ma", "mâ", "me", "mi", "mm", "mo", "mu", "mă",
						"na", "ne", "ni", "no", "nu",
						"oa", "ob", "oc", "od", "of", "ol", "om", "on", "op", "or", "os",
						"pa", "pâ", "pe", "pi", "pl", "po", "pr", "ps", "pu", "pă",
						"ra", "râ", "re", "ri", "ro", "ru", "ră",
						"sa", "sâ", "sc", "se", "sf", "si", "sl", "sn", "so", "sp", "st", "su", "să",
						"ta", "tâ", "te", "ti", "to", "tr", "tu", "tă",
						"uc", "uh", "ui", "ul", "um", "un", "ur", "us", "ut", "uş", "uș",
						"va", "vâ", "ve", "vi", "vo", "vr", "vu", "vă",
						"zâ", "zb", "ze", "zg", "zi", "zo",
						"şa", "şe", "ţi", "și", "şo", "șt",
				}
				));
		// Russian configuration
		allConfigData.put("RU", new TextConfig(
				26,				// Maximum word length - choose something that is reasonable
				3.0, 9.0,		// Average word length is ~5, so choose a reasonable lower and upper bound
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				140,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				", /();:.!?",	// Word Break characters
				",\"'-();:.!?",	// Punctuation character
				new String[] {
						"аа", "ав", "аг", "ад", "ак", "ал", "ам", "ан", "ап", "ар", "ат", "аэ",
						"ба", "бе", "би", "бл", "бо", "бр", "бу", "бы",
						"ва", "вв", "вд", "ве", "вз", "ви", "вк", "вл", "вм", "вн", "во", "вп", "вр", "вс", "вт", "вх", "вы",
						"га", "ге", "гл", "гн", "го", "гр", "гу",
						"да", "дв", "де", "дж", "ди", "дл", "дн", "до", "др", "ду", "ды", "дэ", "дя",
						"ев", "ед", "ез", "ел", "ес", "ем", "ех", "ещ",
						"жа", "жд", "же", "жи", "жу",
						"за", "зв", "зд", "зе", "зл", "зн", "зо", "зр", "зу",
						"иг", "ид", "из", "ии", "ил", "им", "ин", "ис", "ит", "ищ",
						"йо",
						"ка", "кв", "ке", "ки", "кл", "кн", "ко", "кр", "ку", "кэ",
						"ла", "лг", "ле", "лж", "ли", "ло", "лу", "ль", "лю",
						"ма", "ме", "ми", "мн", "мо", "му", "мы", "мэ", "мя",
						"на", "не", "ни", "но", "нр", "ну",
						"об", "ог", "од", "ож", "оз", "ок", "ол", "он", "оо", "оп", "ор", "ос", "от", "оф", "ох", "оч", "ош", "он",
						"па", "пе", "пи", "пл", "по", "пр", "пс", "пт", "пу", "пы", "пь", "пя",
						"ра", "ре", "ри", "ро", "ру", "ры", "рэ", "ря", "рж",
						"са", "сб", "св", "сд", "се", "си", "ск", "сл", "см", "сн", "со", "сп", "ср", "ст", "су", "сх", "сц", "сч", "съ", "сы", "сь", "сэ", "сю", "ся",
						"та", "тв", "те", "ти", "то", "тр", "ту", "ты", "ть", "тэ", "тю", "тя",
						"уб", "ув", "уг", "уд", "уе", "уж", "уз", "уи", "уй", "ук", "ул", "ум", "ун", "уо", "уп", "ур", "ус", "ут", "ух", "уч", "уш",
						"фа", "фе", "фи", "фл", "фо", "фр", "фу",
						"ха", "хв", "хе", "хи", "хл", "хо", "хр", "ху", "хэ",
						"цв", "це", "ци",
						"ча", "че", "чж", "чи", "чл", "чо", "чт", "чу", "чь", "чё",
						"ша", "ше", "ши", "шк", "шл", "шо", "шт", "шу",
						"эд", "эй", "эк", "эл", "эм", "эн", "эр", "эт", "эф",
						"юн",
						"яв", "яз", "ян",
						"ќн",
					}
				));
		// Slovakian configuration
		allConfigData.put("SK", new TextConfig(
				26,				// Maximum word length - choose something that is reasonable
				3.0, 9.0,		// Average word length is ~5, so choose a reasonable lower and upper bound
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				140,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				", /();:.!?",	// Word Break characters
				",\"'-();:.!?",	// Punctuation character
				new String[] {
						"ab", "ad", "af", "ag", "ah", "ak", "al", "am", "an", "án", "ap", "ar", "as", "at", "au",
						"ba", "bá", "be", "bi", "bl", "bo", "br", "bu", "by", "bý",
						"ce", "ch", "ci", "cu",
						"da", "dá", "de", "di", "dl", "dn", "do", "dô", "dr", "du", "dv", "dy", "dž",
						"ef", "ek", "el", "em", "et", "ev", "ex",
						"fa", "fe", "fi", "fl", "fo", "fr", "fu", "fy",
						"ga", "ge", "go", "gr", "gu",
						"ha", "há", "he", "hi", "hl", "hm", "hn", "ho", "hr", "hu",
						"ib", "id", "ih", "il", "im", "in", "ir", "is", "iz",
						"ja", "je", "ju",
						"ka", "ká", "kd", "ke", "kl", "km", "ko", "kr", "kŕ", "kt", "ku", "kú", "kv", "ký",
						"la", "lá", "le", "li", "lí", "lo", "lú",
						"ma", "má", "mä", "me", "mi", "mí", "ml", "mm", "mo", "mô", "mr", "mu", "mú", "my",
						"na", "ná", "ne", "ni", "no",
						"ob", "oc", "od", "oh", "ok", "ol", "om", "on", "op", "or", "os", "ot", "ov", "oz", "oč",
						"pa", "pá", "pä", "pe", "pi", "pí", "pl", "po", "pô", "pr", "ps", "pu",
						"ra", "rá", "re", "ri", "ro", "rô", "ru", "ry", "rý",
						"sa", "sá", "sc", "se", "si", "sk", "sl", "sm", "sn", "so", "sp", "st", "su", "sú", "sv", "sy", "sľ",
						"ta", "tá", "te", "ti", "tí", "tl", "tm", "to", "tr", "tu", "tú", "tv", "ty", "tý",
						"ub", "uc", "ud", "úd", "uh", "ul", "uk", "um", "un", "up", "úp", "ur", "úr", "us", "ús", "ut", "út", "uv", "úv", "uz", "úz", "uč", "už", "úč", "úž",
						"va", "vá", "vä", "ve", "vi", "ví", "vl", "vn", "vo", "vp", "vr", "vs", "vt", "vy", "vý", "vz", "vď", "vš",
						"za", "zá", "zb", "zd", "ze", "zh", "zi", "zí", "zl", "zm", "zn", "zo", "zr", "zu", "zú", "zv", "zľ",
						"ča", "ďa", "ľa", "ša", "šá", "ťa", "če", "še", "že", "či", "čí", "ši", "ší", "ži", "šk", "čl", "čo", "šo", "šp", "št", "ču", "ľu", "šť",
					}
				));
		// Swedish configuration
		allConfigData.put("SV", new TextConfig(
				26,				// Maximum word length - choose something that is reasonable
				3.0, 9.0,		// Average word length is ~5, so choose a reasonable lower and upper bound
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				140,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				", /();:.!?",	// Word Break characters
				",\"'-();:.!?",	// Punctuation character
				new String[] {
						"ab", "ac", "ad", "af", "äg", "ak", "åk", "äk", "al", "ål", "äl", "am", "äm", "an", "ån", "än", "ap", "ar", "år", "är", "as", "at", "åt", "ät", "au", "av", "äv",
						"ba", "bå", "bä", "be", "bi", "bj", "bl", "bo", "bö", "br", "bu", "by",
						"ca", "ce", "ch", "ci", "cy",
						"da", "då", "dä", "de", "di", "dj", "do", "dö", "dr", "du", "dy",
						"ef", "eg", "eh", "ek", "el", "en", "ep", "er", "et", "eu", "ev", "ex",
						"fa", "få", "fä", "fe", "fi", "fj", "fl", "fo", "fö", "fr", "fu", "fy",
						"ga", "gå", "gä", "ge", "gi", "gl", "gn", "go", "gö", "gr", "gu", "gy",
						"ha", "hå", "hä", "he", "hi", "hj", "ho", "hö", "hu", "hy",
						"ib", "id", "if", "il", "im", "in", "ip", "ir", "is",
						"ja", "jä", "je", "jo", "ju",
						"ka", "kä", "ke", "ki", "kl", "kn", "ko", "kö", "kr", "ku", "kv", "ky",
						"la", "lå", "lä", "le", "li", "lj", "lo", "lö", "lu", "ly",
						"ma", "må", "mä", "me", "mi", "mj", "mm", "mo", "mö", "mu", "my",
						"na", "nå", "nä", "ne", "ni", "no", "nö", "nu", "ny",
						"ob", "oc", "od", "öd", "of", "ög", "oj", "ok", "ök", "ol", "om", "on", "ön", "op", "öp", "or", "ör", "os", "ös", "ot", "ov", "öv",
						"pa", "på", "pe", "pi", "pl", "po", "pr", "ps", "pu",
						"ra", "rå", "rä", "re", "ri", "ro", "rö", "ru", "ry",
						"sa", "så", "sä", "sc", "se", "si", "sj", "sk", "sl", "sm", "sn", "so", "sö", "sp", "st", "su", "sv", "sy",
						"ta", "tå", "tä", "te", "ti", "tj", "to", "tr", "tu", "tv", "ty",
						"un", "up", "ur", "ut",
						"va", "vå", "vä", "ve", "vi", "vo", "vr",
						"we",
						"yt",
					}
				));
		// Turkish configuration
		allConfigData.put("TR", new TextConfig(
				26,				// Maximum word length - choose something that is reasonable
				3.0, 11.0,		// Hungarian is an agglutinating language (like German)
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				140,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				", /();:.!?",	// Word Break characters
				",\"'-();:.!?",	// Punctuation character
				new String[] {
						"ab", "ac", "aç", "ad", "af", "ah", "ai", "aj", "ak", "al", "am", "an", "ap", "ar", "as", "at", "av", "ay", "az", "ağ", "aş",
						"ba", "be", "bi", "bo", "bö", "bu", "bü", "bı",
						"ca", "ça", "ce", "çe", "ci", "çi", "ço", "çö", "cu", "cü", "çu", "çü", "çı",
						"da", "de", "di", "do", "dö", "dr", "du", "dü", "dı",
						"eb", "ed", "ef", "eg", "ek", "el", "em", "en", "er", "es", "et", "ev", "ey", "ez", "eğ", "eş",
						"fa", "fe", "fi", "fl", "fo", "fr", "fu", "fı",
						"ga", "ge", "gi", "go", "gö", "gr", "gu", "gü",
						"ha", "hâ", "he", "hi", "ho", "hu", "hü", "hı",
						"ic", "iç", "id", "if", "ih", "ik", "il", "im", "in", "ip", "ir", "is", "it", "iy", "iz", "iş",
						"ja", "jü",
						"ka", "kâ", "ke", "ki", "kl", "ko", "kö", "kr", "ku", "kü", "kı",
						"la", "le", "li", "lo", "lü",
						"ma", "me", "mi", "mm", "mo", "mu", "mü",
						"na", "ne", "ni", "no", "nu", "nü",
						"od", "öd", "ok", "ol", "öl", "om", "on", "ön", "op", "öp", "or", "ör", "ot", "oy", "öy", "öz", "oğ", "öğ",
						"pa", "pe", "pi", "pl", "po", "pr", "ps", "pu",
						"ra", "re", "ri", "ro", "rö", "ru", "rü",
						"sa", "se", "si", "so", "sö", "sp", "st", "su", "sü", "sı",
						"ta", "te", "ti", "to", "tr", "tu", "tü", "tı",
						"uç", "üç", "üf", "ul", "um", "un", "ün", "ür", "us", "üs", "ut", "uy", "üy", "uz", "üz", "uğ",
						"va", "ve", "vi", "vu",
						"ya", "ye", "yi", "yo", "yö", "yu", "yü", "yı",
						"za", "ze", "zi", "zo", "zı",
						"şa", "şe", "şi", "şo", "ır", "ıs", "şu", "şü", "ış",
					}
				));
	}

	public TextProcessor(final Locale locale) {
		this.locale = locale;
		if (this.locale == null)
			this.locale = Locale.getDefault();
		this.config = allConfigData.get(this.locale.getLanguage().toUpperCase(Locale.ROOT));

		// If we don't support the requested locale - just default to English
		if (this.config == null)
			this.config = allConfigData.get("EN");
	}

	/**
	 * Process the newly found word.
	 * @param current The current TextResult
	 * @param wordState The WordState used to track our current state
	 * @param trimmed The source input to analyze
	 * @param idx The location of the cursor in the source input (trimmed)
	 */
	private void endOfWord(final TextResult current, final WordState wordState, final String trimmed, final int idx) {
		// Reject if it looks too long to be a word
		if (idx - lastOffset > config.getLongWord())
			current.determination = Determination.TOO_LONG;

		wordState.lastWordLength = idx - lastOffset;
		current.words++;
		wordsInSentence++;

		// If all the characters in the word are digits then count those digits
		if (wordState.digitsInWord == wordState.lastWordLength && wordState.lastWordLength <= 4)
			current.digits += wordState.digitsInWord;

		// If all the characters in the word are alphas then we have a 'real' word
		if (wordState.charsInWord == wordState.lastWordLength) {
			totalAlphaWordLength += wordState.lastWordLength;
			alphaWords++;
			if (wordState.lastWordLength > 2) {
				longWords++;
				if (config.getStarts().contains(trimmed.substring(lastOffset, lastOffset+2).toLowerCase(locale)))
					realWords++;
			}
		}

		wordState.digitsInWord = 0;
		wordState.charsInWord = 0;
	}

	/**
	 * Analyze the given input string to determine if it looks like free-form text in this locale.
	 * @param trimmed The input string
	 * @return A TextResult with both a determination of the input as well as a set of statistics.
	 */
	public TextResult analyze(final String trimmed) {
		final TextResult ret = new TextResult();
		final int len = trimmed.length();
		char lastCh = ' ';
		boolean wordStarted = false;
		final WordState wordState = new WordState();

		wordsInSentence = 0;
		lastOffset = 0;
		alphaWords = 0;
		realWords = 0;
		longWords = 0;
		totalAlphaWordLength = 0;

		int idx;
		for (idx = 0; idx < len && !(idx >= config.getMaxLength() && Character.isWhitespace(lastCh)); idx++) {
			final char ch = trimmed.charAt(idx);

			// Reached the end of a 'word'
			if (wordStarted && config.getWordBreak().indexOf(ch) != -1 && config.getWordBreak().indexOf(lastCh) == -1) {
				endOfWord(ret, wordState, trimmed, idx);
				if (ret.determination != Determination.OK)
					return ret;

				ret.wordBreaks++;
				lastOffset = idx;
				wordStarted = false;
			}

			if (config.getWordBreak().indexOf(lastCh) != -1)
				lastOffset = idx;

			if (config.getSentenceBreak().indexOf(ch) != -1) {
				// We don't like short sentences on the other hand e.g., i.e., et al., etc. should not be rejected
				if (wordsInSentence == 1 && wordState.lastWordLength > 3) {
					ret.determination = Determination.SENTENCE_TOO_SHORT;
					return ret;
				}
				wordsInSentence = 0;
			}

			if (Character.isWhitespace(ch))
				ret.spaces++;
			else if (Character.isAlphabetic(ch)) {
				ret.alphas++;
				if (Character.isLowerCase(ch))
					ret.alphaLower++;
				else
					ret.alphaUpper++;
				wordState.charsInWord++;
				wordStarted = true;
			}
			else if (Character.isDigit(ch)) {
				wordState.digitsInWord++;
				wordStarted = true;
			}
			else if (config.getPunctuation().indexOf(ch) != -1)
				ret.punctuation++;
			else if (config.getWordBreak().indexOf(ch) != -1 && config.getWordBreak().indexOf(lastCh) != -1)
				ret.wordBreaks++;
			lastCh = ch;
		}

		if (wordStarted) {
			endOfWord(ret, wordState, trimmed, idx);
			if (ret.determination != Determination.OK)
				return ret;
		}

		// Only one word so reject
		if (ret.words == 1) {
			ret.determination = Determination.SINGLE_WORD;
			return ret;
		}

		// Need some real words and a reasonable percentage of real words
		if (realWords == 0 || (longWords != 0 && (longWords - realWords)*100/longWords > 25)) {
			ret.determination = Determination.NOT_ENOUGH_REAL_WORDS;
			return ret;
		}

		// Count the alphas, wordBreaks, punctuation(,.) and any digits in digit only words
		if ((ret.alphas + ret.digits + ret.wordBreaks + ret.spaces + ret.punctuation)*100/idx < config.getSimplePercentage()) {
			ret.determination = Determination.PERCENT_TOO_LOW;
			return ret;
		}

		if ((ret.alphas + ret.spaces) * 100/idx < config.getAlphaSpacePercentage()) {
			ret.determination = Determination.PERCENT_TOO_LOW;
			return ret;
		}

		// Calculate the average word length
		final double avgWordLength = (double)totalAlphaWordLength/alphaWords;

		// Average length of words need to look reasonable for this language
		if (alphaWords > 3 && ((len > 10 && avgWordLength < config.getAverageLow()) || avgWordLength > config.getAverageHigh())) {
			ret.determination = Determination.BAD_AVERAGE_LENGTH;
			return ret;
		}

		return ret;
	}
}
