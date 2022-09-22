# Annotation guidelines for the _french-medical-ner_ model

## Introduction

For the following guidelines, firstly we need to generate the training data.

1. Create blank training data from the body part

```bash
$ java -Xmx4G -jar build/libs/grobid-medical-report-0.0.1-onejar.jar -gH ../grobid-home -dIn ~/path_to_input_directory/ -dOut ~/path_to_output_directory -exe createTrainingBlankFrenchMedicalNER
```

2. Create the training data using the pre-trained model

```bash
$ java -Xmx4G -jar build/libs/grobid-medical-report-0.0.1-onejar.jar -gH ../grobid-home -dIn ~/path_to_input_directory/ -dOut ~/path_to_output_directory -exe createTrainingFrenchMedicalNER
```

For determining mentions, we use the largest entity mentions and does not handle nested entities as explained in [grobid-ner](https://grobid-ner.readthedocs.io/en/latest/largest-entity-mention/).

For the French medical NER model, we use the following TEI elements:

* `<anatomy>` for anatomy
* `<date>` for dates
* `<device>` for devices (e.g., hospital equipments and devices)
* `<dose>` for doses
* `<email>` for emails
* `<fax>` for fax numbers
* `<idno>` for ID number
* `<idType>` for ID types
* `<living>` for living beings (e.g., animals, plants, bacteria, virus, fungus)
* `<location>` for location, address, city and country names
* `<measure>` for measure types or names
* `<medicament>` for medicament
* `<object>` for objects 
* `<orgName>` for organization names
* `<pathology>` for disease names
* `<persName>` for person names
* `<persType>` for person types (e.g., Origine algérienne, italienne)
* `<phone>` for phone numbers
* `<physiology>` for physiology
* `<procedure>` for procedures
* `<roleName>` for person role names
* `<substance>` for chemical substances
* `<symptom>` for observed symptoms and signs, phenomena
* `<time>` for time (e.g., 23h10, 10:00)
* `<unit>` for value units
* `<value>` for values
* `<web>` for websites
* `<other>` for unknown (yet) entities

> Notes:
- The mark-up follows approximately the [TEI](http://www.tei-c.org) format.
- It is therefore recommended to see some examples of how these elements should be used for the __fr-medical-ner__ model in `grobid/grobid-trainer/resources/dataset/fr-medical-ner/corpus/` or `grobid/grobid-trainer/resources/dataset/fr-medical-ner/evaluation/`.


References for annotation guidelines:
* https://www.academie-medecine.fr/le-dictionnaire/
* https://www.cnrtl.fr 
* https://quaerofrenchmed.limsi.fr/
* https://medical-dictionary.thefreedictionary.com
