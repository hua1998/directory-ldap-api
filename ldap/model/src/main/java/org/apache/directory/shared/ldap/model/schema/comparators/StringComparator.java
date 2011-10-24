/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.ldap.model.schema.comparators;


import java.io.Serializable;

import org.apache.directory.shared.ldap.model.schema.LdapComparator;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A comparator for Strings.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Component
@Provides
public class StringComparator extends LdapComparator<String> implements Serializable
{
    /**
     * Property to specify factory type.
     * TODO:This is temporary. Will be vanished after introducing custom annotations
     */
    @Property(name = "ads.comp.type", value = "comparator")
    public String compType;

    /** The serial version UID */
    private static final long serialVersionUID = 2L;

    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( StringComparator.class );


    /**
     * The StringComparator constructor. Its OID is the StringMatch matching
     * rule OID.
     */
    public StringComparator( @Property(name = "ads.comp.comparator.oid") String oid )
    {
        super( oid );
    }


    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "ES_COMPARING_PARAMETER_STRING_WITH_EQ",
        justification = "false positive")
    public int compare( String s1, String s2 )
    {
        LOG.debug( "comparing String objects '{}' with '{}'", s1, s2 );

        if ( s1 == s2 )
        {
            return 0;
        }

        // -------------------------------------------------------------------
        // Handle some basis cases
        // -------------------------------------------------------------------
        if ( s1 == null )
        {
            return ( s2 == null ) ? 0 : -1;
        }

        if ( s2 == null )
        {
            return 1;
        }

        return s1.compareTo( s2 );
    }
}
