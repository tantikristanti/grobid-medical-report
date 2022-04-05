# Annotation guidelines for the _french-medical-ner_ model

We use the French medical NER we use [The_QUAERO_French_Medical_Corpus](https://quaerofrenchmed.limsi.fr/) as our annotation guidelines.

## Introduction

For the following guidelines, firstly we need to generate the training data.

1. Create blank training data from the body part

```bash
> java -Xmx4G -jar build/libs/grobid-medical-report-0.0.1-onejar.jar -gH grobid-home -dIn ~/path_to_input_directory/ -dOut ~/path_to_output_directory -exe createTrainingBlankFrenchMedicalNER
```

2. Create the training data using the pre-trained model

```bash
> java -Xmx4G -jar build/libs/grobid-medical-report-0.0.1-onejar.jar -gH grobid-home -dIn ~/path_to_input_directory/ -dOut ~/path_to_output_directory -exe createTrainingFrenchMedicalNER
```

For the annotation process, we use the largest entity mentions and does not handle nested entities which conform with [grobid-ner](https://grobid-ner.readthedocs.io/en/latest/largest-entity-mention/)).

For the French medical NER model, we use the following TEI elements:

* `<anatomy>` for anatomy
* `<date>` for dates
* `<device>` for devices (e.g., hospital equipments and devices)
* `<dose>` for doses
* `<living>` for living beings
* `<location>` for locations, addresses, city and country names
* `<measure>` for measure types or names
* `<medicament>` for medicament
* `<object>` for objects 
* `<pathology>` for disease names
* `<persName>` for person names
* `<physiology>` for physiology
* `<procedure>` for procedures
* `<substance>` for chemical substances
* `<symptom>` for symptoms and signs
* `<unit>` for value units
* `<value>` for values
* `<other>` for unknown (yet) entities


References for annotation guidelines:
* https://www.academie-medecine.fr/le-dictionnaire/
* https://www.cnrtl.fr 
* https://quaerofrenchmed.limsi.fr/
* https://medical-dictionary.thefreedictionary.com
