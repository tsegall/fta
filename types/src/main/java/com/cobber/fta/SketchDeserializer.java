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
import java.util.Base64;

import com.cobber.fta.core.FTAType;
import com.datadoghq.sketch.ddsketch.DDSketch;
import com.datadoghq.sketch.ddsketch.DDSketches;
import com.datadoghq.sketch.ddsketch.encoding.Input;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;

/*
 * The DDSketch deserializer uses protobuf so convert it so we can use our JSON representation.
 */
public class SketchDeserializer extends JsonDeserializer<Sketch> {
	public class SimpleInput implements Input {
		private byte[] buffer;
		private int offset;

		SimpleInput(final byte[] buffer) {
			this.buffer = buffer;
			this.offset = 0;
		}

		@Override
		public boolean hasRemaining() throws IOException {
			return offset < buffer.length;
		}

		@Override
		public byte readByte() throws IOException {
			return buffer[offset++];
		}
	}

	@Override
	public Sketch deserialize(final JsonParser p, final DeserializationContext ctx) throws IOException {
		final JsonNode node = p.getCodec().readTree(p);
		final FTAType type = FTAType.valueOf(node.get("ftaType").asText());
		final long totalSketchEntries = ((IntNode)node.get("totalSketchEntries")).longValue();
		final double relativeAccuracy = ((DoubleNode)node.get("relativeAccuracy")).doubleValue();
		final byte[] bytes = Base64.getDecoder().decode(node.get("ddSketch").asText());

		final SimpleInput input = new SimpleInput(bytes);

		final DDSketch ddSketch = DDSketches.unboundedDense(relativeAccuracy);

		ddSketch.decodeAndMergeWith(input);

		final JsonNode nodeSC = node.get("stringConverter");
		final StringConverter stringConverter = ctx.readTreeAsValue(nodeSC, StringConverter.class);
		final Sketch ret = new Sketch(type, Facts.getTypedMap(type, stringConverter),  stringConverter, relativeAccuracy);
		ret.setDdSketch(ddSketch);
		ret.totalSketchEntries = totalSketchEntries;

		return ret;
	}
}
