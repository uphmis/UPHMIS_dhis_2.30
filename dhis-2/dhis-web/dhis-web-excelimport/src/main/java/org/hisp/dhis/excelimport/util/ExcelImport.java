package org.hisp.dhis.excelimport.util;

import java.io.Serializable;

@SuppressWarnings( "serial" )
public class ExcelImport implements Serializable
{
    /**
     * 
     */
    private String dataelement;
    
    /**
     * 
     */
    private String orgunit;
   
    private String categoryoptioncombo;
    private String attributeoptioncombo;
    private String comment;
    
    private String orgunitgroup;

    /**
     * Sheet number
     */
    private int sheetno;
    
    /**
     * Row number
     */
    private int rowno;
    
    /**
     * Column number
     */
    private int colno;
    
    /**
     * Formula to calculate the values.
     */
    private String expression;

    private String orgUnitGroupValue;
    
    
    //Constructors
    public ExcelImport()
    {
        
    }
    
    public ExcelImport( String dataelement, String orgunit,String categoryoptioncombo, String attributeoptioncombo,String comment,String orgunitgroup, int sheetno, int rowno, int colno, String expression )
    {
        this.dataelement = dataelement;
        this.orgunit = orgunit;
        this.categoryoptioncombo = categoryoptioncombo;
        this.attributeoptioncombo = attributeoptioncombo;
        this.comment = comment;
        this.orgunitgroup = orgunitgroup;
        this.sheetno = sheetno;
        this.rowno = rowno;
        this.colno = colno;
        this.expression = expression;        
    }

    public ExcelImport( String dataelement, String orgunit,String categoryoptioncombo, String attributeoptioncombo, int sheetno, int rowno, int colno, String expression )
    {
        this.dataelement = dataelement;
        this.orgunit = orgunit;
        this.categoryoptioncombo = categoryoptioncombo;
        this.attributeoptioncombo = attributeoptioncombo;
        this.sheetno = sheetno;
        this.rowno = rowno;
        this.colno = colno;
        this.expression = expression;        
    }
    
    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------
    
    public String getDataelement()
    {
        return dataelement;
    }

    public void setDataelement( String dataelement )
    {
        this.dataelement = dataelement;
    }

    public String getOrgunit()
    {
        return orgunit;
    }

    public void setOrgunit( String orgunit )
    {
        this.orgunit = orgunit;
    }

    public String getCategoryoptioncombo()
    {
        return categoryoptioncombo;
    }

    public void setCategoryoptioncombo( String categoryoptioncombo )
    {
        this.categoryoptioncombo = categoryoptioncombo;
    }

    public String getAttributeoptioncombo()
    {
        return attributeoptioncombo;
    }

    public void setAttributeoptioncombo( String attributeoptioncombo )
    {
        this.attributeoptioncombo = attributeoptioncombo;
    }

    public String getComment()
    {
        return comment;
    }

    public void setComment( String comment )
    {
        this.comment = comment;
    }

    public String getOrgunitgroup()
    {
        return orgunitgroup;
    }

    public void setOrgunitgroup( String orgunitgroup )
    {
        this.orgunitgroup = orgunitgroup;
    }
    
   /** public String getorgUnitGroupValue()
    {
        return getorgUnitGroupValue();
    }

    public void setorgUnitGroupValue( String orgUnitGroupValue )
    {
        this.orgUnitGroupValue = orgUnitGroupValue;
    }**/
    
    public int getSheetno()
    {
        return sheetno;
    }

    public void setSheetno( int sheetno )
    {
        this.sheetno = sheetno;
    }

    public int getRowno()
    {
        return rowno;
    }

    public void setRowno( int rowno )
    {
        this.rowno = rowno;
    }

    public int getColno()
    {
        return colno;
    }

    public void setColno( int colno )
    {
        this.colno = colno;
    }

    public String getExpression()
    {
        return expression;
    }

    public void setExpression( String expression )
    {
        this.expression = expression;
    }
    
    
    
}
