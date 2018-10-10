package org.nuxeo.ecm.restapi.server.jaxrs;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ai.service.AICloudService;
import org.nuxeo.ai.service.PredictionResult;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonReader;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

@WebObject(type = "ai")
public class AIWebObject extends DefaultObject {

	protected static Log log = LogFactory.getLog(AIWebObject.class);

	protected DocumentModel getProjectContainer(String projectId) {
		CoreSession session = getContext().getCoreSession();
		// XXX use a Query Builder to handle escapes
		DocumentModelList docs = session
				.query("select * from AIResourcesContainer where airc:projectid='" + projectId + "'");
		return docs.get(0);
	}

	protected DocumentModel resolveArtifact(String projectId, String id) {
		CoreSession session = getContext().getCoreSession();
		DocumentModel parent = getProjectContainer(projectId);
		DocumentModel doc = session.getDocument(new IdRef(id));

		// check containment ?
		if (!doc.getPathAsString().contains(parent.getPathAsString())) {
			// XXX does not work with version!
			//return null;
		}
		return doc;
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

		String parentPath = parent.getPathAsString();

		if ("AI_Model".equals(inputDoc.getType())) {
			parentPath = parent.getPath().append("models").toString();
		} else if ("AI_Corpus".equals(inputDoc.getType())) {
			parentPath = parent.getPath().append("datasets").toString();
		} else if ("AI_Training".equals(inputDoc.getType())) {
			parentPath = parent.getPath().append("trainings").toString();
		} else {
			return Response.status(Status.BAD_REQUEST).entity("Unexpected Document Type").build();
		}

		DocumentModel createdDoc = session.createDocumentModel(parentPath, inputDoc.getName(), inputDoc.getType());
		DocumentModelJsonReader.applyPropertyValues(inputDoc, createdDoc);
		createdDoc = session.createDocument(createdDoc);
		session.save();
		return Response.ok(createdDoc).status(Status.CREATED).build();
	}

	// RUD for DataSets
	@Path("{projectId}/dataset/{datasetId}")
	public Object getDataSet(@PathParam("projectId") String projectId, @PathParam("datasetId") String datasetId) {
		DocumentModel dataset = resolveArtifact(projectId, datasetId);
		if (dataset == null) {
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

		DocumentModelList docs = session
				.query("select * from AI_Corpus where ecm:path startswith '" + parent.getPathAsString() + "'");
		return docs;
	}

	// RUD for Models
	@Path("{projectId}/model/{modelId}")
	public Object getModel(@PathParam("projectId") String projectId, @PathParam("modelId") String modelId) {
		DocumentModel model = resolveArtifact(projectId, modelId);
		if (model == null) {
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
		DocumentModelList docs = session
				.query("select * from AI_Model where ecm:path startswith '" + parent.getPathAsString() + "'");
		return docs;
	}

	@POST
	@Path("{projectId}/model/{modelId}/predict")
	public Response callModel(@PathParam("projectId") String projectId, @PathParam("modelId") String modelId) {

		InputStream payload;
		try {
			payload = getContext().getRequest().getInputStream();
		} catch (IOException e) {
			log.error("Unable to access InputStream", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		DocumentModel model = resolveArtifact(projectId, modelId);
		if (model == null) {
			return Response.status(Status.NOT_FOUND).build();
		}

		AICloudService service = Framework.getService(AICloudService.class);

		PredictionResult result = service.predict(model, payload);

		if (result.getResponseCode() == 200) {
			return Response.ok(result.getPayload()).build();
		} else {
			if (result.hasNoEndPoint()) {
				return Response.status(404).build();
			} else {
				return Response.status(result.getResponseCode()).build();
			}
		}
	}

}
