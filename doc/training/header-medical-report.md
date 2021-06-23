# Annotation guidelines for the _header-medical-report_ model

## Introduction

For the following guidelines, we first need to generate the training data as explained [here](../Training-the-medical-report-models.md#generation-of-training-data).

In __grobid-medical-report__, the document __header-medical-report__ corresponds to the metadata information sections about the document. This is typical information that can be found at the beginning of the article (i.e., `front`).

For identifying the exact pieces of information to be part of the `header-medical-report` segments, see the [Annotation guidelines of the medical-report-segmenter model](medical-report-segmenter.md).

For the header-medical-report model, we use the following TEI elements:

* `<idno>` for the strong identifiers of the document 
* `<note type="doctype">` for indication on the document type
* `<titlePart>` for the document title
* `<date>` for the date
* `<time>` for the time information
* `<dateline>` for the date containing also the location (e.g., "Paris, le 27 novembre 2020")
* `<medic>` for the list of medics
* `<patient>` the list of patients
* `<affiliation>` for the affiliation information
* `<address>` for the address elements of affiliations
* `<org>` for the information regarding identifiable organization
* `<email>` for the email information of affiliations
* `<phone>` for the phone number of affiliations
* `<fax>` for the fax number of affiliations
* `<ptr type="web">` for the web URL 


> Note that the mark-up follows approximatively the [TEI](http://www.tei-c.org) when used for inline encoding. 

Encoding the header section is challenging because of the variety of information that appears in this section can be in unexpected and overlapped manners. Some information is often redundant (for example, medics, patients, and affiliations can be mentioned several times among different levels of details). These annotation guidelines are thus particularly important to follow to ensure stable encoding practices in the complete training data and to avoid the machine learning models learn contradictory labeling resulting in poorer performance and less valuable training data.
> Note: It is recommended to study first the existing training documents for the __header-medical-report__ model (`grobid/grobid-trainer/resources/dataset/header-medical-report`) to see some examples of how these elements should be used.


## Analysis

The following sections provide detailed information and examples on how to handle certain typical cases.

### Space and new lines

Spaces and a new line in the XML annotated files are not significant and will be all considered by the XML parser as the default separator. So it is possible to add and remove freely space characters and new lines to improve the readability of the annotated document without any impacts. 

Similarly, line break tags `<lb/>` are present in the generated XML training data, but they will be considered as a default separator by the XML parser. They are indicated to help the annotator to identify a piece of text in the original PDF if necessary. Actual line breaks are identified in the PDF and added by aligning the XML/TEI with the feature file generated in parallel which contains all the PDF layout information. 

### Strong identifiers

[\<idno\>](https://tei-c.org/release/doc/tei-p5-doc/en/html/ref-idno.html) is used to identify strong identifiers of the document. 

The identifier name is kept with the identifier value so that Grobid can classify more easily the type of identifier:

```xml
    Concernant examen numéro<idno type="Examen">19A181001</idno>
```

### Document types

Indication of document types is labeled with `<note>`. These indications depend on the editor, document source, etc. We consider as _document type_ the nature of the document (article, review, editorial, etc.), but also some specific aspects that can be highlighted in the presentation format, for instance, indication of an "Open Access" publication expressed independently form the copyrights to characterize the document.

```xml
    <note type="doctype">Ordonnance<lb/></note>
    , Imprimé le <date>10/04/2020</date> <time>18:57</time> <lb/>
```

### Title

Title encoding is realized following the TEI inline scheme:

```xml
    <docTitle>
        <titlePart>Compte-rendu de consultation<lb/></titlePart>
    </docTitle>
```

Subtitles are labeled similarly as a title but as an independent field. It's important to keep a break (in term of XML tagging) between the main title and possible subtitles, even if there are next to each other in the text stream.

Running titles are not labeled at all.

```xml
    <address>Villejuif, France<lb/></address>

    Running title: HBsAg quantification in anti-HBs positive HBV carriers<lb/>
```

In the case of an article written in non-English language having an additional English title as a translation of the original title, we annotate the English title with a tag `<note type="english-title">`.

### Dates
Dates are enclosed in a [\<date\>](https://www.tei-c.org/release/doc/tei-p5-doc/en/html/examples-date.html) element. Additional text or characters that do not belong to this specific element (e.g., the word "date", punctuations) must be left untagged.

For example:
```xml
    Examen reçu le : <date>01/01/2020</date> <lb/>
```

### Times
Times are enclosed in a [\<time\>](https://www.tei-c.org/release/doc/tei-p5-doc/en/html/examples-date.html) element. Additional text or characters that do not belong to this specific element (e.g., punctuations) must be left untagged.

For example:
```xml
    Examen reçu le : <date>05/11/2015</date> <time>15:50</time> <lb/>
```

### Datelines
[\<dateline\>](https://www.tei-c.org/release/doc/tei-p5-doc/en/html/examples-dateline.html) contains a brief description of the place, date, time of production of a letter or other work, prefixed or suffixed to it as a kind of heading or trailer.

For example:
```xml
    <dateline>Paris, le 24 mai 2006</dateline> <lb/>
```

### Medics

All mentions of medics (i.e. medical practitioners) are labeled with `<medic>`, including possible repetition of the medics in the correspondence section. The medic information might be more detailed in the correspondence part and it will be then part of the job of Grobid to identify repeated medics and to "merge" them.
The information contained therein will be extracted further by the [name-medic](medic.md) model.

```xml
    <person>
        <medic>
            Docteur Nathalie DUPONT (MCU-PH) <lb/>
            Tel. 07.12.12.12.12 <lb/>
            nathalie.dupont@aphp.fr <lb/>
            Chef de service <lb/>
        </medic>
    </person>
```

As illustrated above, titles and roles (e.g. Ph.D., MD, Dr., MCU-PH, PH, Chef de service), addresses, emails, phones must be **included** in the medic field.

### Patients

All mentions of patients are tagged as `<patient>`. Titles (e.g. Mme., Mr.), birth date, weight and height, social security numbers and other identifiers (IPP, NDA), emails, addresses of patients are included in the field.

```xml
    <person>
    	<patient>
            Mme Carole MARTIN<lb/>
            Poids : 50.0 kg; Taille : 170 cm
            N°d'immatriculation : 2 00 12 34 56 78 91 01
            carole.martin@yahoo.fr <lb/>
            9 boulevard des coquibus <lb/>
            chez COALLIA <lb/>
            91000 EVRY <lb/>
        </patient>
    </person>
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

### Organization

All the mentions of organizations are labeled under <org>. [Organization](https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-org.html) provides information about an identifiable organizational structure.
The information contained therein will be extracted further by the [organization-medical-report](organization-medical-report.md) model.

```xml
    <org>
        Hôpital de jour : <lb/>
        Pr Daniel DUPONT (PH) <lb/>
        (Chef du service) <lb/>
        Accueil Tel : 01.12.34.56.78 <lb/>
        Sécrétariat Fax : 01.23.34.56.78 <lb/>
    </org>
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

