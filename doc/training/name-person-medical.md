# Annotation guidelines for the _name person_ model

## Introduction

For the following guidelines, we first need to generate the training data as explained [here](../Training-the-medical-report-models.md#generation-of-training-data).

In __grobid-medical-report__, __name_person__ corresponds to the person's name information. 

For identifying the exact pieces of person's name (`medic` or `patient`), see the Annotation guidelines of the <persName> element in the [medic](medic.md) or [patient](patient.md) models.

For the person's name model, we use the following TEI elements:

* `<title>` for the titles 
* `<forename>` for the first name
* `<middlename>` for the middle name
* `<surname>` for the last name 
* `<suffix>` for the suffixs

> Note that the mark-up follows approximatively the [TEI](http://www.tei-c.org) when used for inline encoding.
> It is recommended to study first the existing training documents for the __medic__ model (`grobid/grobid-trainer/resources/dataset/medic`) to see some examples of how these elements should be used.


## Analysis

The following sections provide detailed information and examples on how to handle certain typical cases.

### Space and new lines

Spaces and a new line in the XML annotated files are not significant and will be all considered by the XML parser as the default separator. So it is possible to add and remove freely space characters and new lines to improve the readability of the annotated document without any impacts. 

Similarly, line break tags `<lb/>` are present in the generated XML training data, but they will be considered as a default separator by the XML parser. They are indicated to help the annotator to identify a piece of text in the original PDF if necessary. Actual line breaks are identified in the PDF and added by aligning the XML/TEI with the feature file generated in parallel which contains all the PDF layout information. 

### Person's name 

All the mentions of person names are labeled under `<persName>`. [Person name](https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-persName.html) provides information about an identifiable person name.


```xml
    <persName><title>Docteur</title> <forename>Nathalie</forename> <surname>DUPONT</surname> (<suffix>MCU-PH</suffix>)</persName>
```
