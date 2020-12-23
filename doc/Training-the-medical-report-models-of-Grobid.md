<h1>Training and evaluating GROBID-medical-report models</h1>

## Models

Grobid uses different sequence labelling models depending on the labeling task to be realized. For a complex extraction and parsing tasks (for instance header extraction and parsing), several models are used in cascade. The current models are the following ones:

* medical-report-segmenter

* header-medical-report

* name-medical-personnel

* name-patient

* affiliation-address

* date

* body-medical-report

The models are located under `grobid/grobid-home/models`. Each of these models can be re-trained using additional training data. For production, a model is trained with all the available training data to maximize the performance. For development purposes, it is also possible to evaluate a model with part of the training data. 

## Train and evaluate

For the grobid-medical-report models, the training data is located under `grobid/grobid-medical-report/resources/dataset/*MODEL*/corpus/` 
where *MODEL* is the name of the model (so for instance, `grobid/grobid-medical-report/resources/dataset/medical-report-segmenter/corpus`). 

When generating a new model, a segmentation of data can be done (e.g. 80%-20%) between TEI files for training and for evaluating. This segmentation can be done following two manner: 

- manually: annotated data are moved into two folders, data for training have to be present under `grobid/grobid-medical-report/resources/dataset/*MODEL*/corpus/`, and data for evaluation under `grobid/grobid-medical-report/resources/dataset/*MODEL*/evaluation/`. 

- automatically: The data present under `grobid/grobid-medical-report/resources/dataset/*MODEL*/corpus` are randomly split following a given ratio (e.g. 0.8 for 80%). The first part is used for training and the second for evaluation.

There are different ways to generate the new model and run the evaluation, whether running the training and the evaluation of the new model separately or not, and whether to split automatically the training data or not. For any methods, the newly generated models are saved directly under grobid-home/models and replace the previous one. A rollback can be made by replacing the newly generated model by the backup record (`<model name>.wapiti.old`).

### Train in one command
Train (and generate a new model):
Under the main project directory `grobid/`, run the following command to execute both training and evaluation: 
```bash
> ./gradlew <training goal. I.E: train_medical_report_segmenter, train_header_medical_report>
```
Example of goal names: `train_medical_report_segmenter`, `train_header_medical_report`, `train_name_persoMedical`, `train_name_patient`, `train_affiliation_address`, `train_body_medical_report`, ...

The files used for the training are located under `grobid/grobid-medical-report/resources/dataset/*MODEL*/corpus`, and the evaluation files under `grobid/grobid-trainer/resources/dataset/*MODEL*/evaluation`. 

Examples for training the medical-report-segmenter model: 
```bash
> ./gradlew train_medical_report_segmenter
```

Examples for training the header-medical-report model: 
```bash
> ./gradlew train_header_medical_report
```
Examples for training the model for names of medical personnels in header: 
```bash
> ./gradlew train_name_persoMedical
```

The training files considered are located under `grobid/grobid-medical-report/resources/dataset/*MODEL*/corpus`

The training of the models can be controlled using different parameters. The `grobid.nb_thread` in the file `grobid-home/config/grobid.properties` can be increased to speed up the training. Similarly, modifying the stopping criteria can help speed up the training. Please refer [this comment](https://github.com/kermitt2/grobid/issues/336#issuecomment-412516422) to know more.

### Evaluation in one command
Under the main project directory `grobid/grobid-medical-report/`, execute the following command (be sure to have built the project `./gradlew clean install`:

Evaluate:
```bash
> ./gradlew eval_medical_report_segmenter
```

The considered evaluation files are located under `grobid/grobid-medical-report/resources/dataset/*MODEL*/evaluation`

Automatically split data, train and evaluate:
```bash
> ./gradlew eval_medical_report_segmenter_split
```

For instance, training the date model with a ratio of 75% for training and 25% for evaluation:
```bash
> ./gradlew eval_medical_report_segmenter_split -s 0.75
```

A ratio of 1.0 means that all the data available under `resources/dataset/*MODEL*/corpus/` will be used for training the model, and the evaluation will be empty. 

## Generation of training data
	
To generate some training datas from some input pdf:
* by using the medical-report-segmenter model
```bash
> java -Xmx4G -jar build/libs/grobid-medical-report-0.0.1-onejar.jar -gH ../grobid-home -dIn ~/path_to_input_directory/ -dOut ~/path_to_output_directory -exe createTrainingSegmentation
```

* by using the header-medical-report model
```bash
> java -Xmx4G -jar build/libs/grobid-medical-report-0.0.1-onejar.jar -gH ../grobid-home -dIn ~/path_to_input_directory/ -dOut ~/path_to_output_directory -exe createTrainingHeader
```

For each pdf in input directory, GROBID generates different files because each model has separate training data, and thus uses separate files. So we have one file for TEI (`*.training.MODEL.tei.xml`), 

When a model uses PDF layout features, an additional feature file (for example `*.training.MODEL`) is generated without `.tei.xml` extension. 

If you wish to maintain the training corpus as gold standard, these automatically generated data have to be checked and corrected manually before being moved to the training/evaluation folder of the corresponding model. For correcting/checking these data, the guidelines presented in the next section must be followed to ensure the consistency of the whole training sets. 


## Training guidelines

Annotation guidelines for creating the training data corresponding to the different GROBID models are available from the [following page](training/General-principles.md).
