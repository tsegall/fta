# Merge sharded analyses
Option to merge profile results (e.g. given two profiles from sharded sources - produce a single profile result).

# Split jars so date detection is lightweight
Split fta.jar into two so date analysis can be performed with limited dependencies.

# DataSet attributes
 - Identify recency fields (e.g. tlm_dt)
 - Identify reference/lookup tables
 - Improve FK detection
 - Coalesce multiple fields into concept, e.g.
	 - Honorific, First, Last, Middle into Name
	 - Address Line 1, Address Line2, Zip, State, Country into Full Address

# Field attributes
- Improve PII detection (e.g. DOB, Age, ...)
 - Improve Semantic Type detection (e.g. Age field as AGE, Description fields)
 - Outlier detection in numeric fields
 - Data distribution (e.g. Normal, uniform, ...)
 - Improved anomaly detection (see https://www.sciencedirect.com/science/article/pii/S0925231217309864)

# Broaden Identities supported
 - Chinese SSN
 - Belgian SSN
 - German SSN
 - ...

# Improve International Postal Code support
 - Add Japanese Postal Code support
 - ...

# Internal
 - Support replay via .fta files

