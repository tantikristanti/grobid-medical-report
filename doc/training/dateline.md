# Annotation guidelines for the _dateline_ model

## Introduction

Before performing this procedure, pre-annotated data must be prepared as explained in [generate new datasets](../Training-the-medical-report-models.md#generate-new-datasets).

To identify the dateline (`<dateline>`) element, see the annotation guidelines of the [header-medical-report](header-medical-report.md) model.

For the dateline model, we use the following TEI elements:

* `<date>` for the date
* `<time>` for the time information
* `<placeName>` for the place names
* `<note>` for the type of the dates
  * `<note type="doctype">` for the document type
  * `<note type="date">` for short notes of date

> Notes:
- The mark-up follows approximately the [TEI](http://www.tei-c.org) format.
- It is therefore recommended to see some examples of how these elements should be used for the __dateline__ model in `grobid/grobid-trainer/resources/dataset/dateline/corpus/` or `grobid/grobid-trainer/resources/dataset/dateline/evaluation/`.

## Analysis

The following sections provide detailed information and examples on how to handle certain typical cases.


### Dates
For dates, we follow Grobid's Date model and rules, as can be seen in the [Date Model](https://grobid.readthedocs.io/en/latest/training/date/) documentation.
Dates are enclosed in a [\<date\>](https://www.tei-c.org/release/doc/tei-p5-doc/en/html/examples-date.html) element. Additional text or characters that do not belong to this specific element (e.g., the word "date", punctuations) must be left untagged.

For example:
```xml
    Examen reçu le : <dateline><date>01/01/2020</date> <lb/> à <placeName>Paris</placeName></dateline> 
```

### Times
Times are enclosed in a [\<time\>](https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-time.html) element. Additional text or characters that do not belong to this specific element (e.g., punctuations) must be left untagged.

For example:
```xml
    Examen reçu le : <dateline><date>05/11/2015</date> à <time>15:50</time> </dateline><lb/>
```

### Place names
Place names are enclosed in a [\<placeName\>](https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-placeName.html) element. 

For example:
```xml
    <dateline><placeName>Paris</placeName>, <date>le 24 mai 2006</date></dateline> <lb/>
```

### Notes
Notes are enclosed in a [\<note\>](https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-note.html) element.

For example:
```xml
    <dateline><note>Date de l'examen</note> : <date>le 06 janvier 2000</date></dateline> <lb/>
```
