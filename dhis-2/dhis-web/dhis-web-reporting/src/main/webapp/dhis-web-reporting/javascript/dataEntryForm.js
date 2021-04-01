
window.onload=function(){
	jQuery('#contentDiv').dialog({autoOpen: false});	
}

// viewDataElements
function generateResult( summary )
{
    document.getElementById("selectedButton").value = summary;

    //alert( summary );
    if(formValidationsForDataEntryFormStatus())
    {
        if(summary == "viewDataElements")
        {
        	var url = "generateDataEntryFormStatusResult.action";
    		
    		generateDataEntryFormStatusResult( url );
        }
    }
   
}

// result Action
function generateDataEntryFormStatusResult( url )
{

	jQuery('#loaderDiv').show();
	
	jQuery('#contentDiv').dialog('destroy').remove();
	
	jQuery( '<div id="contentDiv">' ).load( url,
	{
		selectedDataSetId : getFieldValue( 'selectedDataSets' )
		
	} ).dialog( {
		title: 'Data Entry Form Status Result',
		maximize: true, 
		closable: true,
		modal:true,
		overlay:{ background:'#000000', opacity:0.1 },
		width: 1000,
		height: 800
	} );
	
	jQuery('#loaderDiv').hide();
}




//DataEntry Status Form Validations
function formValidationsForDataEntryFormStatus()
{
    var selDSListSize  = document.generateDataEntryStatusForm.selectedDataSets.options.length;
    
    var dataSetId = $( '#selectedDataSets' ).val();
    
    if(selDSListSize <= 0) 
    {
    	alert("Please Select DataSet(s)"); 
    	return false;
    }
    
    else if( dataSetId == "-1" )
	{	
		showWarningMessage( "Please Select DataSet" );
		return false;
	}
    
    return true;

} 