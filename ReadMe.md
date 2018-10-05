### Goal

The goal of this module is to be the Cloud Server side of the Nuxeo AI Service.

 - Storage and management of Datasets/Corpus used to train model
 - Storage and management of AI Models
 - Train, Publish and Call models

<img src="https://www.lucidchart.com/publicSegments/view/0e306c65-0f68-49b4-8e08-8b52955d364b/image.png" width="500px"/>

### Design Choices

The goal is to reuse as much as possible what already exists inside the Nuxeo Platform:

 - for consistency reasons
 - because doing this exercise could help us improving the platform

Because of that, rather than building a brand new REST API, this module is just complementing the existing Nuxeo Rest API and Automation API.

The Repository stores (and manages versions and securty) for 3 main types of objects:

 - Models
 	- the actual TF model
 - Datasets
 	- data extracted from a Nuxeo repository to train a model
 - Training
 	- the gluecode to bind Model + DataSet + SageMaker

In terms of integration:

 - Nuxeo-Stream is used to decouple the Nuxeo side from the notebook/sagemaker side
 - Openshift is expected to provide an API to provision the TF endpoints

### Modules

**Cloud side**

 - a JAX-RS module contributing the additionnal REST API to Nuxeo API
 - a Core module contributing the service and operations
 - a Studio project for content model and UI
 - a marketplace package

**Nuxeo "client" side** 

(not started)




