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
package org.apache.directory.shared.ldap.name;


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.model.entry.StringValue;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.normalizers.OidNormalizer;
import org.apache.directory.shared.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class store the name-component part or the following BNF grammar (as of
 * RFC2253, par. 3, and RFC1779, fig. 1) : <br> - &lt;name-component&gt; ::=
 * &lt;attributeType&gt; &lt;spaces&gt; '=' &lt;spaces&gt;
 * &lt;attributeValue&gt; &lt;attributeTypeAndValues&gt; <br> -
 * &lt;attributeTypeAndValues&gt; ::= &lt;spaces&gt; '+' &lt;spaces&gt;
 * &lt;attributeType&gt; &lt;spaces&gt; '=' &lt;spaces&gt;
 * &lt;attributeValue&gt; &lt;attributeTypeAndValues&gt; | e <br> -
 * &lt;attributeType&gt; ::= [a-zA-Z] &lt;keychars&gt; | &lt;oidPrefix&gt; [0-9]
 * &lt;digits&gt; &lt;oids&gt; | [0-9] &lt;digits&gt; &lt;oids&gt; <br> -
 * &lt;keychars&gt; ::= [a-zA-Z] &lt;keychars&gt; | [0-9] &lt;keychars&gt; | '-'
 * &lt;keychars&gt; | e <br> - &lt;oidPrefix&gt; ::= 'OID.' | 'oid.' | e <br> -
 * &lt;oids&gt; ::= '.' [0-9] &lt;digits&gt; &lt;oids&gt; | e <br> -
 * &lt;attributeValue&gt; ::= &lt;pairs-or-strings&gt; | '#' &lt;hexstring&gt;
 * |'"' &lt;quotechar-or-pairs&gt; '"' <br> - &lt;pairs-or-strings&gt; ::= '\'
 * &lt;pairchar&gt; &lt;pairs-or-strings&gt; | &lt;stringchar&gt;
 * &lt;pairs-or-strings&gt; | e <br> - &lt;quotechar-or-pairs&gt; ::=
 * &lt;quotechar&gt; &lt;quotechar-or-pairs&gt; | '\' &lt;pairchar&gt;
 * &lt;quotechar-or-pairs&gt; | e <br> - &lt;pairchar&gt; ::= ',' | '=' | '+' |
 * '&lt;' | '&gt;' | '#' | ';' | '\' | '"' | [0-9a-fA-F] [0-9a-fA-F] <br> -
 * &lt;hexstring&gt; ::= [0-9a-fA-F] [0-9a-fA-F] &lt;hexpairs&gt; <br> -
 * &lt;hexpairs&gt; ::= [0-9a-fA-F] [0-9a-fA-F] &lt;hexpairs&gt; | e <br> -
 * &lt;digits&gt; ::= [0-9] &lt;digits&gt; | e <br> - &lt;stringchar&gt; ::=
 * [0x00-0xFF] - [,=+&lt;&gt;#;\"\n\r] <br> - &lt;quotechar&gt; ::= [0x00-0xFF] -
 * [\"] <br> - &lt;separator&gt; ::= ',' | ';' <br> - &lt;spaces&gt; ::= ' '
 * &lt;spaces&gt; | e <br>
 * <br>
 * A Rdn is a part of a Dn. It can be composed of many types, as in the Rdn
 * following Rdn :<br>
 * ou=value + cn=other value<br>
 * <br>
 * or <br>
 * ou=value + ou=another value<br>
 * <br>
 * In this case, we have to store an 'ou' and a 'cn' in the Rdn.<br>
 * <br>
 * The types are case insensitive. <br>
 * Spaces before and after types and values are not stored.<br>
 * Spaces before and after '+' are not stored.<br>
 * <br>
 * Thus, we can consider that the following RDNs are equals :<br>
 * <br>
 * 'ou=test 1'<br> ' ou=test 1'<br>
 * 'ou =test 1'<br>
 * 'ou= test 1'<br>
 * 'ou=test 1 '<br> ' ou = test 1 '<br>
 * <br>
 * So are the following :<br>
 * <br>
 * 'ou=test 1+cn=test 2'<br>
 * 'ou = test 1 + cn = test 2'<br> ' ou =test 1+ cn =test 2 ' <br>
 * 'cn = test 2 +ou = test 1'<br>
 * <br>
 * but the following are not equal :<br>
 * 'ou=test 1' <br>
 * 'ou=test 1'<br>
 * because we have more than one spaces inside the value.<br>
 * <br>
 * The Rdn is composed of one or more AttributeTypeAndValue (atav) Those atavs
 * are ordered in the alphabetical natural order : a < b < c ... < z As the type
 * are not case sensitive, we can say that a = A
 * <br>
 * This class is immutable.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Rdn implements Cloneable, Comparable<Rdn>, Externalizable, Iterable<Ava>
{
    /** The LoggerFactory used by this class */
    protected static final Logger LOG = LoggerFactory.getLogger( Rdn.class );

    /** An empty Rdn */
    public static final Rdn EMPTY_RDN = new Rdn();

    /**
    * Declares the Serial Version Uid.
    *
    * @see <a
    *      href="http://c2.com/cgi/wiki?AlwaysDeclareSerialVersionUid">Always
    *      Declare Serial Version Uid</a>
    */
    private static final long serialVersionUID = 1L;

    /** The User Provided Rdn */
    private String upName = null;

    /** The normalized Rdn */
    private String normName = null;

    /** The starting position of this Rdn in the given string from which
     * we have extracted the upName */
    private int start;

    /** The length of this Rdn upName */
    private int length;

    /**
     * Stores all couple type = value. We may have more than one type, if the
     * '+' character appears in the AttributeTypeAndValue. This is a TreeSet,
     * because we want the ATAVs to be sorted. An atav may contain more than one
     * value. In this case, the values are String stored in a List.
     */
    private Set<Ava> atavs = null;

    /**
     * We also keep a set of types, in order to use manipulations. A type is
     * connected with the atav it represents.
     *
     * Note : there is no Generic available classes in commons-collection...
     */
    private MultiMap atavTypes = new MultiValueMap();

    /**
     * We keep the type for a single valued Rdn, to avoid the creation of an HashMap
     */
    private String atavType = null;

    /**
     * A simple AttributeTypeAndValue is used to store the Rdn for the simple
     * case where we only have a single type=value. This will be 99.99% the
     * case. This avoids the creation of a HashMap.
     */
    protected Ava atav = null;

    /**
     * The number of atavs. We store this number here to avoid complex
     * manipulation of atav and atavs
     */
    private int nbAtavs = 0;

    /** CompareTo() results */
    public static final int UNDEFINED = Integer.MAX_VALUE;

    /** Constant used in comparisons */
    public static final int SUPERIOR = 1;

    /** Constant used in comparisons */
    public static final int INFERIOR = -1;

    /** Constant used in comparisons */
    public static final int EQUAL = 0;

    /** A flag used to tell if the Rdn has been normalized */
    private AtomicBoolean normalized = new AtomicBoolean();

    /** the schema manager */
    private transient SchemaManager schemaManager;


    /**
     * A empty constructor.
     */
    public Rdn()
    {
        this( ( SchemaManager ) null );
    }


    /**
     *
     * Creates a new instance of Rdn.
     *
     * @param schemaManager the schema manager
     */
    public Rdn(SchemaManager schemaManager)
    {
        // Don't waste space... This is not so often we have multiple
        // name-components in a Rdn... So we won't initialize the Map and the
        // treeSet.
        this.schemaManager = schemaManager;
        upName = "";
        normName = "";
        normalized.set( false );
    }


    /**
     * A constructor that parse a String representing a Rdn.
     *
     * @param rdn the String containing the Rdn to parse
     * @param schemaManager the schema manager
     * @throws LdapInvalidDnException if the Rdn is invalid
     */
    public Rdn(String rdn, SchemaManager schemaManager) throws LdapInvalidDnException
    {
        start = 0;

        if ( Strings.isNotEmpty(rdn) )
        {
            // Parse the string. The Rdn will be updated.
            RdnParser.parse( rdn, this );

            // create the internal normalized form
            // and store the user provided form
            if ( schemaManager != null )
            {
                this.schemaManager = schemaManager;
                normalize( schemaManager.getNormalizerMapping() );
                normalized.set( true );
            }
            else
            {
                normalize();
                normalized.set( false );
            }

            upName = rdn;
            length = rdn.length();
        }
        else
        {
            upName = "";
            normName = "";
            length = 0;
            normalized.set( false );
        }
    }


    /**
     * A constructor that parse a String representing a Rdn.
     *
     * @param rdn the String containing the Rdn to parse
     * @throws LdapInvalidDnException if the Rdn is invalid
     */
    public Rdn(String rdn) throws LdapInvalidDnException
    {
        this( rdn, ( SchemaManager ) null );
    }


    /**
     * A constructor that constructs a Rdn from a type and a value. Constructs
     * an Rdn from the given attribute type and value. The string attribute
     * values are not interpreted as RFC 4514 formatted Rdn strings. That is,
     * the values are used literally (not parsed) and assumed to be un-escaped.
     *
     * @param upType the user provided type of the Rdn
     * @param upValue the user provided value of the Rdn
     * @param normType the normalized provided type of the Rdn
     * @param normValue the normalized provided value of the Rdn
     * @param schemaManager the schema manager
     * @throws LdapInvalidDnException if the Rdn is invalid
     */
    public Rdn(String upType, String normType, String upValue, String normValue, SchemaManager schemaManager) throws LdapInvalidDnException
    {
        this.schemaManager = schemaManager;

        addAVA( upType, normType, new StringValue( upValue ), new StringValue( normValue ) );

        upName = upType + '=' + upValue;
        start = 0;
        length = upName.length();

        // create the internal normalized form
        normalize();

        if( schemaManager != null )
        {
            normalized.set( true );
        }
        else
        {
            // As strange as it seems, the Rdn is *not* normalized against the schema at this point
            normalized.set( false );
        }
    }


    /**
     * A constructor that constructs a Rdn from a type and a value.
     *
     * @param upType the user provided type of the Rdn
     * @param upValue the user provided value of the Rdn
     * @param normType the normalized provided type of the Rdn
     * @param normValue the normalized provided value of the Rdn
     * @throws LdapInvalidDnException if the Rdn is invalid
     * @see #Rdn(String, String, String, String, SchemaManager)
     */
    public Rdn(String upType, String normType, String upValue, String normValue) throws LdapInvalidDnException
    {
        this( upType, normType, upValue, normValue, null );
    }


    /**
     * A constructor that constructs a Rdn from a type and a value. Constructs
     * an Rdn from the given attribute type and value. The string attribute
     * values are not interpreted as RFC 414 formatted Rdn strings. That is,
     * the values are used literally (not parsed) and assumed to be un-escaped.
     *
     * @param upType the user provided type of the Rdn
     * @param upValue the user provided value of the Rdn
     * @param schemaManager the schema manager
     * @throws LdapInvalidDnException if the Rdn is invalid
     */
    public Rdn(String upType, String upValue, SchemaManager schemaManager) throws LdapInvalidDnException
    {
        addAVA( upType, upType, new StringValue( upValue ), new StringValue( upValue ) );

        upName = upType + '=' + upValue;
        start = 0;
        length = upName.length();

        if( schemaManager != null )
        {
            this.schemaManager = schemaManager;
            normalize( schemaManager.getNormalizerMapping() );
            normalized.set( true );
        }
        else
        {
            // create the internal normalized form
            normalize();

            // As strange as it seems, the Rdn is *not* normalized against the schema at this point
            normalized.set( false );
        }
    }


    /**
     * A constructor that constructs a Rdn from a type and a value.
     *
     * @param upType the user provided type of the Rdn
     * @param upValue the user provided value of the Rdn
     * @throws LdapInvalidDnException if the Rdn is invalid
     * @see #Rdn(String, String, SchemaManager)
     */
    public Rdn(String upType, String upValue) throws LdapInvalidDnException
    {
        this( upType, upValue, null );
    }


    /**
     * A constructor that constructs a Rdn from a type, a position and a length.
     *
     * @param start the starting point for this Rdn in the user provided Dn
     * @param length the Rdn's length
     * @param upName the user provided name
     * @param normName the normalized name
     */
    Rdn(int start, int length, String upName, String normName)
    {
        this.start = start;
        this.length = length;
        this.upName = upName;
        this.normName = normName;
        normalized.set( true );
    }


    /**
     * Constructs an Rdn from the given rdn. The contents of the rdn are simply
     * copied into the newly created
     *
     * @param rdn The non-null Rdn to be copied.
     */
    public Rdn(Rdn rdn)
    {
        nbAtavs = rdn.getNbAtavs();
        this.normName = rdn.normName;
        this.upName = rdn.getName();
        this.start = rdn.start;
        this.length = rdn.length;
        normalized.set(rdn.normalized.get());

        switch ( rdn.getNbAtavs() )
        {
            case 0:
                return;

            case 1:
                this.atav = (Ava) rdn.atav.clone();
                return;

            default:
                // We must duplicate the treeSet and the hashMap
                atavs = new TreeSet<Ava>();
                atavTypes = new MultiValueMap();

                for ( Ava currentAtav : rdn.atavs )
                {
                    atavs.add( (Ava) currentAtav.clone() );
                    atavTypes.put( currentAtav.getNormType(), currentAtav );
                }

                return;
        }
    }


    /**
     * Transform the external representation of the current Rdn to an internal
     * normalized form where :
     * - types are trimmed and lower cased
     * - values are trimmed and lower cased
     */
    // WARNING : The protection level is left unspecified on purpose.
    // We need this method to be visible from the DnParser class, but not
    // from outside this package.
    /* Unspecified protection */void normalize()
    {
        switch ( nbAtavs )
        {
            case 0:
                // An empty Rdn
                normName = "";
                break;

            case 1:
                // We have a single AttributeTypeAndValue
                // We will trim and lowercase type and value.
                if ( !atav.getNormValue().isBinary() )
                {
                    normName = atav.getNormName();
                }
                else
                {
                    normName = atav.getNormType() + "=#" + Strings.dumpHexPairs( atav.getNormValue().getBytes() );
                }

                break;

            default:
                // We have more than one AttributeTypeAndValue
                StringBuffer sb = new StringBuffer();

                boolean isFirst = true;

                for ( Ava ata : atavs )
                {
                    if ( isFirst )
                    {
                        isFirst = false;
                    }
                    else
                    {
                        sb.append( '+' );
                    }

                    sb.append( ata.normalize() );
                }

                normName = sb.toString();
                break;
        }
    }


    /**
     * Transform a Rdn by changing the value to its OID counterpart and
     * normalizing the value accordingly to its type.
     *
     * @param sm the SchemaManager
     * @return this Rdn, normalized
     * @throws org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException if the Rdn is invalid
     */
    public Rdn normalize( SchemaManager sm ) throws LdapInvalidDnException
    {
        return normalize( sm.getNormalizerMapping() );
    }


    /**
     * Transform a Rdn by changing the value to its OID counterpart and
     * normalizing the value accordingly to its type.
     *
     * @param oidsMap the mapping between names and OIDs
     * @return this Rdn, normalized
     * @throws LdapInvalidDnException if the Rdn is invalid
     */
    public Rdn normalize( Map<String, OidNormalizer> oidsMap ) throws LdapInvalidDnException
    {
        if ( ( oidsMap == null ) || ( oidsMap.isEmpty() ) )
        {
            return this;
        }

        if ( normalized.get() )
        {
            return this;
        }

        synchronized ( this )
        {
            String savedUpName = getName();
            Dn.rdnOidToName(this, oidsMap);
            normalize();
            this.upName = savedUpName;
            normalized.set( true );
    
            return this;
        }
    }


    /**
     * Add a AttributeTypeAndValue to the current Rdn
     *
     * @param upType The user provided type of the added Rdn.
     * @param type The normalized provided type of the added Rdn.
     * @param upValue The user provided value of the added Rdn
     * @param value The normalized provided value of the added Rdn
     * @throws LdapInvalidDnException
     *             If the Rdn is invalid
     */
    // WARNING : The protection level is left unspecified intentionally.
    // We need this method to be visible from the DnParser class, but not
    // from outside this package.
    /* Unspecified protection */void addAVA( String upType, String type, Value<?> upValue,
        Value<?> value ) throws LdapInvalidDnException
    {
        // First, let's normalize the type
        Value<?> normalizedValue = value;
        String normalizedType = Strings.lowerCaseAscii(type);

        if( schemaManager != null )
        {
            OidNormalizer oidNormalizer = schemaManager.getNormalizerMapping().get( normalizedType );
            normalizedType = oidNormalizer.getAttributeTypeOid();
            try
            {
                normalizedValue = oidNormalizer.getNormalizer().normalize( value );
            }
            catch( LdapException e )
            {
                throw new LdapInvalidDnException( e.getMessage() );
            }
        }

        switch ( nbAtavs )
        {
            case 0:
                // This is the first AttributeTypeAndValue. Just stores it.
                atav = new Ava( upType, normalizedType, upValue, normalizedValue );
                nbAtavs = 1;
                atavType = normalizedType;
                return;

            case 1:
                // We already have an atav. We have to put it in the HashMap
                // before adding a new one.
                // First, create the HashMap,
                atavs = new TreeSet<Ava>();

                // and store the existing AttributeTypeAndValue into it.
                atavs.add( atav );
                atavTypes = new MultiValueMap();
                atavTypes.put( atavType, atav );

                atav = null;

                // Now, fall down to the commmon case
                // NO BREAK !!!

            default:
                // add a new AttributeTypeAndValue
                Ava newAtav = new Ava( upType, normalizedType, upValue, normalizedValue );
                atavs.add( newAtav );
                atavTypes.put( normalizedType, newAtav );

                nbAtavs++;
                break;

        }
    }


    /**
     * Add a AttributeTypeAndValue to the current Rdn
     *
     * @param value The added AttributeTypeAndValue
     */
    // WARNING : The protection level is left unspecified intentionally.
    // We need this method to be visible from the DnParser class, but not
    // from outside this package.
    /* Unspecified protection */void addAVA( Ava value )
    {
        String normalizedType = value.getNormType();

        switch ( nbAtavs )
        {
            case 0:
                // This is the first AttributeTypeAndValue. Just stores it.
                atav = value;
                nbAtavs = 1;
                atavType = normalizedType;
                return;

            case 1:
                // We already have an atav. We have to put it in the HashMap
                // before adding a new one.
                // First, create the HashMap,
                atavs = new TreeSet<Ava>();

                // and store the existing AttributeTypeAndValue into it.
                atavs.add( atav );
                atavTypes = new MultiValueMap();
                atavTypes.put( atavType, atav );

                this.atav = null;

                // Now, fall down to the commmon case
                // NO BREAK !!!

            default:
                // add a new AttributeTypeAndValue
                atavs.add( value );
                atavTypes.put( normalizedType, value );

                nbAtavs++;
                break;

        }
    }


    /**
     * Clear the Rdn, removing all the AttributeTypeAndValues.
     */
    // WARNING : The protection level is left unspecified intentionally.
    // We need this method to be visible from the DnParser class, but not
    // from outside this package.
    /* No protection */void clear()
    {
        atav = null;
        atavs = null;
        atavType = null;
        atavTypes.clear();
        nbAtavs = 0;
        normName = "";
        upName = "";
        start = -1;
        length = 0;
        normalized.set( false );
    }


    /**
     * Get the Value of the AttributeTypeAndValue which type is given as an
     * argument.
     *
     * @param type the type of the NameArgument
     * @return the Value to be returned, or null if none found.
     * @throws LdapInvalidDnException if the Rdn is invalid
     */
    public Object getValue( String type ) throws LdapInvalidDnException
    {
        // First, let's normalize the type
        String normalizedType = Strings.lowerCaseAscii(Strings.trim(type));

        switch ( nbAtavs )
        {
            case 0:
                return "";

            case 1:
                if ( Strings.equals(atav.getNormType(), normalizedType) )
                {
                    return atav.getNormValue().get();
                }

                return "";

            default:
                if ( atavTypes.containsKey( normalizedType ) )
                {
                    Collection<Ava> atavList = ( Collection<Ava> ) atavTypes.get( normalizedType );
                    StringBuffer sb = new StringBuffer();
                    boolean isFirst = true;

                    for ( Ava elem : atavList )
                    {
                        if ( isFirst )
                        {
                            isFirst = false;
                        }
                        else
                        {
                            sb.append( ',' );
                        }

                        sb.append( elem.getNormValue() );
                    }

                    return sb.toString();
                }

                return "";
        }
    }


    /**
     * Get the start position
     *
     * @return The start position in the Dn
     */
    public int getStart()
    {
        return start;
    }


    /**
     * Get the Rdn length
     *
     * @return the Rdn length
     */
    public int getLength()
    {
        return length;
    }


    /**
     * Get the AttributeTypeAndValue which type is given as an argument. If we
     * have more than one value associated with the type, we will return only
     * the first one.
     *
     * @param type
     *            The type of the NameArgument to be returned
     * @return The AttributeTypeAndValue, of null if none is found.
     */
    public Ava getAttributeTypeAndValue( String type )
    {
        // First, let's normalize the type
        String normalizedType = Strings.lowerCaseAscii(Strings.trim(type));

        switch ( nbAtavs )
        {
            case 0:
                return null;

            case 1:
                if ( atav.getNormType().equals( normalizedType ) )
                {
                    return atav;
                }

                return null;

            default:
                if ( atavTypes.containsKey( normalizedType ) )
                {
                    Collection<Ava> atavList = ( Collection<Ava> ) atavTypes.get( normalizedType );
                    return atavList.iterator().next();
                }

                return null;
        }
    }


    /**
     * Retrieves the components of this Rdn as an iterator of AttributeTypeAndValue.
     * The effect on the iterator of updates to this Rdn is undefined. If the
     * Rdn has zero components, an empty (non-null) iterator is returned.
     *
     * @return an iterator of the components of this Rdn, each an AttributeTypeAndValue
     */
    public Iterator<Ava> iterator()
    {
        if ( nbAtavs == 1 || nbAtavs == 0 )
        {
            return new Iterator<Ava>()
            {
                private boolean hasMoreElement = nbAtavs == 1;


                public boolean hasNext()
                {
                    return hasMoreElement;
                }


                public Ava next()
                {
                    Ava obj = atav;
                    hasMoreElement = false;
                    return obj;
                }


                public void remove()
                {
                    // nothing to do
                }
            };
        }
        else
        {
            return atavs.iterator();
        }
    }


    /**
     * Clone the Rdn
     *
     * @return A clone of the current Rdn
     */
    public Rdn clone()
    {
        try
        {
            Rdn rdn = (Rdn) super.clone();
            rdn.normalized = new AtomicBoolean( normalized.get() );

            // The AttributeTypeAndValue is immutable. We won't clone it

            switch ( rdn.getNbAtavs() )
            {
                case 0:
                    break;

                case 1:
                    rdn.atav = (Ava) this.atav.clone();
                    rdn.atavTypes = atavTypes;
                    break;

                default:
                    // We must duplicate the treeSet and the hashMap
                    rdn.atavTypes = new MultiValueMap();
                    rdn.atavs = new TreeSet<Ava>();

                    for ( Ava currentAtav : this.atavs )
                    {
                        rdn.atavs.add( (Ava) currentAtav.clone() );
                        rdn.atavTypes.put( currentAtav.getNormType(), currentAtav );
                    }

                    break;
            }

            return rdn;
        }
        catch ( CloneNotSupportedException cnse )
        {
            throw new Error( "Assertion failure" );
        }
    }


    /**
     * Compares two RDNs. They are equals if :
     * <li>their have the same number of NC (AttributeTypeAndValue)
     * <li>each ATAVs are equals
     * <li>comparison of type are done case insensitive
     * <li>each value is equal, case sensitive
     * <li>Order of ATAV is not important If the RDNs are not equals, a positive number is
     * returned if the first Rdn is greater, negative otherwise
     *
     * @param rdn the Rdn to be compared
     * @return 0 if both RDNs are equals. -1 if the current Rdn is inferior, 1 if
     *         the current Rdn is superior, UNDEFINED otherwise.
     */
    public int compareTo( Rdn rdn )
    {
        if ( rdn == null )
        {
            return SUPERIOR;
        }

        if ( rdn.nbAtavs != nbAtavs )
        {
            // We don't have the same number of ATAVs. The Rdn which
            // has the higher number of Atav is the one which is
            // superior
            return nbAtavs - rdn.nbAtavs;
        }

        switch ( nbAtavs )
        {
            case 0:
                return EQUAL;

            case 1:
                return atav.compareTo( rdn.atav );

            default:
                // We have more than one value. We will
                // go through all of them.

                // the types are already normalized and sorted in the atavs TreeSet
                // so we could compare the 1st with the 1st, then the 2nd with the 2nd, etc.
                Iterator<Ava> localIterator = atavs.iterator();
                Iterator<Ava> paramIterator = rdn.atavs.iterator();

                while ( localIterator.hasNext() || paramIterator.hasNext() )
                {
                    if ( !localIterator.hasNext() )
                    {
                        return SUPERIOR;
                    }
                    if ( !paramIterator.hasNext() )
                    {
                        return INFERIOR;
                    }

                    Ava localAtav = localIterator.next();
                    Ava paramAtav = paramIterator.next();
                    int result = localAtav.compareTo( paramAtav );
                    if ( result != EQUAL )
                    {
                        return result;
                    }
                }

                return EQUAL;
        }
    }


    /**
     * @return the user provided name
     */
    public String getName()
    {
        return upName;
    }


    /**
     * @return The normalized name
     */
    public String getNormName()
    {
        return normName == null ? "" : normName;
    }


    /**
     * Set the User Provided Name.
     *
     * Package private because Rdn is immutable, only used by the Dn parser.
     *
     * @param upName the User Provided dame
     */
    void setUpName( String upName )
    {
        this.upName = upName;
    }


    /**
     * @return Returns the nbAtavs.
     */
    public int getNbAtavs()
    {
        return nbAtavs;
    }


    /**
     * Return the unique AttributeTypeAndValue, or the first one of we have more
     * than one
     *
     * @return The first AttributeTypeAndValue of this Rdn
     */
    public Ava getAVA()
    {
        switch ( nbAtavs )
        {
            case 0:
                return null;

            case 1:
                return atav;

            default:
                return ( ( TreeSet<Ava> ) atavs ).first().clone();
        }
    }


    /**
     * Return the user provided type, or the first one of we have more than one (the lowest)
     *
     * @return The first user provided type of this Rdn
     */
    public String getUpType()
    {
        switch ( nbAtavs )
        {
            case 0:
                return null;

            case 1:
                return atav.getUpType();

            default:
                return ( ( TreeSet<Ava> ) atavs ).first().getUpType();
        }
    }


    /**
     * Return the normalized type, or the first one of we have more than one (the lowest)
     *
     * @return The first normalized type of this Rdn
     */
    public String getNormType()
    {
        switch ( nbAtavs )
        {
            case 0:
                return null;

            case 1:
                return atav.getNormType();

            default:
                return ( ( TreeSet<Ava> ) atavs ).first().getNormType();
        }
    }


    /**
     * Return the User Provided value
     *
     * @return The first User provided value of this Rdn
     */
    public Value<?> getUpValue()
    {
        switch ( nbAtavs )
        {
            case 0:
                return null;

            case 1:
                return atav.getUpValue();

            default:
                return ( ( TreeSet<Ava> ) atavs ).first().getUpValue();
        }
    }


    /**
     * Return the normalized value, or the first one of we have more than one (the lowest)
     *
     * @return The first normalized value of this Rdn
     */
    public Value<?> getNormValue()
    {
        switch ( nbAtavs )
        {
            case 0:
                return null;

            case 1:
                return atav.getNormValue();

            default:
                return ( ( TreeSet<Ava> ) atavs ).first().getNormValue();
        }
    }


    /**
     * Compares the specified Object with this Rdn for equality. Returns true if
     * the given object is also a Rdn and the two Rdns represent the same
     * attribute type and value mappings. The order of components in
     * multi-valued Rdns is not significant.
     *
     * @param rdn
     *            Rdn to be compared for equality with this Rdn
     * @return true if the specified object is equal to this Rdn
     */
    public boolean equals( Object rdn )
    {
        if ( this == rdn )
        {
            return true;
        }

        if ( !( rdn instanceof Rdn) )
        {
            return false;
        }

        return compareTo( (Rdn) rdn ) == EQUAL;
    }


    /**
     * Get the number of Attribute type and value of this Rdn
     *
     * @return The number of ATAVs in this Rdn
     */
    public int size()
    {
        return nbAtavs;
    }


    /**
     * Unescape the given string according to RFC 2253 If in <string> form, a
     * LDAP string representation asserted value can be obtained by replacing
     * (left-to-right, non-recursively) each <pair> appearing in the <string> as
     * follows: replace <ESC><ESC> with <ESC>; replace <ESC><special> with
     * <special>; replace <ESC><hexpair> with the octet indicated by the
     * <hexpair> If in <hexstring> form, a BER representation can be obtained
     * from converting each <hexpair> of the <hexstring> to the octet indicated
     * by the <hexpair>
     *
     * @param value
     *            The value to be unescaped
     * @return Returns a string value as a String, and a binary value as a byte
     *         array.
     * @throws IllegalArgumentException
     *             When an Illegal value is provided.
     */
    public static Object unescapeValue( String value ) throws IllegalArgumentException
    {
        if ( Strings.isEmpty(value) )
        {
            return "";
        }

        char[] chars = value.toCharArray();

        if ( chars[0] == '#' )
        {
            if ( chars.length == 1 )
            {
                // The value is only containing a #
                return StringConstants.EMPTY_BYTES;
            }

            if ( ( chars.length % 2 ) != 1 )
            {
                throw new IllegalArgumentException( I18n.err( I18n.ERR_04213 ) );
            }

            // HexString form
            byte[] hexValue = new byte[( chars.length - 1 ) / 2];
            int pos = 0;

            for ( int i = 1; i < chars.length; i += 2 )
            {
                if ( Chars.isHex(chars, i) && Chars.isHex(chars, i + 1) )
                {
                    hexValue[pos++] = Hex.getHexValue(chars[i], chars[i + 1]);
                }
                else
                {
                    throw new IllegalArgumentException( I18n.err( I18n.ERR_04214 ) );
                }
            }

            return hexValue;
        }
        else
        {
            boolean escaped = false;
            boolean isHex = false;
            byte pair = -1;
            int pos = 0;

            byte[] bytes = new byte[chars.length * 6];

            for ( int i = 0; i < chars.length; i++ )
            {
                if ( escaped )
                {
                    escaped = false;

                    switch ( chars[i] )
                    {
                        case '\\':
                        case '"':
                        case '+':
                        case ',':
                        case ';':
                        case '<':
                        case '>':
                        case '#':
                        case '=':
                        case ' ':
                            bytes[pos++] = ( byte ) chars[i];
                            break;

                        default:
                            if ( Chars.isHex(chars, i) )
                            {
                                isHex = true;
                                pair = ( ( byte ) ( Hex.getHexValue(chars[i]) << 4 ) );
                            }

                            break;
                    }
                }
                else
                {
                    if ( isHex )
                    {
                        if ( Chars.isHex(chars, i) )
                        {
                            pair += Hex.getHexValue(chars[i]);
                            bytes[pos++] = pair;
                        }
                    }
                    else
                    {
                        switch ( chars[i] )
                        {
                            case '\\':
                                escaped = true;
                                break;

                            // We must not have a special char
                            // Specials are : '"', '+', ',', ';', '<', '>', ' ',
                            // '#' and '='
                            case '"':
                            case '+':
                            case ',':
                            case ';':
                            case '<':
                            case '>':
                            case '#':
                                if ( i != 0 )
                                {
                                    // '#' are allowed if not in first position
                                    bytes[pos++] = '#';
                                    break;
                                }
                            case '=':
                                throw new IllegalArgumentException( I18n.err( I18n.ERR_04215 ) );

                            case ' ':
                                if ( ( i == 0 ) || ( i == chars.length - 1 ) )
                                {
                                    throw new IllegalArgumentException( I18n.err( I18n.ERR_04215 ) );
                                }
                                else
                                {
                                    bytes[pos++] = ' ';
                                    break;
                                }

                            default:
                                if ( ( chars[i] >= 0 ) && ( chars[i] < 128 ) )
                                {
                                    bytes[pos++] = ( byte ) chars[i];
                                }
                                else
                                {
                                    byte[] result = Unicode.charToBytes(chars[i]);
                                    System.arraycopy( result, 0, bytes, pos, result.length );
                                    pos += result.length;
                                }

                                break;
                        }
                    }
                }
            }

            return Strings.utf8ToString(bytes, pos);
        }
    }


    /**
     * Transform a value in a String, accordingly to RFC 2253
     *
     * @param value The attribute value to be escaped
     * @return The escaped string value.
     */
    public static String escapeValue( String value )
    {
        if ( Strings.isEmpty(value) )
        {
            return "";
        }

        char[] chars = value.toCharArray();
        char[] newChars = new char[chars.length * 3];
        int pos = 0;

        for ( int i = 0; i < chars.length; i++ )
        {
            switch ( chars[i] )
            {
                case ' ':
                    if ( ( i > 0 ) && ( i < chars.length - 1 ) )
                    {
                        newChars[pos++] = chars[i];
                    }
                    else
                    {
                        newChars[pos++] = '\\';
                        newChars[pos++] = chars[i];
                    }

                    break;

                case '#':
                    if ( i != 0 )
                    {
                        newChars[pos++] = chars[i];
                    }
                    else
                    {
                        newChars[pos++] = '\\';
                        newChars[pos++] = chars[i];
                    }

                    break;

                case '"':
                case '+':
                case ',':
                case ';':
                case '=':
                case '<':
                case '>':
                case '\\':
                    newChars[pos++] = '\\';
                    newChars[pos++] = chars[i];
                    break;

                case 0x7F:
                    newChars[pos++] = '\\';
                    newChars[pos++] = '7';
                    newChars[pos++] = 'F';
                    break;

                case 0x00:
                case 0x01:
                case 0x02:
                case 0x03:
                case 0x04:
                case 0x05:
                case 0x06:
                case 0x07:
                case 0x08:
                case 0x09:
                case 0x0A:
                case 0x0B:
                case 0x0C:
                case 0x0D:
                case 0x0E:
                case 0x0F:
                    newChars[pos++] = '\\';
                    newChars[pos++] = '0';
                    newChars[pos++] = Strings.dumpHex( ( byte ) ( chars[i] & 0x0F ) );
                    break;

                case 0x10:
                case 0x11:
                case 0x12:
                case 0x13:
                case 0x14:
                case 0x15:
                case 0x16:
                case 0x17:
                case 0x18:
                case 0x19:
                case 0x1A:
                case 0x1B:
                case 0x1C:
                case 0x1D:
                case 0x1E:
                case 0x1F:
                    newChars[pos++] = '\\';
                    newChars[pos++] = '1';
                    newChars[pos++] = Strings.dumpHex( ( byte ) ( chars[i] & 0x0F ) );
                    break;

                default:
                    newChars[pos++] = chars[i];
                    break;

            }
        }

        return new String( newChars, 0, pos );
    }


    /**
     * Transform a value in a String, accordingly to RFC 2253
     *
     * @param attrValue
     *            The attribute value to be escaped
     * @return The escaped string value.
     */
    public static String escapeValue( byte[] attrValue )
    {
        if ( Strings.isEmpty(attrValue) )
        {
            return "";
        }

        String value = Strings.utf8ToString(attrValue);

        return escapeValue( value );
    }


    /**
     * Tells if the Rdn has already been normalized or not
     *
     * @return <code>true</code> if the Rdn is already normalized.
     */
    public boolean isNormalized()
    {
        return normalized.get();
    }


    /**
      * Gets the hashcode of this rdn.
      *
      * @see java.lang.Object#hashCode()
      * @return the instance's hash code
      */
    public int hashCode()
    {
        int result = 37;

        switch ( nbAtavs )
        {
            case 0:
                // An empty Rdn
                break;

            case 1:
                // We have a single AttributeTypeAndValue
                result = result * 17 + atav.hashCode();
                break;

            default:
                // We have more than one AttributeTypeAndValue

                for ( Ava ata : atavs )
                {
                    result = result * 17 + ata.hashCode();
                }

                break;
        }

        return result;
    }


    /**
     * A Rdn is composed of on to many ATAVs (AttributeType And Value).
     * We should write all those ATAVs sequencially, following the
     * structure :
     * <ul>
     *   <li>
     *     <b>parentId</b> The parent entry's Id
     *   </li>
     *   <li>
     *     <b>nbAtavs</b> The number of ATAVs to write. Can't be 0.
     *   </li>
     *   <li>
     *     <b>upName</b> The User provided Rdn
     *   </li>
     *   <li>
     *     <b>normName</b> The normalized Rdn. It can be empty if the normalized
     * name equals the upName.
     *   </li>
     *   <li>
     *     <b>atavs</b>
     *   </li>
     * </ul>
     * <br/>
     * For each ATAV :
     * <ul>
     *   <li>
     *     <b>start</b> The position of this ATAV in the upName string
     *   </li>
     *   <li>
     *     <b>length</b> The ATAV user provided length
     *   </li>
     *   <li>
     *     <b>Call the ATAV write method</b> The ATAV itself
     *   </li>
     * </ul>
     *
     * @see Externalizable#readExternal(ObjectInput)
     * @param out The stream into which the serialized Rdn will be put
     * @throws IOException If the stream can't be written
     */
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        out.writeInt( nbAtavs );
        Unicode.writeUTF(out, upName);

        if ( upName.equals( normName ) )
        {
            Unicode.writeUTF(out, "");
        }
        else
        {
            Unicode.writeUTF(out, normName);
        }

        out.writeInt( start );
        out.writeInt( length );

        switch ( nbAtavs )
        {
            case 0:
                break;

            case 1:
                out.writeObject( atav );
                break;

            default:
                for ( Ava value : atavs )
                {
                    out.writeObject( value );
                }

                break;
        }
    }


    /**
     * We read back the data to create a new RDB. The structure
     * read is exposed in the {@link Rdn#writeExternal(ObjectOutput)}
     * method
     *
     * @see Externalizable#readExternal(ObjectInput)
     * @param in The input stream from which the Rdn will be read
     * @throws IOException If we can't read from the input stream
     * @throws ClassNotFoundException If we can't create a new Rdn
     */
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        // Read the ATAV number
        nbAtavs = in.readInt();

        // Read the UPName
        upName = Unicode.readUTF(in);

        // Read the normName
        normName = Unicode.readUTF(in);

        if ( Strings.isEmpty(normName) )
        {
            normName = upName;
        }

        start = in.readInt();
        length = in.readInt();

        switch ( nbAtavs )
        {
            case 0:
                atav = null;
                break;

            case 1:
                atav = (Ava) in.readObject();
                atavType = atav.getNormType();

                break;

            default:
                atavs = new TreeSet<Ava>();

                atavTypes = new MultiValueMap();

                for ( int i = 0; i < nbAtavs; i++ )
                {
                    Ava value = (Ava) in.readObject();
                    atavs.add( value );
                    atavTypes.put( value.getNormType(), value );
                }

                atav = null;
                atavType = null;

                break;
        }
    }


    /**
     * @return a String representation of the Rdn
     */
    public String toString()
    {
        return upName == null ? "" : upName;
    }

}