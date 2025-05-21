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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import com.datadoghq.sketch.ddsketch.encoding.Output;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/*
 * The DDSketch serializer uses protobuf so convert it so we can use our JSON representation.
 */
public class SketchSerializer extends JsonSerializer<Sketch> {

	public class SimpleOutput implements Output {
		ByteArrayOutputStream buffer;

		SimpleOutput() {
			this.buffer = new ByteArrayOutputStream();
		}

		@Override
		public void writeByte(final byte value) {
			buffer.write(value);
		}
	}

	@Override
	public void serialize(final Sketch value, final JsonGenerator generator, final SerializerProvider serializers) throws IOException {
		generator.writeStartObject();
		generator.writeStringField("ftaType", value.type.name());
		generator.writeNumberField("totalSketchEntries", value.totalSketchEntries);
		generator.writeNumberField("relativeAccuracy", value.relativeAccuracy);
		serializers.defaultSerializeField("stringConverter", value.stringConverter, generator);

		final SimpleOutput output = new SimpleOutput();
		value.getDdSketch().encode(output, false);

		final byte[] encoded = Base64.getEncoder().encode(output.buffer.toByteArray());
		generator.writeStringField("ddSketch", new String(encoded));
		generator.writeEndObject();
	}
}
