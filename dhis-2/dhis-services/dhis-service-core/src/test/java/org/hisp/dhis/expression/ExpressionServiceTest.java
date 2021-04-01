package org.hisp.dhis.expression;

/*
 * Copyright (c) 2004-2018, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.*;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.*;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.indicator.IndicatorValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static org.hisp.dhis.expression.Expression.SEPARATOR;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_DAYS;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_WILDCARD;
import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class ExpressionServiceTest
    extends DhisSpringTest
{
    @Autowired
    private ExpressionService expressionService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ConstantService constantService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;
    
    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private PeriodService periodService;

    private CategoryOption categoryOptionA;
    private CategoryOption categoryOptionB;
    private CategoryOption categoryOptionC;
    private CategoryOption categoryOptionD;

    private Category categoryA;
    private Category categoryB;

    private CategoryCombo categoryCombo;

    private DataElement deA;
    private DataElement deB;
    private DataElement deC;
    private DataElement deD;
    private DataElement deE;
    private DataElementOperand opA;
    private DataElementOperand opB;

    private TrackedEntityAttribute teaA;
    private ProgramTrackedEntityAttributeDimensionItem pteaA;
    private ProgramDataElementDimensionItem pdeA;
    private ProgramIndicator piA;
    
    private Period period;
    
    private Program prA;

    private OrganisationUnit unitA;
    private OrganisationUnit unitB;
    private OrganisationUnit unitC;

    private CategoryOptionCombo coc;
    private CategoryOptionCombo cocA;
    private CategoryOptionCombo cocB;
    
    private Constant constantA;
    
    private OrganisationUnitGroup groupA;

    private ReportingRate reportingRate;
    
    private String expressionA;
    private String expressionB;
    private String expressionC;
    private String expressionD;    
    private String expressionE;
    private String expressionF;
    private String expressionG;
    private String expressionH;
    private String expressionI;
    private String expressionK;
    private String expressionJ;
    private String expressionL;
    private String expressionM;
    private String expressionN;

    private String expressionR;

    private String descriptionA;
    private String descriptionB;
    
    private Set<DataElement> dataElements = new HashSet<>();

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
        throws Exception
    {
        Period peJan = createPeriod( "2017-01" );
        Period peFeb = createPeriod( "2017-02" );
        Period peMar = createPeriod( "2017-03" );
        Period peApril = createPeriod( "2017-04" );

        periodService.addPeriod( peJan );
        periodService.addPeriod( peFeb );
        periodService.addPeriod( peMar );
        periodService.addPeriod( peApril );

        categoryOptionA = new CategoryOption( "Under 5" );
        categoryOptionB = new CategoryOption( "Over 5" );
        categoryOptionC = new CategoryOption( "Male" );
        categoryOptionD = new CategoryOption( "Female" );

        categoryService.addCategoryOption( categoryOptionA );
        categoryService.addCategoryOption( categoryOptionB );
        categoryService.addCategoryOption( categoryOptionC );
        categoryService.addCategoryOption( categoryOptionD );

        categoryA = new Category( "Age", DataDimensionType.DISAGGREGATION );
        categoryB = new Category( "Gender", DataDimensionType.DISAGGREGATION );

        categoryA.getCategoryOptions().add( categoryOptionA );
        categoryA.getCategoryOptions().add( categoryOptionB );
        categoryB.getCategoryOptions().add( categoryOptionC );
        categoryB.getCategoryOptions().add( categoryOptionD );

        categoryService.addCategory( categoryA );
        categoryService.addCategory( categoryB );

        categoryCombo = new CategoryCombo( "Age and gender", DataDimensionType.DISAGGREGATION );
        categoryCombo.getCategories().add( categoryA );
        categoryCombo.getCategories().add( categoryB );

        categoryService.addCategoryCombo( categoryCombo );
        
        categoryService.generateOptionCombos( categoryCombo );
        
        List<CategoryOptionCombo> optionCombos = Lists.newArrayList( categoryCombo.getOptionCombos() );
        
        cocA = optionCombos.get( 0 );
        cocB = optionCombos.get( 1 );
        
        deA = createDataElement( 'A' );
        deB = createDataElement( 'B' );
        deC = createDataElement( 'C' );
        deD = createDataElement( 'D' );
        deE = createDataElement( 'E', categoryCombo );

        dataElementService.addDataElement( deA );
        dataElementService.addDataElement( deB );
        dataElementService.addDataElement( deC );
        dataElementService.addDataElement( deD );
        dataElementService.addDataElement( deE );

        coc = categoryService.getDefaultCategoryOptionCombo();

        coc.getId();
        optionCombos.add( coc );

        opA = new DataElementOperand( deA, coc );
        opB = new DataElementOperand( deB, coc );
        
        idObjectManager.save( opA );
        idObjectManager.save( opB );
        
        period = createPeriod( getDate( 2000, 1, 1 ), getDate( 2000, 1, 31 ) );

        prA = createProgram( 'A' );
        
        idObjectManager.save( prA );
        
        teaA = createTrackedEntityAttribute( 'A' );        
        pteaA = new ProgramTrackedEntityAttributeDimensionItem( prA, teaA );        
        pdeA = new ProgramDataElementDimensionItem( prA, deA );
        piA = createProgramIndicator( 'A', prA, null, null );

        idObjectManager.save( teaA );
        idObjectManager.save( pdeA );
        idObjectManager.save( piA );
        
        unitA = createOrganisationUnit( 'A' );
        unitB = createOrganisationUnit( 'B' );
        unitC = createOrganisationUnit( 'C' );

        organisationUnitService.addOrganisationUnit( unitA );
        organisationUnitService.addOrganisationUnit( unitB );
        organisationUnitService.addOrganisationUnit( unitC );

        constantA = new Constant( "ConstantA", 2.0 );
                
        constantService.saveConstant( constantA );

        groupA = createOrganisationUnitGroup( 'A' );
        groupA.addOrganisationUnit( unitA );
        groupA.addOrganisationUnit( unitB );
        groupA.addOrganisationUnit( unitC );
        
        organisationUnitGroupService.addOrganisationUnitGroup( groupA );

        DataSet dataSetA = createDataSet( 'A' );
        dataSetA.setUid( "a23dataSetA" );
        dataSetA.addOrganisationUnit( unitA );
        dataSetService.addDataSet( dataSetA );

        reportingRate = new ReportingRate( dataSetA );
        
        expressionA = "#{" + opA.getDimensionItem() + "}+#{" + opB.getDimensionItem() + "}";
        expressionB = "#{" + deC.getUid() + SEPARATOR + coc.getUid() + "}-#{" + deD.getUid() + SEPARATOR
            + coc.getUid() + "}";
        expressionC = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "}+#{" + deE.getUid() + "}-10";
        expressionD = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "}+" + SYMBOL_DAYS;
        expressionE = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "}*C{" + constantA.getUid() + "}";
        expressionF = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "}";
        expressionG = expressionF + "+#{" + deB.getUid() + "}-#{" + deC.getUid() + "}";
        expressionH = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "}*OUG{" + groupA.getUid() + "}";        
        expressionI = "#{" + opA.getDimensionItem() + "}*" + "#{" + deB.getDimensionItem() + "}+" + "C{" + constantA.getUid() + "}+5-" +
            "D{" + pdeA.getDimensionItem() + "}+" + "A{" + pteaA.getDimensionItem() + "}-10+" + "I{" + piA.getDimensionItem() + "}";
        expressionJ = "#{" + opA.getDimensionItem() + "}+#{" + opB.getDimensionItem() + "}";
        expressionK = "1.5*AVG(" + expressionJ + ")";
        expressionL = expressionA + "+AVG("+expressionJ+")+1.5*STDDEV("+expressionJ+")+" + expressionB;
        expressionM = "#{" + deA.getUid() + SEPARATOR + SYMBOL_WILDCARD + "}-#{" + deB.getUid() + SEPARATOR + coc.getUid() + "}";
        expressionN = "#{" + deA.getUid() + SEPARATOR + cocA.getUid() + SEPARATOR + cocB.getUid() + "}-#{" + deB.getUid() + SEPARATOR + cocA.getUid() + "}";
        expressionR = "#{" + deB.getUid() + SEPARATOR + coc.getUid() + "}" + " + R{" + reportingRate.getUid() + ".REPORTING_RATE}";

        descriptionA = "Expression A";
        descriptionB = "Expression B";

        dataElements.add( deA );
        dataElements.add( deB );
        dataElements.add( deC );
        dataElements.add( deD );
        dataElements.add( deE );

        dataValueService.addDataValue( createDataValue( deA, period, unitA, "10", coc, coc ) );
        dataValueService.addDataValue( createDataValue( deB, period, unitA, "5", coc, coc ) );
    }

    private DimensionalItemId getId( DimensionalItemObject o )
    {
        DimensionItemType type = o.getDimensionItemType();

        switch ( type )
        {
            case DATA_ELEMENT:
                return new DimensionalItemId( type, o.getUid() );

            case DATA_ELEMENT_OPERAND:
                DataElementOperand deo = (DataElementOperand)o;

                return new DimensionalItemId( type,
                    deo.getDataElement().getUid(),
                    deo.getCategoryOptionCombo() == null ? null : deo.getCategoryOptionCombo().getUid(),
                    deo.getAttributeOptionCombo() == null ? null : deo.getAttributeOptionCombo().getUid() );

            case REPORTING_RATE:
                ReportingRate rr = (ReportingRate)o;

                return new DimensionalItemId( type,
                    rr.getDataSet().getUid(),
                    rr.getMetric().name() );

            case PROGRAM_DATA_ELEMENT:
                ProgramDataElementDimensionItem pde = (ProgramDataElementDimensionItem)o;

                return new DimensionalItemId( type,
                    pde.getProgram().getUid(),
                    pde.getDataElement().getUid() );

            case PROGRAM_ATTRIBUTE:
                ProgramTrackedEntityAttributeDimensionItem pa = (ProgramTrackedEntityAttributeDimensionItem)o;

                return new DimensionalItemId( type,
                    pa.getProgram().getUid(),
                    pa.getAttribute().getUid() );

            case PROGRAM_INDICATOR:
                return new DimensionalItemId( type, o.getUid() );

            default:
                return null;
        }
    }

    // -------------------------------------------------------------------------
    // Business logic tests
    // -------------------------------------------------------------------------

    @Test
    public void testGetElementsAndOptionCombosInExpression()
    {
        Set<String> ids = expressionService.getElementsAndOptionCombosInExpression( expressionC );

        assertEquals( 2, ids.size() );
        assertTrue( ids.contains( deA.getUid() + SEPARATOR + coc.getUid() ) );
        assertTrue( ids.contains( deE.getUid() ) );
    }

    @Test
    public void testGetDimensionalItemIdsInExpression()
    {
        Set<DimensionalItemId> itemIds = expressionService.getDimensionalItemIdsInExpression( expressionI );

        assertEquals( 5, itemIds.size() );
        assertTrue( itemIds.contains( getId( opA ) ) );
        assertTrue( itemIds.contains( getId( deB ) ) );
        assertTrue( itemIds.contains( getId( pdeA ) ) );
        assertTrue( itemIds.contains( getId( pteaA ) ) );
        assertTrue( itemIds.contains( getId( piA ) ) );
    }

    @Test
    public void testGetDimensionalItemObjectsInIndicators()
    {
        Indicator indicator = createIndicator( 'A', null );
        indicator.setNumerator( expressionI );
        indicator.setDenominator( expressionA );
        
        Set<Indicator> indicators = Sets.newHashSet( indicator );
        
        Set<DimensionalItemObject> items = expressionService.getDimensionalItemObjectsInIndicators( indicators );
                
        assertEquals( 6, items.size() );
        assertTrue( items.contains( opA ) );
        assertTrue( items.contains( opB ) );
        assertTrue( items.contains( deB ) );
        assertTrue( items.contains( piA ) );
    }
    
    @Test
    public void testGetDataElementsInExpression()
    {
        Set<DataElement> dataElements = expressionService.getDataElementsInExpression( expressionA );

        assertEquals( 2, dataElements.size() );
        assertTrue( dataElements.contains( deA ) );
        assertTrue( dataElements.contains( deB ) );
        
        dataElements = expressionService.getDataElementsInExpression( expressionG );

        assertEquals( 3, dataElements.size() );
        assertTrue( dataElements.contains( deA ) );
        assertTrue( dataElements.contains( deB ) );
        assertTrue( dataElements.contains( deC ) );
        
        dataElements = expressionService.getDataElementsInExpression( expressionM );

        assertEquals( 2, dataElements.size() );
        assertTrue( dataElements.contains( deA ) );
        assertTrue( dataElements.contains( deB ) );
    }
    
    @Test
    public void testGetOperandsInExpression()
    {
        Set<DataElementOperand> operands = expressionService.getOperandsInExpression( expressionA );

        assertEquals( 2, operands.size() );
        assertTrue( operands.contains( opA ) );
        assertTrue( operands.contains( opB ) );        
    }

    @Test
    public void testGetReportingRatesInExpression()
    {
        Set<DimensionalItemObject> reportingRates = expressionService.getDimensionalItemObjectsInExpression( expressionR );

        assertEquals( 2, reportingRates.size() );
        assertTrue( reportingRates.contains( reportingRate ) );
    }

    @Test
    public void testGetAggregatesAndNonAggregtesInExpression()
    {
        Set<String> aggregates = new HashSet<>();
        Set<String> nonAggregates = new HashSet<>();
        expressionService.getAggregatesAndNonAggregatesInExpression( expressionK.toString(), aggregates, nonAggregates );

        assertEquals( 1, aggregates.size() );
        assertTrue( aggregates.contains( expressionJ ) );

        assertEquals( 1, nonAggregates.size() );
        assertTrue( nonAggregates.contains( "1.5*" ) );

        aggregates = new HashSet<>();
        nonAggregates = new HashSet<>();
        expressionService.getAggregatesAndNonAggregatesInExpression( expressionL.toString(), aggregates, nonAggregates );

        assertEquals( 1, aggregates.size() );
        assertTrue( aggregates.contains( expressionJ ) );

        assertEquals( 3, nonAggregates.size() );
        assertTrue( nonAggregates.contains( expressionA + "+" ) );
        assertTrue( nonAggregates.contains( "+1.5*" ) );
        assertTrue( nonAggregates.contains( "+" + expressionB ) );
    }

    @Test
    public void testCalculateExpressionWithCustomFunctions()
    {
        assertEquals( 5.0, calculateExpression( "COUNT([1,2,3,4,5])" ) );
        assertEquals( 15.0, calculateExpression( "SUM([1,2,3,4,5])" ) );
        assertEquals( 1.0, calculateExpression( "MIN([1,2,3,4,5])" ) );
        assertEquals( 5.0, calculateExpression( "MAX([1,2,3,4,5])" ) );
        assertEquals( 3.0, calculateExpression( "AVG([1,2,3,4,5])" ) );
        assertEquals( Math.sqrt( 2 ), calculateExpression( "STDDEV([1,2,3,4,5])" ) );
    }

    private Object calculateExpression( String expressionString )
    {
        Expression expression = new Expression( expressionString, "Test " + expressionString );
        
        return expressionService.getExpressionValue( expression, new HashMap<DimensionalItemObject, Double>(),
            new HashMap<String, Double>(), new HashMap<String, Integer>(), 0 );
    }

    @Test
    public void testGetOptionCombosInExpression()
    {
        Set<CategoryOptionCombo> optionCombos = expressionService.getOptionCombosInExpression( expressionG );

        assertNotNull( optionCombos );
        assertEquals( 1, optionCombos.size() );

        assertTrue( optionCombos.contains( coc ) );
    }

    @Test
    public void testExpressionIsValid()
    {        
    	assertTrue( expressionService.expressionIsValid( expressionA ).isValid() );
        assertTrue( expressionService.expressionIsValid( expressionB ).isValid() );
        assertTrue( expressionService.expressionIsValid( expressionC ).isValid() );
        assertTrue( expressionService.expressionIsValid( expressionD ).isValid() );
        assertTrue( expressionService.expressionIsValid( expressionE ).isValid() );
        assertTrue( expressionService.expressionIsValid( expressionH ).isValid() );
        assertTrue( expressionService.expressionIsValid( expressionK ).isValid() );
        assertTrue( expressionService.expressionIsValid( expressionL ).isValid() );
        assertTrue( expressionService.expressionIsValid( expressionM ).isValid() );
        assertTrue( expressionService.expressionIsValid( expressionN ).isValid() );
        assertTrue( expressionService.expressionIsValid( expressionR ).isValid() );

        String expression = "#{nonExisting" + SEPARATOR + coc.getUid() + "} + 12";

        assertEquals( ExpressionValidationOutcome.DIMENSIONAL_ITEM_OBJECT_DOES_NOT_EXIST, expressionService.expressionIsValid( expression ) );

        expression = "#{" + deA.getUid() + SEPARATOR + "999} + 12";

        assertEquals( ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED, expressionService
            .expressionIsValid( expression ) );

        expression = "#{" + deA.getUid() + SEPARATOR + coc.getUid() + "} + ( 12";

        assertEquals( ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED, expressionService.expressionIsValid( expression ) );

        expression = "12 x 4";

        assertEquals( ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED, expressionService.expressionIsValid( expression ) );
        
        expression = "1.5*AVG(" + expressionJ;

        assertEquals( ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED, expressionService.expressionIsValid( expression ) );
        
        expression = "12 + C{nonExisting}";

        assertEquals( ExpressionValidationOutcome.CONSTANT_DOES_NOT_EXIST, expressionService.expressionIsValid( expression ) );
        
        expression = "12 + OUG{nonExisting}";
        
        assertEquals( ExpressionValidationOutcome.ORG_UNIT_GROUP_DOES_NOT_EXIST, expressionService.expressionIsValid( expression ) );
    }

    @Test
    public void testGetExpressionDescription()
    {
        String description = expressionService.getExpressionDescription( expressionA );

        assertEquals( "DataElementA+DataElementB", description );
        
        description = expressionService.getExpressionDescription( expressionD );
        
        assertEquals( "DataElementA+" + ExpressionService.DAYS_DESCRIPTION, description );

        description = expressionService.getExpressionDescription( expressionE );
        
        assertEquals( "DataElementA*ConstantA", description );
        
        description = expressionService.getExpressionDescription( expressionH );
        
        assertEquals( "DataElementA*OrganisationUnitGroupA", description );
        
        description = expressionService.getExpressionDescription( expressionM );

        assertEquals( "DataElementA-DataElementB", description );

        description = expressionService.getExpressionDescription( expressionR );

        assertEquals( "DataElementB + DataSetA Reporting rate", description );
    }

    @Test
    public void testGenerateExpressionWithMap()
    {   Map<DimensionalItemObject, Double> valueMap = new HashMap<>();
        valueMap.put( new DataElementOperand( deA, coc ), 12d );
        valueMap.put( new DataElementOperand( deB, coc ), 34d );
        valueMap.put( new DataElementOperand( deA, cocA, cocB ), 26d );
        valueMap.put( new DataElementOperand( deB, cocA ), 16d );
        valueMap.put( reportingRate, 20d );
        
        Map<String, Double> constantMap = new HashMap<>();
        constantMap.put( constantA.getUid(), 2.0 );
        
        Map<String, Integer> orgUnitCountMap = new HashMap<>();
        orgUnitCountMap.put( groupA.getUid(), groupA.getMembers().size() );


        assertEquals( "12.0+34.0", expressionService.generateExpression( expressionA, valueMap, constantMap, null, null, null ) );
        assertEquals( "12.0+5", expressionService.generateExpression( expressionD, valueMap, constantMap, null, 5, null ) );
        assertEquals( "12.0*2.0", expressionService.generateExpression( expressionE, valueMap, constantMap, null, null, null ) );
        assertEquals( "12.0*3", expressionService.generateExpression( expressionH, valueMap, constantMap, orgUnitCountMap, null, null ) );
        assertEquals( "26.0-16.0", expressionService.generateExpression( expressionN, valueMap, constantMap, orgUnitCountMap, null, null ) );
        assertEquals( "34.0 + 20.0", expressionService.generateExpression( expressionR, valueMap, constantMap, orgUnitCountMap, null, null ) );
    }

    @Test
    public void testGenerateExpressionWithMapNullIfNoValues()
    {
        Map<DataElementOperand, Double> valueMap = new HashMap<>();
        
        Map<String, Double> constantMap = new HashMap<>();

        assertNull( expressionService.generateExpression( expressionA, valueMap, constantMap, null, null, MissingValueStrategy.SKIP_IF_ANY_VALUE_MISSING ) );
        assertNull( expressionService.generateExpression( expressionD, valueMap, constantMap, null, 5, MissingValueStrategy.SKIP_IF_ANY_VALUE_MISSING ) );
        assertNotNull( expressionService.generateExpression( expressionE, valueMap, constantMap, null, null, MissingValueStrategy.NEVER_SKIP ) );
    }
    
    @Test
    public void testGetExpressionValue()
    {
        Expression expA = new Expression( expressionA, null );
        Expression expD = new Expression( expressionD, null );
        Expression expE = new Expression( expressionE, null );
        Expression expH = new Expression( expressionH, null );
        Expression expN = new Expression( expressionN, null );
        Expression expR = new Expression( expressionR, null );
        
        Map<DimensionalItemObject, Double> valueMap = new HashMap<>();
        valueMap.put( new DataElementOperand( deA, coc ), 12d );
        valueMap.put( new DataElementOperand( deB, coc ), 34d );
        valueMap.put( new DataElementOperand( deA, cocA, cocB ), 26d );
        valueMap.put( new DataElementOperand( deB, cocA ), 16d );
        valueMap.put( reportingRate, 20d );
        
        Map<String, Double> constantMap = new HashMap<>();
        constantMap.put( constantA.getUid(), 2.0 );
        
        Map<String, Integer> orgUnitCountMap = new HashMap<>();
        orgUnitCountMap.put( groupA.getUid(), groupA.getMembers().size() );
        
        assertEquals( 46d, expressionService.getExpressionValue( expA, valueMap, constantMap, null, null ), DELTA );
        assertEquals( 17d, expressionService.getExpressionValue( expD, valueMap, constantMap, null, 5 ), DELTA );
        assertEquals( 24d, expressionService.getExpressionValue( expE, valueMap, constantMap, null, null ), DELTA );
        assertEquals( 36d, expressionService.getExpressionValue( expH, valueMap, constantMap, orgUnitCountMap, null ), DELTA );
        assertEquals( 10d, expressionService.getExpressionValue( expN, valueMap, constantMap, orgUnitCountMap, null ), DELTA );
        assertEquals( 54d, expressionService.getExpressionValue( expR, valueMap, constantMap, orgUnitCountMap, null ), DELTA );
    }
    
    @Test
    public void testGetIndicatorValue()
    {
        IndicatorType indicatorType = new IndicatorType( "A", 100, false );
        
        Indicator indicatorA = createIndicator( 'A', indicatorType );
        indicatorA.setNumerator( expressionE );
        indicatorA.setDenominator( expressionF );
        
        Indicator indicatorB = createIndicator( 'B', indicatorType );
        indicatorB.setNumerator( expressionN );
        indicatorB.setDenominator( expressionF );

        Map<DataElementOperand, Double> valueMap = new HashMap<>();
        valueMap.put( new DataElementOperand( deA, coc ), 12d );
        valueMap.put( new DataElementOperand( deB, coc ), 34d );
        valueMap.put( new DataElementOperand( deA, cocA, cocB ), 46d );
        valueMap.put( new DataElementOperand( deB, cocA ), 10d );
        
        Map<String, Double> constantMap = new HashMap<>();
        constantMap.put( constantA.getUid(), 2.0 );
        
        assertEquals( 200d, expressionService.getIndicatorValue( indicatorA, period, valueMap, constantMap, null ), DELTA );
        assertEquals( 300d, expressionService.getIndicatorValue( indicatorB, period, valueMap, constantMap, null ), DELTA );
    }

    @Test
    public void testGetIndicatorValueObject()
    {
        IndicatorType indicatorType = new IndicatorType( "A", 100, false );
        
        Indicator indicatorA = createIndicator( 'A', indicatorType );
        indicatorA.setNumerator( expressionE );
        indicatorA.setDenominator( expressionF );

        Indicator indicatorB = createIndicator( 'B', indicatorType );
        indicatorB.setNumerator( expressionN );
        indicatorB.setDenominator( expressionF );
        indicatorB.setAnnualized( true );

        Map<DataElementOperand, Double> valueMap = new HashMap<>();
        valueMap.put( new DataElementOperand( deA, coc ), 12d );
        valueMap.put( new DataElementOperand( deB, coc ), 34d );
        valueMap.put( new DataElementOperand( deA, cocA, cocB ), 46d );
        valueMap.put( new DataElementOperand( deB, cocA ), 10d );
        
        Map<String, Double> constantMap = new HashMap<>();
        constantMap.put( constantA.getUid(), 2.0 );
        
        IndicatorValue value = expressionService.getIndicatorValueObject( indicatorA, period, valueMap, constantMap, null );
        
        assertEquals( 24d, value.getNumeratorValue(), DELTA );
        assertEquals( 12d, value.getDenominatorValue(), DELTA );
        assertEquals( 100, value.getMultiplier() );
        assertEquals( 1, value.getDivisor() );
        assertEquals( 100d, value.getFactor(), DELTA );
        assertEquals( 200d, value.getValue(), DELTA );
        
        value = expressionService.getIndicatorValueObject( indicatorB, period, valueMap, constantMap, null );

        assertEquals( 36d, value.getNumeratorValue(), DELTA );
        assertEquals( 12d, value.getDenominatorValue(), DELTA );
        assertEquals( 36500, value.getMultiplier() );
        assertEquals( 31, value.getDivisor() );
        assertEquals( 1177.419, value.getFactor(), DELTA );
        assertEquals( 3532.258, value.getValue(), DELTA );
    }
    
    // -------------------------------------------------------------------------
    // CRUD tests
    // -------------------------------------------------------------------------

    @Test
    public void testAddGetExpression()
    {
        Expression expression = new Expression( expressionA, descriptionA );

        int id = expressionService.addExpression( expression );

        expression = expressionService.getExpression( id );

        assertEquals( expressionA, expression.getExpression() );
        assertEquals( descriptionA, expression.getDescription() );
    }

    @Test
    public void testUpdateExpression()
    {
        Expression expression = new Expression( expressionA, descriptionA );

        int id = expressionService.addExpression( expression );

        expression = expressionService.getExpression( id );

        assertEquals( expressionA, expression.getExpression() );
        assertEquals( descriptionA, expression.getDescription() );

        expression.setExpression( expressionB );
        expression.setDescription( descriptionB );

        expressionService.updateExpression( expression );

        expression = expressionService.getExpression( id );

        assertEquals( expressionB, expression.getExpression() );
        assertEquals( descriptionB, expression.getDescription() );
    }

    @Test
    public void testDeleteExpression()
    {
        Expression exprA = new Expression( expressionA, descriptionA );
        Expression exprB = new Expression( expressionB, descriptionB );

        int idA = expressionService.addExpression( exprA );
        int idB = expressionService.addExpression( exprB );

        assertNotNull( expressionService.getExpression( idA ) );
        assertNotNull( expressionService.getExpression( idB ) );

        expressionService.deleteExpression( exprA );

        assertNull( expressionService.getExpression( idA ) );
        assertNotNull( expressionService.getExpression( idB ) );

        expressionService.deleteExpression( exprB );

        assertNull( expressionService.getExpression( idA ) );
        assertNull( expressionService.getExpression( idB ) );
    }

    @Test
    public void testGetAllExpressions()
    {
        Expression exprA = new Expression( expressionA, descriptionA );
        Expression exprB = new Expression( expressionB, descriptionB );

        expressionService.addExpression( exprA );
        expressionService.addExpression( exprB );

        List<Expression> expressions = expressionService.getAllExpressions();

        assertTrue( expressions.size() == 2 );
        assertTrue( expressions.contains( exprA ) );
        assertTrue( expressions.contains( exprB ) );
    }
    
    @Test
    public void testGetOrganisationUnitGroupsInExpression()
    {        
        Set<OrganisationUnitGroup> groups = expressionService.getOrganisationUnitGroupsInExpression( expressionH );
        
        assertNotNull( groups );
        assertEquals( 1, groups.size() );
        assertTrue( groups.contains( groupA ) );

        groups = expressionService.getOrganisationUnitGroupsInExpression( null );
        
        assertNotNull( groups );
        assertEquals( 0, groups.size() );        
    }
}
