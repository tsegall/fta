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
package com.cobber.fta.plugins;

import org.apache.commons.validator.routines.checkdigit.IBANCheckDigit;

import com.cobber.fta.PluginDefinition;
import com.cobber.fta.core.FTAType;

/**
 * Plugin to detect valid International Bank Account Numbers (IBAN) .
 */
public class CheckDigitIBAN extends CheckDigitLT {
	/** The Semantic type for this Plugin. */
	public static final String SEMANTIC_TYPE = "CHECKDIGIT.IBAN";

	/** The Regular Express for this Semantic type. */
	public static final String REGEXP = "([A-Z]{2}[ \\-]?[0-9]{2})(?=(?:[ \\-]?[A-Z0-9]){9,30}$)((?:[ \\-]?[A-Z0-9]{3,5}){2,7})([ \\-]?[A-Z0-9]{1,3})";

	private final static String[] SAMPLES = {
			"NL47CITI0080734537", "NL64ABNA0998881740", "NL37ARBN0845390481", "NL65CITC0881228753",
			"NL37NNBA0477341969", "NL98INGB0618648542", "NL32UBSW0188040064", "NL10BNGH0364982365",
			"NL20ANDL0731953959", "NL33KABA0404574777", "NL07ABNA0056892195", "NL32BMEU0798878975",
			"NL04LOYD0499048482", "NL04ISBK0647256630", "NL47BKCH0611644134", "NL77BOFS0096902299",
			"NL10INGB0755184998", "NL34CITC0712707913", "NL38ABNA0499780639", "NL98SOGE0110402545",
			"NL61DEUT0523173156", "NL23SNSB0721066607", "NL26KOEX0060578971", "NL61ASNB0340790164",
			"NL54CITC0276664132", "NL07BLGW0117671851", "NL32CITI0149667663", "NL85BICK0597444064",
			"NL83INGB0869180800", "NL63RBOS0864358911",
			"AD1400080001001234567890", "AT483200000012345864", "AZ96AZEJ00000000001234567890",
			"BH02CITI00001077181611", "BY86AKBB10100000002966000000", "BE71096123456769",
			"BA393385804800211234", "BR1500000000000010932840814P2", "BG18RZBB91550123456789",
			"CR23015108410026012345", "HR1723600001101234565", "CY21002001950000357001234567",
			"CZ5508000000001234567899", "DK9520000123456789", "DO22ACAU00000000000123456789",
			"EG800002000156789012345180002", "SV43ACAT00000000000000123123", "EE471000001020145685",
			"FO9264600123456789", "FI1410093000123458", "FR7630006000011234567890189",
			"GE60NB0000000123456789", "DE75512108001245126199", "GI04BARC000001234567890",
			"GR9608100010000001234567890", "GL8964710123456789", "GT20AGRO00000000001234567890",
			"VA59001123000012345678", "HU93116000060000000012345676", "IS750001121234563108962099",
			"IQ20CBIQ861800101010500", "IE64IRCE92050112345678", "IL170108000000012612345",
			"IT60X0542811101000000123456", "JO71CBJO0000000000001234567890", "KZ563190000012344567",
			"XK051212012345678906", "KW81CBKU0000000000001234560101", "LV97HABA0012345678910",
			"LB92000700000000123123456123", "LY38021001000000123456789", "LI7408806123456789012",
			"LT601010012345678901", "LU120010001234567891", "MT31MALT01100000000000000000123",
			"MR1300020001010000123456753", "MU43BOMM0101123456789101000MUR", "MD21EX000000000001234567",
			"MC5810096180790123456789085", "ME25505000012345678951", "NL02ABNA0123456789",
			"MK07200002785123453", "NO8330001234567", "PK36SCBL0000001123456702",
			"PS92PALS000000000400123456702", "PL10105000997603123456789123", "PT50002700000001234567833",
			"QA54QNBA000000000000693123456", "RO09BCYP0000001234567890", "LC14BOSL123456789012345678901234",
			"SM76P0854009812123456789123", "ST23000200000289355710148", "SA4420000001234567891234",
			"RS35105008123123123173", "SC52BAHL01031234567890123456USD", "SK8975000000000012345671",
			"SI56192001234567892", "ES7921000813610123456789", "SD8811123456789012",
			"SE7280000810340009783242", "CH5604835012345678009", "TL380010012345678910106",
			"TN5904018104004942712345", "TR320010009999901234567890", "UA903052992990004149123456789",
			"AE460090000000123456789", "GB33BUKB20201555555555", "VG21PACG0000000123456789"
	};

	/**
	 * Construct a plugin to detect IBANs (International Bank Account Numbers) based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public CheckDigitIBAN(final PluginDefinition plugin) {
		super(plugin, -1);
		validator = new IBANCheckDigit();
	}

	@Override
	public String getRegExp() {
		return REGEXP;
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public FTAType getBaseType() {
		return FTAType.STRING;
	}

	@Override
	public String nextRandom() {
		return SAMPLES[random.nextInt(SAMPLES.length)];
	}
}
