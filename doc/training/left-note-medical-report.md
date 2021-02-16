# Annotation guidelines for the _left-note-medical-report_ model

## Introduction

For the following guidelines, we first need to generate the training data as explained [here](../Training-the-medical-report-models/#generation-of-training-data).

In __grobid-medical-report__, the document __header-medical-report__ corresponds to the metadata information sections about the document. This is typical information that can be found at the beginning of the article (i.e., `front`).

For identifying the exact pieces of information to be part of the `header-medical-report` segments, see the [Annotation guidelines of the medical report segmenter model](medical-report-segmenter.md).

For the left-note medical report model, we use the following TEI elements:

* `<medic>` for the medics list 
* `<affiliation>` for the affiliation information
* `<placeName>` for the place names
* `<address>` for the address elements
* `<email>` for the email information
* `<phone>` for the phone number
* `<fax>` for the fax number
* `<ptr type="web">` for the web URL 

> Note that the mark-up follows approximatively the [TEI](http://www.tei-c.org) when used for inline encoding. 

Encoding the header section is challenging because of the variety of information that appears in this section can be in unexpected and overlapped manners. Some information is often redundant (for example, medics, patients, and affiliations can be mentioned several times among different levels of details). These annotation guidelines are thus particularly important to follow to ensure stable encoding practices in the complete training data and to avoid the machine learning models learn contradictory labeling resulting in poorer performance and less valuable training data.
> Note: It is recommended to study first the existing training documents for the __header-medical-report__ model (`grobid/grobid-medical-report/resources/dataset/header-medical-report`) to see some examples of how these elements should be used.


## Analysis

The following sections provide detailed information and examples on how to handle certain typical cases.

### Space and new lines

Spaces and a new line in the XML annotated files are not significant and will be all considered by the XML parser as the default separator. So it is possible to add and remove freely space characters and new lines to improve the readability of the annotated document without any impacts. 

Similarly, line break tags `<lb/>` are present in the generated XML training data, but they will be considered as a default separator by the XML parser. They are indicated to help the annotator to identify a piece of text in the original PDF if necessary. Actual line breaks are identified in the PDF and added by aligning the XML/TEI with the feature file generated in parallel which contains all the PDF layout information. 

### Medics

All mentions of the medics (i.e. medical practitioners) are labeled with `<medic>`, including possible repetition of the medics in the correspondence section. The medic information might be more detailed in the correspondence part and it will be then part of the job of Grobid to identify repeated medics and to "merge" them.

```xml
    <person>
        <medic>Docteur Nathalie DUPONT</medic>
    </person>
```

As illustrated above, titles like "Ph.D.", "MD", "Dr.", etc. must be **included** in the medic field. 

The only exception is when the indication of medics is given around an email or a phone number. In this case, we consider that the occurrence of medic names (including abbreviated names) is purely for practical reasons and should be ignored.
```xml
    Email: Calum J Maclean* -
     <email>calum.maclean@ucl.ac.uk</email>; 
```   


Full job names like "Head of...", "Chef de service..." should be excluded when possible (i.e. when it does not break the sequence) from the tagged field.

```xml
    <person>
    	<medic>Dr Agnès DUPONT<lb/> Néonatologie</medic>
    </person> 

    Chef de service <lb/>
```

### Affiliation and address

All the mentions of affiliations are labeled, including in the correspondence parts. Grobid will have to merge appropriately redundant affiliations. It's important to keep markers inside the labeled fields because they are used to associate the right affiliations to the medics.

Addresses are labeled with their own tag `<address>`. 

```xml
    <byline>
        <affiliation>CENTRE HOSPITALIER UNIVERSITAIRE HENRI MONDOR<lb/></affiliation>
    </byline>

    <address>51 Avenue du Maréchal de Lattre de Tassigny <lb/>94010 CRETEIL</address> <lb/>
```

### Emails

Email must be tagged in a way that is limited to an actual [\<email\>](https://tei-c.org/release/doc/tei-p5-doc/en/html/ref-email.html), excluding the "Email" word, punctuations, and person name information.

```xml
    Email: Ren H Wu -
    <email>wurh20000@sina.com</email> 
```   

### Phone numbers

Phone numbers including international prefix symbols are labeled with `<phone>`. Punctuation and words (e.g., "phone", "telephone") must be excluded from the label field.

```xml
    <address>Box 457, SE 405 30<lb/> Göteborg, Sweden.</address>

    Tel.: <phone>+46 31 7866104</phone>.<lb/> 

    E-mail address: <email>eva.brink@gu.se</email>
```

### Fax numbers

Fax numbers including international prefix symbols are labeled with `<fax>`. Punctuation and words (e.g., "fax") must be excluded from the label field. 

```xml
    Secrétariat Général : <lb/>
    Tél : <phone>01 44 38 18 64 /68/69</phone> <lb/>
    Fax : <fax>01 44 38 18 80</fax> <lb/>
```

### Web URL
Web URLs are enclosed in [\<ptr\>](https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-ptr.html). Punctuation and words like "web", "url" must be excluded from the label field. 

```xml
    <byline>
            <affiliation>Service d&apos;Oncologie Médicale <lb/>
            Hôpital Saint-Antoine</affiliation>
    </byline> 
    -
    <address>184 rue du Fg Saint-Antoine -75571 Paris Cx 12</address> <lb/>
    <email>oncologie.saint-antoine@sat.aphp.fr</email> 
    <ptr type="web">http://www.oncosat.com</ptr> <lb/>
```
