# Annotation guidelines for the _organization-medical-report_ model

## Introduction
Before performing this procedure, pre-annotated data must be prepared as explained in [generate new datasets](../Training-the-medical-report-models.md#generate-new-datasets).

To identify the organization (`<org>`) element, see the annotation guidelines of the [header-medical-report](header-medical-report.md) and [left-note-medical-report](left-note-medical-report) models.

For the organization model, we use the following TEI elements:

* `<affiliation>` for the affiliation name (i.e., the hospital name)
* `<orgName>` for the organization name
  * `<orgName type="ghu">` for the university hospital group (GHU, Groupement hospitalier universitaire)
  * `<orgName type="chu">` for the university hospital center (CHU, Centre hospitalier universitaire)
  * `<orgName type="dmu">` for the medical university departments  (DMU, Départements Médico-Universitaires)
  * `<orgName type="pole">` for the pole type
  * `<orgName type="institution">` for the institution type
  * `<orgName type="university">` for the university type
  * `<orgName type="site">` for the site type
  * `<orgName type="hospital">` for the hospital type
  * `<orgName type="center">` for the center type
  * `<orgName type="service">` for the service type
  * `<orgName type="department">` for the department type
  * `<orgName type="unit">` for the unit type
  * `<orgName type="sub">` for the sub-organization type
  * `<orgName type="other">` for the other organization type
* `<address>` for the address elements of affiliations
* `<country>` for the country
* `<settlement>` for the city
* `<email>` for the email information of affiliations
* `<phone>` for the phone number of affiliations
* `<fax>` for the fax number of affiliations
* `<ptr type="web">` for the web URL
* `<note type="short">` for the any short notes in the header part

> Notes:
- The mark-up follows approximately the [TEI](http://www.tei-c.org) format.
- It is therefore recommended to see some examples of how these elements should be used for the __organization__ model in `grobid/grobid-trainer/resources/dataset/organization/corpus/` or `grobid/grobid-trainer/resources/dataset/organization/evaluation/`.

## Analysis

The following sections provide detailed information and examples on how to handle certain typical cases.

### Space and new lines

Spaces and a new line in the XML annotated files are not significant and will be all considered by the XML parser as the default separator. So it is possible to add and remove freely space characters and new lines to improve the readability of the annotated document without any impacts. 

Similarly, line break tags `<lb/>` are present in the generated XML training data, but they will be considered as a default separator by the XML parser. They are indicated to help the annotator to identify a piece of text in the original PDF if necessary. Actual line breaks are identified in the PDF and added by aligning the XML/TEI with the feature file generated in parallel which contains all the PDF layout information. 

### Organization names

All the mentions of organization names are labeled under `<orgName>`. [Place](https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-org.html) provides information about an identifiable organization name.

```xml

    <orgName>Hôpital de jour</orgName> : <lb/>
    Tel : (33) 01.02.03.04.05
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

### Place names

All the mentions of place names are labeled under `<place>`. [Place](https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-place.html) provides information about an identifiable place name.

```xml
    <place>
        Chirurgie CHB1 : 01.12.13.14.15 <lb/>
    </place>
```
