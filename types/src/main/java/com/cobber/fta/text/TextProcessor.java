/*
 * Copyright 2017-2022 Tim Segall
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
		// German configuration
		allConfigData.put("DE", new TextConfig(
				26,				// Maximum word length - choose something that is reasonable
				3.0, 10.0,		// Average word length is ~6.5, so choose a reasonable lower and upper bound
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				160,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				", /();:.!?",	// Word Break characters
				",\"'-();:.!?",	// Punctuation character
				new String[] {
						"ab", "ac", "ad", "af", "ag", "ai", "ak", "al", "am", "an", "ap", "ar", "as", "at", "au",
						"ba", "bä", "be", "bi", "bl", "bo", "bö", "br", "bu", "bü",
						"ca", "cd", "ce", "ch", "ci", "cl", "co", "cr",
						"da", "de", "di", "do", "dr", "du", "dü",
						"eb", "ec", "ed", "eh", "ei", "el", "em", "en", "er", "es", "et", "eu", "ev", "ex",
						"fa", "fä", "fe", "fi", "fl", "fo", "fö", "fr", "fu", "fü",
						"ga", "gä", "ge", "gi", "gl", "go", "gr", "gu", "gü",
						"ha", "hä", "he", "hi", "ho", "hö", "ht", "hu", "hü", "hy",
						"id", "im", "in", "ir", "is", "it",
						"ja", "je", "jo", "ju",
						"ka", "kä", "ke", "ki", "kl", "kn", "ko", "kö", "kr", "ku", "kü",
						"la", "lä", "le", "li", "lo", "lö", "lu",
						"ma", "mä", "me", "mi", "mo", "mö", "mu", "mü",
						"na", "nä", "ne", "ni", "no", "nu",
						"ob", "of", "oh", "ök", "ol", "öl", "on", "op", "or", "os",
						"pa", "pe", "pf", "ph", "pi", "pl", "po", "pr", "ps", "pu",
						"qu",
						"ra", "re", "rh", "ri", "ro", "rö", "ru", "rü",
						"sa", "sä", "sc", "se", "sh", "si", "sk", "so", "sp", "st", "su", "sü", "sy",
						"ta", "te", "th", "ti", "to", "tr", "ts", "tu", "tü",
						"üb", "ul", "um", "un", "ur", "us",
						"va", "ve", "vi", "vo",
						"wa", "wä", "we", "wi", "wo", "wu", "wü", "ww",
						"za", "ze", "zi", "zo", "zu", "zw"
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
		// French configuration
		allConfigData.put("FR", new TextConfig(
				26,				// Maximum word length - choose something that is reasonable
				3.0, 9.0,		// Average word length is ~5, so choose a reasonable lower and upper bound
				30,				// The percentage of 'alpha' characters that we expect to be present
				80,				// The percentage of 'reasonable' characters that we expect to be present
				140,			// Only analyze the first <n> characters
				".!?",			// Sentence Break characters
				", /();:.!?",	// Word Break characters
				",\"'-();:.!?",	// Punctuation character
				new String[] {
						"ab", "ac", "ad", "aé", "af", "ag", "ai", "aj", "al", "am", "an", "ap", "ar", "as", "at", "au", "av", "az",
						"ba", "bâ", "be", "bé", "bi", "bl", "bo", "br", "bu",
						"ca", "câ", "ce", "cé", "ch", "ci", "cl", "co", "cr", "cu", "cy",
						"da", "de", "dé", "di", "do", "dr", "du",
						"éb", "éc", "éd", "ef", "ég", "él", "em", "ém", "en", "én", "ép", "éq", "er", "ér", "es", "ét", "eu", "év", "ex",
						"fa", "fe", "fé", "fi", "fl", "fo", "fr", "fu",
						"ga", "ge", "gé", "gi", "gl", "go", "gr", "gu",
						"ha", "he", "hé", "hi", "ho", "hô", "hu", "hy",
						"id", "ig", "il", "im", "in", "ir", "is",
						"ja", "je", "jo", "ju",
						"ka",
						"la", "le", "lé", "li", "lo", "lu", "ly",
						"ma", "me", "mé", "mi", "mo", "mu", "my",
						"na", "ne", "né", "ni", "no", "nu",
						"ob", "oc", "of", "om", "on", "op", "or", "os", "ou",
						"pa", "pâ", "pe", "pé", "ph", "pi", "pl", "po", "pr", "ps", "pu",
						"qu",
						"ra", "re", "ré", "rê", "ri", "ro", "ru",
						"sa", "sc", "se", "sé", "sh", "si", "so", "sp", "st", "su", "sy",
						"ta", "tc", "te", "té", "th", "ti", "to", "tr", "tu", "ty",
						"ul", "un", "ur", "us", "ut",
						"va", "ve", "vé", "vi", "vo", "vr", "vu",
						"wa",
						"ya"
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
						"ев", "ед", "ез", "ел", "ес", "ех", "ещ",
						"жа", "жд", "же", "жи", "жу",
						"за", "зв", "зд", "зе", "зл", "зн", "зо", "зр", "зу",
						"иг", "ид", "из", "ии", "ил", "им", "ин", "ис", "ит", "ищ",
						"йо",
						"ка", "кв", "ке", "ки", "кл", "кн", "ко", "кр", "ку", "кэ",
						"ла", "лг", "ле", "лж", "ли", "ло", "лу", "ль", "лю",
						"ма", "ме", "ми", "мн", "мо", "му", "мы", "мэ", "мя",
						"на", "не", "ни", "но", "нр", "ну",
						"об", "ог", "од", "ож", "оз", "ок", "ол", "он", "оо", "оп", "ор", "ос", "от", "оф", "ох", "оч", "ош",
						"па", "пе", "пи", "пл", "по", "пр", "пс", "пт", "пу", "пы", "пь", "пя",
						"ра", "ре", "ри", "ро", "ру", "ры", "рэ", "ря",
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

		// If all the characters in the word are digits then could those digits
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
		WordState wordState = new WordState();

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
