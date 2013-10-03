package polimi.deib.rsp_service4csparql_server.query;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.UUID;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import polimi.deib.rsp_service4csparql_server.common.CsparqlComponentStatus;
import polimi.deib.rsp_service4csparql_server.configuration.Config;
import polimi.deib.rsp_service4csparql_server.observer.utilities.Observer4HTTP;
import polimi.deib.rsp_service4csparql_server.query.utilities.CsparqlObserver;
import polimi.deib.rsp_service4csparql_server.query.utilities.CsparqlQuery;
import polimi.deib.rsp_service4csparql_server.query.utilities.CsparqlQueryDescriptionForGet;
import polimi.deib.rsp_service4csparql_server.stream.utilities.CsparqlStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import eu.larkc.csparql.cep.api.RdfStream;
import eu.larkc.csparql.core.engine.CsparqlEngine;
import eu.larkc.csparql.core.engine.CsparqlQueryResultProxy;
import eu.larkc.csparql.core.engine.RDFStreamFormatter;

public class SingleQueryDataServer extends ServerResource {

	private static Hashtable<String, CsparqlQuery> csparqlQueryTable;
	private static Hashtable<String, CsparqlStream> csparqlStreamTable;
	private CsparqlEngine engine;
	private Gson gson = new GsonBuilder().disableHtmlEscaping().create();

	private Logger logger = LoggerFactory.getLogger(SingleQueryDataServer.class.getName());

