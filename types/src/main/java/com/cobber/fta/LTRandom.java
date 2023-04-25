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
 * This interface provides a nextRandom method which creates a new valid example of the Semantic Type.
 */
public interface LTRandom {
	/**
	 * nextRandom will generate a random (secure) valid example of this Semantic Type.
	 * @return a new valid example of the Semantic Type.
	 */
	String nextRandom();

	/**
	 * Seed the secure random number generator used to create examples.
	 * @param seed The Byte array used to seed the random number geerator.
	 */
	void seed(byte[] seed);
}
