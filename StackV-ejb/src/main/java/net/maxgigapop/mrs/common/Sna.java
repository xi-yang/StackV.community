/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2014

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 */

package net.maxgigapop.mrs.common;

import com.hp.hpl.jena.rdf.model.*;

/**
 *
 * @author onos-stackv
 */
public class Sna {

    private static Model m_model = ModelFactory.createDefaultModel();

    public static final String NS = "http://schemas.ogf.org/sna/2015/08/network#";

    public static String getURI() {
        return NS;
    }

    public static final Resource NAMESPACE = m_model.createResource(NS);

    public static final Property severity = m_model.createProperty("http://schemas.ogf.org/sna/2015/08/network#severity");

    public static final Property occurenceProbability = m_model.createProperty("http://schemas.ogf.org/sna/2015/08/network#occurenceProbability");

    public static final Resource SRRG = m_model.createResource("http://schemas.ogf.org/sna/2015/08/network#SRRG");

    public static final Resource protectionSwitchingService = m_model.createResource("http://schemas.ogf.org/sna/2015/08/network#ProtectionSwitchingService");

    public static final Resource protectionRoutingService = m_model.createResource("http://schemas.ogf.org/sna/2015/08/network#ProtectionRoutingService");

    public static final Resource protectionOpenflowService = m_model.createResource("http://schemas.ogf.org/sna/2015/08/network#ProtectionOpenflowService");

}
