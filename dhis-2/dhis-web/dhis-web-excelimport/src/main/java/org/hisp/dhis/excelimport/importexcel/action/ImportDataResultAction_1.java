package org.hisp.dhis.excelimport.importexcel.action;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jxl.Sheet;
import jxl.Workbook;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.excelimport.util.ExcelImport;
import org.hisp.dhis.excelimport.util.ExcelImportService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSetStore;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.WeeklyPeriodType;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

public class ImportDataResultAction_1 
implements Action
{
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  @Autowired
  private OrganisationUnitGroupSetStore organisationUnitGroupSetStore;


  @Autowired
  private PeriodService periodService;

  @Autowired
  private CategoryService categoryService;
  
  @Autowired
  private DataElementService dataElementService;
  
  @Autowired
  private OrganisationUnitService organisationUnitService;
  
  @Autowired
  private DataValueService dataValueService;
  
  @Autowired
  private CurrentUserService currentUserService;

  @Autowired
  private ExcelImportService excelImportService;

  // -------------------------------------------------------------------------
  // Input/Output
  // -------------------------------------------------------------------------
  private File file;

  public void setUpload( File file )
  {
      this.file = file;
  }

  private String fileName;

  public String getFileName()
  {
      return fileName;
  }

  public void setUploadFileName( String fileName )
  {
      this.fileName = fileName;
  }

  private InputStream inputStream;

  public InputStream getInputStream()
  {
      return inputStream;
  }

  private Integer year;

  private String org;

  public void setorg( String org )
  {
      this.org = org;
  }

  public void setYear( Integer year )
  {
      this.year = year;
  }

  private String orgUnitGroupId;

  public void setOrgUnitGroupId( String orgUnitGroupId )
  {
      this.orgUnitGroupId = orgUnitGroupId;
  }

  private String weeklyPeriodTypeName;

  private String deCodesXMLFileName = "";

  private SimpleDateFormat simpleDateFormat;

  private String message;

  public String getMessage()
  {
      return message;
  }
  
  OrganisationUnitGroup orgUnitGroup;
  OrganisationUnitGroup orgUnitGroup1;

  String orgGroup ,orgGroupUid;

  private ArrayList<OrganisationUnitGroup> organisationUnitGroups;

  private String storedBy;
  
  // -------------------------------------------------------------------------
  // Action implementation
  // -------------------------------------------------------------------------

  @SuppressWarnings( "unchecked" )
  public String execute()
      throws Exception
  {
     // System.out.println( "orgunit --" + org );
      OrganisationUnitGroupSet OrganisationUnitGroupSet = organisationUnitGroupSetStore.getByCode( "ExcelExportGroupSet" );
      organisationUnitGroups = new ArrayList<OrganisationUnitGroup>( OrganisationUnitGroupSet.getOrganisationUnitGroups());
      
      message = "";
      
      System.out.println( "Start Time : " + new Date() );
      simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd" );

      String startDate = "01-01-" + year;
      String endDate = "31-12-" + year;

      // period information
      weeklyPeriodTypeName = WeeklyPeriodType.NAME;
      PeriodType periodType = periodService.getPeriodTypeByName( weeklyPeriodTypeName );

      
      storedBy = currentUserService.getCurrentUsername();
      List<ExcelImport> excelExportDesignList = new ArrayList<ExcelImport>();
      
      deCodesXMLFileName = "importData.xml";
      
      excelExportDesignList = new ArrayList<ExcelImport>( excelImportService.getExcelImportDesign( deCodesXMLFileName ) );
      
      
      String fileType = fileName.substring( fileName.indexOf( '.' ) + 1, fileName.length() );

      if ( !fileType.equalsIgnoreCase( "xls" ) )
      {
          message = "The file you are trying to import is not an excel file";

          return SUCCESS;
      }

      System.out.println( " File Name  : " + fileName );
      Workbook excelImportFile = Workbook.getWorkbook( file );
      
      Iterator<ExcelImport> excelExportDesignIterator = excelExportDesignList.iterator();
      int count1 = 0;
      //System.out.println( count1 + " ---  " + excelExportDesignList.size() ); 
      while ( excelExportDesignIterator.hasNext() )
      {
    	 // System.out.println( count1 + " ---  "  ); 
    	  ExcelImport exceEmportDesign = (ExcelImport) excelExportDesignIterator.next();

          String deCodeString = exceEmportDesign.getExpression();
         // System.out.println( count1 + " ---  " + deCodeString ); 
          String dataElementUid = exceEmportDesign.getDataelement();
          String categroyComboUid = exceEmportDesign.getCategoryoptioncombo();
          String attributeComboUid = exceEmportDesign.getAttributeoptioncombo();
          String orgUnitUid = exceEmportDesign.getOrgunit();
          int tempRowNo = exceEmportDesign.getRowno();
          int tempColNo = exceEmportDesign.getColno();
          int sheetNo = exceEmportDesign.getSheetno();
          
          Sheet sheet = excelImportFile.getSheet( sheetNo );
          String cellContent = sheet.getCell( tempColNo, tempRowNo ).getContents();
          String cellContentPeriodUid = sheet.getCell( tempColNo-4, tempRowNo ).getContents();
          
          System.out.println( " cellContent : " + cellContent );
          System.out.println( " cellContentPeriodUid : " + cellContentPeriodUid );
          
          DataElement dataElement = dataElementService.getDataElement(dataElementUid);
          CategoryOptionCombo categoryOptionCombo = categoryService.getCategoryOptionCombo( categroyComboUid );
          //DataElementCategoryOptionCombo categoryOptionCombo = categoryService.getDataElementCategoryOptionCombo( categroyComboUid );
          
          //DataElementCategoryOptionCombo attributeOptionCombo = categoryService.getDataElementCategoryOptionCombo( attributeComboUid );
          
          CategoryOptionCombo attributeOptionCombo = categoryService.getCategoryOptionCombo( attributeComboUid );
          OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit(orgUnitUid);
          
          
          List<Period> periods = new ArrayList<Period>();
          Period tempPeriod  = new Period();
          if ( cellContentPeriodUid != null )
          {

              Period period = periodService.reloadIsoPeriod( cellContentPeriodUid );

              if ( period != null )
              {
                  periods = new ArrayList<Period>( periodService.getPeriodsBetweenDates( periodType,
                      period.getStartDate(), period.getEndDate() ) );
              }
              
              if( periods != null && periods.size() > 0 )
              {
            	  tempPeriod = periods.get(1);
              }
          }
          
          if ( cellContent.equalsIgnoreCase( "" ) || cellContent == null || cellContent.equalsIgnoreCase( " " ) )
          {
              count1++;

              continue;
          }
          
          
//          System.out.println( " dataElement : " + dataElement.getId() );
//          System.out.println( " tempPeriod : " + tempPeriod.getId() );
//          System.out.println( " organisationUnit : " + organisationUnit.getId() );
//          System.out.println( " categoryOptionCombo : " + categoryOptionCombo.getId() );
//          System.out.println( " attributeOptionCombo : " + attributeOptionCombo.getId() );
//          
          Date now = new Date();

          DataValue dataValue = dataValueService.getDataValue( dataElement, tempPeriod, organisationUnit, categoryOptionCombo, attributeOptionCombo );
          
          if ( dataValue == null )
          {
        	  try
              {
                  //dataValueService.addDataValue( dataValue );
                  dataValue = new DataValue();

                  dataValue.setDataElement( dataElement );
                  dataValue.setPeriod( tempPeriod );
                  dataValue.setSource( organisationUnit );
                  dataValue.setCategoryOptionCombo( categoryOptionCombo );
                  dataValue.setAttributeOptionCombo(attributeOptionCombo);
                  dataValue.setLastUpdated(  new Date() );
                  dataValue.setStoredBy( storedBy );
                  dataValue.setValue(cellContent);
                  dataValue.setCreated( now );
                  dataValueService.addDataValue( dataValue );
                 // System.out.println( " dataValue : " + cellContent + " -- added " );
                  
              }
              catch ( Exception ex )
              {
                  throw new RuntimeException( "Cannot add datavalue", ex );
              }
          } 
          else 
          {
              try
              {
            	  dataValue.setValue( cellContent );
            	  dataValue.setLastUpdated( now );
            	  dataValue.setStoredBy( storedBy );

                  dataValueService.updateDataValue( dataValue );
                  
                  //System.out.println( " dataValue : " + cellContent + " -- updated " );
                  
              }
              catch ( Exception ex )
              {
                  throw new RuntimeException( "Cannot update datavalue", ex );
              }
          }

          count1++;
          
          
//          System.out.println( " dataElement : " + dataElement.getId() );
//          System.out.println( " tempPeriod : " + tempPeriod.getId() );
//          System.out.println( " organisationUnit : " + organisationUnit.getId() );
//          System.out.println( " categoryOptionCombo : " + categoryOptionCombo.getId() );
//          System.out.println( " attributeOptionCombo : " + attributeOptionCombo.getId() );
         // System.out.println( " dataValue : " + cellContent + " -- updated " );
          System.out.println( count1 + " ---  " + deCodeString ); 
      }// inner while loop end
      
      
      
      
      
     
      excelImportFile.close();

      message = "The report has been imported successfully";

		try
		{
		}
		catch( Exception e )
		{
		}
		finally
		{
			if( inputStream != null )
				inputStream.close();		 
		}
		
      System.out.println( "End Time : " + new Date() );

      return SUCCESS;
  }

  // getting data value using Map
  private String getAggVal( String expression, Map<String, String> aggDeMap )
  {
      try
      {
          Pattern pattern = Pattern.compile( "(\\[\\d+\\.\\d+\\])" );

          Matcher matcher = pattern.matcher( expression );
          StringBuffer buffer = new StringBuffer();

          String resultValue = "";

          while ( matcher.find() )
          {
              String replaceString = matcher.group();

              replaceString = replaceString.replaceAll( "[\\[\\]]", "" );

              replaceString = aggDeMap.get( replaceString );

              if ( replaceString == null )
              {
                  replaceString = "0";
              }

              matcher.appendReplacement( buffer, replaceString );

              resultValue = replaceString;
          }

          matcher.appendTail( buffer );

          double d = 0.0;
          try
          {
              d = MathUtils.calculateExpression( buffer.toString() );
          }
          catch ( Exception e )
          {
              d = 0.0;
              resultValue = "";
          }

          resultValue = "" + (int) d;

          return resultValue;
      }
      catch ( NumberFormatException ex )
      {
          throw new RuntimeException( "Illegal DataElement id", ex );
      }
  }

}

