package org.nuxeo.ecm.restapi.server.jaxrs;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonReader;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

@WebObject(type = "ai")
public class AIWebObject extends DefaultObject {

	protected DocumentModel getProjectContainer(String projectId) {
        CoreSession session = getContext().getCoreSession();
        // XXX use a Query Builder to handle escapes
		DocumentModelList docs = session.query("select * from AIResourcesContainer where airc:projectid='" + projectId + "'");                
        return docs.get(0);        
	}	   
    
    // CREATE for DataSet or Model    
    @POST
    @Path("{projectId}")    
    @Consumes({ "application/json+nxentity", "application/json" })
    public Response doPost(DocumentModel inputDoc, @PathParam("projectId") String projectId) {
        CoreSession session = ctx.getCoreSession();

        if (StringUtils.isBlank(inputDoc.getType()) || StringUtils.isBlank(inputDoc.getName())) {
            return Response.status(Status.BAD_REQUEST).entity("type or name property is missing").build();
        }

        DocumentModel parent = getProjectContainer(projectId);
        // XXX check inputDoc.getType() ?        
        DocumentModel createdDoc = session.createDocumentModel(parent.getPathAsString(), inputDoc.getName(),
                inputDoc.getType());
        DocumentModelJsonReader.applyPropertyValues(inputDoc, createdDoc);
        createdDoc = session.createDocument(createdDoc);
        session.save();
        return Response.ok(createdDoc).status(Status.CREATED).build();
    }
    
    // RUD for DataSets
    @Path("{projectId}/dataset/{datasetId}")
    public Object getDataSet(@PathParam("projectId") String projectId, @PathParam("datasetId") String datasetId) {
        CoreSession session = getContext().getCoreSession();        
        DocumentModel parent = getProjectContainer(projectId);        
        DocumentModel dataset = session.getDocument(new IdRef(datasetId));

        // check containment ?
        if (!dataset.getPathAsString().contains(parent.getPathAsString())) {
        	return Response.status(Status.NOT_FOUND).build();
        }        
        return newObject("Document", dataset);
    }    

    // Lists DataSets
    @GET
    @Path("{projectId}/datasets")
    public DocumentModelList getDatasets(@PathParam("projectId") String projectId) {
        CoreSession session = getContext().getCoreSession();                        
        DocumentModel parent = getProjectContainer(projectId);                
        // XX handle batching
        DocumentModelList docs = session.query("select * from AI_Corpus where ecm:path startswith '" + parent.getPathAsString() + "'");                        
        return docs;        
    }

    // RUD for Models
    @Path("{projectId}/model/{modelId}")
    public Object getModel(@PathParam("projectId") String projectId, @PathParam("modelId") String modelId) {
        CoreSession session = getContext().getCoreSession();        
        DocumentModel parent = getProjectContainer(projectId);        
        DocumentModel model = session.getDocument(new IdRef(modelId));

        // check containment
        if (!model.getPathAsString().contains(parent.getPathAsString())) {
        	return Response.status(Status.NOT_FOUND).build();
        }        
        return newObject("Document", model);
    }    

    // Lists DataSets
    @GET
    @Path("{projectId}/models")
    public DocumentModelList getModels(@PathParam("projectId") String projectId) {
        CoreSession session = getContext().getCoreSession();                        
        DocumentModel parent = getProjectContainer(projectId);                
        // XX handle batching
        DocumentModelList docs = session.query("select * from AI_Model where ecm:path startswith '" + parent.getPathAsString() + "'");                        
        return docs;        
    }

}
