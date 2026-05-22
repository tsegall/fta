package com.cobber.fta.dto;

import java.util.List;

import com.cobber.fta.faker.FakerParameters;

public record GenerateRequest(List<FakerParameters> spec, int count, String locale) {}
