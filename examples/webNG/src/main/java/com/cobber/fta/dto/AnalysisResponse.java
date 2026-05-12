package com.cobber.fta.dto;

import java.util.List;

public record AnalysisResponse(
	String filename,
	String locale,
	int recordCount,
	List<FieldResult> fields
) {}
