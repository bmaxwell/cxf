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
package org.apache.cxf.transport.jms;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.transport.jms.util.DestinationResolver;
import org.apache.cxf.transport.jms.util.JMSDestinationResolver;
import org.apache.cxf.transport.jms.util.JndiHelper;

@NoJSR250Annotations
public class JMSConfiguration {
    /**
     * Default value to mark as unset
     */
    public static final int DEFAULT_VALUE = -1;

    private boolean usingEndpointInfo = true;

    private JndiHelper jndiTemplate;
    private ConnectionFactory connectionFactory;
    private DestinationResolver destinationResolver = new JMSDestinationResolver();
    private boolean reconnectOnException = true;
    private boolean pubSubNoLocal;
    private Long clientReceiveTimeout = 60000L;
    private Long serverReceiveTimeout;
    private boolean explicitQosEnabled;
    private int deliveryMode = Message.DEFAULT_DELIVERY_MODE;
    private int priority = Message.DEFAULT_PRIORITY;
    private long timeToLive = Message.DEFAULT_TIME_TO_LIVE;
    private boolean sessionTransacted;

    private int maxSuspendedContinuations = DEFAULT_VALUE;
    private int reconnectPercentOfMax = 70;

    private volatile String messageSelector;
    private boolean subscriptionDurable;
    private String durableSubscriptionClientId;
    private String durableSubscriptionName;

    private String targetDestination;
    
    /**
     * Destination name to listen on for reply messages
     */
    private String replyDestination;
    private Destination replyDestinationDest;
    
    /**
     * Destination name to send out as replyTo address in the message 
     */
    private String replyToDestination;
    private String messageType = JMSConstants.TEXT_MESSAGE_TYPE;
    private boolean pubSubDomain;
    private boolean replyPubSubDomain;
    private Boolean useConduitIdSelector;
    private String conduitSelectorPrefix;
    private boolean jmsProviderTibcoEms;

    // For jms spec. Do not configure manually
    private String targetService;
    private String requestURI;

    private JNDIConfiguration jndiConfig;

    public void ensureProperlyConfigured() {
        if (connectionFactory == null) {
            connectionFactory = JMSFactory.getConnectionFactoryFromJndi(this);
        }
        if (connectionFactory == null) {
            throw new IllegalArgumentException("connectionFactory may not be null");
        }
        if (targetDestination == null) {
            throw new IllegalArgumentException("targetDestination may not be null");
        }
    }

    public boolean isUsingEndpointInfo() {
        return this.usingEndpointInfo;
    }

    public void setUsingEndpointInfo(boolean usingEndpointInfo) {
        this.usingEndpointInfo = usingEndpointInfo;
    }

    public boolean isPubSubNoLocal() {
        return pubSubNoLocal;
    }

    public void setPubSubNoLocal(boolean pubSubNoLocal) {
        this.pubSubNoLocal = pubSubNoLocal;
    }

    public Long getReceiveTimeout() {
        return clientReceiveTimeout;
    }

    public void setReceiveTimeout(Long receiveTimeout) {
        this.clientReceiveTimeout = receiveTimeout;
    }
    public Long getServerReceiveTimeout() {
        return serverReceiveTimeout;
    }

    public void setServerReceiveTimeout(Long receiveTimeout) {
        this.serverReceiveTimeout = receiveTimeout;
    }
    public boolean isExplicitQosEnabled() {
        return explicitQosEnabled;
    }

    public void setExplicitQosEnabled(boolean explicitQosEnabled) {
        this.explicitQosEnabled = explicitQosEnabled;
    }

