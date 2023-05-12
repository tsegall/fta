# DataSet attributes
 - Identify recency fields (e.g. tlm_dt)
 - Identify reference/lookup tables
 - Improve FK detection
 - Coalesce multiple fields into concept, e.g.
	 - Honorific, First, Last, Middle into Name
	 - Address Line 1, Address Line2, Zip, State, Country into Full Address

# Field attributes
 - Improve PII detection (e.g. DOB, Age, ...)
 - Improve Semantic Type detection
	- IDENTITY.VAT_<COUNTRY> (DONE)
	- PERSON.YEAR_OF_BIRTH and PERSON.DATE_OF_BIRTH (DONE)
	- PERSON.AGE_RANGE
	- PERSON.RACE (Done)
	- PERSON.AGE (Done)
	- Mime Type
	- UserAgent
 - Outlier detection in numeric fields
 - Improved anomaly detection (see https://www.sciencedirect.com/science/article/pii/S0925231217309864)
 - Add Filename detection 
 - Add ContentFormat attribute: e.g. JSON, XML, base64 encoded, encrypted? (DONE)
 - Add Distribution attribute: uniform, normal, log-normal, exponential, other? (Kolmogorov-Smirnov test?)
 - Distinguish between HOME/WORK attributes (e.g. EMAIL, PHONE, ADDRESS)

# Broaden Identity supported
 - Chinese SSN
 - Belgian SSN
 - German SSN
 - Canadian SIN
 - ...

# Generalize Bloom Filter support for large static reference lists

# Rework Address Detection (DONE)
 - Improve precision - e.g. Full Address
 - Improve detection - e.g. locate Street Number

# Record Level detection (DONE)
 - Enables a set of additional detection if Pass One determines Semantic Types, and subsequent pass(es) use this information
 - Can improve detection - i.e. reject a description field as FREE_TEXT if highly correlated with another field

# Add distinction between invalid entries and outliers (DONE)

# Improve Date Detection (DONE)

# Improve Internationalization
 - Headers RegExp should be per locale (DONE)
 - "regExpsToMatch", "regExpReturned", "isRegExpComplete" should be per locale (DONE)
 - Gender support needs rework (DONE)
 - Need to support a concept of a non-localized Double (e.g. for example latitude which commonly does not use locale specific decimal separator) (DONE)
 - Address detection in Western Europe (DONE)
 - Should support localized true/false?
 - Broaden languages supported for FREE_TEXT
 - Add Japanese Postal Code support (DONE)

# Merge sharded analyses (DONE)
Option to merge profile results (e.g. given two profiles from sharded sources - produce a single profile result).

# Support Trace Replay (DONE)
 - Support replay via .fta files

# Split jars so date detection is lightweight (DONE)
 - Split fta.jar into two so date analysis can be performed with limited dependencies.

# Add support for Distinct count (DONE)