	@SuppressWarnings("unchecked")
	@Put
	public void registerQuery(Representation rep){

		ArrayList<String> inputStreamNameList;
		String extractedQueryName;
		String parameterQueryName;
		boolean queryStreamWellRegistered = true;

		csparqlStreamTable = (Hashtable<String, CsparqlStream>) getContext().getAttributes().get("csaprqlinputStreamTable");
		csparqlQueryTable = (Hashtable<String, CsparqlQuery>) getContext().getAttributes().get("csaprqlQueryTable");
		engine = (CsparqlEngine) getContext().getAttributes().get("csparqlengine");
		String server_address = (String) getContext().getAttributes().get("complete_server_address");

		try{
			String queryBody = rep.getText();

			parameterQueryName = (String) this.getRequest().getAttributes().get("queryname");
			extractedQueryName = extractNameFromQuery(queryBody);
			inputStreamNameList = extractStreamNamesFromQuery(queryBody);
			String queryURI = new String();

			if(parameterQueryName.equals(extractedQueryName)){
				queryURI = server_address + "/queries/" + parameterQueryName;
				if(!csparqlQueryTable.containsKey(parameterQueryName)){
					//					for(int i = 0 ; i < 2 ; i++){
					//						if(!checkInputStream(inputStreamNameList))
					//							Thread.sleep(500);
					//					}
					if(checkInputStream(inputStreamNameList)){
						String queryType = extractQueryType(queryBody);
						if(queryType.equals("stream")){
							String newStreamName = Config.getInstance().getStreamBaseUri() + parameterQueryName;
							if(!csparqlStreamTable.contains(newStreamName)){
								CsparqlQueryResultProxy rp = engine.registerQuery(queryBody);
								csparqlQueryTable.put(parameterQueryName, new CsparqlQuery(rp.getId(), parameterQueryName, queryType, inputStreamNameList, queryBody, rp, new HashMap<String, CsparqlObserver>(), CsparqlComponentStatus.RUNNING));
								RDFStreamFormatter stream = new RDFStreamFormatter(newStreamName);
								csparqlStreamTable.put(newStreamName, new CsparqlStream(stream, CsparqlComponentStatus.RUNNING));
								RdfStream rdfStream = null;
								try{
									rdfStream = engine.registerStream(stream);
								} catch (Exception e){
									queryStreamWellRegistered = false;
								}
								if(rdfStream == null){
									queryStreamWellRegistered = false;
								}
								if(!queryStreamWellRegistered){
									engine.unregisterQuery(rp.getId());
									this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,"Error while registering stream created by stream query");
									this.getResponse().setEntity(gson.toJson("Error while registering stream created by stream query"), MediaType.APPLICATION_JSON);									
								} else {
									rp.addObserver(stream);
									getContext().getAttributes().put("csaprqlinputStreamTable", csparqlStreamTable);
									getContext().getAttributes().put("csaprqlQueryTable", csparqlQueryTable);
									getContext().getAttributes().put("csparqlengine", engine);
									this.getResponse().setStatus(Status.SUCCESS_OK,"Query and stream " + newStreamName + " succesfully registered");
									this.getResponse().setEntity(gson.toJson(queryURI), MediaType.APPLICATION_JSON);
								}
							} else {		
								this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,"Stream query name corresponds to a stream already registered. Change query name.");
								this.getResponse().setEntity(gson.toJson("Stream query name corresponds to a stream already registered. Change query name."), MediaType.APPLICATION_JSON);
							}
						} else {
							CsparqlQueryResultProxy rp = engine.registerQuery(queryBody);
							csparqlQueryTable.put(parameterQueryName, new CsparqlQuery(rp.getId(), parameterQueryName, queryType, inputStreamNameList, queryBody, rp, new HashMap<String, CsparqlObserver>(), CsparqlComponentStatus.RUNNING));
							getContext().getAttributes().put("csaprqlinputStreamTable", csparqlStreamTable);
							getContext().getAttributes().put("csaprqlQueryTable", csparqlQueryTable);
							getContext().getAttributes().put("csparqlengine", engine);
							this.getResponse().setStatus(Status.SUCCESS_OK,"Query succesfully registered");
							this.getResponse().setEntity(gson.toJson(queryURI), MediaType.APPLICATION_JSON);
						}

					} else {
						this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,"One or more of the specified input stream not exist");
						this.getResponse().setEntity(gson.toJson("One or more of the specified input stream not exist"), MediaType.APPLICATION_JSON);
					}
				}else {
					this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,"Query with same name already exists");
					this.getResponse().setEntity(gson.toJson("Query with same name already exists"), MediaType.APPLICATION_JSON);
				}
			} else {
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,"Query name specified in the uri must be equals to the name contained in the query body");
				this.getResponse().setEntity(gson.toJson("Query name specified in the uri must be equals to the name contained in the query body"), MediaType.APPLICATION_JSON);
			}
		} catch (ParseException e) {
			logger.error("Error while parsing query",e);
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,"Error while parsing query");
			this.getResponse().setEntity(gson.toJson("Error while parsing query"), MediaType.APPLICATION_JSON);
		} catch (IOException e) {
			logger.error("Error while reading query body",e);
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,"Error while reading query body");
			this.getResponse().setEntity(gson.toJson("Error while reading query body"), MediaType.APPLICATION_JSON);
		} finally{
			this.getResponse().commit();
			this.commit();	
			this.release();
		}

	}

	@SuppressWarnings("unchecked")
	@Delete
	public void unregisterQuery(){

		csparqlStreamTable = (Hashtable<String, CsparqlStream>) getContext().getAttributes().get("csaprqlinputStreamTable");
		csparqlQueryTable = (Hashtable<String, CsparqlQuery>) getContext().getAttributes().get("csaprqlQueryTable");
		engine = (CsparqlEngine) getContext().getAttributes().get("csparqlengine");
		String server_address = (String) getContext().getAttributes().get("complete_server_address");

		String queryURI = (String) this.getRequest().getAttributes().get("queryname");
		String queryName = queryURI.replace(server_address + "/queries/", "");

		try{
			if(csparqlQueryTable.containsKey(queryName)){
				CsparqlQuery csparqlQuery = csparqlQueryTable.get(queryName);
				if(csparqlQuery.getType().equals("stream")){
					String newStreamName = Config.getInstance().getStreamBaseUri() + csparqlQuery.getName();
					if(csparqlStreamTable.contains(newStreamName)){
						engine.unregisterStream(newStreamName);
						csparqlStreamTable.remove(newStreamName);
						engine.unregisterQuery(queryName);
						csparqlQueryTable.remove(queryName);
						getContext().getAttributes().put("csaprqlinputStreamTable", csparqlStreamTable);
						getContext().getAttributes().put("csaprqlQueryTable", csparqlQueryTable);
						getContext().getAttributes().put("csparqlengine", engine);
						this.getResponse().setStatus(Status.SUCCESS_OK,"Query and stream " + newStreamName + " succesfully unregistered");
						this.getResponse().setEntity(gson.toJson("Query and stream " + newStreamName + " succesfully unregistered"), MediaType.APPLICATION_JSON);
					}
				} else {
					engine.unregisterQuery(csparqlQueryTable.get(queryName).getId());
					csparqlQueryTable.remove(queryName);
					getContext().getAttributes().put("csaprqlinputStreamTable", csparqlStreamTable);
					getContext().getAttributes().put("csaprqlQueryTable", csparqlQueryTable);
					getContext().getAttributes().put("csparqlengine", engine);
					this.getResponse().setStatus(Status.SUCCESS_OK,queryURI + " succesfully unregistered");
					this.getResponse().setEntity(gson.toJson(queryURI + " succesfully unregistered"), MediaType.APPLICATION_JSON);
				}
			} else {
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,queryURI + " ID is not associated to any registered query");
				this.getResponse().setEntity(gson.toJson(queryURI + " ID is not associated to any registered query"), MediaType.APPLICATION_JSON);
			}
		} catch (Exception e) {
			logger.error("Error while unregistering query" + queryURI, e);
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,"Error while unregistering query" + queryURI);
			this.getResponse().setEntity(gson.toJson("Error while unregistering query" + queryURI), MediaType.APPLICATION_JSON);
		} finally{
			this.getResponse().commit();
			this.commit();	
			this.release();
		}

	}

	@SuppressWarnings("unchecked")
	@Post
	public void changeQueryStatus(Representation rep){

		csparqlStreamTable = (Hashtable<String, CsparqlStream>) getContext().getAttributes().get("csaprqlinputStreamTable");
		csparqlQueryTable = (Hashtable<String, CsparqlQuery>) getContext().getAttributes().get("csaprqlQueryTable");
		engine = (CsparqlEngine) getContext().getAttributes().get("csparqlengine");
		String server_address = (String) getContext().getAttributes().get("complete_server_address");
		try{

			String callbackUrl = rep.getText();
			String action = null;

			if(callbackUrl.startsWith("action="))
				action = callbackUrl.substring(callbackUrl.indexOf("=") + 1, callbackUrl.length());

			String queryURI = (String) this.getRequest().getAttributes().get("queryname");
			String queryName = queryURI.replace(server_address + "/queries/", "");

			if(csparqlQueryTable.containsKey(queryName)){
				CsparqlQuery csparqlQuery = csparqlQueryTable.get(queryName);
				if(action == null){
					String observerID = UUID.randomUUID().toString();
					String observerURI = server_address + "/queries/" + queryName + "/observers/" + observerID;
					CsparqlObserver csObs = new CsparqlObserver(observerID, new Observer4HTTP(callbackUrl, Config.getInstance().getSendEmptyResultsProperty()));
					csparqlQuery.addObserver(csObs);
					this.getResponse().setStatus(Status.SUCCESS_OK,"Observer succesfully registered");
					this.getResponse().setEntity(gson.toJson(observerURI), MediaType.APPLICATION_JSON);	
				} else {
					if(action.equals("pause")){
						if(!csparqlQuery.getStatus().equals(CsparqlComponentStatus.PAUSED)){
							engine.stopQuery(csparqlQuery.getId());
							csparqlQuery.setStatus(CsparqlComponentStatus.PAUSED);
							csparqlQueryTable.put(queryName, csparqlQuery);
							this.getResponse().setStatus(Status.SUCCESS_OK,queryURI + " succesfully paused");
							this.getResponse().setEntity(gson.toJson(queryURI + " succesfully paused"), MediaType.APPLICATION_JSON);
						} else {
							this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,queryURI + " is already paused");
							this.getResponse().setEntity(gson.toJson(queryURI + " is already paused"), MediaType.APPLICATION_JSON);
						}
					} else if(action.equals("restart")){
						if(!csparqlQuery.getStatus().equals(CsparqlComponentStatus.RUNNING)){
							engine.startQuery(csparqlQuery.getId());
							csparqlQuery.setStatus(CsparqlComponentStatus.RUNNING);
							csparqlQueryTable.put(queryName, csparqlQuery);
							this.getResponse().setStatus(Status.SUCCESS_OK,queryURI + " succesfully restarted");
							this.getResponse().setEntity(gson.toJson(queryURI + " succesfully restarted"), MediaType.APPLICATION_JSON);
						} else {
							this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,queryURI + " is already running");
							this.getResponse().setEntity(gson.toJson(queryURI + " is already running"), MediaType.APPLICATION_JSON);
						}
					} else {
						this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,"Problem with specified action. The action must be pause or restart");
						this.getResponse().setEntity(gson.toJson("Problem with specified action. The action must be pause or restart"), MediaType.APPLICATION_JSON);
					}
				}
				getContext().getAttributes().put("csaprqlQueryTable", csparqlQueryTable);
				getContext().getAttributes().put("csparqlengine", engine);
			} else {
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,queryURI + " ID is not associated to any registered query");
				this.getResponse().setEntity(gson.toJson(queryURI + " ID is not associated to any registered query"), MediaType.APPLICATION_JSON);
			}
		} catch (Exception e) {
			logger.error("Error while changing query status", e);
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,"Error during query operations");
			this.getResponse().setEntity(gson.toJson("Error during query operations"), MediaType.APPLICATION_JSON);
		} finally{
			this.getResponse().commit();
			this.commit();	
			this.release();
		}
	}

	//	@SuppressWarnings("unchecked")
	//	@Post("text/plain")
	//	public void addObserver(Representation rep){
	//
	//		csparqlStreamTable = (Hashtable<String, CsparqlStream>) getContext().getAttributes().get("csaprqlinputStreamTable");
	//		csparqlQueryTable = (Hashtable<String, CsparqlQuery>) getContext().getAttributes().get("csaprqlQueryTable");
	//		engine = (CsparqlEngine) getContext().getAttributes().get("csparqlengine");
	//
	//		String queryName = (String) this.getRequest().getAttributes().get("queryname");
	//
	//		try{
	//			if(csparqlQueryTable.containsKey(queryName)){
	//				CsparqlQuery csparqlQuery = csparqlQueryTable.get(queryName);
	//
	//				String callbackUrl = rep.getText();
	//				String observerId = UUID.randomUUID().toString();
	//				CsparqlObserver csObs = new CsparqlObserver(observerId, new ResultObserver(callbackUrl));
	//				csparqlQuery.addObserver(csObs);
	//				this.getResponse().setStatus(Status.SUCCESS_OK,"Observer succesfully registered");
	//				this.getResponse().setEntity(gson.toJson(observerId), MediaType.APPLICATION_JSON);					
	//			} else {
	//				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,"The specified ID is not associated to any registered query");
	//				this.getResponse().setEntity(gson.toJson("The specified ID is not associated to any registered query"), MediaType.APPLICATION_JSON);
	//			}
	//			getContext().getAttributes().put("csaprqlQueryTable", csparqlQueryTable);
	//			getContext().getAttributes().put("csparqlengine", engine);
	//
	//		} catch (Exception e) {
	//			logger.error("Error while adding new observer", e);
	//			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,"Error while adding new observer");
	//			this.getResponse().setEntity(gson.toJson("Error while adding new observer"), MediaType.APPLICATION_JSON);
	//		} finally{
	//			this.getResponse().commit();
	//			this.commit();	
	//			this.release();
	//		}
	//	}
	//
	@SuppressWarnings("unchecked")
	@Get
	public void getQueryInformations(){

		csparqlStreamTable = (Hashtable<String, CsparqlStream>) getContext().getAttributes().get("csaprqlinputStreamTable");
		csparqlQueryTable = (Hashtable<String, CsparqlQuery>) getContext().getAttributes().get("csaprqlQueryTable");
		engine = (CsparqlEngine) getContext().getAttributes().get("csparqlengine");
		//			String server_address = (String) getContext().getAttributes().get("complete_server_address");

		String server_address = (String) getContext().getAttributes().get("complete_server_address");

		String queryURI = (String) this.getRequest().getAttributes().get("queryname");
		String queryName = queryURI.replace(server_address + "/queries/", "");

		try{
			if(csparqlQueryTable.containsKey(queryName)){
				CsparqlQuery csparqlQuery = csparqlQueryTable.get(queryName);
				System.out.println(gson.toJson(new CsparqlQueryDescriptionForGet(csparqlQuery.getId(), csparqlQuery.getName(), csparqlQuery.getType(), csparqlQuery.getStreams(), csparqlQuery.getBody(), csparqlQuery.getStatus())));
				this.getResponse().setStatus(Status.SUCCESS_OK,queryName + " succesfully unregistered");
				this.getResponse().setEntity(gson.toJson(new CsparqlQueryDescriptionForGet(csparqlQuery.getId(), csparqlQuery.getName(), csparqlQuery.getType(), csparqlQuery.getStreams(), csparqlQuery.getBody(), csparqlQuery.getStatus())), MediaType.APPLICATION_JSON);

			} else {
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,queryName + " ID is not associated to any registered query");
				this.getResponse().setEntity(gson.toJson(queryName + " ID is not associated to any registered query"), MediaType.APPLICATION_JSON);
			}
		} catch (Exception e) {
			logger.error("Error while getting information about query", e);
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,"Error while getting information about " + queryURI);
			this.getResponse().setEntity(gson.toJson("Error while getting information about " + queryURI), MediaType.APPLICATION_JSON);
		} finally{
			this.getResponse().commit();
			this.commit();	
			this.release();
		}

	}

	private ArrayList<String> extractStreamNamesFromQuery(String query){
		String tempQuery = query;
		ArrayList<String> streamNameList = new ArrayList<String>();

		int index = tempQuery.indexOf("FROM STREAM ");

		while(index != -1){

			tempQuery = tempQuery.substring(index + 12, tempQuery.length());
			streamNameList.add(tempQuery.substring(tempQuery.indexOf("<") + 1, tempQuery.indexOf(">")));

			index = tempQuery.indexOf("FROM STREAM ");

		}

		return streamNameList;
	}

	private String extractNameFromQuery(String query){
		String tempQuery = query;

		int index = tempQuery.indexOf("REGISTER QUERY ");
		if(index != -1){
			return tempQuery.substring(index + 15, tempQuery.indexOf("AS") - 1);
		} else {
			index = tempQuery.indexOf("REGISTER STREAM ");
			return tempQuery.substring(index + 16, tempQuery.indexOf("AS") - 1);
		}
	}

	private String extractQueryType(String query){
		if(query.contains("REGISTER STREAM"))
			return "stream";
		else
			return "query";
	}

	private boolean checkInputStream(ArrayList<String> streamList){
		for(String stream : streamList){
			if(!csparqlStreamTable.containsKey(stream)){
				return false;
			}
		}
		return true;
	}
}
