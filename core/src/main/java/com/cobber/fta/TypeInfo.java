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

import java.util.Objects;

import com.cobber.fta.KnownTypes.ID;
import com.cobber.fta.core.FTAType;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The TypeInfo class maintains a set of information about a simple pattern.
 * This is used to derive a Type from a pattern. For example,
 *  new PatternInfo("(?i)true|false", null, "", "Boolean", null)
 * indicates that a case insensitive match for true or false indicates a boolean type
 * ("BOOLEAN").
 */
public class TypeInfo {
	public enum TypeModifier {
		SIGNED,
		GROUPING,
		EXPONENT,
		NON_LOCALIZED,
		SIGNED_TRAILING,
		NULL,
		BLANK,
		BLANKORNULL,
		TRUE_FALSE,
		YES_NO,
		Y_N,
		ONE_ZERO;

		private final int flag = 1 << ordinal();

		int getFlag() {
			return flag;
		}
	}

	public final static int SIGNED_FLAG = TypeModifier.SIGNED.getFlag();
	public final static int GROUPING_FLAG = TypeModifier.GROUPING.getFlag();
	public final static int EXPONENT_FLAG = TypeModifier.EXPONENT.getFlag();
	public final static int NON_LOCALIZED_FLAG = TypeModifier.NON_LOCALIZED.getFlag();
	public final static int SIGNED_TRAILING_FLAG = TypeModifier.SIGNED_TRAILING.getFlag();
	public final static int NULL_FLAG = TypeModifier.NULL.getFlag();
	public final static int BLANK_FLAG = TypeModifier.BLANK.getFlag();
	public final static int BLANKORNULL_FLAG = TypeModifier.BLANKORNULL.getFlag();
	public final static int TRUE_FALSE_FLAG = TypeModifier.TRUE_FALSE.getFlag();
	public final static int YES_NO_FLAG = TypeModifier.YES_NO.getFlag();
	public final static int Y_N_FLAG = TypeModifier.Y_N.getFlag();
	public final static int ONE_ZERO_FLAG = TypeModifier.ONE_ZERO.getFlag();

	private int minLength;
	private int maxLength;
	private FTAType baseType;

	public KnownTypes.ID id;
	public String regexp;
	public String generalPattern;
	public String format;
	public String typeModifier;
	public int typeModifierFlags;
	public boolean isSemanticType;
	private String semanticType;
	private boolean isForce;

	/**
	 * Construct a new information block for the supplied pattern.
	 * @param id
	 *            The ID of interest.
	 * @param regexp
	 *            The pattern of interest.
	 * @param baseType
	 *            The type of the pattern.
	 * @param newType
	 *            Either the Type Modifier or the Semantic Type (if is Semantic Type is true) (optional).
	 * @param isSemanticType
	 *			  A boolean indicating if this is a Semantic Type.
	 * @param minLength
	 *            The minimum length of this pattern (-1 implies undefined)
	 * @param maxLength
	 *            The maximum length of this pattern (-1 implies undefined)
	 * @param generalPattern
	 *            The general case of this pattern (optional).
	 * @param format
	 *            The Java format specified for a date pattern (optional).
	 */
	public TypeInfo(final ID id, final String regexp, final FTAType baseType, final String newType,
			final boolean isSemanticType, final int minLength, final int maxLength, final String generalPattern,
			final String format) {
		this.id = id;
		this.regexp = regexp;
		this.baseType = baseType;
		if (isSemanticType)
			this.semanticType = newType;
		else
			this.typeModifier = newType;
		this.isSemanticType = isSemanticType;
		this.minLength = minLength;
		this.maxLength = maxLength;
		this.generalPattern = generalPattern;
		this.format = format;
	}

	/**
	 * Construct a new information block for the supplied pattern (simple only - not Semantic Type).
	 * @param id The ID of interest.
	 * @param regexp The pattern of interest.
	 * @param baseType The type of the pattern.
	 * @param typeModifier The type modifier of the pattern (optional).
	 * @param typeModifierFlags For numerics a set of flags representing the modifier.
	 */
	public TypeInfo(final ID id, final String regexp, final FTAType baseType, final String typeModifier, final int typeModifierFlags) {
		this.id = id;
		this.regexp = regexp;
		this.baseType = baseType;
		this.typeModifier = typeModifier;
		this.isSemanticType = false;
		this.minLength = -1;
		this.maxLength = -1;
		this.generalPattern = null;
		this.format = null;
		this.typeModifierFlags = typeModifierFlags;
	}

	/**
	 * Construct a new information block for the supplied pattern (Semantic Type).
	 * @param regexp The pattern of interest.
	 * @param baseType The type of the pattern.
	 * @param semanticType The Semantic Type of the pattern.
	 * @param prior The previous TypeInfo (we preserve information about the Base Type).
	 */
	public TypeInfo(final String regexp, final FTAType baseType, final String semanticType, final TypeInfo prior) {
		this.id = null;
		this.regexp = regexp;
		this.baseType = baseType;
		if (prior != null && prior.getBaseType().equals(baseType)) {
			this.typeModifier = prior.typeModifier;
			this.typeModifierFlags = prior.typeModifierFlags;
		}
		else {
			this.typeModifier = null;
			this.typeModifierFlags = 0;
		}
		this.semanticType = semanticType;
		this.isSemanticType = true;
		this.minLength = -1;
		this.maxLength = -1;
		this.generalPattern = null;
		this.format = null;
	}

