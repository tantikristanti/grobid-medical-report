# Annotation guidelines

This annotation guidelines explains how to correct pre-annotated data. Therefore, before carrying out this stage, pre-annotated data needs to be prepared and the steps can be seen here [generate new datasets](../Training-the-medical-report-models.md#generate-new-datasets).

## Correcting pre-annotated files

The most important principle when correcting the pre-annotated data is to __keep the stream of text untouched__. Only the tags can be moved, while the text itself shall not be modified or corrected.  

There are two exceptions to this main rule:

* Actual __end-of-lines__ from the PDF files are indicated by element `<lb/>`. These tags should be considered as part of the stream of text, and they should not be moved or removed concerning the overall text stream. 

* In the TEI/XML files, end-of-line is equivalent to a space character. It is possible to add or remove end-of-line characters, as long as the spacing is preserved. 

Examples of existing annotations for each model are under `grobid/grobid-trainer/resources/dataset/*MODEL*/corpus/tei/` or `grobid/grobid-trainer/resources/dataset/*MODEL*/evaluation/tei/`. 

We cannot add XML files as training and evaluation datasets for the following cases:
* If XML files are incorrectly produced  (e.g., `*.training.header.medical.xml` is produced after a chunk of text wrongly identified as the header section), then these files must be removed.

* If XML files are missing (e.g., a chunk of text should be as the header section, but Grobid predicted it as a note), then the additional XML file `*.training.header.medical.xml` shall not be created.

XML files can be modified or deleted, but they cannot be created manually.

More detailed annotation guidelines for each model can be seen in each of the following sections:
1. Guidelines for [address](address.md) model.
2. Guidelines for [dateline](dateline.md) model.
3. Guidelines for [french-medical-ner](french-medical-ner.md) model.
4. Guidelines for [full-medical-text](full-medical-text.md) model.
5. Guidelines for [header-medical-report](header-medical-report.md) model.
6. Guidelines for [left-note-medical-report](left-note-medical-report.md) model.
7. Guidelines for [medic](medic.md) model.
8. Guidelines for [medical-report-segmenter](medical-report-segmenter.md) model.
9. Guidelines for [name-person-medical](name-person-medical.md) model.
10. Guidelines for [organization](organization.md) model.
11. Guidelines for [patient](patient.md) model.
