# Semantic Type Detection #

Semantic type detection aims to identify the meaning of a data field. For instance, a field could have a data type of "String", but the semantic type might be "First Name".

By establishing correspondences with real-world concepts, semantic types can provide detailed data descriptions and improve data preparation and information retrieval tasks such as data cleansing, schema matching, semantic search, and anomaly detection.

Examples of semantic types include:

- First Name (See https://www.wikidata.org/wiki/Property:P735)
- Birth Date (See https://www.wikidata.org/wiki/Property:P569)
- Phone number (See https://en.wikipedia.org/wiki/Telephone_number and https://www.wikidata.org/wiki/Property:P1329)
- Email address (See https://https://en.wikipedia.org/wiki/Email_address and https://www.wikidata.org/wiki/Property:P968)
- Country Code (See https://en.wikipedia.org/wiki/ISO_3166-1_alpha-3 and https://www.wikidata.org/wiki/Property:P298)

Semantic types typically have an underlying base type which is either numerical (e.g. Long, Double), categorical, temporal (e.g. Date, Time), or boolean.

Semantic types are either closed (i.e. they are from a finite set) or open (from an infinite set).
For example ISO 3166-1 alpha-3 is a finite set of three-letter country codes whereas 'First Name' can include any string of characters ('X Æ A-Xii' is an example of a recently minted first name).

Semantic type detection can be used to improve the accuracy and efficiency of data processing tasks.
For example, if a system knows that a column contains a Country Code then it can automatically validate that the values are valid.
Similarly, if a system knows that a column contains a Birth Date, it can automatically format the dates in a consistent way and potentially flag values as invalid, e.g. the Policy holder can not have been born 150 years ago.

Semantic type detection can also be used to improve the usability of data.
For example, if a system knows that a column contains an email address, it may display the values with an appropriate link.

Semantic Types typically belong to one of three classes.
- Domain/industry agnostic: these include many widely recognized Semantic Types (e.g., Email Address, First Name, Last Name, Age, ...). 
- Industry-specific: For example, ICD-10 is used as a medical classification scheme by the World Health Organization)
- Company/Organization specific: For example, the policy holder number for your Insurance company.

# Detection Approaches #

There are a number of different approaches to semantic type detection. The most common approaches include:

- List lookup 
- Regular expression matching
- Code-based 
- Machine learning

List lookup is the simplest approach to semantic type detection. It involves comparing the values to a pre-defined valid set.

Regular expression matching is a more sophisticated approach to semantic type detection.
For example you could use \d{3}-\d{2}-\d{4} for a naive regular expression to match a US Social Security Number.
There are two possible approaches to using regular expression matching, either you compare the data to a known regular expression to determine it validity or you use the data to generate a regular expression and hence determine its semantic type.

Code-based matching is particularly effective for unbounded semantic types with structure - e.g. a Credit Card number which has a Check Digit.
The obvious downside to code-based implementations is that there is significant implementation effort.

Machine learning is the most sophisticated approach to semantic type detection.
It involves training a machine learning model to identify the semantic types of a data stream.
This approach is potentially highly accurate, but it typically depends on sample data, known as [training data].

Independent of the approach taken the semantic type detection can be guided by additional context, for example the column name, or the proximity of this column to another column with a given Semantic Type.