	public TypeInfo(final TypeInfo that) {
		this.id = that.id;
		this.regexp = that.regexp;
		this.baseType = that.baseType;
		this.typeModifier = that.typeModifier;
		this.semanticType = that.semanticType;
		this.isSemanticType = that.isSemanticType;
		this.minLength = that.minLength;
		this.maxLength = that.maxLength;
		this.generalPattern = that.generalPattern;
		this.format = that.format;
		this.typeModifierFlags = that.typeModifierFlags;
	}

	protected TypeInfo() {
	}

	/**
	 * Is this pattern Numeric?
	 *
	 * @return A boolean indicating if the Type for this pattern is numeric.
	 */
	@JsonIgnore
	public boolean isNumeric() {
		return this.baseType.isNumeric();
	}

	/**
	 * Is this pattern a Date Type?
	 *
	 * @return A boolean indicating if the Type for this pattern includes a Date.
	 */
	@JsonIgnore
	public boolean isDateType() {
		return this.baseType.isDateType();
	}

	/**
	 * The base FTAType that this pattern is based on.
	 *
	 * @return The FTAType of the TypeInfo.
	 */
	public FTAType getBaseType() {
		return baseType;
	}

	/**
	 * Set the base FTAType - use this method with care!
	 * @param baseType The type of the TypeInfo.
	 */
	protected void setBaseType(final FTAType baseType) {
		this.baseType = baseType;
		this.typeModifier = null;
		this.typeModifierFlags = 0;
	}

	/**
	 * The Semantic Type detected.
	 *
	 * @return The Semantic Type for this pattern.
	 */
	public String getSemanticType() {
		return semanticType;
	}

	/**
	 * Set the Semantic Type - use this method with care!
	 * @param semanticType The Semantic type of the TypeInfo.
	 */
	protected void setSemanticType(final String semanticType) {
		this.semanticType = semanticType;
		if (semanticType != null)
			this.isSemanticType = true;
	}

	/**
	 * The minimum length of this pattern (-1 indicates undefined).
	 *
	 * @return An integer with the minimum length of this pattern.
	 */
	public int getMinLength() {
		return minLength;
	}

	/**
	 * The maximum length of this pattern (-1 indicates undefined).
	 *
	 * @return An integer with the maximum length of this pattern.
	 */
	public int getMaxLength() {
		return maxLength;
	}

	/**
	 * Is this pattern Alphabetic?
	 *
	 * @return A boolean indicating if the Type for this pattern is Alphabetic.
	 */
	@JsonIgnore
	public boolean isAlphabetic() {
		return this.regexp.startsWith(KnownTypes.PATTERN_ALPHA);
	}

	/**
	 * Is this pattern Alphanumeric?
	 *
	 * @return A boolean indicating if the Type for this pattern is Alphanumeric.
	 */
	@JsonIgnore
	public boolean isAlphanumeric() {
		return this.regexp.startsWith(KnownTypes.PATTERN_ALPHANUMERIC);
	}

	/**
	 * Is this TypeInfo a Semantic Type?
	 *
	 * @return A boolean indicating if this is a Semantic Type.
	 */
	public boolean isSemanticType() {
		return isSemanticType;
	}

	/**
	 * Is this TypeInfo a 'forced' Semantic Type - i.e. an external agent has told us the Semantic Type.
	 *
	 * @return A boolean indicating if this is a forced Semantic Type.
	 */
	public boolean isForce() {
		return isForce;
	}

	/**
	 * Indicate if this TypeInfo is a 'forced' Semantic Type - i.e. an external agent has told us the Semantic Type.
	 *
	 * @param isForce A boolean indicating if this is a forced Semantic Type.
	 */
	public void setForce(final boolean isForce) {
		this.isForce = isForce;
	}

	@JsonIgnore
	public boolean hasExponent() {
		return (typeModifierFlags & EXPONENT_FLAG) != 0;
	}

	@JsonIgnore
	public boolean hasGrouping() {
		return (typeModifierFlags & GROUPING_FLAG) != 0;
	}

	@JsonIgnore
	public boolean isNonLocalized() {
		return (typeModifierFlags & NON_LOCALIZED_FLAG) != 0;
	}

	@JsonIgnore
	public boolean isSigned() {
		return (typeModifierFlags & SIGNED_FLAG) != 0;
	}

	@JsonIgnore
	public boolean isTrailingMinus() {
		return (typeModifierFlags & SIGNED_TRAILING_FLAG) != 0;
	}

	@JsonIgnore
	public boolean isNull() {
		return (typeModifierFlags & NULL_FLAG) != 0;
	}

	@JsonIgnore
	public boolean isBlank() {
		return (typeModifierFlags & BLANK_FLAG) != 0;
	}

	@JsonIgnore
	public boolean isBlankOrNull() {
		return (typeModifierFlags & BLANKORNULL_FLAG) != 0;
	}

	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder(100);

		ret.append("type: ").append(baseType).append(", typeModifier: ").append(typeModifier);

		if (isSemanticType)
			ret.append(", SemanticType: ").append(semanticType);
		ret.append(", regexp: ").append(regexp);

		return ret.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(baseType, format, generalPattern, id, isSemanticType, maxLength, minLength, regexp,
				semanticType, typeModifier, typeModifierFlags);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final TypeInfo other = (TypeInfo) obj;
		return baseType == other.baseType && Objects.equals(format, other.format)
				&& Objects.equals(generalPattern, other.generalPattern) && id == other.id
				&& isSemanticType == other.isSemanticType && maxLength == other.maxLength && minLength == other.minLength
				&& Objects.equals(regexp, other.regexp) && Objects.equals(semanticType, other.semanticType)
				&& Objects.equals(typeModifier, other.typeModifier) && typeModifierFlags == other.typeModifierFlags;
	}
}