    public int getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(int deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(long timeToLive) {
        this.timeToLive = timeToLive;
    }

    public String getMessageSelector() {
        return messageSelector;
    }

    public void setMessageSelector(String messageSelector) {
        this.messageSelector = messageSelector;
    }

    public void setConduitSelectorPrefix(String conduitSelectorPrefix) {
        this.conduitSelectorPrefix = conduitSelectorPrefix;
    }

    public String getConduitSelectorPrefix() {
        if (conduitSelectorPrefix == null) {
            return "";
        }
        return conduitSelectorPrefix;
    }

    public boolean isSetConduitSelectorPrefix() {
        return conduitSelectorPrefix != null;
    }

    public boolean isSubscriptionDurable() {
        return subscriptionDurable;
    }

    public void setSubscriptionDurable(boolean subscriptionDurable) {
        this.subscriptionDurable = subscriptionDurable;
    }

    public String getDurableSubscriptionName() {
        return durableSubscriptionName;
    }

    public void setDurableSubscriptionName(String durableSubscriptionName) {
        this.durableSubscriptionName = durableSubscriptionName;
    }

    public void afterPropertiesSet() throws Exception {
        if (connectionFactory == null) {
            throw new RuntimeException("Required property connectionfactory was not set");
        }
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public String getTargetDestination() {
        return targetDestination;
    }

    public void setTargetDestination(String targetDestination) {
        this.targetDestination = targetDestination;
    }

    public String getReplyDestination() {
        return replyDestination;
    }

    public void setReplyDestination(String replyDestination) {
        this.replyDestination = replyDestination;
    }

    public String getReplyToDestination() {
        return replyToDestination != null ? replyToDestination : replyDestination;
    }

    public void setReplyToDestination(String replyToDestination) {
        this.replyToDestination = replyToDestination;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public boolean isPubSubDomain() {
        return pubSubDomain;
    }

    public void setPubSubDomain(boolean pubSubDomain) {
        this.pubSubDomain = pubSubDomain;
    }
    
    public boolean isReplyPubSubDomain() {
        return replyPubSubDomain;
    }
    
    public void setReplyPubSubDomain(boolean replyPubSubDomain) {
        this.replyPubSubDomain = replyPubSubDomain;
    }

    public DestinationResolver getDestinationResolver() {
        return destinationResolver;
    }

    public void setDestinationResolver(DestinationResolver destinationResolver) {
        this.destinationResolver = destinationResolver;
    }

    public boolean isSessionTransacted() {
        return sessionTransacted;
    }

    public void setSessionTransacted(boolean sessionTransacted) {
        this.sessionTransacted = sessionTransacted;
    }

    @Deprecated
    public void setTransactionManager(Object transactionManager) {
    }

    public int getMaxSuspendedContinuations() {
        return maxSuspendedContinuations;
    }

    public void setMaxSuspendedContinuations(int maxSuspendedContinuations) {
        this.maxSuspendedContinuations = maxSuspendedContinuations;
    }

    public int getReconnectPercentOfMax() {
        return reconnectPercentOfMax;
    }

    public void setReconnectPercentOfMax(int reconnectPercentOfMax) {
        this.reconnectPercentOfMax = reconnectPercentOfMax;
    }

    public void setUseConduitIdSelector(boolean useConduitIdSelector) {
        this.useConduitIdSelector = useConduitIdSelector;
    }

    public boolean isUseConduitIdSelector() {
        if (useConduitIdSelector == null) {
            return true;
        }
        return useConduitIdSelector;
    }

    public boolean isSetUseConduitIdSelector() {
        return useConduitIdSelector != null;
    }

    public void setJndiTemplate(JndiHelper jndiTemplate) {
        this.jndiTemplate = jndiTemplate;
    }

    public JndiHelper getJndiTemplate() {
        return jndiTemplate;
    }

    public JNDIConfiguration getJndiConfig() {
        return jndiConfig;
    }

    public void setJndiConfig(JNDIConfiguration jndiConfig) {
        this.jndiConfig = jndiConfig;
    }

    public boolean isReconnectOnException() {
        return reconnectOnException;
    }

    public void setReconnectOnException(boolean reconnectOnException) {
        this.reconnectOnException = reconnectOnException;
    }

    public ConnectionFactory getConnectionFactory() {
        if (connectionFactory == null) {
            connectionFactory = JMSFactory.getConnectionFactoryFromJndi(this);
        }
        return connectionFactory;
    }
    
    public String getDurableSubscriptionClientId() {
        return durableSubscriptionClientId;
    }

    public void setDurableSubscriptionClientId(String durableSubscriptionClientId) {
        this.durableSubscriptionClientId = durableSubscriptionClientId;
    }

    public void setTargetService(String targetService) {
        this.targetService = targetService;
    }

    public String getTargetService() {
        return targetService;
    }

    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    public String getRequestURI() {
        return requestURI;
    }

    /** * @return Returns the jmsProviderTibcoEms.
     */
    public boolean isJmsProviderTibcoEms() {
        return jmsProviderTibcoEms;
    }

    /**
     * @param jmsProviderTibcoEms The jmsProviderTibcoEms to set.
     */
    public void setJmsProviderTibcoEms(boolean jmsProviderTibcoEms) {
        this.jmsProviderTibcoEms = jmsProviderTibcoEms;
    }

    public static Destination resolveOrCreateDestination(final Session session,
                                                         final DestinationResolver resolver,
                                                         final String replyToDestinationName,
                                                         final boolean pubSubDomain) throws JMSException {
        if (replyToDestinationName == null) {
            return session.createTemporaryQueue();
        }
        return resolver.resolveDestinationName(session, replyToDestinationName, pubSubDomain);
    }
    
    public Destination getReplyToDestination(Session session, String userDestination) throws JMSException {
        String replyTo = userDestination;
        if (replyTo == null) {
            return getReplyDestination(session);
        }
        return destinationResolver.resolveDestinationName(session, replyTo, replyPubSubDomain);
    }
    
    public Destination getReplyDestination(Session session) throws JMSException {
        if (replyDestinationDest == null) {
            replyDestinationDest = replyDestination == null 
                ? session.createTemporaryQueue()
                : destinationResolver.resolveDestinationName(session, replyDestination, replyPubSubDomain);
        }
        return replyDestinationDest;
    }

    public Destination getTargetDestination(Session session) throws JMSException {
        return destinationResolver.resolveDestinationName(session, targetDestination, pubSubDomain);
    }

    public Destination getReplyDestination(Session session, String replyToName) throws JMSException {
        return destinationResolver.resolveDestinationName(session, replyToName, replyPubSubDomain);
    }

}
