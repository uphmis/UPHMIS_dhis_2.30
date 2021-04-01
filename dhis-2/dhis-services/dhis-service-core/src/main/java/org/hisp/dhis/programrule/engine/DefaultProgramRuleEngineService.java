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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.rules.models.RuleEffect;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * Created by zubair@dhis2.org on 23.10.17.
 */
public class DefaultProgramRuleEngineService 
    implements ProgramRuleEngineService
{
    private static final Log log = LogFactory.getLog( DefaultProgramRuleEngineService.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private ProgramRuleEngine programRuleEngine;

    @Autowired
    private List<RuleActionImplementer> ruleActionImplementers;

    @Autowired
    private ProgramRuleService programRuleService;

    @Override
    public List<RuleEffect> evaluate( ProgramInstance programInstance )
    {
        List<RuleEffect> ruleEffects = new ArrayList<>();

        try
        {
            ruleEffects = programRuleEngine.evaluateEnrollment( programInstance );
        }
        catch( Exception ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );
            log.error( DebugUtils.getStackTrace( ex.getCause() ) );
        }

        for ( RuleEffect effect : ruleEffects )
        {
            ruleActionImplementers.stream().filter( i -> i.accept( effect.ruleAction() ) ).forEach( i ->
            {
                log.info( String.format( "Invoking action implementer: %s", i.getClass().getSimpleName() ) );

                i.implement( effect, programInstance );
            } );
        }

        return ruleEffects;
    }

    @Override
    public List<RuleEffect> evaluate( ProgramStageInstance programStageInstance )
    {
        List<RuleEffect> ruleEffects = new ArrayList<>();

        try
        {
            ruleEffects = programRuleEngine.evaluateEvent( programStageInstance );
        }
        catch( Exception ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );
            log.error( DebugUtils.getStackTrace( ex.getCause() ) );
        }

        for ( RuleEffect effect : ruleEffects )
        {
            ruleActionImplementers.stream().filter( i -> i.accept( effect.ruleAction() ) ).forEach( i ->
            {
                log.info( String.format( "Invoking action implementer: %s", i.getClass().getSimpleName() ) );

                i.implement( effect, programStageInstance );
            } );
        }

        return ruleEffects;
    }
}
