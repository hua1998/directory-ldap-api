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
package org.apache.directory.shared.ldap.schema.syntax;


import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A SyntaxChecker which verifies that a value is a PostalAddress according to 
 * RFC 4517 :
 * 
 * <postal-address> = <dstring> <dstring-list>
 * <dstring-list> = "$" <dstring> <dstring-list> | e
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class PostalAddressSyntaxChecker extends AbstractSyntaxChecker
{
    /** The Syntax OID, according to RFC 4517 */
    private static final String SC_OID = "1.3.6.1.4.1.1466.115.121.1.41";
    
    /**
     * 
     * Creates a new instance of PostalAddressSyntaxChecker.
     *
     */
    public PostalAddressSyntaxChecker()
    {
        super( SC_OID );
    }
    
    /**
     * 
     * Creates a new instance of PostalAddressSyntaxChecker.
     * 
     * @param oid the oid to associate with this new SyntaxChecker
     *
     */
    protected PostalAddressSyntaxChecker( String oid )
    {
        super( oid );
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.SyntaxChecker#isValidSyntax(java.lang.Object)
     */
    public boolean isValidSyntax( Object value )
    {
        String strValue = null;

        if ( value == null )
        {
            return false;
        }
        
        if ( value instanceof String )
        {
            strValue = ( String ) value;
        }
        else if ( value instanceof byte[] )
        {
            strValue = StringTools.utf8ToString( ( byte[] ) value ); 
        }
        else
        {
            strValue = value.toString();
        }

        if ( strValue.length() == 0 )
        {
            return false;
        }

        // Search for the '$' separator
        int dollar = strValue.indexOf( '$' );
        
        if ( dollar == -1 )
        {
            // No '$' => only a dstring
            return true;
        }

        int pos = 0;
        do
        {
            // check that the element between each '$' is not empty
            String address = strValue.substring( pos, dollar );
            
            if ( StringTools.isEmpty( address ) )
            {
                return false;
            }
            
            pos = dollar + 1;
            
            if ( pos == strValue.length() )
            {
                // we should not have a '$' at the end
                return false;
            }
            
            dollar = strValue.indexOf( '$', pos );
        } while ( dollar > -1 );
        
        return true;
    }
}
