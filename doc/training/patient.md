# Annotation guidelines for the _patient_ model

## Introduction

For the following guidelines, we first need to generate the training data as explained [here](../Training-the-medical-report-models.md#generation-of-training-data).

In __grobid-medical-report__, __patient__ corresponds to the patient information. 

For identifying the exact pieces of information to be part of the `header-medical-report`, see the [Annotation guidelines of the header-medical-report model](header-medical-report.md).

For the patient model, we use the following TEI elements:

* `<persName>` for the name of patients
* `<address>` for the address elements of patients  
* `<email>` for the email information
* `<phone>` for the phone number

> Note that the mark-up follows approximatively the [TEI](http://www.tei-c.org) when used for inline encoding.
> It is recommended to study first the existing training documents for the __patient__ model (`grobid/grobid-medical-report/resources/dataset/patient`) to see some examples of how these elements should be used.


## Analysis

The following sections provide detailed information and examples on how to handle certain typical cases.

### Space and new lines

Spaces and a new line in the XML annotated files are not significant and will be all considered by the XML parser as the default separator. So it is possible to add and remove freely space characters and new lines to improve the readability of the annotated document without any impacts. 

Similarly, line break tags `<lb/>` are present in the generated XML training data, but they will be considered as a default separator by the XML parser. They are indicated to help the annotator to identify a piece of text in the original PDF if necessary. Actual line breaks are identified in the PDF and added by aligning the XML/TEI with the feature file generated in parallel which contains all the PDF layout information. 

### Personal names

All the mentions of person names are labeled under `<persName>`. [Person name](https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-persName.html) provides information about an identifiable person name.


```xml
    <persName>Madame Chaterine BELLE</persName>
```

As illustrated above, titles and roles (e.g. Madame, Monsieur) must be **included** in the patient field.

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

### Emails

Email must be tagged in a way that is limited to an actual [\<email\>](https://tei-c.org/release/doc/tei-p5-doc/en/html/ref-email.html), excluding the "Email" word, punctuations, and person name information.

```xml
    Email: 
    <email>chaterine.belle@google.com</email> 
```   

### Phone numbers

Phone numbers including international prefix symbols are labeled with `<phone>`. Punctuation and words (e.g., "phone", "telephone") must be excluded from the label field.

```xml
    Tel.: <phone>+33 123456789</phone>.<lb/> 
```
