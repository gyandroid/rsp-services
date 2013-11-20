/*******************************************************************************
 * Copyright 2013 Marco Balduini, Emanuele Della Valle
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
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class MultipleStreamsDataServer extends ServerResource {

	/* Method to get informations about all the registered streams
	 * 
	 */
	@Get
	public void getStreamsInformations(){
		this.getResponse().setStatus(Status.SUCCESS_OK,"TODO: implement method to get informations about all registered streams");
		this.getResponse().setEntity("TODO: implement method to get informations about all registered streams", MediaType.TEXT_PLAIN);
	}
}
