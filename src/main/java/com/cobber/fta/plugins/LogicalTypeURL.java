package com.cobber.fta.plugins;

import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.apache.commons.validator.routines.UrlValidator;

import com.cobber.fta.LogicalTypeInfinite;
import com.cobber.fta.PatternInfo;
import com.cobber.fta.PluginDefinition;
import com.cobber.fta.StringFacts;

/**
 * Plugin to detect URLs.
 */
public class LogicalTypeURL extends LogicalTypeInfinite {
	public final static String SEMANTIC_TYPE = "URL";
	public final static String REGEXP_PROTOCOL = "(https?|ftp|file)";
	public final static String REGEXP_RESOURCE = "[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
	static UrlValidator validator = null;
	private static Random random = null;
	private static String[] sitesList = new String[] {
			"www.jnj.com", "http://www.medifast1.com/index.jsp", "graybar.com", "johnsoncontrols.com", "commscope.com", "www.energizer.com", "ashland.com", "hersheys.com", "www.flowserve.com", "www.exxonmobil.com",
			"pattersoncompanies.com", "www.campbellsoup.com", "mars.com", "www.bjs.com", "conagra.com", "www.lilly.com", "sap.com", "jci.com", "www.dtcc.com", "bakerhughes.com", "www.microsoft.com", "www.jackson.com",
			"www.chs.net", "www.gallo.com", "www.hormelfoods.com", "perrigo.com", "www.cognizant.com", "www.generalmills.com", "hrblock.com", "www.abbott.com", "www.acuity.com", "www.corning.com", "www.lear.com",
			"bunge.com", "fultonschools.org", "www.agcocorp.com", "www.tcfbank.com", "bamfunds.com", "starbucks.com", "www.heicocompanies.com", "www.hertz.com", "canpack.eu", "dell.com", "disney.com", "gm.com",
			"www.mccormick.com", "www.merck.com", "www.versummaterials.com", "www.baxter.com", "www.chevron.com", "www.raytheon.com", "www.valvoline.com", "http://www.pcg-usa.com/", "www.aam.com", "https://techwave.net/",
			"www.bridgestone-firestone.com/", "www.goodyear.com", "www.rockwellcollins.com", "www.rpminc.com", "www.albemarle.com", "www.dow.com", "www.firstmidwest.com", "http://whirlpoolcorp.com", "www.akorn.com",
			"www.apple.com", "www.cardinalhealth.com", "www.fairwayne.com", "www.horacemann.com", "www.kohler.com", "www.richs.com", "www.amgen.com", "www.bms.com", "www.cargill.com",
			"www.cisco.com", "www.exeloncorp.com", "www.farmers.com", "www.firstenergycorp.com", "www.marathonpetroleum.com", "www.usfoods.com", "www.weber.com", "aig.com", "caterpillar.com"
	};

	static {
		validator = UrlValidator.getInstance();
	}
	int[] protocol = new int[2];

	public LogicalTypeURL(PluginDefinition plugin) {
		super(plugin);
	}

	@Override
	public boolean initialize(Locale locale) {
		threshold = 95;

		random = new Random(402);

		return true;
	}

	@Override
	public String nextRandom() {
		return sitesList[random.nextInt(sitesList.length)];
	}

	@Override
	public String getQualifier() {
		return SEMANTIC_TYPE;
	}

	@Override
	public String getRegExp() {
		if (protocol[0] != 0)
			return protocol[1] != 0 ? REGEXP_PROTOCOL + "?" + REGEXP_RESOURCE : REGEXP_PROTOCOL + REGEXP_RESOURCE;

		return REGEXP_RESOURCE;
	}

	@Override
	public boolean isRegExpComplete() {
		return true;
	}

	@Override
	public PatternInfo.Type getBaseType() {
		return PatternInfo.Type.STRING;
	}

	@Override
	public boolean isValid(String input) {
		int index = 0;
		if (input.indexOf("://") == -1) {
			input = "http://" + input;
			index = 1;
		}

		boolean ret = validator.isValid(input);
		if (ret)
			protocol[index]++;

		return ret;
	}

	@Override
	public boolean isCandidate(String trimmed, StringBuilder compressed, int[] charCounts, int[] lastIndex) {
		// Quickly rule out rubbish
		if (charCounts[' '] != 0)
				return false;

		// Does it have a protocol?
		if (charCounts[':'] != 0 && compressed.indexOf("://") != -1)
			return true;

		return validator.isValid("http://" + trimmed);
	}

	@Override
	public String isValidSet(String dataStreamName, long matchCount, long realSamples, StringFacts stringFacts, Map<String, Integer> cardinality, Map<String, Integer> outliers) {

		return getConfidence(matchCount, realSamples) >= getThreshold()/100.0 ? null : ".+";
	}

	@Override
	public double getConfidence(long matchCount, long realSamples) {
		// If we have only seen items with no protocol then drop our confidence
		return matchCount == realSamples && protocol[0] == 0 ? 0.95 : (double)matchCount/realSamples;
	}
}
