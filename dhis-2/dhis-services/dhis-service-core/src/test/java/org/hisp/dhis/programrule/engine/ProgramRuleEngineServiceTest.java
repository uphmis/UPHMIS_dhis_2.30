package org.hisp.dhis.programrule.engine;

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

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionSendMessage;
import org.hisp.dhis.rules.models.RuleEffect;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by zubair@dhis2.org on 04.02.18.
 */
@RunWith( MockitoJUnitRunner.class )
public class ProgramRuleEngineServiceTest extends DhisConvenienceTest
{
    private static final String NOTIFICATION_UID = "abc123";
    private static final String DATA = "abc123";

    // -------------------------------------------------------------------------
    // Mocking Dependencies
    // -------------------------------------------------------------------------

    @Mock
    private ProgramRuleEngine programRuleEngine;

    @Mock
    private RuleActionSendMessageImplementer ruleActionSendMessage;

    @Mock
    private ProgramRuleService programRuleService;

    @Spy
    private ArrayList<RuleActionImplementer> ruleActionImplementers;

    @InjectMocks
    private DefaultProgramRuleEngineService service;

    private ProgramInstance programInstance;

    private ProgramStageInstance programStageInstance;

    private ProgramRule programRuleA;

    private List<ProgramRule> programRules = new ArrayList<>();

    private ProgramRuleAction programRuleActionA;

    private List<RuleEffect> ruleEffects;

    @Before
    public void initTest()
    {
        ruleEffects = new ArrayList<>();
        List<RuleEffect> effects = new ArrayList<>();
        effects.add( RuleEffect.create( RuleActionSendMessage.create( NOTIFICATION_UID, DATA ) ) );

        setUpInstances();

        // fill up the spy
        ruleActionImplementers.add( ruleActionSendMessage );

        // stub for programRuleEngine
        when( programRuleEngine.evaluateEnrollment( any() ) ).thenReturn( effects );
        when( programRuleEngine.evaluateEvent( any() ) ).thenReturn( effects );

        // stub for ruleActionSendMessage
        when( ruleActionSendMessage.accept( any() ) ).thenReturn( true );

        doAnswer( invocationOnMock ->
        {
            ruleEffects.add( (RuleEffect) invocationOnMock.getArguments()[0] );
            return ruleEffects;
        }).when( ruleActionSendMessage ).implement( any(), any( ProgramInstance.class ) );

        doAnswer( invocationOnMock ->
        {
            ruleEffects.add( (RuleEffect) invocationOnMock.getArguments()[0] );
            return ruleEffects;
        }).when( ruleActionSendMessage ).implement( any(), any( ProgramStageInstance.class ) );

        when( programRuleService.getProgramRule( any( Program.class ) ) ).thenReturn( programRules );
    }

    @Test
    public void testWhenNoImplementableActionExist_programInstance()
    {
        setProgramRuleActionType_ShowError();

        verify( programRuleEngine, never() ).evaluateEnrollment( programInstance );
        assertEquals( 0, ruleEffects.size() );
    }

    @Test
    public void testWithImplementableActionExist_programInstance()
    {
        setProgramRuleActionType_SendMessage();

        ArgumentCaptor<ProgramInstance> argumentCaptor = ArgumentCaptor.forClass( ProgramInstance.class );

        List<RuleEffect> effects = service.evaluate( programInstance );

        assertEquals( 1, effects.size() );

        RuleAction action = effects.get( 0 ).ruleAction();
        if ( action instanceof  RuleActionSendMessage )
        {
            RuleActionSendMessage ruleActionSendMessage = (RuleActionSendMessage) action;

            assertEquals( NOTIFICATION_UID, ruleActionSendMessage.notification() );
        }

        verify( programRuleEngine, times( 1 ) ).evaluateEnrollment( argumentCaptor.capture() );
        assertEquals( programInstance, argumentCaptor.getValue() );

        verify( ruleActionSendMessage ).accept( action );
        verify( ruleActionSendMessage ).implement( Matchers.any( RuleEffect.class ), argumentCaptor.capture() );

        assertEquals( 1, this.ruleEffects.size() );
        assertTrue( this.ruleEffects.get( 0 ).ruleAction() instanceof RuleActionSendMessage );
    }

    @Test
    public void testWithImplementableActionExist_programStageInstance()
    {
        setProgramRuleActionType_SendMessage();

        ArgumentCaptor<ProgramStageInstance> argumentCaptor = ArgumentCaptor.forClass( ProgramStageInstance.class );

        List<RuleEffect> ruleEffects = service.evaluate( programStageInstance );

        assertEquals( 1, ruleEffects.size() );

        verify( programRuleEngine, times( 1 ) ).evaluateEvent( argumentCaptor.capture() );
        assertEquals( programStageInstance, argumentCaptor.getValue() );

        verify( ruleActionSendMessage ).accept( ruleEffects.get( 0 ).ruleAction() );
        verify( ruleActionSendMessage ).implement( Matchers.any( RuleEffect.class ), argumentCaptor.capture() );

        assertEquals( 1, this.ruleEffects.size() );
        assertTrue( this.ruleEffects.get( 0 ).ruleAction() instanceof RuleActionSendMessage );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void setUpInstances()
    {
        OrganisationUnit organisationUnitA = createOrganisationUnit( 'A' );

        Program programA = createProgram('A', new HashSet<>(), organisationUnitA );
        ProgramStage programStageA = createProgramStage( 'A', programA );

        programRuleA = createProgramRule( 'R', programA );
        programRuleActionA = createProgramRuleAction( 'T' );

        programRuleA.setProgram( programA );

        programInstance = new ProgramInstance();
        programInstance.setProgram( programA );

        programStageA = createProgramStage( 'S', programA );
        programA.getProgramStages().add( programStageA );

        programStageInstance = new ProgramStageInstance();
        programStageInstance.setProgramStage( programStageA );
        programStageInstance.setProgramInstance( programInstance );

        programRules.add( programRuleA );
    }

    private void setProgramRuleActionType_SendMessage()
    {
        programRuleActionA.setProgramRuleActionType( ProgramRuleActionType.SENDMESSAGE );
        programRuleA.getProgramRuleActions().add( programRuleActionA );
    }

    private void setProgramRuleActionType_ShowError()
    {
        programRuleActionA.setProgramRuleActionType( ProgramRuleActionType.SHOWWARNING );
        programRuleA.getProgramRuleActions().add( programRuleActionA );
    }
}
