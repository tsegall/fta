package com.cobber.fta.dto;

import tools.jackson.databind.JsonNode;

public record FieldResult(
	String fieldName,
	boolean isSemanticType,
	String type,
	String typeModifier,
	String semanticTypeName,
	String minValue,
	String maxValue,
	JsonNode details
) {}
