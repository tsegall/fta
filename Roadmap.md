# DataSet attributes
 - Identify recency fields (e.g. tlm_dt)
 - Identify reference/lookup tables
 - Improve FK detection
 - Coalesce multiple fields into concept, e.g.
	 - Honorific, First, Last, Middle into Name
	 - Address Line 1, Address Line2, Zip, State, Country into Full Address

# Field attributes
 - Improve PII detection (e.g. DOB, Age, ...)
 - Improve Semantic Type detection (e.g. Age field as AGE or AGE_RANGE, Race)
 - Outlier detection in numeric fields
 - Improved anomaly detection (see https://www.sciencedirect.com/science/article/pii/S0925231217309864)
 - Add Filename detection
 - Add ContentFormat attribute: e.g. JSON, XML, base64 encoded, encrypted?
 - Add Distribution attribute: uniform, normal, random
 - Distinguish between HOME/WORK attributes (e.g. EMAIL, PHONE, ADDRESS)

# Broaden Identities supported
 - Chinese SSN
 - Belgian SSN
 - German SSN
 - ...

# Improve International Postal Code support
 - Add Japanese Postal Code support
 - ...

# Generalize Bloom Filter support for large static reference lists

# Improve Date Detection

# Improve Internationalization
 - Headers RegExp should be per locale (DONE)
 - "regExpsToMatch", "regExpReturned", "isRegExpComplete" should be per locale (DONE)
 - Gender support needs rework (DONE)
 - Need to support a concept of a non-localized Double (e.g. for example latitude which commonly does not use locale specific decimal separator) (DONE)
 - Address detection in Western Europe
 - Should support localized true/false?

# Merge sharded analyses (DONE)
Option to merge profile results (e.g. given two profiles from sharded sources - produce a single profile result).

# Support Trace Replay (DONE)
 - Support replay via .fta files

# Split jars so date detection is lightweight (DONE)
 - Split fta.jar into two so date analysis can be performed with limited dependencies.

# Add support for Distinct count (DONE)
