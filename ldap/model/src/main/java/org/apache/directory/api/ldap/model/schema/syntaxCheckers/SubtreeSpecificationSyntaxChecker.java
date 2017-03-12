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
package org.apache.directory.api.ldap.model.schema.syntaxCheckers;


import java.text.ParseException;

import org.apache.directory.api.i18n.I18n;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.SyntaxChecker;
import org.apache.directory.api.ldap.model.subtree.SubtreeSpecificationChecker;
import org.apache.directory.api.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A SyntaxChecker which verifies that a value is a subtree specification.
 * <p>
 * It has been removed in RFC 4517
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@SuppressWarnings("serial")
public class SubtreeSpecificationSyntaxChecker extends SyntaxChecker
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( SubtreeSpecificationSyntaxChecker.class );

    /** The associated checker */
    private transient SubtreeSpecificationChecker subtreeSpecificationChecker;
    
    /**
     * A static instance of SubtreeSpecificationSyntaxChecker
     */
    public static final SubtreeSpecificationSyntaxChecker INSTANCE = new SubtreeSpecificationSyntaxChecker();

    
    /**
     * Creates an instance of SubtreeSpecificationSyntaxChecker
     */
    public SubtreeSpecificationSyntaxChecker()
    {
        super( SchemaConstants.SUBTREE_SPECIFICATION_SYNTAX );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidSyntax( Object value )
    {
        String strValue;

        if ( value == null )
        {
            LOG.debug( I18n.err( I18n.ERR_04489_SYNTAX_INVALID, "null" ) );
            return false;
        }

        if ( value instanceof String )
        {
            strValue = ( String ) value;
        }
        else if ( value instanceof byte[] )
        {
            strValue = Strings.utf8ToString( ( byte[] ) value );
        }
        else
        {
            strValue = value.toString();
        }

        if ( strValue.length() == 0 )
        {
            LOG.debug( I18n.err( I18n.ERR_04489_SYNTAX_INVALID, value ) );
            return false;
        }

        try
        {
            synchronized ( subtreeSpecificationChecker )
            {
                subtreeSpecificationChecker.parse( strValue );
            }

            LOG.debug( I18n.msg( I18n.MSG_04490_SYNTAX_VALID, value ) );
            return true;
        }
        catch ( ParseException pe )
        {
            LOG.debug( I18n.err( I18n.ERR_04489_SYNTAX_INVALID, value ) );
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setSchemaManager( SchemaManager schemaManager )
    {
        subtreeSpecificationChecker = new SubtreeSpecificationChecker( schemaManager );
    }
}
