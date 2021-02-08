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

#### createTrainingSegmentation
`createTrainingSegmentation` batch command will generate the __grobid-medical-report__ training dataset for the medical report segmentation model from PDF files. The needed parameters are:

* -gH: path to grobid-home directory

* -dIn: path to the input (i.e., Pdf files) directory

* -dOut: path to the output directory where the generated data will be saved

Example:
```bash
> java -Xmx4G -jar build/libs/grobid-medical-report-0.0.1-onejar.jar -gH ../grobid-home -dIn ~/path_to_input_directory/ -dOut ~/path_to_output_directory -exe createTrainingSegmentation
```

#### createTrainingHeader
`createTrainingHeader` batch command will generate the __grobid-medical-report__ training dataset for the header model from PDF files. The needed parameters are:

* -gH: path to grobid-home directory

* -dIn: path to the input (i.e., Pdf files) directory 

* -dOut: path to the output directory where the generated data will be saved

Example:
```bash
> java -Xmx4G -jar build/libs/grobid-medical-report-0.0.1-onejar.jar -gH ../grobid-home -dIn ~/path_to_input_directory/ -dOut ~/path_to_output_directory -exe createTrainingHeader
```

#### createTrainingLeftNote
`createTrainingHeader` batch command will generate the __grobid-medical-report__ training dataset for the header model from PDF files. The needed parameters are:

* -gH: path to grobid-home directory

* -dIn: path to the input (i.e., Pdf files) directory

* -dOut: path to the output directory where the generated data will be saved

Example:
```bash
> java -Xmx4G -jar build/libs/grobid-medical-report-0.0.1-onejar.jar -gH ../grobid-home -dIn ~/path_to_input_directory/ -dOut ~/path_to_output_directory -exe createTrainingLeftNote
```
