# Annotation guidelines for the _address_ model

## Introduction
Before performing this procedure, pre-annotated data must be prepared as explained in [generate new datasets](../Training-the-medical-report-models.md#generate-new-datasets).

To identify the address (`<address>`) element, see the annotation guidelines of the [medic](medic.md) and [patient](patient.md) models.

For the address model, we use the following TEI elements:
* `<streetNumber>` for the street numbers
* `<streetName>` for the street names
* `<buildingNumber>` for the entrance numbers (residences or apartments)
* `<buildingName>` for the building name (e.g., Résidence de Lilas)
* `<city>` for the city names (e.g., Lyon, Paris)
* `<postCode>` for the postal code (e.g., 75001)
* `<poBox>` for the PO box number (e.g., BP 123)
* `<community>` for the community names (e.g., Créteil)
* `<district>` for the district name or number (e.g., IIème arrodissement, quartier Perrache)
* `<departmentNumber>` for the department numbers (e.g., `75` for `Paris`, 9175` for `Essone`, `77` for `Seine-et-Marne`)
* `<departmentName>` for the department names (e.g., Paris, Essone, Seine-et-Marne)
* `<region>` for the region names (e.g., Île-de-France, Bretagne, Normandie, Départements de DOM-TOM)
* `<country>` for the country names (e.g., France)
* `<note type="address">` for the notes concerning the address

> Notes:
- The mark-up follows approximately the [TEI](http://www.tei-c.org) format.
- It is therefore recommended to see some examples of how these elements should be used for the __address__ model in `grobid/grobid-trainer/resources/dataset/address/corpus/` or `grobid/grobid-trainer/resources/dataset/address/evaluation/`.
> The list of regions, departments, cities in France can be found here [Liste régions, départements et communes de France](https://www.villesfrance.fr/fr/).

## Analysis

The following sections provide detailed information and examples on how to handle certain typical cases.

### Space and new lines

Spaces and a new line in the XML annotated files are not significant and will be all considered by the XML parser as the default separator. So it is possible to add and remove freely space characters and new lines to improve the readability of the annotated document without any impacts. 

Similarly, line break tags `<lb/>` are present in the generated XML training data, but they will be considered as a default separator by the XML parser. They are indicated to help the annotator to identify a piece of text in the original PDF if necessary. Actual line breaks are identified in the PDF and added by aligning the XML/TEI with the feature file generated in parallel which contains all the PDF layout information. 

### Address 

All the mentions of address are labeled under `<address>`. [address](https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-persName.html) provides information about an identifiable address.


```xml
<address><streetNumber>20</streetNumber> <street>RUE SADI CARNOT</street> <postCode>93170</postCode> <department>BAGNOLET</department></address>
```
