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

package org.apache.cxf.ws.security.policy.interceptors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.SoapActionInInterceptor;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.trust.DefaultSymmetricBinding;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JStaxInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JUtils;
import org.apache.neethi.All;
import org.apache.neethi.Assertion;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.apache.wss4j.dom.message.token.SecurityContextToken;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.SPConstants.IncludeTokenType;
import org.apache.wss4j.policy.model.AbstractBinding;
import org.apache.wss4j.policy.model.Header;
import org.apache.wss4j.policy.model.ProtectionToken;
import org.apache.wss4j.policy.model.SecureConversationToken;
import org.apache.wss4j.policy.model.SignedParts;
import org.apache.wss4j.policy.model.Trust10;
import org.apache.wss4j.policy.model.Trust13;
import org.apache.xml.security.utils.Base64;

class SecureConversationInInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
    
    public SecureConversationInInterceptor() {
        super(Phase.PRE_STREAM);
        getBefore().add(WSS4JStaxInInterceptor.class.getName());
    }
    private AbstractBinding getBinding(AssertionInfoMap aim) {
        Collection<AssertionInfo> ais = 
            NegotiationUtils.getAllAssertionsByLocalname(aim, SPConstants.SYMMETRIC_BINDING);
        if (!ais.isEmpty()) {
            return (AbstractBinding)ais.iterator().next().getAssertion();
        }
        ais = NegotiationUtils.getAllAssertionsByLocalname(aim, SPConstants.ASYMMETRIC_BINDING);
        if (!ais.isEmpty()) {
            return (AbstractBinding)ais.iterator().next().getAssertion();
        }
        ais = NegotiationUtils.getAllAssertionsByLocalname(aim, SPConstants.TRANSPORT_BINDING);
        if (!ais.isEmpty()) {
            return (AbstractBinding)ais.iterator().next().getAssertion();
        }
        return null;
    }
    
    public void handleMessage(SoapMessage message) throws Fault {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        // extract Assertion information
        if (aim != null) {
            Collection<AssertionInfo> ais = 
                NegotiationUtils.getAllAssertionsByLocalname(aim, SPConstants.SECURE_CONVERSATION_TOKEN);
            if (ais.isEmpty()) {
                return;
            }
            if (isRequestor(message)) {
                //client side should be checked on the way out
                for (AssertionInfo ai : ais) {
                    ai.setAsserted(true);
                }
                assertPolicies(aim);
                
                Object s = message.getContextualProperty(SecurityConstants.STS_TOKEN_DO_CANCEL);
                if (s != null && (Boolean.TRUE.equals(s) || "true".equalsIgnoreCase(s.toString()))) {
                    message.getInterceptorChain().add(SecureConversationCancelInterceptor.INSTANCE);
                }
                return;
            }
            String s = (String)message.get(SoapBindingConstants.SOAP_ACTION);
            if (s == null) {
                s = SoapActionInInterceptor.getSoapAction(message);
            }
            String addNs = null;
            AddressingProperties inProps = (AddressingProperties)message
                .getContextualProperty(JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_INBOUND);
            if (inProps != null) {
                addNs = inProps.getNamespaceURI();
                if (s == null) {
                    //MS/WCF doesn't put a soap action out for this, must check the headers
                    s = inProps.getAction().getValue();
                }
            }

            if (s != null 
                && s.contains("/RST/SCT")
                && (s.startsWith(STSUtils.WST_NS_05_02)
                    || s.startsWith(STSUtils.WST_NS_05_12))) {

                SecureConversationToken tok = (SecureConversationToken)ais.iterator()
                    .next().getAssertion();
                Policy pol = tok.getBootstrapPolicy().getPolicy();
                if (s.endsWith("Cancel") || s.endsWith("/Renew")) {
                    //Cancel and Renew just sign with the token
                    Policy p = new Policy();
                    ExactlyOne ea = new ExactlyOne();
                    p.addPolicyComponent(ea);
                    All all = new All();
                    Assertion ass = NegotiationUtils.getAddressingPolicy(aim, false);
                    all.addPolicyComponent(ass);
                    ea.addPolicyComponent(all);
                    
                    final SecureConversationToken secureConversationToken = 
                        new SecureConversationToken(
                            SPConstants.SPVersion.SP12,
                            SPConstants.IncludeTokenType.INCLUDE_TOKEN_NEVER,
                            null,
                            null,
                            null,
                            new Policy()
                        );
                    
                    Policy sctPolicy = new Policy();
                    ExactlyOne sctPolicyEa = new ExactlyOne();
                    sctPolicy.addPolicyComponent(sctPolicyEa);
                    All sctPolicyAll = new All();
                    sctPolicyAll.addPolicyComponent(secureConversationToken);
                    sctPolicyEa.addPolicyComponent(sctPolicyAll);
                    
                    Policy bindingPolicy = new Policy();
                    ExactlyOne bindingPolicyEa = new ExactlyOne();
                    bindingPolicy.addPolicyComponent(bindingPolicyEa);
                    All bindingPolicyAll = new All();
                    
                    AbstractBinding origBinding = getBinding(aim);
                    bindingPolicyAll.addPolicyComponent(origBinding.getAlgorithmSuite());
                    bindingPolicyAll.addPolicyComponent(new ProtectionToken(SPConstants.SPVersion.SP12, sctPolicy));
                    bindingPolicyAll.addAssertion(
                        new PrimitiveAssertion(SP12Constants.INCLUDE_TIMESTAMP));
                    bindingPolicyAll.addAssertion(
                        new PrimitiveAssertion(SP12Constants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY));
                    bindingPolicyEa.addPolicyComponent(bindingPolicyAll);
                    
                    DefaultSymmetricBinding binding = 
                        new DefaultSymmetricBinding(SPConstants.SPVersion.SP12, bindingPolicy);
                    binding.setOnlySignEntireHeadersAndBody(true);
                    binding.setProtectTokens(false);
                    
                    all.addPolicyComponent(binding);
                    
                    SignedParts signedParts = getSignedParts(aim, addNs);
                    all.addPolicyComponent(signedParts);
                    pol = p;
                    message.getInterceptorChain().add(SecureConversationTokenFinderInterceptor.INSTANCE);
                } else {
                    Policy p = new Policy();
                    ExactlyOne ea = new ExactlyOne();
                    p.addPolicyComponent(ea);
                    All all = new All();
                    Assertion ass = NegotiationUtils.getAddressingPolicy(aim, false);
                    all.addPolicyComponent(ass);
                    ea.addPolicyComponent(all);
                    pol = p.merge(pol);
                }
                
                //setup SCT endpoint and forward to it.
                unmapSecurityProps(message);
                String ns = STSUtils.WST_NS_05_12;
                if (s.startsWith(STSUtils.WST_NS_05_02)) {
                    ns = STSUtils.WST_NS_05_02;
                }
                NegotiationUtils.recalcEffectivePolicy(message, ns, pol, 
                                                       new SecureConversationSTSInvoker(),
                                                       true);
                //recalc based on new endpoint
                SoapActionInInterceptor.getAndSetOperation(message, s);
            } else {
                message.getInterceptorChain().add(SecureConversationTokenFinderInterceptor.INSTANCE);
            }
            
            assertPolicies(aim);
        }
    }
    
    private SignedParts getSignedParts(AssertionInfoMap aim, String addNs) {
        Collection<AssertionInfo> signedPartsAis = 
            NegotiationUtils.getAllAssertionsByLocalname(aim, SPConstants.SIGNED_PARTS);
        SignedParts signedParts = null;
        if (!signedPartsAis.isEmpty()) {
            signedParts = (SignedParts)signedPartsAis.iterator().next().getAssertion();
        }
        if (signedParts == null) {
            List<Header> headers = new ArrayList<Header>();
            if (addNs != null) {
                headers.add(new Header("To", addNs));
                headers.add(new Header("From", addNs));
                headers.add(new Header("FaultTo", addNs));
                headers.add(new Header("ReplyTo", addNs));
                headers.add(new Header("Action", addNs));
                headers.add(new Header("MessageID", addNs));
                headers.add(new Header("RelatesTo", addNs));
            }
            
            signedParts = 
                new SignedParts(SPConstants.SPVersion.SP12, true, null, headers, false);
        }
        return signedParts;
    }
    
    private void assertPolicies(AssertionInfoMap aim) {
        NegotiationUtils.assertPolicy(aim, SPConstants.BOOTSTRAP_POLICY);
        NegotiationUtils.assertPolicy(aim, SPConstants.MUST_NOT_SEND_AMEND);
        NegotiationUtils.assertPolicy(aim, SPConstants.MUST_NOT_SEND_CANCEL);
        NegotiationUtils.assertPolicy(aim, SPConstants.MUST_NOT_SEND_RENEW);
        QName oldCancelQName = 
            new QName(
                "http://schemas.microsoft.com/ws/2005/07/securitypolicy", 
                SPConstants.MUST_NOT_SEND_CANCEL
            );
        NegotiationUtils.assertPolicy(aim, oldCancelQName);
    }

    private void unmapSecurityProps(Message message) {
        Exchange ex = message.getExchange();
        for (String s : SecurityConstants.ALL_PROPERTIES) {
            Object v = message.getContextualProperty(s + ".sct");
            if (v == null) {
                v = message.getContextualProperty(s);
            }
            if (v != null) {
                ex.put(s, v);
            }
        }
    }

    public class SecureConversationSTSInvoker extends STSInvoker {

        void doIssue(
            Element requestEl,
            Exchange exchange,
            Element binaryExchange,
            W3CDOMStreamWriter writer,
            String prefix, 
            String namespace
        ) throws Exception {
            if (STSUtils.WST_NS_05_12.equals(namespace)) {
                writer.writeStartElement(prefix, "RequestSecurityTokenResponseCollection", namespace);
            }
            writer.writeStartElement(prefix, "RequestSecurityTokenResponse", namespace);
            
            byte clientEntropy[] = null;
            int keySize = 256;
            long ttl = 300000L;
            String tokenType = null;
            Element el = DOMUtils.getFirstElement(requestEl);
            while (el != null) {
                String localName = el.getLocalName();
                if (namespace.equals(el.getNamespaceURI())) {
                    if ("Entropy".equals(localName)) {
                        Element bs = DOMUtils.getFirstElement(el);
                        if (bs != null) {
                            clientEntropy = Base64.decode(bs.getTextContent());
                        }
                    } else if ("KeySize".equals(localName)) {
                        keySize = Integer.parseInt(el.getTextContent());
                    } else if ("TokenType".equals(localName)) {
                        tokenType = el.getTextContent();
                    }
                }
                
                el = DOMUtils.getNextElement(el);
            }
            
            // Check received KeySize
            if (keySize < 128 || keySize > 512) {
                keySize = 256;
            }
            
            writer.writeStartElement(prefix, "RequestedSecurityToken", namespace);
            SecurityContextToken sct =
                new SecurityContextToken(NegotiationUtils.getWSCVersion(tokenType), writer.getDocument());
            
            Date created = new Date();
            Date expires = new Date();
            expires.setTime(created.getTime() + ttl);
            
            SecurityToken token = new SecurityToken(sct.getIdentifier(), created, expires);
            token.setToken(sct.getElement());
            token.setTokenType(sct.getTokenType());
            
            writer.getCurrentNode().appendChild(sct.getElement());
            writer.writeEndElement();        
            
            writer.writeStartElement(prefix, "RequestedAttachedReference", namespace);
            token.setAttachedReference(
                writeSecurityTokenReference(writer, "#" + sct.getID(), tokenType)
            );
            writer.writeEndElement();
            
            writer.writeStartElement(prefix, "RequestedUnattachedReference", namespace);
            token.setUnattachedReference(
                writeSecurityTokenReference(writer, sct.getIdentifier(), tokenType)
            );
            writer.writeEndElement();
            
            writeLifetime(writer, created, expires, prefix, namespace);

            byte[] secret = writeProofToken(prefix, namespace, writer, clientEntropy, keySize);
            
            token.setSecret(secret);
            
            SecurityContext sc = exchange.getInMessage().get(SecurityContext.class);
            if (sc != null) {
                token.setSecurityContext(sc);
            }
            
            // Get Bootstrap Token
            SecurityToken bootstrapToken = getBootstrapToken(exchange.getInMessage());
            if (bootstrapToken != null) {
                Properties properties = new Properties();
                properties.put(SecurityToken.BOOTSTRAP_TOKEN_ID, bootstrapToken.getId());
                token.setProperties(properties);
            }
            
            ((TokenStore)exchange.get(Endpoint.class).getEndpointInfo()
                    .getProperty(TokenStore.class.getName())).add(token);
            
            
            writer.writeEndElement();
            if (STSUtils.WST_NS_05_12.equals(namespace)) {
                writer.writeEndElement();
            }
        }

        private SecurityToken getBootstrapToken(Message message) {
            SecurityToken st = (SecurityToken)message.getContextualProperty(SecurityConstants.TOKEN);
            if (st == null) {
                String id = (String)message.getContextualProperty(SecurityConstants.TOKEN_ID);
                if (id != null) {
                    st = WSS4JUtils.getTokenStore(message).getToken(id);
                }
            }
            return st;
        }
    }
    
    
    static final class SecureConversationTokenFinderInterceptor 
        extends AbstractPhaseInterceptor<SoapMessage> {
        
        static final SecureConversationTokenFinderInterceptor INSTANCE 
            = new SecureConversationTokenFinderInterceptor();
        
        private SecureConversationTokenFinderInterceptor() {
            super(Phase.PRE_PROTOCOL);
            addAfter(WSS4JInInterceptor.class.getName());
        }

        public void handleMessage(SoapMessage message) throws Fault {
            boolean foundSCT = NegotiationUtils.parseSCTResult(message);

            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                Collection<AssertionInfo> ais = 
                    NegotiationUtils.getAllAssertionsByLocalname(aim, SPConstants.SECURE_CONVERSATION_TOKEN);
                if (ais.isEmpty()) {
                    return;
                }
                for (AssertionInfo inf : ais) {
                    SecureConversationToken token = (SecureConversationToken)inf.getAssertion();
                    IncludeTokenType inclusion = token.getIncludeTokenType();
                    if (foundSCT || token.isOptional()
                        || (!foundSCT && inclusion == IncludeTokenType.INCLUDE_TOKEN_NEVER)) {
                        inf.setAsserted(true);
                    } else {
                        inf.setNotAsserted("No SecureConversation token found in message.");
                    }
                }
            }
        }
    }
    
    static class SecureConversationCancelInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
        static final SecureConversationCancelInterceptor INSTANCE = new SecureConversationCancelInterceptor();
        
        public SecureConversationCancelInterceptor() {
            super(Phase.POST_LOGICAL);
        }
        
        public void handleMessage(SoapMessage message) throws Fault {
            // TODO Auto-generated method stub
            
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim == null) {
                return;
            }
            Collection<AssertionInfo> ais = 
                NegotiationUtils.getAllAssertionsByLocalname(aim, SPConstants.SECURE_CONVERSATION_TOKEN);
            if (ais.isEmpty()) {
                return;
            }
            
            SecureConversationToken tok = (SecureConversationToken)ais.iterator()
                .next().getAssertion();
            doCancel(message, aim, tok);

        }
        
        private void doCancel(SoapMessage message, AssertionInfoMap aim, SecureConversationToken itok) {
            Message m2 = message.getExchange().getOutMessage();
            
            SecurityToken tok = (SecurityToken)m2.getContextualProperty(SecurityConstants.TOKEN);
            if (tok == null) {
                String tokId = (String)m2.getContextualProperty(SecurityConstants.TOKEN_ID);
                if (tokId != null) {
                    tok = NegotiationUtils.getTokenStore(m2).getToken(tokId);
                }
            }

            STSClient client = STSUtils.getClient(m2, "sct");
            AddressingProperties maps =
                (AddressingProperties)message
                    .get("javax.xml.ws.addressing.context.inbound");
            if (maps == null) {
                maps = (AddressingProperties)m2
                    .get("javax.xml.ws.addressing.context");
            }
            
            synchronized (client) {
                try {
                    SecureConversationTokenInterceptorProvider
                        .setupClient(client, message, aim, itok, true);

                    if (maps != null) {
                        client.setAddressingNamespace(maps.getNamespaceURI());
                    }
                    
                    client.cancelSecurityToken(tok);
                    NegotiationUtils.getTokenStore(m2).remove(tok.getId());
                    m2.setContextualProperty(SecurityConstants.TOKEN, null);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new Fault(e);
                } finally {
                    client.setTrust((Trust10)null);
                    client.setTrust((Trust13)null);
                    client.setTemplate(null);
                    client.setLocation(null);
                    client.setAddressingNamespace(null);
                }
            }

        }

        
    }
    

    
}