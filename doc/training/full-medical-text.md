# Annotation guidelines for the _full-medical-text_ model

## Introduction

For the following guidelines, we first need to generate the training data as explained [here](../Training-the-medical-report-models/#generation-of-training-data).

Full-medical-text model attempts to recognize and struture information appearing in the body area of medical reports.  This model is totally different from the `medical-report-segmenter` model which tries to recognize general sections (front, left-note, body) and which is applied before the `full-medical-text` model.

> Note: Whitespace is not important in the XML that Grobid uses. You can add newline characters and spaces to make the XML document more legible.

For the full-medical-text model, we use the following TEI elements:

* `<head>` for section titles
* `<p>` for paragraphs
* `<item>` for list items inside lists
* `<figure>` for figures
* `<figure type="table">` for tables
* `<ref>` markers, for reference to other parts of the document
    * `<ref type="figure">` a pointer to a figure in the document
    * `<ref type="table">` a link to a table in the document
* `<other>` for unknown (yet) part

> Note: It is recommended to study first the existing training documents for the __full-medical-text__ model (`grobid/grobid-medical-report/resources/dataset/full-medical-text`) to see some examples of how these elements should be used.

The following sections will give examples for each of the objects above and how they should be marked up. Note that the mark-up follows nearly overall the [TEI](http://www.tei-c.org).

## Analysis

### Section titles

To indicate sub parts of an article, authors may have used section titles that subdivide the flow of the content into smaller chunks. These titles should appear on the same level as the paragraphs, formulas, etc.  Here are some examples:

```xml
<head>CERAMIDE AND S1P BOTH TRIGGER AUTOPHAGY<lb/></head>
```

```xml
<head>1. Introduction<lb/></head>
<head>2 Background<lb/></head>
```

```xml
<head>B. Focusing an IACT<lb/></head>
```

```xml
<head>II. PROBLEM AND SOLUTION PROCEDURE<lb/></head>
```

```xml
<head>4 RESULTS<lb/></head>
<head>4.1 Image quality<lb/></head>
```

```xml
<head>Results<lb/></head>
<head>Patient characteristics<lb/></head>
```

```xml
<head>MATERIALS AND METHODS<lb/></head>
<head>Tissue samples<lb/></head>
```

Example for sections with levels:
```xml
<head level="1">Results<lb/></head> 
<head level="2">Both species avoid hybridisation<lb/></head>
```

### Paragraphs

Paragraphs constitute the main bulk of most typical articles or publications and contain text which in turn may contain inline elements such as references (see below) or line breaks.

```xml
<p>Our group has investigated the correlation between sphingolipid metabolism, the<lb/>
  ...
  are able to induce autophagy in a breast cancer cell line. 3,4<lb/>
</p>
```

> Note: The `<lb/>` (line break) elements are there because they have been recognized as such in the PDF in the text flow. However the fact that they are located within or outside a tagged paragraph or section title has no impact. Just be sure NOT to modify the order of the text flow and `<lb/>` as mentionned [here](General-principles/#correcting-pre-annotated-files).

Following the TEI, formulas should be on the same hierarchical level as paragraphs, and not be contained inside paragraphs:

```xml
<p>Exponentiation mixes. Our protocol will benefit from the exponentiation mix<lb/>
	...
  MS i+1 . The first server takes the original list of PKs. The net effect is a list: <lb/>
</p>


<p>and g s is also published by the last Teller.<lb/>
  ...
  (g s ) xi and finding the match.<lb/>
</p>
```

The next example illustrates similarly that in TEI list items should contained inside `<list>` elements which in turn are on the same hierarchical level as paragraphs and other block-level elements.

```xml
<p>The estimation of the eligible own funds and the SCR requires to carry out calculations <lb/>
  ...
  the following constraints:<lb/>
</p>

<list>
  <item>• updating the assets and liabilities model points;<lb/></item>

  <item>• constructing a set of economic scenarios under the risk-neutral probability and<lb/>
	  checking its market-consistency;<lb/></item>
</list>

<p>The wild-type strain was Bristol N2. All animals were raised at<lb/>
  20uC. The following alleles and transgenes were used:<lb/>
</p>

<p>LGI: hda-3(ok1991)<lb/>
</p>

<p>LGII: hda-2(ok1479) <lb/>
</p>
```

### List items

Following the TEI, list items (`<item>` elements) should be contained in a `<list>` element and must not occur within `<p>` elements. At this stage no difference is made between ordered and unordered lists.

List item markers such as hyphens, bullet points (for unordered lists) or numbers and letters (for ordered lists) should be contained within the `<item>` element.

```xml
<p>Introducing ballot identifiers has the appeal that it provides voters with a<lb/>
  very simple, direct and easy to understand way to confirm that their vote is<lb/>
  ...
  this observation that we exploit to counter this threat: we arrange for the voters<lb/>
  to learn their tracker numbers only after the information has been posted to the<lb/>
  WBB.<lb/>
  This paper presents a scheme that addresses both of these shortcomings by:<lb/>
</p>

<list>
  <item>– Guaranteeing that voters get unique trackers.<lb/></item>

  <item>– Arranging for voters to learn their tracker only after the votes and corre-<lb/>
  sponding tracking numbers have been posted (in the clear).<lb/></item>
</list>

<list>
	<item>1) The difficulty of identifying passages in a user&apos;s manual based on an individual word.<lb/></item>

  <item>2) The difficulty of distinguishing affirmative and negative sentences which mean	two different<lb/>
  features in the manual.<lb/></item>

  <item>3) The difficulty of retrieving appropriate passages for a query using words not appearing in the<lb/>
  manual.<lb/></item>
</list>
```

### Figures and tables

A photo, picture or other graphical representation (this could be a chart or another figure) and boxes, are to be marked up using the `<figure>` element. This element contains the title, the figure/table/boxed content/photo itself, captions, any legend or notes it may have.

Note that following the TEI, a table is maked as figure of type "table" (the actual `<table>` element appears in the `table` model applied in cascade) and a boxed content is marked as a figure of type "box".

The following XML sample shows one figure (`<figure>`) followed by two tables which are marked up as `<figure type="table">` elements.

```xml
<figure>Figure 1. Hypothetical model for ceramide and S1P-induced autophagy and thei	consequences on cell fate. An<lb/>
  ...
  <lb/>
  ....
  promotes cell survival by inhibiting the induction of apoptosis.<lb/>
</figure>

<figure type="table"> Table 1 Clades of clownfishes used in this study<lb/>
  Clade name<lb/>
	Species<lb/>
  percula<lb/>
  A. ocellaris, A. percula, P. biaculeatus<lb/>
  Australian<lb/>
	A. akindynos, A. mccullochi<lb/>
  ...
  of clownfish species [19].<lb/>
</figure>

<figure type="table"> Table 1 The clinicopathological data of PDAC tissue samples<lb/>
  Sample<lb/>
  Age<lb/>
  Sex<lb/>
  Location a<lb/>
  Histology b<lb/>
  T<lb/>
  N<lb/>
  ...
  1<lb/>
  1<lb/>
  IVb<lb/>
  a<lb/>
  P ¼ primary lesion; Ph ¼ head; Pb ¼ body; Pt ¼ tail of the pancreas; LM ¼ liver metastatic lesion. b
	mod ¼ moderately; poor ¼ poorly differentiated tubular adenocarcinoma.<lb/>
  PDAC ¼ pancreatic ductal adenocarcinoma; FISH ¼ fluorescence in situ hybridisation; ISH ¼ in
	situ RNA hybridisation.<lb/>
</figure>
```

<!-- NOTE: the problem below is fixed and <table> in the generated training data should not appear anymore!

Finally, an example where GROBID has recognized a table but used the `<table>` element to mark it up; this needs to be corrected to `<figure type="table">`.

The XML as suggested by Grobid before the training (note the `<table>` element):

The corrected XML (note the `<figure type="table">` element):

-->

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

As visible in the examples, markers to figure, table or formula are annotated by including only the key information of the refered object. Brackets, parenthesis, extra wording, extra information are left outside of the tagged text (in contrast to bibliographical markers, where we keep the brackets and parenthesis by convention). 
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
