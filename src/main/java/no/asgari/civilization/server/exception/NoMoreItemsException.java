/*
 * Copyright (c) 2015-2021 Shervin Asgari
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package no.asgari.civilization.server.exception;

import no.asgari.civilization.server.dto.MessageDTO;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class NoMoreItemsException extends WebApplicationException {
    public NoMoreItemsException(String name) {
        super(Response.status(Response.Status.GONE)
                .entity(Entity.json(new MessageDTO("No more " + name + " to draw!")))
                .type(MediaType.APPLICATION_JSON)
                .build());
    }

}
