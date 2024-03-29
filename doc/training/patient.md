# Annotation guidelines for the _patient_ model

## Introduction
Before performing this procedure, pre-annotated data must be prepared as explained in [generate new datasets](../Training-the-medical-report-models.md#generate-new-datasets).

To identify the patient (`<patient>`) element, see the annotation guidelines of the [header-medical-report](header-medical-report.md) and [full-medical-text](full-medical-text) models.

For the patient model, we use the following TEI elements:

* `<idno>` for the ID number of patients
* `<idType>` for the ID types
* `<persName>` for the name of patients
* `<sex>` for the sex type of patients
* `<birthDate>` for the date of birth
* `<birthPlace>` for the place of birth of patients
* `<age>` for the age
* `<death>` for the date of death
* `<address>` for the address elements of patients
* `<country>` for the country 
* `<settlement>` for the city
* `<phone>` for the phone number
* `<email>` for the email if it exists 
* `<note type="patient">` for the notes concerning the patients

> Notes:
- The mark-up follows approximately the [TEI](http://www.tei-c.org) format.
- It is therefore recommended to see some examples of how these elements should be used for the __patient__ model in `grobid/grobid-trainer/resources/dataset/patient/corpus/` or `grobid/grobid-trainer/resources/dataset/patient/evaluation/`.

## Analysis

The following sections provide detailed information and examples on how to handle certain typical cases.

### Space and new lines

Spaces and a new line in the XML annotated files are not significant and will be all considered by the XML parser as the default separator. So it is possible to add and remove freely space characters and new lines to improve the readability of the annotated document without any impacts. 

Similarly, line break tags `<lb/>` are present in the generated XML training data, but they will be considered as a default separator by the XML parser. They are indicated to help the annotator to identify a piece of text in the original PDF if necessary. Actual line breaks are identified in the PDF and added by aligning the XML/TEI with the feature file generated in parallel which contains all the PDF layout information. 


### Strong identifiers

[\<idno\>](https://tei-c.org/release/doc/tei-p5-doc/en/html/ref-idno.html) is used to identify strong identifiers of the document.

The identifier name is kept with the identifier value so that Grobid can classify more easily the type of identifier:

```xml
    <idType>NIP</idType> : <idno>1234567890</idno>
```

### Personal names

All the mentions of person names are labeled under `<persName>`. [Person name](https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-persName.html) provides information about an identifiable person name.


```xml
    <persName>Madame Chaterine BELLE</persName>
```

As illustrated above, titles and roles (e.g. Madame, Monsieur) must be **included** in the patient field.


### Sex type

All the mentions of sex type are labeled under `<sex>`. [Sex type](https://tei-c.org/release/doc/tei-p5-doc/en/html/ref-sex.html) provides information about an identifiable sex type.


```xml
    <sex>F</sex>
```


### Address

All the mentions of affiliations are labeled, including in the correspondence parts. Grobid will have to merge appropriately redundant affiliations. It's important to keep markers inside the labeled fields because they are used to associate the right affiliations to the medics.

Addresses are labeled with their own tag `<address>`.

```xml
    <address>
        1 Avenue du Général Leclerc <lb/>
        chez GAMBETA <lb/>
        75014 Paris <lb/>
    </address>
```

### Phone numbers

Phone numbers including international prefix symbols are labeled with `<phone>`. Punctuation and words (e.g., "phone", "telephone") must be excluded from the label field.

```xml
    Tel.: <phone>+33 123456789</phone>.<lb/> 
```
