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


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.directory.api.i18n.I18n;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.schema.SyntaxChecker;
import org.apache.directory.api.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A SyntaxChecker which verifies that a value is a TelephoneNumber according to ITU
 * recommendation E.123 (which is quite vague ...).
 * <p>
 * A valid Telephone number respects more or less this syntax :
 * 
 * <pre>
 * " *[+]? *((\([0-9- ,;/#*]+\))|[0-9- ,;/#*]+)+"
 * </pre>
 * 
 * If needed, and to allow more syntaxes, a list of regexps has been added
 * which can be initialized to other values
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@SuppressWarnings("serial")
public class TelephoneNumberSyntaxChecker extends SyntaxChecker
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( TelephoneNumberSyntaxChecker.class );

    /** Other regexps to extend the initial one */
    private List<String> regexps;

    /** Other regexp to extend the initial one, compiled */
    private List<Pattern> compiledREs;

    /** The default pattern used to check a TelephoneNumber */
    private static final String DEFAULT_REGEXP = "^ *[+]? *((\\([0-9- ,;/#*]+\\))|[0-9- ,;/#*]+)+$";

    /** The compiled default pattern */
    private Pattern defaultPattern = Pattern.compile( DEFAULT_REGEXP );

    /** A flag set when only the default regexp should be tested */
    protected boolean defaultMandatory = false;
    
    /**
     * A static instance of TelephoneNumberSyntaxChecker
     */
    public static final TelephoneNumberSyntaxChecker INSTANCE = new TelephoneNumberSyntaxChecker();

    
    /**
     * Creates a new instance of TelephoneNumberSyntaxChecker.
     */
    public TelephoneNumberSyntaxChecker()
    {
        super( SchemaConstants.TELEPHONE_NUMBER_SYNTAX );
    }

    
    /**
     * Creates a new instance of a child of this class, with an OID.
     * 
     * @param oid the child's OID
     */
    protected TelephoneNumberSyntaxChecker( String oid )
    {
        super( oid );
    }


    /**
     * Add a new valid regexp for a Telephone number
     * @param regexp The new regexp to check
     */
    public void addRegexp( String regexp )
    {
        if ( defaultMandatory )
        {
            return;
        }

        try
        {
            Pattern compiledRE = Pattern.compile( regexp );

            if ( regexps == null )
            {
                regexps = new ArrayList<>();
                compiledREs = new ArrayList<>();
            }

            regexps.add( regexp );
            compiledREs.add( compiledRE );
        }
        catch ( PatternSyntaxException pse )
        {
            return;
        }
    }


    /**
     * Set the defaut regular expression for the Telephone number
     * 
     * @param regexp the default regular expression.
     */
    public void setDefaultRegexp( String regexp )
    {
        try
        {
            defaultPattern = Pattern.compile( regexp );

            defaultMandatory = true;
            regexps = null;
            compiledREs = null;
        }
        catch ( PatternSyntaxException pse )
        {
            return;
        }
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

        // We will use a regexp to check the TelephoneNumber.
        if ( defaultMandatory )
        {
            // We have a unique regexp to check, the default one
            // We have to do that in a protected section
            boolean result;
            
            synchronized ( defaultPattern )
            {
                result = defaultPattern.matcher( strValue ).matches();
            }

            if ( result )
            {
                LOG.debug( I18n.msg( I18n.MSG_04490_SYNTAX_VALID, value ) );
            }
            else
            {
                LOG.debug( I18n.err( I18n.ERR_04489_SYNTAX_INVALID, value ) );
            }

            return result;
        }
        else
        {
            boolean result;
            
            synchronized ( defaultPattern )
            {
                result = defaultPattern.matcher( strValue ).matches();
            }
            
            if ( result )
            {
                LOG.debug( I18n.msg( I18n.MSG_04490_SYNTAX_VALID, value ) );
                return true;
            }
            else
            {
                if ( compiledREs == null )
                {
                    LOG.debug( I18n.err( I18n.ERR_04489_SYNTAX_INVALID, value ) );
                    return false;
                }

                synchronized ( compiledREs )
                {
                    // The default is not enough, let's try
                    // the other regexps
                    for ( Pattern pattern : compiledREs )
                    {
                        if ( pattern.matcher( strValue ).matches() )
                        {
                            LOG.debug( I18n.msg( I18n.MSG_04490_SYNTAX_VALID, value ) );
                            return true;
                        }
                    }
                }

                LOG.debug( I18n.err( I18n.ERR_04489_SYNTAX_INVALID, value ) );
                return false;
            }
        }
    }
}
