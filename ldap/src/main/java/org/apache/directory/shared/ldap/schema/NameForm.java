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
package org.apache.directory.shared.ldap.schema;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;


/**
 * A nameForm description. NameForms define the relationship between a
 * STRUCTURAL objectClass definition and the attributeTypes allowed to be used
 * for the naming of an Entry of that objectClass: it defines which attributes
 * can be used for the RDN.
 * <p>
 * According to ldapbis [MODELS]:
 * </p>
 * 
 * <pre>
 *  4.1.7.2. Name Forms
 *  
 *   A name form &quot;specifies a permissible RDN for entries of a particular
 *   structural object class.  A name form identifies a named object
 *   class and one or more attribute types to be used for naming (i.e.
 *   for the RDN).  Name forms are primitive pieces of specification
 *   used in the definition of DIT structure rules&quot; [X.501].
 * 
 *   Each name form indicates the structural object class to be named,
 *   a set of required attribute types, and a set of allowed attributes
 *   types.  A particular attribute type cannot be listed in both sets.
 * 
 *   Entries governed by the form must be named using a value from each
 *   required attribute type and zero or more values from the allowed
 *   attribute types.
 * 
 *   Each name form is identified by an object identifier (OID) and,
 *   optionally, one or more short names (descriptors).
 * 
 *   Name form descriptions are written according to the ABNF:
 * 
 *     NameFormDescription = LPAREN WSP
 *         numericoid                ; object identifier
 *         [ SP &quot;NAME&quot; SP qdescrs ]  ; short names (descriptors)
 *         [ SP &quot;DESC&quot; SP qdstring ] ;String description
 *         [ SP &quot;OBSOLETE&quot; ]         ; not active
 *         SP &quot;OC&quot; SP oid            ; structural object class
 *         SP &quot;MUST&quot; SP oids         ; attribute types
 *         [ SP &quot;MAY&quot; SP oids ]      ; attribute types
 *         extensions WSP RPAREN     ; extensions
 * 
 *   where:
 * 
 *     [numericoid] is object identifier which identifies this name form;
 *     NAME [qdescrs] are short names (descriptors) identifying this name
 *         form;
 *     DESC [qdstring] is a short descriptive string;
 *     OBSOLETE indicates this name form is not active;
 *     OC identifies the structural object class this rule applies to,
 *     MUST and MAY specify the sets of required and allowed, respectively,
 *         naming attributes for this name form; and
 *     [extensions] describe extensions.
 * 
 *   All attribute types in the required (&quot;MUST&quot;) and allowed (&quot;MAY&quot;) lists
 *   shall be different.
 * </pre>
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc225String2.html">RFC2252 Section 6.22</a>
 * @see <a
 *      href="http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-models-11.txt">ldapbis
 *      [MODELS]</a>
 * @see DescriptionUtils#getDescription(NameForm)
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NameForm extends SchemaObject
{
    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** The structural object class OID this rule applies to */
    private String structuralObjectClassOid;
    
    /** The structural object class this rule applies to */
    private ObjectClass structuralObjectClass;
    
    /** The set of required attribute OIDs for this name form */
    private List<String> mustAttributeTypeOids;

    /** The set of required AttributeTypes for this name form */
    private List<AttributeType> mustAttributeTypes;

    /** The set of allowed attribute OIDs for this name form */
    private List<String> mayAttributeTypeOids;
    
    /** The set of allowed AttributeTypes for this name form */
    private List<AttributeType> mayAttributeTypes;
    

    /**
     * Creates a new instance of MatchingRule.
     *
     * @param oid The MatchingRule OID
     * @param registries The Registries reference
     */
    public NameForm( String oid )
    {
        super( SchemaObjectType.NAME_FORM, oid );
        
        mustAttributeTypeOids = new ArrayList<String>();
        mayAttributeTypeOids = new ArrayList<String>();

        mustAttributeTypes = new ArrayList<AttributeType>();
        mayAttributeTypes = new ArrayList<AttributeType>();
    }
    
    
    /**
     * Inject the registries into this Object, updating the references to
     * other SchemaObject
     *
     * @param registries The Registries
     */
    public void setRegistries( Registries registries ) throws NamingException
    {
        if ( registries != null )
        {
            AttributeTypeRegistry atRegistry = registries.getAttributeTypeRegistry();

            structuralObjectClass = registries.getObjectClassRegistry().lookup( structuralObjectClassOid );
            
            if ( mayAttributeTypeOids != null )
            {
                mayAttributeTypes = new ArrayList<AttributeType>( mayAttributeTypeOids.size() );
                
                for ( String oid : mayAttributeTypeOids )
                {
                    mayAttributeTypes.add( atRegistry.lookup( oid ) );
                }
            }

            if ( mustAttributeTypeOids != null )
            {
                mustAttributeTypes = new ArrayList<AttributeType>( mustAttributeTypeOids.size() );
                
                for ( String oid : mustAttributeTypeOids )
                {
                    mustAttributeTypes.add( atRegistry.lookup( oid ) );
                }
            }
        }
    }


    /**
     * Gets the STRUCTURAL ObjectClass this name form specifies naming
     * attributes for.
     * 
     * @return the ObjectClass's oid this NameForm is for
     */
    public String getStructuralObjectClassOid()
    {
        return structuralObjectClassOid;
    }


    /**
     * Gets the STRUCTURAL ObjectClass this name form specifies naming
     * attributes for.
     * 
     * @return the ObjectClass this NameForm is for
     * @throws NamingException If the structuralObjectClass is invalid
     */
    public ObjectClass getStructuralObjectClass() throws NamingException
    {
        return structuralObjectClass;
    }


    /**
     * Sets the structural object class this rule applies to
     * 
     * @param structuralObjectClass the structural object class to set
     */
    public void setStructuralObjectClassOid( String structuralObjectClassOid )
    {
        if ( !isReadOnly )
        {
            this.structuralObjectClassOid = structuralObjectClassOid;
        }
    }


    /**
     * Sets the structural object class this rule applies to
     * 
     * @param structuralObjectClass the structural object class to set
     */
    public void setStructuralObjectClass( ObjectClass structuralObjectClass )
    {
        if ( !isReadOnly )
        {
            this.structuralObjectClass = structuralObjectClass;
            this.structuralObjectClassOid = structuralObjectClass.getOid();
        }
    }


    /**
     * Gets all the AttributeTypes OIDs of the attributes this NameForm specifies as
     * having to be used in the given objectClass for naming: as part of the
     * Rdn.
     * 
     * @return the AttributeTypes OIDs of the must use attributes
     * @throws NamingException if there is a failure resolving one AttributeTyoe
     */
    public List<String> getMustAttributeTypeOids() throws NamingException
    {
        return Collections.unmodifiableList( mustAttributeTypeOids );
    }


    /**
     * Gets all the AttributeTypes of the attributes this NameForm specifies as
     * having to be used in the given objectClass for naming: as part of the
     * Rdn.
     * 
     * @return the AttributeTypes of the must use attributes
     */
    public List<AttributeType> getMustAttributeTypes()
    {
        return Collections.unmodifiableList( mustAttributeTypes );
    }


    /**
     * Sets the list of required AttributeTypes OIDs
     *
     * @param mustAttributeTypeOids the list of required AttributeTypes OIDs
     */
    public void setMustAttributeTypeOids( List<String> mustAttributeTypeOids )
    {
        if ( !isReadOnly )
        {
            this.mustAttributeTypeOids = mustAttributeTypeOids;
        }
    }

    
    /**
     * Sets the list of required AttributeTypes
     *
     * @param mayAttributeTypes the list of required AttributeTypes
     */
    public void setMustAttributeTypes( List<AttributeType> mustAttributeTypes )
    {
        if ( !isReadOnly )
        {
            this.mustAttributeTypes = mustAttributeTypes;
            
            // update the OIDS now
            mustAttributeTypeOids.clear();
            
            for ( AttributeType may : mustAttributeTypes )
            {
                mustAttributeTypeOids.add( may.getOid() );
            }
        }
    }

    
    /**
     * Add a required AttributeType OID
     *
     * @param oid The attributeType OID
     */
    public void addMustAttributeTypeOids( String oid )
    {
        if ( !isReadOnly )
        {
            mustAttributeTypeOids.add( oid );
        }
    }


    /**
     * Add a required AttributeType
     *
     * @param attributeType The attributeType
     */
    public void addMustAttributeTypes( AttributeType attributeType )
    {
        if ( !isReadOnly )
        {
            if ( ! mustAttributeTypeOids.contains( attributeType.getOid() ) )
            {
                mustAttributeTypes.add( attributeType );
                mustAttributeTypeOids.add( attributeType.getOid() );
            }
        }
    }
    
    
    /**
     * Gets all the AttributeTypes OIDs of the attribute this NameForm specifies as
     * being usable without requirement in the given objectClass for naming: as
     * part of the Rdn.
     * 
     * @return the AttributeTypes OIDs of the may use attributes
     * @throws NamingException if there is a failure resolving one AttributeTyoe
     */
    public List<String> getMayAttributeTypeOids() throws NamingException
    {
        return Collections.unmodifiableList( mayAttributeTypeOids );
    }

    
    /**
     * Gets all the AttributeTypes of the attribute this NameForm specifies as
     * being useable without requirement in the given objectClass for naming: as
     * part of the Rdn.
     * 
     * @return the AttributeTypes of the may use attributes
     */
    public List<AttributeType> getMayAttributeTypes()
    {
        return Collections.unmodifiableList( mayAttributeTypes );
    }
    
    
    /**
     * Sets the list of allowed AttributeTypes
     *
     * @param mayAttributeTypeOids the list of allowed AttributeTypes
     */
    public void setMayAttributeTypeOids( List<String> mayAttributeTypeOids )
    {
        if ( !isReadOnly )
        {
            this.mayAttributeTypeOids = mayAttributeTypeOids;
        }
    }
    
    
    /**
     * Sets the list of allowed AttributeTypes
     *
     * @param mayAttributeTypes the list of allowed AttributeTypes
     */
    public void setMayAttributeTypes( List<AttributeType> mayAttributeTypes )
    {
        if ( !isReadOnly )
        {
            this.mayAttributeTypes = mayAttributeTypes;
            
            // update the OIDS now
            mayAttributeTypeOids.clear();
            
            for ( AttributeType may : mayAttributeTypes )
            {
                mayAttributeTypeOids.add( may.getOid() );
            }
        }
    }
    
    
    /**
     * Add an allowed AttributeType
     *
     * @param oid The attributeType oid
     */
    public void addMayAttributeTypeOids( String oid )
    {
        if ( !isReadOnly )
        {
            mayAttributeTypeOids.add( oid );
        }
    }


    /**
     * Add an allowed AttributeType
     *
     * @param attributeType The attributeType
     */
    public void addMayAttributeTypes( AttributeType attributeType )
    {
        if ( !isReadOnly )
        {
            if ( ! mayAttributeTypeOids.contains( attributeType.getOid() ) )
            {
                mayAttributeTypes.add( attributeType );
                mayAttributeTypeOids.add( attributeType.getOid() );
            }
        }
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return DescriptionUtils.getDescription( this );
    }
    
    
    /**
     * Clone a NameForm
     */
    public NameForm clone() throws CloneNotSupportedException
    {
        NameForm clone = (NameForm)super.clone();
        
        // Clone the MAY AttributeTypes
        clone.mayAttributeTypeOids = new ArrayList<String>();
        
        for ( String oid : mayAttributeTypeOids )
        {
            clone.mayAttributeTypeOids.add( oid );
        }
        
        clone.mayAttributeTypes = new ArrayList<AttributeType>();
        
        // Clone the MUST AttributeTypes
        clone.mustAttributeTypeOids = new ArrayList<String>();
        
        for ( String oid : mustAttributeTypeOids )
        {
            clone.mustAttributeTypeOids.add( oid );
        }
        
        clone.mustAttributeTypes = new ArrayList<AttributeType>();

        // All the references to other Registries object are set to null.
        clone.structuralObjectClass = null;
        
        return clone;
    }
}
