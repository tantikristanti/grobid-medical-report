# Annotation guidelines for the _medic_ model

## Introduction

For the following guidelines, we first need to generate the training data as explained [here](../Training-the-medical-report-models.md#generation-of-training-data).

In __grobid-medical-report__, __medic__ corresponds to the medic information. 

For identifying the exact pieces of information to be part of the `header-medical-report` or `left-note-medical-report`, see the [Annotation guidelines of the left-note-medical-report model](left-note-medical-report.md).

For the medic model, we use the following TEI elements:

* `<idno>` for the ID of medics (ex., N° RPPS)
* `<roleName>` for the role of medics
* `<persName>` for the name of medics 
* `<affiliation>` for the affiliation attached directly to medics
* `<orgName>` for the organization attached directly to medics
* `<institution>` for the institution attached directly to medics
* `<address>` for the address of medics
* `<country>` for the country
* `<settlement>` for the city
* `<email>` for the email information
* `<phone>` for the phone number
* `<fax>` for the fax number
* `<ptr type="web">` for the web URL 
* `<note type="short">` for the notes concerning the medics

> Note that the mark-up follows approximatively the [TEI](http://www.tei-c.org) when used for inline encoding.
> It is recommended to study first the existing training documents for the __medic__ model (`grobid/grobid-trainer/resources/dataset/medic`) to see some examples of how these elements should be used.


## Analysis

The following sections provide detailed information and examples on how to handle certain typical cases.

### Space and new lines

Spaces and a new line in the XML annotated files are not significant and will be all considered by the XML parser as the default separator. So it is possible to add and remove freely space characters and new lines to improve the readability of the annotated document without any impacts. 

Similarly, line break tags `<lb/>` are present in the generated XML training data, but they will be considered as a default separator by the XML parser. They are indicated to help the annotator to identify a piece of text in the original PDF if necessary. Actual line breaks are identified in the PDF and added by aligning the XML/TEI with the feature file generated in parallel which contains all the PDF layout information. 

### Personal names

All the mentions of person names are labeled under `<persName>`. [Person name](https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-persName.html) provides information about an identifiable person name.


```xml
    <persName>Docteur Nathalie DUPONT (MCU-PH)</persName>
```

As illustrated above, titles and roles (e.g. Ph.D., MD, Dr., MCU-PH, PH, Chef de service) must be **included** in the medic field.


### Emails

Email must be tagged in a way that is limited to an actual [\<email\>](https://tei-c.org/release/doc/tei-p5-doc/en/html/ref-email.html), excluding the "Email" word, punctuations, and person name information.

```xml
    Email: 
    <email>nathalie.dupont@aphp.fr</email> 
```   

### Phone numbers

Phone numbers including international prefix symbols are labeled with `<phone>`. Punctuation and words (e.g., "phone", "telephone") must be excluded from the label field.

```xml
    Tel.: <phone>+46 31 7866104</phone>.<lb/> 
```

### Fax numbers

Fax numbers including international prefix symbols are labeled with `<fax>`. Punctuation and words (e.g., "fax") must be excluded from the label field. 

```xml
    Fax : <fax>01 44 38 18 80</fax> <lb/>
```

### Web URL
Web URLs are enclosed in [\<ptr\>](https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-ptr.html). Punctuation and words like "web", "url" must be excluded from the label field. 

```xml
    <ptr type="web">http://nathalie.dupont.aphp.fr</ptr> <lb/>
```
