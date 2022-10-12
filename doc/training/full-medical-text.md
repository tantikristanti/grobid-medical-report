# Annotation guidelines for the _full-medical-text_ model

## Introduction
Before performing this procedure, pre-annotated data must be prepared as explained in [generate new datasets](../Training-the-medical-report-models.md#generate-new-datasets).

Full-medical-text model attempts to recognize and struture information appearing in the body area of medical reports.  This model is totally different from the `medical-report-segmenter` model which tries to recognize general sections (front, left-note, body) and which is applied before the `full-medical-text` model.

> Note: Whitespace is not important in the XML that Grobid uses. You can add newline characters and spaces to make the XML document more legible.

For the full-medical-text model, we use the following TEI elements:

* `<titlePart>` for the document title (that appears in the body part)
* `<head>` for section titles
    * `<head level="1">` for sections
    * `<head level="2">` for subsections
* `<p>` for paragraphs
* `<item>` for list items inside lists
* `<figure>` for figures
* `<figure type="table">` for tables
* `<medic>` for the list of medics still found in the body part
* `<patient>` the list of patients still found in the body part
* `<ref>` markers, for reference to other parts of the document
    * `<ref type="figure">` a pointer to a figure in the document
    * `<ref type="table">` a link to a table in the document
* `<note>` for headnotes and footnotes that can appear in the body part
    * `<note place="headnote">` for the page header note
    * `<note place="footnote">` for the page footer note
    * `<note type="licence">` for the licence and data access information
    * `<note type="signature">` for the signatures
    * `<note type="note">` for other type of notes
* `<other>` for unknown (yet) part

> Notes:
- The mark-up follows approximately the [TEI](http://www.tei-c.org) format.
- It is therefore recommended to see some examples of how these elements should be used for the __full-medical-text__ model in `grobid/grobid-trainer/resources/dataset/full-medical-text/corpus/tei/` or `grobid/grobid-trainer/resources/dataset/full-medical-text/evaluation/tei`.

## Analysis

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

### Section titles

To indicate sub-parts of medical reports. Section titles subdivide the flow of the content into smaller chunks. These titles should appear on the same level as the paragraphs.  Here are some examples:

```xml
<head>MOTIF D&apos;HOSPITALISATION<lb/></head>
```

```xml
<head>1. MODE DE VIE<lb/></head>
<head>2. ANTÉCÉDENTS<lb/></head>
```

Example for sections with levels:
```xml
<head level="1">HISTOIRE DE LA MALADIE<lb/></head> 
<head level="2">RAPPEL DES ANTÉCÉDENTS<lb/></head>
```

### Paragraphs

Paragraphs constitute the main bulk and contain text which in turn may contain inline elements such as line breaks.

```xml
<p> Bon équilibre du diabète insipide, évaluation diététique faite.<lb/> 
    sera revu dans 6 mois avec une échographie abdominale pour hépatomégalie et consultation ORL<lb/> 
    avec audiogramme<lb/>
    ...
</p>
```

> Note: The `<lb/>` (line break) elements are there because they have been recognized as such in the PDF in the text flow. However the fact that they are located within or outside a tagged paragraph or section title has no impact. Just be sure NOT to modify the order of the text flow and `<lb/>` as mentionned [here](Annotation_guidelines.md#correcting-pre-annotated-files).

### List items

Following the TEI, list items (`<item>` elements) should be contained in a `<list>` element and must not occur within `<p>` elements. At this stage no difference is made between ordered and unordered lists.

List item markers such as hyphens, bullet points (for unordered lists) or numbers and letters (for ordered lists) should be contained within the `<item>` element.

```xml
<list>
  <item>-Information du patient / de sa famille : Oui<lb/></item>

  <item>-Inclusion dans un protocole de recherche : Non<lb/></item>
</list>
```

### Figures and tables

A photo, picture, or other graphical representation (this could be a chart or another figure) and boxes, are to be marked up using the `<figure>` element. This element contains the title, the figure/table/boxed content/photo itself, captions, any legend or notes it may have.

Note that following the TEI, a table is marked as a figure of type "table" (the actual `<table>` element appears in the `table` model applied in cascade) and a boxed content is marked as a figure of type "box".

The following XML sample shows a table that is marked up as a `<figure type="table">` element.

```xml
<figure type="table">
_______________________________________________________________________________________<lb/>
|27/09/16<lb/> |<lb/> Analyse<lb/> |Unité<lb/> |12:25<lb/> |Valeurs de …<lb/>
_______________________________________________________________________________________<lb/>
HEMATOLOGIE -CYTOLOGIE<lb/>
Numération<lb/>
Leucocytes<lb/> |x10*9/L |9.04<lb/> |4.00-10.00<lb/>
Hématies<lb/> |x10*12/L|5.38<lb/> |4.20-5.90<lb/>
Hémoglobine<lb/> |g/dL<lb/> |15.1<lb/> |13.0-17.0<lb/>
...
</figure>
```

### Markers (callouts to structures)

These elements appear as inline elements, inside `<p>`, `<item>`, or other elements containing and usual reference other parts of the document. They could be understood as links.  Here is a list of currently supported markers:

* `<ref type="figure">` a pointer to a figure elsewhere in the document (*Fig 5b, left*)
* `<ref type="table">` a link to a table in the document

#### Markers to tables and figures

The next example shows markers (callouts) to a table and a figure (as noted earlier, whitespace is not of importance for GROBID and can therefore be used liberally, like here to better show the tagging):

```xml
<p>The patient group comprised all six patients with<lb/>
  juvenile cervical
	flexion myelopathy admitted to<lb/>
  our hospital

   (Table <ref type="table">1</ref>).

  In all of them, cervical flexion<lb/>
  ...
  alignment in the extended neck position

  (Figure <ref type="figure">3</ref>).<lb/>

  Cervical MR imaging in the neutral neck position of<lb/>
  five of the six patients showed a straight cervical<lb/>
</p>
```

As visible in the examples, markers to figure, table, or formula are annotated by including only the key information of the referred object. Brackets, parenthesis, extra wording, extra information are left outside of the tagged text (in contrast to bibliographical markers, where we keep the brackets and parenthesis by convention).
Here are some more short examples for figure markers:

```xml
(Supplementary Fig. <ref type="figure">1</ref><lb/> online)
```

```xml
(Fig. <ref type="figure">5b</ref>, left)
```

```xml
Figure <ref type="figure">2</ref> exemplifies
```

```xml
(10.3% of those analysed; Fig. <ref type="figure">1a</ref>).
```

We group under the same element a conjunction/list of callouts:

```xml
In figs. <ref type="figure">3 and 4</ref>
```
