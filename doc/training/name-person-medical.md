# Annotation guidelines for the _name person_ model

## Introduction
Before performing this procedure, pre-annotated data must be prepared as explained in [generate new datasets](../Training-the-medical-report-models.md#generate-new-datasets).

To identify the person name (`<persName>`) element, see the annotation guidelines of the [medic](medic.md) and [patient](patient.md) models.

For the person's name model, we use the following TEI elements:

* `<title>` for the titles 
* `<forename>` for the first name
* `<middlename>` for the middle name
* `<surname>` for the last name 
* `<suffix>` for the suffixs

> Notes:
- The mark-up follows approximately the [TEI](http://www.tei-c.org) format.
- It is therefore recommended to see some examples of how these elements should be used for the __name-person-medical__ model in `grobid/grobid-trainer/resources/dataset/name/person-medical/corpus/` or `grobid/grobid-trainer/resources/dataset/name/person-medical/evaluation/`.

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
