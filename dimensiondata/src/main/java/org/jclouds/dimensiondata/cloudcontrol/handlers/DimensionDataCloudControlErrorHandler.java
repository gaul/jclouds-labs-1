/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.dimensiondata.cloudcontrol.handlers;

import org.jclouds.http.HttpCommand;
import org.jclouds.http.HttpErrorHandler;
import org.jclouds.http.HttpResponse;
import org.jclouds.http.HttpResponseException;
import org.jclouds.rest.AuthorizationException;
import org.jclouds.rest.ResourceAlreadyExistsException;
import org.jclouds.rest.ResourceNotFoundException;

import javax.inject.Singleton;

import java.util.ConcurrentModificationException;

import static org.jclouds.http.HttpUtils.closeClientButKeepContentStream;

/**
 * This will org.jclouds.dimensiondata.cloudcontrol.parse and set an appropriate exception on the command object.
 */
@Singleton
public class DimensionDataCloudControlErrorHandler implements HttpErrorHandler {

   public void handleError(HttpCommand command, HttpResponse response) {
      // it is important to always read fully and close streams
      byte[] data = closeClientButKeepContentStream(response);
      String message = data != null ? new String(data) : null;

      Exception exception = message != null ?
            new HttpResponseException(command, response, message) :
            new HttpResponseException(command, response);
      message = message != null ?
            message :
            String.format("%s -> %s", command.getCurrentRequest().getRequestLine(), response.getStatusLine());
      switch (response.getStatusCode()) {
         case 400:
            if (message.contains("RESOURCE_NOT_FOUND")) {
               exception = new ResourceNotFoundException(message, exception);
            } else if (message.contains("OPERATION_NOT_SUPPORTED")) {
               exception = new UnsupportedOperationException(message, exception);
            } else if (message.contains("RESOURCE_BUSY")) {
               exception = new ConcurrentModificationException(message, exception);
            } else if (message.contains("RESOURCE_LOCKED")) {
               exception = new IllegalStateException(message, exception);
            } else if (message.contains("NAME_NOT_UNIQUE")) {
               exception = new ResourceAlreadyExistsException(message, exception);
            } else if (message.contains("UNEXPECTED_ERROR")
                  || message.contains("RETRYABLE_SYSTEM_ERROR")
                  || message.contains("SYSTEM_ERROR")) {
               break;
            } else {
               exception = new IllegalArgumentException(message, exception);
            }
            break;
         case 401:
            exception = new AuthorizationException(message, exception);
            break;
         case 403:
            exception = new AuthorizationException(message, exception);
            break;
         case 404:
            // CloudControl uses error code 400 with RESOURCE_NOT_FOUND to report missing assets
            // 404 means malformed URI only
            exception = new IllegalArgumentException(message, exception);
            break;
      }
      command.setException(exception);
   }
}
