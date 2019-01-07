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
package org.apache.directory.api.ldap.codec;


import org.apache.directory.api.i18n.I18n;
import org.apache.directory.api.ldap.codec.api.LdapCodecConstants;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.util.Strings;


/**
 * A class to store an attribute value assertion. 
 * The grammar is :
 * 
 * AttributeValueAssertion ::= SEQUENCE {
 *           attributeDesc   AttributeDescription,
 *           assertionValue  AssertionValue }
 *
 * AttributeDescription ::= LDAPString
 * 
 * AssertionValue ::= OCTET STRING
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AttributeValueAssertion
{
    /** The attribute description */
    private String attributeDesc;

    /** The assertion as we received it */
    private byte[] assertion;


    /**
     * Helper method to render an object which can be a String or a byte[]
     *
     * @param object the Object to render
     * @return A string representing the object
     */
    public static String dumpObject( Object object )
    {
        if ( object != null )
        {
            if ( object instanceof String )
            {
                return ( String ) object;
            }
            else if ( object instanceof byte[] )
            {
                return Strings.dumpBytes( ( byte[] ) object );
            }
            else if ( object instanceof Value )
            {
                return ( ( Value ) object ).getString();
            }
            else
            {
                return "<unknown type>";
            }
        }
        else
        {
            return "";
        }
    }


    /**
     * Get the attribute description
     * 
     * @return Returns the attributeDesc.
     */
    public String getAttributeDesc()
    {
        return attributeDesc;
    }


    /**
     * Set the attribute description
     * 
     * @param attributeDesc The attributeDesc to set.
     */
    public void setAttributeDesc( String attributeDesc )
    {
        this.attributeDesc = attributeDesc;
    }


    /**
     * Get a String representation of an AttributeValueAssertion
     * 
     * @param tabs The spacing to be put before the string
     * @return An AttributeValueAssertion String
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "AttributeValueAssertion\n" );
        sb.append( tabs ).append( "    Assertion description : '" );
        sb.append( attributeDesc != null ? attributeDesc : "null" );
        sb.append( "'\n" );
        sb.append( tabs ).append( "    Assertion value : '" ).append( dumpObject( assertion ) ).append( "'\n" );

        return sb.toString();
    }


    /**
     * Get a String representation of an AttributeValueAssertion, as of RFC
     * 2254.
     * 
     * @param filterType The filter type
     * @return An AttributeValueAssertion String
     */
    public String toStringRFC2254( int filterType )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( attributeDesc );

        switch ( filterType )
        {
            case LdapCodecConstants.EQUALITY_MATCH_FILTER:
                sb.append( '=' );
                break;

            case LdapCodecConstants.LESS_OR_EQUAL_FILTER:
                sb.append( "<=" );
                break;

            case LdapCodecConstants.GREATER_OR_EQUAL_FILTER:
                sb.append( ">=" );
                break;

            case LdapCodecConstants.APPROX_MATCH_FILTER:
                sb.append( "~=" );
                break;

            default:
                throw new IllegalArgumentException( I18n.err( I18n.ERR_05503_UNEXPECTED_FILTER_TYPE, filterType ) );
        }

        sb.append( dumpObject( assertion ) );

        return sb.toString();
    }


    /**
     * @return the assertion
     */
    public byte[] getAssertion()
    {
        return assertion;
    }


    /**
     * @param assertion the assertion to set
     */
    public void setAssertion( byte[] assertion )
    {
        if ( assertion != null )
        {
            this.assertion = new byte[assertion.length];
            System.arraycopy( assertion, 0, this.assertion, 0, assertion.length );
        }
    }


    /**
     * Get a String representation of an AttributeValueAssertion
     * 
     * @return An AttributeValueAssertion String
     */
    @Override
    public String toString()
    {
        return toString( "" );
    }
}
