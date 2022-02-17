# Annotation guidelines for the _left-note-medical-report_ model

## Introduction

For the following guidelines, we first need to generate the training data as explained [here](../Training-the-medical-report-models.md#generation-of-training-data).

In __grobid-medical-report__, the document __left-note-medical-report__ corresponds to information of hospital organizational structure.

For identifying the exact pieces of information to be part of the `left-note-medical-report` segments, see the [Annotation guidelines of the medical report segmenter model](medical-report-segmenter.md).

For the left-note-medical-report model, we use the following TEI elements:

* `<idno>` for the strong identifiers of the document (ex. no. FINESS)
* `<affiliation>` for the affiliation information
* `<org>` for the information regarding identifiable organization
    * `<org type="ghu">` for the university hospital group (GHU, Groupement hospitalier universitaire)
    * `<org type="chu">` for the university hospital center (CHU, Centre hospitalier universitaire)
    * `<org type="dmu">` for the medical university departments  (DMU, Départements Médico-Universitaires)
    * `<org type="pole">` for the pole type
    * `<org type="institution">` for the institution type
    * `<org type="site">` for the site type
    * `<org type="hospital">` for the hospital type
    * `<org type="center">` for the center type
    * `<org type="service">` for the service type
    * `<org type="department">` for the department type
    * `<org type="unit">` for the unit type
    * `<org type="sub">` for the suborganization type
    * `<org type="other">` for the other organization type
* `<address>` for the address elements of affiliations
* `<email>` for the email information of affiliations
* `<phone>` for the phone number of affiliations
* `<fax>` for the fax number of affiliations
* `<ptr type="web">` for the web URL 
* `<medic>` for the list of medics

> Note that the mark-up follows approximatively the [TEI](http://www.tei-c.org) when used for inline encoding. 

Encoding the left-note section is challenging because of the variety of information that appears in this section can be in unexpected and overlapped manners (e.g., the overlapped information in affiliation and organization). Some information is often redundant (for example, medics can be mentioned several times among different levels of details). These annotation guidelines are thus particularly important to follow to ensure stable encoding practices in the complete training data and to avoid the machine learning models learn contradictory labeling resulting in poorer performance and less valuable training data.
> Note: It is recommended to study first the existing training documents for the __left-note-medical-report__ model (`grobid/grobid-trainer/resources/dataset/left-note-medical-report`) to see some examples of how these elements should be used.

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

### Affiliation and address

All the mentions of affiliations are labeled, including in the correspondence parts. Grobid will have to merge appropriately redundant affiliations. It's important to keep markers inside the labeled fields because they are used to associate the right affiliations to the medics.

Addresses are labeled with their own tag `<address>`.

```xml
    <byline>
        <affiliation>CENTRE HOSPITALIER UNIVERSITAIRE HENRI MONDOR<lb/></affiliation>
    </byline>

    <address>51 Avenue du Maréchal de Lattre de Tassigny <lb/>94010 CRETEIL</address> <lb/>
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

### Organization

All the mentions of organizations are labeled under <org>. [Organization](https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-org.html) provides information about an identifiable organizational structure.
The information contained therein will be extracted further by the [organization-medical-report](organization.md) model.

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
    Email: Hépatologie : <lb/>
    <email>hépatologie@aphp.fr</email> 
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
