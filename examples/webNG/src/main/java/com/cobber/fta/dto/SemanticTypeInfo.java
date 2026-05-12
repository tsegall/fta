package com.cobber.fta.dto;

import java.util.List;

public record SemanticTypeInfo(
	String id,
	String description,
	List<String> documentation,
	List<String> languages
) {}
