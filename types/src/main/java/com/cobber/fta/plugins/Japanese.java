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
package com.cobber.fta.plugins;

/**
 * Utilities for detecting Japanese characters in text.
 */
public final class Japanese {
	private Japanese() {}

	/**
	 * Returns true if {@code ch} is a Japanese character: hiragana, katakana
	 * (full- and half-width), CJK Unified Ideographs, CJK Extension A, or
	 * the ideographic iteration mark 々 (U+3005).
	 */
	public static boolean isJapaneseChar(final char ch) {
		return (ch >= '･' && ch <= 'ﾟ') ||	// Halfwidth Katakana
			   (ch >= '぀' && ch <= 'ヿ') ||	// Hiragana (3040–309F) + Katakana (30A0–30FF)
			   (ch >= '一' && ch <= '鿿') ||	// CJK Unified Ideographs
			   (ch >= '㐀' && ch <= '䶿') ||	// CJK Extension A
			   ch == '々';							// 々 Ideographic Iteration Mark
	}
}
