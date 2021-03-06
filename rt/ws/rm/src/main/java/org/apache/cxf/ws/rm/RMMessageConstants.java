/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.ws.rm;

/**
 * A container for WS-RM message constants.
 */
public final class RMMessageConstants {
    
    /**
     * Used to cache outbound RM properties in message.
     */
    public static final String RM_PROPERTIES_OUTBOUND = "org.apache.cxf.ws.rm.outbound";
    
    /**
     * Used to cache inbound RM properties in message.
     */
    public static final String RM_PROPERTIES_INBOUND = "org.apache.cxf.ws.rm.inbound";
    
    public static final String ORIGINAL_REQUESTOR_ROLE = "org.apache.cxf.client.original";
    
    /** Message content (must be an instance of {@link RewindableInputStream}. */
    public static final String SAVED_CONTENT = "org.apache.cxf.ws.rm.content";
    
    /** Retransmission in progress flag (Boolean.TRUE if in progress). */
    public static final String RM_RETRANSMISSION = "org.apache.cxf.ws.rm.retransmitting";
    
    /** Boolean property TRUE for a chain used only to capture (not send) a message. */
    public static final String MESSAGE_CAPTURE_CHAIN = "org.apache.cxf.rm.captureOnly";
    
    /** Client callback (must be instance of {@link MessageCallback}). */
    public static final String RM_CLIENT_CALLBACK = "org.apache.cxf.rm.clientCallback";
    
    static final String RM_PROTOCOL_VARIATION = "org.apache.cxf.ws.rm.protocol";

    // keep this constant in the ws-rm package until it finds a general use outside of ws-rm
    static final String DELIVERING_ROBUST_ONEWAY = "org.apache.cxf.oneway.robust.delivering";
    
    
    /**
     * Prevents instantiation. 
     */
    private RMMessageConstants() {
    }
}
