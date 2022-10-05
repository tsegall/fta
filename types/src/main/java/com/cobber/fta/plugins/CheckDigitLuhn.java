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

import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;

import com.cobber.fta.PluginDefinition;

/**
 * Plugin to detect valid Luhn check digits (typically Credit Cards or IMEI Numbers).
 */
public class CheckDigitLuhn extends CheckDigitLT {
	/** The Semantic type for this Plugin. */
	public static final String SEMANTIC_TYPE = "CHECKDIGIT.LUHN";

	private final static String[] SAMPLES = {
			"5336066487174265", "5152085894236419", "6011352181320804", "4040861696988699",
			"379263518411175", "349635067146996", "4094833846177296", "379798201251917",
			"6011097612307009", "6011430779457350", "6011790026589879", "375959643587672",
			"342101796845421", "6458334201598593", "30418984504967", "30101909852741",
			"3530762763703806", "5243566131471456", "5404353653950224", "340003117928906",
			"346198540435315", "375001413995411", "3570326761758591", "3578409602184995",
			"3540785467082852", "3531088265159894", "4353659343287239", "375819865868442",
			"6011970224663557", "3585933447413831", "30378911900573", "3549227000323783",
			"5350441504117848", "4183247424721064", "3547000483688549", "6011056751216082",
			"3582562173472878", "370489247813535", "4058930351915799", "4537524395796637",
			"6011154019528859", "5185744993157105", "4863977274915278", "340361918241808",
			"373997869491312", "376247433687830", "6011074460280935", "5109557920544382",
			"4840144213641386", "30944773721611", "36891812075117", "3541302833590136",
			"30100970459766", "4432672470607793", "6471176324140693", "5400291701537243",
			"5237506120562152", "6011797378029594", "6011316750430039", "6011986506948961",
			"3543202162621783", "6011508670837380", "4000962466359869", "5229573565398575",
			"30907555346659", "30994688773504", "5420212533905586", "6226926045801942",
			"4781096058689949", "374378100732419", "370611358828065", "4072156516243099",
			"340962746370725", "6011241436563872", "4473939723377072", "6011441347488082",
			"5419979595723825", "5272156700720501", "5129093766594782", "5381875761588030",
			"3541969500869140", "30183745305696", "5248717442597415", "5326623131230851",
			"5220622501339463", "3544363140412494", "6011108707690637", "4302298836704251",
			"39316479097102", "3565267582109829", "5395131577905214", "5425631765960487",
			"4381165413919301", "4351908301577970", "3564305175308647", "4100026543775767",
			"6011764528404192", "372284536807811", "3540617433797323", "4961364634920028",
			"3575233039092495", "4465042911991489", "5452409726547867", "5471910314470682",
			"3551106042199564", "4262108230362085", "3542609864524563", "3584053321431139",
			"5375301625509474", "4162854447981859", "3570124519482774", "5499453176769451",
			"6011605646956748", "6520507556542662", "5149521464837349", "5200871670622977",
			"5235551440353082", "372400580525824", "3559303162212937", "38878618243393",
			"343013941571498", "4591487432269328", "5188222030612322", "4826336448831711"
	};

	/**
	 * Construct a plugin to detect Luhn Check Digits based on the Plugin Definition.
	 * @param plugin The definition of this plugin.
	 */
	public CheckDigitLuhn(final PluginDefinition plugin) {
		super(plugin, -1);
		validator = new LuhnCheckDigit();
	}

	@Override
	public boolean isValid(final String input, final boolean detectMode) {
		return input.length() >= 8 && input.length() < 30 && validator.isValid(input);
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public String nextRandom() {
		return SAMPLES[random.nextInt(SAMPLES.length)];
	}
}
