# grobid-medical-report :hospital:
grobid-medical-report is a [GROBID](https://github.com/kermitt2/grobid) module for extracting and restructuring medical reports from raw documents (PDF, text) into encoded documents (XML/TEI). All models built in this module are machine learning models that implement Wapiti CRF as Grobid's default models (it's possible to use deep learning models developed with [DeLFT](https://github.com/kermitt2/delft/) in Grobid as an alternative to the Wapiti CRF).

![FromPDF2TEI](doc/img/PDF2TEI.png)

## Clone and install GROBID
**grobid-medical-report** is a module of GROBID and therefore the installation of GROBID is required.

### Clone GROBID
First, clone the latest version of GROBID. We can clone the forked project or the original [GROBID](https://github.com/kermitt2/grobid.git) repository (with slight adjustments to use **grobid-medical-report** module).

1. Clone from the forked project:

```
$ git clone https://github.com/tantikristanti/grobid.git
```

- Change directory (cd) to GROBID path

```
$ cd grobid
```

- Switch to *grobid-medical-report* branch

```
$ git checkout grobid-medical-report
```

** OR **
2. Clone from the original GROBID repository: 

```
$ git https://github.com/kermitt2/grobid.git
```

- Change directory (cd) to GROBID path

```
$ cd grobid
```

- In order not to interfere existing branches, it is recommended to create and switch to a new branch.
```
$ git checkout -b [NEW_BRANCH]
```
* Slight adjustments
  1. Registration of the new model names in the GrobidModels class (grobid-core/src/main/java/org/grobid/core/GrobidModels.java).
  2. Configuration of **grobid-medical-report** models in the grobid.yaml (grobid-home/config/grobid.yaml) by specifying:
      - Model names
      - Engine (machine learning with [Wapiti](https://wapiti.limsi.fr/) or deep learning with [Delft](https://github.com/kermitt2/delft/))
      - Training parameters
  3. Activation of the **-readingOrder** option to read the document block according to the reading order.
  
### Install GROBID
Install and build GROBID: 

```
$ ./gradlew clean install
```

To install and build GROBID under the proxy, we need to add the proxy host and port:
```
$  ./gradlew -DproxySet=true -DproxyHost=[proxy_host] -DproxyPort=[proxy_port] clean install
```

## Clone and install *grobid-medical-report*

Make sure that the current working directory is `grobid`:
```
$ pwd
    --> grobid
```

### Clone *grobid-medical-report*

Clone **grobid-medical-report** from this repository:
```
$ git clone https://github.com/tantikristanti/grobid-medical-report.git
```

- Change directory (cd) to **grobid-medical-report** path

```
$ cd grobid-medical-report
$ pwd
    --> grobid/grobid-medical-report
```

### Install *grobid-medical-report*
Install and build **grobid-medical-report**:

```
$ ./gradlew clean install
```

To install and build **grobid-medical-report** under the proxy, we need to add the proxy host and port:
```
$  ./gradlew -DproxySet=true -DproxyHost=[proxy_host] -DproxyPort=[proxy_port] clean install
```

## Copyright
This repository was originally prepared for a collaborative project between [INRIA](https://www.inria.fr/) and  [APHP](https://www.aphp.fr/). Original datasets and models containing genuine sensitive data are not possible to share publicly. 
