/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2;

/**
 * Define a superclass for all the CDM node classes: Group, Dimension, etc.
 * Define the sort of the node {@link CDMSort} so that we can
 * 1. do true switching on node type
 * 2. avoid use of instanceof
 * 3. Use container classes that have more than one kind of node
 *
 * Also move various common fields and methods to here.
 *
 * @author Heimbigner
 */

public class CDMNode
{
    CDMSort sort = null;
    Group group = null;
    boolean immutable = false;
    String shortName = null;

    // Constructors

    protected CDMNode()
    {
	// Use Instanceof to figure out the sort
	if(this instanceof Attribute)
	    setSort(CDMSort.ATTRIBUTE);
	else if(this instanceof Dimension)
	    setSort(CDMSort.DIMENSION);
	else if(this instanceof EnumTypedef)
	    setSort(CDMSort.ENUMERATION);
	else if(this instanceof Sequence)
	    setSort(CDMSort.SEQUENCE);
	else if(this instanceof Structure)
	    setSort(CDMSort.STRUCTURE);
	else if(this instanceof Group)
	    setSort(CDMSort.GROUP);
	else if(this instanceof Variable) // Only case left is atomic var
	    setSort(CDMSort.VARIABLE);
    }

    public CDMNode(String name) {this(); setShortName(name);}

    // Get/Set
    public CDMSort getSort() {return this.sort;}    

    public void setSort(CDMSort sort) {if(!immutable) this.sort = sort;}

    /**
     * Get the short name of this Variable. The name is unique within its parent group.
     */
    public String getShortName() {return this.shortName;}    

    /**
     * Set the short name of this Variable. The name is unique within its parent group.
     * @param name new short name
     */
    public void setShortName(String name)
	{if(!immutable) this.shortName = name;}

   /**
    * Get its parent Group, or null if its the root group.
    *
    * @return parent Group
    */
    public Group getParentGroup() {return this.group;}    

   /**
    * Alias for getParentGroup
    *
    * @return parent Group
    */
    public Group getGroup() {return getParentGroup();}    

   /**
    * Set the parent Group
    *
    * @param parent The new parent group
    */
    public void setParentGroup(Group parent)
	{if(!immutable) this.group = parent;}

   /**
    * Get immutable flag
    * As a rule, subclasses will access directly
    *
    * @return Immutable flag
    */
    public boolean getImmutable() {return this.immutable;}    

   /**
    * Set the immutable flag
    *
    * @param tf The new value for the immutable flag
    */
    public void setImmutable(boolean tf) {this.immutable = tf;}
}
