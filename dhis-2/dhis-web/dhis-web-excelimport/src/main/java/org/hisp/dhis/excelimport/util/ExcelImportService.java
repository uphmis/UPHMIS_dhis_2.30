/* Updated by Sunakshi and Mithilesh Kumar Thakur on 10/04/18
 * Line 239*/
package org.hisp.dhis.excelimport.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.hisp.dhis.config.ConfigurationService;
import org.hisp.dhis.config.Configuration_IN;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.WeeklyPeriodType;
import org.hisp.dhis.system.util.MathUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class ExcelImportService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------
    private ConfigurationService configurationService;

    public void setConfigurationService( ConfigurationService configurationService )
    {
        this.configurationService = configurationService;
    }

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private PeriodService periodService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // -------------------------------------------------------------------------
    // Support Methods Defination
    // -------------------------------------------------------------------------
    public List<ExcelImport> getExcelImportDesignDesign( String xmlFileName )
    {
        List<ExcelImport> deCodes = new ArrayList<ExcelImport>();

        String raFolderName = configurationService.getConfigurationByKey( Configuration_IN.KEY_REPORTFOLDER )
            .getValue();

        String path = System.getenv( "DHIS2_HOME" ) + File.separator + raFolderName + File.separator + xmlFileName;

        try
        {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse( new File( path ) );
            if ( doc == null )
            {
                System.out.println( "DECodes related XML file not found" );
                return null;
            }

            NodeList listOfDECodes = doc.getElementsByTagName( "de-code" );
            int totalDEcodes = listOfDECodes.getLength();

            for ( int s = 0; s < totalDEcodes; s++ )
            {
                Element deCodeElement = (Element) listOfDECodes.item( s );
                NodeList textDECodeList = deCodeElement.getChildNodes();

                String expression = ((Node) textDECodeList.item( 0 )).getNodeValue().trim();
                if ( expression != null && !expression.equalsIgnoreCase( "0" ) )
                {
                    String dataelement = deCodeElement.getAttribute( "dataelement" );
                    String orgunit = deCodeElement.getAttribute( "orgunit" );
                    String categoryoptioncombo = deCodeElement.getAttribute( "categoryoptioncombo" );
                    String attributeoptioncombo = deCodeElement.getAttribute( "attributeoptioncombo" );
                    String comment = deCodeElement.getAttribute( "comment" );
                    String orgunitgroup = deCodeElement.getAttribute( "orgunitgroup" );

                    int sheetno = new Integer( deCodeElement.getAttribute( "sheetno" ) );
                    int rowno = new Integer( deCodeElement.getAttribute( "rowno" ) );
                    int colno = new Integer( deCodeElement.getAttribute( "colno" ) );

                    ExcelImport exportDataDesign = new ExcelImport( dataelement, orgunit, categoryoptioncombo,
                        attributeoptioncombo, comment, orgunitgroup, sheetno, rowno, colno, expression );

                    deCodes.add( exportDataDesign );
                }

            } // end of for loop with s var
        } // try block end
        catch ( SAXParseException err )
        {
            System.out.println( "** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId() );
            System.out.println( " " + err.getMessage() );
        }
        catch ( SAXException e )
        {
            Exception x = e.getException();
            ((x == null) ? e : x).printStackTrace();
        }
        catch ( Throwable t )
        {
            t.printStackTrace();
        }
        return deCodes;
    }// getDECodes end

    public String getDataelementIds( List<ExcelImport> reportDesignList )
    {
        String dataElmentIdsByComma = "-1";
        for ( ExcelImport excelImportDesign : reportDesignList )
        {
            String formula = excelImportDesign.getExpression();

            try
            {
                // Pattern pattern = Pattern.compile( "(\\[\\d+\\.\\d+\\])" );
                Pattern pattern = Pattern.compile( "(\\[\\w+\\.\\w+\\])" );

                Matcher matcher = pattern.matcher( formula );

                StringBuffer buffer = new StringBuffer();

                while ( matcher.find() )
                {

                    String replaceString = matcher.group();

                    replaceString = replaceString.replaceAll( "[\\[\\]]", "" );
                    replaceString = replaceString.substring( 0, replaceString.indexOf( '.' ) );

                    dataElementService.getDataElement( replaceString );

                    String dataElementUid = replaceString;

                    DataElement de = dataElementService.getDataElement( dataElementUid );
                    if ( de != null )
                    {
                        int dataElementId = de.getId();

                        dataElmentIdsByComma += "," + dataElementId;
                        replaceString = "";
                        matcher.appendReplacement( buffer, replaceString );
                    }

                }
            }
            catch ( Exception e )
            {

            }
        }

        return dataElmentIdsByComma;
    }

    public String getDataelementUIds( List<ExcelImport> reportDesignList )
    {
        String dataElmentIdsByComma = "'" + "temp" + "'";
        for ( ExcelImport report_inDesign : reportDesignList )
        {
            String formula = report_inDesign.getExpression();
            try
            {
                Pattern pattern = Pattern.compile( "(\\[\\w+\\.\\w+\\])" );

                Matcher matcher = pattern.matcher( formula );
                StringBuffer buffer = new StringBuffer();

                while ( matcher.find() )
                {
                    String replaceString = matcher.group();

                    replaceString = replaceString.replaceAll( "[\\[\\]]", "" );
                    replaceString = replaceString.substring( 0, replaceString.indexOf( '.' ) );

                    String dataElementUId = replaceString;
                    dataElmentIdsByComma += "," + "'" + dataElementUId + "'";
                    replaceString = "";
                    matcher.appendReplacement( buffer, replaceString );
                }
            }
            catch ( Exception e )
            {

            }
        }

        return dataElmentIdsByComma;
    }

    public Map<String, String> getAggDataFromDataValueTable( String orgUnitIdsByComma, String dataElmentIdsByComma,
        String periodIdsByComma )
    {
        Map<String, String> aggDeMap = new HashMap<String, String>();

        try
        {
            String query = "";

            query = "SELECT de.uid,coc.uid, SUM( cast( dv.value as numeric) ) FROM datavalue dv  "
                + " INNER JOIN dataelement de ON de.dataelementid = dv.dataelementid "
                + " INNER JOIN categoryoptioncombo coc ON coc.categoryoptioncomboid = dv.categoryoptioncomboid "
                + " WHERE de.uid IN (" + dataElmentIdsByComma + " ) AND " + " sourceid IN (" + orgUnitIdsByComma
                + " ) AND " + " periodid IN (" + periodIdsByComma + ") GROUP BY de.uid,coc.uid";

            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                String deUId = rs.getString( 1 );
                String categoryComUId = rs.getString( 2 );
                Double aggregatedValue = rs.getDouble( 3 );
                if ( aggregatedValue != null )
                {
                    aggDeMap.put( deUId + "." + categoryComUId, "" + aggregatedValue );
                }
            }

            return aggDeMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id", e );
        }
    }

    /* get attribute value and code */

    public Map<String, String> getAttributeValueCode()
    {

        String query = "";

        Map<String, String> attValueMap = new HashMap<String, String>();

        query = "SELECT orgUnitGroup.uid, av.value FROM attributevalue av "
            + " INNER JOIN orgunitgroupattributevalues oav ON av.attributevalueid = oav.attributevalueid "
            + " INNER JOIN orgunitgroup orgUnitGroup ON orgUnitGroup.orgunitgroupid = oav.orgunitgroupid "
            + " INNER JOIN attribute attr ON av.attributeid = attr.attributeid "
            + " WHERE attr.code = 'OrgUnitGroupCode' ";

        SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

        while ( rs.next() )
        {
            String orgGroupUId = rs.getString( 1 );
            String attributeValue = rs.getString( 2 );

            if ( orgGroupUId != null && attributeValue != null )
            {
                attValueMap.put( orgGroupUId, attributeValue );
            }
        }
        return attValueMap;

    }

    public List<ExcelImport> getExcelImportDesign( String xmlFileName )
    {
        List<ExcelImport> deCodes = new ArrayList<ExcelImport>();

        String raFolderName = configurationService.getConfigurationByKey( Configuration_IN.KEY_REPORTFOLDER )
            .getValue();

        String path = System.getenv( "DHIS2_HOME" ) + File.separator + raFolderName + File.separator + xmlFileName;

        try
        {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse( new File( path ) );
            if ( doc == null )
            {
                System.out.println( "DECodes related XML file not found" );
                return null;
            }

            NodeList listOfDECodes = doc.getElementsByTagName( "de-code" );
            int totalDEcodes = listOfDECodes.getLength();

            for ( int s = 0; s < totalDEcodes; s++ )
            {
                Element deCodeElement = (Element) listOfDECodes.item( s );
                NodeList textDECodeList = deCodeElement.getChildNodes();

                String expression = ((Node) textDECodeList.item( 0 )).getNodeValue().trim();
                if ( expression != null && !expression.equalsIgnoreCase( "0" ) )
                {
                    String dataelement = deCodeElement.getAttribute( "dataelement" );
                    String categoryoptioncombo = deCodeElement.getAttribute( "categoryoptioncombo" );
                    String attributeoptioncombo = deCodeElement.getAttribute( "attributeoptioncombo" );
                    String orgunit = deCodeElement.getAttribute( "orgunit" );
                    int sheetno = new Integer( deCodeElement.getAttribute( "sheetno" ) );
                    int rowno = new Integer( deCodeElement.getAttribute( "rowno" ) );
                    int colno = new Integer( deCodeElement.getAttribute( "colno" ) );

                    ExcelImport exportDataDesign = new ExcelImport( dataelement, orgunit, categoryoptioncombo,
                        attributeoptioncombo, sheetno, rowno, colno, expression );

                    deCodes.add( exportDataDesign );
                }

            } // end of for loop with s var
        } // try block end
        catch ( SAXParseException err )
        {
            System.out.println( "** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId() );
            System.out.println( " " + err.getMessage() );
        }
        catch ( SAXException e )
        {
            Exception x = e.getException();
            ((x == null) ? e : x).printStackTrace();
        }
        catch ( Throwable t )
        {
            t.printStackTrace();
        }
        return deCodes;
    }// getDECodes end

    public Map<String, Integer> getDataElementsIdCodeMap()
    {
        Map<String, Integer> dataElementIdCodeMap = new HashMap<String, Integer>();

        try
        {
            String query = "";

            query = "SELECT dataelementid, code FROM dataelement where code is not null;";

            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer deId = rs.getInt( 1 );
                String deCode = rs.getString( 2 );
                
                if ( deCode != null && deId != null )
                {
                    dataElementIdCodeMap.put( deCode, deId);
                }
            }

            return dataElementIdCodeMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal DataElement id, code", e );
        }
    }
    
    public Map<String, Integer> getCOCIdUidMap()
    {
        Map<String, Integer> cocIdUidMap = new HashMap<String, Integer>();

        try
        {
            String query = "";

            query = "SELECT categoryoptioncomboid,uid FROM categoryoptioncombo;";

            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer cocId = rs.getInt( 1 );
                String cocUid = rs.getString( 2 );
                
                if ( cocId != null && cocUid != null )
                {
                    cocIdUidMap.put( cocUid, cocId );
                }
            }

            return cocIdUidMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal categoryoptioncomboid,uid", e );
        }
    }    

    public Map<String, Integer> getOrgUnitIdCodeMap()
    {
        Map<String, Integer> orgUnitIdUidMap = new HashMap<String, Integer>();

        try
        {
            String query = "";

            query = "SELECT organisationunitid, code FROM organisationunit where code is not null;";

            SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

            while ( rs.next() )
            {
                Integer orgUnitId = rs.getInt( 1 );
                String orgUnitCode = rs.getString( 2 );
                
                if ( orgUnitId != null && orgUnitCode != null )
                {
                    orgUnitIdUidMap.put( orgUnitCode, orgUnitId );
                }
            }

            return orgUnitIdUidMap;
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Illegal organisationunitid, code", e );
        }
    }    
 
  // for selection of weekly period 2nd week of month // for other than SriLanka not required
    public Integer getSecondWeekPeriodId( String isoPeriod )
    {
        Integer periodId = null;
        
        String weeklyPeriodTypeName = WeeklyPeriodType.NAME;
        PeriodType periodType = periodService.getPeriodTypeByName( weeklyPeriodTypeName );
        
        Period period = periodService.reloadIsoPeriod( isoPeriod );
        List<Period> periods = new ArrayList<Period>();
        
      //from LocalDate
//        LocalDate d = LocalDate.of(2018, Month.JANUARY, 10);
//        LocalDate d2 = d.with( TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.MONDAY) );
//        System.out.println( "d2-- " + d2);

        /*
        //from YearMonth
        YearMonth ym = YearMonth.of(2018, Month.JANUARY);
        LocalDate d3 = ym.atDay(1).with(TemporalAdjusters.dayOfWeekInMonth( 2, DayOfWeek.MONDAY) );
        System.out.println( "d3 Jan -- " +  d3);
        
        
        //from YearMonth
        YearMonth ym1 = YearMonth.of(2018, Month.FEBRUARY);
        LocalDate d4 = ym1.atDay(1).with(TemporalAdjusters.dayOfWeekInMonth( 2, DayOfWeek.MONDAY) );
        System.out.println( "d4 Feb -- " +  d4);
        
        //from YearMonth
        YearMonth ym2 = YearMonth.of(2018, Month.of( 03 ));
        LocalDate d5 = ym2.atDay(1).with(TemporalAdjusters.dayOfWeekInMonth( 2, DayOfWeek.MONDAY) );
        System.out.println( "d5 march -- " +  d5);
        */
        
        if( period != null )
        {
            periods = new ArrayList<Period>( periodService.getPeriodsBetweenDates( periodType, period.getStartDate(), period.getEndDate() ) );
         
            //periods = new ArrayList<Period>( periodService.getIntersectingPeriodsByPeriodType( periodType, period.getStartDate(), period.getEndDate() ) );
            //periods = new ArrayList<Period>( periodService.getPeriodsBetweenOrSpanningDates(period.getStartDate(), period.getEndDate() ) );
            
            if ( periods != null && periods.size() > 0 )
            {
                //Collections.sort( periods, new PeriodComparator() );
                Collections.sort( periods );
                Collections.reverse( periods );
                periodId = periods.get( 1 ).getId();

            }
            /*
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd" );
            for ( Period p1 : periods )
            {
                String tempPeriodName = p1.getIsoDate() + " - " +simpleDateFormat.format( p1.getStartDate() ) + " - " + simpleDateFormat.format( p1.getEndDate() );
                System.out.println( "tempPeriodName -- " +  tempPeriodName );
            }
            */
        }
        
        return periodId;
    }    
    
    // for selection of weekly period 2nd week of month // for SriLanka not required
    public Integer getWeekPeriodId( String isoPeriod ) throws Exception
    {
        //System.out.println( isoPeriod + " --  Inside period -- "  );
        Integer periodId = null;
        if( !isoPeriod.equalsIgnoreCase( "period" ))
        {
            int year = Integer.parseInt( isoPeriod.substring(0, 4 ) );
            int month = Integer.parseInt( isoPeriod.substring( isoPeriod.length() - 2 ) );

            //from YearMonth
            
            //System.out.println( "Year -- " + year + " Month -- " +  month );
            
            YearMonth ym2 = YearMonth.of(year, Month.of( month ));
            LocalDate tempWeekDateOfMonth = ym2.atDay(1).with(TemporalAdjusters.dayOfWeekInMonth( 2, DayOfWeek.MONDAY) );
            String secondWeekDateOfMonth = ""+ tempWeekDateOfMonth;
            
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd" );
            Date weeklyDateObject = simpleDateFormat.parse( secondWeekDateOfMonth );
            Calendar cal = Calendar.getInstance();
            cal.setTime( weeklyDateObject );
            int weekNoOfYear = cal.get( Calendar.WEEK_OF_YEAR );
            
            String isoWeeklyPeriod = year+"W"+weekNoOfYear;
            //System.out.println( isoWeeklyPeriod + " Week No -- " +  weekNoOfYear );
            
            Period period = periodService.reloadIsoPeriod( isoWeeklyPeriod );
            
            if( period != null )
            {
                periodId = period.getId();
                //System.out.println( isoPeriod + " Week No -- " +  weekNoOfYear );
            }
            
        }
        
        return periodId;
    }
    //
    @SuppressWarnings( "unused" )
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

    public Integer getOrgUnitIdByCode( String orgUnitCode )
    {
        Integer organisationId = null;
        String query = "SELECT organisationunitid FROM organisationunit WHERE  code = '" + orgUnitCode + "'";

        SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

        while ( rs.next() )
        {
            Integer ouId = rs.getInt( 1 );

            if ( ouId != null )
            {
                organisationId = ouId;
            }
        }

        return organisationId;
    }

    public Integer getCategoryOptionComboByUid( String categoryOptionComboUid )
    {
        Integer categoryOptionComboId = null;
        String query = "SELECT categoryoptioncomboid FROM categoryoptioncombo WHERE  uid = '" + categoryOptionComboUid
            + "'";

        SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

        while ( rs.next() )
        {
            Integer cocId = rs.getInt( 1 );

            if ( cocId != null )
            {
                categoryOptionComboId = cocId;
            }
        }

        return categoryOptionComboId;
    }

    public Integer getAttributeOptionComboByUid( String attributeOptionComboUid )
    {
        Integer attributeOptionComboId = null;
        String query = "SELECT categoryoptioncomboid FROM categoryoptioncombo WHERE  uid = '" + attributeOptionComboUid
            + "'";

        SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

        while ( rs.next() )
        {
            Integer aocId = rs.getInt( 1 );

            if ( aocId != null )
            {
                attributeOptionComboId = aocId;
            }
        }

        return attributeOptionComboId;
    }

    public Integer getDataElementByCode( String dataElementCode )
    {
        Integer dataElementId = null;
        String query = "SELECT dataelementid FROM dataelement WHERE code = '" + dataElementCode + "'";

        SqlRowSet rs = jdbcTemplate.queryForRowSet( query );

        while ( rs.next() )
        {
            Integer deId = rs.getInt( 1 );

            if ( deId != null )
            {
                dataElementId = deId;
            }
        }

        return dataElementId;
    }
   
}
