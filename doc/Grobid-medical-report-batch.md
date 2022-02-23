<h1>grobid-medical-report batch mode</h1>

## Using the batch

Be sure that the __grobid-medical-report__ project has been built (`./gradlew clean install`).

Go under the project directory `grobid/grobid-medical-report`:
```bash
> cd grobid/grobid-medical-report
```

The following command displays some help for the batch commands:
```bash
> java -jar build/libs/grobid-medical-report-<current-version>-onejar.jar -h
```

Be sure to replace `<current-version>` with the current version of __grobid-medical-report__. For example:
```bash
> java -jar build/libs/grobid-medical-report-0.0.1-onejar.jar -h
```

The available batch commands are listed below. For those commands, at least `-Xmx1G` is used to set the JVM Memory to avoid *OutOfMemoryException* given the current size of the Grobid models and the craziness of some PDF files. For complete Full-Text processing, which involves all the Grobid and the subproject __grobid-medical-report__ models, `-Xmx4G` is recommended (although allocating less memory works fine as well).

The so-called "GROBID home" in GROBID is the path to `grobid-home` (by default `grobid/grobid-home`). For the following batch command lines, the direction to the "GROBID home" path is specified by the parameter `-gH`.

### Create training
The following commands are used to create datasets. Two types of data will be generated, raw files containing features and TEI files. Only TEI files that are intended to be corrected by annotators, while the raw data should remain untouched.

#### 1. createTrainingBlank
Unlabeled datasets are usually used for building models from scratch. Datasets for all models used for grobid-medical-report will be built. Raw data can be copied directly into `grobid-trainer/resources/dataset/[MODEL]/corpus/raw`, while TEI data needs to be labeled before being copied to  `grobid-trainer/resources/dataset/[MODEL]/corpus/tei`.

```bash
> java -Xmx4G -jar build/libs/grobid-medical-report-0.0.1-onejar.jar -gH grobid-home -dIn ~/path_to_input_directory/ -dOut ~/path_to_output_directory -exe createTrainingBlank
```

#### 2. createTraining
The difference between this command and the `createTrainingBlank` command is simply the fact that the TEI files are pre annotated by using pre-trained models. Raw data can be copied directly into `grobid-trainer/resources/dataset/[MODEL]/corpus/raw`, while TEI data needs to be corrected by human annotators before being copied to  `grobid-trainer/resources/dataset/[MODEL]/corpus/tei`.

```bash
> java -Xmx4G -jar build/libs/grobid-medical-report-0.0.1-onejar.jar -gH grobid-home -dIn ~/path_to_input_directory/ -dOut ~/path_to_output_directory -exe createTraining
```

with:
* -gH: path to grobid-home directory

* -dIn: path to the input (i.e., Pdf files) directory

* -dOut: path to the output directory where the generated data will be saved

#### 1. processHeader
'processHeader' batch command will extract, structure and normalise the header part of medical reports in TEI format. The output is a TEI file corresponding to the structured report header.

Example:
```bash
> java -Xmx4G -jar build/libs/grobid-medical-report-0.0.1-onejar.jar -gH grobid-home -dIn ~/path_to_input_directory/ -dOut ~/path_to_output_directory -r -exe processHeader 
```

#### 2. processLeftNote
'processLeftNote' batch command will extract, structure and normalise the left note part of medical reports in TEI format. The output is a TEI file corresponding to the structured report left note.

Example:
```bash
> java -Xmx4G -jar build/libs/grobid-medical-report-0.0.1-onejar.jar -gH grobid-home -dIn ~/path_to_input_directory/ -dOut ~/path_to_output_directory -r -exe processLeftNote 
```

#### 3. processFullText
To get the results from the combination of all the prepared models, we can use this command:

Example:
```bash
> java -Xmx4G -jar build/libs/grobid-medical-report-0.0.1-onejar.jar -gH grobid-home -dIn ~/path_to_input_directory/ -dOut ~/path_to_output_directory -r -exe processFullText 
```

with:

* -gH: path to grobid-home directory

* -dIn: path to the input (i.e., Pdf files) directory

* -dOut: path to the output directory where the extracted data will be saved

* -r: recursive processing of files in the sub-directories (by default not recursive)
