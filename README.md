# grobid-medical-report
grobid-medical-report is a [GROBID](https://github.com/kermitt2/grobid) module for extracting and structuring medical reports into structured XML/TEI encoded documents. As the other GROBID models, this module relies on machine learning using linear Chain Conditional Random Fields (CRF). 

## Install - Build - Run

First install the latest development version of GROBID as explained by the [documentation](http://grobid.readthedocs.org).

Copy the module grobid-medical-report as a sibling sub-project of GROBID (e.g., grobid-core, grobid-trainer) :
> cp -r grobid-medical-report grobid/

> cd PATH-TO-GROBID/grobid/grobid-medical-report

Copy the existing trained model in the standard `grobid-home` path, type the command under `grobid/grobid-medical-report` path:

> ./gradlew copyModels 

In general, the model will be placed under `PATH-TO-GROBID/grobid/grobid-home/models/medical-report/`

Try compiling everything with:

> ./gradlew clean install

To build grobid-medical-report under the proxy, the proxy host and port need to be added : 
>  ./gradlew -DproxySet=true -DproxyHost=[proxy_host] -DproxyPort=[proxy_port] clean install

## Start the Service

> ./gradlew appRun

Demo console (Web app) is accessible at ```http://localhost:8080```. Close the application by pressing a button to kill the Java process and not via Ctrl+C. 

Using ```curl``` POST/GET requests:

```
curl -X POST -d "text=Text to be processed." localhost:8080/service/processNameText
```

```
curl -GET --data-urlencode "text=Text to be processed." localhost:8080/service/processNameText
```

## Training and Evaluation


To train and to evaluate the model under the proxy, the proxy host and port need to be added : 
Example : 
```
> cd PATH-TO-GROBID/grobid/grobid-medical-report

> ./gradlew train_medical_report -DproxySet=true -DproxyHost=
  [proxy_host] -DproxyPort=[proxy_port]
```

### Training Only

For training the medical-report model with all the available training data:

```
> cd PATH-TO-GROBID/grobid/grobid-medical-report

> ./gradlew train_medical_report
```

The training data must be under ```grobid-medical-report/resources/dataset/medical-report/corpus```

### Evaluating Only

For evaluating under the labeled data under ```grobid-medical-report/resources/dataset/medical-report/evaluation```, use the command:

```
>  ./gradlew eval_medical_report
```

### Automatic Corpus Split

To split automatically and randomly the available annotated data (under ```resources/dataset/medical-report/corpus/```) into a training set and an evaluation set, we use the following commands:

```
>  ./gradlew eval_medical_report_split
```

It trains the model based on the first data set and launch an evaluation based on the second one. 
By default, 80% of the available data is for training and the remaining for evaluation. The ratio can be changed by editing the corresponding exec profile in the pom.xml file. 


## Generation of New Training Data

To generate new annotated data in TEI format based on the current model : 

```
> java -Xmx4G -jar build/libs/grobid-medical-report-0.0.1-onejar.jar -gH ../grobid-home -dIn ~/path_to_input_directory/ -dOut ~/path_to_output_directory -exe createTraining
```

We need to define the path to the input and output directories.

## Training Data and Models Copyright

This module is dedicated specifically to projects related to [APHP](https://www.aphp.fr/) where training data and models are not to be shared publicly. Only the codes can be accessed. 
