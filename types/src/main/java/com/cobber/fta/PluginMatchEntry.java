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
package com.cobber.fta;

/*
 * Used by RegExp plugins.  Each RegExp plugin has a set of PluginMatchEntries which have an ordered list of regular Expressions used to match against the Stream Data.
 * If the data matches one of the regular expressions then the regExpReturned is the answer returned to the user.  For example for a SSN the "regExpsToMatch" is
 * [ "\\d{3}-\\d{2}-\\d{4}" ] and the "regExpReturned" is "(?!666|000|9\\d{2})\\d{3}-(?!00)\\d{2}-(?!0{4})\\d{4}" (which is a more accurate regular expression for a SSN).
 * The isRegExpComplete indicates if all values that the regular expression allows are valid.
 */
public class PluginMatchEntry {
	/** RegExp plugins: the RegExps to be matched to qualify as this Semantic Type. */
	public String[] regExpsToMatch;
	/** RegExp plugins: the RegExp to be returned for this Semantic Type. */
	public String regExpReturned;
	/** Is the returned Regular Expression a complete representation of the Semantic Type. */
	public boolean isRegExpComplete;

	PluginMatchEntry() {
	}

	PluginMatchEntry(final String regExpReturned) {
		this.regExpReturned = regExpReturned;
	}

	public String[] getRegExpsToMatch() {
		// The optional 'regExpsToMatch' tag is an ordered list of Regular Expressions used to match against the Stream Data.
		// If not set then the regExpReturned is used to match.
		if (regExpsToMatch == null)
			return new String[] { regExpReturned };

		return regExpsToMatch;
	}

	public boolean isRegExpComplete() {
		return isRegExpComplete;
	}

	public String getRegExpReturned() {
		return regExpReturned;
	}

	public boolean matches(String regExp) {
		for (final String re : getRegExpsToMatch()) {
			if (".+".equals(re) || regExp.equals(re))
				return true;
		}

		return false;
	}
}
