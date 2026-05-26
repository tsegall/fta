/*
 * Copyright 2017-2026 Tim Segall
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAType;

public class TestTypeInfo {

	private TypeInfo flagged(final int flags) {
		return new TypeInfo(KnownTypes.ID.ID_DOUBLE, "\\d+\\.\\d+", FTAType.DOUBLE, "SIGNED", flags);
	}

	// ---- flag-based boolean getters ----

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void isSigned() {
		assertTrue(flagged(TypeInfo.SIGNED_FLAG).isSigned());
		assertFalse(flagged(0).isSigned());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void hasGrouping() {
		assertTrue(flagged(TypeInfo.GROUPING_FLAG).hasGrouping());
		assertFalse(flagged(0).hasGrouping());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void hasExponent() {
		assertTrue(flagged(TypeInfo.EXPONENT_FLAG).hasExponent());
		assertFalse(flagged(0).hasExponent());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void isNonLocalized() {
		assertTrue(flagged(TypeInfo.NON_LOCALIZED_FLAG).isNonLocalized());
		assertFalse(flagged(0).isNonLocalized());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void isTrailingMinus() {
		assertTrue(flagged(TypeInfo.SIGNED_TRAILING_FLAG).isTrailingMinus());
		assertFalse(flagged(0).isTrailingMinus());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void isNull() {
		final TypeInfo ti = new TypeInfo(KnownTypes.ID.ID_NULL, "[NULL]", FTAType.STRING, (String) null, TypeInfo.NULL_FLAG);
		assertTrue(ti.isNull());
		assertFalse(flagged(0).isNull());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void isBlank() {
		final TypeInfo ti = new TypeInfo(KnownTypes.ID.ID_BLANK, "", FTAType.STRING, (String) null, TypeInfo.BLANK_FLAG);
		assertTrue(ti.isBlank());
		assertFalse(flagged(0).isBlank());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void isBlankOrNull() {
		final TypeInfo ti = new TypeInfo(KnownTypes.ID.ID_BLANKORNULL, "", FTAType.STRING, (String) null, TypeInfo.BLANKORNULL_FLAG);
		assertTrue(ti.isBlankOrNull());
		assertFalse(flagged(0).isBlankOrNull());
	}

	// ---- setters ----

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void setAndGetRegExp() {
		final TypeInfo ti = flagged(TypeInfo.SIGNED_FLAG);
		ti.setRegExp("\\d+");
		assertEquals(ti.getRegExp(), "\\d+");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void setAndGetBaseType() {
		final TypeInfo ti = flagged(0);
		ti.setBaseType(FTAType.LONG);
		assertEquals(ti.getBaseType(), FTAType.LONG);
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void setAndGetSemanticType() {
		final TypeInfo ti = new TypeInfo("\\S+@\\S+", FTAType.STRING, "EMAIL", null);
		assertTrue(ti.isSemanticType());
		ti.setSemanticType("PHONE");
		assertEquals(ti.getSemanticType(), "PHONE");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void setAndGetForce() {
		final TypeInfo ti = flagged(0);
		assertFalse(ti.isForce());
		ti.setForce(true);
		assertTrue(ti.isForce());
	}

	// ---- toString ----

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void toStringContainsBaseType() {
		final TypeInfo ti = flagged(TypeInfo.SIGNED_FLAG);
		final String s = ti.toString();
		assertTrue(s.contains("Double"), "toString must mention base type");
		assertTrue(s.contains("regexp"), "toString must mention regexp");
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void toStringSemanticTypeIncluded() {
		final TypeInfo ti = new TypeInfo("\\S+@\\S+", FTAType.STRING, "EMAIL", null);
		final String s = ti.toString();
		assertTrue(s.contains("EMAIL"), "toString must include semantic type when isSemanticType=true");
	}

	// ---- hashCode ----

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void hashCodeEqualObjectsSameHash() {
		final TypeInfo a = flagged(TypeInfo.SIGNED_FLAG);
		final TypeInfo b = new TypeInfo(KnownTypes.ID.ID_DOUBLE, "\\d+\\.\\d+", FTAType.DOUBLE, "SIGNED", TypeInfo.SIGNED_FLAG);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test(groups = { TestGroups.ALL, TestGroups.TOKENS })
	public void hashCodeDifferentObjectsDifferentHash() {
		final TypeInfo a = flagged(TypeInfo.SIGNED_FLAG);
		final TypeInfo b = flagged(TypeInfo.GROUPING_FLAG);
		assertNotEquals(a, b);
	}
}
