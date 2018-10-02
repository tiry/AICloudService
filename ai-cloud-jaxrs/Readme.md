
This module contribute to the main Nuxeo API adding the `/api/v1/ai` endpoint  

### REST API


#### Datasets

    CREATE		POST    /api/v1/ai/{project-id}

    READ		GET     /api/v1/ai/{project-id}/dataset/{dataset-id}
    EDIT      	PUT		/api/v1/ai/{project-id}/dataset/{dataset-id}
    DELETE	    DELETE  /api/v1/ai/{project-id}/dataset/{dataset-id}

    LIST        GET     /api/v1/ai/{project-id}/datasets


#### Models

    CREATE		POST 	/api/v1/ai/{project-id}

    READ		GET 	/api/v1/ai/{project-id}/model/{model-id}
    EDIT      	PUT		/api/v1/ai/{project-id}/model/{model-id}
    DELETE		DELETE  /api/v1/ai/{project-id}/model/{model-id}

    LIST        GET 	/api/v1/ai/{project-id}/models

    TRAIN       POST    /api/v1/ai/{project-id}/model/{model-id}/@op/Train
    PUBLISH     POST    /api/v1/ai/{project-id}/model/{model-id}/@op/Publish
    STATUS      POST    /api/v1/ai/{project-id}/model/{model-id}/@op/Status
    PREDICT     POST    /api/v1/ai/{project-id}/model/{model-id}/@op/Predict
    
