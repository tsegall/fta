package com.cobber.fta;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.testng.annotations.Test;

import com.cobber.fta.core.FTAException;
import com.cobber.fta.core.Utils;
import com.cobber.fta.dates.DateTimeParser;

public class TestIssues {

	private List<String[]> asRecords(String[] fieldValues) {
		ArrayList<String[]> ret = new ArrayList<>();

		for (String fieldValue : fieldValues)
			ret.add(new String[] { fieldValue });

		return ret;
	}

	@Test(groups = { TestGroups.ALL, TestGroups.LONGS })
	public void issue24() throws IOException, FTAException {
		final int LONGEST = 40;
		final String longBlank = Utils.repeat(' ', LONGEST);
		final int SHORTEST = 1;
		final String shortBlank = Utils.repeat(' ', SHORTEST);
		final String[] values = { "", "cmcfarlan13@aol.com", "cgorton14@dell.com", "kkorneichike@marriott.com",
				"alovattj@qq.com", "wwinterscalek@weibo.com", "cfugglel@pen.io.co.uk", "bsel&%odp@bloglovin.com",
				"gjoplingq@guardian.co.uk", "cvall$&owr@vkontakte.ru", "fpenas@bandcamp.com", "''", "NULL", "",
				"kkirsteiny@icio.us", "jgeistbeckz@shutterfly.com", "achansonne10@mac.com",
				"bpiotrkowski11#barnesandnoble.com", "jaikett15@netlog.com", "dattril17@phoca.cz",
				"abranchet18@psu.edu", "ddisley19@alexa.com", "vspriddle1a@japanpost.jp", "fdurbin1b@intel.com",
				"yedelheit1c@usda.gov", "msimacek1d@wikia.com", "rmessage1e@bizjournals.com",
				"hallenson1f@linkedin.com", "hrutley1g@phoca.cz", "kroakes1h@issuu.com", "msign1i@ocn.ne.jp",
				"hsiderfin1j@qq.com", "civakhin1k@sphinn.com", "abetty1l@yolasite.com", "lgussin1m@ft.com",
				"kfairleigh1n@ftc.gov", "kbrocklesby1o@tumblr.com", "nrands1p@google.com.br",
				"thattoe1q@washingtonpost.com", "vmadle1r@soup.io", "", "twhordley2c@addtoany.com",
				shortBlank, longBlank
		};

		AnalyzerContext context = new AnalyzerContext(null, DateTimeParser.DateResolutionMode.Auto, "withBlanks",
				new String[] { "email" });
		TextAnalyzer template = new TextAnalyzer(context);
		template.setLocale(Locale.getDefault());
		RecordAnalyzer analyzer = new RecordAnalyzer(template);

		for (String[] value : asRecords(values))
			analyzer.train(value);

		for (TextAnalysisResult result : analyzer.getResult().getStreamResults()) {
			assertEquals(result.getSampleCount(), values.length);
			assertEquals(result.getMaxLength(), LONGEST);
			assertEquals(result.getMinLength(), SHORTEST);
			assertTrue(TestUtils.checkCounts(result));
		}
	}
}
