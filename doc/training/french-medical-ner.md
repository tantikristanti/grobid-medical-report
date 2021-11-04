# Annotation guidelines for the _french-medical-ner_ model

Since for the French medical NER we use [The_QUAERO_French_Medical_Corpus](https://quaerofrenchmed.limsi.fr/) as our recources for building the model-0, it's obvious that we follow their annotation guidelines.

## Introduction

For the following guidelines, we need to generate the training data first. 

To generate the training data with the Quaero corpus as input and to conform to the Grobid format, we can use the CreateMedicalDatasetsFromQuaeroCorpus class. For our case which uses the largest entity mentions and does not handle nested entities, we adapt the generated datasets to conform to the Grobid format (in this case, corform to [grobid-ner](https://grobid-ner.readthedocs.io/en/latest/largest-entity-mention/)).

However, to generate the training data using Model-0 (developed using the Quaero Copus) from any new corpus, we can see the explanation from [here](../Training-the-medical-report-models.md#generation-of-training-data).

For the French medical NER model, we use the following TEI elements:

* `<anatomy>` for anatomy
* `<device>` for devices
* `<disorder>` for disorders
* `<drug>` for chemical and drugs
* `<living>` for living beings
* `<location>` for geographic areas
* `<object>` for objects
* `<phenomena>` for phenomena
* `<physiology>` for physiology
* `<procedure>` for procedures
* `<other>` for unknown (yet) part

