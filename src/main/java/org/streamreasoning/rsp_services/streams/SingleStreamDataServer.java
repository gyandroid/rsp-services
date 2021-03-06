/*******************************************************************************
 * Copyright 2013
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.streamreasoning.rsp_services.streams;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;

public class SingleStreamDataServer extends ServerResource {

	/**
	 * Method to register new stream
	 * The implementation of this method needs to call the method registerStream 
	 * of a class that implements RDF_Stream_Processor_Interface interface
	 */
	@Put
	public void registerStream(){
		this.getResponse().setStatus(Status.SUCCESS_OK,"TODO: implement method to register new stream");
		this.getResponse().setEntity("TODO: implement method to register new stream", MediaType.TEXT_PLAIN);
	}

	/**
	 * Method to delete stream 
	 * The implementation of this method needs to call the method unregisterStream 
	 * of a class that implements RDF_Stream_Processor_Interface interface
	 */
	@Delete
	public void unregisterStream(){
		this.getResponse().setStatus(Status.SUCCESS_OK,"TODO: implement method to unregister stream");
		this.getResponse().setEntity("TODO: implement method to unregister stream", MediaType.TEXT_PLAIN);
	}

	/**
	 * Method to feed registered stream 
	 * The implementation of this method needs to call the method feed_RDF_stream 
	 * of a class that implements RDF_Stream_Interface interface
	 */
	@Post
	public void feedStream(Representation rep){
		this.getResponse().setStatus(Status.SUCCESS_OK,"TODO: implement method to feed stream");
		this.getResponse().setEntity("TODO: implement method to feed stream", MediaType.TEXT_PLAIN);
	}

	/**
	 * Method to get informations about single registered stream 
	 */
	@Get
	public void getStreamInformations(){
		this.getResponse().setStatus(Status.SUCCESS_OK,"TODO: implement method to get information about specific stream");
		this.getResponse().setEntity("TODO: implement method to get information about specific stream", MediaType.TEXT_PLAIN);
	}
}
