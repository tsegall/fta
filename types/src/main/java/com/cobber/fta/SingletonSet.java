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
package com.cobber.fta;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.cobber.fta.core.RandomSet;

/**
 * Class used to cache large sets across multiple threads.
 */
public class SingletonSet {
	private final Content content;
	private final String key;
	private final String commentLeader;

	private final static Map<String, RandomSet<String>> MEMBER_CACHE = new ConcurrentHashMap<>();

	/**
	 * Create a SingletonSet using "#" as the comment character.
	 * @param content The content object which is either inline, file or resource.
	 */
	public SingletonSet(final Content content) {
		this(content, "#");
	}

	/**
	 * Create a SingletonSet.
	 * @param content The content object which is either inline, file or resource.
	 * @param commentLeader Use the supplied String as the comment leader.
	 */
	public SingletonSet(final Content content, final String commentLeader) {
		this.content = content;
		this.key = content.getCacheKey();
		this.commentLeader = commentLeader;
	}

	/**
	 * Accessor for the members of the Set.
	 * @return The members of this Set.
	 */
	public Set<String> getMembers() {
		synchronized(MEMBER_CACHE) {
			final Set<String> result = MEMBER_CACHE.get(key);
			if (result != null)
				return result;

			final RandomSet<String> members = new RandomSet<>();
			if ("inline".equals(content.type)) {
				members.addAll(Arrays.asList(content.members));
			}
			else {
				Reader reader;
				if ("file".equals(content.type))
					try {
						reader = new InputStreamReader(new FileInputStream(content.reference), StandardCharsets.UTF_8);
					} catch (FileNotFoundException e) {
						throw new IllegalArgumentException("Internal error: Issues with 'file' content: " + content, e);
					}
				else if ("resource".equals(content.type)) {
					final InputStream stream = LogicalTypeFiniteSimpleExternal.class.getResourceAsStream(content.reference);
					if (stream == null)
						throw new IllegalArgumentException("Internal error: Issues with 'resource' content: " + content);
					reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
				}
				else
					throw new IllegalArgumentException("Internal error: contentType must be 'inline', 'file' or 'resource'");

				try (BufferedReader bufferedReader = new BufferedReader(reader)){
					String line;

					while ((line = bufferedReader.readLine()) != null) {
						if (commentLeader != null && line.startsWith(commentLeader))
							continue;
						members.add(line);
					}
				} catch (IOException e) {
					throw new IllegalArgumentException("Internal error: Issues with 'file/resource' content: " + content, e);
				}
			}

			MEMBER_CACHE.put(key, members);

			return members;
		}
	}

	public String getRandom(final SecureRandom random) {
		final int size = getMembers().size();
		return MEMBER_CACHE.get(key).get(random.nextInt(size));
	}
}
