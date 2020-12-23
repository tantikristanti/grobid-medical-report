# Annotation Guidelines for the 'Medical Report Segmenter' Model

## Introduction

For the following guidelines, it is expected that training data has been generated as explained [here](../Training-the-models-of-Grobid/#generation-of-training-data).

The following TEI elements are used by the medical report segmenter model:

* `<front>` for document header
* `<note place="headnote">` for the page header note
* `<note place="footnote">` for the page footer note
* `<note place="left">` for the notes on the document left section
* `<note place="right">` for the notes on the document right section
* `<body>` for the document body
* `<page>` for the page numbers
* `<div type="acknowledgment">` for the acknowledgment
* `<other>` for unknown (yet) part

It is necessary to identify these above substructures when interrupting the `<body>`. Figures and tables (including their potential titles, captions and notes) are considered part of the body, so contained by the `<body>` element.

Note that the mark-up follows overall the [TEI](http://www.tei-c.org). 

> Note: It is recommended to study the existing training documents for the medical-report-segmenter model first to see some examples of how these elements should be used.

![Segmented medical document](img/segmented document.png)

## Analysis

The following sections provide detailed information and examples on how to handle certain typical cases.

### Header of the document (front)

The header section typically contains document's information (i.e., number, title, type, date), medics with affiliations, patients, and complete correspondence with telephone, fax, e-mail, and web information. All this material should be contained within the `<front>` element. In general, we expect as part of the header of the document to find information about the type and the title of the document, the date and time of the document, the owner of the document, and if it's possible, the actors (e.g., medics, patients) involved in the document at an early stage before proceeding to the contents of the document.  This should be followed in order to ensure homogeneity across the training data.

There should be as many `<front>` elements as necessary that contain all the contents identified as 'front contents'. Note that for the segmentation model, there aren't any `<title>`, `<medic>` or `<patient>` elements as they are handled in the cascaded `header-medical-report` model applied in a next stage.

Any footnotes referenced from within the `<body>` should remain there.

