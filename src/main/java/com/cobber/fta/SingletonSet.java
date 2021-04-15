/*
 * Copyright 2017-2021 Tim Segall
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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.cobber.fta.core.RandomSet;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class used to cache the large sets across multiple threads.
 */
public class SingletonSet {
	private final String contentType;
	private final String content;
	private final String key;

	private final static Map<String, RandomSet<String>> memberCache = new HashMap<>();

	public SingletonSet(final String contentType, final String content) {
		this.contentType = contentType;
		this.content = content;
		this.key = contentType + "---" + content;
	}

	public Set<String> getMembers() {
		synchronized(memberCache) {
			final Set<String> result = memberCache.get(key);
			if (result != null)
				return result;

			final RandomSet<String> members = new RandomSet<>();
			if ("inline".equals(contentType)) {
				InlineContent inline;
				try {
					inline = (new ObjectMapper()).readValue(content, new TypeReference<InlineContent>(){});
				} catch (IOException e) {
					throw new IllegalArgumentException("Internal error: Issues with 'inline' content: " + content, e);
				}
				members.addAll(Arrays.asList(inline.members));
			}
			else {
				Reader reader;
				if ("file".equals(contentType))
					try {
						reader = new InputStreamReader(new FileInputStream(content));
					} catch (FileNotFoundException e) {
						throw new IllegalArgumentException("Internal error: Issues with 'file' content: " + content, e);
					}
				else if ("resource".equals(contentType))
					reader = new InputStreamReader(LogicalTypeFiniteSimpleExternal.class.getResourceAsStream(content));
				else
					throw new IllegalArgumentException("Internal error: contentType must be 'file' or 'resource'");

				try (BufferedReader bufferedReader = new BufferedReader(reader)){
					String line;

					while ((line = bufferedReader.readLine()) != null)
						members.add(line);
				} catch (IOException e) {
					throw new IllegalArgumentException("Internal error: Issues with 'file/resource' content: " + content, e);
				}
			}

			memberCache.put(key, members);

			return members;
		}
	}

	public String getAt(final int i) {
		return memberCache.get(key).get(i);
	}
}
