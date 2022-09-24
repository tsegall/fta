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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/*
 * The DDSketch serializer uses protobuf so convert it so we can use our JSON representation.
 */
public class SketchSerializer extends JsonSerializer<Sketch> {
	@Override
	public void serialize(Sketch value,  JsonGenerator generator, SerializerProvider serializers) throws IOException {
		ByteBuffer buffer = value.getDdSketch().serialize();
		byte[] encoded = Base64.getEncoder().encode(buffer.array());
		generator.writeString(new String(encoded));
	}
}
