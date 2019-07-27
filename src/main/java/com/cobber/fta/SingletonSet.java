package com.cobber.fta;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Class used to cache the large sets across multiple threads.
 */
public class SingletonSet {
	String contentType;
	String content;
	String key;

	static HashMap<String, Set<String>> memberCache = new HashMap<>();
	static HashMap<String, String[]> memberArrayCache = new HashMap<>();

	public SingletonSet(String contentType, String content) {
		this.contentType = contentType;
		this.content = content;
		this.key = contentType + "---" + content;
	}

	public Set<String> getMembers() {
		synchronized(memberCache) {
			Set<String> result = memberCache.get(key);
			if (result != null)
				return result;

			Reader reader = null;
			if (contentType.equals("inline"))
				reader = new StringReader(content.replace('|', '\n'));
			else if (contentType.equals("file"))
				try {
					reader = new InputStreamReader(new FileInputStream(content));
				} catch (FileNotFoundException e) {
					throw new IllegalArgumentException("Internal error: Issues with database for: " + content, e);
				}
			else if (contentType.equals("resource"))
				reader = new InputStreamReader(LogicalTypeFiniteSimpleExternal.class.getResourceAsStream(content));

			Set<String> members = new HashSet<>();

			try (BufferedReader bufferedReader = new BufferedReader(reader)){
				String line = null;

				while ((line = bufferedReader.readLine()) != null)
					members.add(line);
			} catch (IOException e) {
				throw new IllegalArgumentException("Internal error: Issues with database for: " + content, e);
			}

			memberCache.put(key, members);

			return members;
		}
	}

	public String[] getMemberArray() {
		synchronized(memberArrayCache) {
			String[] result = memberArrayCache.get(key);

			if (result != null)
				return result;

			result = memberCache.get(key).toArray(new String[memberCache.get(key).size()]);
			memberArrayCache.put(key, result);

			return result;
		}
	}
}
