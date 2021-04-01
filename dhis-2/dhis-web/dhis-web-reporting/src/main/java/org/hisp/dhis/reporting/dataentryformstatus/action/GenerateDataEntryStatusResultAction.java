package org.hisp.dhis.reporting.dataentryformstatus.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

/**
 * @author Mithilesh Kumar Thakur
 */
public class GenerateDataEntryStatusResultAction implements Action
{
    final Pattern INPUT_PATTERN = Pattern.compile( "(<input.*?/>)", Pattern.DOTALL );
    final Pattern IDENTIFIER_PATTERN = Pattern.compile( "id=\"(\\w+)-(\\w+)-val\"" );
    final Pattern DATAELEMENT_TOTAL_PATTERN = Pattern.compile( "dataelementid=\"(\\w+?)\"" );
    final Pattern INDICATOR_PATTERN = Pattern.compile( "indicatorid=\"(\\w+)\"" );
    final Pattern VALUE_TAG_PATTERN = Pattern.compile( "value=\"(.*?)\"", Pattern.DOTALL );
    final Pattern TITLE_TAG_PATTERN = Pattern.compile( "title=\"(.*?)\"", Pattern.DOTALL );

	// -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------
    @Autowired
    private DataSetService dataSetService;
    
    @Autowired
    private CategoryService categoryService;
    // -------------------------------------------------------------------------
    // input / output
    // -------------------------------------------------------------------------
    
    private Integer selectedDataSetId;
    
    public void setSelectedDataSetId( Integer selectedDataSetId )
    {
        this.selectedDataSetId = selectedDataSetId;
    }
    
    private List<DataElement> dataElementList = new ArrayList<DataElement>();
  
    public List<DataElement> getDataElementList()
    {
        return dataElementList;
    }

    private Set<DataElement> dataElements = new HashSet<DataElement>();
    
    public Set<DataElement> getDataElements()
    {
        return dataElements;
    }
    
    private DataSet dataSet;
    
    public DataSet getDataSet()
    {
        return dataSet;
    }
    
    private List<DataElement> deList = new ArrayList<DataElement>();
    
    public List<DataElement> getDeList()
    {
        return deList;
    }
    
    private int dataSetMemberCount;
    
    public int getDataSetMemberCount()
    {
        return dataSetMemberCount;
    }

    
    // -------------------------------------------------------------------------
    // Action Implementation
    // -------------------------------------------------------------------------

    public String execute() throws Exception
    {
        if( selectedDataSetId != null || selectedDataSetId != -1 )
        {
            dataSet = dataSetService.getDataSet( selectedDataSetId );
            
            dataElements = new HashSet<DataElement>( getDataElementsInDataEntryForm( dataSet ));
            
            dataElementList = new ArrayList<DataElement>( dataElements );
            
            deList = new ArrayList<DataElement>( dataSet.getDataElements() );
            
            deList.removeAll( dataElementList );
        }
        
        //dataSet.getDataElements().size();
        
        //Collections.sort( dataElementList, new IdentifiableObjectNameComparator() );
        
        //Collections.sort( deList, new IdentifiableObjectNameComparator() );
        
        dataSetMemberCount = 0;
        for ( DataElement de : dataSet.getDataElements() )
        {
            //dataSetMemberCount += de.getCategoryCombo().getOptionCombos().size();
            dataSetMemberCount += de.getCategoryOptionCombos().size();
        }
        
        return SUCCESS;
    }
    
    
    // get DataElement in dataEntry Form by DataSet
    public Set<DataElement> getDataElementsInDataEntryForm( DataSet dataSet )
    {
        if ( dataSet == null || !dataSet.hasDataEntryForm() )
        {
            return null;
        }

        Map<String, DataElement> dataElementMap = getDataElementMap( dataSet );
        
        Set<DataElementOperand> operands = new HashSet<>();

        Set<DataElement> dataElements = new HashSet<>();

        Matcher inputMatcher = INPUT_PATTERN.matcher( dataSet.getDataEntryForm().getHtmlCode() );
        
        
        //DataElementCategoryOptionCombo categoryOptionCombo = null;
        CategoryOptionCombo categoryOptionCombo = null;
        
        while ( inputMatcher.find() )
        {
            String inputHtml = inputMatcher.group();

            Matcher identifierMatcher = IDENTIFIER_PATTERN.matcher( inputHtml );
            Matcher dataElementTotalMatcher = DATAELEMENT_TOTAL_PATTERN.matcher( inputHtml );

            DataElement dataElement = null;

            if ( identifierMatcher.find() && identifierMatcher.groupCount() > 0 )
            {
                String dataElementId = identifierMatcher.group( 1 );
                dataElement = dataElementMap.get( dataElementId );
                
                String categoryOptionComboId = identifierMatcher.group( 2 );
                
                categoryOptionCombo = categoryService.getCategoryOptionCombo( categoryOptionComboId );
                
                //DataElementOperand operand = new DataElementOperand( dataElementId, categoryOptionComboId );
                
                DataElementOperand operand = new DataElementOperand( dataElement, categoryOptionCombo );

                operands.add( operand );
                
                if ( categoryOptionComboId != null )
                {
                    categoryOptionCombo = categoryService.getCategoryOptionCombo( categoryOptionComboId );
                    //categoryOptionCombo = categoryService.getDataElementCategoryOptionCombo( categoryOptionComboId );
                }
                else
                {
                    categoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();
                    //categoryOptionCombo = categoryService.getDataElementCategoryOptionCombo( categoryOptionComboId );
                }
                
            }
            else if ( dataElementTotalMatcher.find() && dataElementTotalMatcher.groupCount() > 0 )
            {
                String dataElementId = dataElementTotalMatcher.group( 1 );
                dataElement = dataElementMap.get( dataElementId );
                
                String categoryOptionComboId = dataElementTotalMatcher.group( 2 );
                
                //DataElementOperand operand = new DataElementOperand( dataElementId, categoryOptionComboId );
                DataElementOperand operand = new DataElementOperand( dataElement, categoryOptionCombo );
                operands.add( operand );
                
                if ( categoryOptionComboId != null )
                {
                    categoryOptionCombo = categoryService.getCategoryOptionCombo( categoryOptionComboId );
                }
                else
                {
                    categoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();
                }
                
            }
            
            //System.out.println(  " operands size -- " + operands.size() );
            
            //System.out.println(  " dataElement -- " + dataElement.getName() + "--- UID " + dataElement.getUid() + " categoryOptionCombo -- " + categoryOptionCombo.getName() + "--- UID " + categoryOptionCombo.getUid());
            
            /*
            for( DataElementOperand op : operands )
            {
                System.out.println( op.getDataElement().getName() + " -- " + op.getCategoryOptionCombo().getName() );
            }
            */
            
            if ( dataElement != null )
            {
                dataElements.add( dataElement );
            }
        }

        return dataElements;
    }
    
    
    
    private Map<String, DataElement> getDataElementMap( DataSet dataSet )
    {
        Map<String, DataElement> map = new HashMap<>();

        for ( DataElement element : dataSet.getDataElements() )
        {
            map.put( element.getUid(), element );
        }

        return map;
    }    
    

}
