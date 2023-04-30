/*
 * Copyright 2017-2023 Tim Segall
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

/**
 * The Documentation entry on the plugin has an array of entries. Each entry has a documentation
 * source (e.g. wikidata, wikipedia) and a source specific reference - commonly a URL to the actual definition.
 */
public class PluginDocumentationEntry {
	public String source;
	public String reference;
	public PluginMatchEntry[] matchEntries;
}
