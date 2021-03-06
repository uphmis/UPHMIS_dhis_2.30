package org.hisp.dhis.reports;

/*
 * Copyright (c) 2004-2005, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in element and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of element code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of the <ORGANIZATION> nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
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

import java.util.Collection;

import org.hisp.dhis.dataelement.DataElement;
//import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;

public interface ReportStore 
{
        String ID = ReportStore.class.getName();
        
    // -------------------------------------------------------------------------
    // Report_in
    // -------------------------------------------------------------------------

        int addReport( Report_in report );
        
        void updateReport( Report_in report );
        
        void deleteReport( Report_in report );
        
        Report_in getReport( int id );
        
        Report_in getReportByName( String name );
        
        Collection<Report_in> getReportBySource( OrganisationUnit source );
        
        Collection<Report_in> getAllReports();
        
        Collection<Report_in> getReportsByReportType( String reportType );
        
        Collection<Report_in> getReportsByPeriodType( PeriodType periodType );
        
        Collection<Report_in> getReportsByPeriodAndReportType( PeriodType periodType, String reportType );

        Collection<Report_in> getReportsByPeriodSourceAndReportType( PeriodType periodType, OrganisationUnit source, String reportType );
		
	// getPatientByOrgUnit
	//Collection<Patient> getPatientByOrgUnit( OrganisationUnit organisationUnit );
    
	// get Patients List By OrgUnit and Program
	//Collection<Patient> getPatientByOrgUnitAndProgram( OrganisationUnit organisationUnit, Program program );

	// Get Data value for Latest Period
	    
	DataValue getLatestDataValue( DataElement dataElement, CategoryOptionCombo categoryOptionCombo, OrganisationUnit organisationUnit );
	
	// Methods for delete Lock Exception
	void deleteLockException( DataSet dataSet, Period period, OrganisationUnit organisationUnit );

}
