### Goal

The goal of this module is to be the Cloud Server side of the Nuxeo AI Service.

 - Storage and management of Datasets/Corpus used to train model
 - Storage and management of AI Models
 - Train, Publish and Call models

<img src="https://www.lucidchart.com/publicSegments/view/e0131d14-a1c1-4491-8d38-5b822f4916e9/image.png" width="500px"/>

### Design Choices

The goal is to reuse as much as possible what already exists inside the Nuxeo Platform:

 - for consistency reasons
 - because doing this exercise could help us improving the platform

Because of that, rather than building a brand new REST API, this module is just complementing the existing Nuxeo Rest API and Automation API.

### Modules

**Cloud side**

 - a JAX-RS module contributing the additionnal REST API to Nuxeo API
 - a Core module contributing the service and operations
 - a Studio project for content model and UI
 - a marketplace package

**Nuxeo "client" side** 

(not started)




